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
public class NotCurrent extends BagOperationException
{

    private static final long serialVersionUID = 4859189430080794926L;

    /**
     * Constructor
     */
    public NotCurrent() {
        super("Not all bags are current");
    }
}
