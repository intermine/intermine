package ambit2.export.isa.v1_0.objects;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * ISA person schema
 * JSON-schema representing a person in the ISA model
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "@id",
    "lastName",
    "firstName",
    "midInitials",
    "email",
    "phone",
    "fax",
    "address",
    "affiliation",
    "roles",
    "comments"
})
public class Person
{
    @JsonProperty("@id")
    public URI id;
    @JsonProperty("lastName")
    public String lastName;
    @JsonProperty("firstName")
    public String firstName;
    @JsonProperty("midInitials")
    public String midInitials;
    @JsonProperty("email")
    public String email;
    @JsonProperty("phone")
    public String phone;
    @JsonProperty("fax")
    public String fax;
    @JsonProperty("address")
    public String address;
    @JsonProperty("affiliation")
    public String affiliation;
    @JsonProperty("roles")
    public List<OntologyAnnotation> roles = new ArrayList<OntologyAnnotation>();
    @JsonProperty("comments")
    public List<Comment> comments = new ArrayList<Comment>();
}
