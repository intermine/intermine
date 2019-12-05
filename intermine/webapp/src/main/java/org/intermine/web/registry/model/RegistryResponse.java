package org.intermine.web.registry.model;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "instance",
    "statusCode",
    "executionTime"
})
public class RegistryResponse {

    @JsonProperty("instance")
    private Instance instance = null;
    @JsonProperty("statusCode")
    private int statusCode;
    @JsonProperty("executionTime")
    private String executionTime;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("instance")
    public Instance getInstance() {
        return instance;
    }

    @JsonProperty("instance")
    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public RegistryResponse withInstance(Instance instances) {
        this.instance = instance;
        return this;
    }

    @JsonProperty("statusCode")
    public int getStatusCode() {
        return statusCode;
    }

    @JsonProperty("statusCode")
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public RegistryResponse withStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    @JsonProperty("executionTime")
    public String getExecutionTime() {
        return executionTime;
    }

    @JsonProperty("executionTime")
    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
    }

    public RegistryResponse withExecutionTime(String executionTime) {
        this.executionTime = executionTime;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public RegistryResponse withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

}
