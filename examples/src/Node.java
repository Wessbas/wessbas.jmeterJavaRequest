package net.sf.markov4jmeter.test;

import java.io.Serializable;

// Class must implement the Serializable interface for being encoded to a
// String and vice versa.
public class Node implements Serializable {

    // Default serial version ID.
    private static final long serialVersionUID = 1L;

    private static int idCounter = 1;

    private final Node parent;
    private final int id;


    // Constructor with a specific parent.
    private Node (Node parent) {

        this.parent = parent;
        this.id     = Node.idCounter++;

    }

    // Static method for creating nodes.
    public static Node create (Node parent) {

        return (parent == null) ? new Node(null) : parent.create();
    }

    // Private method for creating child nodes.
    private Node create () {

        return new Node(this);
    }

    @Override
    public String toString () {

        // memory hash code for identification purposes.
        final int identityHashCode = System.identityHashCode(this);

        return parent + " <- " + id + " (" + identityHashCode + ")";
    }


    // Example code for using the Node class.
    public static void main (String[] argv) {

        // create an initial node.
        final Node example = Node.create(null);

        // create a child node of the initial node.
        System.out.println( example.create() );
    }
}