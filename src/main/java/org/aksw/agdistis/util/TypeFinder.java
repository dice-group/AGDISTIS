/*
 * Copyright (C) 2016 diegomoussallem
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.aksw.agdistis.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.aksw.agdistis.fox.ASpotter;
import org.aksw.agdistis.fox.Fox;
import org.aksw.agdistis.model.NamedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author diegomoussallem
 */
public class TypeFinder {

    private static Logger log = LoggerFactory.getLogger(TypeFinder.class);

    public String typeFinder(String preAnnotatedText) throws InterruptedException, IOException {
        List<NamedEntity> listTyped = new ArrayList<NamedEntity>();
        log.info("\tText: " + preAnnotatedText);
        int startpos = 0, endpos = 0;
        int count = 0;
        String type = "";
        StringBuilder sb = new StringBuilder();
        int start[] = new int[1];
        int end2[] = new int[1];

        try {
            if (preAnnotatedText.contains("<")) {

                if (count == 0) {
                    sb.append(preAnnotatedText.substring(0, preAnnotatedText.length()));
                }

                while (startpos >= 0) {
                    if (startpos >= 0) {
                        type = "entity";
                        while (sb.toString().contains("<entity>")) {
                            endpos = 0;
                            startpos += 8;
                            endpos = sb.toString().indexOf("</entity>", startpos);
                            start[0] = startpos;
                            end2[0] = endpos;
                            String entityLabel = sb.toString().substring(startpos, endpos);
                            listTyped.add(new NamedEntity(start, end2, entityLabel.length(), entityLabel, type));
                            sb.delete(startpos - 8, startpos);
                            sb.delete(endpos - 8, (endpos - 8) + 9);
                            endpos += entityLabel.length();
                            startpos = sb.toString().indexOf("<entity>", 0);
                        }
                    }
                }
            }
        } catch (IndexOutOfBoundsException e) {
            System.err.println("IndexOutOfBoundsException: " + e.getMessage());
        }
        String text = preAnnotatedText.replaceAll("<entity>", "").replaceAll("</entity>", "");

        for (int i = 0; i < listTyped.size(); i++) {

            if (listTyped.get(i).getStart()[0] != text.indexOf(listTyped.get(i).getNamedEntity())) {
                int[] set = new int[1];
                set[0] = text.indexOf(listTyped.get(i).getNamedEntity());
                listTyped.get(i).setStart(set);
            }

        }

        ASpotter fox = new Fox();
        List<NamedEntity> entities = new ArrayList<NamedEntity>();
        try {
            entities = fox.getEntities(text);

            Collections.sort(listTyped);
            Collections.sort(entities);

            StringBuilder textBuilder = new StringBuilder();
            int lastPos = 0;

            for (int i = 0; i < listTyped.size(); i++) {
                NamedEntity span = listTyped.get(i);

                for (int j = 0; j < entities.size(); j++) {
                    if (entities.get(j).getStart()[0] == span.getStart()[0] && entities.get(j).getOffset() == span.getOffset()) {
                        type = entities.get(j).getType();
                        //System.out.println(entities.get(j).getNamedEntity());
                        //System.out.println(type);
                    }
                }

                int begin = span.getStart()[0];
                int end = begin + span.getOffset();

                if (begin < lastPos) {
                    // we have two overlapping mentions --> take the larger one
                    NamedEntity prev = listTyped.get(i - 1);
                    log.warn("\"{}\" at pos {} overlaps with \"{}\" at pos {}",
                            text.substring(span.getStart()[0], span.getStart()[0] + span.getOffset()),
                            span.getStart()[0],
                            text.substring(prev.getStart()[0], prev.getStart()[0] + prev.getOffset()),
                            prev.getStart()[0]);
                    if (span.getOffset() > prev.getOffset()) {
                        // current is larger --> replace previous with current
                        textBuilder.delete(textBuilder.length() - prev.getOffset(), textBuilder.length());
                        lastPos -= prev.getOffset();
                    } else // previous is larger or equal --> skip current
                    {
                        continue;
                    }
                } else if (type.equals("entity")) {
                    textBuilder.append(text.substring(lastPos, span.getStart()[0])).append("<misc>" + span.getNamedEntity() + "</misc>");
                    lastPos = end;
                    continue;
                }

                String before = text.substring(lastPos, begin);
                String label = text.substring(begin, end);
                lastPos = end;

                if (type.equals("http://ns.aksw.org/scms/annotations/PERSON")) {
                    textBuilder.append(before).append("<person>" + label + "</person>");
                } else if (type.equals("http://ns.aksw.org/scms/annotations/LOCATION")) {
                    textBuilder.append(before).append("<location>" + label + "</location>");
                } else if (type.equals("http://ns.aksw.org/scms/annotations/ORGANIZATION")) {
                    textBuilder.append(before).append("<organization>" + label + "</organization>");
                } else if (type.isEmpty()) {
                    textBuilder.append(before).append("<misc>" + label + "</misc>");

                }
                type = "";
            }

            String lastSnippet = text.substring(lastPos, text.length());
            textBuilder.append(lastSnippet);
            return textBuilder.toString();
        } catch (IndexOutOfBoundsException e) {
            System.err.println("IndexOutOfBoundsException: " + e.getMessage());
            return preAnnotatedText;
        }
    }
}
