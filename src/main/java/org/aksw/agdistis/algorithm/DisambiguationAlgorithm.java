package org.aksw.agdistis.algorithm;

import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntityInText;

public interface DisambiguationAlgorithm {

	public abstract void run(Document document);

	public abstract String findResult(NamedEntityInText namedEntity);

	public abstract void close();

	public abstract void setThreshholdTrigram(double threshholdTrigram);

	public abstract double getThreshholdTrigram();

	public abstract void setMaxDepth(int maxDepth);

	public abstract String getRedirect(String findResult);

}