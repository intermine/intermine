package org.intermine.bio.isa;

import java.net.URI;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

//class created for property: linkages
//schema: constituent_schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "constituent",
    "linkageType"
})
public class Linkage
{
    @JsonProperty("constituent")
    public URI constituent;
    @JsonProperty("linkageType")
    public String linkageType;
}
