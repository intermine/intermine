package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.OrderDescending;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.MainHelper;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.results.ResultElement;

/**
 * @author Xavier Watkins
 *
 */
public class BagTableWidgetLoader
{
    private List columns;
    private List flattenedResults;
    private String title, description;

    /**
     * This class loads and formats the data for the count
     * table widgets in the bag details page
     *
     * @param type The type to do the count on
     * @param collectionName the name of the collection corresponding to the
     * bag type
     * @param bag the bag
     * @param os the objectstore
     * @param webConfig the webConfig
     * @param model the model
     * @param classKeys the classKeys
     */
    public BagTableWidgetLoader(String title, String description, String type, String
            collectionName, InterMineBag bag, ObjectStore os, WebConfig webConfig, Model model,
            Map classKeys, String fields) {
        this.title = title;
        this.description = description;
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
        QueryReference qr;
        try {
            qr = new QueryCollectionReference(qClassB, collectionName);
        } catch (Exception e) {
            qr = new QueryObjectReference(qClassB, collectionName);
        }
        ContainsConstraint cstr = new ContainsConstraint(qr, ConstraintOp.CONTAINS, qClassA);
        cstSet.addConstraint(cstr);

        QueryField qf = new QueryField(qClassB, "id");
        BagConstraint bagCstr = new BagConstraint(qf, ConstraintOp.IN, bag.getListOfIds());
        cstSet.addConstraint(bagCstr);

        q.setConstraint(cstSet);

        q.addToGroupBy(qClassA);
        q.addToOrderBy(new OrderDescending(count));

        List results;
        try {
            results = os.execute(q, 0, 10, true, true, os.getSequence());
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
        ClassDescriptor cld = MainHelper.getClassDescriptor(type, model);
        columns = new ArrayList();

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
        flattenedResults = new ArrayList();
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
                    flattenedRow.add(new ResultElement(String.valueOf((Long) element), "bagName="
                                + bag.getName() + "&typeB=" + type + "&typeA=" + bag.getType()
                                + "&collection=" + collectionName + "&id=" + lastObjectId
                                .toString()));
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

}
