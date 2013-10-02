package org.aksw.agdistis.algorithm;

import java.util.Comparator;

import datatypeshelper.utils.doc.ner.NamedEntityInText;

/**
 * Comparator for sorting Named Entities according to their length
 * 
 * @author r.usbeck
 * 
 */
public class NamedEntityLengthComparator implements Comparator<NamedEntityInText> {

    @Override
    public int compare(NamedEntityInText o1, NamedEntityInText o2) {
        return Double.compare(o1.getLength(), o2.getLength());
    }

}
