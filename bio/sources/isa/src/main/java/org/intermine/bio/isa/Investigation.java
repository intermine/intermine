package ambit2.export.isa.v1_0.objects;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.util.Date;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * ISA investigation schema
 * JSON-schema representing an investigation in the ISA model
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "@id",
    "filename",
    "identifier",
    "title",
    "description",
    "submissionDate",
    "publicReleaseDate",
    "ontologySourceReferences",
    "publications",
    "people",
    "studies",
    "comments"
})
public class Investigation
{
    @JsonProperty("@id")
    public URI id;
    @JsonProperty("filename")
    public String filename;
    @JsonProperty("identifier")
    public String identifier;
    @JsonProperty("title")
    public String title;
    @JsonProperty("description")
    public String description;
    @JsonProperty("submissionDate")
    public Date submissionDate;
    @JsonProperty("publicReleaseDate")
    public Date publicReleaseDate;
    @JsonProperty("ontologySourceReferences")
    public List<OntologySourceReference> ontologySourceReferences = new ArrayList<OntologySourceReference>();
    @JsonProperty("publications")
    public List<Publication> publications = new ArrayList<Publication>();
    @JsonProperty("people")
    public List<Person> people = new ArrayList<Person>();
    @JsonProperty("studies")
    public List<Study> studies = new ArrayList<Study>();
    @JsonProperty("comments")
    public List<Comment> comments = new ArrayList<Comment>();
    
    //Material extension
    public McmMaterial mcmMaterial = null;
}
