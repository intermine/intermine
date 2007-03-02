package org.intermine.web.widget;

/*
 * Copyright (C) 2002-2005 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.ClassKeyHelper;
import org.intermine.web.MainHelper;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.config.FieldConfig;
import org.intermine.web.config.FieldConfigHelper;
import org.intermine.web.config.WebConfig;
import org.intermine.web.results.ResultElement;

/**
 * @author Xavier Watkins
 *
 */
public class BagTableWidgetLoader
{
    private Results results;
    private LinkedList columns;
    private LinkedList flattenedResults;
    private String title;
    
    /**
     * This class loads and formats the data for the count
     * table widgets in the bag details page
     * 
     * @param type The type to do the count on
     * @param collectionName the name of the collection corresponding to the
     * bag type
     * @param reverseCollection the name oof the corresponding collection
     * for the bag type
     * @param bag the bag
     * @param os the objectstore
     * @param webConfig the webConfig
     * @param model the model
     * @param classKeys the classKeys
     */
    public BagTableWidgetLoader(String title, String type, String collectionName, InterMineBag bag,
        ObjectStore os, WebConfig webConfig, Model model, Map classKeys, String fields) {
        this.title = title;
        Query q = new Query();
        
        Class clazzA = null;
        Class clazzB = null;
        try {
            clazzA = Class.forName(model.getPackageName() + "." + type);
            clazzB = Class.forName(model.getPackageName() + "." + bag.getType());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        
        QueryClass qClassA = new QueryClass(clazzA);
        QueryClass qClassB = new QueryClass(clazzB);
        
        q.addFrom(qClassA);
        q.addFrom(qClassB);

        QueryFunction count = new QueryFunction();
        
        q.addToSelect(qClassA);
        q.addToSelect(count);
        
        
        ConstraintSet cstSet = new ConstraintSet(ConstraintOp.AND);
        QueryCollectionReference qcr = new QueryCollectionReference(qClassB, collectionName);
        ContainsConstraint cstr = new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qClassA);
        cstSet.addConstraint(cstr);
        
        QueryField qf = new QueryField(qClassB, "id");
        BagConstraint bagCstr = new BagConstraint(qf, ConstraintOp.IN, bag.getListOfIds());
        cstSet.addConstraint(bagCstr);
        
        q.setConstraint(cstSet);
        
        q.addToGroupBy(qClassA);
        q.addToOrderBy(count);
        
        results = new Results(q, os, os.getSequence());
        ClassDescriptor cld = MainHelper.getClassDescriptor(type, model);
        columns = new LinkedList();
        
        if (fields!=null && fields.length() != 0) {
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
        flattenedResults = new LinkedList();
        for (Iterator iter = results.iterator(); iter.hasNext();) {
            ArrayList flattenedRow = new ArrayList();
            ResultsRow resRow = (ResultsRow) iter.next();
            Integer lastObjectId = null;
            for (Iterator iterator = resRow.iterator(); iterator.hasNext();) {
                Object element = (Object) iterator.next();
                if (element instanceof InterMineObject) {
                    InterMineObject o = (InterMineObject) element;
                    for (Iterator iterator3 = columns.iterator(); iterator3.hasNext();) {
                        String columnName = (String) iterator3.next();
                        Path path = new Path(model, columnName);
                        Object fieldValue = path.resolve(o);
                        String thisType = TypeUtil.unqualifiedName(path.getStartClassDescriptor()
                            .getName());
                        String fieldName = path.getEndFieldDescriptor().getName();
                        boolean isKeyField = ClassKeyHelper.isKeyField(classKeys, thisType,
                                                                       fieldName);
                        flattenedRow.add(new ResultElement(os, fieldValue, o.getId(), thisType,
                                                       path, isKeyField));
                    }
                    lastObjectId = o.getId();
                } else if (element instanceof Long) {
                    flattenedRow.add(new ResultElement(String.valueOf((Long) element),
                                                           "bagName="
                                                                           + bag.getName()
                                                                           + "&typeB="
                                                                           + type
                                                                           + "&typeA="
                                                                           + bag.getType()
                                                                           + "&reverseCollection="
                                                                           + collectionName
                                                                           + "&id="
                                                                           + lastObjectId
                                                                               .toString()));
                } 

            }
            flattenedResults.add(flattenedRow);
        }
        // Add the count column
        columns.add(bag.getType());
    }
    
    /**
     * get the flattened results
     * @return the flattened results
     */
    public List getFlattenedResults() {
        LinkedList reverseResults = new LinkedList();
        int i = 0;
        // The following code is needed to reverse sort
        // because there is no asc/desc choice in the IM query
        ListIterator iter = flattenedResults.listIterator();
        while (iter.hasNext()) {
            iter.next();
            // Move iterator to the end
        }
        while (iter.hasPrevious()) {
            reverseResults.add((ArrayList) iter.previous());
            if (i>=9){
                return reverseResults;
            }
            i++;
        }
        return reverseResults;
    }
    
    /**
     * Get the columnNames
     * @return the columnNames
     */
    public List getColumns() {
        return columns;
    }
    
    public String getTitle() {
        return title;
    }
}
