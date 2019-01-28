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
 * Study JSON Schema
 * JSON Schema describing an Study
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
    "publications",
    "people",
    "studyDesignDescriptors",
    "protocols",
    "materials",
    "processSequence",
    "assays",
    "factors",
    "characteristicCategories",
    "unitCategories",
    "comments"
})
public class Study
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
    @JsonProperty("publications")
    public List<Publication> publications = new ArrayList<Publication>();
    @JsonProperty("people")
    public List<Person> people = new ArrayList<Person>();
    @JsonProperty("studyDesignDescriptors")
    public List<OntologyAnnotation> studyDesignDescriptors = new ArrayList<OntologyAnnotation>();
    @JsonProperty("protocols")
    public List<Protocol> protocols = new ArrayList<Protocol>();
    @JsonProperty("materials")
    public Materials_ materials;
    @JsonProperty("processSequence")
    public List<Process> processSequence = new ArrayList<Process>();
    @JsonProperty("assays")
    public List<Assay> assays = new ArrayList<Assay>();
    @JsonProperty("factors")
    public List<Factor> factors = new ArrayList<Factor>();
    @JsonProperty("characteristicCategories")
    public List<MaterialAttribute> characteristicCategories = new ArrayList<MaterialAttribute>();
    @JsonProperty("unitCategories")
    public List<OntologyAnnotation> unitCategories = new ArrayList<OntologyAnnotation>();
    @JsonProperty("comments")
    public List<Comment> comments = new ArrayList<Comment>();
}
