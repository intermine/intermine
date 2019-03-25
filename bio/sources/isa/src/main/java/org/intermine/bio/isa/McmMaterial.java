package ambit2.export.isa.v1_0.objects;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Material
 * Definition of Material (or Substance)
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "@id",
    "lotIdentifier",
    "name",
    "sourceName",
    "root",
    "constituents",
    "description",
    "synthesis",
    "designRationale",
    "intendedApplication",
    "characteristics",
    "mcmType",
    "chemicalName",
    "dataFiles"
})
public class McmMaterial
{
    @JsonProperty("@id")
    public URI id;
    @JsonProperty("lotIdentifier")
    public String lotIdentifier;
    @JsonProperty("name")
    public String name;
    @JsonProperty("sourceName")
    public String sourceName;
    @JsonProperty("root")
    public String root;
    @JsonProperty("constituents")
    public List<Constituent> constituents = new ArrayList<Constituent>();
    @JsonProperty("description")
    public String description;
    @JsonProperty("synthesis")
    public String synthesis;
    @JsonProperty("designRationale")
    public String designRationale;
    @JsonProperty("intendedApplication")
    public IntendedApplication intendedApplication;
    @JsonProperty("characteristics")
    public List<MaterialAttributeValue> characteristics = new ArrayList<MaterialAttributeValue>();
    @JsonProperty("mcmType")
    public McmType mcmType;
    @JsonProperty("chemicalName")
    public ChemicalName chemicalName;
    @JsonProperty("dataFiles")
    public List<DataFile> dataFiles = new ArrayList<DataFile>();
}
