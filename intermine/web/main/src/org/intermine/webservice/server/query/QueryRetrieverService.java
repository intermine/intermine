package org.intermine.webservice.server.query;

import java.io.PrintWriter;
import java.io.StringReader;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.query.KeyFormatException;
import org.intermine.api.query.NotPresentException;
import org.intermine.api.query.QueryStoreException;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;

public class QueryRetrieverService extends WebService {

    private static Logger LOG = Logger.getLogger(QueryRetrieverService.class);

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
    protected void execute() throws ServiceException {
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
