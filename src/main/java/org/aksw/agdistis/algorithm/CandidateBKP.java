
package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;
import org.apache.lucene.search.spell.NGramDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.aksw.agdistis.util.PreprocessingNLP;
import org.aksw.agdistis.util.Stemming;
import org.aksw.agdistis.util.TripleIndexAcronym;
import org.aksw.agdistis.util.TripleIndexContext;
import org.aksw.agdistis.util.TripleIndexCounts;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.lucene.search.spell.LevensteinDistance;

public class CandidateBKP {

    private static Logger log = LoggerFactory.getLogger(CandidateUtil.class);
    private String nodeType;
    private TripleIndex index;
    private TripleIndexAcronym index2;
    private TripleIndexCounts index3;
    private TripleIndexContext index4;
    private LevensteinDistance levenstein;
    private NGramDistance nGramDistance;
    private JaroWinklerDistance jaroWinklerDistance;
    private CorporationAffixCleaner corporationAffixCleaner;
    private DomainWhiteLister domainWhiteLister;

    public CandidateBKP() throws IOException {
        Properties prop = new Properties();
        InputStream input = CandidateUtil.class.getResourceAsStream("/config/agdistis.properties");
        prop.load(input);

        this.nodeType = prop.getProperty("nodeType");
        this.nGramDistance = new NGramDistance(Integer.valueOf(prop.getProperty("ngramDistance")));
        this.jaroWinklerDistance = new JaroWinklerDistance();
        this.levenstein = new LevensteinDistance();
        this.index = new TripleIndex();
        this.index4 = new TripleIndexContext();
        this.index3 = new TripleIndexCounts();
        this.index2 = new TripleIndexAcronym();
        this.corporationAffixCleaner = new CorporationAffixCleaner();
        this.domainWhiteLister = new DomainWhiteLister(index);
    }

