package com.unipi.database.graph.graphNodes;

public class GraphNode<T> implements Node {
    private T value;

    public GraphNode(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof GraphNode<?> g){
            return value.equals(g.value);
        }

        return false;
    }

    @Override
    public String toString() {
        return "[NODE] " + value.toString();
    }
}
