package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2021 FlyMine
 * Copyright (C) 2020 SIB Swiss Institute of Bioinformatics
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Class facilitating the creation of R2RML properties
 * @author Jerven Bolleman
 * @author Daniela Butano
 */
public final class R2RML
{
    private R2RML() {
        // Hidden constructor.
    }

    /**
     * R2RML namespace
     */
    public static final String URI = "http://www.w3.org/ns/r2rml#";

    /**
     * Create TriplesMap property
     */
    public static final Resource TRIPLE_MAP = ResourceFactory.createProperty(URI, "TriplesMap");

    /**
     * Create logicalTable property
     */
    public static final Property LOGICAL_TABLE = ResourceFactory.createProperty(URI,
            "logicalTable");

    /**
     * Create tableName property
     */
    public static final Property TABLE_NAME = ResourceFactory.createProperty(URI, "tableName");

    /**
     * Create subjectMap property
     */
    public static final Property SUBJECT_MAP = ResourceFactory.createProperty(URI, "subjectMap");

    /**
     * Create template property
     */
    public static final Property TEMPLATE = ResourceFactory.createProperty(URI, "template");

    /**
     * Create class property
     */
    public static final Property CLASS_PROPERTY = ResourceFactory.createProperty(URI, "class");

    /**
     * Create objectMap property
     */
    public static final Property OBJECT_MAP = ResourceFactory.createProperty(URI, "objectMap");

    /**
     * Create TermMap property
     */
    public static final RDFNode TERM_MAP = ResourceFactory.createProperty(URI, "TermMap");

    /**
     * Create Literal property
     */
    public static final RDFNode LITERAL = ResourceFactory.createProperty(URI, "Literal");

    /**
     * Create termType property
     */
    public static final Property TERM_TYPE = ResourceFactory.createProperty(URI, "termType");

    /**
     * Create datatype property
     */
    public static final Property DATA_TYPE = ResourceFactory.createProperty(URI, "datatype");

    /**
     * Create property
     */
    public static final Property COLUMN = ResourceFactory.createProperty(URI, "column");

    /**
     * Create column property
     */
    public static final Property PREDICATE = ResourceFactory.createProperty(URI, "predicate");

    /**
     * Create predicate property
     */
    public static final Property PARENT_TRIPLE_MAP = ResourceFactory.createProperty(URI,
            "parentTriplesMap");

    /**
     * Create joinCondition property
     */
    public static final Property JOIN_CONDITION = ResourceFactory.createProperty(URI,
            "joinCondition");

    /**
     * Create child property
     */
    public static final Property CHILD = ResourceFactory.createProperty(URI, "child");

    /**
     * Create parent property
     */
    public static final Property PARENT = ResourceFactory.createProperty(URI, "parent");

    /**
     * Create R2RMLView property
     */
    public static final Property R2RML_VIEW = ResourceFactory.createProperty(URI, "R2RMLView");

    /**
     * Create property
     */
    public static final Property SQL_QUERY = ResourceFactory.createProperty(URI, "sqlQuery");

    /**
     * Create sqlQuery property
     */
    public static final Property PREDICATE_OBJECT_MAP = ResourceFactory.createProperty(URI,
            "predicateObjectMap");

    /**
     * Create IRI property
     */
    public static final RDFNode IRI = ResourceFactory.createProperty(URI, "IRI");
}
