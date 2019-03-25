package org.intermine.bio.isa;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Assay JSON Schema
 * JSON Schema describing an Assay
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "@id",
    "comments",
    "filename",
    "measurementType",
    "technologyType",
    "technologyPlatform",
    "dataFiles",
    "materials",
    "characteristicCategories",
    "unitCategories",
    "processSequence"
})
public class Assay
{
    @JsonProperty("@id")
    public URI id;
    @JsonProperty("comments")
    public List<Comment> comments = new ArrayList<Comment>();
    @JsonProperty("filename")
    public String filename;
    @JsonProperty("measurementType")
    public OntologyAnnotation measurementType;
    @JsonProperty("technologyType")
    public TechnologyType technologyType;
    @JsonProperty("technologyPlatform")
    public String technologyPlatform;
    @JsonProperty("dataFiles")
    public List<Data> dataFiles = new ArrayList<Data>();
    @JsonProperty("materials")
    public Materials materials;
    @JsonProperty("characteristicCategories")
    public List<MaterialAttribute> characteristicCategories = new ArrayList<MaterialAttribute>();
    @JsonProperty("unitCategories")
    public List<OntologyAnnotation> unitCategories = new ArrayList<OntologyAnnotation>();
    @JsonProperty("processSequence")
    public List<Process> processSequence = new ArrayList<Process>();
}
