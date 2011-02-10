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
     * Shall we show this inline list in the header of the report page (and nowhere else?)
     * @param showInHeader set from WebConfig
     */
    public void setShowInHeader(Boolean showInHeader) {
        this.showInHeader  = showInHeader;
    }

    /**
     *
     * @return String path so that DisplayObject can resolve the actual Objects
     */
    public String getPath() {
        return path;
    }

    /**
     * Set a set ;) of Objects by turning them into InterMineObjects and then InlineListObjects
     * @param list received from DisplayObject resolver
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
        return showInHeader;
    }

}
