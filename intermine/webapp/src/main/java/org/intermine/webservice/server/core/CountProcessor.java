package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;

import org.intermine.webservice.server.output.Output;

/**
 * A class that processes counts.
 * @author Alex Kalderimis
 *
 */
public class CountProcessor
{
    /** Constructor **/
    public CountProcessor() {
        // Empty constructor
    }

    /**
     * Write the count to the output.
     * @param count The count
     * @param output The output.
     */
    public void writeCount(int count, Output output) {
        output.addResultItem(Arrays.asList(String.valueOf(count)));
    }
}
