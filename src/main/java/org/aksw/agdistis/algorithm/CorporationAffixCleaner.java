package org.aksw.agdistis.algorithm;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;

public class CorporationAffixCleaner {

	HashSet<String> corporationAffixes = new HashSet<String>();

	public CorporationAffixCleaner() throws IOException {
		Properties prop = new Properties();
		InputStream input = new FileInputStream("config/agdistis.properties");
		prop.load(input);
		String file = prop.getProperty("corporationAffixes");

		loadCorporationAffixes(file);
	}

	private void loadCorporationAffixes(String file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		while (br.ready()) {
			String line = br.readLine();
			corporationAffixes.add(line);
		}
		br.close();
	}

	String cleanLabelsfromCorporationIdentifier(String label) {
		for (String corporationAffix : corporationAffixes) {
			if (label.endsWith(corporationAffix)) {
				label = label.substring(0, label.lastIndexOf(corporationAffix));
			}
		}
		return label.trim();
	}

}
