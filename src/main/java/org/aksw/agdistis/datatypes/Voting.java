package org.aksw.agdistis.datatypes;

public class Voting extends Candidate {

    private int votingId;
    private String user;

    public Voting(int votingId, int candidateId, String url, String label, String user) {
        super(candidateId, url, label, null);
        this.votingId = votingId;
        this.user = user;
    }

    public Voting(int votingId, int candidateId, String user) {
        this(votingId, candidateId, null, null, user);
    }

    @Override
    public int getId() {
        return votingId;
    }

    @Override
    public void setId(int id) {
        this.votingId = id;
    }

    public int getCandidateId() {
        return super.getId();
    }

    public void setCandidateId(int candidateId) {
        super.setId(candidateId);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "c: " + getLabel() + " -> " + getUrl() + " id: " + super.getId();
    }
}
