package org.intermine.web.logic.config;

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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Bean to hold configuration for report displayers.  One instance of this class represents
 * a single report page displayer.
 * @author Richard Smith
 *
 */
public class ReportDisplayerConfig
{
    private String javaClass;
    private String jspName;
    private String placement;
    private String parameters;
    private String replacesFields;
    private String types;
    private Set<String> configuredTypes = null;
    private Set<String> replacedFieldNames = null;
    private Boolean showImmediately = false;

    /**
     * Get the comma separated unqualified class names for which this displayer should be displayed.
     * Provides direct access to the value provided in the config file.
     * @return a comma separated list of unqualified class names
     */
    public String getTypes() {
        return types;
    }

    /**
     * Set the types this displayer should be used for as a comma separated list of unqualified
     * class names.
     * @param types a comma separated list of unqualified class names
     */
    public void setTypes(String types) {
        this.types = types;
    }

    /**
     * Get the unqualified class names for which this displayer should be displayed.  This returns
     * splits values listed in the webconfig-model.xml, inheritance is handled elsewhere.
     * @return unqualified class names
     */
    public Set<String> getConfiguredTypes() {
        if (configuredTypes == null) {
            configuredTypes = new HashSet<String>();
            for (String type : types.split(",")) {
                configuredTypes.add(type.trim());
            }
        }
        return configuredTypes;
    }

    /**
     * Get the Java class name of the controller for this displayer.
     * @return a fully qualified java class name
     */
    public String getJavaClass() {
        return javaClass;
    }

    /**
     * Set the Java class name of the controller for this displayer.
     * @param javaClass a fully qualified java class name
     */
    public void setJavaClass(String javaClass) {
        this.javaClass = javaClass;
    }

    /**
     * Get the name of the JSP associated with this displayer.
     * @return The name of the JSP associated with this displayer
     */
    public String getJspName() {
        return jspName;
    }

    /**
     * Set the name of the JSP associated with this displayer.
     * @param jspName the name of the JSP
     */
    public void setJspName(String jspName) {
        this.jspName = jspName;
    }

    /**
     * Get the data category name under which this displayer should appear.
     * @return a data category name
     */
    public String getPlacement() {
        return placement;
    }

    /**
     * Set the data category name under which this displayer should appear.
     * @param placement a data category name
     */
    public void setPlacement(String placement) {
        this.placement = placement;
    }

    /**
     * A JSON string representing custom parameters for this displayer.  Anything can be specified
     * in the parameters, the controller for the displayer needs to read and handle them.
     * @return parameters the JSON string with custom parameters from the config file
     */
    public String getParameterString() {
        return parameters;
    }

    /**
     * Get the parameters for this displayer as a JSONObject
     * @return The parameters as a JSONObject
     * @throws RuntimeException if the parameter string does not parse into JSON
     */
    public JSONObject getParameterJson() {
        try {
            JSONObject paramObj = new JSONObject(parameters);
            return paramObj;
        } catch (JSONException e) {
            throw new RuntimeException("Could not parse displayer parameters", e);
        }
    }

    /**
     * A JSON string representing custom parameters for this displayer.  Anything can be specified
     * in the parameters, the controller for the displayer needs to read and handle them.
     * @param parameters a JSON string with custom parameters
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    /**
     * Get the comma separated list of field names that should not be displayed on the report page
     * when this displayer is used.  May be an empty string.
     * @return a comma separated list of field names
     */
    public String getReplacesFields() {
        return replacesFields;
    }

    /**
     * Set the comma separated list of field names that should not be displayed on the report page
     * when this displayer is used.  May be an empty string.
     * @param replacesFields a comma separated list of field names
     */
    public void setReplacesFields(String replacesFields) {
        this.replacesFields = replacesFields;
    }

    /**
     * Should we display this 'splayer immediately, wo/ waiting for the AJAX call?
     * @return true if we should display immediately
     */
    public Boolean getShowImmediately() {
        return showImmediately;
    }

    /**
     * Set if we should display the displayer immediately
     * @param showImmediately sets whether to display immediately or not
     */
    public void setShowImmediately(Boolean showImmediately) {
        this.showImmediately  = showImmediately;
    }

    /**
     * Get the names of fields this displayer replaces.
     *
     * @return a set of names for all fields of this class that no longer need displaying.
     */
    public Set<String> getReplacedFieldNames() {
        if (replacedFieldNames == null) {
            replacedFieldNames = new HashSet<String>();
            for (String name : replacesFields.split(",")) {
                replacedFieldNames.add(name.trim());
            }
        }
        return replacedFieldNames;
    }
}
