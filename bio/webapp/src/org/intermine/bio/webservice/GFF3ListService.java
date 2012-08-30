package org.intermine.bio.webservice;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.exceptions.BadRequestException;

public class GFF3ListService extends GFFQueryService
{

    private static final String LIST_PARAM = "list";

    public GFF3ListService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected PathQuery getQuery() {
        InterMineBag list = getList();
        PathQuery pq = new PathQuery(im.getModel());
        pq.addView(list.getType() + ".primaryIdentifier");
        pq.addConstraint(Constraints.in(list.getType(), list.getName()));
        return pq;
    }

    private InterMineBag getList() {
        String listName = request.getParameter(LIST_PARAM);
        if (StringUtils.isEmpty(listName)) {
            throw new BadRequestException("missing list name parameter");
        }
        InterMineBag list = null;
        if (this.isAuthenticated()) {
            Profile p = SessionMethods.getProfile(request.getSession());
            list = im.getBagManager().getBag(p, listName);
        } else {
            list = im.getBagManager().getGlobalBag(listName);
        }
        if (list == null) {
            throw new BadRequestException("Cannot access a list called" + listName);
        }
        return list;
    }

}
