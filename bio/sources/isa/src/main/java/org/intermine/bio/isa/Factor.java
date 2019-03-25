package ambit2.export.isa.v1_0.objects;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * ISA factor schema
 * JSON-schema representing a factor value in the ISA model
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "@id",
    "factorName",
    "factorType",
    "comments"
})
public class Factor
{
    @JsonProperty("@id")
    public URI id;
    @JsonProperty("factorName")
    public String factorName;
    @JsonProperty("factorType")
    public OntologyAnnotation factorType;
    @JsonProperty("comments")
    public List<Comment> comments = new ArrayList<Comment>();
}
