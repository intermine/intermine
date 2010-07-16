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

import java.io.BufferedReader;
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
        OboToModel.createAndWriteModel(oboName, oboFilename,  packageName, termsFile);
    }

    /**
     * Parses OBO file and writes an OBO_additions.xml file including those terms.
     *
     * @param oboName name of ontology, eg. SO
     * @param oboFilename path to file, eg. /home/guest/so.obo
     * @param packageName name of package, eg. org.intermine.bio
     * @param termsFile file containing list of SO terms to be included in model (optional)
     */
    public static void createAndWriteModel(String oboName, String oboFilename, String packageName,
            File termsFile) {

        Set<String> termsToKeep = processTermFile(termsFile);

        //String additionsFile = "bio/sources/so/so_additions.xml";
        String additionsFile =  oboName + "_additions.xml";
        OboToModelMapping oboToModelMapping = new OboToModelMapping(termsToKeep, packageName);

        // parse oboterms, delete terms not in list
        System.out .println("Starting OboToModel conversion from " + oboFilename + " to "
                + additionsFile);
        parseOboTerms(oboToModelMapping, oboFilename, termsFile);

        // classes to go into the final model
        LinkedHashSet<ClassDescriptor> clds = new LinkedHashSet<ClassDescriptor>();

        // process each oboterm - add parent and collections
        for (String childIdentifier : oboToModelMapping.getOboTermIdentifiers()) {
            String parents = processParents(oboToModelMapping, childIdentifier);
            ClassDescriptor cd = processCollections(oboToModelMapping, parents, childIdentifier);
            clds.add(cd);
        }

        // sort classes by name
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
        File modelFile = new File(additionsFile);
        PrintWriter out = null;
        try {
            model = new Model(oboName, oboToModelMapping.getNamespace(), sortedClds);
            out = new PrintWriter(new BufferedWriter(new FileWriter(modelFile)));
        } catch (MetaDataException e) {
            throw new RuntimeException("Bad model", e);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create new model file", e);
        }
        out.println(model);
        out.flush();
        out.close();
        System.out .println("Wrote " + additionsFile);
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

    private static ClassDescriptor processCollections(OboToModelMapping oboToModelMapping,
            String parents, String childIdentifier) {
        Set<AttributeDescriptor> fakeAttributes = Collections.emptySet();
        Set<ReferenceDescriptor> fakeReferences = Collections.emptySet();
        Set<CollectionDescriptor> collections = Collections.emptySet();
        Set<String> collectionIdentifiers = oboToModelMapping.getPartOfs(childIdentifier);
        if (collectionIdentifiers != null) {
            collections = new HashSet<CollectionDescriptor>();
            for (String partof : oboToModelMapping.getPartOfs(childIdentifier)) {
                if (oboToModelMapping.classInModel(partof)) {
                    String partOfName = oboToModelMapping.getName(partof);
                    String fullyQualifiedClassName = TypeUtil.generateClassName(
                            oboToModelMapping.getNamespace(), partOfName);
                    CollectionDescriptor cd = new CollectionDescriptor(
                        TypeUtil.javaiseClassName(partOfName) + "s", fullyQualifiedClassName, null);
                    collections.add(cd);
                }
            }
        }
        String childName = TypeUtil.generateClassName(oboToModelMapping.getNamespace(),
                oboToModelMapping.getName(childIdentifier));
        return new ClassDescriptor(childName, parents, true, fakeAttributes, fakeReferences,
                collections);
    }

    private static void parseOboTerms(OboToModelMapping oboToModelMapping, String oboFilename,
            File termsFile) {
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
        oboToModelMapping.validateTermsToKeep(oboFilename, termsFile);
        oboToModelMapping.processRelations(parser.getOboRelations());
    }

    // move terms from (user provided) file to list
    // only these terms (and dependents) will be processed
    private static Set<String> processTermFile(File filename) {
        Set<String> terms = new HashSet<String>();
        try {
            BufferedReader br =  new BufferedReader(new FileReader(filename));
            try {
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (StringUtils.isNotEmpty(line)) {
                        terms.add(line);
                    }
                }
            } finally {
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return terms;
    }
}
