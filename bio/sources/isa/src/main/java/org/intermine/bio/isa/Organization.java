package org.intermine.bio.isa;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * ISA organization schema
 * JSON-schema representing an organization in the ISA model v1.0
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "name"
})
public class Organization
{
    @JsonProperty("name")
    public String name;
}
