package org.texttechnologylab.dependency.json;

public class GraphIteratorItem {

    public final String textId;
    public final Integer[][] dependencyEdges;
    public final Integer[][] punctEdges;

    public GraphIteratorItem(String textId, Integer[][] dependencyEdges, Integer[][] punctEdges) {
        this.textId = textId;
        this.dependencyEdges = dependencyEdges;
        this.punctEdges = punctEdges;
    }
}
