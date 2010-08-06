package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;

/**
 * Processes list of root OboTerms to produce the equivalent Model
 *
 * @author Julie Sullivan
 */
public class OboToModel
{

    /**
     * Run conversion from Obo to Model format.
     *
     * Examples:
     *  oboName = so
     *  oboFileName = $PATH/bio/sources/so/so.obo
     *  additionsFile = $PATH/bio/sources/so/so_additions.xml
     *  packageName = org.intermine.model.bio
     *  filename = so_terms.txt (OPTIONAL)
     * @param args oboName oboFileName additionsFile packageName filename
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            String msg = "Usage: oboName oboFileName packageName filename\n "
                   + "eg. so /home/guest/so.obo org.intermine.bio /home/guest/so_terms.txt";
            throw new IllegalArgumentException(msg);
        }

        String oboName = args[0];
        String oboFilename = args[1];
        String packageName = args[2];
        File termsFile = null;
        if (args.length > 3) {
            termsFile = new File(args[3]);
        }
        String additions = oboName + "_additions.xml";
        File additionsFile = new File(additions);

        OboToModel.createAndWriteModel(oboName, oboFilename,  packageName, termsFile,
                additionsFile);
    }

    /**
     * Parses OBO file and writes an OBO_additions.xml file including those terms.
     *
     * @param oboName name of ontology, eg. SO
     * @param oboFilename path to file, eg. /home/guest/so.obo
     * @param packageName name of package, eg. org.intermine.bio
     * @param termsFile file containing list of SO terms to be included in model (optional)
     * @param outputFile file to write to
     */
    public static void createAndWriteModel(String oboName, String oboFilename, String packageName,
            File termsFile, File outputFile) {

        OboToModelMapping oboToModelMapping = new OboToModelMapping(termsFile, packageName);

        // parse oboterms, delete terms not in list
        String msg = "Starting OboToModel conversion from " + oboFilename + " to "
            + outputFile.getPath() + ".  Filtering on " + oboToModelMapping.getTermsCount()
            + " obo terms from " + termsFile.getPath();
        System.out .println(msg);
        parseOboTerms(oboToModelMapping, oboFilename, termsFile.getName());

        // classes to go into the final model
        LinkedHashSet<ClassDescriptor> clds = new LinkedHashSet<ClassDescriptor>();

        // process each oboterm - add parent and collections
        for (String childIdentifier : oboToModelMapping.getOboTermIdentifiers()) {
            // is_a
            String parents = processParents(oboToModelMapping, childIdentifier);
            // part_of
            ClassDescriptor cd = processRefsAndColls(oboToModelMapping, parents, childIdentifier);
            clds.add(cd);
        }

        // sort classes by name for readability
        Comparator<ClassDescriptor> comparator = new Comparator<ClassDescriptor>() {
            public int compare(ClassDescriptor o1, ClassDescriptor o2) {
                String fieldName1 = o1.getName().toLowerCase();
                String fieldName2 = o2.getName().toLowerCase();
                return fieldName1.compareTo(fieldName2);
            }
        };

        TreeSet<ClassDescriptor> sortedClds = new TreeSet<ClassDescriptor>(comparator);
        sortedClds.addAll(clds);

        // write out final model
        Model model = null;
        PrintWriter out = null;
        try {
            model = new Model(oboName, oboToModelMapping.getNamespace(), sortedClds);
            out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
        } catch (MetaDataException e) {
            throw new RuntimeException("Invalid model", e);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create new model file", e);
        }
        out.println(model.toAdditionsXML());
        out.flush();
        out.close();
        System.out .println("Wrote " + outputFile.getPath());
    }

    private static String processParents(OboToModelMapping oboToModelMapping,
            String childIdentifier) {
        Set<String> parents = oboToModelMapping.getParents(childIdentifier);
        Set<String> parentsInModel = new HashSet<String>();
        if (parents != null && !parents.isEmpty()) {
            for (String parentIdentifier : parents) {
                if (oboToModelMapping.classInModel(parentIdentifier)) {
                    String parentName = oboToModelMapping.getName(parentIdentifier);
                    parentName = TypeUtil.generateClassName(oboToModelMapping.getNamespace(),
                            parentName);
                    parentsInModel.add(parentName);
                }
            }
        }
        String parentList = StringUtil.join(parentsInModel, " ");
        if (StringUtils.isBlank(parentList)) {
            parentList = null;
        }
        return parentList;
    }

