package org.intermine.api.bag;

import java.util.Collection;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.DescriptorUtils;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ObjectStoreBagCombination;

public class IntersectionOperation extends BagOperation {

    public IntersectionOperation(Model model, Collection<InterMineBag> bags,
            Profile profile) {
        super(model, bags, profile);
    }

    @Override
    protected String getNewBagType() throws MetaDataException {
        return DescriptorUtils.findIntersectionType(getClasses()).getUnqualifiedName();
    }

    @Override
    protected int getOperationCode() {
        return ObjectStoreBagCombination.INTERSECT;
    }

}
