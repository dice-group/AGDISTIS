package org.aksw.agdistis.datatypes;

import java.util.List;

public class TextWithLabels extends Text {

    private List<Label> labels;

    public TextWithLabels(Text text, List<Label> labels) {
        super(text.getId(), text.getText());
        this.labels = labels;
    }

    public TextWithLabels(String text, List<Label> labels) {
        super(text);
        this.labels = labels;
    }

    public TextWithLabels(int id, String text, List<Label> labels) {
        super(id, text);
        this.labels = labels;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getText().toString() + "\n");
        for (Label l : this.getLabels()) {
            sb.append("\t" + l.toString() + "\n");
            for (Candidate c : l.getCandidates()) {
                sb.append("\t" + c.toString() + "\n");
            }
        }
        return sb.toString();
    }
}
