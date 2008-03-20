  package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryHelper;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.query.MainHelper;

/**
 * @author Xavier Watkins
 *
 */
public class BagTableWidgetLoader
{
    private List<String> columns;
    private List flattenedResults;
    private String title, description;

    /**
     * This class loads and formats the data for the count
     * table widgets in the bag details page
     *
     * @param pathStrings the path to group the objects by, ie Employee.city will return the list of
     * employees by city
     * @param bag the bag
     * @param os the objectstore
     * @param webConfig the webConfig
     * @param model the model
     * @param classKeys the classKeys
     * @param fields fields involved in widget
     * @param urlGen the class that generates the pathquery used in the links from the widget
     * @throws ClassNotFoundException if some class in the widget paths is not found
     */
    public BagTableWidgetLoader(String pathStrings, InterMineBag bag, ObjectStore os, 
                                WebConfig webConfig, Model model, Map classKeys, 
                                String fields, String urlGen) 
    throws ClassNotFoundException {
        
        Query q = constructQuery(model, bag, pathStrings);

        List results;
        try {
            results = os.execute(q, 0, 10, true, true, ObjectStore.SEQUENCE_IGNORE);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
        ClassDescriptor cld;
        String[] s = pathStrings.split("\\.");
        String type = s[s.length - 1];
        try {
            cld = MainHelper.getClassDescriptor(type, model);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("unexpected exception", e);
        }
        columns = new ArrayList<String>();
        if ((fields != null) && (fields.length() != 0)) {
            String[] fieldArray = fields.split(",");
            for (int i = 0; i < fieldArray.length; i++) {
                String field = fieldArray[i];
                String newColumnName = type + "." + field;
                columns.add(newColumnName);
            }
        } else {
            List cldFieldConfigs = FieldConfigHelper.getClassFieldConfigs(webConfig, cld);
            for (Iterator iter = cldFieldConfigs.iterator(); iter.hasNext();) {
                FieldConfig fc = (FieldConfig) iter.next();
                if (!fc.getShowInResults()) {
                    continue;
                }
                String fieldExpr = fc.getFieldExpr();
                String newColumnName = type + "." + fieldExpr;
                columns.add(newColumnName);
            }
        }
        
        
        flattenedResults = new ArrayList<ArrayList>();
        for (Iterator iter = results.iterator(); iter.hasNext();) {
            ArrayList<String[]> flattenedRow = new ArrayList<String[]>();
            ResultsRow resRow = (ResultsRow) iter.next();
            //Integer lastObjectId = null;
            String key = "";
            for (Iterator iterator = resRow.iterator(); iterator.hasNext();) {
                Object element = iterator.next();
                if (element instanceof InterMineObject) {
                    InterMineObject o = (InterMineObject) element;
                    for (Iterator iterator3 = columns.iterator(); iterator3.hasNext();) {
                        String columnName = (String) iterator3.next();
                        Path path = new Path(model, columnName);
                        Object fieldValue = path.resolve(o);
                        Class thisType = path.getStartClassDescriptor().getType();
                        String fieldName = path.getEndFieldDescriptor().getName();
                        boolean isKeyField = ClassKeyHelper.isKeyField(classKeys,
                                TypeUtil.unqualifiedName(thisType.getName()), fieldName);
                        String link = null;
                        if (isKeyField) {
                            key = fieldValue.toString();
                            link = "objectDetails.do?id=" + o.getId() + "&amp;trail=|bag."
                                   + bag.getName() + "|" + o.getId();
                        }
                        flattenedRow.add(new String[]
                            {
                                (String) fieldValue, link
                            });
                    }
                } else if (element instanceof Long) {
                    flattenedRow.add(new String[]
                            {
                                String.valueOf((Long) element),
                                "widgetAction.do?bagName=" + bag.getName() + "&link=" + urlGen
                                                + "&key=" + key
                            });
                }

            }
            flattenedResults.add(flattenedRow);
        }
        // Add the count column
        columns.add(bag.getType() + "s");
    }

    /**
     * get the flattened results
     * @return the flattened results
     */
    public List getFlattenedResults() {
        return flattenedResults;
    }

    /**
     * Get the columnNames
     * @return the columnNames
     */
    public List getColumns() {
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
     * Construct an objectstore query represented by the given path.
     * @param model the Model use to find meta data
     * @param paths path to construct query for
     * @param bag the bag for this widget
     * @return the constructed query
     * @throws ClassNotFoundException if problem processing path
     * @throws IllegalArgumentException if problem processing path
     */
    private Query constructQuery(Model model, InterMineBag bag, String pathStrings)
        throws ClassNotFoundException, IllegalArgumentException {
        
        String[] paths = pathStrings.split(","); 
        
        Query q = new Query();
        boolean first = true;
        QueryClass qcStart = null;
        for (String path : paths) {
        
            String[] queryBits = path.split("\\.");

            // validate path against model
            //PathQueryUtil.validatePath(path, model);
            
            QueryClass qcLast = null;
            
            for (int i = 0; i + 2 < queryBits.length; i += 2) {
                qcStart = new QueryClass(Class.forName(model.getPackageName()
                                                                  + "." + queryBits[i]));
                String refName = queryBits[i + 1];
                QueryClass qcEnd = new QueryClass(Class.forName(model.getPackageName()
                                                                + "." + queryBits[i + 2]));
                if (qcLast != null) {
                    qcStart = qcLast;
                }
                qcLast = addReferenceConstraint(model, q, qcStart, refName, qcEnd, first);
                first = false;
            }
        }
        
        BagConstraint bc = new BagConstraint(new 
                           QueryField(qcStart, "id"), ConstraintOp.IN, bag.getOsb());
        QueryHelper.addConstraint(q, bc); 
        return q;
    }

    /**
     * Add a contains constraint to Query (q) from qcStart from qcEnd via reference refName.
     * Return qcEnd as it may need to be passed into mehod again as qcStart.
     * @param model the Model use to find meta data
     * @param q the query
     * @param qcStart the QueryClass that contains the reference
     * @param refName name of reference to qcEnd
     * @param qcEnd the target QueryClass of refName
     * @param first true if this is the first constraint added - qcStart needs to be added
     * to the query
     * @return QueryClass return qcEnd
     */
    private QueryClass addReferenceConstraint(Model model, Query q, QueryClass qcStart, 
                                              String refName, QueryClass qcEnd, boolean first) {

        q.addToSelect(qcEnd);
        q.addFrom(qcEnd);
        q.addToGroupBy(qcEnd);
        
        if (first) {            
            QueryFunction qf = new QueryFunction();
            q.addToSelect(qf);
            q.addFrom(qcStart);
            q.addToOrderBy(qf, "desc");
        }
        
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
        QueryHelper.addConstraint(q, cc);

        return qcEnd;
    }
    
}
  