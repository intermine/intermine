package ambit2.export.isa.v1_0.objects;

import java.net.URI;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * ISA material attribute schema
 * JSON-schema representing a material attribute (or characteristic) value in the ISA model
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "@id",
    "category",
    "value",
    "unit"
})
public class MaterialAttributeValue
{
    @JsonProperty("@id")
    public URI id;
    @JsonProperty("category")
    public MaterialAttribute category;
    @JsonProperty("value")
    public Object value;
    @JsonProperty("unit")
    public OntologyAnnotation unit;
}
