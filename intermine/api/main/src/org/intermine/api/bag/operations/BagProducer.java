package org.intermine.api.bag.operations;

/*
 * Copyright (C) 2002-2014 FlyMine
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
 * @author Alex Kalderimis.
 */
public interface BagProducer
{

    InterMineBag operate() throws BagOperationException, MetaDataException;
}
