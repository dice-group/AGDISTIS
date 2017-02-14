package org.aksw.agdistis.model;

import java.util.Arrays;
import java.util.List;

public class NamedEntity implements Comparable<NamedEntity> {

    private String namedEntity;
    private int start[];
    private int end[];
    private int offset = -1;
    private String disambiguatedURL;
    private String type;

    public NamedEntity() {

    }

    public NamedEntity(NamedEntity n) {
        this.namedEntity = n.getNamedEntity();
        this.start = n.getStart();
        this.end = n.getEnd();
        this.offset = n.getOffset();
        this.disambiguatedURL = n.getDisambiguatedURL();
        this.type = n.getType();
    }

    public NamedEntity(int newStartPost[], int end[], int length, String entityLabel, String type) {
        this.namedEntity = entityLabel;
        this.start = newStartPost;
        this.end = end;
        this.offset = length;
        this.disambiguatedURL = null;
        this.type = type;
    }

    public String getNamedEntity() {
        return namedEntity;
    }

    public void setNamedEntity(String namedEntity) {
        this.namedEntity = namedEntity;
    }

    public int[] getStart() {
        return start;
    }

    public void setStart(int[] start) {
        this.start = start;
    }

    public int[] getEnd() {
        return end;
    }

    public String getType() {
        return type;
    }

    public void setEnd(int[] end) {
        this.end = end;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getDisambiguatedURL() {
        return disambiguatedURL;
    }

    public void setDisambiguatedURL(String disambiguatedURL) {
        this.disambiguatedURL = disambiguatedURL;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NamedEntity [namedEntity=");
        builder.append(namedEntity);
        builder.append(", start=");
        builder.append(Arrays.toString(start));
        builder.append(", end=");
        builder.append(Arrays.toString(end));
        builder.append(", offset=");
        builder.append(offset);
        builder.append(", disambiguatedURL=");
        builder.append(disambiguatedURL);
        builder.append(", type=");
        builder.append(type);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((disambiguatedURL == null) ? 0 : disambiguatedURL.hashCode());
        result = prime * result + Arrays.hashCode(end);
        result = prime * result + ((namedEntity == null) ? 0 : namedEntity.hashCode());
        result = prime * result + offset;
        result = prime * result + Arrays.hashCode(start);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NamedEntity other = (NamedEntity) obj;
        if (disambiguatedURL == null) {
            if (other.disambiguatedURL != null) {
                return false;
            }
        } else if (!disambiguatedURL.equals(other.disambiguatedURL)) {
            return false;
        }
        if (!Arrays.equals(end, other.end)) {
            return false;
        }
        if (namedEntity == null) {
            if (other.namedEntity != null) {
                return false;
            }
        } else if (!namedEntity.equals(other.namedEntity)) {
            return false;
        }

        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }

        if (offset != other.offset) {
            return false;
        }
        if (!Arrays.equals(start, other.start)) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(NamedEntity o) {
        if (this.start[0] < o.start[0]) {
            return -1;
        } else if (this.start[0] > o.start[0]) {
            return 1;
        }
        return 0;
    }

}
