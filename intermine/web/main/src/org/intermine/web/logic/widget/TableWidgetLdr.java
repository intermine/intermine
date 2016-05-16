package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.util.PathUtil;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryHelper;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.widget.config.TableWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
/**
 * @author Xavier Watkins
 * @author Daniela Butano
 *
 */
public class TableWidgetLdr extends WidgetLdr
{
    private List<String> columns;
    private List<List<Object>> flattenedResults;
    private String title, description;
    private int widgetTotal = 0;
    private String pathString;
    private Model model;
    private String displayFields, exportField;
    private Path origPath;
    private String type;
    private TableWidgetConfig config;
    private ClassDescriptor cld;

    /**
     * This class loads and formats the data for the count
     * table widgets in the bag details page
     *
     * @param widgetConfig the configuration settings for this widget
     * @param bag bag for this widget
     * @param os the objectstore
     * @param ids intermine IDs, required if bag is NULL
     * @throws UnsupportedEncodingException if can't encode url
     */
    public TableWidgetLdr(WidgetConfig widgetConfig, InterMineBag bag, ObjectStore os, String ids)
        throws UnsupportedEncodingException {
        super(bag, os, null, widgetConfig, ids);
        this.config = (TableWidgetConfig) widgetConfig;
        pathString = config.getPathStrings();
        model = os.getModel();
        try {
            origPath = new Path(model, pathWithNoConstraints(pathString));
        } catch (PathException e) {
            throw new Error("Cannot create path for widget with path \"" + pathString
                    + "\" - check widget configuration");
        }
        cld = origPath.getLastClassDescriptor();
        type = cld.getUnqualifiedName();
        displayFields = config.getDisplayFields();
        exportField = config.getExportField();
        setFlattenedResults();
    }

    /** @return the class name of the path this table widget loads. **/
    public String getType() {
        return type;
    }

    /**
     * builds, runs query.  builds the results sets to be used to build widget
     * @throws UnsupportedEncodingException if url can't be encoded
     */
    public void setFlattenedResults() throws UnsupportedEncodingException {
        WebConfig webConfig = config.getWebConfig();
        Query q = getQuery(false, null);

        List<?> results;
        try {
            results = os.execute(q, 0, 5000, true, true, ObjectStore.SEQUENCE_IGNORE);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }

        columns = new ArrayList<String>();
        if ((displayFields != null) && (displayFields.length() != 0)) {
            String[] fieldArray = displayFields.split("[, ]+");
            for (int i = 0; i < fieldArray.length; i++) {
                String field = fieldArray[i];
                String newColumnName = type + "." + field;
                columns.add(newColumnName);
            }
        } else {
            for (FieldConfig fc : FieldConfigHelper.getClassFieldConfigs(webConfig, cld)) {
                if (!fc.getShowInResults()) {
                    continue;
                }
                String fieldExpr = fc.getFieldExpr();
                String newColumnName = type + "." + fieldExpr;
                columns.add(newColumnName);
            }
        }

        flattenedResults = new ArrayList<List<Object>>();
        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
            ArrayList<Object> flattenedRow = new ArrayList<Object>();
            ResultsRow<?> resRow = (ResultsRow<?>) iter.next();

            for (int i = 0; i < q.getSelect().size(); i++) {
                QuerySelectable select = q.getSelect().get(i);

                // this will be the last column
                if (select instanceof QueryFunction) {
                    flattenedRow.add(resRow.get(i));
                } else {
                    if (select instanceof QueryClass) {
                        InterMineObject o = (InterMineObject) resRow.get(i);
                        for (String columnName : columns) {
                            Path path;
                            Object fieldValue;
                            try {
                                path = new Path(model, columnName);
                                fieldValue = PathUtil.resolvePath(path, o);
                            } catch (PathException e) {
                                throw new Error("Cannot create path \"" + columnName
                                        + "\" for widget - check config");
                            }
                            flattenedRow.add(fieldValue);
                        }
                        flattenedRow.add(o.getId());
                    } else if (select instanceof QueryField) {
                        Object queryFieldObj = resRow.get(i);
                        if (queryFieldObj instanceof Integer) {
                            flattenedRow.add((Integer) queryFieldObj);
                        } else {
                            flattenedRow.add(String.valueOf(resRow.get(i)));
                        }
                    }
                }

            }
            flattenedResults.add(flattenedRow);
        }
        // Add the count column
        if (config.getColumnTitle() != null) {
            columns.add(config.getColumnTitle());
        } else {
            columns.add(bag.getType() + "s");
        }

