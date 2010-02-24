package edu.ucla.sspace.dependency;

public class DependencyLink {

    private String relation;
    private int neighbor;

    public DependencyLink(int neighbor, String relation) {
        this.relation = relation;
        this.neighbor = neighbor;
    }

    public String relation() {
        return relation;
    }

    public int neighbor() {
        return neighbor;
    }
}
