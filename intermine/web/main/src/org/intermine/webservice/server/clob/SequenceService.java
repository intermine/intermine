package org.intermine.webservice.server.clob;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.query.MainHelper;
import org.intermine.model.FastPathObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.query.AbstractQueryService;
import org.intermine.webservice.server.query.QueryRequestParser;
import org.intermine.webservice.server.query.result.PathQueryBuilder;

/**
 * <p>A service to provide access to substrings of <code>ClobAccess</code> data. Ideally this
 * could be replaced by enhancing path queries to provide access to functions
 * such as <code>SUBSTR</code>.</p>
 * 
 * <p>This service expects the following parameters:</p>
 * <ul>
 *   <li><em>query</em>: A query with a single selected path, which must resolve to a string or clob.</li>
 *   <li><em>start</em>: The index of the first character of the string or clob to return.</li>
 *   <li><em>end</em>: The index of the first character not to return.</li>
 * </ul>
 * 
 * @author Alex Kalderimis
 *
 */
public class SequenceService extends JSONService {

    public SequenceService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getResultsKey() {
        return "results";
    }

    @Override
    protected void execute() {
        Integer start = getIntParameter("start", 0);
        Integer end = getIntParameter("end", null);
        PathQuery pq = getQuery();
        CharSequence chars = getSequence(pq);

        serveSubSequence(chars, start, end);
    }

    private CharSequence getSequence(PathQuery pq) {
        validateQuery(pq);
        Query q;
        try {
            q = MainHelper.makeQuery(pq, getListManager().getListMap(), new HashMap(), null, new HashMap());
        } catch (ObjectStoreException e) {
            throw new InternalErrorException(e);
        }

        Iterator<Object> results = im.getObjectStore().executeSingleton(q).iterator();

        FastPathObject obj = (FastPathObject) results.next();

        if (obj == null || results.hasNext()) {
            String msg = (obj == null) ? "empty" : "not unique";
            throw new BadRequestException("Results are " + msg);
        }

        CharSequence chars;
        try {
            chars = (CharSequence) obj.getFieldValue(pq.makePath(pq.getView().get(0)).getEndFieldDescriptor().getName());
        } catch (IllegalAccessException e) {
            throw new InternalErrorException(e);
        } catch (PathException e) {
            throw new InternalErrorException(e);
        }
        return chars;
    }

    private void validateQuery(PathQuery pq) {
        List<String> view = pq.getView();
        if (view.size() != 1) {
            throw new BadRequestException("Expected only a single view column, got: " + view);
        }
        Path column;
        try {
            column = pq.makePath(view.get(0));
        } catch (PathException e) {
            throw new BadRequestException(e);
        }
        if (!CharSequence.class.isAssignableFrom(column.getEndType())) {
            throw new BadRequestException("Expected the column to provide a CharSequence value, got: " + column.getEndType());
        }
    }

    private void serveSubSequence(CharSequence chars, Integer start, Integer end) {
        CharSequence subSequence;
        try {
            if (end == null) {
                subSequence = chars.subSequence(start, chars.length());
            } else {
                subSequence = chars.subSequence(start, end);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new BadRequestException("Illegal start/end values: " + e.getMessage());
        }
        addResultValue(subSequence, false);
    }

    private PathQuery getQuery() {
        String xml                     = new QueryRequestParser(im.getQueryStore(), request).getQueryXml();
        String schemaUrl               = AbstractQueryService.getSchemaLocation(request);
        Map<String, InterMineBag> bags = getListManager().getListMap();
        PathQueryBuilder builder       = new PathQueryBuilder(xml, schemaUrl, bags);

        return builder.getQuery();
    }

}
