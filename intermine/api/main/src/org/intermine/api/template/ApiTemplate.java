package org.intermine.api.template;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.search.OriginatingEvent;
import org.intermine.api.search.PropertyChangeEvent;
import org.intermine.api.search.WebSearchWatcher;
import org.intermine.api.search.WebSearchable;
import org.intermine.api.tag.TagTypes;
import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.model.userprofile.Tag;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;

/**
 * This class extends TemplateQuery to provide the features needed by
 * the API - Lucene indexing and database serialisation.
 * @author Alex Kalderimis
 *
 */
public class ApiTemplate extends TemplateQuery implements WebSearchable
{

    /** SavedTemplateQuery object in the UserProfile database, so we can update summaries. */
    protected SavedTemplateQuery savedTemplateQuery = null;

    // so we can include tags
    private InterMineAPI im;

    /**
     * @param name name of template
     * @param title title of template
     * @param comment comment
     * @param query query
     */
    public ApiTemplate(String name, String title, String comment,
            PathQuery query) {
        super(name, title, comment, query);
    }

    /** Construct a new API template that has all the same properties as the TemplateQuery
     * passed into the constructor.
     *
     * @param template The prototypical template to be like.
     */
    public ApiTemplate(TemplateQuery template) {
        super(template);
    }

    /**
     * Clone this ApiQuery
     * @return template
     */
    @Override
    public synchronized ApiTemplate clone() {
        super.clone();
        return new ApiTemplate(this);
    }

    /**
     * Sets the saved template query object.
     *
     * @param savedTemplateQuery the database object
     */
    public void setSavedTemplateQuery(SavedTemplateQuery savedTemplateQuery) {
        this.savedTemplateQuery = savedTemplateQuery;
    }

    /**
     * Gets the saved template query object.
     *
     * @return a SavedTemplateQuery object that represents this TemplateQuery in the userprofile
     * database
     */
    public SavedTemplateQuery getSavedTemplateQuery() {
        return savedTemplateQuery;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((observers == null) ? 0 : observers.hashCode());
        result = prime
                * result
                + ((savedTemplateQuery == null) ? 0 : savedTemplateQuery
                        .hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    // WebSearchable Implementation //
    private final Set<WebSearchWatcher> observers = new HashSet<WebSearchWatcher>();

    @Override
    public void addObserver(WebSearchWatcher wsw) {
        observers.add(wsw);
    }

    @Override
    public void removeObserver(WebSearchWatcher wsw) {
        observers.remove(wsw);
    }

    @Override
    public String getTagType() {
        return TagTypes.TEMPLATE;
    }

    @Override
    public void fireEvent(OriginatingEvent e) {
        if (observers != null) { // Due to the order of member initialisation.
            Collection<WebSearchWatcher> watchers = new ArrayList<WebSearchWatcher>(observers);
            for (WebSearchWatcher wsw: watchers) {
                wsw.receiveEvent(e);
            }
        }
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        fireEvent(new PropertyChangeEvent(this));
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        fireEvent(new PropertyChangeEvent(this));
    }

    @Override
    public synchronized void setDescription(String description) {
        super.setDescription(description);
        fireEvent(new PropertyChangeEvent(this));
    }

    /**
     * Only used on export so we can get the tags related to this template.
     *
     * @param im InterMine API
     */
    public void setAPI(InterMineAPI im) {
        this.im = im;
    }

    @Override
    protected Map<String, Object> getHeadAttributes() {
        Map<String, Object> retVal = super.getHeadAttributes();

        if (im != null) {
            TemplateManager manager = im.getTemplateManager();
            List<Tag> tags = manager.getTags(this, im.getProfileManager().getSuperuserProfile());
            List<String> tagNames = new ArrayList<String>();
            for (Tag t: tags) {
                tagNames.add(t.getTagName());
            }
            retVal.put("tags", tagNames);
        }

        return retVal;
    }
}
