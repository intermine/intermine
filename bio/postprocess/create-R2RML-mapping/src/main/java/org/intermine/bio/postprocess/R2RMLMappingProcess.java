package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2022 FlyMine
 * Copyright (C) 2020 SIB Swiss Institute of Bioinformatics
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.identifiers.IdentifiersMapper;
import org.intermine.api.rdf.RDFHelper;
import org.intermine.postprocess.PostProcessor;
import org.intermine.objectstore.ObjectStoreWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.CollectionDescriptor;

import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.sql.DatabaseUtil;
import org.intermine.api.rdf.Namespaces;

/**
 * This class translates the InterMine mapping files into
 * a <a href="https://www.w3.org/TR/r2rml/R2RML">R2RML</a>
 * mapping file.
 *
 * This allows a R2RML processor to translate
 * <a href="https://www.w3.org/TR/sparql11-overview/SPARQL">SPARQL</a> queries
 * to run against the InterMine backend database.
 *
 * @author Jerven Bolleman
 * @author Daniela Butano
 */
public class R2RMLMappingProcess extends PostProcessor
{
    static final int FORMAT_VERSION = 1;
    private String baseUri;
    private Set<String> excludedClasses;
    private static final Logger LOG = Logger.getLogger(R2RMLMappingProcess.class);


    /**
     * Create a new instance
     *
     * @param osw object store writer
     */
    public R2RMLMappingProcess(ObjectStoreWriter osw) {
        super(osw);
        excludedClasses = new HashSet<>();
        excludedClasses.add("Annotatable");
        excludedClasses.add("BioEntity");
        excludedClasses.add("Comment");
        excludedClasses.add("OntologyAnnotation");
        excludedClasses.add("OntologyTerm");
        excludedClasses.add("SequenceCollection");
        excludedClasses.add("SequenceFeature");
    }

    /**
     * set the base uri to create the predicate
     *
     * @param baseUri the base uri e.g. https://flymine.org
     */
    public void setBaseuri(String baseUri) {
        this.baseUri = (!baseUri.endsWith("/")) ? baseUri.concat("/") : baseUri;
    }

