package org.aksw.agdistis.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.DocumentText;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.gerbil.transfer.nif.NIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.NIFDocumentParser;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NifBasedServerResource extends ServerResource {
	private static Logger log = LoggerFactory.getLogger(NifBasedServerResource.class);
	private NEDAlgo_HITS agdistis;
	private NIFDocumentParser documentReader = new TurtleNIFDocumentParser();
	private NIFDocumentCreator documentWriter = new TurtleNIFDocumentCreator();

	public NifBasedServerResource() {
		try {
			agdistis = new NEDAlgo_HITS();
		} catch (IOException e) {
			log.error(
					"Can not load index due to either wrong properties in agdistis.properties or missing index at location",
					e);
			System.exit(0);
		}
	}

	@Post
	public String postText(Representation entity) {
		log.info("Start working on Request for AGDISTIS");
		// Parse the given representation and retrieve data
		Map<String, Object> requestAttributes = this.getRequestAttributes();

		log.error(entity.toString());

		org.aksw.gerbil.transfer.nif.Document nifDoc = null;
		try {
			nifDoc = documentReader.getDocumentFromNIFStream(entity.getStream());
		} catch (Exception e) {
			log.error("Couldn't parse NIF document.", e);
			this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e, "Couldn't parse NIF document.");
			return "ERROR: Couldn't parse NIF document.";
		}

		try {
			evaluationOptionalParameters(requestAttributes);
		} catch (Exception e) {
			log.error("Couldn't parse parameters.", e);
			this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e, "Couldn't parse parameters.");
			return "ERROR: Couldn't parse parameters.";
		}

		HashMap<NamedEntityInText, String> results = null;
		Document d = nifToDocument(nifDoc);

		if (requestAttributes.containsKey(RestletApplication.TYPE_PARAMETER_NAME)
				&& requestAttributes.get(RestletApplication.TYPE_PARAMETER_NAME).equals("agdistis")) {
			results = results(d, agdistis);
		} else {
			this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Got the wrong type or the type parameter is missing.");
			return "ERROR: wrong type";
		}

		org.aksw.gerbil.transfer.nif.Document resultDoc = new DocumentImpl(nifDoc.getText(), nifDoc.getDocumentURI());
		for (NamedEntityInText namedEntity : results.keySet()) {
			String disambiguatedURL = results.get(namedEntity);
			resultDoc.addMarking(new NamedEntity(namedEntity.getStartPos(), namedEntity.getLength(), disambiguatedURL));
		}
		log.info("Finished Request");
		return documentWriter.getDocumentAsNIFString(resultDoc);

	}

	private void evaluationOptionalParameters(Map<String, Object> requestAttributes) {

		if (requestAttributes.containsKey(RestletApplication.SIMILARITY_PARAMETER_NAME)) {
			double similarityThreshold = (Double) requestAttributes.get(RestletApplication.SIMILARITY_PARAMETER_NAME);
			log.info("similarityThreshold: " + similarityThreshold);
			agdistis.setThreshholdTrigram(Double.valueOf(similarityThreshold));
		} else {
			log.info("similarityThreshold: null");
			agdistis.setThreshholdTrigram(0.82);
		}

		if (requestAttributes.containsKey(RestletApplication.DEPTH_PARAMETER_NAME)) {
			int explorationDepth = (Integer) requestAttributes.get(RestletApplication.DEPTH_PARAMETER_NAME);
			log.info("explorationDepth: " + explorationDepth);
			agdistis.setMaxDepth(explorationDepth);
		} else {
			log.info("explorationDepth: null");
			agdistis.setMaxDepth(2);
		}

		if (requestAttributes.containsKey(RestletApplication.HEURISTIC_PARAMETER_NAME)) {
			boolean heuristicExpansion = (Boolean) requestAttributes.get(RestletApplication.HEURISTIC_PARAMETER_NAME);
			log.info("heuristicExpansion: " + heuristicExpansion);
			agdistis.setHeuristicExpansionOn(Boolean.valueOf(heuristicExpansion));
		} else {
			log.info("heuristicExpansion: null");
			agdistis.setHeuristicExpansionOn(true);
		}
	}

	public static Document nifToDocument(org.aksw.gerbil.transfer.nif.Document nifDoc) {
		Document document = new Document();
		ArrayList<NamedEntityInText> list = new ArrayList<NamedEntityInText>();
		String text = nifDoc.getText();
		log.info("\tText: " + text);
		for (Span span : nifDoc.getMarkings(Span.class)) {
			list.add(new NamedEntityInText(span.getStartPosition(), span.getLength(),
					text.substring(span.getStartPosition(), span.getStartPosition() + span.getLength())));
		}

		NamedEntitiesInText nes = new NamedEntitiesInText(list);
		DocumentText docText = new DocumentText(nifDoc.getText());

		document.addTest(docText);
		document.addNamedEntitiesInText(nes);
		return document;
	}

	private static HashMap<NamedEntityInText, String> results(Document document, NEDAlgo_HITS algo) {
		algo.run(document);
		NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
		HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = algo.findResult(namedEntity);
			results.put(namedEntity, disambiguatedURL);
		}
		return results;
	}

	public void close() throws IOException {
		agdistis.close();
	}

}
