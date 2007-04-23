package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class ResultsRow extends ArrayList
{
    /**
     * @see ArrayList#ArrayList
     */
    public ResultsRow() {
        super();
    }

    /**
     * @see ArrayList#ArrayList(Collection)
     *
     * @param c an existing Collection
     */
    public ResultsRow(Collection c) {
        super(c);
    }
}
