package org.aksw.agdistis.graph;

import java.util.Collection;


import edu.uci.ics.jung.graph.Graph;

/**
 * @author m
 *
 */
public class PageRank {
	
	


	/**
	 * @param args
	 */
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void runPr(Graph g, int maxIteration, double threshold)  {
		
		int numNodes = g.getVertices().size();
		int numIteration = 0;
		Node n;
		Node m;
		Object[] successors;
		Collection succ;
		
		double w = 0.85; // standard weight 
		double pr = (double )1/numNodes;  // temporary storage for pagerank
		double randomWalker;
		double distance;
		double sumPr;
		
	    // initialize PR set every value to 1/numNodes
		for (Object o : g.getVertices()) {
			n = (Node) o;
			n.setPageRank(pr);  
			n.setPageRankNew(0);
			//System.out.println("INITIALIZE NODE: "+ n + "WITH PR: " + pr);
		}
	
	
	 	// Update Pagerank while distance between graphs is higher than threshold
		// and numIteration < maxIteration
		// but at least once
		do {
		    randomWalker = 0;
		    // iterate over every node
			for (Object o : g.getVertices()) {
			    n = (Node) o;
			    
			    // if n has outgoing edges, spread the weight
			    succ = g.getSuccessors(n);
			   // System.out.println("SUCCESSORS FOR " + n + " ARE " + succ);
			    if (!succ.isEmpty() ) {
					
			        // weight to be spread is current weight divided by outgoing edges
			        pr = (double) n.getPageRank() / succ.size();
			        
					successors = (Object[]) succ.toArray();
					for(int i = 0; i<succ.size(); i++) {
					    m = (Node) successors[i];
					    // add to pageRank new!
					    m.setPageRankNew(m.getPageRankNew() + pr);
					}
				} else {
					randomWalker += (double)n.getPageRank()/numNodes;
					//System.out.println("ADDED TO RW: " + randomWalker);
				}
			}
			
			// distribute randomWalker 
			for (Object o : g.getVertices()) {
                n = (Node) o;
                n.setPageRankNew(   (w * (n.getPageRankNew() + randomWalker)) +((1-w) / numNodes ) );
            }
			
			// get Distance
		    distance = computeDistance(g);
		    //System.out.println("Distance: "+distance);
			
			// update Graph and get sum of Values
		    sumPr = 0;
		    for (Object o : g.getVertices()) {
                n = (Node) o;
                n.setPageRank(n.getPageRankNew());
                n.setPageRankNew(0);
                sumPr += n.getPageRank();
                //System.out.println("NODE AFTER ITERATION: " + numIteration +": " +n);
                
            }
		    
		    // normalize
		    for (Object o : g.getVertices()) {
                n = (Node) o;
                n.setPageRank((double) n.getPageRank() / sumPr);
                
            }
		    
			
			// inkrement iteration
			numIteration += 1;
			
			
		}while( (distance > threshold)  && numIteration < maxIteration );
		
		
	}
	
	@SuppressWarnings("rawtypes")
	private double computeDistance(Graph g) {
	    Node n;
	    double distance = 0;
	    for (Object o : g.getVertices()) {
            n = (Node) o;
            distance += Math.abs( n.getPageRank() - n.getPageRankNew() );
            //System.out.println("DIST: " + distance);
        }
	    return distance;
	}
	
	
}
