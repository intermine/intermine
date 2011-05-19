package org.intermine.web.logic.config;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Set;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;

/**
 * Represents an inline list to be shown on a Report page
 * @author radek
 *
 */
public class InlineList
{

    private String path;
    private Boolean showLinksToObjects = false;
    private Set<InlineListObject> listOfObjects;
    private Boolean showInHeader = false;
    private FieldDescriptor fieldDescriptor = null;
    private Integer lineLength = null;

    /**
     * Set FieldDescriptor so we can work with placements
     * @param fd FieldDescriptor set by ReportObject on init
     */
    public void setDescriptor(FieldDescriptor fd) {
        fieldDescriptor = fd;
    }

    /**
     *
     * @return FieldDecriptor
     */
    public FieldDescriptor getDescriptor() {
        return fieldDescriptor;
    }

    /**
     * Path set from WebConfig, ie "probeSets.primaryIdentifier"
     * @param path String
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Shall we show links to the objects's report pages?
     * @param showLinksToObjects set from WebConfig
     */
    public void setShowLinksToObjects(Boolean showLinksToObjects) {
        this.showLinksToObjects = showLinksToObjects;
    }

    /**
     * Show this list in the header?
     * @param showInHeader Passed from WebConfig
     */
    public void setShowInHeader(Boolean showInHeader) {
        this.showInHeader = showInHeader;
    }

    /**
     * Sets the amount of entries to show based on their total length
     * @see the number is approximate as we do not break inside the text
     * @param lineLength total character length (spaces, commas included!)
     */
    public void setLineLength(Integer lineLength) {
        this.lineLength = lineLength;
    }

    /**
     *
     * @see our JavaScript (jQuery) expects non set values to be "0"
     * @return total character length (spaces, commas included) to show
     */
    public Integer getLineLength() {
        return (lineLength != null) ? lineLength : 0;
    }

    /**
     *
     * @return String path so that ReportObject can resolve the actual Objects
     */
    public String getPath() {
        return path;
    }

    /**
     *
     * @return String of everything before the first dot
     */
    public String getPrefix() {
        String[] parts = path.split("\\.");
        return parts[0];
    }

    /**
     * Set a set ;) of Objects by turning them into InterMineObjects and then InlineListObjects
     * @param list received from ReportObject resolver
     * @param key is a value by which we want to show the objects by,
     *  "probeSets.primaryIdentifier" => primaryIdentifier
     */
    public void setListOfObjects(Set<Object> list, String key) {
        listOfObjects = new HashSet<InlineListObject>();

        for (Object listObject : list) {
            InterMineObject interMineListObject = (InterMineObject) listObject;

            Object value = null;
            Object id = null;
            try {
                // get field values from the object
                value = interMineListObject.getFieldValue(key);
                id = interMineListObject.getFieldValue("id");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            // make our InlineListObject
            InlineListObject inlineListObject =
                new InlineListObject(interMineListObject, value, id);

            listOfObjects.add(inlineListObject);
        }
    }

    /**
     *
     * @return a set of InlineListObjects
     */
    public Set<InlineListObject> getItems() {
        return listOfObjects;
    }

    /**
     *
     * @return the length of the strings in this list combined, delimiters included!
     */
    public Integer getLength() {
        Integer length = 0;
        Set<InlineListObject> items = getItems();
        if (items != null) {
            for (InlineListObject ilObj : getItems()) {
                Object value = ilObj.getValue();
                String valueString = value.toString();
                length += valueString.length() + 2;
            }
            length -= 2;
        }
        return length;
    }

    /**
     *
     * @return a size of the collection so we can determine if we
     *  are outputting a last item in the JSP etc.
     */
    public int getSize() {
        return listOfObjects.size();
    }

    /**
     *
     * @return are we to show links to the objects'? report page? the JSP asks...
     */
    public Boolean getShowLinksToObjects() {
        return showLinksToObjects;
    }

    /**
     *
     * @return are we to show this inline list in the header of the report page?
     */
    public Boolean getShowInHeader() {
        return (showInHeader);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "path=" + path + " showLinksToObjects=" + showLinksToObjects + " fieldDescriptor="
            + fieldDescriptor;
    }

}
