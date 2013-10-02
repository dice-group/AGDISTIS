package datatypeshelper.utils.doc.ner;

import java.io.Serializable;

public class NamedEntityInText implements Comparable<NamedEntityInText>, Cloneable, Serializable {

    private static final long serialVersionUID = -6037260139634126022L;

    private int startPos;
    private int length;
    private String namedEntityUri;
    private String label;

    public NamedEntityInText(int startPos, int length, String namedEntityUri) {
        this.startPos = startPos;
        this.length = length;
        this.namedEntityUri = namedEntityUri;
        if (namedEntityUri != null) {
            this.label = extractLabel(namedEntityUri);
        }
    }

    public NamedEntityInText(NamedEntityInText entity) {
        this.startPos = entity.startPos;
        this.length = entity.length;
        this.namedEntityUri = entity.namedEntityUri;
        this.label = entity.label;
    }

    public NamedEntityInText(NamedEntityInText entity, int startPos, int length) {
        this.startPos = startPos;
        this.length = length;
        this.namedEntityUri = entity.namedEntityUri;
        this.label = entity.label;
    }

    public int getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public int getLength() {
        return length;
    }

    public int getEndPos() {
        return startPos + length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getNamedEntityUri() {
        return namedEntityUri;
    }

    public void setNamedEntity(String namedEntityUri) {
        this.namedEntityUri = namedEntityUri;
        this.label = extractLabel(namedEntityUri);
    }

    public String getLabel() {
        return label;
    }

    public String getSingleWordLabel() {
        return label.replaceAll("[ _\\(\\)]", "");
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("(\"");
        result.append(namedEntityUri);
        result.append("\", ");
        result.append(startPos);
        result.append(", ");
        result.append(length);
        result.append(")");
        return result.toString();
    }

    private static String extractLabel(String namedEntityUri) {
        int posSlash = namedEntityUri.lastIndexOf('/');
        int posPoints = namedEntityUri.lastIndexOf(':');
        if (posSlash > posPoints) {
            return namedEntityUri.substring(posSlash + 1);
        } else if (posPoints < posSlash) {
            return namedEntityUri.substring(posPoints + 1);
        } else {
            return namedEntityUri;
        }
    }

    @Override
    public int compareTo(NamedEntityInText ne) {
        // NamedEntityInText objects are typically ordered from the end of the text to its beginning
        int diff = (ne.startPos + ne.length) - (this.startPos + this.length);
        if (diff == 0) {
            diff = ne.length - this.length;
            if (diff == 0) {
                diff = this.namedEntityUri.compareTo(ne.namedEntityUri);
            }
        }
        if (diff < 0) {
            return -1;
        } else if (diff > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NamedEntityInText) {
            return compareTo((NamedEntityInText) obj) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return namedEntityUri.hashCode() ^ startPos ^ length;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new NamedEntityInText(this);
    }
}