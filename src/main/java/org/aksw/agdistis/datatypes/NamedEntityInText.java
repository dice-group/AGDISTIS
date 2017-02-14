package org.aksw.agdistis.datatypes;

import java.io.Serializable;

public class NamedEntityInText implements Comparable<NamedEntityInText>, Cloneable, Serializable {

    private static final long serialVersionUID = -6037260139634126022L;

    private int startPos;
    private int length;
    private String namedEntityUri;
    private String label;
    private String type;

    private String domain;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;

    }

    public NamedEntityInText(int startPos, int length, String namedEntityUri, String type) {
        this.startPos = startPos;
        this.length = length;
        this.namedEntityUri = namedEntityUri;
        if (namedEntityUri != null) {
            this.label = extractLabel(namedEntityUri);
        }
        this.type = type;
    }

    public NamedEntityInText(NamedEntityInText entity) {
        this.startPos = entity.startPos;
        this.length = entity.length;
        this.namedEntityUri = entity.namedEntityUri;
        this.label = entity.label;
        this.type = entity.type;
    }

    public NamedEntityInText(NamedEntityInText entity, int startPos, int length) {
        this.startPos = startPos;
        this.length = length;
        this.namedEntityUri = entity.namedEntityUri;
        this.label = entity.label;
        this.type = entity.type;
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
        //this.label = extractLabel(namedEntityUri);
    }

    public String getLabel() {
        return label;
    }

    public String getSingleWordLabel() {
        return label.replaceAll("[ _\\(\\)]", "");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;

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
        result.append(", ");
        result.append(type);
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
        // NamedEntityInText objects are typically ordered from the end of the
        // text to its beginning
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
        return label.hashCode() ^ startPos ^ length;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new NamedEntityInText(this);
    }

}
