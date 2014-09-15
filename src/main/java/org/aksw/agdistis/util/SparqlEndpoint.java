package org.aksw.agdistis.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

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

public class SparqlEndpoint {
	private Logger log = LoggerFactory.getLogger(SparqlEndpoint.class);
	private RepositoryConnection con;
	private QueryLanguage queryLanguage = QueryLanguage.SPARQL;

	public SparqlEndpoint() throws RepositoryException, IOException  {
			Properties prop = new Properties();
			InputStream input = new FileInputStream("agdistis.properties");
			prop.load(input);

			String endpoint = prop.getProperty("endpoint");
			log.info("The endpoint will be: " + endpoint);

			SPARQLRepository rep = new SPARQLRepository(endpoint);
			rep.initialize();
			con = rep.getConnection();
	}

	/**
	 * returns a matrix of query results where each row represents a result and each column a variable projection of the query
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
		} catch (QueryEvaluationException | RepositoryException | MalformedQueryException | TupleQueryResultHandlerException e) {
			log.error("Cannot execute SPARQL query.", e);
		}
		return result;
	}

	public void close() throws RepositoryException {
		con.close();
	}

}