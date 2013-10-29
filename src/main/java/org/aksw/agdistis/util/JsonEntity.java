package org.aksw.agdistis.util;

public class JsonEntity {
    public String URI;
    public int offset;

    @Override
    public String toString() {
        return URI + " " + offset;
    }
}
