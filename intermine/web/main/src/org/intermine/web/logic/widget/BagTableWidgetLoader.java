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
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Xavier Watkins
 *
 */
public class BagTableWidgetLoader
{
    private List<String> columns;
    private List flattenedResults;
    private String title, description;
    protected Query q;
    private int widgetTotal = 0;
    
    /**
     * This class loads and formats the data for the count
     * table widgets in the bag details page
     *
     * @param pathString the path to group the objects by, ie Employee.city will return the list of
     * employees by city
     * @param bag the bag
     * @param os the objectstore
     * @param webConfig the webConfig
     * @param model the model
     * @param classKeys the classKeys
     * @param fields fields involved in widget
     * @param urlGen the class that generates the pathquery used in the links from the widget
     * @param columnTitle title for count column
     * @param externalLink link to external source
     * @param externalLinkLabel name of external data source
     * @throws ClassNotFoundException if some class in the widget paths is not found
     * @throws UnsupportedEncodingException if something goes wrong encoding the URL
     */
    public BagTableWidgetLoader(String pathString, InterMineBag bag, ObjectStore os, 
                                WebConfig webConfig, Model model, 
                                Map<String, List<FieldDescriptor>> classKeys, 
                                String fields, String urlGen, String columnTitle,
                                String externalLink, String externalLinkLabel) 
    throws ClassNotFoundException, UnsupportedEncodingException {
        Path pathTmp = new Path(model, pathWithNoConstraints(pathString));
        ClassDescriptor cld = pathTmp.getEndClassDescriptor();
        String type = cld.getUnqualifiedName();
        
        // TODO validate start type vs. bag type
        ClassDescriptor cldStart = pathTmp.getStartClassDescriptor();
        q = constructQuery(model, bag, cldStart, pathString, false);
        
        List results;
        try {
            results = os.execute(q, 0, 50, true, true, ObjectStore.SEQUENCE_IGNORE);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
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
            for (Iterator<FieldConfig> iter = cldFieldConfigs.iterator(); iter.hasNext();) {
                FieldConfig fc = iter.next();
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
                    boolean isFirst = true;
                    for (Iterator iterator3 = columns.iterator(); iterator3.hasNext();) {
                        
                        String columnName = (String) iterator3.next();
                        Path path = new Path(model, columnName);
                        Object fieldValue = path.resolve(o);
                        Class thisType = path.getStartClassDescriptor().getType();
                        String fieldName = path.getEndFieldDescriptor().getName();
                        boolean isKeyField = ClassKeyHelper.isKeyField(classKeys,
                                TypeUtil.unqualifiedName(thisType.getName()), fieldName);
                        String link = null;
                        String val = fieldValue.toString();
                        if (isKeyField) {
                            key = fieldValue.toString();
                            link = "objectDetails.do?id=" + o.getId() + "&amp;trail=|bag."
                                   + bag.getName() + "|" + o.getId();
                        } else if (externalLink != null && !externalLink.equals("")) {
                            val = val + " <a href=\"" + externalLink + key 
                            + "\" target=\"_new\" class=\"extlink\">[" 
                            + externalLinkLabel + "]</a>";
                        }

                        if (isFirst) {

                            String checkbox = "<input name=\"selected\" value=\"" + key 
                            + "\" id=\"selected_" + key + "\" type=\"checkbox\">";

                            flattenedRow.add(new String[]
                                                        {
                                checkbox
                                                        });
                            isFirst = false;
                        }

                        flattenedRow.add(new String[]
                                                    {
                            val, link
                                                    });
                        
                    }
                } else if (element instanceof Long) {
                    flattenedRow.add(new String[]
                                                {
                        String.valueOf((Long) element),
                        "widgetAction.do?bagName=" + bag.getName() + "&link=" + urlGen
                        + "&key=" + URLEncoder.encode(key, "UTF-8")
                                                });
                }

            }
            flattenedResults.add(flattenedRow);
        }
        // Add the count column
        if (columnTitle != null) {
            columns.add(columnTitle);
        } else {
            columns.add(bag.getType() + "s");
        }

