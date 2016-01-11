package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.io.StringReader;

import org.intermine.api.InterMineAPI;
import org.intermine.api.query.NotPresentException;
import org.intermine.api.query.QueryStoreException;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

/**
 * A service that retrieves a stored query by qid.
 * queries.
 * @author Alex Kalderimis
 *
 */
public class QueryRetrieverService extends WebService
{

    /** @param im The InterMine state object **/
    public QueryRetrieverService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Format getDefaultFormat() {
        return Format.JSON;
    }

    @Override
    protected boolean canServe(Format format) {
        return (format == Format.JSON || format == Format.XML);
    }

    @Override
    protected String getDefaultFileName() {
        return "query";
    }

    @Override
    protected void postInit() {
        output = null;
    }

    @Override
    protected void execute() {
        String qid = getRequiredParameter("id");
        String xml;
        try {
            xml = im.getQueryStore().getQuery(qid);
        } catch (NotPresentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        } catch (QueryStoreException e) {
            throw new BadRequestException(e.getMessage());
        }
        PathQuery pq;
        try {
            pq = PathQueryBinding.unmarshalPathQuery(
                new StringReader(xml), PathQuery.USERPROFILE_VERSION);
        } catch (Exception e) {
            // Shouldn't happen. Never harms to check.
            throw new BadRequestException(e);
        }
        String ret = formatPathQuery(pq);
        PrintWriter pw = getRawOutput();
        pw.write(ret);
        pw.flush();
    }

    private String formatPathQuery(PathQuery pq) {
        switch(getFormat()) {
            case JSON:
                return pq.toJson();
            case XML:
                return pq.toXml();
            default:
                throw new IllegalStateException("Only JSON and XML supported");
        }
    }

}
