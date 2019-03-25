package ambit2.export.isa.v1_0.objects;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * ISA protocol schema
 * JSON-schema representing a protocol in the ISA model
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "@id",
    "comments",
    "name",
    "protocolType",
    "description",
    "uri",
    "version",
    "parameters",
    "components"
})
public class Protocol
{
    @JsonProperty("@id")
    public URI id;
    @JsonProperty("comments")
    public List<Comment> comments = new ArrayList<Comment>();
    @JsonProperty("name")
    public String name;
    @JsonProperty("protocolType")
    public OntologyAnnotation protocolType;
    @JsonProperty("description")
    public String description;
    @JsonProperty("uri")
    public URI uri;
    @JsonProperty("version")
    public String version;
    @JsonProperty("parameters")
    public List<ProtocolParameter> parameters = new ArrayList<ProtocolParameter>();
    @JsonProperty("components")
    public List<Component> components = new ArrayList<Component>();
}