    /**
     * set the classes that will not be mapped
     *
     * @param classes the classes that will not mapped
     */
    public void setExcludes(String classes) {
        if (classes != null) {
            org.intermine.metadata.Model model =
                    org.intermine.metadata.Model.getInstanceByName("genomic");
            String[] classesArray = classes.split(",");
            String className = null;
            for (int index = 0; index < classesArray.length; index++) {
                className = classesArray[index];
                if (model.getClassDescriptorByName(className) != null) {
                    excludedClasses.add(className);
                } else {
                    LOG.error("Class " + className + " doesn't not belong to the model");
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void postProcess() throws Exception {
        final Model jenaModel = ModelFactory.createDefaultModel();
        setKnownPrefixes(jenaModel);
        Set<ClassDescriptor> classDescriptors = getMappableClasses();
        for (ClassDescriptor cd : classDescriptors) {
            mapBasicFields(cd, jenaModel);
            mapJoinToOtherTable(cd, jenaModel);
        }
        try {
            String mappingFile = "mapping.ttl";
            PrintWriter out = new PrintWriter(new FileWriter(mappingFile));
            jenaModel.write(out, "turtle");
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    private void setKnownPrefixes(final Model jenaModel) {
        Map<String, String> namespaces = Namespaces.getNamespaces();
        for (String prefix : namespaces.keySet()) {
            jenaModel.setNsPrefix(prefix, namespaces.get(prefix));
        }
        jenaModel.setNsPrefix("rr", R2RML.URI);
    }

    private Set<ClassDescriptor> getMappableClasses() {
        Set<ClassDescriptor> validClassDescriptors = new HashSet<ClassDescriptor>();
        Set<ClassDescriptor> classDescriptors =
                org.intermine.metadata.Model.getInstanceByName("genomic").getClassDescriptors();
        for (ClassDescriptor cd : classDescriptors) {
            if (isMappable(cd)) {
                validClassDescriptors.add(cd);
            }
        }
        return validClassDescriptors;
    }

    private boolean isMappable(ClassDescriptor classDescriptor) {
        String dataType = classDescriptor.getSimpleName();
        if (excludedClasses.contains(dataType)) {
            return false;
        }
        return true;
    }

    private void mapBasicFields(ClassDescriptor cd, Model model) {
        final String tableName = DatabaseUtil.getTableName(cd);
        LOG.info("TABLE: " + tableName);
        final Resource basicTableMapping = createMappingNameForTable(model, tableName);
        final Resource logicalTable = model.createResource();

        final AttributeDescriptor subjectMap = generateSubjectMap(cd, model, basicTableMapping);
        if (subjectMap != null) {
            model.add(basicTableMapping, RDF.type, R2RML.TRIPLE_MAP);
            model.add(basicTableMapping, R2RML.LOGICAL_TABLE, logicalTable);
            model.add(logicalTable, R2RML.TABLE_NAME, tableName);
        }
        for (FieldDescriptor fd : cd.getAllFieldDescriptors()) {
            String columnName = DatabaseUtil.getColumnName(fd);

            if (fd instanceof AttributeDescriptor) {
                mapPrimitiveObjects(model, basicTableMapping, (AttributeDescriptor) fd);
                LOG.info(columnName
                        + ("id".equalsIgnoreCase(columnName) ? ": PRIMARY KEY" : ": column")
                        + " with type " + ((AttributeDescriptor) fd).getType() + "\n");
            } else if (!fd.isCollection()) {
                mapManyToOne(model, basicTableMapping, fd);
            }
        }
    }

    private AttributeDescriptor generateSubjectMap(ClassDescriptor cd, Model model,
                                                          final Resource basicTableMapping) {
        String tableName = DatabaseUtil.getTableName(cd);
        for (FieldDescriptor fd : cd.getAllFieldDescriptors()) {
            String columnName = DatabaseUtil.getColumnName(fd);
            if (fd instanceof AttributeDescriptor) {
                AttributeDescriptor ad = (AttributeDescriptor) fd;
                if (isURIIdentifier(tableName, columnName)) {

                    Resource subjectMap = model.createResource();
                    model.add(basicTableMapping, R2RML.SUBJECT_MAP, subjectMap);
                    model.add(subjectMap, R2RML.TEMPLATE, createURI(tableName));

                    if (cd.getOntologyTerm() != null && !cd.getOntologyTerm().isEmpty()) {
                        String[] terms = cd.getOntologyTerm().split(",");
                        for (int index = 0; index < terms.length; index++) {
                            Resource classInOutsideWorld =
                                    ResourceFactory.createProperty(terms[index]);
                            model.add(subjectMap, R2RML.CLASS_PROPERTY, classInOutsideWorld);
                        }
                    }  else {
                        model.add(subjectMap, R2RML.CLASS_PROPERTY,
                                RDFHelper.createIMResource(cd));
                    }
                    return ad;
                }
            }
        }
        return null;
    }

    private boolean isURIIdentifier(String type, String attribute) {
        String identifier = IdentifiersMapper.getMapper().getIdentifier(type);
        if (identifier != null && identifier.equalsIgnoreCase(attribute)) {
            return true;
        }
        return false;
    }

    private String createURI(String type) {
        String identifier = IdentifiersMapper.getMapper().getIdentifier(type);
        if (identifier != null) {
/*            if (("Protein").equalsIgnoreCase(type)) {
                return RDFHelper.UNIPROT_KBNS + "{" + identifier + "}";
            } else {*/
            return baseUri + StringUtils.lowerCase(type) + ":{" + identifier + "}";
            //}
        }
        return null;
    }

    /**
     * A primitive object is field that is just a value such as true, false or 1 or "lala"
     * @param model
     * @param basicTableMapping
     * @param ad
     */
    private void mapPrimitiveObjects(Model model,
                    final Resource basicTableMapping, AttributeDescriptor ad) {
        String columnName = DatabaseUtil.getColumnName(ad);
        Resource predicateObjectMap = model.createResource();
        Resource objectMap = model.createResource();
        model.add(basicTableMapping, R2RML.PREDICATE_OBJECT_MAP, predicateObjectMap);
        model.add(predicateObjectMap, R2RML.OBJECT_MAP, objectMap);
        model.add(objectMap, RDF.type, R2RML.TERM_MAP);
        model.add(objectMap, RDF.type, R2RML.OBJECT_MAP);
        model.add(objectMap, R2RML.TERM_TYPE, R2RML.LITERAL);

        model.add(objectMap, R2RML.DATA_TYPE, getXsdForFullyQualifiedClassName(ad));
        model.add(objectMap, R2RML.COLUMN, columnName);
        model.add(predicateObjectMap, R2RML.PREDICATE, RDFHelper.createIMProperty(ad));
    }

    private AttributeDescriptor findSubjectMap(ClassDescriptor cd) {
        String tableName = DatabaseUtil.getTableName(cd);
        for (FieldDescriptor fd : cd.getAllFieldDescriptors()) {
            String columnName = DatabaseUtil.getColumnName(fd);
            if (fd instanceof AttributeDescriptor) {
                AttributeDescriptor ad = (AttributeDescriptor) fd;
                if (isURIIdentifier(tableName, columnName)) {
                    return ad;
                }
            }
        }
        return null;
    }

    /**
     * We generate a simple join condition here as in
     * <a href="https://www.w3.org/TR/r2rml/#example-fk"> the example</a>
     * of the R2RML spec
     *
     * @param model
     * @param basicTableMapping
     * @param fd
     */
    private void mapManyToOne(Model model, final Resource basicTableMapping,
                                     FieldDescriptor fd) {
        final ClassDescriptor refClassDescriptor =
                ((ReferenceDescriptor) fd).getReferencedClassDescriptor();
        org.intermine.metadata.Model imModel =
                org.intermine.metadata.Model.getInstanceByName("genomic");
        Set<ClassDescriptor> toSubDescriptors = imModel.getAllSubs(refClassDescriptor);
        if (!toSubDescriptors.isEmpty()) {
            for (ClassDescriptor toSubDescriptor : toSubDescriptors) {
                if (isMappable(toSubDescriptor)) {
                    createManyToOneResources(model, basicTableMapping, fd, toSubDescriptor);
                }
            }
        } else {
            if (isMappable(refClassDescriptor)) {
                createManyToOneResources(model, basicTableMapping, fd, refClassDescriptor);
            }
        }

    }

    private void createManyToOneResources(Model model, final Resource basicTableMapping,
                 FieldDescriptor fieldDescriptor, ClassDescriptor referencedClassDescriptor) {
        String jointTable = DatabaseUtil.getTableName(referencedClassDescriptor);
        if (findSubjectMap(referencedClassDescriptor) != null) {
            Resource objectMap = model.createResource();
            Resource objectPredicateMap = model.createResource();
            Resource joinCondition = model.createResource();
            model.add(basicTableMapping, R2RML.PREDICATE_OBJECT_MAP, objectPredicateMap);
            model.add(objectPredicateMap, R2RML.PREDICATE,
                    RDFHelper.createIMProperty((ReferenceDescriptor) fieldDescriptor));
            model.add(objectPredicateMap, R2RML.OBJECT_MAP, objectMap);
            model.add(objectMap, R2RML.PARENT_TRIPLE_MAP,
                    createMappingNameForTable(model, jointTable));
            model.add(objectMap, R2RML.JOIN_CONDITION, joinCondition);
            model.add(joinCondition, R2RML.CHILD, fieldDescriptor.getName() + "id");
            model.add(joinCondition, R2RML.PARENT, "id");
        }
    }

    /***
     * Here we map two tables to each other linked by many_to_many relationships
     * Normally one would follow https://www.w3.org/TR/r2rml/#example-m2m
     *
     * However, that does not apply here as we the intermine primary keys are internal only.
     * We need to expose the relation as it is in the outside world.
     *
     * This means building a 'view' that links the two tables together via their external known
     * URIs.
     *
     * @param cd
     * @param model
     */
    private void mapJoinToOtherTable(ClassDescriptor cd, Model model) {
        for (CollectionDescriptor collection : cd.getAllCollectionDescriptors()) {
            if (FieldDescriptor.M_N_RELATION == collection.relationType()) {
                String indirectionTable = DatabaseUtil.getIndirectionTableName(collection);
                LOG.info("JOINING TABLE: " + indirectionTable);

                final ClassDescriptor toTableDescription =
                        collection.getReferencedClassDescriptor();

                org.intermine.metadata.Model imModel =
                        org.intermine.metadata.Model.getInstanceByName("genomic");
                Set<ClassDescriptor> toSubDescriptors = imModel.getAllSubs(toTableDescription);
                if (!toSubDescriptors.isEmpty()) {
                    for (ClassDescriptor toSubDescriptor : toSubDescriptors) {
                        if (isMappable(toSubDescriptor)) {
                            createManyToManyResources(model, cd, toSubDescriptor, collection);
                        }
                    }
                } else {
                    if (isMappable(toTableDescription)) {
                        createManyToManyResources(model, cd, toTableDescription, collection);
                    }
                }
            }
        }
    }

    private void createManyToManyResources(Model model, ClassDescriptor fromTableDescription,
               ClassDescriptor toTableDescription, CollectionDescriptor collection) {
        final String fromTableName = DatabaseUtil.getTableName(fromTableDescription);
        final String joinTableName = DatabaseUtil.getIndirectionTableName(collection);
        final String toTableName = DatabaseUtil.getTableName(toTableDescription);
        String fromJoinColumn =
                DatabaseUtil.getInwardIndirectionColumnName(collection, FORMAT_VERSION);
        String toJoinColumn =
                DatabaseUtil.getOutwardIndirectionColumnName(collection, FORMAT_VERSION);
        Resource table = model.createResource();
        if (findSubjectMap(fromTableDescription) != null) {
            Resource jointTriplesMap = createMappingNameForJoinTable(model, fromTableName,
                    joinTableName, toTableName);
            final AttributeDescriptor toColumnName = findSubjectMap(toTableDescription);
            if (toColumnName != null) {
                final AttributeDescriptor fromColumnname =
                        generateSubjectMap(fromTableDescription, model, jointTriplesMap);
                Resource objectPredicateMap = model.createResource();
                Resource objectMap = model.createResource();
                model.add(jointTriplesMap, RDF.type, R2RML.TRIPLE_MAP);
                model.add(jointTriplesMap, R2RML.LOGICAL_TABLE, table);
                model.add(table, RDF.type, R2RML.R2RML_VIEW);
                final Stream<String> distinct =
                        Stream.of(fromTableName, joinTableName, toTableName).distinct();
                String tables = distinct.collect(Collectors.joining(","));
                // We build a big sql query to join internally via the intermine id's.
                // But expose the external identifiers only.
                // We need the "AS" fromColumnname because that column name is used in the
                // GenerateSubjectsMap method.
                model.add(table, R2RML.SQL_QUERY,
                    "SELECT " + fromTableName + "." + fromColumnname.getName() + ", " + toTableName
                        + "." + toColumnName.getName() + " AS toColumnName  FROM " + tables
                        + " WHERE " + fromTableName + ".id = "
                        + joinTableName + "." + fromJoinColumn + " AND "
                        + toTableName + ".id = " + joinTableName + "." + toJoinColumn);
                model.add(jointTriplesMap, R2RML.PREDICATE_OBJECT_MAP, objectPredicateMap);
                model.add(objectPredicateMap, R2RML.PREDICATE,
                        RDFHelper.createIMProperty(collection));
                model.add(objectPredicateMap, R2RML.OBJECT_MAP, objectMap);
                model.add(objectMap, RDF.type, R2RML.TERM_MAP);
                model.add(objectMap, RDF.type, R2RML.OBJECT_MAP);
                // We need to disambiguate here as the toColumnName and fromColumnName might
                // be lexically the same.
                model.add(objectMap, R2RML.COLUMN, toColumnName.getName());
                model.add(objectMap, R2RML.TERM_TYPE, R2RML.IRI);
                model.add(objectMap, R2RML.TEMPLATE, createURI(toTableName, "toColumnName"));
            }
        }
    }

    private String createURI(String type, String allias) {
/*        if (("Protein").equalsIgnoreCase(type)) {
            return RDFHelper.UNIPROT_KBNS + "{" + allias + "}";
        } else {*/
        return baseUri + StringUtils.lowerCase(type) + ":{" + allias + "}";
       // }
    }

    private Resource createMappingNameForTable(Model model, final String tableName) {
        return model.createResource("urn:intermine-table:" + tableName);
    }

    private Resource createMappingNameForJoinTable(Model model, final String tableName,
                   final String joinTableName, final String otherTableName) {
        return model.createResource("urn:intermine-join-tables:"
                + tableName + '/' + joinTableName + '/' + otherTableName);
    }

    /**
     * We need to map the java fully qualified object name to the right kind
     * of XSD type.
     * @param ad
     * @return an XSD for the value
     */
    private Resource getXsdForFullyQualifiedClassName(AttributeDescriptor ad) {
        switch (ad.getType()) {
            case "org.intermine.objectstore.query.ClobAccess":
            case "java.lang.String":
                return XSD.xstring;
            case "java.lang.Boolean":
                return XSD.xboolean;
            case "int":
            case "java.lang.Integer":
                return XSD.integer;
            case "double":
            case "java.lang.Double":
                return XSD.decimal;
            case "java.lang.Float":
                return XSD.xfloat;
            default: {
                LOG.error("Unknown primitive datatype: " + ad.getType());
                return null;
            }
        }
    }
}