    public void insertCandidatesIntoText(DirectedSparseGraph<Node, String> graph, Document document, double threshholdTrigram, Boolean heuristicExpansionOn) throws IOException {
        NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
        String text = document.DocumentText().getText();        
        HashMap<String, Node> nodes = new HashMap<String, Node>();

        // used for heuristic label expansion start with longest Named Entities
        Collections.sort(namedEntities.getNamedEntities(), new NamedEntityLengthComparator());
        Collections.reverse(namedEntities.getNamedEntities());
          String entities = "";
        for (NamedEntityInText namedEntity : namedEntities) {
            entities = " ".concat(namedEntity.getLabel());
        }
        log.info("entities" + entities);
        HashSet<String> heuristicExpansion = new HashSet<String>();
        for (NamedEntityInText entity : namedEntities) {
            String label = text.substring(entity.getStartPos(), entity.getEndPos());

            log.info("\tLabel: " + label);
            long start = System.currentTimeMillis();

            if (heuristicExpansionOn) {
                label = heuristicExpansion(heuristicExpansion, label);
            }
            checkLabelCandidates(graph, threshholdTrigram, nodes, entity, label, false);

            log.info("\tGraph size: " + graph.getVertexCount() + " took: " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    private String heuristicExpansion(HashSet<String> heuristicExpansion, String label) {
        String tmp = label;
        boolean expansion = false;
        for (String key : heuristicExpansion) {
            if (key.contains(label)) {
                // take the shortest possible expansion
                if (tmp.length() > key.length() && tmp != label) {
                    tmp = key;
                    expansion = true;
                    log.debug("Heuristic expansion: " + label + "-->" + key);
                }
                if (tmp.length() < key.length() && tmp == label) {
                    tmp = key;
                    expansion = true;
                    log.debug("Heuristic expansion: " + label + "-->" + key);
                }
            }
        }
        label = tmp;
        if (!expansion) {
            heuristicExpansion.add(label);
        }
        return label;
    }

    public void addNodeToGraph(DirectedSparseGraph<Node, String> graph, HashMap<String, Node> nodes, NamedEntityInText entity, Triple c, String candidateURL) throws IOException {
        Node currentNode = new Node(candidateURL, 0, 0);
        log.debug("CandidateURL: " + candidateURL);
        // candidates are connected to a specific label in the text via their start position
        if (!graph.addVertex(currentNode)) {
            int st = entity.getStartPos();
            if (nodes.get(candidateURL) != null) {
                nodes.get(candidateURL).addId(st);
            } else {
                log.error("This vertex couldn't be added because of an bug in Jung: " + candidateURL);
            }
        } else {
            currentNode.addId(entity.getStartPos());
            nodes.put(candidateURL, currentNode);
        }
    }

    private void checkLabelCandidates(DirectedSparseGraph<Node, String> graph, double threshholdTrigram, HashMap<String, Node> nodes, NamedEntityInText entity, String label, boolean searchInSurfaceForms) throws IOException {
        ArrayList<String> finalCandidates = new ArrayList<String>();
        ArrayList<String> finalCandidatesSorted = new ArrayList<String>();

        List<Triple> allCandidates = new ArrayList<Triple>();
        List<Triple> allCandidatesTemp = new ArrayList<Triple>();
        List<Triple> tripleCandidates = new ArrayList<Triple>();

        List<Triple> candidates = new ArrayList<Triple>();
        List<Triple> candidatesTemp = new ArrayList<Triple>();

        List<Triple> candidatesSFA = new ArrayList<Triple>();

        List<Triple> acronymCandidatesTemp = new ArrayList<Triple>();
        List<Triple> acronymCandidatesTemp2 = new ArrayList<Triple>();
        List<Triple> acronymCandidates = new ArrayList<Triple>();

        List<Triple> candidatesRD = new ArrayList<Triple>();
        List<Triple> candidatesTempRD = new ArrayList<Triple>();
        List<Triple> candidatesFilteredRD = new ArrayList<Triple>();

        List<Triple> candidatesContext = new ArrayList<Triple>();
        List<Triple> candidatesContextbyLabel = new ArrayList<Triple>();

        List<Triple> candidatesScore = new ArrayList<Triple>();
        List<Triple> finalSetCandidates = new ArrayList<Triple>();
        List<Triple> tempList = new ArrayList<Triple>();

        List<Triple> candidatesByURL = new ArrayList<Triple>();

        PreprocessingNLP nlp = new PreprocessingNLP();
//Label treatment 
        label = corporationAffixCleaner.cleanLabelsfromCorporationIdentifier(label);
        log.info("Label:" + label);

        label = nlp.Preprocessing(label);

//label treatment finished -> 
//searchByAcronym
        if (label.equals(label.toUpperCase()) || label.length() <= 3) {
            acronymCandidatesTemp = searchbyAcronym(label, searchInSurfaceForms, entity.getType());
            for (Triple triple : acronymCandidatesTemp) {
                acronymCandidatesTemp2 = searchAcronymByLabel(triple.getSubject(), searchInSurfaceForms, entity.getType());
                for (Triple triple2 : acronymCandidatesTemp2) {
                    if (nGramDistance.getDistance(triple.getSubject(), triple2.getObject()) > 0.82) {
                        acronymCandidates.add(triple2);
                    }
                }
            }
        }
        log.info("\t\tnumber of candidates by acronym: " + acronymCandidates.size());
//searchByAcronymFinished

        //SURFACE FORMS BY RDFSLABEL STARTS HERE
        candidatesTemp = searchCandidatesByLabel(label, searchInSurfaceForms, entity.getType());

        log.info("\t\tnumber of candidates by label: " + candidatesTemp.size());
        for (Triple c : candidatesTemp) {
            if (nGramDistance.getDistance(c.getObject(), label) > 0.82) {
                c = redirectT(c);
                candidates.add(c);
            }
        }

        if (candidates.isEmpty()) {
            if (label.endsWith("'s")) {
                // removing plural s
                label = label.substring(0, label.lastIndexOf("'s"));
            } else if (label.endsWith("s")) {
                // removing genitiv s
                label = label.substring(0, label.lastIndexOf("s"));
            }

            candidatesTemp = searchCandidatesByLabel(label, searchInSurfaceForms, entity.getType());

            log.info("\t\tnumber of candidates by label removing plural and genitive: " + candidatesTemp.size());
            for (Triple c : candidatesTemp) {
                if (nGramDistance.getDistance(c.getObject(), label) > 0.82) {
                    c = redirectT(c);
                    candidates.add(c);
                }
            }
        }

        if (candidates.isEmpty()) {
            Stemming stemmer = new Stemming();
            String temp = stemmer.stemming(label);
            candidatesTemp = searchCandidatesByLabel(temp, searchInSurfaceForms, entity.getType());
            for (Triple c : candidatesTemp) {
                if (nGramDistance.getDistance(c.getObject(), temp) > 0.82) {
                    c = redirectT(c);
                    candidates.add(c);
                }
            }
            log.info("\t\tnumber of all candidates by stemming: " + candidates.size());
        }

        List<Triple> candidatesRdfsLabel = new ArrayList<Triple>();
        for (Triple triple : candidates) {
            candidatesRdfsLabel.add(new Triple(triple.getSubject(), triple.getPredicate(), triple.getObject()));
        }

        allCandidates.addAll(candidatesRdfsLabel);

        for (Triple c : candidates) {

            c.setPredicate(c.getObject());
            c.setObject("1000000");
        }
        log.info("\t\tnumber of candidates filtered by label: " + candidates.size());

        //SURFACE FORMS BY REDIRECTS AND DISAMBIGUATION START HERE
        candidatesTempRD = searchCandidatesByLabelRD(label, searchInSurfaceForms, entity.getType());
        log.info("\t\tnumber of candidates by redirects and disambiguation: " + candidatesTempRD.size());
        for (Triple c : candidatesTempRD) {

            if (nGramDistance.getDistance(c.getObject(), label) < 0.7) {
                continue;
            }
            if (isDisambiguationResource(c.getSubject())) {
                continue;
            }
            c = redirectT(c);
            candidatesRD.add(c);

        }
        candidatesRD.addAll(acronymCandidates); // inserting list of possible URIs from acronym's search with candidatesRD in order to sort by URIcount. 

        List<Triple> candidatesRDLabel = new ArrayList<Triple>();
        for (Triple triple : candidatesRD) {
            candidatesRDLabel.add(new Triple(triple.getSubject(), triple.getPredicate(), triple.getObject()));
        }
        allCandidates.addAll(candidatesRDLabel);

        for (Triple c : candidatesRD) {

            String uri = c.getSubject().replace("http://dbpedia.org/resource/", "");
            candidatesScore = searchCandidatesByScore(uri);
            c.setPredicate(c.getObject());
            if (candidatesScore.isEmpty()) {
                c.setObject("1");
            } else {
                c.setObject(candidatesScore.get(0).getObject());
            }
        }

        log.info("\t\tnumber of candidates filtered by redirects and disambiguation: " + candidatesRD.size());

        //SURFACE FORMS BY ANCHORS STARTS HERE
        candidatesSFA = searchCandidatesByLabelCounts(label, searchInSurfaceForms, entity.getType());

        List<Triple> candidatesSFAlabel = new ArrayList<Triple>();
        for (Triple triple : candidatesSFA) {
            candidatesSFAlabel.add(new Triple(triple.getSubject(), triple.getPredicate(), triple.getObject()));
        }

        for (Triple triple : candidatesSFAlabel) {
            triple.setObject(triple.getPredicate());
            triple.setPredicate("http://www.w3.org/2004/02/skos/core#altLabel");
        }
        allCandidates.addAll(candidatesSFAlabel);

        int count = 0;
        if (!candidatesRD.isEmpty() && !candidatesSFA.isEmpty()) {
            for (Triple c1 : candidatesRD) {
                for (Triple c2 : candidatesSFA) {
                    if (c1.getSubject().equals(c2.getSubject()) && c1.getPredicate().equals(c2.getPredicate())) {
                        //log.info("equal");
                        break;
                    } else if (!(c1.getSubject().equals(c2.getSubject())) && !(c1.getPredicate().equals(c2.getPredicate())) && count == 0) {
                        candidatesFilteredRD.add(c1);
                        count++;
                        break;
                    } else if (!(c1.getSubject().equals(c2.getSubject())) && !(c1.getPredicate().equals(c2.getPredicate())) && count > 1) {
                    }
                }
                count = 0;
            }
        } else {
            candidatesFilteredRD = candidatesRD;
        }

        log.info("\t\tnumber of candidates cleanead by redirects and disambiguation: " + candidatesFilteredRD.size());
        log.info("\t\tCandidates by SF" + candidatesSFA.size());
        tempList.addAll(candidates); //inserting the candidates by label with 0 frequency
        tempList.addAll(candidatesFilteredRD); //inserting the candidates by disambiguation and redirects with 0 frequency
        tempList.addAll(candidatesSFA); // inserting candidates by SF with frequency from anchors.ttl 

        if (tempList.isEmpty()) {

            candidatesContext = searchCandidatesByContext(label);

            List<Triple> candidatesContextLabel = new ArrayList<Triple>();

            if (candidatesContext != null) {
                for (Triple triple : candidatesContext) {
                    String url = "http://dbpedia.org/resource/" + triple.getPredicate();
                    candidatesContextbyLabel.addAll(searchCandidatesByUrl(url, searchInSurfaceForms));
                }
            }

            for (Triple triple : candidatesContextbyLabel) {
                candidatesContextLabel.add(new Triple(triple.getSubject(), triple.getPredicate(), triple.getObject()));
            }
            allCandidates.addAll(candidatesContextLabel);
            log.info("\t\tnumber of candidates by context: " + candidatesContextbyLabel.size());
            count = 0;
            for (Triple tripleContext : candidatesContextbyLabel) {
                tripleContext.setPredicate(tripleContext.getObject());
                String uri = tripleContext.getSubject().replace("http://dbpedia.org/resource/", "");
                candidatesScore = searchCandidatesByScore(uri);
                if (candidatesScore.isEmpty()) {
                    tripleContext.setObject("1");
                } else {
                    tripleContext.setObject(candidatesScore.get(0).getObject());
                    //c.setObject("999999");
                }

                if (candidates.contains(tripleContext.getSubject())) {
                    continue;
                } else {
                    candidates.add(tripleContext);
                }
            }
            tempList.addAll(candidates);
        }

        Collections.sort(tempList);

        List<Triple> resultTemp = new ArrayList<Triple>();
        for (Triple triple : tempList) {
            resultTemp.add(triple);
        }
        Collections.sort(resultTemp);

        count = 0;

        Triple tripleTemp = new Triple("vazio", "vazio", "vazio");
        for (Triple triple : tempList) {

            if (triple.getSubject().equals(tripleTemp.getSubject()) && count == 1) {
                continue;
            } else {
                count = 0;
            }
            for (Triple triple2 : resultTemp) {
                if (triple.getSubject().equals(triple2.getSubject()) && count == 0) {
                    allCandidatesTemp.add(triple);
                    count = 1;
                }
            }
            tripleTemp = triple;
        }

        log.info("\t\tAll candidates temporary size" + allCandidatesTemp.size());

        log.info("\t\tAll candidates triples" + allCandidates.size());

        Collections.sort(allCandidatesTemp);

        log.info("\t\tAll candidates temporary size reordered" + allCandidatesTemp.size());

        if (allCandidatesTemp.size() < 1) {
            for (Triple triple : allCandidatesTemp.subList(0, allCandidatesTemp.size())) {
                for (Triple triple2 : allCandidates) {
                    if (triple.getSubject().equals(triple2.getSubject()) && triple.getPredicate().equals(triple2.getObject())) {
                        tripleCandidates.add(triple2);
                        continue;
                    }

                }
            }
        } else {

            for (Triple triple : allCandidatesTemp.subList(0,1)) {
                for (Triple triple2 : allCandidates) {
                    if (triple.getSubject().equals(triple2.getSubject()) && triple.getPredicate().equals(triple2.getObject())) {
                        tripleCandidates.add(triple2);
                        continue;
                    }

                }
            }
        }

        log.info("\t\tnumber of tripleCandidates: " + tripleCandidates.size());

        for (Triple triple : tripleCandidates) {
                addNodeToGraph(graph, nodes, entity, triple, triple.getSubject());
        }
    }
   
    
    
    public ArrayList<Triple> searchCandidatesByLabel(String label, boolean searchInSurfaceFormsToo, String type) {
        ArrayList<Triple> tmp = new ArrayList<Triple>();
        tmp.addAll(index.search(null, "http://www.w3.org/2000/01/rdf-schema#label", label, 1000));

        return tmp;
    }

    public ArrayList<Triple> searchCandidatesByLabelRD(String label, boolean searchInSurfaceFormsToo, String type) {
        ArrayList<Triple> tmp = new ArrayList<Triple>();
        tmp.addAll(index.search(null, "http://www.w3.org/2004/02/skos/core#altLabel2", label, 1000));
        return tmp;
    }

    public ArrayList<Triple> searchCandidatesByLabelCounts(String label, boolean searchInSurfaceFormsToo, String type) {
        ArrayList<Triple> tmp = new ArrayList<Triple>();
        tmp.addAll(index3.searchC(null, "http://www.w3.org/2004/02/skos/core#altLabel", label, 1000));
        return tmp;
    }

    public ArrayList<Triple> searchbyAcronym(String label, boolean searchInSurfaceFormsToo, String type) {
        ArrayList<Triple> tmp = new ArrayList<Triple>();
        tmp.addAll(index2.search(null, "http://dbpedia.org/property/acronym", label, 100));
        return tmp;
    }

    public ArrayList<Triple> searchAcronymByLabel(String label, boolean searchInSurfaceFormsToo, String type) {
        ArrayList<Triple> tmp = new ArrayList<Triple>();
        tmp.addAll(index.search(null, null, label, 100));
        return tmp;
    }

    ArrayList<Triple> searchCandidatesByContext(String label) {
        ArrayList<Triple> tmp = new ArrayList<Triple>();
        tmp.addAll(index4.search(label, null, null, 1000));

        return tmp;
    }

    ArrayList<Triple> searchCandidatesByScore(String label) {
        ArrayList<Triple> tmp = new ArrayList<Triple>();
        tmp.addAll(index4.search(null, label, null));

        return tmp;
    }

    ArrayList<Triple> searchCandidatesByUriCount(String label) {
        ArrayList<Triple> tmp = new ArrayList<Triple>();
        tmp.addAll(index.search(label, "uriCounts", null));

        return tmp;
    }

    ArrayList<Triple> searchCandidatesByUrl(String url, boolean searchInSurfaceFormsToo) {
        ArrayList<Triple> tmp = new ArrayList<Triple>();
        tmp.addAll(index.search(url, "http://www.w3.org/2000/01/rdf-schema#label", null, 100));
        tmp.addAll(index.search(url, "http://www.w3.org/2004/02/skos/core#altLabel", null, 100));
        tmp.addAll(index.search(url, "http://www.w3.org/2004/02/skos/core#altLabel2", null, 100));

        return tmp;
    }
    
        ArrayList<Triple> searchFinalCandidatesByUrl(String url, boolean searchInSurfaceFormsToo) {
        ArrayList<Triple> tmp = new ArrayList<Triple>();
        tmp.addAll(index.search(url, "http://www.w3.org/2000/01/rdf-schema#label", null, 100));
        return tmp;
    }

    private boolean isDisambiguationResource(String candidateURL) {
        List<Triple> tmp = index.search(candidateURL, "http://dbpedia.org/ontology/wikiPageDisambiguates", null);
        if (tmp.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    private String redirect(String candidateURL) {
        if (candidateURL == null) {
            return candidateURL;
        }
        List<Triple> redirect = index.search(candidateURL, "http://dbpedia.org/ontology/wikiPageRedirects", null);
        if (redirect.size() == 1) {
            return redirect.get(0).getObject();
        } else if (redirect.size() > 1) {
            log.error("Several redirects detected for :" + candidateURL);
            return candidateURL;
        } else {
            return candidateURL;
        }
    }

    private Triple redirectT(Triple candidate) {
        String candidateURL = candidate.getSubject();
        if (candidateURL == null) {
            return candidate;
        }
        List<Triple> redirect = index.search(candidateURL, "http://dbpedia.org/ontology/wikiPageRedirects", null);
        if (redirect.size() == 1) {
            candidate.setSubject(redirect.get(0).getObject());
            return candidate;
        } else if (redirect.size() > 1) {
            log.error("Several redirects detected for :" + candidateURL);
            return candidate;
        } else {
            return candidate;
        }
    }

    public void close() throws IOException {
        index.close();
    }

    public TripleIndex getIndex() {
        return index;
    }

}
