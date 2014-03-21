package net.sf.markov4jmeter.javarequest.test;

import net.sf.markov4jmeter.javarequest.test.Node;

public class Node {

    private static volatile int idCounter = 1;

    private final Node parent;
    private final int  id;


    // constructor with a specific parent.
    private Node (Node parent) {

        this.parent = parent;
        this.id     = this.getId();
    }

    private synchronized int getId () {

        return Node.idCounter++;
    }

    // static method for creating nodes
    public static Node create (Node parent) {

        return (parent == null) ? new Node(null) : parent.create();
    }

    // private method for creating child nodes.
    private Node create () {

        return new Node(this);
    }

    @Override
    public String toString () {

        // memory hash code for identification purposes.
        final int identityHashCode = System.identityHashCode(this);

        return parent + " <- " + id + " (" + identityHashCode + ")";
    }

    public static void main (String[] argv) {

        // create an initial node.
        final Node example = Node.create(null);

        // create a child node of the initial node.
        System.out.println( example.create() );
    }
}