package org.aksw.agdistis.paths_webservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;

import org.aksw.agdistis.graph.Node;
import org.apache.commons.math3.linear.OpenMapRealMatrix;

import com.google.common.collect.Lists;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

import org.aksw.agdistis.graph.Node;

public class Client {
	    public DirectedSparseGraph<Node, String> request(DirectedSparseGraph<Node, String> graph) throws IOException, ClassNotFoundException {
	    	String[] uris = listNodes(graph);
	    	
	    	OpenMapRealMatrix result=null;
	    	try {
	    		for (int i=0; i< uris.length; i++){
				String[] tmp=uris[i].split("resource/");
				uris[i]=tmp[0]+"resource/"+URLEncoder.encode(tmp[1],"UTF-8");
				System.out.println(uris[i]);
	    		}
	        	Socket socket = new Socket("localhost", 1112);
	        	
	        	OutputStream os = socket.getOutputStream();
	            ObjectOutputStream oos = new ObjectOutputStream(os);
	            long startTime = System.currentTimeMillis();
	            System.out.println("Sending ...");
	            oos.writeObject(uris);
	            
	            System.out.println("Sent!");	
	            
	            InputStream is = socket.getInputStream();
	        	ObjectInputStream ois = new ObjectInputStream(is);
	        	result = (OpenMapRealMatrix)ois.readObject();
	        	
	        	long estimatedTime = System.currentTimeMillis() - startTime;
	    		System.out.println(estimatedTime);
	        	
	    		//Print result
	        	
	    		if (result!=null){
	        		for (int i1=0; i1<uris.length; i1++){
	        			for (int j=0; j<uris.length; j++){
	        				//if (to.)
	        				System.out.print(result.getEntry(i1, j)+" ");
	        			}
	        			System.out.print("\n");
	        		}
	        	}
	        	
	    		
	    		Map<String, Node> m = mapNodes(graph);
	        	for (int i=0; i<uris.length; i++){
	        		for (int j=0; j<uris.length; j++){
	        			if (result.getEntry(i, j)!=0){
	        				//Create a new edge and add weight to the edge
	        				graph.addEdge(graph.getEdgeCount() + ";" + result.getEntry(i, j), m.get(uris[i]), m.get(uris[j]));
	        			}
	        		}
	        		System.out.print("\n");
	        	}
	        	
	    		
	        	oos.close();
	        	os.close();
	        	ois.close();
	        	is.close();
	        	socket.close();
	        } catch (UnknownHostException e) {
	            System.err.println("Don't know about host ???"+ e);
	            System.exit(1);
	        } catch (IllegalArgumentException e) {
	        	System.out.println(e.getMessage());
	        } catch (IOException e) {
	        	System.out.println(e);
	        	System.out.println(e.getMessage());
	            System.err.println("Couldn't get I/O for the connection to ???");
	            System.exit(1);
	        }
	    	return graph;
	    }
	   
	    private String[] listNodes(DirectedSparseGraph<Node, String> graph) {
			List<String> candidates = Lists.newArrayList();
			for (Node n : graph.getVertices()) {
				candidates.add(n.getCandidateURI());
			}
			return (String[]) candidates.toArray(new String[candidates.size()]);
		}
	    
	    private Map<String, Node> mapNodes(DirectedSparseGraph<Node, String> graph) {
	    	Map<String, Node> r = new HashMap<String,Node>();
			for (Node n : graph.getVertices()) {
				r.put(n.getCandidateURI(), n);
			}
			return r;
		}

}
