package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.web.logic.config.WebConfig;

/**
 * An inline table of ResultElements used ob BagUploadConfirm
 *
 * At the moment, we use this custom class to create a custom type of table row that will be
 *  extended with identifier and rowspan attributes needed on list upload confirmation step
 *
 * @author Radek Stepan
 */
public class BagUploadConfirmInlineResultsTable extends InlineResultsTable
{

    /**
     * Construct a new InlineResultsTable object
     *
     * @param results the List to display object
     * @param model the current Model
     * @param webConfig the WebConfig object for this webapp
     * @param classKeys class keys for this mine.
     * @param size the maximum number of rows to list from the collection, or -1 if we should
     * @param ignoreDisplayers if true don't include any columns that have jsp displayers defined
     * @param listOfTypes resolved using PathQueryResultHelper.queryForTypesInCollection on a
     *  Collection, a Reference object will have null instead and its Type will be resolved
     *  using getListOfTypes()
     */
    public BagUploadConfirmInlineResultsTable(
            Collection<?> results, Model model,
            WebConfig webConfig,
            Map<String, List<FieldDescriptor>> classKeys,
            int size,
            boolean ignoreDisplayers,
            List<Class<?>> listOfTypes) {
        super(results, model, webConfig, classKeys, size, ignoreDisplayers, listOfTypes);
    }

    /**
     * Create a table row object
     *
     * @return a BagUploadConfirmInlineResultsTableRow
     */
    protected final Object returnNewTableRow() {
        return new BagUploadConfirmInlineResultsTableRow();
    }

    /**
     * Set a class name on table row
     *
     * @param className string name of the class of the object
     * @param tableRowObject BagUploadConfirmInlineResultsTableRow
     */
    protected final void setClassNameOnTableRow(String className, Object tableRowObject) {
        BagUploadConfirmInlineResultsTableRow tableRow =
            (BagUploadConfirmInlineResultsTableRow) tableRowObject;
        tableRow.setClassName(className);
    }

    /**
     * Add a result element (RE or "" String)
     *
     * @param resultElement to be saved in the list
     * @param tableRowObject BagUploadConfirmInlineResultsTableRow
     */
    protected final void addResultElementToTableRow(Object resultElement, Object tableRowObject) {
        BagUploadConfirmInlineResultsTableRow tableRow =
            (BagUploadConfirmInlineResultsTableRow) tableRowObject;
        tableRow.add(resultElement);
    }

    /**
     * Set InterMine Object id for the table row
     * @param id imObj
     * @param tableRowObject BagUploadConfirmInlineResultsTableRow
     */
    protected void saveObjectIdOnTableRow(Integer id, Object tableRowObject) {
        BagUploadConfirmInlineResultsTableRow tableRow =
            (BagUploadConfirmInlineResultsTableRow) tableRowObject;
        tableRow.setObjectId(id);
    }

}
