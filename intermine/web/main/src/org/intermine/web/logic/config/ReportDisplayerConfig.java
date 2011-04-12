package org.intermine.web.logic.config;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

public class ReportDisplayerConfig {
    private String javaClass;
    private String jspName;
    private String placement;
    private String parameters;
    private String replacesFields;
    private String types;
    private Set<String> configuredTypes = null;
    private Set<String> replacedFieldNames = null;

    public String getTypes() {
        return types;
    }
    public void setTypes(String types) {
        this.types = types;
    }

    public Set<String> getConfiguredTypes() {
        if (configuredTypes == null) {
            configuredTypes = new HashSet<String>();
            for (String type : types.split(",")) {
                configuredTypes.add(type.trim());
            }
        }
        return configuredTypes;
    }

    public String getJavaClass() {
        return javaClass;
    }
    public void setJavaClass(String javaClass) {
        this.javaClass = javaClass;
    }

    /**
     * @return The name of the JSP associated with this displayer
     */
    public String getJspName() {
        return jspName;
    }

    /**
     * Set the name of the JSP associated with this displayer
     * @param jspName the name of the JSP
     */
    public void setJspName(String jspName) {
        this.jspName = jspName;
    }

    public String getPlacement() {
        return placement;
    }
    public void setPlacement(String placement) {
        this.placement = placement;
    }

    /**
     * Get the parameters for this displayer as a string.
     * @return The string used to configure this displayer.
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
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    public String getReplacesFields() {
        return replacesFields;
    }
    public void setReplacesFields(String replacesFields) {
        this.replacesFields = replacesFields;
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
