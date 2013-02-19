package org.intermine.api.bag;

import java.util.Collection;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.DescriptorUtils;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ObjectStoreBagCombination;

public class UnionOperation extends BagOperation {

    public UnionOperation(Model model, Collection<InterMineBag> bags,
            Profile profile) {
        super(model, bags, profile);
    }

    @Override
    protected String getNewBagType() throws MetaDataException {
        return DescriptorUtils.findSumType(getClasses()).getUnqualifiedName();
    }

    @Override
    protected int getOperationCode() {
        return ObjectStoreBagCombination.UNION;
    }

}
