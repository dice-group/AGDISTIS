package org.aksw.agdistis.algorithm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.aksw.agdistis.index.Index;
import org.aksw.agdistis.util.Triple;
//import org.aksw.agdistis.index.indexImpl.TripleIndex;

public class DomainWhiteLister {
	private Index index;
	HashSet<String> whiteList = new HashSet<String>();

	public DomainWhiteLister(Index index) throws IOException {
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
}
