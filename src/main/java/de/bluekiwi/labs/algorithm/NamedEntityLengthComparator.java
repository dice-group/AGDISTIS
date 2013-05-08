package de.bluekiwi.labs.algorithm;

import java.util.Comparator;

import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

public class NamedEntityLengthComparator implements Comparator<NamedEntityInText> {

    @Override
    public int compare(NamedEntityInText o1, NamedEntityInText o2) {
        return Double.compare(o1.getLength(), o2.getLength());
    }

}
