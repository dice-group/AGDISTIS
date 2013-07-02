package de.bluekiwi.labs.util;

public class JsonEntity {
    String URI;
    int offset;

    @Override
    public String toString() {
        return URI + " " + offset;
    }
}
