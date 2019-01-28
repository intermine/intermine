package ambit2.export.isa.v1_0.objects;

import java.net.URI;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * ISA protocol parameter schema
 * JSON-schema representing a parameter for a protocol (category declared in the investigation file) in the ISA model
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "@id",
    "parameterName"
})
public class ProtocolParameter
{
    @JsonProperty("@id")
    public URI id;
    @JsonProperty("parameterName")
    public OntologyAnnotation parameterName;
}
