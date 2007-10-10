package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.InterMineException;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.XmlUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Reference;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Convert Drosdel data in fulldata Item format to fulldata Item format conforming to the FlyMine
 * model.
 *
 * @author Kim Rutherford
 */
public class DrosdelDataTranslator extends DataTranslator
{
    private Item drosdelDb;
    private Item drosdelDataSource;
    private Reference drosdelDataSourceRef;
    private Item organism;
    private Reference organismRef;
    private Map chromosomes = new HashMap();

    protected static final Logger LOG = Logger.getLogger(DrosdelDataTranslator.class);

    /**
     * {@inheritDoc}
     */
    public DrosdelDataTranslator(ItemReader srcItemReader, Properties mapping, Model srcModel,
                                 Model tgtModel) {
        super(srcItemReader, mapping, srcModel, tgtModel);

        organism = createItem("Organism");
        organism.addAttribute(new Attribute("taxonId", "7227"));
        organismRef = new Reference("organism", organism.getIdentifier());

        drosdelDb = createItem("DataSet");
        drosdelDb.addAttribute(new Attribute("title", "DrosDel data set"));

        drosdelDataSource = createItem("DataSource");
        drosdelDataSource.addAttribute(new Attribute("name", "DrosDel"));
        drosdelDataSourceRef = new Reference("source", drosdelDataSource.getIdentifier());
    }

    /**
     * {@inheritDoc}
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {
        tgtItemWriter.store(ItemHelper.convert(organism));
        tgtItemWriter.store(ItemHelper.convert(drosdelDb));
        tgtItemWriter.store(ItemHelper.convert(drosdelDataSource));

        super.translate(tgtItemWriter);

        for (Iterator i = chromosomes.values().iterator(); i.hasNext();) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }
    }

    /**
     * {@inheritDoc}
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {

        Collection result = new HashSet();
        String className = XmlUtil.getFragmentFromURI(srcItem.getClassName());
        Collection translated = super.translateItem(srcItem);
        if (translated != null) {
            for (Iterator i = translated.iterator(); i.hasNext();) {
                Item tgtItem = (Item) i.next();
                if ("deletion".equals(className)) {
                    tgtItem.addReference(organismRef);
                    addReferencedItem(tgtItem, drosdelDb, "evidence", true, "", false);
                    String available = srcItem.getAttribute("available").getValue();
                    if (available.equals("0") || available.equals("false")) {
                        tgtItem.setAttribute("available", "false");
                    } else {
                        if (available.equals("1") || available.equals("true")) {
                            tgtItem.setAttribute("available", "true");
                        } else {
                            throw new RuntimeException("unknown value for deletion.available: "
                                                       + available);
                        }
                    }
                    Item location = createLocation(srcItem, tgtItem, "deletion");
                    result.add(location);



                    Item synonym = createSynonym(tgtItem.getIdentifier(), "name",
                                                 tgtItem.getAttribute("identifier").getValue(),
                                                 drosdelDataSourceRef);
                    addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                    result.add(synonym);




               } else if ("element".equals(className)) {
                    tgtItem.addReference(organismRef);
                    tgtItem.setAttribute("symbol", srcItem.getAttribute("name").getValue());
                    addReferencedItem(tgtItem, drosdelDb, "evidence", true, "", false);
                    Item location = createLocation(srcItem, tgtItem, "element");
                    result.add(location);


                    Item synonym = createSynonym(tgtItem.getIdentifier(), "identifier",
                                                 tgtItem.getAttribute("identifier").getValue(),
                                                 drosdelDataSourceRef);
                    addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                    result.add(synonym);


                } else {
                    throw new RuntimeException("cannot translate item of class: " + className);
                }

                result.add(tgtItem);
            }
        }
        return result;
    }

    /**
     * Translate a "located" Item into an Item and a location
     * @param srcItem the source Item
     * @param tgtItem the target Item (after translation)
     * @param idPrefix the id prefix for this class
     * @return the location
     */
    protected Item createLocation(Item srcItem, Item tgtItem, String idPrefix) {
        String namespace = XmlUtil.getNamespaceFromURI(tgtItem.getClassName());

        String chromosomeName = srcItem.getAttribute("chromosomeName").getValue();

        if (chromosomeName == null) {
            throw new RuntimeException("can't find chromosomeName attribute on source item");
        }

        Item chromosome = (Item) chromosomes.get(chromosomeName);

        if (chromosome == null) {
            chromosome = createItem(namespace + "Chromosome", "");
            chromosome.setAttribute("identifier", chromosomeName);
            chromosome.addReference(organismRef);
            chromosomes.put(chromosomeName, chromosome);
        }

        Item location = createItem(namespace + "Location", "");

        moveField(srcItem, location, "start", "start");
        moveField(srcItem, location, "end", "end");
        location.setAttribute("startIsPartial", "false");
        location.setAttribute("endIsPartial", "false");
        location.setAttribute("strand", "0");

        location.setReference("subject", tgtItem.getIdentifier());
        location.setReference("object", chromosome.getIdentifier());

        return location;
    }

    private Item createSynonym(String subjectId, String type, String value, Reference ref) {
        Item synonym = createItem("Synonym");
        synonym.addReference(new Reference("subject", subjectId));
        synonym.addAttribute(new Attribute("type", type));
        synonym.addAttribute(new Attribute("value", value));
        synonym.addReference(ref);
        return synonym;
    }
}
