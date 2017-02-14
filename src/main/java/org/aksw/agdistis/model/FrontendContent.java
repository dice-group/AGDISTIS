package org.aksw.agdistis.model;

import java.util.Arrays;

public class FrontendContent {

    private String text;
    private String[] entities;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String[] getEntities() {
        return entities;
    }

    public void setEntities(String[] entities) {
        this.entities = entities;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(entities);
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FrontendContent other = (FrontendContent) obj;
        if (!Arrays.equals(entities, other.entities))
            return false;
        if (text == null) {
            if (other.text != null)
                return false;
        } else if (!text.equals(other.text))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FrontendContent [text=");
        builder.append(text);
        builder.append(", entities=");
        builder.append(Arrays.toString(entities));
        builder.append("]");
        return builder.toString();
    }

}
