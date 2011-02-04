package org.intermine.api.config;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A single constant.
 *
 * @author Richard Smith
 */
public final class Constants
{
    private Constants() {
        // don't instantiate
    }

    /**
     * Batch size for the underlying objectstore
     */
    public static final int BATCH_SIZE = 500;
}
