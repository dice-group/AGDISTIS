package org.aksw.agdistis.algorithm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;

public class DomainWhiteLister {
	private TripleIndex index;
	HashSet<String> whiteList = new HashSet<String>();

	public DomainWhiteLister(TripleIndex index) throws IOException {
		Properties prop = new Properties();
		InputStream input = DomainWhiteLister.class.getResourceAsStream("/config/agdistis.properties");
		prop.load(input);
		String envWhiteList = System.getenv("AGDISTIS_WHITELIST");
		String file = envWhiteList != null ? envWhiteList : prop.getProperty("whiteList");

		loadWhiteDomains(file);

		this.index = index;
	}

	private void loadWhiteDomains(String file) throws IOException {
		BufferedReader br = new BufferedReader(
				new InputStreamReader(DomainWhiteLister.class.getResourceAsStream(file)));
		while (br.ready()) {
			String line = br.readLine();
			whiteList.add(line);
		}
		br.close();
	}

	public boolean fitsIntoDomain(String candidateURL) {
		List<Triple> tmp = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
		tmp.addAll(index.search(candidateURL, "http://www.wikidata.org/prop/direct/P31",null));
		if (tmp.isEmpty())
			return true;
		for (Triple triple : tmp) {
			if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory"))
				if (whiteList.contains(triple.getObject())) {
					return true;
				}
		}
		return false;
	}
	public boolean fitsPlace(String candidateURL){
		List<Triple> tmp = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
		tmp.addAll(index.search(candidateURL, "http://www.wikidata.org/prop/direct/P31",null));
		if (tmp.isEmpty())
			return true;
		for (Triple triple : tmp) {
			if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory"))
				if (triple.getObject().equals("http://www.wikidata.org/entity/Q22808404")||
						triple.getObject().equals("http://www.wikidata.org/entity/Q153562")||
						triple.getObject().equals("http://www.wikidata.org/entity/Q24354")||
						triple.getObject().equals("http://d-nb.info/standards/elementset/gnd#CorporateBody")){
					return true;
				}
		}
		return false;
	}
	public boolean fitsPlay(String candidateURL){
		List<Triple> tmp = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
		tmp.addAll(index.search(candidateURL, "http://www.wikidata.org/prop/direct/P31",null));
		if (tmp.isEmpty())
			return true;
		for (Triple triple : tmp) {
			if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory"))
				if(triple.getObject().equals("http://d-nb.info/standards/elementset/gnd#MusicalWork") ||
					triple.getObject().equals("http://d-nb.info/standards/elementset/gnd#Work") ||
					triple.getObject().equals("http://purl.org/ontology/bibo/Document") ||
					triple.getObject().equals("https://www.wikidata.org/entity/Q7725634")||
					triple.getObject().equals("http://www.wikidata.org/entity/Q1132324")||
				triple.getObject().equals("http://www.wikidata.org/entity/Q1344")||
				triple.getObject().equals("http://www.wikidata.org/entity/Q25379"))
					return true;

		}
		return false;
	}
}
