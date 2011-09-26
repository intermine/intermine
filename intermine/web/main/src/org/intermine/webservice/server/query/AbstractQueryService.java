package org.intermine.webservice.server.query;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.ListManager;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.query.result.PathQueryBuilder;
import org.intermine.webservice.server.query.result.PathQueryBuilderForJSONObj;

public abstract class AbstractQueryService extends WebService {

    private static final String XML_SCHEMA_LOCATION = "webservice/query.xsd";

    public AbstractQueryService(InterMineAPI im) {
        super(im);
    }

    protected String getXMLSchemaUrl() {
        try {
            String relPath = request.getContextPath() + "/"
                    + XML_SCHEMA_LOCATION;
            URL url = new URL(request.getScheme(), request.getServerName(),
                    request.getServerPort(), relPath);
            return url.toString();
        } catch (MalformedURLException e) {
            throw new InternalErrorException(e);
        }
    }

    protected PathQueryBuilder getQueryBuilder(String xml, HttpServletRequest req) {
        ListManager listManager = new ListManager(req);

        Map<String, InterMineBag> savedBags = new HashMap<String, InterMineBag>();
        for (InterMineBag bag: listManager.getLists()) {
            savedBags.put(bag.getName(), bag);
        }

        if (formatIsJsonObj()) {
            return new PathQueryBuilderForJSONObj(xml, getXMLSchemaUrl(),
                    savedBags);
        } else {
            return new PathQueryBuilder(xml, getXMLSchemaUrl(), savedBags);
        }
    }

}
