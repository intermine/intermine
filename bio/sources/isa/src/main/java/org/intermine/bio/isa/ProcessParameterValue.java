package ambit2.export.isa.v1_0.objects;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * ISA process parameter value schema
 * JSON-schema representing a Parameter Value (associated with a Protocol REF) in the ISA model
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "category",
    "value",
    "unit"
})
public class ProcessParameterValue
{
    @JsonProperty("category")
    public ProtocolParameter category;
    @JsonProperty("value")
    public Object value;
    @JsonProperty("unit")
    public OntologyAnnotation unit;
}
