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

import org.apache.log4j.Logger;

/**
 * Abstract parent class of all DataConverters
 * @author Mark Woodbridge
 */
 public abstract class DataConverter
{
    private static final Logger LOG = Logger.getLogger(DataConverter.class);

    protected ItemWriter writer;

    /**
    * Constructor that should be called by children
    * @param writer an ItemWriter used to handle the resultant Items
    */
    public DataConverter(ItemWriter writer) {
        this.writer = writer;
    }
}
