package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;

/**
 * Parent class for DataConverters that read from files
 * @author Mark Woodbridge
 */
public abstract class FileConverter extends DataConverter
{
    protected BufferedReader reader;

    /**
     * Constructor
     * @param reader BufferedReader used as input
     * @param writer the Writer used to output the resultant items
     */
    protected FileConverter(BufferedReader reader, ItemWriter writer) {
        super(writer);
        this.reader = reader;
    }
}
