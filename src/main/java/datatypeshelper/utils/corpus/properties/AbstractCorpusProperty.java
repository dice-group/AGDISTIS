package datatypeshelper.utils.corpus.properties;

public abstract class AbstractCorpusProperty implements CorpusProperty {

    private static final long serialVersionUID = -449507835435340908L;

    @Override
    public int hashCode() {
        return getValue().hashCode();
    }

    @Override
	public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(this.getClass().getSimpleName());
        result.append("=\"");
        result.append(getValue().toString());
        result.append('"');
        return result.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof CorpusProperty) {
            Object value = this.getValue();
            if (value == null) {
                return ((CorpusProperty) obj).getValue() == null;
            } else {
                return value.equals(((CorpusProperty) obj).getValue());
            }
        } else {
            return super.equals(obj);
        }
    }
}
