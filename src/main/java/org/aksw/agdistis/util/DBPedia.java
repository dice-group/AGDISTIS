package org.aksw.agdistis.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.TupleQueryResultBuilder;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unister.semweb.ned.QRToolNED.datatypes.Candidate;

public class DBPedia {
    private Logger log = LoggerFactory.getLogger(DBPedia.class);
    private RepositoryConnection con;
    private QueryLanguage queryLanguage = QueryLanguage.SPARQL;

    public DBPedia(String publicEndpoint) throws RepositoryException {
        SPARQLRepository rep = new SPARQLRepository(publicEndpoint);
        rep.initialize();
        con = rep.getConnection();
    }

    public DBPedia() throws RepositoryException {
        SPARQLRepository rep = new SPARQLRepository("http://bigdata-dbpedia.aws.semweb.unister.de/sparql");
        String userpass = "unister" + ":" + "s3mw3b";
        String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
        HashMap<String, String> authMap = new HashMap<String, String>();
        authMap.put("Authorization", basicAuth);
        rep.setAdditionalHttpHeaders(authMap);

        rep.initialize();
        con = rep.getConnection();

    }

    /**
     * returns a set of all resources connected to the nodeURI via the predicate
     * 
     * @param nodeURI
     * @param predicate
     * @return
     */
    public Set<String> getOutgoingNodes(String nodeURI, String predicate) {
        HashSet<String> set = new HashSet<String>();
        String query = "SELECT ?o WHERE { " +
                "<" + nodeURI + ">  ?p ?o . " +
                "FILTER(isIRI(?o)&&STRSTARTS(STR(?p),\"" + predicate + "\")&&STRSTARTS(STR(?o),\"http://dbpedia.org/resource\"))}";
        ArrayList<ArrayList<String>> result = askDbpedia(query);
        for (ArrayList<String> row : result)
        {
            set.add(row.get(0));
        }
        return set;
    }

    /**
     * returns the one abstract per URL
     * 
     * @param nodeURI
     * @param LanguageAbbreviation
     * @return null if there is no
     */
    public String getAbstractsToURI(String nodeURI, String LanguageAbbreviation) {
        String query = "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/> " +
                "SELECT ?o WHERE { " +
                "<" + nodeURI + "> dbpedia-owl:abstract ?o." +
                "FILTER(langMatches(lang(?o), \"" + LanguageAbbreviation + "\"))}";

        String error = "More than one abstrect for this URI page! " + nodeURI + "\n";
        return getSingleResult(askDbpedia(query), error);
    }

    /**
     * fetches the wikipedia page of a resource uri
     * 
     * @param nodeURI
     * @param LanguageAbbreviation
     * @return string of wikipedia url or null if there is no
     */
    public String getWikipediaUrlOfResource(String nodeURI, String LanguageAbbreviation) {
        String query = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
                + "SELECT ?o WHERE { "
                + "<" + nodeURI + "> foaf:isPrimaryTopicOf ?o. }";
        // + "FILTER(langMatches(lang(?y), \"" + LanguageAbbreviation + "\"))}";

        String error = "More wikipedia pages for one resource than expected! " + nodeURI + "\n";
        return getSingleResult(askDbpedia(query), error);
    }

    public List<Candidate> getWikipediaUrls(List<Candidate> candidates, String langugage, int numberOfCandidatesPerLabel) {
        log.debug("\tStart getting wikiurls");
        ArrayList<Candidate> tmp = new ArrayList<Candidate>();
        int i = 0;
        for (Candidate c : candidates) {
            log.debug("\t\tCandidate label: " + c.getLabel());
            String wikipediaURL = getWikipediaUrlOfResource(c.getUrl(), langugage);
            if (wikipediaURL != null) {
                c.setDescription(wikipediaURL);
                tmp.add(c);
                i++;
            }
            if (i > numberOfCandidatesPerLabel)
            {
                break;
            }
        }
        log.debug("\tFinished getting wikiurls");
        return tmp;
    }

    public List<Candidate> filterRedirects(List<Candidate> candidates) {
        log.debug("\tNumber of candidates before redirecting: " + candidates.size());
        HashSet<Candidate> set = new HashSet<Candidate>();
        for (Candidate c : candidates)
        {

            String redirect = getRedirectOfResource(c.getUrl());
            log.debug("\t\tredirect: " + c.getUrl() + " -> " + redirect);
            if (redirect.equals(c.getUrl()))
            {
                set.add(c);
            } else {
                log.debug("Candidate " + c.getUrl() + " redirected: " + redirect);
                set.add(new Candidate(redirect, c.getLabel(), c.getDescription(), c.getOutgoingEdgeCount()));
            }
        }
        log.debug("\tNumber of candidates after redirecting: " + set.size());
        ArrayList<Candidate> tmp = new ArrayList<Candidate>();
        tmp.addAll(set);
        return tmp;
    }

