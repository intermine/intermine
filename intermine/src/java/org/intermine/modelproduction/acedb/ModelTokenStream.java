package org.intermine.modelproduction.acedb;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Lexes the AceDB model file given, and produces a stream of tokens.
 *
 * @author Matthew Wakeling
 */
public class ModelTokenStream
{
    private BufferedReader in;
    private String currentLine;
    private int indent;
    private int charPos;

    /**
     * Constructor - takes a BufferedReader.
     *
     * @param in the BufferedReader from which to read text
     */
    public ModelTokenStream(BufferedReader in) {
        this.in = in;
        currentLine = "";
        indent = 0;
        charPos = 0;
    }

    /**
     * Returns the next token in the stream, or null if the end of the file has been reached.
     *
     * @return a ModelNode object containing the text and the indent level
     * @throws IOException if the BufferedReader does
     */
    public ModelNode nextToken() throws IOException {
        do {
            if (currentLine == null) {
                return null;
            } else if (currentLine.length() <= charPos) {
                currentLine = in.readLine();
                if (currentLine != null) {
                    int commentPos = currentLine.indexOf("//");
                    if (commentPos != -1) {
                        currentLine = currentLine.substring(0, commentPos);
                    }
                }
                indent = 0;
                charPos = 0;
            } else if (currentLine.charAt(charPos) == '\t') {
                charPos++;
                indent += 8 - (indent % 8);
            } else if (currentLine.charAt(charPos) == ' ') {
                charPos++;
                indent++;
            } else {
                // Normal character found - so, we return the word and set up for next time.
                int nextSpace = currentLine.indexOf(" ", charPos);
                nextSpace = (nextSpace == -1 ? currentLine.length() : nextSpace);
                int nextTab = currentLine.indexOf("\t", charPos);
                nextTab = (nextTab == -1 ? currentLine.length() : nextTab);
                nextSpace = (nextSpace < nextTab ? nextSpace : nextTab);
                ModelNode retval = new ModelNode(indent, currentLine.substring(charPos, nextSpace));
                indent += nextSpace - charPos;
                charPos = nextSpace;
                return retval;
            }
        } while (true);
    }
}

