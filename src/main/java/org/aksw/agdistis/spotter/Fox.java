package org.aksw.hawk.nlp.spotter;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.autosparql.commons.qald.Question;
import org.aksw.autosparql.commons.qald.uri.Entity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

public class Fox extends ASpotter {
	static Logger log = LoggerFactory.getLogger(Fox.class);

	private String requestURL = "http://139.18.2.164:4444/api";
	private String outputFormat = "N3";
	private String taskType = "NER";
	private String inputType = "text";

	private String doTASK(String inputText) throws MalformedURLException, IOException, ProtocolException {

		String urlParameters = "type=" + inputType;
		urlParameters += "&task=" + taskType;
		urlParameters += "&output=" + outputFormat;
		urlParameters += "&input=" + URLEncoder.encode(inputText, "UTF-8");

		return requestPOST(urlParameters, requestURL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.hawk.nlp.NERD_module#getEntities(java.lang.String)
	 */
	@Override
	public Map<String, List<Entity>> getEntities(String question) {
		HashMap<String, List<Entity>> tmp = new HashMap<String, List<Entity>>();
		try {
			String foxJSONOutput = doTASK(question);

			JSONParser parser = new JSONParser();
			JSONArray jsonArray = (JSONArray) parser.parse(foxJSONOutput);

			String output = URLDecoder.decode((String) ((JSONObject) jsonArray.get(0)).get("output"), "UTF-8");

			String baseURI = "http://dbpedia.org";
			Model model = ModelFactory.createDefaultModel();
			RDFReader r = model.getReader("N3");
			r.read(model, new StringReader(output), baseURI);

			ResIterator iter = model.listSubjects();
			ArrayList<Entity> tmpList = new ArrayList<>();
			while (iter.hasNext()) {
				Resource next = iter.next();
				StmtIterator statementIter = next.listProperties();
				Entity ent = new Entity();
				while (statementIter.hasNext()) {
					Statement statement = statementIter.next();
					String predicateURI = statement.getPredicate().getURI();
					if (predicateURI.equals("http://www.w3.org/2000/10/annotation-ns#body")) {
						ent.label = statement.getObject().asLiteral().getString();
					} else if (predicateURI.equals("http://ns.aksw.org/scms/means")) {
						String uri = statement.getObject().asResource().getURI();
						String encode = uri.replaceAll(",","%2C");
						ResourceImpl e = new ResourceImpl(encode);
						ent.uris.add(e);
					} else if (predicateURI.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
						ent.posTypesAndCategories.add(statement.getObject().asResource());
					}
				}
				tmpList.add(ent);
			}
			tmp.put("en", tmpList);

		} catch (IOException | ParseException e) {
			log.error("Could not call FOX for NER/NED", e);
		}
		if (!tmp.isEmpty()) {
			log.debug("\t" + Joiner.on("\n").join(tmp.get("en")));
		}
		return tmp;
	}

	public static void main(String args[]) {
		Question q = new Question();
		q.languageToQuestion.put("en", "Which buildings in art deco style did Shreve, Lamb and Harmon design?");
		ASpotter fox = new Fox();
		q.languageToNamedEntites = fox.getEntities(q.languageToQuestion.get("en"));
		for (String key : q.languageToNamedEntites.keySet()) {
			System.out.println(key);
			for (Entity entity : q.languageToNamedEntites.get(key)) {
				System.out.println("\t" + entity.label + " ->" + entity.type);
				for (Resource r : entity.posTypesAndCategories) {
					System.out.println("\t\tpos: " + r);
				}
				for (Resource r : entity.uris) {
					System.out.println("\t\turi: " + r);
				}
			}
		}
	}
}
