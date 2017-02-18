/*
 * Copyright (C) 2016 diegomoussallem
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.aksw.agdistis.util;

import java.io.IOException;
/**
 *
 * @author diegomoussallem
 */
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import smile.nlp.stemmer.LancasterStemmer;

public class Stemming {

	protected StanfordCoreNLP pipeline;

	public Stemming() {

		// Create StanfordCoreNLP object properties, with POS tagging
		// (required for lemmatization), and lemmatization
		Properties props;
		props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");

		/*
		 * This is a pipeline that takes in a string and returns various
		 * analyzed linguistic forms. The String is tokenized via a tokenizer
		 * (such as PTBTokenizerAnnotator), and then other sequence model style
		 * annotation can be used to add things like lemmas, POS tags, and named
		 * entities. These are returned as a list of CoreLabels. Other analysis
		 * components build and store parse trees, dependency graphs, etc.
		 * 
		 * This class is designed to apply multiple Annotators to an Annotation.
		 * The idea is that you first build up the pipeline by adding
		 * Annotators, and then you take the objects you wish to annotate and
		 * pass them in and get in return a fully annotated object.
		 * 
		 * StanfordCoreNLP loads a lot of models, so you probably only want to
		 * do this once per execution
		 */
		this.pipeline = new StanfordCoreNLP(props);
	}

	public String stemming(String documentText) {
		List<String> lemmas = new LinkedList<String>();
		String label = null;
		LancasterStemmer stem = new LancasterStemmer();
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		this.pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the
				// list of lemmas
				// lemmas.add(token.get(LemmaAnnotation.class));
				// lemmas.add(morpho.stem(token.word()));
				lemmas.add(stem.stem(token.get(LemmaAnnotation.class)));
			}
		}

		label = lemmas.toString();
		Pattern p = Pattern.compile("[,.;!?(){}\\[\\]<>%]");
		label = p.matcher(label).replaceAll("");

		return label;
	}

	public static void main(String[] args) throws IOException {

		/*
		 * System.out.println("Starting Stanford Lemmatizer"); String text =
		 * "How could you be seeing into my eyes like open doors? \n"+
		 * "You led me down into my core where I've became so numb \n"+
		 * "Without a soul my spirit's sleeping somewhere cold \n"+
		 * "Until you find it there and led it back home \n"+
		 * "You woke me up inside \n"+
		 * "Called my name and saved me from the dark \n"+
		 * "You have bidden my blood and it ran \n"+
		 * "Before I would become undone \n"+
		 * "You saved me from the nothing I've almost become \n"+
		 * "You were bringing me to life \n"+
		 * "Now that I knew what I'm without \n"+ "You can've just left me \n"+
		 * "You breathed into me and made me real \n"+
		 * "Frozen inside without your touch \n"+
		 * "Without your love, darling \n"+
		 * "Only you are the life among the dead \n"+
		 * "I've been living a lie, there's nothing inside \n"+
		 * "You were bringing me to life."; Lemmatization slem = new
		 * Lemmatization(); System.out.println(slem.lemmatize(text));
		 */
		// String label = "Northern India";
		String label = "Tibetan";
		Stemming slem = new Stemming();

		label = slem.stemming(label);
		System.out.println(label);

	}

}
