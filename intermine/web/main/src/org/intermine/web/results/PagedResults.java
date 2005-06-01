package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreLimitReachedException;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryHelper;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.FromElement;
import org.intermine.metadata.Model;
import org.intermine.metadata.FieldDescriptor;

/**
 * A pageable and configurable table created from the Results object.
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class PagedResults extends PagedTable
{
    protected Results results;

    /**
     * Create a new PagedResults object from the given Results object.
     *
     * @param results the Results object
     * @param model the Model for the Results
     */
    public PagedResults(Results results, Model model) {
        this(QueryHelper.getColumnAliases(results.getQuery()), results, model);
    }

    /**
     * Create a new PagedResults object from the given Results object.
     *
     * @param results the Results object
     * @param columnNames the headings for the Results columns
     * @param model the Model for the Results
     */
    public PagedResults(List columnNames, Results results, Model model) {
        super(columnNames);
        this.results = results;
        setColumnTypes(model);
        try {
            updateRows();
        } catch (PageOutOfRangeException e) {
            throw new RuntimeException("unable to create a PagedResults object", e);
        }
    }

    /**
     * @see PagedTable#getAllRows
     */
    public List getAllRows() {
        return results;
    }

    /**
     * @see PagedTable#getSize
     */
    public int getSize() {
        try {
            return results.getInfo().getRows();
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see PagedTable#isSizeEstimate
     */
    public boolean isSizeEstimate() {
        try {
            return results.getInfo().getStatus() != ResultsInfo.SIZE;
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see PagedTable#getExactSize
     */
    protected int getExactSize() {
        return results.size();
    }

    /**
     * @throws PageOutOfRangeException
     * @see PagedTable#updateRows
     */
    protected void updateRows() throws PageOutOfRangeException {
        rows = new ArrayList();
        try {
            for (int i = startRow; i < startRow + pageSize; i++) {
                rows.add(results.get(i));
            }
        } catch (IndexOutOfBoundsException e) {
                // ignore 
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ObjectStoreLimitReachedException) {
                throw new PageOutOfRangeException("PagedResults.updateRows() failed",
                                                  e.getCause());
            }
        }
    }

    /**
     * Return information about the results
     * @return the relevant ResultsInfo
     * @throws ObjectStoreException if an error occurs accessing the underlying ObjectStore
     */
    public ResultsInfo getResultsInfo() throws ObjectStoreException {
        return results.getInfo();
    }

    /**
     * @see PagedTable#getMaxRetrievableIndex
     */
    public int getMaxRetrievableIndex() {
        return results.getObjectStore().getMaxOffset();
    }

    /**
     * Call setType() on each Column, setting it the type to a ClassDescriptor, a FieldDescriptor
     * or Object.class
     */
    private void setColumnTypes(Model model) {
        Query q = results.getQuery();

        Iterator columnIter = getColumns().iterator();
        Iterator selectListIter = q.getSelect().iterator();

        while (columnIter.hasNext()) {
            Column thisColumn = (Column) columnIter.next();
            QueryNode thisQueryNode = (QueryNode) selectListIter.next();

            if (thisQueryNode instanceof QueryClass) {
                Class thisQueryNodeClass = ((QueryClass) thisQueryNode).getType();
                thisColumn.setType(model.getClassDescriptorByName(thisQueryNodeClass.getName()));
            } else {
                if (thisQueryNode instanceof QueryField) {
                    QueryField queryField = (QueryField) thisQueryNode;
                    FromElement fe = queryField.getFromElement();
                    if (fe instanceof QueryClass) {
                        QueryClass queryClass = (QueryClass) fe;
                        Map fieldMap = model.getFieldDescriptorsForClass(queryClass.getType());
                        FieldDescriptor fd =
                            (FieldDescriptor) fieldMap.get(queryField.getFieldName());
                        thisColumn.setType(fd);
                    } else {
                        thisColumn.setType(Object.class);
                    }
                } else {
                    thisColumn.setType(Object.class);
                }
            }
        }
    }
}
