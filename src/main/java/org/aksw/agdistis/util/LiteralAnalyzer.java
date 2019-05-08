package org.aksw.agdistis.util;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import org.apache.lucene.analysis.core.LetterTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;




public class LiteralAnalyzer extends Analyzer {

	/**
	 * Creates a new {@link Analyzer}
	 *
	 */
	public LiteralAnalyzer() {
		super();
	}



	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		final Tokenizer source = new LetterTokenizer();
		TokenStream result = new LowerCaseFilter(source);
		result =new ASCIIFoldingFilter(result);
		return new  TokenStreamComponents(source, result);

	}

}