package de.bluekiwi.labs.util;

import java.util.ArrayList;
import java.util.List;

public class JsonText {
    List<JsonEntity> Resources = new ArrayList<JsonEntity>();

    @Override
    public String toString() {
        String tmp = "";
        for (JsonEntity ent : Resources) {
            tmp += ent.toString();
        }
        return tmp;
    }
}
