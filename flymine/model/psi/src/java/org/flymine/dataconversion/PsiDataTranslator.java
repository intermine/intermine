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
import java.io.FileReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

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
 * DataTranslator specific to Protein Interaction data in PSI XML format.
 *
 * @author Richard Smith
 * @author Andrew Varley
 */
public class PsiDataTranslator extends DataTranslator
{
    private Item db = null;
    private Item swissProt = null;
    private Map pubs = new HashMap();
    private Set noPubs = new HashSet();

    /**
     * @see DataTranslator#DataTranslator
     */
    public PsiDataTranslator(ItemReader srcItemReader, OntModel model, String ns) {
        super(srcItemReader, model, ns);
    }

    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {

        tgtItemWriter.store(ItemHelper.convert(getSwissProt()));
        super.translate(tgtItemWriter);
    }

   /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {

        Collection result = new HashSet();
        String srcNs = XmlUtil.getNamespaceFromURI(srcItem.getClassName());
        String className = XmlUtil.getFragmentFromURI(srcItem.getClassName());
        Collection translated = super.translateItem(srcItem);
        if (translated != null) {
            for (Iterator i = translated.iterator(); i.hasNext();) {
                boolean storeTgtItem = true;
                Item tgtItem = (Item) i.next();
                String tgtClassName =  XmlUtil.getFragmentFromURI(tgtItem.getClassName());
                if ("ExperimentType".equals(className)) {
                    Item pub = createPublication(srcItem);
                    if (pub != null) {
                        tgtItem.addReference(new Reference("publication", pub.getIdentifier()));
                        result.add(pub);
                    }
                    // no confidence values in current data set
                } else if ("InteractionElementType".equals(className)) {
                    // create ProteinInteraction relation
                    Item interaction = createProteinInteraction(srcItem, tgtItem);
                    addReferencedItem(tgtItem, interaction, "relations", true, "evidence", true);

                    // reference source database
                    addReferencedItem(tgtItem, getDb(), "source", false, "", false);

                    // ExperimentalResult need to reference ProteinInteractionExperiment
                    promoteField(tgtItem, srcItem, "analysis", "experimentList", "experimentRef");

                    // set confidence from attributeList
                    if (srcItem.getReference("attributeList") != null) {
                        Iterator iter = ItemHelper.convert(srcItemReader.getItemById(srcItem
                                                         .getReference("attributeList").getRefId()))
                            .getCollection("attributes").getRefIds().iterator();
                        while (iter.hasNext()) {
                            Item attribute = ItemHelper.convert(srcItemReader
                                                                .getItemById((String) iter.next()));
                            String value = attribute.getAttribute("attribute").getValue().trim();
                            String name = attribute.getAttribute("name").getValue().trim();
                            if (Character.isDigit(value.charAt(0))
                                && name.equals("author-confidence")) {
                                tgtItem.addAttribute(new Attribute("confidence", value));
                            }
                        }
                    }
                    result.add(interaction);
                } else if ("ProteinInteractorType".equals(className)) {
                    // protein needs swissprot id set and synonym to swissprot
                    Item xref = ItemHelper.convert(srcItemReader
                                  .getItemById(srcItem.getReference("xref").getRefId()));
                    Item dbXref = ItemHelper.convert(srcItemReader
                                    .getItemById(xref.getReference("primaryRef").getRefId()));
                    if (dbXref.getAttribute("db").getValue().equals("uniprot")) {
                        String value = dbXref.getAttribute("id").getValue();
                        tgtItem.addAttribute(new Attribute("swissProtId", value));
                        //tgtItem.addAttribute(new Attribute("identifer", value));
                        Item synonym = createItem(tgtNs + "Synonym", "");
                        addReferencedItem(synonym, getSwissProt(), "source", false, "", false);
                        synonym.addAttribute(new Attribute("synonym", value));
                        addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                        result.add(synonym);
                    }
                } else if ("Source_Entry_EntrySet".equals(className)) {
                    tgtItem.setIdentifier(getDb().getIdentifier());
                    tgtItem.addAttribute(new Attribute("title",
                            ItemHelper.convert(srcItemReader
                                 .getItemById(srcItem.getReference("names").getRefId()))
                                                 .getAttribute("shortLabel").getValue()));
                } else if ("ProteinInteractionTerm".equals(tgtClassName)) {
                    Item xref = ItemHelper.convert(srcItemReader
                                                   .getItemById(srcItem.getReference("xref")
                                                                .getRefId()));
                    Item dbXref = ItemHelper.convert(srcItemReader
                                                     .getItemById(xref.getReference("primaryRef")
                                                                  .getRefId()));
                    tgtItem.addAttribute(new Attribute("identifier",
                                                       dbXref.getAttribute("id").getValue()));
                }
                result.add(tgtItem);
            }
        }
        return result;
    }

