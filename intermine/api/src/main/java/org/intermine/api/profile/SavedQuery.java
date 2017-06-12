package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;

import org.intermine.pathquery.PathQuery;


/**
 * Container for a saved query.
 *
 * @author Thomas Riley
 */
public class SavedQuery
{
    private String name;
    private Date dateCreated;
    private PathQuery query;

    /**
     * Construct a new instance of SavedQuery.
     * @param queryName the name of the saved query
     * @param dateCreated the date created
     * @param query the actual PathQuery
     */
    public SavedQuery(String queryName, Date dateCreated, PathQuery query) {
        super();
        this.name = queryName;
        this.dateCreated = dateCreated;
        this.query = query;
    }

    /**
     * Get the date on which the query was created.
     * @return date on which query was created
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * Get the name of the saved query.
     * @return name of saved query
     */
    public String getName() {
        return name;
    }

    /**
     * Get the saved PathQuery.
     * @return the PathQuery
     */
    public PathQuery getPathQuery() {
        return query;
    }

    /**
     * Test receiver for equality with passed object.
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        SavedQuery sq = (SavedQuery) obj;
        return (sq.name.equals(name)
                && sq.dateCreated.equals(dateCreated)
                && sq.query.toString().equals(query.toString()));
    }

    /**
     * Hash code.
     * {@inheritDoc}
     */
    public int hashCode() {
        return (name.hashCode() + 3 * dateCreated.hashCode() + 5 * query.toString().hashCode());
    }


}
