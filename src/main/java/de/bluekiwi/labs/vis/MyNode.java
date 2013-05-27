package de.bluekiwi.labs.vis;

import java.text.DecimalFormat;
import java.util.HashSet;

public class MyNode implements Comparable<MyNode> {

    private HashSet<Integer> ids;
    private double activation;
    private String candidateURI;
    private int level;
    private double HubWeight;
    private double AuthorityWeight;

    public MyNode(String uri, double activation, int level) {
        this.candidateURI = uri;
        this.activation = activation;
        this.level = level;
        this.HubWeight = 1;
        this.AuthorityWeight = 1;
        ids = new HashSet<Integer>();
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#.####");
        return candidateURI.replace("http://dbpedia.org/resource/", "") + ":" + String.valueOf(df.format(activation))
                + " H: "
                + String.valueOf(df.format(HubWeight)) + " A: " + String.valueOf(df.format(AuthorityWeight));
    }

    @Override
    public int hashCode()
    {
        return candidateURI.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this.hashCode() == o.hashCode()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(MyNode m) {
        if (m.getAuthorityWeight() == this.getAuthorityWeight()) {
            return 0;
        } else if (m.getAuthorityWeight() > this.getAuthorityWeight()) {
            return 1;
        } else {
            return -1;
        }
    }

    public boolean containsId(int id) {
        return ids.contains(id);
    }

    public void addId(int id) {
        ids.add(id);
    }

    public String getCandidateURI() {
        return candidateURI;
    }

    public void setCandidateURI(String uri) {
        this.candidateURI = uri;
    }

    public void setActivation(double activation) {
        this.activation = activation;

    }

    public double getActivation() {
        return activation;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int i) {
        level = i;

    }

    public double getHubWeight() {
        return HubWeight;
    }

    public void setAuthorityWeight(double x) {
        this.AuthorityWeight = x;

    }

    public double getAuthorityWeight() {
        return AuthorityWeight;
    }

    public void setHubWeight(double y) {
        this.HubWeight = y;

    }

    public Integer[] getLabels() {
        return ids.toArray(new Integer[ids.size()]);
    }
}
