package org.aksw.agdistis.algorithm;


public class CorporationAffixCleaner {

	public CorporationAffixCleaner() {

	}

	String cleanLabelsfromCorporationIdentifier(String label) {
		// TODO put that in agdistis.properties as well
		if (label.endsWith("corp")) {
			label = label.substring(0, label.lastIndexOf("corp"));
		} else if (label.endsWith("Corp")) {
			label = label.substring(0, label.lastIndexOf("Corp"));
		} else if (label.endsWith("ltd")) {
			label = label.substring(0, label.lastIndexOf("ltd"));
		} else if (label.endsWith("Ltd")) {
			label = label.substring(0, label.lastIndexOf("Ltd"));
		} else if (label.endsWith("inc")) {
			label = label.substring(0, label.lastIndexOf("inc"));
		} else if (label.endsWith("Inc")) {
			label = label.substring(0, label.lastIndexOf("Inc"));
		} else if (label.endsWith("Co")) {
			label = label.substring(0, label.lastIndexOf("Co"));
		} else if (label.endsWith("co")) {
			label = label.substring(0, label.lastIndexOf("co"));
		}

		return label.trim();
	}
	
	
}
