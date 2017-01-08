
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.util.NIFParser;
import org.aksw.agdistis.webapp.GetDisambiguation;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.junit.Test;

public class AGDISTISTest {
/*
    @Test
    public void testUmlaute() throws InterruptedException, IOException {
        String osumi = "Masaaki_Ōsumi";
        String osumiURL = "http://dbpedia.org/resource/Masaaki_Ōsumi";
        String japan = "Japan";
        String japanURL = "http://dbpedia.org/resource/Japan";

        HashMap<String, String> correct = new HashMap<String, String>();
        correct.put(osumi, osumiURL);
        correct.put(japan, japanURL);

        String preAnnotatedText = "<entity>" + osumi + "</entity> works in <entity>" + japan + "</entity>.";

        NEDAlgo_HITS agdistis = new NEDAlgo_HITS();
        Document d = GetDisambiguation.textToDocument(preAnnotatedText);
        String type = "agdistis";
        agdistis.run(d, type);
        NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
        HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
        for (NamedEntityInText namedEntity : namedEntities) {
            String disambiguatedURL = agdistis.findResult(namedEntity);
            results.put(namedEntity, disambiguatedURL);
        }

        for (NamedEntityInText namedEntity : results.keySet()) {
            String disambiguatedURL = results.get(namedEntity);
            System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
            assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
        }

    }
*/
    @Test
    public void testMinimalExample() throws InterruptedException, IOException {
        String obama = "Barack Obama";
        String obamaURL = "http://dbpedia.org/resource/Barack_Obama";
        String merkel = "Angela Merkel";
        String merkelURL = "http://dbpedia.org/resource/Angela_Merkel";
        String city = "Berlin";
        String cityURL = "http://dbpedia.org/resource/Berlin";

        HashMap<String, String> correct = new HashMap<String, String>();
        correct.put(obama, obamaURL);
        correct.put(merkel, merkelURL);
        correct.put(city, cityURL);

        String preAnnotatedText = "<entity>" + obama + "</entity> visits <entity>" + merkel + "</entity> in <entity>" + city + "</entity>.";

        NEDAlgo_HITS agdistis = new NEDAlgo_HITS();
        Document d = GetDisambiguation.textToDocument(preAnnotatedText);
        String type = "agdistis";
        agdistis.run(d, null);
        
        
        NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
        HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
        for (NamedEntityInText namedEntity : namedEntities) {
            String disambiguatedURL = namedEntity.getNamedEntityUri();
           results.put(namedEntity, disambiguatedURL);
        }
        for (NamedEntityInText namedEntity : namedEntities) {
            String disambiguatedURL = namedEntity.getNamedEntityUri();
            System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
            assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
        }

    }
/*
    @Test
    public void testNIF() throws InterruptedException, IOException, Exception {

        TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
        TurtleNIFDocumentCreator creator = new TurtleNIFDocumentCreator();

        String preAnnotatedText = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix nif: <http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#> .\n"
                + "@prefix itsrdf: <http://www.w3.org/2005/11/its/rdf#> .\n"
                + "\n"
                + "\n"
                + "<http://example.org/document-1#char=0,52>\n"
                + "        a                     nif:RFC5147String , nif:String , nif:Context ;\n"
                + "        nif:beginIndex        \"0\"^^xsd:nonNegativeInteger ;\n"
                + "        nif:endIndex          \"52\"^^xsd:nonNegativeInteger ;\n"
                + "        nif:isString          \"Today, Barack Obama visited Berlin and met John Doe.\"@en .\n"
                + "\n"
                + "<http://example.org/document-1#char=7,19>\n"
                + "        a                     nif:RFC5147String , nif:String ;\n"
                + "        nif:anchorOf          \"Barack Obama\"@en ;\n"
                + "        nif:beginIndex        \"7\"^^xsd:nonNegativeInteger ;\n"
                + "        nif:endIndex          \"19\"^^xsd:nonNegativeInteger ;\n"
                + "        nif:referenceContext  <http://example.org/document-1#char=0,52> .\n"
                + "\n"
                + "<http://example.org/document-1#char=28,34>\n"
                + "        a                     nif:RFC5147String , nif:String ;\n"
                + "        nif:anchorOf          \"Berlin\"@en ;\n"
                + "        nif:beginIndex        \"28\"^^xsd:nonNegativeInteger ;\n"
                + "        nif:endIndex          \"34\"^^xsd:nonNegativeInteger ;\n"
                + "        nif:referenceContext  <http://example.org/document-1#char=0,52> .\n"
                + "\n"
                + "<http://example.org/document-1#char=43,51>\n"
                + "        a                     nif:RFC5147String , nif:String ;\n"
                + "        nif:anchorOf          \"John Doe\"@en ;\n"
                + "        nif:beginIndex        \"43\"^^xsd:nonNegativeInteger ;\n"
                + "        nif:endIndex          \"51\"^^xsd:nonNegativeInteger ;\n"
                + "        nif:referenceContext  <http://example.org/document-1#char=0,52> .";

        NIFParser nifParser = new NIFParser();

        org.aksw.gerbil.transfer.nif.Document document;
        document = parser.getDocumentFromNIFString(preAnnotatedText);

        //log.debug("Request: " + document.toString());
        document.setMarkings(new ArrayList<Marking>(nifParser.convertNIF(document)));
        //log.debug("Result: " + document.toString());
        String nifDocument = creator.getDocumentAsNIFString(document);
        //System.out.println(nifDocument);
        System.out.println(nifDocument);

    }
*/
}
