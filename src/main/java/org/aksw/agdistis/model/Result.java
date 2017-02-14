package org.aksw.agdistis.model;

import java.util.List;

public class Result {

    private List<AgdistisEntity> namedEntities;
    private String detectedLanguage;

    public List<AgdistisEntity> getNamedEntities() {
        return namedEntities;
    }

    public void setNamedEntities(List<AgdistisEntity> namedEntities) {
        this.namedEntities = namedEntities;
    }

    public String getDetectedLanguage() {
        return detectedLanguage;
    }

    public void setDetectedLanguage(String detectedLanguage) {
        this.detectedLanguage = detectedLanguage;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((detectedLanguage == null) ? 0 : detectedLanguage.hashCode());
        result = prime * result + ((namedEntities == null) ? 0 : namedEntities.hashCode());
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
        Result other = (Result) obj;
        if (detectedLanguage == null) {
            if (other.detectedLanguage != null)
                return false;
        } else if (!detectedLanguage.equals(other.detectedLanguage))
            return false;
        if (namedEntities == null) {
            if (other.namedEntities != null)
                return false;
        } else if (!namedEntities.equals(other.namedEntities))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Result [namedEntities=");
        builder.append(namedEntities);
        builder.append(", detectedLanguage=");
        builder.append(detectedLanguage);
        builder.append("]");
        return builder.toString();
    }

}
