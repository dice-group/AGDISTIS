package org.aksw.agdistis.datatypes;

public class Text {
    private int id;
    private String text;

    public Text(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public Text(String text) {
        this.id = -1;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "t: " + id + " -> " + text.substring(0, 20);
    }
}
