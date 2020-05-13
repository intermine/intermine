package org.intermine.bio.isa;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

//class created for property: mcmType
//schema: mcm_material_schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "name",
    "ontologyAnnotation"
})
public class McmType
{
    @JsonProperty("name")
    public String name;
    @JsonProperty("ontologyAnnotation")
    public OntologyAnnotation ontologyAnnotation;
}