        q = constructQuery(model, bag, cldStart, pathString, true);
        widgetTotal = calcTotal(os, q);
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
    private Query constructQuery(Model model, InterMineBag bag, ClassDescriptor cldStart,
                                 String pathString, boolean calcTotal)
        throws ClassNotFoundException, IllegalArgumentException {
                
        Query q = new Query();
        boolean first = true;
        String[] queryBits = pathString.split("\\.");
        QueryClass qcStart = null;
        QueryField qfStartId = null;
        for (int i = 1; i < queryBits.length; i++) {
            if (qcStart == null) {
                qcStart = new QueryClass(cldStart.getType());
                qfStartId = new QueryField(qcStart, "id");
            }
            
            QueryField qfId = new QueryField(qcStart, "id");
            
            if (first) {
                q.addFrom(qcStart);
                // this is the start of the path sp constraint to be in bag
                QueryHelper.addConstraint(q, 
                                          new BagConstraint(qfId, ConstraintOp.IN, bag.getOsb())); 
            }
            
            String refName;
            String constraintName = null, constraintValue = null;
            // extra constraints have syntax Company.departments[name=DepartmentA].employees
            if (queryBits[i].indexOf('[') > 0) {
                String s = queryBits[i];
                refName = s.substring(0, s.indexOf('['));
                constraintName = s.substring(s.indexOf('[') + 1, s.indexOf('='));
                constraintValue = s.substring(s.indexOf('=') + 1, s.indexOf(']'));
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

            addReferenceConstraint(model, q, qcStart, refName, qcEnd);
            
            if (constraintName != null && constraintValue != null) {
                AttributeDescriptor attFld = cldEnd.getAttributeDescriptorByName(constraintName);
                if (attFld == null) {
                    throw new IllegalArgumentException("Class '" + cldEnd.getType() + "' has no '"
                                                       + constraintName + "' field");
                }
                if (!attFld.getType().equals("java.lang.String")) {
                    throw new IllegalArgumentException("Constraints can only be on String fields '"
                                                       + constraintName 
                                                       + "' is a " + attFld.getType());
                }
                SimpleConstraint sc = new SimpleConstraint(new QueryField(qcEnd, constraintName),
                                                           ConstraintOp.EQUALS,
                                                           new QueryValue(constraintValue));
                QueryHelper.addConstraint(q, sc);
                constraintName = null;
                constraintValue = null;
            }
            
            QueryFunction qf = new QueryFunction();
            
            // if we are at the end of the path, add to select and group by
            if (queryBits.length == (i + 1)) {
                
                if (!calcTotal) {
                    
                    q.setDistinct(false);
                    q.addToSelect(qcEnd);
                    q.addToGroupBy(qcEnd);
                    
                    q.addToSelect(qf);
                    q.addToOrderBy(qf, "desc");
                    
                } else {
                    
                    Query subQ = new Query();
                    subQ = q;
                    subQ.setDistinct(true);
                    subQ.clearSelect();
                    subQ.addToSelect(qfStartId);
                    
                    q = new Query();
                    q.setDistinct(false);
                    q.addToSelect(qf);
                    q.addFrom(subQ);
                }
            }
            cldStart = cldEnd;
            qcStart = qcEnd;
            first = false;
        }

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
     * to the query
     * @return QueryClass return qcEnd
     */
    private QueryClass addReferenceConstraint(Model model, Query q, QueryClass qcStart, 
                                              String refName, QueryClass qcEnd) {
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
        QueryHelper.addConstraint(q, cc);
        
        return qcEnd;
    }
    
    // return a path with out any [] constraints
    private String pathWithNoConstraints(String path) {
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
        Iterator iter = res.iterator();
        int n = 0;
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            n = ((java.lang.Long) resRow.get(0)).intValue();
        }
        return n;
    }
}
  