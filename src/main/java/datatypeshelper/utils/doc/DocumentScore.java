package datatypeshelper.utils.doc;

public class DocumentScore extends AbstractDocumentProperty implements ParseableDocumentProperty {

    private static final long serialVersionUID = -1965728034855570011L;

    private double score;

    public DocumentScore() {
    }

    public DocumentScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public Object getValue() {
        return score;
    }

    @Override
    public void parseValue(String value) {
        score = Double.parseDouble(value);
    }
}
