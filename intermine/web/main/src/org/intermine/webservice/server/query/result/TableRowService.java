package org.intermine.webservice.server.query.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.BagNotFound;
import org.intermine.api.query.MainHelper;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.WebServiceInput;
import org.intermine.webservice.server.core.Either;
import org.intermine.webservice.server.core.EitherVisitor;
import org.intermine.webservice.server.core.Page;
import org.intermine.webservice.server.core.SubTable;
import org.intermine.webservice.server.core.TableCell;
import org.intermine.webservice.server.core.TableRowIterator;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.output.TableCellFormatter;
import org.json.JSONArray;

public class TableRowService extends QueryResultService
{
    private static final Logger LOG = Logger.getLogger(TableRowService.class);

    public TableRowService(InterMineAPI im) {
        super(im);
    }


    @Override
    protected int getDefaultFormat() {
        return JSON_FORMAT;
    }
    
    @Override
    protected void setHeaderAttributes(PathQuery pq, Integer start, Integer size) {
        try {
            Profile p = getPermission().getProfile();
            Query q = MainHelper.makeQuery(pq, p.getCurrentSavedBags(),
                    new HashMap(), im.getBagQueryRunner(), new HashMap());
            ObjectStore os = im.getObjectStore();
            int count = os.count(q, new HashMap());
            attributes.put("iTotalRecords", count);
        } catch (BagNotFound e) {
            throw new BadRequestException(e.getMessage());
        } catch (ObjectStoreException e) {
            throw new InternalErrorException("Error counting rows.", e);
        }
        super.setHeaderAttributes(pq, start, size);
    }

    @Override
    public void runPathQuery(PathQuery pathQuery, int firstResult,
            int maxResults, String title, String description,
            WebServiceInput input, String mineLink, String layout) {
        ObjectStore os = im.getObjectStore();
        Profile p = getPermission().getProfile();
        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
        Query q;
        try {
            q = MainHelper.makeQuery(pathQuery, p.getCurrentSavedBags(),
                    pathToQueryNode, im.getBagQueryRunner(), new HashMap());
        } catch (ObjectStoreException e) {
            throw new InternalErrorException("Could not run query", e);
        }
        Results results = os.execute(q, QueryResultService.BATCH_SIZE, true, false, false);
        Page page = new Page(firstResult, (maxResults == 0) ? null : maxResults);
        
        TableRowIterator iter = new TableRowIterator(pathQuery, results, pathToQueryNode, page, im);
        
        final Processor processor = new Processor(im);
        
        while (iter.hasNext()) {
            List<Map<String, Object>> rowdata = new LinkedList<Map<String, Object>>();
            for (Either<TableCell, SubTable> cell: iter.next()) {
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
    
    private static final class Processor extends EitherVisitor<TableCell, SubTable, Map<String, Object>>
    {
        private static final String CELL_KEY_COLUMN = "column";
        private static final String CELL_KEY_VIEW = "view";
        private static final String CELL_KEY_ROWS = "rows";

        private TableCellFormatter tableCellFormatter;

        Processor(InterMineAPI im) {
            this.tableCellFormatter = new TableCellFormatter(im);
        }

        @Override
        public Map<String, Object> visitLeft(TableCell a) {
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
            for (List<Either<TableCell, SubTable>> items: b.getRows()){
                List<Map<String, Object>> row = new ArrayList<Map<String, Object>>();
                rows.add(row);
                for (Either<TableCell, SubTable> item: items) {
                    row.add(item.accept(this));
                }
            }
            return cell;
        }
    }
}
