package winsome.database.graph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import winsome.database.Database;
import winsome.database.graph.graphNodes.GraphNode;
import winsome.database.graph.graphNodes.Node;
import winsome.database.tables.Comment;
import winsome.database.tables.Like;
import winsome.database.tables.Post;
import winsome.database.tables.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/*
    Classe adibita al salvataggio dei dati del database sul disco (viene messo tutto in una cartella apposita).
    Il salvataggio cambia a seconda del tipo di dato
 */
public class GraphSaver {
    private Gson gson;

    public GraphSaver() {
        gson = new GsonBuilder().setPrettyPrinting().setDateFormat(Database.getDateFormat().toString()).create();
    }

    // Semplice funzione che crea un file nella cartella del DB
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

    // Appende in fondo al file, denominato dal nome dell autore, l id del post
    // e viene salvata la posizione della riga appena scritta dentro l oggetto passato
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

    // rimozione di un post dal disco.
    // viene rimosso il post e tutti i suoi like/commenti accedendo alle righe salvate in precedenza.
    // l operazione avviene nel seguente modo:
    // 1. apro il file, vado alla riga del post e la cancello con #
    // 2. per ogni like e commento, vado alla riga corrispondente e la cancello con #
    // ricordo che io so già a prescindere dove si trovano le righe, quindi non devo cercare nulla nel file!
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
                        System.err.println("Verrà cancellato in futuro");
                        file.deleteOnExit();
                    }

                }
            }

            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Salvo il commento dentro il file dell autore del post.
    // Il parametro "username" corrisponde al nome dell'autore del post sotto cui è stato lasto il commento "c";
    // questo perchè ne facilita la cancellazione del post (devo aprire un file solo)
    // il procedimento è il seguente:
    // 1. apro il file dell'autore del post
    // 2. vado in fondo al file
    // 3. scrivo il commento e lo marco come "NEW_ENTRY" per il calcolo delle ricompense
    // 4. salvo la posizione del commento nel file
    // 5. chiudo il file
    // 6. salvo il commento nel disco in formato json (il nome del file corrisponde all'id del commento)
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

    // metodo per salvare un like nel disco
    // il procedimento è simile a quello adottato per il salvataggio di un commento
    // cambia solo che se il like già esiste, allorno aggiorno il file
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

    // metodo per salvare un rewin sul disco
    // i rewin vengono salvati tutti in unico file, in modo da poterli leggere in una sola volta.
    // la tecnica usata per i post normali non viene adottata per mancanza di tempo (servirebbe creare un oggetto Rewin)
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

    // metodo per cancellare un rewin dal disco.
    // A differenza dei post normali, i rewin vengono cancellati senza l ausilio del #
    // questo ne comporta in un maggior costo, in quanto le righe successive devono essere spostate per non incorrere
    // in file di grandi dimensioni
    public void removeRewinFromFile(String username, UUID idPost) {
        try {
            String rewinsFile = rewinsPath();
            if (!Files.exists(Paths.get(rewinsFile))) return;

            RandomAccessFile file = new RandomAccessFile(rewinsFile, "rw");
            long offset = 0, lineLenght;
            boolean found = false;

            String line;
            String record = String.format("%s;%s", username, idPost.toString());
            // cerco la riga da cancellare e memorizzo la sua posizione
            while ((line = file.readLine()) != null) {
                if (line.equals(record)) {
                    found = true;
                    break;
                }
                offset = file.getFilePointer();
            }

            if (!found) {
                file.close();
                return;
            }

            // lineLenght contiene la posizione della prima lettera della riga da cancellare
            //                                      ciao mondo
            // file.getFilePointer() - offset -----^          ^---- file.getFilePointer()
            lineLenght = file.getFilePointer() - offset;

            int read;
            byte[] buffer = new byte[512];
            // in pratica leggo quello che viene dopo la riga da cancellare,
            // poi mi posizione all inizio della riga e sovrascrivo tutto
            while ((read = file.read(buffer)) > -1) {
                file.seek(file.getFilePointer() - read - lineLenght);
                file.write(buffer, 0, read);
                file.seek(file.getFilePointer() + lineLenght);
            }

            //aggiorno la grandezza del file e cancello le righe superflue
            file.setLength(file.length() - lineLenght);
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // metodo per rimuovere la marcatura "NEW_ENTRY".
    // Una volta che la ricompensa è stata calcolata, il like/commento non viene più considerato
    // per i successivi calcoli
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

    // funzione che restituisce il path della cartella del db
    // la cartella prende il nome del database
    private String pathOf(String username) {
        return Database.getName() + File.separator + username;
    }

    // funzione che restituisce il path del file json del like/commento
    private String jsonPathOf(String filename) {
        return Database.getName() + File.separator + "jsons" + File.separator + filename + ".json";
    }

    // funzione che restituisce il path del rewin file
    private String rewinsPath() {
        return Database.getName() + File.separator + "rewins";
    }
}