    public List<Candidate> filterDisambiguations(List<Candidate> candidates) {
        log.debug("\tFiltering disambiguation sites");
        ArrayList<Candidate> cand = new ArrayList<Candidate>();
        for (Candidate c : candidates)
        {
            log.debug("\tCandidate: " + c.getUrl());
            String nodeURI = c.getUrl();
            String query = "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/> "
                    + "SELECT ?o WHERE { "
                    + "<" + nodeURI + ">  dbpedia-owl:wikiPageDisambiguates ?o.}";
            ArrayList<ArrayList<String>> results = askDbpedia(query);
            if (results.size() > 0)
            {
                for (ArrayList<String> row : results)
                {
                    cand.add(new Candidate(row.get(0), c.getLabel(), null));
                }
            } else {
                cand.add(c);
            }
        }
        log.debug("\tFinished filtering disambiguation sites");
        return cand;
    }

    public String getRedirectOfResource(String nodeURI) {
        HashSet<String> recursionPretender = new HashSet<String>();
        while (true) {
            recursionPretender.add(nodeURI);
            String query = "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/> "
                    + "SELECT ?o WHERE { "
                    + "<" + nodeURI + ">  dbpedia-owl:wikiPageRedirects ?o.}";
            String error = "More than one redirect per page!";
            String redirectedNodeUri = getSingleResult(askDbpedia(query), error);
            log.debug("\t\t\tredirect: " + nodeURI + " -> " + redirectedNodeUri);
            if (redirectedNodeUri == null) {
                return nodeURI;
            } else {
                if (recursionPretender.contains(redirectedNodeUri))
                {
                    // already seen
                    return nodeURI;
                } else {
                    nodeURI = redirectedNodeUri;
                }
            }
        }
    }

    private String getSingleResult(ArrayList<ArrayList<String>> result, String error) {
        if (result == null) {
            return null;
        } else if (result.size() == 0) {
            return null;
        } else if (result.get(0) == null) {
            return null;
        } else if (result.size() > 1) {
            log.error(error);
            return null;
        } else if (result.get(0).size() > 1) {
            log.error("Query returns more than one tuple projetion");
            return null;
        } else {
            return result.get(0).get(0);
        }
    }

    public List<Candidate> setOutgoingEdgeCountPerResource(List<Candidate> candidates, String predicate) {
        log.debug("\tStart set outgoing edges");
        for (Candidate c : candidates)
        {
            log.debug("\tCandidate: " + c.getUrl());
            String query = "SELECT distinct ?o WHERE { " +
                    "<" + c.getUrl() + "> ?p ?o. " +
                    "FILTER(STRSTARTS(STR(?p),\"" + predicate + "\"))}";

            ArrayList<ArrayList<String>> results = askDbpedia(query);

            if (results != null) {
                c.setOutgoingEdgeCount(results.size());
            }
        }
        log.debug("\tFinished setting outgoing edges");
        return candidates;
    }

    public Set<String> getOutgoingNodes(String nodeURI) {
        String query = "SELECT ?o WHERE { " +
                "<" + nodeURI + "> ?p ?o. }";
        log.debug("Search outgoing nodes for: " + nodeURI);
        ArrayList<ArrayList<String>> results = askDbpedia(query);
        Set<String> set = new HashSet<String>();
        for (ArrayList<String> row : results)
        {
            log.debug("\tOutgoing Node: " + row.get(0));
            set.add(row.get(0));
        }
        return set;
    }

    /**
     * returns a matrix of query results where each row represents a result and each column a variable projection of the
     * query
     * 
     * @param query
     * @return matrix of results, null if an exception occured
     */
    public ArrayList<ArrayList<String>> askDbpedia(String query) {
        ArrayList<ArrayList<String>> result = null;
        try {
            result = new ArrayList<ArrayList<String>>();
            TupleQuery tupleQuery = con.prepareTupleQuery(queryLanguage, query);
            TupleQueryResultBuilder tQRW = new TupleQueryResultBuilder();
            tupleQuery.evaluate(tQRW);
            TupleQueryResult tQR = tQRW.getQueryResult();
            while (tQR.hasNext()) {
                ArrayList<String> tmp = new ArrayList<String>();
                BindingSet st = tQR.next();
                Iterator<Binding> stIterator = st.iterator();
                while (stIterator.hasNext()) {
                    // watch out! the binding has to ensure the order
                    Binding b = stIterator.next();
                    tmp.add(b.getValue().stringValue());
                }
                result.add(tmp);
            }
        } catch (QueryEvaluationException e) {
            log.error(query);
        } catch (TupleQueryResultHandlerException e) {
            log.error(e.getLocalizedMessage());
        } catch (RepositoryException e) {
            log.error(e.getLocalizedMessage());
        } catch (MalformedQueryException e) {
            log.error(e.getLocalizedMessage());
        }
        return result;
    }

    public void close() throws RepositoryException {
        con.close();
    }

    public HashSet<String> getOntologyPredicates() {
        HashSet<String> set = new HashSet<String>();
        String query = "select distinct ?p where {?s ?p ?o. " +
                "       FILTER regex(str(?p), \"^http://dbpedia.org/ontology\")}";
        ArrayList<ArrayList<String>> result = askDbpedia(query);
        for (ArrayList<String> row : result)
        {
            set.add(row.get(0));
        }
        return set;
    }

}