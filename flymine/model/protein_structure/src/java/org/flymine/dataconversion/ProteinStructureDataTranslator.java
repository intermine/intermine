package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.intermine.InterMineException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ItemHelper;
import org.intermine.dataconversion.ObjectStoreItemReader;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.util.XmlUtil;

/**
 * DataTranslator specific to protein structure data
 * @author Mark Woodbridge
 */
public class ProteinStructureDataTranslator extends DataTranslator
{
    protected static final String ENDL = System.getProperty("line.separator");
    proected String dataLocation;

    /**
     * @see DataTranslator#DataTranslator
     */
    public ProteinStructureDataTranslator(ItemReader srcItemReader, OntModel model, String ns, String dataLocation) {
        super(srcItemReader, model, ns);
        this.dataLocation = dataLocation;
    }

   /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {
        Collection result = new HashSet();
        String className = XmlUtil.getFragmentFromURI(srcItem.getClassName());
        if ("Fragment_Protein_structure".equals(className)) {
            String id = srcItem.getAttribute("id").getValue();

            // modelledRegion -> proteinRegion
            // link proteinRegion to protein using relation
            Item modelledRegion = getReferencedItem(srcItem, "modelled_region");
            Item proteinRegion = createItem(tgtNs + "ProteinRegion", "");
            Item protein = createItem(tgtNs + "Protein", "");
            protein.addAttribute(new Attribute("primaryAccession", modelledRegiom
                                               .getAttribute("uniprot_id").getValue()));
            Item location = createItem(tgtNs + "Location", "");
            location.addAttribute(new Attribute("start", modelledRegion
                                                .getAttribute("begin").getValue()));
            location.addAttribute(new Attribute("end", modelledRegion
                                                .getAttribute("end").getValue()));
            location.addReference(new Reference("subject", protein.getIdentifier()));
            location.addReference(new Reference("object", proteinRegion.getIdentifier()));

            // model -> modelledProteinStructure
            Item model = getReferencedItem(srcItem, "model");
            
            Item modelledProteinStructure = createItem(tgtNs + "ModelledProteinStructure", "");
            modelledProteinStructure.addAttribute(new Attribute("QScore", model
                                                                .getAttribute("prosa_q_score")
                                                                .getValue()));
            modelledProteinStructure.addAttribute(new Attribute("ZScore", model
                                                                .getAttribute("prosa_z_score")
                                                                .getValue()));
            String str;
            StringBuffer atm = new StringBuffer();
            try {
                String filename = dataLocation + id + "/" + id + ".atm";
                if (new File(filename).exists()) {
                    BufferedReader in = new BufferedReader(new FileReader(filename));
                    while ((str = in.readLine()) != null) {
                        atm.append(str + ENDL);
                    }
                    in.close();
                }
            } catch (IOException e) {
                throw new InterMineException(e);
            }
            modelledProteinStructure.addAttribute(new Attribute("atm", atm.toString()));
            
            // link proteinRegion and modelledProteinStructure using annotation
            Item annotation = createItem(tgtNs + "Annotation", "");
            annotation.addReference(new Reference("subject", proteinRegion.getIdentifier()));
            annotation.addReference(new Reference("property", modelledProteinStructure
                                                  .getIdentifier()));

            result.add(proteinRegion);
            result.add(protein);
            result.add(location);
            result.add(modelledProteinStructure);
            result.add(annotation);
        }

        return result;
    }

    /**
     * Retrieve, convert and return a referenced item
     * @param srcItem the source item
     * @param fieldName the field name
     * @return the referenced item
     * @throws ObjectStoreException if the reference can't be retrieved
     */
    protected Item getReferencedItem(Item srcItem, String fieldName) throws ObjectStoreException {
        return ItemHelper.convert(srcItemReader.getItemById(srcItem.getReference(fieldName)
                                                            .getRefId()));
    }

    /**
     * Main method
     * @param args command line arguments
     * @throws Exception if something goes wrong
     */
    public static void main (String[] args) throws Exception {
        String srcOsName = args[0];
        String tgtOswName = args[1];
        String modelName = args[2];
        String format = args[3];
        String namespace = args[4];

        ObjectStore osSrc = ObjectStoreFactory.getObjectStore(srcOsName);
        ObjectStoreWriter oswTgt = ObjectStoreWriterFactory.getObjectStoreWriter(tgtOswName);
        ItemWriter tgtItemWriter = new ObjectStoreItemWriter(oswTgt);

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new FileReader(new File(modelName)), null, format);
        ProteinStructureDataTranslator dt =
            new ProteinStructureDataTranslator(new ObjectStoreItemReader(osSrc), model, namespace, dataLocation);
        model = null;
        dt.translate(tgtItemWriter);
        tgtItemWriter.close();
    }
}
