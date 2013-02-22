package org.intermine.api.bag.operations;

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.MetaDataException;

/**
 * Functional interface for BagOperations, so we can be all forward looking.
 * @author Alex Kalderimis.
 *
 */
public interface BagProducer {

    InterMineBag operate() throws BagOperationException, MetaDataException;
}
