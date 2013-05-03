package org.intermine.webservice.server.lists;


import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.operations.Intersection;

public class ListIntersectionService extends ListOperationService {

    public ListIntersectionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Intersection getOperation(ListInput input) {
        return new Intersection(im.getModel(), getPermission().getProfile(), input.getLists());
    }

}
