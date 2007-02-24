package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.*;

import org.intermine.InterMineException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.dataconversion.*;
import org.intermine.metadata.Model;
import org.intermine.util.XmlUtil;
import org.apache.log4j.Logger;

/**
 * DataTranslator specific to protein structure data
 * @author Mark Woodbridge
 */
public class ProteinStructureDataTranslator extends DataTranslator
{

    protected static final Logger LOG = Logger.getLogger(ProteinStructureDataTranslator.class);

    private static final String SRC_NS = "http://www.intermine.org/model/protein_structure#";
    protected static final String ENDL = System.getProperty("line.separator");
    protected Item dataSet;
    protected Map proteinFeatures = new HashMap();
    protected Item organism;
    protected String dataLocation;

    /**
     * @see DataTranslator#DataTranslator(ItemReader, Properties, Model, Model)
     */
    public ProteinStructureDataTranslator(ItemReader srcItemReader, Properties mapping,
                                          Model srcModel, Model tgtModel) {
        super(srcItemReader, mapping, srcModel, tgtModel);
        // proteins are all Drosophila
        this.organism = createItem(tgtNs + "Organism", "");
        organism.setAttribute("taxonId", "7227");
    }

    /**
     * Pick up the data location from the ant, the translator needs to open some more files.
     * @param srcdatadir location of the source data
     */
    public void setSrcDataDir(String srcdatadir) {
        this.dataLocation = srcdatadir;
    }

    /**
     * @see DataTranslator#translate(ItemWriter)
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {

        if (dataLocation == null || dataLocation.startsWith("$")) {
            throw new IllegalArgumentException("No data location specified, required"
                              + "for finding .atm structure files (was: " + dataLocation + ")");
        }

        tgtItemWriter.store(ItemHelper.convert(organism));

        dataSet = createItem(tgtNs + "DataSet", "");
        dataSet.addAttribute(new Attribute("title", "Mizuguchi protein structure predictions"));
        tgtItemWriter.store(ItemHelper.convert(dataSet));

        super.translate(tgtItemWriter);
    }

    /**
     * @see DataTranslator#translateItem(Item)
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {
        Collection result = new HashSet();
        String className = XmlUtil.getFragmentFromURI(srcItem.getClassName());
        if ("Fragment_Protein_structure".equals(className)) {
            String id = srcItem.getAttribute("id").getValue();

            // modelledRegion -> proteinRegion
            // link proteinRegion to protein using relation
            Item modelledRegion = getReference(srcItem, "modelled_region");
            Item proteinRegion = createItem(tgtNs + "ProteinRegion", "");
            Item protein = createItem(tgtNs + "Protein", "");
            String proteinAccession = modelledRegion.getAttribute("uniprot_id").getValue();
            protein.setAttribute("primaryAccession", proteinAccession);
            protein.setReference("organism", organism.getIdentifier());
            proteinRegion.addReference(new Reference("protein", protein.getIdentifier()));
            Item location = createItem(tgtNs + "Location", "");
            location.addAttribute(new Attribute("start", modelledRegion
                                                .getAttribute("begin").getValue()));
            location.addAttribute(new Attribute("end", modelledRegion
                                                .getAttribute("end").getValue()));
            location.addReference(new Reference("object", protein.getIdentifier()));
            location.addReference(new Reference("subject", proteinRegion.getIdentifier()));

            location.addCollection(new ReferenceList("evidence", Arrays.asList(new Object[]
                {dataSet.getIdentifier()})));

            // model -> modelledProteinStructure
            Item model = getReference(srcItem, "model");

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
                String filename =
                        ((dataLocation.lastIndexOf("/") == (dataLocation.length() - 1))
                                ? dataLocation
                                : dataLocation + "/")
                                + id + "/" + id + ".atm";
                if (new File(filename).exists()) {
                    BufferedReader in = new BufferedReader(new FileReader(filename));
                    while ((str = in.readLine()) != null) {
                        atm.append(str + ENDL);
                    }
                    in.close();
                } else {
                    LOG.warn("ATM FILE NOT FOUND:" + filename);
                }
            } catch (IOException e) {
                throw new InterMineException(e);
            }
            modelledProteinStructure.addAttribute(new Attribute("atm", atm.toString()));

            // link proteinRegion and modelledProteinStructure using annotation, and add shortcut
            Item annotation = createItem(tgtNs + "Annotation", "");
            annotation.addReference(new Reference("subject", proteinRegion.getIdentifier()));
            annotation.addReference(new Reference("property", modelledProteinStructure
                                                  .getIdentifier()));
            modelledProteinStructure.addReference(new Reference("region",
                                                                proteinRegion.getIdentifier()));

            // sequenceFamily -> proteinFeature
            Item sequenceFamily = getReference(srcItem, "sequence_family");
            String pfamId = sequenceFamily.getAttribute("pfam_id").getValue();
            Item proteinFeature = (Item) proteinFeatures.get(pfamId);
            if (proteinFeature == null) {
                proteinFeature = createItem(tgtNs + "ProteinFeature", "");
                proteinFeature.addAttribute(new Attribute("identifier", pfamId));
                proteinFeatures.put(pfamId, proteinFeature);
            }

            // link proteinRegion and proteinSequenceFamily
            proteinRegion.addReference(new Reference("proteinFeature",
                                                     proteinFeature.getIdentifier()));

            String structureId = proteinAccession + "-" + pfamId;
            modelledProteinStructure.setAttribute("identifier", structureId);
            result.add(proteinRegion);
            result.add(protein);
            result.add(location);
            result.add(modelledProteinStructure);
            result.add(annotation);
            result.add(proteinFeature);
        }

        return result;
    }


  /**
   * @return A map of all the interpro related prefetch descriptors.
   * */
  public static Map getPrefetchDescriptors() {
      Map paths    = new HashMap();
      Set fpsDescs = new HashSet();

      ItemPrefetchDescriptor prs2sfDesc =
              new ItemPrefetchDescriptor("(Fragment_Protein_structure.sequence_family)");
      prs2sfDesc.addConstraint(new ItemPrefetchConstraintDynamic("sequence_family",
              ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      fpsDescs.add(prs2sfDesc);

      ItemPrefetchDescriptor prs2mrDesc =
              new ItemPrefetchDescriptor("(Fragment_Protein_structure.modelled_region)");
      prs2mrDesc.addConstraint(new ItemPrefetchConstraintDynamic("modelled_region",
              ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      fpsDescs.add(prs2mrDesc);

      ItemPrefetchDescriptor prs2mDesc =
              new ItemPrefetchDescriptor("(Fragment_Protein_structure.model)");
      prs2mDesc.addConstraint(new ItemPrefetchConstraintDynamic("model",
              ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      fpsDescs.add(prs2mDesc);

      paths.put(SRC_NS + "Fragment_Protein_structure", fpsDescs);
      return paths;
  }


}
