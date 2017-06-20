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

import org.intermine.metadata.MetaDataException;

/**
 *
 * @author Alex
 *
 */
public class IncompatibleTypes extends BagOperationException
{

    private static final long serialVersionUID = 5382508035006834031L;

    /**
     *
     * @param e exception
     */
    public IncompatibleTypes(MetaDataException e) {
        super("Incompatible types", e);
    }
}
