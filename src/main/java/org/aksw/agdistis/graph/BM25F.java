package org.aksw.agdistis.graph;

import edu.uci.ics.jung.graph.Graph;
import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndexContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BM25F{

    /*
     * Implementation of the BM25F.
     * The implementation is depended on the paper: Blanco, Roi & Mika, Peter & Vigna, Sebastiano. (2011).
     * Effective and Efficient Entity Search in RDF Data. 83-97. 10.1007/978-3-642-25073-6_6.
     * */

    private int k=5;           // the value of the K can be from 1 to 10
    private double bs=0.5;           // the value of bs is (0<=b<=1)
    private double avgl = 0;        // THe value of the ls(field length is taken form the paper:)
    private int D = 100;             // Number of documents checked
    private double ni = 0;          // Number of documents the term occurs
    private double inverse_df = 0;         // inverse document frequency
    private double term_frequency = 0;          // term frequency
    private double Bs = 0;          // Document length normalization
    private int ls = 0;         // as mentioned in the paper
    private double Wi = 0;          // Sigmoid function value
    private int lmax = 50;


    public BM25F() throws IOException {
    }



/* To get the score of the documents */
    public void GetDoc(Graph<Node, String> g) throws IOException {
        List<String> var_list = new ArrayList<>();
        List<String> query_terms = new ArrayList<>();
        List<Triple> results;
        int doc_length = 0;
        int size = 0;
        int count = 0;
        String text =new String();
        double freq = 0;
        int term_count =0;
        double tf =0;


        var_list.addAll(g.getEdges());

        size = var_list.size();
        for (int a = 0; a< var_list.size(); a++)
        {
            query_terms.add(var_list.get(a).substring(var_list.get(a).indexOf(":")+1,var_list.get(a).length()));
        }// getting the triples from corpus base using the keywords from query.

        Collections.sort(var_list);
        TripleIndexContext getdoc = new TripleIndexContext();

        for (int i = 0; i < query_terms.size(); i++) { // loop to get the query terms
            results = getdoc.search(query_terms.get(i), null, null);
            //System.out.println("the query term: " + query_terms.get(i));
            ni = results.size();

            for (int j = 0; j < results.size(); j++) {// Getting the documents for each query term
                //System.out.println("The doc against index of query: " + results.get(j));

                //doc.add(results.get(j));

                text = results.get(j).toString();
                //doc_length = text.length();
                if (text.length()>lmax)
                    doc_length =ls;
                else
                    doc_length=text.length();

                //System.out.println("The doc length is  "+doc_length);
                term_count = wordcount(text);
                //System.out.println("The word count is  "+wordcount(text));
                freq = frequency(text,query_terms.get(i));
                tf = freq/term_count;
                avgl = getavgl(text);
                Bs = (1-bs)+bs*(doc_length/avgl);
                term_frequency = tf/Bs;
               // System.out.println("the frequency is "+freq+" and term frequency is "+tf);
                inverse_df = Math.log((D-ni+0.5) / (ni+0.5));
               // System.out.println("the idf "+inverse_df);
                Wi = (term_frequency/k+term_frequency)*inverse_df;
                //System.out.println("the valuse of ni "+ni);
                System.out.println("The Query term is: "+query_terms.get(i)+" and the Score of the "+j+" document is: "+Wi);
               // System.out.println("The score is "+Wi);

            }// End of inner loop


        }  // end of first loop


    } // end of GetDoc function

    public double frequency(String text, String word){
        int count = 0;
        int counts = 0;
        if (word.contains("_")){
            String[] values = word.split("_");
            for (int i = 0 ; i< values.length; i++){
                String each = values[i];

                Pattern p = Pattern.compile(each);
                Matcher m = p.matcher( text );
                while (m.find()) {
                    count++;
                }
                counts += count;
            }
        }
        return counts;
    } // end of the tfidf

    public int wordcount(String string)
    {
        int count=0;

        char ch[]= new char[string.length()];
        for(int i=0;i<string.length();i++)
        {
            ch[i]= string.charAt(i);
            if( ((i>0)&&(ch[i]!=' ')&&(ch[i-1]==' ')) || ((ch[0]!=' ')&&(i==0)) )
                count++;
        }
        return count;
    } // end of wordcount

    public double getavgl(String doc)
    {
        String word =null;
        int counts =0;
        String a[] = doc.split(" ");

        // search for pattern in a
        int count = 0;
        for (int i = 0; i < a.length; i++)
        {
            // if match found increase count
            word = a[i];
            if (word.equals(a[i]))
                count++;
            counts += count;
        }

        return counts/a.length;
    } // end of getavgl

    } // end of class

