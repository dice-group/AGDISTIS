//package org.aksw.agdistis.experiments;
//
//import java.util.Set;
//
//import org.aksw.autosparql.commons.qald.QALD4_EvaluationUtils;
//import org.aksw.hawk.querybuilding.SPARQL;
//import org.aksw.hawk.querybuilding.SPARQLQuery;
//import org.aksw.jena_sparql_api.cache.extra.CacheFrontend;
//import org.aksw.jena_sparql_api.cache.h2.CacheUtilsH2;
//import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory;
//import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
//import org.apache.jena.query.QueryExecution;
//import org.apache.jena.query.ResultSet;
//import org.apache.jena.rdf.model.RDFNode;
//import org.apache.jena.rdf.model.impl.ResourceImpl;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.google.common.collect.Sets;
//
//public class SPARQLLabelFinder {
//	Logger log = LoggerFactory.getLogger(SPARQLLabelFinder.class);
//	public QueryExecutionFactory qef;
//
//	public SPARQL() {
//		try {
//			long timeToLive = 360l * 24l * 60l * 60l * 1000l;
//			// CacheBackend cacheBackend = CacheCoreH2.create("./sparql",
//			// timeToLive, true);
//			// CacheFrontend cacheFrontend = new
//			// CacheFrontendImpl(cacheBackend);
//			CacheFrontend cacheFrontend = CacheUtilsH2.createCacheFrontend("./sparql", true, timeToLive);
//
//			// AKSW SPARQL API call
//			// qef = new
//			// QueryExecutionFactoryHttp("http://192.168.15.69:8890/sparql",
//			// "http://dbpedia.org/");
//			// qef = new
//			// QueryExecutionFactoryHttp("http://localhost:3030/ds/sparql");
//			qef = FluentQueryExecutionFactory.http("http://139.18.2.164:3030/ds/sparql").config().withCache(cacheFrontend).end().create();
//
//			// qef = new
//			// QueryExecutionFactoryHttp("http://localhost:3030/ds/sparql");
//			// qef = new
//			// QueryExecutionFactoryHttp("http://dbpedia.org/sparql","http://dbpedia.org");
//
//			// qef = new
//			// QueryExecutionFactoryHttp("http://live.dbpedia.org/sparql","http://dbpedia.org");
//			// qef = new
//			// QueryExecutionFactoryHttp("http://lod.openlinksw.com/sparql/",
//			// "http://dbpedia.org");
//			// qef = new
//			// QueryExecutionFactoryHttp("http://vtentacle.techfak.uni-bielefeld.de:443/sparql",
//			// "http://dbpedia.org");
//			// --> No reason to be nice
//			// qef = new QueryExecutionFactoryDelay(qef, 2000);
//			// qef = new QueryExecutionFactoryCacheEx(qef, cacheFrontend);
//			// qef = new QueryExecutionFactoryDelay(qef, 150);
//			// qef = new QueryExecutionFactoryPaginated(qef, 10000);
//		} catch (RuntimeException e) {
//			log.error("Could not create SPARQL interface! ", e);
//			System.exit(0);
//		}
//	}
//
//	/**
//	 * using the AKSW library for wrapping Jena API
//	 * 
//	 */
//	public Set<RDFNode> sparql(final String query) {
//		Set<RDFNode> set = Sets.newHashSet();
//		try {
//			QueryExecution qe = qef.createQueryExecution(query);
//			if (qe != null && query.toString() != null) {
//				if (QALD4_EvaluationUtils.isAskType(query)) {
//					set.add(new ResourceImpl(String.valueOf(qe.execAsk())));
//				} else {
//					ResultSet results = qe.execSelect();
//					while (results.hasNext()) {
//						set.add(results.next().get("proj"));
//					}
//				}
//			}
//		} catch (Exception e) {
//			log.error(query.toString(), e);
//		}
//		return set;
//	}
//
//	// TODO Christian: transform to unit test
//	public static void main(final String args[]) {
//		SPARQL sqb = new SPARQL();
//
//		SPARQLQuery query = new SPARQLQuery();
//		query.addConstraint("?proj a <http://dbpedia.org/ontology/Cleric>.");
//		// query.addConstraint("?proj ?p ?const.");
//		// query.addFilter("proj",
//		// Lists.newArrayList("http://dbpedia.org/resource/Pope_John_Paul_I",
//		// "http://dbpedia.org/resource/Pope_John_Paul_II"));
//		// query.addFilter("const",
//		// Lists.newArrayList("http://dbpedia.org/resource/Canale_d'Agordo"));
//		for (String q : query.generateQueries()) {
//			Set<RDFNode> set = sqb.sparql(q);
//			for (RDFNode item : set) {
//				System.out.println(item);
//			}
//		}
//	}
//
//}