        q = getQuery(true, null);
        widgetTotal = calcTotal(os, q);
    }

    /**
     * get the flattened results
     * @return the flattened results
     */
    public List<List<Object>> getFlattenedResults() {
        return flattenedResults;
    }

    /**
     * Get the columnNames
     * @return the columnNames
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * Get the title
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the description
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param calcTotal if we are generating the query to calculate the widget totals
     * @param keys constraints to add to the query
     * @return query representing data related to a table widget
     */
    public Query getQuery(boolean calcTotal, List<String> keys) {

        Query q = new Query();
        String[] queryBits = pathString.split("\\.");
        QueryClass qcStart = null;
        QueryField qfStartId = null;
        ClassDescriptor cldStart = origPath.getStartClassDescriptor();
        QueryClass qcExport = null;
        for (int i = 1; i < queryBits.length; i++) {

            if (qcStart == null) {
                qcStart = new QueryClass(cldStart.getType());
                qfStartId = new QueryField(qcStart, "id");
                qcExport = qcStart;
                q.addFrom(qcStart);
                QueryHelper.addAndConstraint(q, new BagConstraint(qfStartId, ConstraintOp.IN,
                            bag.getOsb()));
            }

            String refName;
            String constraintName = null, constraintValue = null;
            ConstraintOp constraintOp = null;
            // extra constraints have syntax Company.departments[name=DepartmentA].employees
            if (queryBits[i].indexOf('[') > 0) {
                String s = queryBits[i];
                refName = s.substring(0, s.indexOf('['));
                int startOp, endOp;
                if (s.indexOf("!=") != -1) {
                    startOp = s.indexOf("!=");
                    endOp = startOp + 2;
                    constraintOp = ConstraintOp.NOT_EQUALS;
                } else {
                    startOp = s.indexOf("=");
                    endOp = startOp + 1;
                    constraintOp = ConstraintOp.EQUALS;
                }
                constraintName = s.substring(s.indexOf('[') + 1, startOp);
                constraintValue = s.substring(endOp, s.indexOf(']'));
            } else {
                refName = queryBits[i];
            }

            FieldDescriptor fld = cldStart.getFieldDescriptorByName(refName);
            if (fld == null) {
                throw new IllegalArgumentException("Class '" + cldStart.getType() + "' has no '"
                                                   + refName + "' field");
            }
            if (fld.isAttribute()) {
                throw new IllegalArgumentException("path element not a reference/collection: "
                                                   + fld.getName());
            }
            ClassDescriptor cldEnd = ((ReferenceDescriptor) fld).getReferencedClassDescriptor();
            QueryClass qcEnd = new QueryClass(cldEnd.getType());

            addReferenceConstraint(q, qcStart, refName, qcEnd);

            if (constraintName != null && constraintValue != null) {
                AttributeDescriptor attFld = cldEnd.getAttributeDescriptorByName(constraintName);
                if (attFld == null) {
                    throw new IllegalArgumentException("Class '" + cldEnd.getType() + "' has no '"
                                                       + constraintName + "' field");
                }
                if (!"java.lang.String".equals(attFld.getType())) {
                    throw new IllegalArgumentException("Constraints can only be on String fields '"
                                                       + constraintName
                                                       + "' is a " + attFld.getType());
                }
                SimpleConstraint sc = new SimpleConstraint(new QueryField(qcEnd, constraintName),
                                                           constraintOp,
                                                           new QueryValue(constraintValue));
                QueryHelper.addAndConstraint(q, sc);
                constraintName = null;
                constraintValue = null;
            }

            QueryFunction qfCount = new QueryFunction();

            // if we are at the end of the path, add to select and group by
            //if (queryBits.length == (i + 1)) {
            if (cldEnd.equals(origPath.getLastClassDescriptor())) {
                if (keys != null) { // export
                    q.setDistinct(true);
                    QueryField keyField = new QueryField(qcEnd, getKeyField(displayFields));
                    BagConstraint bc = new BagConstraint(keyField, ConstraintOp.IN, keys);
                    QueryHelper.addAndConstraint(q, bc);
                    String[] fields = displayFields.split("[, ]+");
                    for (String field : fields) {
                        q.addToSelect(new QueryField(qcEnd, field));
                    }
                    q.addToSelect(new QueryField(qcExport, exportField));
                } else if (!calcTotal) {
                    Query mainQuery = new Query();
                    mainQuery.setDistinct(false);
                    Query subQ = q;
                    subQ.setDistinct(true);
                    subQ.addToSelect(qfStartId);
                    mainQuery.addFrom(subQ);
                    QueryField outerQfEnd = null;
                    if (origPath.endIsAttribute()) {
                        QueryField qfEnd = new QueryField(qcEnd, origPath.getLastElement());
                        subQ.addToSelect(qfEnd);
                        outerQfEnd = new QueryField(subQ, qfEnd);
                        //subQ.addToGroupBy(qfEnd);
                    } else {
                        String[] fields = displayFields.split("[, ]+");
                        for (String field : fields) {
                            QueryField qf = new QueryField(qcEnd, field);
                            subQ.addToSelect(qf);
                            outerQfEnd = new QueryField(subQ, qf);
                            mainQuery.addToSelect(outerQfEnd);
                            mainQuery.addToGroupBy(outerQfEnd);
                        }
                        QueryField qfId = new QueryField(qcEnd, "id");
                        subQ.addToSelect(qfId);
                        QueryField outerQfId = new QueryField(subQ, qfId);
                        mainQuery.addToSelect(outerQfId);
                        mainQuery.addToGroupBy(outerQfId);
                    }
                    mainQuery.addToSelect(qfCount);
                    mainQuery.addToOrderBy(qfCount, "desc");
                    return mainQuery;

                } else {
                    Query subQ = new Query();
                    subQ = q;
                    subQ.setDistinct(true);
                    subQ.clearSelect();
                    subQ.addToSelect(qfStartId);

                    q = new Query();
                    q.setDistinct(false);
                    q.addToSelect(qfCount);
                    q.addFrom(subQ);
                }
                // we've reached the end but if the end of the path was an attribute queryBits
                // still has one more element
                break;
            }
            cldStart = cldEnd;
            qcStart = qcEnd;
        }
        return q;
    }

    /**
     * Add a contains constraint to Query (q) from qcStart from qcEnd via reference refName.
     * Return qcEnd as it may need to be passed into mehod again as qcStart.
     *
     * @param q the query
     * @param qcStart the QueryClass that contains the reference
     * @param refName name of reference to qcEnd
     * @param qcEnd the target QueryClass of refName
     * to the query
     * @return QueryClass return qcEnd
     */
    private QueryClass addReferenceConstraint(Query q, QueryClass qcStart, String refName,
            QueryClass qcEnd) {
        q.addFrom(qcEnd);

        // already validated against model
        ClassDescriptor startCld = model.getClassDescriptorByName(qcStart.getType().getName());
        FieldDescriptor fd = startCld.getFieldDescriptorByName(refName);

        QueryReference qRef;
        if (fd.isReference()) {
            qRef = new QueryObjectReference(qcStart, refName);
        } else {
            qRef = new QueryCollectionReference(qcStart, refName);
        }
        ContainsConstraint cc = new ContainsConstraint(qRef, ConstraintOp.CONTAINS, qcEnd);
        QueryHelper.addAndConstraint(q, cc);

        return qcEnd;
    }

    /**
     * @param path the path to parse
     * @return a path with out any [] constraints
     */
    protected String pathWithNoConstraints(String path) {
        StringBuffer sb = new StringBuffer(path.length());
        String[] queryBits = path.split("\\.");
        for (int i = 0; i < queryBits.length; i++) {
            String refName = queryBits[i];
            if (refName.indexOf('[') > 0) {
                refName = refName.substring(0, refName.indexOf('['));
            }
            if (sb.length() > 0) {
                sb.append(".");
            }
            sb.append(refName);
        }
        return sb.toString();
    }

    /**
     * @return the total number of objects analysed in this widget
     */
    public int getWidgetTotal() {
        return widgetTotal;
    }

    private static int calcTotal(ObjectStore os, Query q) {
        Results res = os.execute(q);
        Iterator<?> iter = res.iterator();
        //Iterator iter = os.executeSingleton(q).iterator();
        int n = 0;
        while (iter.hasNext()) {
            ResultsRow<?> resRow = (ResultsRow<?>) iter.next();
            n = ((java.lang.Long) resRow.get(0)).intValue();
        }
        return n;
    }

    /**
     * @param selected selected records to export
     * @return list of lists of records to export
     * @throws Exception if something goes horribly wrong
     */
    public List<List<String>> getExportResults(String[] selected) throws Exception {

        List<List<String>> exportResults = new ArrayList<List<String>>();
        List<String> selectedIds = Arrays.asList(selected);
        Map<String, String> termToLabel = new HashMap<String, String>();
        Query q = getQuery(false, selectedIds);

        Results res = os.execute(q);
        Iterator<?> iter = res.iterator();
        HashMap<String, List<String>> termsToIds = new HashMap<String, List<String>>();
        boolean hasLabelColumn = false;
        while (iter.hasNext()) {
            ResultsRow<?> resRow = (ResultsRow<?>) iter.next();
            String term = resRow.get(0).toString();   // annotation (like go term)
            String id = null;
            if (resRow.size() > 2) {
                termToLabel.put(term, resRow.get(1).toString());
                id = resRow.get(2).toString();  // object identifier (like gene.identifier)
                hasLabelColumn = true;
            } else {
                id = resRow.get(1).toString();     // object identifier (like gene.identifier)
            }
            if (!termsToIds.containsKey(term)) {
                termsToIds.put(term, new ArrayList<String>());
            }
            termsToIds.get(term).add(id);
        }

        for (String term : selectedIds) {
            if (termsToIds.get(term) != null) {
                List<String> row = new LinkedList<String>();
                row.add(term);                    // identifier
                if (hasLabelColumn) {
                    row.add(termToLabel.get(term));     // label, like term name
                }
                List<String> ids = termsToIds.get(term);
                StringBuffer sb = new StringBuffer();
                for (String identifier : ids) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(identifier);
                }
                row.add(sb.toString());
                exportResults.add(row);
            }
        }
        return exportResults;
    }

    // TODO this is hacky.  assumes key field is first
    private String getKeyField(String s) {
        if (s.contains(",")) {
            String[] strings = s.split("[, ]+");
            return strings[0];
        }
        return s;
    }

    /**
     * Returns the pathquery based on the views set in config file and the bag constraint
     * Executed when the user selects any item in the matches column in the enrichment widget.
     * @return the query generated
     */
    public PathQuery createPathQuery() {
        PathQuery q = new PathQuery(model);
        String[] views = config.getViews().split("\\s*,\\s*");
        String prefix = config.getTypeClass();
        for (String view : views) {
            if (!view.startsWith(prefix)) {
                view = prefix  + "." + view;
            }
            q.addView(view);
        }
        q.addConstraint(Constraints.in(prefix, bag.getName()));

        String ps = config.getPathStrings();
        if (ps.contains("[") && ps.contains("]")) {
            //e.g.Gene.homologues[type=orthologue].homologue.organism
            String constraintPath, constraintValue;
            int startOp, endOp;
            if (ps.indexOf("!=") != -1) {
                startOp = ps.indexOf("!=");
                endOp = startOp + 2;
            } else {
                startOp = ps.indexOf("=");
                endOp = startOp + 1;
            }
            constraintPath = ps.substring(ps.indexOf('[') + 1, startOp); //e.g. type
            constraintValue = ps.substring(endOp, ps.indexOf(']')); //e.g. orthologue
            if (ps.contains("!=")) {
                q.addConstraint(Constraints.neq(ps.substring(0,
                    ps.indexOf('[')) + "." + constraintPath, constraintValue));
            } else {
                q.addConstraint(Constraints.neq(ps.substring(0,
                    ps.indexOf('[')) + "." + constraintPath, constraintValue));
            }
        }
        return q;
    }
}
