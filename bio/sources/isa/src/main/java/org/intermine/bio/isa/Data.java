package org.intermine.bio.isa;

import com.fasterxml.jackson.annotation.*;

import javax.annotation.Generated;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ISA data schema
 * JSON-schema representing a data file in the ISA model
**/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("ambit.json2pojo")
@JsonPropertyOrder({
    "@id",
    "name",
    "type",
    "comments"
})
public class Data
{
    @JsonProperty("@id")
    public URI id;
    @JsonProperty("name")
    public String name;
    @JsonProperty("type")
    public Data.Type type;
    @JsonProperty("comments")
    public List<Comment> comments = new ArrayList<Comment>();

    @Generated("ambit.json2pojo")
    public static enum Type {
        METABOLYTE_ASSIGNMENT_FILE("Metabolite Assignment File"),
        RAW_DATA_FILE("Raw Data File"),
        DERIVED_DATA_FILE("Derived Data File"),
        IMAGE_FILE("Image File"),
        ACQUISITION_PARAMETER_DATA_FILE("Acquisition Parameter Data File"),
        DERIVED_SPECTRAL_DATA_FILE("Derived Spectral Data File"),
        PROTEIN_ASSIGNMENT_FILE("Protein Assignment File"),
        RAW_SPECTRAL_DATA_FILE("Raw Spectral Data File"),
        PEPTIDE_ASSIGNMENT_FILE("Peptide Assignment File"),
        ARRAY_DATA_FILE("Array Data File"),
        DERIVED_ARRAY_DATA_FILE("Derived Array Data File"),
        POST_TRANSLATIONAL_MODIFICATION_ASSIGNMENT_FILE("Post Translational Modification Assignment File"),
        DERIVED_ARRAY_DATA_MATRIX_FILE("Derived Array Data Matrix File");
        private final String value;
        private static Map<String, Data.Type> constants = new HashMap<String, Data.Type>();

        static {
            for (Data.Type c: values()) {
                constants.put(c.value, c);
            }
        }

        private Type(String value) {
            this.value = value;
        }

        @JsonValue
        @Override
        public String toString() {
            return this.value;
        }

        @JsonCreator
        public static Data.Type fromValue(String value) {
            Data.Type constant = constants.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }
}
