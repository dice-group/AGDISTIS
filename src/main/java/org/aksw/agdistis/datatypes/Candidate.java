package org.aksw.agdistis.datatypes;

public class Candidate implements Comparable<Candidate> {

    public static final int NO_ENTITY_CANDIDATE_ID = -1;
    public static final int OTHER_ENTITY_CANDIDATE_ID = -2;

    private int id;
    private String url;
    private String label;
    private String description;
    private int outgoingEdgeCount;

    public Candidate(String url, String label, String description) {
        this(NO_ENTITY_CANDIDATE_ID, url, label, description);
    }

    public Candidate(int id, String url, String label, String description) {
        this(id, url, label, description, -1);
    }

    public Candidate(String url, String label, String description, int outgoingEdgeCount) {
        this(NO_ENTITY_CANDIDATE_ID, url, label, description, outgoingEdgeCount);
    }

    public Candidate(int id, String url, String label, String description, int outgoingEdgeCount) {
        this.id = id;
        this.url = url;
        this.label = label;
        this.description = description;
        this.outgoingEdgeCount = outgoingEdgeCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public boolean equals(Object c) {
        if (c instanceof Candidate)
        {
            if (((Candidate) c).getUrl().equals(url))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "c: " + label + " -> " + url + " #E " + outgoingEdgeCount;
    }

    @Override
    public int compareTo(Candidate o) {
        if (o.getOutgoingEdgeCount() > this.getOutgoingEdgeCount()) {
            return 1;
        } else if (o.getOutgoingEdgeCount() < this.getOutgoingEdgeCount()) {
            return -1;
        } else {
            return 0;
        }
    }

    public int getOutgoingEdgeCount() {
        return outgoingEdgeCount;
    }

    public void setOutgoingEdgeCount(int e) {
        this.outgoingEdgeCount = e;
    }
}
