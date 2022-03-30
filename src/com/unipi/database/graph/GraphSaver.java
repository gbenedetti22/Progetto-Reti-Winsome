package com.unipi.database.graph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unipi.database.Database;
import com.unipi.database.graph.graphNodes.GraphNode;
import com.unipi.database.graph.graphNodes.Node;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Like;
import com.unipi.database.tables.Post;
import com.unipi.database.tables.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

public class GraphSaver {
    private Gson gson;

    public GraphSaver() {
        gson = new GsonBuilder().setPrettyPrinting().setDateFormat(Database.getDateFormat().toString()).create();
    }

    public synchronized void saveUser(String username) {
        if (username == null) return;

        try {
            File file = new File(pathOf(username));
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    System.err.println("Impossibile salvare l utente: " + username);
                    System.err.println("Non è stato possibile creare il file");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void savePost(Post p) {
        if (p == null || p.getLinePosition() != -1) return;

        try {
            RandomAccessFile file = new RandomAccessFile(pathOf(p.getAuthor()), "rw");
            file.seek(file.length());
            p.setLinePosition(file.getFilePointer());

            file.writeBytes(String.format("POST;%s\n", p.getId().toString()));
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removePost(Post p, Set<Node> comments, Set<Node> likes) {
        if (p == null || p.getLinePosition() < 0) return;

        String username = p.getAuthor();
        try {
            RandomAccessFile raf = new RandomAccessFile(pathOf(username), "rw");
            raf.seek(p.getLinePosition());
            raf.writeBytes("#".repeat(5 + 36)); //lunghezza "POST;" + lunghezza UUID

            for (Node n : comments) {
                if (n instanceof GraphNode<?> g && g.getValue() instanceof Comment c) {
                    long linePosition = c.getLinePosition();
                    raf.seek(linePosition);
                    raf.writeBytes("#".repeat(8 + 36 + 10)); //COMMENT; + UUID + ;NEW_ENTRY

                    File file = new File(jsonPathOf(c.getId().toString()));
                    if (!file.delete()) {
                        System.err.println("Errore nel cancellare il file " + file.getName());
                        file.deleteOnExit();
                    }
                }
            }

            for (Node n : likes) {
                if (n instanceof GraphNode<?> g && g.getValue() instanceof Like l) {
                    long linePosition = l.getLinePosition();
                    raf.seek(linePosition);
                    raf.writeBytes("#".repeat(5 + 36 + 10)); //LIKE; + UUID + ;NEW_ENTRY

                    File file = new File(jsonPathOf(l.getId().toString()));
                    if (!file.delete()) {
                        System.err.println("Errore nel cancellare il file " + file.getName());
                        file.deleteOnExit();
                    }

                }
            }

            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void saveComment(String username, Comment c) {
        if (username == null || c == null || c.getLinePosition() != -1) return;

        try {
            RandomAccessFile file = new RandomAccessFile(pathOf(username), "rw");
            file.seek(file.length());
            c.setLinePosition(file.getFilePointer());

            file.writeBytes(String.format("COMMENT;%s;NEW_ENTRY\n", c.getId()));
            file.close();

            String s = gson.toJson(c);
            BufferedWriter out = new BufferedWriter(new PrintWriter(jsonPathOf(c.getId().toString())));
            out.write(s);
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveLike(String username, Like l) {
        if (l == null) return;

        //Se il like già esiste, allora aggiorno solo il json
        if (Files.exists(Path.of(jsonPathOf(l.getId().toString())))) {
            try {
                String s = gson.toJson(l);
                BufferedWriter out = new BufferedWriter(new PrintWriter(jsonPathOf(l.getId().toString())));
                out.write(s);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            RandomAccessFile file = new RandomAccessFile(pathOf(username), "rw");
            file.seek(file.length());
            l.setLinePosition(file.getFilePointer());

            file.writeBytes(String.format("LIKE;%s;NEW_ENTRY\n", l.getId()));
            file.close();

            String s = gson.toJson(l);
            BufferedWriter out = new BufferedWriter(new PrintWriter(jsonPathOf(l.getId().toString())));
            out.write(s);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveRewin(User u, UUID idPost) {
        String rewinFile = rewinsPath();

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(rewinFile, true));
            out.write(String.format("%s;%s\n", u.getUsername(), idPost.toString()));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeRewinFromFile(String username, UUID idPost) {
        try {
            if (!Files.exists(Paths.get(rewinsPath()))) return;

            RandomAccessFile file = new RandomAccessFile(rewinsPath(), "rw");
            long offset = 0, lineLenght;
            boolean found = false;

            String line;
            String record = String.format("%s;%s", username, idPost.toString());
            while ((line = file.readLine()) != null) {
                if (line.equals(record)) {
                    found = true;
                    break;
                }
                offset = file.getFilePointer();
            }

            lineLenght = file.getFilePointer() - offset;

            if (!found) {
                file.close();
                return;
            }

            int read;
            byte[] buffer = new byte[512];
            // in pratica leggo quello che viene dopo la riga da cancellare,
            // poi mi posizione all inizio della riga e sovrascrivo tutto
            while ((read = file.read(buffer)) > -1) {
                file.seek(file.getFilePointer() - read - lineLenght);
                file.write(buffer, 0, read);
                file.seek(file.getFilePointer() + lineLenght);
            }

            file.setLength(file.length() - lineLenght);
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void removeEntry(String username, Like l) {
        if (l == null || l.getLinePosition() == -1) return;

        try {
            RandomAccessFile file = new RandomAccessFile(pathOf(username), "rw");
            long linePosition = l.getLinePosition();

            file.seek(linePosition + "LIKE".length() + 36 + 2);
            file.writeBytes("#".repeat("NEW_ENTRY".length()));
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeEntry(String username, Comment c) {
        if (c == null || c.getLinePosition() == -1) return;

        try {
            RandomAccessFile file = new RandomAccessFile(pathOf(username), "rw");
            long linePosition = c.getLinePosition();

            file.seek(linePosition + "COMMENT".length() + 36 + 2);
            file.writeBytes("#".repeat("NEW_ENTRY".length()));
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String pathOf(String username) {
        return Database.getName() + File.separator + username;
    }

    private String jsonPathOf(String filename) {
        return Database.getName() + File.separator + "jsons" + File.separator + filename + ".json";
    }

    private String rewinsPath() {
        return Database.getName() + File.separator + "rewins";
    }
}
