package org.flymine.ontology;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;

import org.flymine.metadata.*;
import org.flymine.util.TypeUtil;
import org.flymine.modelproduction.ModelParser;
import org.flymine.modelproduction.xml.FlyMineModelParser;

/**
 * Convert a FlyMine metadata model to a Jena OntModel.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class FlyMine2Owl
{

    /**
     * Create a Jena OntModel by processing a FlyMine metadata Model.
     * @param model the source FlyMine metadata model
     * @return a Jena OntModel
     */
    public OntModel process(Model model) {

        OntModel ont = ModelFactory.createOntologyModel();
        String tgtNamespace = model.getNameSpace().toString();

        Iterator i = model.getClassDescriptors().iterator();
        while (i.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) i.next();
            if (cld.getName().equals("org.flymine.model.FlyMineBusinessObject")) {
                continue;
            }

            OntClass ontCls = getOntClass(cld, ont);
            Iterator j = cld.getSubDescriptors().iterator();
            while (j.hasNext()) {
                ontCls.addSubClass(getOntClass((ClassDescriptor) j.next(), ont));
            }

            Iterator k = cld.getFieldDescriptors().iterator();
            while (k.hasNext()) {
                FieldDescriptor fld = (FieldDescriptor) k.next();
                if (fld.isAttribute()) {
                    DatatypeProperty prop = ont.createDatatypeProperty(
                        OntologyUtil.generatePropertyName(fld));
                    prop.setDomain(ontCls);
                    prop.setRange(ont.createResource(
                        OntologyUtil.javaToXmlType(((AttributeDescriptor) fld).getType())));
                } else {
                    ReferenceDescriptor rfd = (ReferenceDescriptor) fld;
                    ObjectProperty prop = getObjectProperty(rfd, ont);

                    // consistent order for which property is owl:inverseOf
                    ReferenceDescriptor revRfd = rfd.getReverseReferenceDescriptor();
                    String propName = OntologyUtil.generatePropertyName(rfd);
                    if (revRfd != null
                        && propName.compareTo(OntologyUtil.generatePropertyName(revRfd)) > 0) {
                        prop.addInverseOf(getObjectProperty(revRfd, ont));

                    } else {
                        prop.setDomain(ontCls);
                        prop.setRange(getOntClass(((ReferenceDescriptor) fld)
                                                  .getReferencedClassDescriptor(), ont));
                    }
                    if (rfd.isReference()) {
                        ontCls.addSuperClass(ont.createMaxCardinalityRestriction(null, prop, 1));
                    }
                }
            }
        }
        return ont;
    }


    /**
     * Create or get from OntModel an OntClass corresponding to name of the given
     * ClassDescriptor.
     * @param cld ClassDescriptor to create/get OntClass for
     * @param ont an OntModel to create/get OntClass
     * @return the OntClass
     */
    protected OntClass getOntClass(ClassDescriptor cld, OntModel ont) {
        String uri =  cld.getModel().getNameSpace() + TypeUtil.unqualifiedName(cld.getName());
        OntClass ontCls = ont.getOntClass(uri);
        if (ontCls == null) {
            ontCls = ont.createClass(uri);
        }
        return ontCls;
    }

    /**
     * Create or get from OntModel an ObjectProperty corresponding to name of the given
     * ReferenceDescriptor.
     * @param rfd ReferenceDescriptor to create/get ObjectProperty for
     * @param ont an OntModel to create/get ObjectProperty in
     * @return the ObjectProperty
     */
    protected ObjectProperty getObjectProperty(ReferenceDescriptor rfd, OntModel ont) {
        ClassDescriptor cld = rfd.getClassDescriptor();
        //String uri = OntologyUtil.generatePropertyName(rfd);
        String uri =  cld.getModel().getNameSpace() + TypeUtil.unqualifiedName(cld.getName())
            + "__" + rfd.getName();
        ObjectProperty prop = ont.getObjectProperty(uri);
        if (prop == null) {
            prop = ont.createObjectProperty(uri);
        }
        return prop;
    }


    /**
     * Main method to convert FlyMine model XML to OWL.
     * @param args srcFilename, tgtFilename, OWL format
     * @throws Exception if anything goes wrong
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: FlyMine2Owl source dest format");
        }
        String srcFilename = args[0];
        String tgtFilename = args[1];
        String format = args[2];

        File srcFile = new File(srcFilename);
        File tgtFile = new File(tgtFilename);

        FlyMine2Owl f2o = new FlyMine2Owl();
        Model model = null;
        ModelParser parser = new FlyMineModelParser();
        model = parser.process(new FileReader(srcFile));

        OntModel ont = f2o.process(model);
        FileWriter writer = new FileWriter(tgtFile);
        ont.write(writer, format);
        writer.close();
    }

}
