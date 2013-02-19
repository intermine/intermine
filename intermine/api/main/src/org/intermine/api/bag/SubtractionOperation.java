package org.intermine.api.bag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ObjectStoreBagCombination;

public class SubtractionOperation extends BagOperation
{

    private InterMineBag left;
    private InterMineBag right;

    public SubtractionOperation(
        Model model, InterMineBag from, Collection<InterMineBag> exclude, Profile profile) {
        super(model, headAndTail(from, exclude), profile);
        this.left = from;
    }

    @Override
    protected String getNewBagType() throws MetaDataException {
        return left.getType();
    }

    @Override
    protected int getOperationCode() {
        return ObjectStoreBagCombination.EXCEPT;
    }

    private static Collection<InterMineBag> headAndTail(InterMineBag head, Collection<InterMineBag> tail) {
        List<InterMineBag> ret = new ArrayList<InterMineBag>();
        ret.add(head);
        ret.addAll(tail);
        return ret;
    }
}
