package org.intermine.web.registry.model;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pojo class to map the intermine instance retrieved from the registry
 * @author Daniela Butano
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "name",
    "url",
    "created_at",
    "last_time_updated",
    "status",
    "isProduction",
    "twitter",
    "description",
    "api_version",
    "intermine_version",
    "release_version",
    "__v",
    "maintainerOrgName",
    "maintainerUrl",
    "maintainerEmail",
    "maintainerGithubUrl",
    "namespace",
    "organisms",
    "neighbours"
})
public class Instance
{

    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("url")
    private String url;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("last_time_updated")
    private String lastTimeUpdated;
    @JsonProperty("status")
    private String status;
    @JsonProperty("isProduction")
    private boolean isProduction;
    @JsonProperty("twitter")
    private String twitter;
    @JsonProperty("description")
    private String description;
    @JsonProperty("api_version")
    private String apiVersion;
    @JsonProperty("intermine_version")
    private String intermineVersion;
    @JsonProperty("release_version")
    private String releaseVersion;
    @JsonProperty("__v")
    private int v;
    @JsonProperty("maintainerOrgName")
    private String maintainerOrgName;
    @JsonProperty("maintainerUrl")
    private String maintainerUrl;
    @JsonProperty("maintainerEmail")
    private String maintainerEmail;
    @JsonProperty("maintainerGithubUrl")
    private String maintainerGithubUrl;
    @JsonProperty("namespace")
    private String namespace;
    @JsonProperty("organisms")
    private List<String> organisms = null;
    @JsonProperty("neighbours")
    private List<String> neighbours = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public Instance withId(String id) {
        this.id = id;
        return this;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public Instance withName(String name) {
        this.name = name;
        return this;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    public Instance withUrl(String url) {
        this.url = url;
        return this;
    }

    @JsonProperty("created_at")
    public String getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("created_at")
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Instance withCreatedAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @JsonProperty("last_time_updated")
    public String getLastTimeUpdated() {
        return lastTimeUpdated;
    }

    @JsonProperty("last_time_updated")
    public void setLastTimeUpdated(String lastTimeUpdated) {
        this.lastTimeUpdated = lastTimeUpdated;
    }

    public Instance withLastTimeUpdated(String lastTimeUpdated) {
        this.lastTimeUpdated = lastTimeUpdated;
        return this;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    public Instance withStatus(String status) {
        this.status = status;
        return this;
    }

    @JsonProperty("isProduction")
    public boolean isIsProduction() {
        return isProduction;
    }

    @JsonProperty("isProduction")
    public void setIsProduction(boolean isProduction) {
        this.isProduction = isProduction;
    }

    public Instance withIsProduction(boolean isProduction) {
        this.isProduction = isProduction;
        return this;
    }

    @JsonProperty("twitter")
    public String getTwitter() {
        return twitter;
    }

    @JsonProperty("twitter")
    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    public Instance withTwitter(String twitter) {
        this.twitter = twitter;
        return this;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    public Instance withDescription(String description) {
        this.description = description;
        return this;
    }

    @JsonProperty("api_version")
    public String getApiVersion() {
        return apiVersion;
    }

    @JsonProperty("api_version")
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Instance withApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    @JsonProperty("intermine_version")
    public String getIntermineVersion() {
        return intermineVersion;
    }

    @JsonProperty("intermine_version")
    public void setIntermineVersion(String intermineVersion) {
        this.intermineVersion = intermineVersion;
    }

    public Instance withIntermineVersion(String intermineVersion) {
        this.intermineVersion = intermineVersion;
        return this;
    }

    @JsonProperty("release_version")
    public String getReleaseVersion() {
        return releaseVersion;
    }

    @JsonProperty("release_version")
    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public Instance withReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
        return this;
    }

    @JsonProperty("__v")
    public int getV() {
        return v;
    }

    @JsonProperty("__v")
    public void setV(int v) {
        this.v = v;
    }

    public Instance withV(int v) {
        this.v = v;
        return this;
    }

    @JsonProperty("maintainerOrgName")
    public String getMaintainerOrgName() {
        return maintainerOrgName;
    }

    @JsonProperty("maintainerOrgName")
    public void setMaintainerOrgName(String maintainerOrgName) {
        this.maintainerOrgName = maintainerOrgName;
    }

    public Instance withMaintainerOrgName(String maintainerOrgName) {
        this.maintainerOrgName = maintainerOrgName;
        return this;
    }

    @JsonProperty("maintainerUrl")
    public String getMaintainerUrl() {
        return maintainerUrl;
    }

    @JsonProperty("maintainerUrl")
    public void setMaintainerUrl(String maintainerUrl) {
        this.maintainerUrl = maintainerUrl;
    }

    public Instance withMaintainerUrl(String maintainerUrl) {
        this.maintainerUrl = maintainerUrl;
        return this;
    }

    @JsonProperty("maintainerEmail")
    public String getMaintainerEmail() {
        return maintainerEmail;
    }

    @JsonProperty("maintainerEmail")
    public void setMaintainerEmail(String maintainerEmail) {
        this.maintainerEmail = maintainerEmail;
    }

    public Instance withMaintainerEmail(String maintainerEmail) {
        this.maintainerEmail = maintainerEmail;
        return this;
    }

    @JsonProperty("maintainerGithubUrl")
    public String getMaintainerGithubUrl() {
        return maintainerGithubUrl;
    }

    @JsonProperty("maintainerGithubUrl")
    public void setMaintainerGithubUrl(String maintainerGithubUrl) {
        this.maintainerGithubUrl = maintainerGithubUrl;
    }

    public Instance withMaintainerGithubUrl(String maintainerGithubUrl) {
        this.maintainerGithubUrl = maintainerGithubUrl;
        return this;
    }

    @JsonProperty("namespace")
    public String getNamespace() {
        return namespace;
    }

    @JsonProperty("namespace")
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Instance withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    @JsonProperty("organisms")
    public List<String> getOrganisms() {
        return organisms;
    }

    @JsonProperty("organisms")
    public void setOrganisms(List<String> organisms) {
        this.organisms = organisms;
    }

    public Instance withOrganisms(List<String> organisms) {
        this.organisms = organisms;
        return this;
    }

    @JsonProperty("neighbours")
    public List<String> getNeighbours() {
        return neighbours;
    }

    @JsonProperty("neighbours")
    public void setNeighbours(List<String> neighbours) {
        this.neighbours = neighbours;
    }

    public Instance withNeighbours(List<String> neighbours) {
        this.neighbours = neighbours;
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

    public Instance withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

}
