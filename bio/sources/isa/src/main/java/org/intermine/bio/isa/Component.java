package org.intermine.bio.isa;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

//class created for property: components
//schema: protocol_schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "componentName",
    "componentType"
})
public class Component
{
    @JsonProperty("componentName")
    public String componentName;
    @JsonProperty("componentType")
    public OntologyAnnotation componentType;
}
