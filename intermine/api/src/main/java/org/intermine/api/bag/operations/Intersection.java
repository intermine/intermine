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

import java.util.Collection;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.DescriptorUtils;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ObjectStoreBagCombination;

/**
 *
 * @author Alex
 *
 */
public class Intersection extends BagOperation
{

    /**
     *
     * @param model data model
     * @param profile user profile
     * @param bags lists to intersect
     */
    public Intersection(Model model, Profile profile, Collection<InterMineBag> bags) {
        super(model, profile, bags);
    }

    @Override
    public String getNewBagType() throws IncompatibleTypes {
        try {
            return DescriptorUtils.findIntersectionType(getClasses()).getUnqualifiedName();
        } catch (MetaDataException e) {
            throw new IncompatibleTypes(e);
        }
    }

    @Override
    protected int getOperationCode() {
        return ObjectStoreBagCombination.INTERSECT;
    }

}
