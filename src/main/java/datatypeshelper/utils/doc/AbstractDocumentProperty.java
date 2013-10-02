package datatypeshelper.utils.doc;

public abstract class AbstractDocumentProperty implements DocumentProperty {

    private static final long serialVersionUID = 4242987535838261287L;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getSimpleName());
        builder.append("=\"");
        builder.append(this.getValue().toString());
        builder.append("\"");
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DocumentProperty) {
            if (this.getValue() == null) {
                return ((DocumentProperty) o).getValue() == null;
            } else {
                return this.getValue().equals(((DocumentProperty) o).getValue());
            }
        }
        return super.equals(o);
    }
}
