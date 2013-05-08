package de.bluekiwi.labs.algorithm;

import java.util.Comparator;

import com.unister.semweb.ned.QRToolNED.datatypes.Label;

public class LabelLengthComparator implements Comparator<Label> {

    @Override
    public int compare(Label o1, Label o2) {
        return Double.compare(o1.getStart(), o2.getStart());
    }

}
