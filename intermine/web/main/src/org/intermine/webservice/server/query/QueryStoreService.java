package org.intermine.webservice.server.query;

import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.core.JSONService;

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
        int id = QueryStore.putQuery(xml);
        this.addResultValue(id, false);
    }

}
