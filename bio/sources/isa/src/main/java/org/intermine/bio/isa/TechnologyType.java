package ambit2.export.isa.v1_0.objects;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

//class created for property: technologyType
//schema: assay_schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "ontologyAnnotation"
})
public class TechnologyType
{
    @JsonProperty("ontologyAnnotation")
    public OntologyAnnotation ontologyAnnotation;
}
