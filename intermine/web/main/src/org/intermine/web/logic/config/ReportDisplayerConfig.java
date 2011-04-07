package org.intermine.web.logic.config;

import java.util.HashSet;
import java.util.Set;

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
    public String getJspName() {
        return jspName;
    }
    public void setJspName(String jspName) {
        this.jspName = jspName;
    }
    public String getPlacement() {
        return placement;
    }
    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public String getParameters() {
        return parameters;
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
