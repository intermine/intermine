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

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.MetaDataException;

/**
 * Functional interface for BagOperations, so we can be all forward looking.
 *
 * @author Alex Kalderimis.
 */
public interface BagProducer
{

    /**
     * @return list
     * @throws BagOperationException if bag operation fails
     * @throws MetaDataException if something goes wrong
     */
    InterMineBag operate() throws BagOperationException, MetaDataException;
}
