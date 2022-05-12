package winsome.database.graph.graphNodes;

import java.util.Objects;

public class GroupNode implements Node {
    private String label;
    private Node parent;

    public GroupNode(String label, Node parent) {
        this.label = label;
        this.parent = parent;
    }

    public Node getParent() {
        return parent;
    }

    @Override
    public int hashCode() {
        if (parent == null) return Objects.hash(label);

        return Objects.hash(parent.hashCode(), label);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GroupNode g) {
            if (g.parent != null && parent == null) return false;
            if (g.parent == null && parent != null) return false;
            if (g.parent == null) return true;

            return parent.equals(g.getParent());
        }

        return false;
    }

    @Override
    public String toString() {
        if (parent == null) return "[Group: " + label + "]";

        return "[Group: " + label + " -> parent: " + parent + "]";
    }
}
