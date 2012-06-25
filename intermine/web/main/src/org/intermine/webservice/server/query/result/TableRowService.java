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
import org.intermine.api.query.MainHelper;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.PortalHelper;
import org.intermine.webservice.server.WebServiceInput;
import org.intermine.webservice.server.core.Either;
import org.intermine.webservice.server.core.EitherVisitor;
import org.intermine.webservice.server.core.Page;
import org.intermine.webservice.server.core.SubTable;
import org.intermine.webservice.server.core.TableCell;
import org.intermine.webservice.server.core.TableRowIterator;
import org.intermine.webservice.server.exceptions.InternalErrorException;
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
        
        TableRowIterator iter = new TableRowIterator(pathQuery, results,
                pathToQueryNode, new Page(firstResult, (maxResults == 0) ? null : maxResults));
        
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
        private static final String CELL_KEY_URL = "url";
        private static final String CELL_KEY_VALUE = "value";
        private static final String CELL_KEY_CLASS = "class";
        private static final String CELL_KEY_ID = "id";
        private static final String CELL_KEY_COLUMN = "column";
        private static final String CELL_KEY_VIEW = "view";
        private static final String CELL_KEY_ROWS = "rows";
        
        private final InterMineAPI im;
        
        Processor(InterMineAPI im) {
            this.im = im;
        }
        
        @Override
        public Map<String, Object> visitLeft(TableCell a) {
            Map<String, Object> cell = new HashMap<String, Object>();
            cell.put(CELL_KEY_VALUE, a.getValue());
            cell.put(CELL_KEY_ID, a.getId());
            cell.put(CELL_KEY_CLASS, a.getType());
            cell.put(CELL_KEY_COLUMN, a.getColumn().toStringNoConstraints());
            String link;
            if (im.getLinkRedirector() != null && a.getObject() instanceof InterMineObject) {
                link = im.getLinkRedirector().generateLink(im, (InterMineObject) a.getObject());
            } else {
                // Convert to ResultElement which the portal helper understands.
                // TODO: unify TableCell and ResultElement!!
                List<FieldDescriptor> keyFields = im.getClassKeys().get(a.getType());
                boolean isKeyField; 
                if (keyFields == null) {
                    isKeyField = false;
                } else {
                    isKeyField = keyFields.contains(a.getColumn().getEndFieldDescriptor());
                }
                ResultElement re = new ResultElement(a.getObject(), a.getColumn(), isKeyField);
                link = PortalHelper.generateReportPath(re);
            }
            cell.put(CELL_KEY_URL, link);
            return cell;
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
