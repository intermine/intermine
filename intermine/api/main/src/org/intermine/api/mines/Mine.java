package org.intermine.api.mines;

import java.util.List;
import java.util.Set;

import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;

public interface Mine {

    /**
     * @return the name of the mine
     */
    public abstract String getName();

    /**
     * @return the description of the mine
     */
    public abstract String getDescription();

    /**
     * @return the url to the mine
     */
    public abstract String getUrl();

    /**
     * @return the logo
     */
    public abstract String getLogo();

    /**
     * @return bgcolor
     */
    public abstract String getBgcolor();

    /**
     * @return frontcolor
     */
    public abstract String getFrontcolor();

    /**
     * @return the releaseVersion
     */
    public abstract String getReleaseVersion();

    public abstract Model getModel();

    /**
     * @return the defaultValue
     */
    public abstract Set<String> getDefaultValues();

    /**
     * get first default value. used in querybuilder to select default extra value
     * @return the defaultValue
     */
    public abstract String getDefaultValue();

    public List<List<Object>> getRows(PathQuery query);

    public List<List<Object>> getRows(String xml);

}