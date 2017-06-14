package org.intermine.webservice.server.clob;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.query.MainHelper;
import org.intermine.model.FastPathObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.NotImplementedException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.output.JSONFormatter;
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
 *   <li><em>query</em>: A query with a single selected path,
 *       which must resolve to a string or clob.
 *   </li>
 *   <li><em>start</em>: The index of the first character of the string or clob to return.</li>
 *   <li><em>end</em>: The index of the first character not to return.</li>
 * </ul>
 *
 * @author Alex Kalderimis
 *
 */
public class SequenceService extends JSONService
{

    private static final String EXPECTED_CHAR_SEQUENCE
        = "Expected the column to provide a CharSequence value, got: ";

    /** @param im The InterMine state object. **/
    public SequenceService(InterMineAPI im) {
        super(im);
    }

    /**
     * Why is the results key "features", pray? and not something more sensible, like,
     * "results", say. Well, this it just seemed sensible to make this service
     * directly consumable by <em>jbrowse</em>, which is obviously the whole point of this
     * service. <em>Sigh</em>.
     * @return The header attributes.
     */
    @Override
    protected Map<String, Object> getHeaderAttributes() {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.putAll(super.getHeaderAttributes());
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"features\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
        }
        return attributes;
    }

    @Override
    protected void execute() {
        Integer start = getIntParameter("start", 0);
        Integer end = getIntParameter("end", null);
        PathQuery pq = getQuery();

        Iterator<CharSequence> sequences = getSequences(pq);
        while (sequences.hasNext()) {
            CharSequence chars = sequences.next();
            addResultItem(makeFeature(chars, start, end), sequences.hasNext());
        }
    }

    private Iterator<CharSequence> getSequences(final PathQuery pq) {
        validateQuery(pq);
        BagQueryRunner bqr = im.getBagQueryRunner();
        final Query q;
        try {
            q = MainHelper.makeQuery(
                    pq,
                    getListManager().getListMap(),
                    new HashMap<String, QuerySelectable>(),
                    bqr,
                    new HashMap<String, BagQueryResult>());
        } catch (ObjectStoreException e) {
            throw new ServiceException(e);
        }

        final Iterator<Object> results = im.getObjectStore().executeSingleton(q).iterator();

        return new Iterator<CharSequence>() {

            @Override
            public boolean hasNext() {
                return results.hasNext();
            }

            @Override
            public CharSequence next() {
                FastPathObject obj = (FastPathObject) results.next();

                CharSequence chars;
                try {
                    chars = (CharSequence) obj.getFieldValue(
                            pq.makePath(pq.getView().get(0)).getEndFieldDescriptor().getName());
                } catch (IllegalAccessException e) {
                    throw new ServiceException(e);
                } catch (PathException e) {
                    throw new ServiceException(e);
                }
                return chars;
            }

            @Override
            public void remove() {
                throw new NotImplementedException(getClass(), "remove");
            }
        };

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
            throw new BadRequestException(EXPECTED_CHAR_SEQUENCE + column.getEndType());
        }
    }

    private Map<String, Object> makeFeature(CharSequence chars, Integer start, Integer end) {
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
        Map<String, Object> feat = new HashMap<String, Object>();
        feat.put("start", start);
        feat.put("end", subSequence.length() + start);
        // Have to do this, otherwise json.org goes insane.
        feat.put("seq", String.valueOf(subSequence));

        return feat;
    }

    private PathQuery getQuery() {
        String xml           = new QueryRequestParser(im.getQueryStore(), request).getQueryXml();
        String schemaUrl     = AbstractQueryService.getSchemaLocation(request);
        PathQueryBuilder bdr = new PathQueryBuilder(xml, schemaUrl, getListManager());

        return bdr.getQuery();
    }

}
