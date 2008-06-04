package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryHelper;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.flatouterjoins.MultiRow;
import org.intermine.path.Path;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;

/**
 * A pageable and configurable table of data.
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class PagedTable
{
    private static final int FIRST_SELECTED_FIELDS_COUNT = 25;
    private WebTable webTable;
    private List<String> columnNames = null;
    private List<List<ResultElement>> resultElementRows = null;
    private int startRow = 0;

    private int pageSize = Constants.DEFAULT_TABLE_SIZE;
    private List<Column> columns;
    private String tableid;

    private List<List<Object>> rows = null;

    // object ids that have been selected in the table
    // TODO this may be more memory efficient with an IntPresentSet
    // note: if allSelected != -1 then this map contains those objects that are NOT selected
    private Map<Integer, String> selectionIds = new LinkedHashMap<Integer, String>();

    // the index of the column the has all checkbox checked
    private int allSelected = -1;

    private String selectedClass;

    /**
     * Construct a PagedTable with a list of column names
     * @param webTable the WebTable that this PagedTable will display
     */
    public PagedTable(WebTable webTable) {
        super();
        this.webTable = webTable;
    }

    /**
     * Construct a PagedTable with a list of column names
     * @param webTable the WebTable that this PagedTable will display
     * @param pageSize the number of records to show on each page.  Default value is 10.
     */
    public PagedTable(WebTable webTable, int pageSize) {
        super();
        this.webTable = webTable;
        this.pageSize = pageSize;
    }


    /**
     * Get the list of column configurations
     *
     * @return the List of columns in the order they are to be displayed
     */
    public List<Column> getColumns() {
        return Collections.unmodifiableList(getColumnsInternal());
    }

    private List<Column> getColumnsInternal() {
        if (columns == null) {
            columns = webTable.getColumns();
        }
        return columns;
    }

    /**
     * Return the column names
     * @return the column names
     */
    public List<String> getColumnNames() {
        if (columnNames == null) {
            columnNames = new ArrayList<String>();
            Iterator<Column> iter = getColumns().iterator();
            while (iter.hasNext()) {
                String columnName = iter.next().getName();
                columnNames.add(columnName);
            }
        }
        return columnNames;
    }

    /**
     * Return the number of visible columns.  Used by JSP pages.
     * @return the number of visible columns.
     */
    public int getVisibleColumnCount() {
        int count = 0;
        for (Iterator<Column> i = getColumnsInternal().iterator(); i.hasNext();) {
            Column obj = i.next();
            if (obj.isVisible()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Move a column left
     *
     * @param index the index of the column to move
     */
    public void moveColumnLeft(int index) {
        if (index > 0 && index <= getColumnsInternal().size() - 1) {
            getColumnsInternal().add(index - 1, getColumnsInternal().remove(index));
        }
    }

    /**
     * Move a column right
     *
     * @param index the index of the column to move
     */
    public void moveColumnRight(int index) {
        if (index >= 0 && index < getColumnsInternal().size() - 1) {
            getColumnsInternal().add(index + 1, getColumnsInternal().remove(index));
        }
    }

    /**
     * Set the page size of the table
     *
     * @param pageSize the page size
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
        startRow = (startRow / pageSize) * pageSize;
        updateResultElementRows();
    }

    /**
     * Get the page size of the current page
     *
     * @return the page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Get the index of the first row of this page
     * @return the index
     */
    public int getStartRow() {
        return startRow;
    }

    /**
     * Get the page index.
     * @return current page index
     */
    public int getPage() {
        return (startRow / pageSize);
    }

    /**
     * Set the page size and page together.
     *
     * @param page page number
     * @param size page size
     */
    public void setPageAndPageSize(int page, int size) {
        this.pageSize = size;
        this.startRow = size * page;
        updateResultElementRows();
    }

    /**
     * Get the index of the last row of this page
     * @return the index
     */
    public int getEndRow() {
        return startRow + getResultElementRows().size() - 1;
    }

    /**
     * Go to the first page
     */
    public void firstPage() {
        startRow = 0;
        updateResultElementRows();
    }

    /**
     * Check if were are on the first page
     * @return true if we are on the first page
     */
    public boolean isFirstPage() {
        return (startRow == 0);
    }

    /**
     * Go to the last page
     */
    public void lastPage() {
        startRow = ((getExactSize() - 1) / pageSize) * pageSize;
        updateResultElementRows();
    }

    /**
     * Check if we are on the last page
     * @return true if we are on the last page
     */
    public boolean isLastPage() {
        return (!isSizeEstimate() && getEndRow() == getEstimatedSize() - 1);
    }

    /**
     * Go to the previous page
     */
    public void previousPage() {
        if (startRow >= pageSize) {
            startRow -= pageSize;
        }
        updateResultElementRows();
    }

    /**
     * Go to the next page
     */
    public void nextPage() {
        startRow += pageSize;
        updateResultElementRows();
    }

    /**
     * Return the currently visible rows of the table as a List of Lists of ResultElement objects.
     * @return the resultElementRows of the table
     */
    public List<List<Object>> getRows() {
        if (rows == null) {
            updateRows();
        }
        return rows;
    }

    /**
     * Return the currently visible rows of the table as a List of Lists of raw values/Objects.
     * @return the ResultElement of the table as rows
     */
    public List<List<ResultElement>> getResultElementRows() {
        if (resultElementRows == null) {
            updateResultElementRows();
        }
        return resultElementRows;
    }

    /**
     * Return all the resultElementRows of the table as a List of Lists.
     *
     * @return all the resultElementRows of the table
     */
    public WebTable getAllRows() {
        return webTable;
    }

    /**
     * Get the (possibly estimated) number of resultElementRows of this table
     * @return the number of resultElementRows
     */
    public int getEstimatedSize() {
        return webTable.getEstimatedSize();
    }

    /**
     * Check whether the result of getSize is an estimate
     * @return true if the size is an estimate
     */
    public boolean isSizeEstimate() {
        return webTable.isSizeEstimate();
    }

    /**
     * Get the exact number of resultElementRows of this table
     * @return the number of resultElementRows
     */
    public int getExactSize() {
        return webTable.size();
    }

    /**
     * Add an object id and its field value
     * that has been selected in the table.
     * @param objectId the id to select
     * @param columnIndex the column of the selected id
     */
    public void selectId(Integer objectId, int columnIndex) {
        if (allSelected == -1) {
            ResultElement resultElement = findIdInVisible(objectId);
            if (resultElement != null) {
                if (resultElement.getField() == null) {
                    selectionIds.put(objectId, null);
                } else {
                    selectionIds.put(objectId, resultElement.getField().toString());
                }
                setSelectedClass(TypeUtil.unqualifiedName(columns.get(columnIndex).getType()
                                    .getName()));
            }
        } else {
            // remove because the all checkbox is on
            selectionIds.remove(objectId);
        }
    }

    /**
     * Remove the object with the given object id from the list of selected objects.
     * @param objectId the object store id
     */
    public void deSelectId(Integer objectId) {
       if (allSelected == -1) {
           selectionIds.remove(objectId);
           if (selectionIds.size() <= 0) {
                setSelectedClass(null);
            }
       } else {
           // add because the all checkbox is on
           selectionIds.put(objectId, null);
       }
    }

    /**
     * Search the visible rows and return the first ResultElement with the given ID./
     */
    private ResultElement findIdInVisible(Integer id) {
        for (List<ResultElement> resultElements: getResultElementRows()) {
            for (ResultElement resultElement : resultElements) {
                if ((resultElement != null) && (resultElement.getId().equals(id))
                    && (resultElement.isKeyField())) {
                    return resultElement;
                }
            }
        }
        return null;
    }

    /**
     * Return the fields for the first selected objects.  Return the first
     * FIRST_SELECTED_FIELDS_COUNT fields.  If there are more than that, append "..."
     * @param os the ObjectStore
     * @param classKeysMap map of key field for a given class name
     * @return the list
     */
    public List<String> getFirstSelectedFields(ObjectStore os,
                                               Map<String, List<FieldDescriptor>> classKeysMap) {
        List<String> retList = new ArrayList<String>();
        Iterator<SelectionEntry> selectedEntryIter = selectedEntryIterator();
        boolean seenNullField = false;
        while (selectedEntryIter.hasNext()) {
            if (retList.size() < FIRST_SELECTED_FIELDS_COUNT) {
                SelectionEntry entry = selectedEntryIter.next();
                String fieldValue = entry.fieldValue;
                if (fieldValue == null) {
                    // the select column doesn't have a value for this object; use class keys to
                    // find a value
                    InterMineObject object;
                    try {
                        Integer id = entry.id;
                        object = os.getObjectById(id);
                        if (object == null) {
                            throw new RuntimeException("internal error - unknown object id: " + id);
                        } else {
                            String classKeyFieldValue = findClassKeyValue(classKeysMap, object);
                            if (classKeyFieldValue == null) {
                                seenNullField = true;
                            } else {
                                retList.add(classKeyFieldValue);
                            }
                        }
                    } catch (ObjectStoreException e) {
                        seenNullField = true;
                    }
                } else {
                    retList.add(fieldValue);
                }
            } else {
                retList.add("...");
                return retList;
            }
        }
        if (seenNullField) {
            // if there are null that we can't find a field value for, just append "..." because
            // showing "[no value]" in the webapp is of no value
            retList.add("...");
        }
        return retList;
    }

    /**
     * Return the first non-null class key field for the object with the given id.
     */
    private String findClassKeyValue(Map<String, List<FieldDescriptor>> classKeysMap,
                                     InterMineObject object) {
        try {
            String objectClassName = DynamicUtil.getFriendlyName(object.getClass());
            List<FieldDescriptor> classKeyFds = classKeysMap.get(objectClassName);
            for (FieldDescriptor fd: classKeyFds) {
                Object value = TypeUtil.getFieldValue(object, fd.getName());
                if (value != null) {
                    return value.toString();
                }
            }
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * Return selected object ids of the current page as a String[], needed for jsp multibox.
     * @return selected ids as Strings.
     */
    public String[] getCurrentSelectedIdStrings() {
        return getCurrentSelectedIdStringsList().toArray(new String[0]);
    }

    /**
     * Return selected object ids of the current page as a List.
     * @return the list.
     */
    public List<String> getCurrentSelectedIdStringsList() {
        List<String> selected = new ArrayList<String>();
        if (allSelected == -1) {
            if (!selectionIds.isEmpty()) {
                for (List<ResultElement> currentRow: getResultElementRows()) {
                    for (ResultElement resElt: currentRow) {
                        if (resElt != null) {
                            if (selectionIds.containsKey(resElt.getId())) {
                                selected.add(resElt.getId().toString());
                            }
                        }
                    }
                }
            }
        } else {
            for (List<ResultElement> currentRow: getResultElementRows()) {
                ResultElement resElt = currentRow.get(allSelected);
                if (resElt != null) {
                    if (!selectionIds.containsKey(resElt.getId())) {
                        selected.add(resElt.getId().toString());
                    }
                }
            }
            selected.add(columns.get(allSelected).getColumnId());
        }
        return selected;
    }

    /**
     * Clear the table selection
     */
    public void clearSelectIds() {
        selectionIds = new LinkedHashMap<Integer, String>();
        allSelected = -1;
    }

    private class SelectionEntry
    {
        Integer id;
        String fieldValue;
    }

    /**
     * Return an Iterator over the selected id/fieldname pairs
     */
    private Iterator<SelectionEntry> selectedEntryIterator() {
        if (allSelected == -1) {
            return new Iterator<SelectionEntry>() {
                Iterator<Map.Entry<Integer, String>> selectionIter =
                    selectionIds.entrySet().iterator();
                public boolean hasNext() {
                    return selectionIter.hasNext();
                }
                public SelectionEntry next() {
                    SelectionEntry retEntry = new SelectionEntry();
                    Map.Entry<Integer, String> entry = selectionIter.next();
                    retEntry.id = entry.getKey();
                    retEntry.fieldValue = entry.getValue();
                    return retEntry;
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } else {
            return new Iterator<SelectionEntry>() {
                SelectionEntry nextEntry = null;
                int currentIndex = 0;
                {
                    moveToNext();
                }

                private void moveToNext() {
                    while (true) {
                        try {
                            List<ResultElement> row = getAllRows().getResultElements(currentIndex);
                            ResultElement element = row.get(allSelected);
                            Integer elementId = element.getId();
                            if (!selectionIds.containsKey(elementId)) {
                                nextEntry = new SelectionEntry();
                                nextEntry.id = elementId;
                                if (element.getField() == null) {
                                    nextEntry.fieldValue = null;
                                } else {
                                    nextEntry.fieldValue = element.getField().toString();
                                }
                                break;
                            }
                        } catch (IndexOutOfBoundsException e) {
                            nextEntry = null;
                            break;
                        } finally {
                            currentIndex++;
                        }
                    }
                }

                public boolean hasNext() {
                    return nextEntry != null;
                }

                public SelectionEntry next() {
                   SelectionEntry retVal = nextEntry;
                   moveToNext();
                   return retVal;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /**
     * Return an Iterator over the selected Ids
     * @return the Iterator
     */
    public Iterator<Integer> selectedIdsIterator() {
        return new Iterator<Integer>() {
            Iterator<SelectionEntry> selectedEntryIter = selectedEntryIterator();
            public boolean hasNext() {
               return selectedEntryIter.hasNext();
            }
            public Integer next() {
                return selectedEntryIter.next().id;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * If a whole column is selected, return its index, otherwise return -1.
     * @return the index of the column that is selected
     */
    public int getAllSelectedColumn() {
        if (selectionIds.isEmpty()) {
            return allSelected;
        } else {
            return -1;
        }
    }

    /**
     * Select a whole column.
     * @param columnSelected the column index
     */
    public void setAllSelectedColumn(int columnSelected) {
        if (columnSelected == -1) {
            selectedClass = null;
        } else {
            Class<?> columnClass = getAllRows().getColumns().get(columnSelected).getType();
            selectedClass = TypeUtil.unqualifiedName(columnClass.getName());
        }
        this.allSelected = columnSelected;
    }

    /**
     * @return the selectedClass
     */
    public String getSelectedClass() {
        return selectedClass;
    }

    /**
     * @param selectedClass the selectedClass to set
     */
    public void setSelectedClass(String selectedClass) {
        this.selectedClass = selectedClass;
    }

    /**
     * Set the rows fields to be a List of Lists of values from ResultElement objects from
     * getResultElementRows().
     */
    private void updateRows() {
        rows = new ArrayList<List<Object>>();
        for (int i = getStartRow(); i < getStartRow() + getPageSize(); i++) {
            try {
                List<Object> newRow = getAllRows().get(i);
                rows.add(newRow);
            } catch (IndexOutOfBoundsException e) {
                // we're probably at the end of the results object, so stop looping
                break;
            }
        }
    }

    /**
     * Update the internal row list
     */
    private void updateResultElementRows() {
        List<List<ResultElement>> newRows = new ArrayList<List<ResultElement>>();
        String invalidStartMessage = "Invalid start row of table: " + getStartRow();
        if (getStartRow() < 0) {
            throw new PageOutOfRangeException(invalidStartMessage);
        }

        try {
            if (getStartRow() == 0) {
                // no problem - 0 is always valid
            } else {
                getAllRows().getResultElements(getStartRow());
            }
        } catch (IndexOutOfBoundsException e) {
            throw new PageOutOfRangeException(invalidStartMessage);
        }

        for (int i = getStartRow(); i < getStartRow() + getPageSize(); i++) {
            try {
                List<ResultElement> resultsRow = getAllRows().getResultElements(i);
                // if some objects already selected, set corresponding  ResultElements here
                if (!selectionIds.isEmpty()) {
                    for (ResultElement re : resultsRow) {
                        if (re != null && selectionIds.keySet().contains(re.getId())) {
                            re.setSelected(true);
                        }
                    }
                }
                newRows.add(resultsRow);
            } catch (IndexOutOfBoundsException e) {
                // we're probably at the end of the results object, so stop looping
                break;
            }
        }
        this.resultElementRows = newRows;
        // clear so that getRows() recreates it
        this.rows = null;
    }

    /**
     * Return the maximum retrievable index for this PagedTable.  This will only ever return less
     * than getExactSize() if the underlying data source has a restriction on the maximum index
     * that can be retrieved.
     * @return the maximum retrieved index
     */
    public int getMaxRetrievableIndex() {
        return webTable.getMaxRetrievableIndex();
    }

    /**
     * Return the class from the data model for the data displayed in indexed column.
     * This may be the parent class of a field e.g. if column displays A.field where
     * field is a String and A is a class in the model this method will return A.
     * @param index of column to find type for
     * @return the class or parent class for the indexed column
     */
    public Class<?> getTypeForColumn(int index) {
        return webTable.getColumns().get(index).getType();
    }

    /**
     * Set the column names
     * @param columnNames a list of Strings
     */
    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    /**
     * @return the webTable
     */
    public WebTable getWebTable() {
        return webTable;
    }

    /**
     * @return the tableid
     */
    public String getTableid() {
        return tableid;
    }

    /**
     * @param tableid the tableid to set
     */
    public void setTableid(String tableid) {
        this.tableid = tableid;
    }

    /**
     * Returns indexes of columns, that should be displayed.
     * @return indexes
     */
    public List<Integer> getVisibleIndexes() {
        List<Integer> ret = new ArrayList<Integer>();
        for (int i = 0; i < getColumns().size(); i++) {
            if (getColumns().get(i) != null && getColumns().get(i).isVisible()) {
                ret.add(getColumns().get(i).getIndex());
            }
        }

        return ret;
    }

    /**
     * Returns a List containing the results, with the columns rearranged.
     *
     * @return a List of rows, each of which is a List
     */
    public List getRearrangedResults() {
        return new RearrangedList();
    }

    private class RearrangedList extends AbstractList
    {
        private List<Integer> visibleIndexes;

        public RearrangedList() {
            visibleIndexes = getVisibleIndexes();
        }

        @Override
        public List get(int index) {
            return translateRow(webTable.getResultElements(index));
        }

        private List translateRow(List row) {
            if (row instanceof MultiRow) {
                MultiRow ret = new MultiRow();
                for (List subRow : ((List<List>) row)) {
                    ret.add(translateRow(subRow));
                }
                return ret;
            }
            List ret = new ResultsRow();
            for (int i = 0; i < visibleIndexes.size(); i++) {
                ret.add(row.get(visibleIndexes.get(i)));
            }
            return ret;
        }

        @Override
        public int size() {
            return webTable.size();
        }

        @Override
        public Iterator iterator() {
            return new Iter();
        }

        private class Iter implements Iterator
        {
            private Iterator subIter = webTable.iterator();

            public boolean hasNext() {
                return subIter.hasNext();
            }

            public Object next() {
                List originalRow = (List) subIter.next();
                return translateRow(originalRow);
            }

            public void remove() {
                throw (new UnsupportedOperationException());
            }
        }
    }

    /**
     * Return true if and only if nothing is selected
     * @return true if and only if nothing is selected
     */
    public boolean isEmptySelection() {
        return !selectedIdsIterator().hasNext();
    }

    /**
     * Return true iff a whole column is selected.
     * @return if a column is selected
     */
    public boolean isAllSelected() {
        if (allSelected == -1) {
            int selectedCount = selectionIds.size();
            if (selectedCount > 0) {
                // If there is at least one more row than there are selected elements, it's not
                // all selected.  We use get()/try/catch to avoid callin size() which can be slow
                try {
                    getAllRows().get(selectedCount);
                    // success
                    return false;
                } catch (IndexOutOfBoundsException e) {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return selectionIds.size() == 0;
        }
    }

    /**
     * Return a Query that produces a Results object of the selected objects.
     * @return the query
     */
    public Query getBagCreationQuery() {
        if (allSelected == -1) {
            Query query = new Query();
            QueryClass qc = new QueryClass(InterMineObject.class);
            query.addFrom(qc);
            query.addToSelect(qc);

            BagConstraint bc = new BagConstraint(new QueryField(qc, "id"),
                                                 ConstraintOp.IN, selectionIds.keySet());

            query.setConstraint(bc);

            return query;
        } else {
            WebResults webResults = (WebResults) getAllRows();
            Results results = webResults.getInterMineResults();
            Query origQuery = results.getQuery();
            Query newQuery = QueryCloner.cloneQuery(origQuery);

            Map<Path, QueryNode> pathToQueryNodeMap = webResults.getPathToQueryNode();
            Path path = columns.get(allSelected).getPath().getPrefix();
            QueryNode qn = pathToQueryNodeMap.get(path.toStringNoConstraints());

            int nodeIndex = origQuery.getSelect().indexOf(qn);

            QueryClass newNode = (QueryClass) newQuery.getSelect().get(nodeIndex);

            newQuery.clearSelect();

            newQuery.addToSelect(newNode);

            BagConstraint bc =
                new BagConstraint(newNode, ConstraintOp.NOT_IN, selectionIds.keySet());

            QueryHelper.addAndConstraint(newQuery, bc);

            return newQuery;
        }
    }

}
