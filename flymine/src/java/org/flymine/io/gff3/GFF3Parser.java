package org.flymine.io.gff3;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.io.BufferedReader;

/**
 * A parser for GFF3 files.  This code was taken from Matthew Pocock's GFF3 parser in BioJava.
 *
 * @author Kim Rutherford
 */

public class GFF3Parser
{
    public static List parse(final BufferedReader reader) throws IOException {
        int lineNum = 0;
        List list = new ArrayList();

        String line = null;

        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();

            if (trimmedLine.length() == 0 || trimmedLine.startsWith("#")) {
                continue;
            }

            list.add(new GFF3Record(line));
        }

        return list;
    }
}
