package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
import java.util.Set;

import org.apache.log4j.Logger;
import org.biopax.paxtools.io.jena.JenaIOHandler;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * 
 * @author
 */
public class BioPAXConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(BioPAXConverter.class);
    private static final String DATASET_TITLE = "BioPAX";
    private static final String DATA_SOURCE_NAME = "BioPAX data set";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public BioPAXConverter(ItemWriter writer, Model model) 
    throws ObjectStoreException {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        Item o = createItem("Organism");
        o.setAttribute("taxonId", "4932");
        try {
            store(o);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {

        // TODO the reader is never used
        
        JenaIOHandler jenaIOHandler = new JenaIOHandler();
        File file = getCurrentFile();
        org.biopax.paxtools.model.Model jenaModel 
            = jenaIOHandler.convertFromOWL(new FileInputStream(file));
//        Set<org.biopax.paxtools.model.level2.pathway> pathways = jenaModel.getObjects(org.biopax.paxtools.model.level2.pathway.class);
        
        Set objects = jenaModel.getObjects();
        
        for (Object object : objects) {
            if (object.getClass().getCanonicalName().equals("org.biopax.paxtools.impl.level2.pathwayImpl")) {
                storePathways((org.biopax.paxtools.model.level2.pathway) object);
            } else if (object.getClass().getCanonicalName().contains("biochemicalReaction")) {
                org.biopax.paxtools.model.level2.biochemicalReaction br = (org.biopax.paxtools.model.level2.biochemicalReaction) object;
                Set<org.biopax.paxtools.model.level2.InteractionParticipant> participants = br.getPARTICIPANTS();
                
                for (org.biopax.paxtools.model.level2.InteractionParticipant participant : participants) {
                    
                }
                
            }
//            physicalEntityParticipant
        }
    }
    
    private void storePathways(org.biopax.paxtools.model.level2.pathway pathway) 
    throws ObjectStoreException {
        String shortName = pathway.getSHORT_NAME();
        String name = pathway.getNAME();
        Set<String> comments = pathway.getCOMMENT();
        Set<org.biopax.paxtools.model.level2.dataSource> datasources = pathway.getDATA_SOURCE();
        org.biopax.paxtools.model.level2.bioSource organism = pathway.getORGANISM();
        Set<org.biopax.paxtools.model.level2.evidence> evidences = pathway.getEVIDENCE();
        Set<org.biopax.paxtools.model.level2.pathwayComponent> components = pathway.getPATHWAY_COMPONENTS();
        String rfId = pathway.getRDFId();
        Set<String> synonyms = pathway.getSYNONYMS();
        Set<org.biopax.paxtools.model.level2.xref> xrefs = pathway.getXREF();
        String id = "";
        for (org.biopax.paxtools.model.level2.xref xref : xrefs) {
            id = xref.getID();
        }
            
        Item item = createItem("Pathway");
        item.setAttribute("identifier", id);
        item.setAttribute("name", name);
        try {
            store(item);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException(e);
        }        
    }
    
  }