    // create a publication given an ExperimentType item
    private Item createPublication(Item exptType) throws ObjectStoreException {
        Item bibrefType = ItemHelper.convert(srcItemReader
                            .getItemById(exptType.getReference("bibref").getRefId()));
        Item xrefType = ItemHelper.convert(srcItemReader
                          .getItemById(bibrefType.getReference("xref").getRefId()));
        Item dbrefType = ItemHelper.convert(srcItemReader
                           .getItemById(xrefType.getReference("primaryRef").getRefId()));

        Item pub = null;
        if (dbrefType != null && dbrefType.getAttribute("db").getValue().equals("pubmed")) {
            pub = getPub(exptType);
            pub.addAttribute(new Attribute("pubMedId", dbrefType.getAttribute("id").getValue()));
        }
        return pub;
    }

    private Item createProteinInteraction(Item intElType, Item tgtItem)
        throws ObjectStoreException {
        Item interaction = createItem(tgtNs + "ProteinInteraction", "");

        Item experimentList = ItemHelper.convert(srcItemReader
                                 .getItemById(intElType.getReference("experimentList").getRefId()));
        String experimentId = experimentList.getReference("experimentRef").getRefId();
        Item exptType = ItemHelper.convert(srcItemReader
                            .getItemById(experimentList.getReference("experimentRef").getRefId()));
        Item pub = getPub(exptType);
        if (pub != null) {
            addReferencedItem(interaction, pub, "evidence", true, "", false);
        }
        Item participants = ItemHelper.convert(srcItemReader
                              .getItemById(intElType.getReference("participantList").getRefId()));
        Iterator iter = participants.getCollection("proteinParticipants").getRefIds().iterator();
        while (iter.hasNext()) {
            // protein has role attribute which is either prey or bait
            Item protein = ItemHelper.convert(srcItemReader.getItemById((String) iter.next()));
            interaction.addReference(new Reference(protein.getAttribute("role").getValue(),
                                     protein.getReference("proteinInteractorRef").getRefId()));
        }
        // object = prey, subject = bait
        interaction.addReference(new Reference("object",
                                               interaction.getReference("prey").getRefId()));
        interaction.addReference(new Reference("subject",
                                               interaction.getReference("bait").getRefId()));

        addReferencedItem(interaction, getDb(), "evidence", true, "", false);
        return interaction;
    }

    // find a publication for the experiment - if no experiment return null
    private Item getPub(Item exptType) throws ObjectStoreException {
        String experimentId = exptType.getIdentifier();

        if (noPubs.contains(experimentId)) {
            return null;
        }

        Item pub = (Item) pubs.get(experimentId);
        if (pub == null) {
            String pubmedId = null;
            Item bibRefType = ItemHelper.convert(srcItemReader
                  .getItemById(exptType.getReference("bibref").getRefId()));
            if (bibRefType != null) {
                Item xRefType = ItemHelper.convert(srcItemReader
                  .getItemById(bibRefType.getReference("xref").getRefId()));
                if (xRefType != null) {
                    Item dbReferenceType = ItemHelper.convert(srcItemReader
                      .getItemById(xRefType.getReference("primaryRef").getRefId()));
                    if (dbReferenceType != null) {
                        Attribute dbAttr = dbReferenceType.getAttribute("db");
                        if (dbAttr != null && dbAttr.getValue().equals("pubmed")) {
                            Attribute idAttr = dbReferenceType.getAttribute("id");
                            if (idAttr != null) {
                                pubmedId = idAttr.getValue();
                            }
                        }
                    }
                }
            }
            if (pubmedId != null) {
                pub = createItem(tgtNs + "Publication", "");
                pubs.put(experimentId, pub);
            } else {
                noPubs.add(experimentId);
                return null;
            }
        }
        return pub;
    }

    private Item getDb() {
        if (db == null) {
            db = createItem(tgtNs + "Database", "");
        }
        return db;
    }

    private Item getSwissProt() {
        if (swissProt == null) {
            swissProt = createItem(tgtNs + "Database", "");
            swissProt.addAttribute(new Attribute("title", "Swiss-Prot"));
        }
        return swissProt;
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
        PsiDataTranslator dt = new PsiDataTranslator(new ObjectStoreItemReader(osSrc), model,
                                                     namespace);
        model = null;
        dt.translate(tgtItemWriter);
        tgtItemWriter.close();
    }
}
