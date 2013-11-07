package org.aksw.agdistis.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mpi.aida.AidaManager;
import mpi.aida.Disambiguator;
import mpi.aida.Preparator;
import mpi.aida.config.settings.disambiguation.CocktailPartyDisambiguationSettings;
import mpi.aida.config.settings.preparation.StanfordHybridPreparationSettings;
import mpi.aida.data.DisambiguationResults;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;
import mpi.aida.graph.similarity.exception.MissingSettingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class NEDAIDADisambiguator implements DisambiguationAlgorithm {
	Logger log = LoggerFactory.getLogger(NEDAIDADisambiguator.class);
	private StanfordHybridPreparationSettings prepSettings;
	private Preparator p;
	private CocktailPartyDisambiguationSettings disSettings;
	private DisambiguationResults results;

	public NEDAIDADisambiguator() {
		try {
			// Prepare the input for disambiguation. The Stanford NER
			// will be run to identify names. Strings marked with [[ ]]
			// will also betreated as names.
			prepSettings = new StanfordHybridPreparationSettings();
			p = new Preparator();
			disSettings = new CocktailPartyDisambiguationSettings();
		} catch (MissingSettingException e) {
			log.error(e.getLocalizedMessage());
		}
	}

	@Override
	public void run(Document document) {
		try {
			String text = markupText(document);
			PreparedInput input = p.prepare(text, prepSettings);

			// Disambiguate the input with the graph coherence algorithm.
			Disambiguator d = new Disambiguator(input, disSettings);
			results = d.disambiguate();
		} catch (Exception e) {
			log.error("Can not process document: " + document.getDocumentId());
			log.error(e.getLocalizedMessage());
		}

	}

	@Override
	public String findResult(NamedEntityInText namedEntity) {
		// Print the disambiguation results.
		int startPos = namedEntity.getStartPos();
		for (ResultMention rm : results.getResultMentions()) {
			ResultEntity re = results.getBestEntity(rm);
			if (rm.getCharacterOffset() == startPos) {
				String wikiURL = AidaManager.getWikipediaUrl(re);
				return wikiURL.replace("http://en.wikipedia.org/wiki", "http://dbpedia.org/resource");
			}
		}
		return null;
	}

	@Override
	public void close() {

	}

	@Override
	public String getRedirect(String findResult) {
		return findResult;
	}

	private String markupText(Document document) {
		List<String> textParts = new ArrayList<String>();
		List<NamedEntityInText> labels = document.getProperty(NamedEntitiesInText.class).getNamedEntities();
		Collections.sort(labels);
		String originalText = document.getProperty(DocumentText.class).getText();
		// start with the last label and add the parts of the new text beginning
		// with its end to the array
		// Note that we are expecting that the labels are sorted descending by
		// there position in the text!
		int startFormerLabel = originalText.length();
		for (NamedEntityInText entity : labels) {
			// proof if this label undercuts the last one.
			if (startFormerLabel >= entity.getEndPos()) {
				// append the text between this label and the former one
				textParts.add(originalText.substring(entity.getEndPos(), startFormerLabel));
				// append the markedup label
				String label = originalText.substring(entity.getStartPos(), entity.getEndPos());
				textParts.add("[[" + label + "]]");
				// remember the start position of this label
				startFormerLabel = entity.getStartPos();
			} else {
				log.error("Label undercuts another label. TextId: " + document.getDocumentId());
			}
		}
		textParts.add(originalText.substring(0, startFormerLabel));
		// Form the new text beginning with its end
		StringBuilder textWithMarkups = new StringBuilder();
		for (int i = textParts.size() - 1; i >= 0; --i) {
			textWithMarkups.append(textParts.get(i));
		}
		return textWithMarkups.toString();
	}

	@Override
	public void setThreshholdTrigram(double threshholdTrigram) {
		
	}

	@Override
	public void setMaxDepth(int maxDepth) {
		
	}
	@Override
	public double getThreshholdTrigram() {
		return 0;
	}

}
