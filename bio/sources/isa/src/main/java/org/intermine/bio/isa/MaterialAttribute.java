package ambit2.export.isa.v1_0.objects;

import java.net.URI;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * ISA material attribute schema
 * JSON-schema representing a characteristics category (what appears between the brackets in Charactersitics[]) in the ISA model
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "@id",
    "characteristicType"
})
public class MaterialAttribute
{
    @JsonProperty("@id")
    public URI id;
    @JsonProperty("characteristicType")
    public OntologyAnnotation characteristicType;
}
