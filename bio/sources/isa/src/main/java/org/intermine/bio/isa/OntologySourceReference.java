package ambit2.export.isa.v1_0.objects;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

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
