package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * An instance of this bean is kept in the session context while the query
 * builder is in template building mode.
 * 
 * @author Thomas Riley
 */
public class TemplateBuildState
{
    private String title = "";
    private String description = "";
    private String comment = "";
    private String keywords = "";
    private String name = "";
    private boolean important;
    private TemplateQuery updating;
    
    /**
     * Construct a new instance of TemplateBuildState.
     * @param template template to take initial state from
     */
    public TemplateBuildState(TemplateQuery template) {
        description = template.getDescription();
        title = template.getTitle();
        comment = template.getComment();
        name = template.getName();
        keywords = template.getKeywords();
        important = template.isImportant();
        updating = template;
    }
    
    /**
     * Construct a new instance of TemplateBuildState.
     */
    public TemplateBuildState() {
        // empty
    }

    /**
     * @return Returns the important.
     */
    public boolean isImportant() {
        return important;
    }

    /**
     * @param important The important to set.
     */
    public void setImportant(boolean important) {
        this.important = important;
    }
    
    /**
     * Get keywords
     * @return keywords
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * Set keywords
     * @param keywords the keywords
     */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    /**
     * Get the template name
     * @return the template name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the template name
     * @param name the template name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the template title
     * @return the template title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Set the template title
     * @param title the template title
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Get the template description
     * @return the template desccription
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the template description
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the template comment
     * @return the template comment
     */
    public String getComment() {
        return comment;
    }
    
    /**
     * Set the template comment
     * @param comment the comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Get a reference to the template query being updated. This method
     * will return null if we use is not updating an existing query.
     * @return template query to update (overwrite)
     */
    public TemplateQuery getUpdatingTemplate() {
        return updating;
    }
}
