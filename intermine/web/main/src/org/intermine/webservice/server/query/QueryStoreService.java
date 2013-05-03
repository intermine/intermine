package org.intermine.webservice.server.query;

import org.intermine.api.InterMineAPI;
import org.intermine.api.query.BadQueryException;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;

public class QueryStoreService extends JSONService {

    public QueryStoreService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getResultsKey() {
        return "id";
    }

    @Override
    protected void execute() throws Exception {
        String xml = getRequiredParameter("query");
        String id;
        try {
            id = im.getQueryStore().putQuery(xml);
        } catch (BadQueryException e) {
            throw new BadRequestException(e.getMessage());
        }
        this.addResultValue(id, false);
    }

}
