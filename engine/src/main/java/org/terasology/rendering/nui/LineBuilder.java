/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.nui;

import com.google.common.collect.Lists;
import org.terasology.rendering.assets.font.Font;

import java.util.Arrays;
import java.util.List;

/**
 * @author Immortius
 */
public class LineBuilder {

    private final Font font;
    private final int spaceWidth;
    private final int maxWidth;
    private List<String> lines = Lists.newArrayList();

    private int currentLineLength;
    private StringBuilder lineBuilder = new StringBuilder();

    public LineBuilder(Font font, int maxWidth) {
        this.font = font;
        this.spaceWidth = font.getWidth(' ');
        this.maxWidth = maxWidth;
    }

    public static List<String> getLines(Font font, String text, int maxWidth) {
        LineBuilder lineBuilder = new LineBuilder(font, maxWidth);
        lineBuilder.addText(text);
        return lineBuilder.getLines();
    }

    public void addText(String text) {
        List<String> paragraphs = Arrays.asList(text.split("\\r?\\n"));
        for (String paragraph : paragraphs) {
            String remainder = paragraph;
            while (!remainder.isEmpty()) {
                String[] split = remainder.split(" ", 2);
                String word = split[0];
                if (split.length > 1) {
                    remainder = split[1];
                } else {
                    remainder = "";
                }

                addWord(word);
            }
            endLine();
        }
    }

    public void addWord(String word) {
        int wordWidth = font.getWidth(word);
        if (wordWidth > maxWidth) {
            if (currentLineLength > 0) {
                endLine();
            }
            for (char c : word.toCharArray()) {
                int charWidth = font.getWidth(c);
                if (currentLineLength + charWidth > maxWidth) {
                    endLine();
                }
                lineBuilder.append(c);
                currentLineLength += charWidth;
            }
        } else {
            if (currentLineLength > 0 && currentLineLength + spaceWidth + wordWidth > maxWidth) {
                endLine();
            }
            if (currentLineLength != 0) {
                lineBuilder.append(' ');
                currentLineLength += spaceWidth;
            }
            lineBuilder.append(word);
            currentLineLength += wordWidth;
        }
    }

    public List<String> getLines() {
        return lines;
    }

    private void endLine() {
        currentLineLength = 0;
        lines.add(lineBuilder.toString());
        lineBuilder.setLength(0);
    }
}
