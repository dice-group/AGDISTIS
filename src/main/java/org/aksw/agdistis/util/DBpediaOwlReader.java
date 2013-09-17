package org.aksw.agdistis.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

public class DBpediaOwlReader {

    public HashSet<String> hashset;

    public DBpediaOwlReader(String file) {
        hashset = new HashSet<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            while (br.ready()) {
                hashset.add(br.readLine());
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
