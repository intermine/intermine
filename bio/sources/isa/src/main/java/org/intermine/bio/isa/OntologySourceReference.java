package org.intermine.bio.isa;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.List;

/**
 * ISA ontology source reference schema
 * JSON-schema representing an ontology reference in the ISA model
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "comments",
    "description",
    "file",
    "name",
    "version"
})
public class OntologySourceReference
{
    @JsonProperty("comments")
    public List<Comment> comments = new ArrayList<Comment>();
    @JsonProperty("description")
    public String description;
    @JsonProperty("file")
    public String file;
    @JsonProperty("name")
    public String name;
    @JsonProperty("version")
    public String version;
}
