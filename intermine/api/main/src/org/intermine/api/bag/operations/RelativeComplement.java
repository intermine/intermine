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

import java.util.Collection;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ObjectStoreBagCombination;

import static java.util.Arrays.asList;
import static org.intermine.metadata.DescriptorUtils.findSumType;
import static org.intermine.metadata.DescriptorUtils.findIntersectionType;;

public class RelativeComplement extends BagOperation
{

    private Collection<InterMineBag> excluded;

    public RelativeComplement(
        Model model, Profile profile, Collection<InterMineBag> froms,
            Collection<InterMineBag> exclude) {
        super(model, profile, froms);
        this.excluded = exclude;
    }

    @Override
    public String getNewBagType() throws IncompatibleTypes {
        try {
            ClassDescriptor leftType = findSumType(getClasses());
            // We have to check these individually, because of multiple inheritance on the
            // left. We could, for example have Employees on the left, and subtract Things and
            // HasAddresses from them, even though there is no common type of Thing and
            // HasAddress.
            for (InterMineBag bag: excluded) {
                // Just check that it makes sense to subtract the rights from the lefts,
                // ie. that there is some kind of common type here at all.
                findIntersectionType(asList(leftType,
                        model.getClassDescriptorByName(bag.getType())));
            }
            // But in all cases, the final type is the left type.
            return leftType.getUnqualifiedName();
        } catch (MetaDataException e) {
            throw new IncompatibleTypes(e);
        }
    }

    @Override
    protected ObjectStoreBagCombination combineBags() {
        ObjectStoreBagCombination leftUnion =
                new ObjectStoreBagCombination(ObjectStoreBagCombination.UNION);
        for (InterMineBag bag : getBags()) {
            leftUnion.addBag(bag.getOsb());
        }
        ObjectStoreBagCombination osbc = new ObjectStoreBagCombination(getOperationCode());
        osbc.addBagCombination(leftUnion);
        for (InterMineBag bag : excluded) {
            osbc.addBag(bag.getOsb());
        }
        return osbc;
    }

    @Override
    protected int getOperationCode() {
        return ObjectStoreBagCombination.EXCEPT;
    }

}
