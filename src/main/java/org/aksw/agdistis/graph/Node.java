package org.aksw.agdistis.graph;

import java.text.DecimalFormat;
import java.util.HashSet;

public class Node implements Comparable<Node> {

	private HashSet<Integer> ids;
	private double activation;
	private String candidateURI;
	private int level;
	private double hubWeightForCalculation = 1;
	private double authorityWeightForCalculation = 1;
	private double unnormalizedHubWeight;
	private double unnormalizedAuthorityWeight;
	private double hubWeight;
	private double authorityWeight;
	private double pageRank;
	private double pageRankNew; 
	

    private HashSet<Node> predecessors;
	private HashSet<Node> successors;

	public Node(String uri, double activation, int level) {
		this.candidateURI = uri;
		this.activation = activation;
		this.level = level;
		this.hubWeight = 1;
		this.authorityWeight = 1;
		ids = new HashSet<Integer>();
		this.successors = new HashSet<Node>();
		this.predecessors = new HashSet<Node>();
		this.pageRank = 0; 
	}

	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("#.####");
		return candidateURI + ":" + String.valueOf(df.format(activation)) + " H: " + String.valueOf(df.format(hubWeight) + " A: " + String.valueOf(df.format(authorityWeight)  + " PR: " + String.valueOf(df.format(pageRank))));
	}

	@Override
	public int hashCode() {
		return candidateURI.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this.hashCode() == o.hashCode()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	// this determines the ordering of candidates and thus the result of the algorithm
	// change to hub score
	public int compareTo(Node m) {
		
	    // AuthorityWeight
	    /*  
		if (m.getAuthorityWeight() == this.getAuthorityWeight()) {
			return 0;
		} else if (m.getAuthorityWeight() > this.getAuthorityWeight()) {
			return 1;
		} else {
			return -1;
		}
		*/
	    
	    // HubWeight
	    /*
		if (m.getHubWeight() == this.getHubWeight()) {
			return 0;
		} else if (m.getHubWeight() > this.getHubWeight()) {
			return 1;
		} else {
			return -1;
		}
		*/
	    
	    // PageRank
		if (m.getPageRank() == this.getPageRank()) {
            return 0;
        } else if (m.getPageRank() > this.getPageRank()) {
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
		return hubWeight;
	}

	public void setAuthorityWeight(double x) {
		this.authorityWeight = x;

	}

	public double getAuthorityWeight() {
		return authorityWeight;
	}

	public void setHubWeight(double y) {
		this.hubWeight = y;

	}

	public Integer[] getLabels() {
		return ids.toArray(new Integer[ids.size()]);
	}

	public HashSet<Node> getSuccessors() {
		return successors;
	}

	public HashSet<Node> getPredecessors() {
		return predecessors;
	}

	public void addPredecessor(Node first) {
		predecessors.add(first);
	}

	public void addSuccesor(Node second) {
		successors.add(second);
	}

	public void removeSuccesor(Node right) {
		successors.remove(right);
	}

	public void removePredecessor(Node left) {
		predecessors.remove(left);
	}

	public double getHubWeightForCalculation() {
		return hubWeightForCalculation;
	}

	public void setHubWeightForCalculation(double hubWeightForCalculation) {
		this.hubWeightForCalculation = hubWeightForCalculation;
	}

	public double getAuthorityWeightForCalculation() {
		return authorityWeightForCalculation;
	}

	public void setAuthorityWeightForCalculation(double authorityWeightForCalculation) {
		this.authorityWeightForCalculation = authorityWeightForCalculation;
	}

	public double getUnnormalizedHubWeight() {
		return unnormalizedHubWeight;
	}

	public void setUnnormalizedHubWeight(double unnormalizedHubWeight) {
		this.unnormalizedHubWeight = unnormalizedHubWeight;
	}

	public double getUnnormalizedAuthorityWeight() {
		return unnormalizedAuthorityWeight;
	}

	public void setUnnormalizedAuthorityWeight(double unnormalizedAuthorityWeight) {
		this.unnormalizedAuthorityWeight = unnormalizedAuthorityWeight;
	}

	public double getPageRank() {
		return pageRank;
	}

	public void setPageRank(double pageRank) {
		this.pageRank = pageRank;
	}

    public double getPageRankNew() {
        return pageRankNew;
    }
    
    public void setPageRankNew(double pageRankNew) {
        this.pageRankNew = pageRankNew;
    }

}
