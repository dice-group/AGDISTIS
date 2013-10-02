package datatypeshelper.utils.doc.ner;

public class SignedNamedEntityInText extends NamedEntityInText {

    private static final long serialVersionUID = -5165915876107803417L;

    private String source;

    public SignedNamedEntityInText(NamedEntityInText entity) {
        super(entity);
        if (entity instanceof SignedNamedEntityInText) {
            this.source = ((SignedNamedEntityInText) entity).source;
        }
    }

    public SignedNamedEntityInText(NamedEntityInText entity, String source) {
        super(entity);
        this.source = source;
    }

    public SignedNamedEntityInText(NamedEntityInText entity, int startPos, int length) {
        super(entity, startPos, length);
        if (entity instanceof SignedNamedEntityInText) {
            this.source = ((SignedNamedEntityInText) entity).source;
        }
    }

    public SignedNamedEntityInText(int startPos, int length, String namedEntityUri, String source) {
        super(startPos, length, namedEntityUri);
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("(\"");
        result.append(getNamedEntityUri());
        result.append("\", ");
        result.append(getStartPos());
        result.append(", ");
        result.append(getLength());
        if (source != null) {
            result.append(", source=\"");
            result.append(source);
            result.append("\")");
        } else {
            result.append(")");
        }
        return result.toString();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new SignedNamedEntityInText(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SignedNamedEntityInText) {
            if (compareTo((SignedNamedEntityInText) obj) == 0) {
                return this.source.equals(((SignedNamedEntityInText) obj).source);
            } else {
                return false;
            }
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ source.hashCode();
    }
}
