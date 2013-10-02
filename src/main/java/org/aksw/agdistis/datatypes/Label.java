package org.aksw.agdistis.datatypes;

import java.util.ArrayList;
import java.util.List;

public class Label {

    private static final String LABEL_HAS_MULTIPLIE_CANDIDATES_CLASS = "multiple_candidate_label";
    private static final String LABEL_HAS_ONE_CANDIDATE_CLASS = "single_candidate_label";
    private static final String LABEL_HAS_NO_CANDIDATES_CLASS = "no_candidates_label";

    private int labelId;
    private int start;
    private int end;
    private String label;
    private List<Candidate> candidates;
    private int textHasLabelId;

    public Label(int labelId, int start, int end, String label, List<Candidate> candidates) {
        this.labelId = labelId;
        this.start = start;
        this.end = end;
        this.label = label;
        this.candidates = candidates;
    }

    public Label(int labelId, int start, int end, String label) {
        this(labelId, start, end, label, new ArrayList<Candidate>());
    }

    public Label(int start, int end, String label) {
        this(-1, start, end, label);
    }

    public Label(String label) {
        this(-1, -1, label);
    }

    public int getLabelId() {
        return labelId;
    }

    public void setLabelId(int labelId) {
        this.labelId = labelId;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getMarkedupLabel() {
        StringBuilder result = new StringBuilder();
        result.append("<span unselectable=\"off\" id=\"");
        result.append(textHasLabelId);
        result.append("\" value=\"");
        result.append(label);
        result.append("\" class=\"");
        switch (candidates.size()) {
        case 0: {
            result.append(LABEL_HAS_NO_CANDIDATES_CLASS);
            break;
        }
        case 1: {
            result.append(LABEL_HAS_ONE_CANDIDATE_CLASS);
            break;
        }
        default: {
            result.append(LABEL_HAS_MULTIPLIE_CANDIDATES_CLASS);
            break;
        }
        }
        result.append("\">");
        result.append(label);
        result.append("</span>");
        return result.toString();
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public int getTextHasLabelId() {
        return textHasLabelId;
    }

    public void setTextHasLabelId(int textHasLabelId) {
        this.textHasLabelId = textHasLabelId;
    }

    @Override
    public String toString() {
        return "l: " + label + ": " + start + " -> " + end;
    }
}
