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

import java.util.HashSet;
import java.util.Set;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.web.logic.config.InlineListObject;

/**
 * @author I would say probably Radek
 *
 */
public class InlineList
{

    private Set<InlineListObject> listOfObjects = null;
    private FieldDescriptor fieldDescriptor = null;
    private Boolean showLinksToObjects = false;
    private String path = null;
    private Integer lineLength = null;

    /**
     * Set a set ;) of Objects by turning them into InterMineObjects and then InlineListObjects
     * @param listOfListObjects received from ReportObject resolver
     * @param columnToDisplayBy No idea - I'm just making checkstyle happy.
     * @param showLinksToObjects No idea - I'm just making checkstyle happy.
     * @param path No idea - I'm just making checkstyle happy.
     * @param lineLength No idea - I'm just making checkstyle happy.
     */
    public InlineList(
            Set<Object> listOfListObjects,
            String columnToDisplayBy,
            Boolean showLinksToObjects,
            String path,
            Integer lineLength) {
        this.showLinksToObjects = showLinksToObjects;
        this.path = path;
        this.lineLength = lineLength;

        listOfObjects = new HashSet<InlineListObject>();

        for (Object listObject : listOfListObjects) {
            if (listObject != null) {
                InterMineObject interMineListObject = (InterMineObject) listObject;

                Object value = null;
                Object id = null;
                try {
                    // get field values from the object
                    value = interMineListObject.getFieldValue(columnToDisplayBy);
                    id = interMineListObject.getFieldValue("id");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                // make our InlineListObject
                InlineListObject inlineListObject =
                    new InlineListObject(interMineListObject, value, id);

                listOfObjects.add(inlineListObject);
            } else {
                // do nothing
            }

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
     *   @return a size of the collection so we can determine if we
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
     * @see our JavaScript (jQuery) expects non set values to be "0"
     * @return total character length (spaces, commas included) to show
     */
    public Integer getLineLength() {
        return (lineLength != null) ? lineLength : 0;
    }

    /**
     * @return String path so that ReportObject can resolve the actual Objects
     */
    public String getPath() {
        return path;
    }

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
     *
     * @return String of everything before the first dot
     */
    public String getPrefix() {
        String[] parts = path.split("\\.");
        return parts[0];
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return String.format(
                "<inline-list path=%s, showLinksToObjects=%s, fieldDescriptor=%s>",
                path, showLinksToObjects, fieldDescriptor);
    }

}
