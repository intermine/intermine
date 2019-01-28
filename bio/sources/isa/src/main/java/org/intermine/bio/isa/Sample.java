package ambit2.export.isa.v1_0.objects;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * ISA sample schema
 * JSON-schema representing a sample in the ISA model. A sample represents a major output resulting from a protocol application other than the special case outputs of Extract or a Labeled Extract.
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "@id",
    "name",
    "characteristics",
    "factorValues",
    "derivesFrom"
})
public class Sample
{
    @JsonProperty("@id")
    public URI id;
    @JsonProperty("name")
    public String name;
    @JsonProperty("characteristics")
    public List<MaterialAttributeValue> characteristics = new ArrayList<MaterialAttributeValue>();
    @JsonProperty("factorValues")
    public List<FactorValue> factorValues = new ArrayList<FactorValue>();
    @JsonProperty("derivesFrom")
    public List<Source> derivesFrom = new ArrayList<Source>();
}
