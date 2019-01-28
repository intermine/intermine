package ambit2.export.isa.v1_0.objects;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * ISA ontology reference schema
 * JSON-schema representing an ontology reference or annotation in the ISA model (for fields that are required to be ontology annotations)
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "@id",
    "annotationValue",
    "termSource",
    "termAccession",
    "comments"
})
public class OntologyAnnotation
{
    @JsonProperty("@id")
    public URI id;
    @JsonProperty("annotationValue")
    public Object annotationValue;
    @JsonProperty("termSource")
    public String termSource;
    @JsonProperty("termAccession")
    public URI termAccession;
    @JsonProperty("comments")
    public List<Comment> comments = new ArrayList<Comment>();
}
