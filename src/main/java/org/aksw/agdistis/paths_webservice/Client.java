package org.aksw.agdistis.paths_webservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.math3.linear.OpenMapRealMatrix;

public class Client {
	    public OpenMapRealMatrix request(String[] uris) throws IOException, ClassNotFoundException {
	    	OpenMapRealMatrix result=null;
	    	try {
	    		for (String s : uris){
	    			System.out.println(s);
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
	    	return result;
	    }
}