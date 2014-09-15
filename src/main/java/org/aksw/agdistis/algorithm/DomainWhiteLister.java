package org.aksw.agdistis.algorithm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;

public class DomainWhiteLister {
	private String nodeType;
	private TripleIndex index;
	public DomainWhiteLister(TripleIndex index) throws IOException {
		Properties prop = new Properties();
		InputStream input = new FileInputStream("agdistis.properties");
		prop.load(input);

		this.nodeType = prop.getProperty("nodeType");
		this.index = index;
	}
	public boolean fitsIntoDomain(String candidateURL) {
		//TODO put that in properties file as well
		HashSet<String> whiteList = new HashSet<String>();
		if (nodeType.contains("http://dbpedia.org")) {
			whiteList.add("http://dbpedia.org/ontology/Place");
			whiteList.add("http://dbpedia.org/ontology/Person");
			whiteList.add("http://dbpedia.org/ontology/Organisation");
			whiteList.add("http://dbpedia.org/class/yago/YagoGeoEntity");
			whiteList.add("http://xmlns.com/foaf/0.1/Person");
			whiteList.add("http://dbpedia.org/ontology/WrittenWork");
		} else {
			whiteList.add("http://yago-knowledge.org/resource/yagoGeoEntity");
			whiteList.add("http://yago-knowledge.org/resource/yagoLegalActor");
			whiteList.add("http://yago-knowledge.org/resource/wordnet_exchange_111409538");
		}

		List<Triple> tmp = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
		if (tmp.isEmpty())
			return true;
		for (Triple triple : tmp) {
			if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory"))
				// log.debug("\ttype: " + triple.getObject());
				if (whiteList.contains(triple.getObject())) {
					return true;
				}
		}
		return false;
	}
}