    private static ClassDescriptor processRefsAndColls(OboToModelMapping oboToModelMapping,
            String parents, String childIdentifier) {
        Set<AttributeDescriptor> fakeAttributes = Collections.emptySet();
        Set<ReferenceDescriptor> references = new HashSet<ReferenceDescriptor>();
        Set<CollectionDescriptor> collections = new HashSet<CollectionDescriptor>();
        Set<String> reversePartOfs = oboToModelMapping.getReversePartOfs(childIdentifier);
        Set<String> partOfIdentifiers = oboToModelMapping.getPartOfs(childIdentifier);
        String childOBOName = oboToModelMapping.getName(childIdentifier);

        // part ofs, reference to parent
        // can be a collection if in config, though
        if (partOfIdentifiers != null) {
            for (String parent : partOfIdentifiers) {
                if (oboToModelMapping.classInModel(parent)) {
                    // reference
                    String parentName = oboToModelMapping.getName(parent);
                    String fullyQualifiedClassName = TypeUtil.generateClassName(
                            oboToModelMapping.getNamespace(), parentName);
                    parentName = TypeUtil.javaiseClassName(parentName);
                    parentName = StringUtil.decapitalise(parentName);

                    // reverse reference
                    String reverseReference = TypeUtil.javaiseClassName(childOBOName) + "s";
                    reverseReference = StringUtil.decapitalise(reverseReference);

                    if (oboToModelMapping.isManyToMany(parent, childIdentifier)) {
                        parentName = parentName + "s";
                        CollectionDescriptor cd = new CollectionDescriptor(parentName,
                                fullyQualifiedClassName, reverseReference);
                        collections.add(cd);
                    } else {
                        ReferenceDescriptor rd = new ReferenceDescriptor(parentName,
                                fullyQualifiedClassName, reverseReference);
                        references.add(rd);
                    }
                }
            }
        }

        // other side of part_of relationship, collection of children
        // reverse reference can be a collection if in config
        if (reversePartOfs != null) {
            for (String collection : reversePartOfs) {
                if (oboToModelMapping.classInModel(collection)) {
                    // collection
                    String collectionName = TypeUtil.javaiseClassName(
                            oboToModelMapping.getName(collection));
                    String fullyQualifiedClassName = TypeUtil.generateClassName(
                            oboToModelMapping.getNamespace(), collectionName);
                    collectionName = StringUtil.decapitalise(collectionName) + "s";
                    // reverse reference
                    String reverseReference = TypeUtil.javaiseClassName(childOBOName);
                    reverseReference = StringUtil.decapitalise(reverseReference);
                    if (oboToModelMapping.isManyToMany(collection, childIdentifier)) {
                        reverseReference = reverseReference + "s";
                    }
                    // cd
                    CollectionDescriptor cd = new CollectionDescriptor(collectionName ,
                            fullyQualifiedClassName, reverseReference);
                    collections.add(cd);
                }
            }
        }

        String childName = TypeUtil.generateClassName(oboToModelMapping.getNamespace(),
                oboToModelMapping.getName(childIdentifier));
        return new ClassDescriptor(childName, parents, true, fakeAttributes, references,
                collections);
    }

    private static void parseOboTerms(OboToModelMapping oboToModelMapping, String oboFilename,
            String termsFileName) {
        File oboFile = new File(oboFilename);

        // parse file using OBOEdit
        OboParser parser = new OboParser();
        try {
            parser.processOntology(new FileReader(oboFile));
            parser.processRelations(oboFilename);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Couldn't find obo file", e);
        } catch (Exception e) {
            throw new RuntimeException("Parsing obo file failed", e);
        }

        // process results of parsing by OBOEdit.  flatten and trim unwanted terms
        oboToModelMapping.processOboTerms(parser.getOboTerms());
        oboToModelMapping.validateTermsToKeep(oboFilename, termsFileName);
        oboToModelMapping.processRelations(parser.getOboRelations());
    }
}
