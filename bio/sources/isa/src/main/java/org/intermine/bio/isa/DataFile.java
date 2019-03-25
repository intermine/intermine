package ambit2.export.isa.v1_0.objects;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

//class created for property: dataFiles
//schema: mcm_material_schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "name",
    "type",
    "description",
    "ontologyAnnotation"
})
public class DataFile
{
    @JsonProperty("name")
    public String name;
    @JsonProperty("type")
    public String type;
    @JsonProperty("description")
    public String description;
    @JsonProperty("ontologyAnnotation")
    public OntologyAnnotation ontologyAnnotation;
}
