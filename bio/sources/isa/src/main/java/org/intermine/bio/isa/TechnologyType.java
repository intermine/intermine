package org.intermine.bio.isa;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;

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
