package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.BagNotFound;
import org.intermine.api.query.MainHelper;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ResultCell;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.core.Either;
import org.intermine.webservice.server.core.EitherVisitor;
import org.intermine.webservice.server.core.Page;
import org.intermine.webservice.server.core.SubTable;
import org.intermine.webservice.server.core.TableRowIterator;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.output.TableCellFormatter;
import org.json.JSONArray;

/**
 * A service that produces results in nested rows for use in tables.
 * @author Alex Kalderimis
 *
 */
public class TableRowService extends QueryResultService
{

    /** @param im The InterMine state object **/
    public TableRowService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Format getDefaultFormat() {
        return Format.JSON;
    }

    @Override
    protected boolean canServe(Format format) {
        return format == Format.JSON;
    }

    @Override
    protected void setHeaderAttributes(PathQuery pq, Integer start, Integer size) {
        try {
            Profile p = getPermission().getProfile();
            PathQueryExecutor pqe = im.getPathQueryExecutor(p);
            int count = pqe.count(pq);
            attributes.put("iTotalRecords", count);
        } catch (BagNotFound e) {
            throw new BadRequestException(e.getMessage());
        } catch (ObjectStoreException e) {
            throw new ServiceException("Error counting rows.", e);
        }
        super.setHeaderAttributes(pq, start, size);
    }

    @Override
    public void runPathQuery(PathQuery pathQuery, int firstResult, int maxResults) {
        final ObjectStore os = im.getObjectStore();
        final Profile p = getPermission().getProfile();
        final Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
        Query q;
        try {
            q = MainHelper.makeQuery(
                    pathQuery,
                    im.getBagManager().getCurrentBags(p),
                    pathToQueryNode,
                    im.getBagQueryRunner(),
                    new HashMap<String, BagQueryResult>());
        } catch (ObjectStoreException e) {
            throw new ServiceException("Could not run query", e);
        }
        final Results results = os.execute(q, QueryResultService.BATCH_SIZE, true, false, false);
        final Page page = new Page(firstResult, (maxResults == 0) ? null : maxResults);

        Query realQ = results.getQuery();
        if (realQ == q) {
            getPathQueryExecutor().updateQueryToPathToQueryNode(q, pathToQueryNode);
        }

        TableRowIterator iter = new TableRowIterator(
                pathQuery, q, results, pathToQueryNode, page, im);

        final Processor processor = new Processor(im);

        while (iter.hasNext()) {
            List<Map<String, Object>> rowdata = new LinkedList<Map<String, Object>>();
            for (Either<ResultCell, SubTable> cell: iter.next()) {
                rowdata.add(cell.accept(processor));
            }
            JSONArray ja = new JSONArray(rowdata);
            if (iter.hasNext()) {
                output.addResultItem(Arrays.asList(ja.toString(), ""));
            } else {
                output.addResultItem(Arrays.asList(ja.toString()));
            }
        }
    }

    private static final class Processor
        extends EitherVisitor<ResultCell, SubTable, Map<String, Object>>
    {
        private static final String CELL_KEY_COLUMN = "column";
        private static final String CELL_KEY_VIEW = "view";
        private static final String CELL_KEY_ROWS = "rows";

        private TableCellFormatter tableCellFormatter;

        Processor(InterMineAPI im) {
            this.tableCellFormatter = new TableCellFormatter(im);
        }

        @Override
        public Map<String, Object> visitLeft(ResultCell a) {
            return tableCellFormatter.toMap(a);
        }

        @Override
        public Map<String, Object> visitRight(SubTable b) {
            Map<String, Object> cell = new HashMap<String, Object>();
            cell.put(CELL_KEY_COLUMN, b.getJoinPath().toStringNoConstraints());
            cell.put(CELL_KEY_VIEW,
                    CollectionUtils.collect(b.getColumns(),
                            InvokerTransformer.getInstance("toStringNoConstraints")));
            List<List<Map<String, Object>>> rows = new ArrayList<List<Map<String, Object>>>();
            cell.put(CELL_KEY_ROWS, rows);
            for (List<Either<ResultCell, SubTable>> items: b.getRows()) {
                List<Map<String, Object>> row = new ArrayList<Map<String, Object>>();
                rows.add(row);
                for (Either<ResultCell, SubTable> item: items) {
                    row.add(item.accept(this));
                }
            }
            return cell;
        }
    }
}
