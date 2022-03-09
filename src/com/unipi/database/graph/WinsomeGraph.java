package com.unipi.database.graph;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.unipi.database.Database;
import com.unipi.database.graph.graphNodes.GroupNode;
import com.unipi.database.graph.graphNodes.Node;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WinsomeGraph {
    private MutableGraph<Node> graph;
    private ReentrantReadWriteLock rwlock;
    private GroupNode newEntryGroup;

    public WinsomeGraph(Database db) {
        graph = GraphBuilder.undirected().allowsSelfLoops(false).build();
        rwlock = new ReentrantReadWriteLock();
        newEntryGroup = new GroupNode("NEW ENTRY", null);

        File dbFolder = new File(Database.getName());
        if (!dbFolder.exists()) {
            if (!dbFolder.mkdir()) {
                System.err.println("Errore nella creazione della cartella del grafo");
                System.exit(-1);
            }

            File jsonsFolder = new File(Database.getName() + File.separator + "jsons");
            if(!jsonsFolder.mkdir()) {
                System.err.println("Errore nella creazione della cartella jsons");
                System.exit(-1);
            }
        }
        GraphLoader loader = new GraphLoader(db, this);
        try {
            loader.loadGraph();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean addNode(Node n) {
        rwlock.writeLock().lock();
        boolean inserted = graph.addNode(n);
        rwlock.writeLock().unlock();

        return inserted;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean putEdge(Node n1, Node n2) {
        rwlock.writeLock().lock();
        boolean inserted = graph.putEdge(n1, n2);
        rwlock.writeLock().unlock();

        return inserted;
    }

    public Set<Node> adjacentNodes(Node n) {
        rwlock.readLock().lock();
        Set<Node> set = graph.adjacentNodes(n);
        rwlock.readLock().unlock();

        return set;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean removeNode(Node n) {
        rwlock.writeLock().lock();
        boolean deleted = graph.removeNode(n);
        rwlock.writeLock().unlock();

        return deleted;
    }

    public Set<Node> nodes() {
        rwlock.readLock().lock();
        Set<Node> set = graph.nodes();
        rwlock.readLock().unlock();

        return set;
    }

    public Set<EndpointPair<Node>> edges() {
        rwlock.readLock().lock();
        Set<EndpointPair<Node>> set = graph.edges();
        rwlock.readLock().unlock();

        return set;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean removeEdge(Node n1, Node n2) {
        rwlock.writeLock().lock();
        boolean deleted = graph.removeEdge(n1, n2);
        rwlock.writeLock().unlock();

        return deleted;
    }

    public void clearEdges(Node n){
        rwlock.writeLock().lock();
        graph.removeNode(n);
        graph.addNode(n);
        rwlock.writeLock().unlock();
    }

    public MutableGraph<Node> getGraph() {
        return graph;
    }

    private String pathof(String username) {
        return "graphDB" + File.separator + username;
    }

    private String jsonPathOf(String filename) {
        return "graphDB" + File.separator + "jsons" + File.separator + filename + ".json";
    }
}
