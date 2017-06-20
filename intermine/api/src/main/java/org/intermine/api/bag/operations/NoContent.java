package org.intermine.api.bag.operations;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 *
 * @author Alex
 *
 */
public class NoContent extends BagOperationException
{

    private static final long serialVersionUID = -1679235731173923331L;

    /**
     * Constructor
     */
    public NoContent() {
        super("This operation failed to produce any content");
    }
}
