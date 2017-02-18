package org.aksw.agdistis.experiments;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.aksw.jena_sparql_api.cache.extra.CacheFrontend;
import org.aksw.jena_sparql_api.cache.h2.CacheUtilsH2;
import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class SPARQLLabelFinder {
	Logger log = LoggerFactory.getLogger(SPARQLLabelFinder.class);
	public QueryExecutionFactory qef;

	public SPARQLLabelFinder() {
		try {
			long timeToLive = 360l * 24l * 60l * 60l * 1000l;
			CacheFrontend cacheFrontend = CacheUtilsH2.createCacheFrontend("./sparql", true, timeToLive);

			// qef =
			// FluentQueryExecutionFactory.http("http://139.18.2.164:3030/ds/sparql").config().withCache(cacheFrontend).end().create();
			qef = FluentQueryExecutionFactory.http("http://dbpedia.org/sparql").config().withCache(cacheFrontend).end()
					.create();

		} catch (RuntimeException e) {
			log.error("Could not create SPARQL interface! ", e);
			System.exit(0);
		}
	}

	/**
	 * using the AKSW library for wrapping Jena API
	 * 
	 */
	public Set<RDFNode> sparql(final String query) {
		Set<RDFNode> set = Sets.newHashSet();
		try {
			QueryExecution qe = qef.createQueryExecution(query);
			if (qe != null && query.toString() != null) {
				ResultSet results = qe.execSelect();
				while (results.hasNext()) {
					set.add(results.next().get("proj"));
				}
			}
		} catch (Exception e) {
			log.error(query.toString(), e);
		}
		return set;
	}

	public static void main(final String args[]) {

		String fileName = "./src/main/resources/exportedURIs.txt.uniq";

		double match = 0;
		double nomatch = 0;

		List<String[]> list;

		try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {

			// br returns as stream and convert it into a List
			list = br.lines().map((line) -> {
				String[] p = line.split("\t");
				return p;
			}).collect(Collectors.toList());

			// for (String[] p : list) {
			// if (p[0].startsWith("http://dbpedia.org/resource")) {
			// SPARQLLabelFinder sqb = new SPARQLLabelFinder();
			// String label = p[1].replaceAll("\"", "\\\"");
			// String query = "PREFIX
			// rdfs:<http://www.w3.org/2000/01/rdf-schema#>" + "SELECT ?proj
			// {?proj rdfs:label \"" + label + "\"@en}";
			// Set<RDFNode> set = sqb.sparql(query);
			// boolean matchFound = false;
			// for (RDFNode item : set) {
			// String uri = item.asResource().getURI().toString();
			// if (uri.startsWith("http://dbpedia.org/resource")) {
			// System.out.println(p[0] + " => " + uri);
			// if (uri.equals(p[0])) {
			// matchFound = true;
			// }
			// }
			// }
			// if (matchFound) {
			// match++;
			// } else {
			// nomatch++;
			// }
			// }
			// }
			// System.out.println("Accuracy rdfs:label: " + match / (match +
			// nomatch));
			// Accuracy rdfs:label: 0.2834343896579339

			for (String[] p : list) {
				if (p[0].startsWith("http://dbpedia.org/resource")) {
					SPARQLLabelFinder sqb = new SPARQLLabelFinder();
					String label = p[1].replaceAll("\"", "\\\"");
					String query = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>" + "SELECT ?proj {?proj ?p \""
							+ label + "\"@en}";
					Set<RDFNode> set = sqb.sparql(query);
					boolean matchFound = false;
					for (RDFNode item : set) {
						String uri = item.asResource().getURI().toString();
						if (uri.startsWith("http://dbpedia.org/resource")) {
							System.out.println(p[0] + " => " + uri);
							if (uri.equals(p[0])) {
								matchFound = true;
							}
						}
					}
					if (matchFound) {
						match++;
					} else {
						nomatch++;
					}
				}
			}
			System.out.println("Accuracy rdfs:label: " + match / (match + nomatch));
			// Accuracy rdfs:label: 0.3457842352133623
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
