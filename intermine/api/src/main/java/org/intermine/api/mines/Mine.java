package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.List;
import java.util.Set;

import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;

/**
 * The type of objects that represent InterMine instances.
 * @author Alex Kalderimis
 *
 */
public interface Mine
{

    /**
     * @return the name of the mine
     */
    String getName();

    /**
     * @return the description of the mine
     */
    String getDescription();

    /**
     * @return the url to the mine
     */
    String getUrl();

    /**
     * @return the logo
     */
    String getLogo();

    /**
     * @return bgcolor
     */
    String getBgcolor();

    /**
     * @return frontcolor
     */
    String getFrontcolor();

    /**
     * @return the releaseVersion
     */
    String getReleaseVersion();

    /**
     * Get the main model associated with this application.
     * @return The data model.
     */
    Model getModel();

    /**
     * @return the defaultValue
     */
    Set<String> getDefaultValues();

    /**
     * get first default value. used in querybuilder to select default extra value
     * @return the defaultValue
     */
    String getDefaultValue();

    /**
     * Run a path query and get back all the results.
     * @param query The query to run.
     * @return A list of rows.
     */
    List<List<Object>> getRows(PathQuery query);

    /**
     * Run a path query and get back all the results.
     * @param xml The query to run, in XML format.
     * @return A list of rows.
     */
    List<List<Object>> getRows(String xml);

}
