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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.Traverser;
import org.biopax.paxtools.controller.Visitor;
import org.biopax.paxtools.io.jena.JenaIOHandler;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.model.level2.pathwayComponent;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
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
    List<String> canons = new ArrayList();
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
        
        
        
        
//        for (Object object : objects) {
//            if (object.getClass().getCanonicalName().equals("org.biopax.paxtools.impl.level2.pathwayImpl")) {
//                storePathways((org.biopax.paxtools.model.level2.pathway) object);
//            }
//        }


        
//        ProteinNameLister lister = new ProteinNameLister();
//        lister.listProteinUnificationXrefsPerPathway(jenaModel);
        
        // This is a visitor for elements in a pathway - direct and indirect
        Visitor visitor = new Visitor() {
            //This is the only method to implement
            //You define what to do when the visitor object visited
            // bpe in the model
            public void visit(BioPAXElement bpe, org.biopax.paxtools.model.Model model, PropertyEditor editor) {
                
//                String name = bpe.getClass().getCanonicalName().toString();
//                if (!canons.contains(name)) {
//                    canons.add(name);
//                    System.out.println(name);
//                }
                
                
                
                if (bpe.getClass().getCanonicalName().contains("unificationXrefImpl")) {
//                if (bpe instanceof org.biopax.paxtools.model.level2.physicalEntity) {
                    // Do whatever you want with the pe here
//                    org.biopax.paxtools.model.level2.physicalEntity pe = (org.biopax.paxtools.model.level2.physicalEntity) bpe;
                    org.biopax.paxtools.model.level2.unificationXref xref = (org.biopax.paxtools.model.level2.unificationXref) bpe;
                    System.out.println("pe.getNAME() = " + xref.getDB());

//                    ClassFilterSet<org.biopax.paxtools.model.level2.unificationXref> unis=
//                    new ClassFilterSet<org.biopax.paxtools.model.level2.unificationXref>(pe.getXREF(),
//                                    org.biopax.paxtools.model.level2.unificationXref.class);
//                    for (org.biopax.paxtools.model.level2.unificationXref uni : unis)
//                    {
                        System.out.println("uni = " + xref.getID());
//                    }
                }
            }
        };
       
        //Now we have defined what we want to do with the traversed objects
        // Let's actually go ahead and traverse the model with our new visitor,
        //for every pathway in the model.

        Traverser traverser = new Traverser(new SimpleEditorMap(BioPAXLevel.L2), visitor); 
        
        Set<org.biopax.paxtools.model.level2.pathway> pathways = jenaModel.getObjects(org.biopax.paxtools.model.level2.pathway.class);
        for (org.biopax.paxtools.model.level2.pathway pathway : pathways) {
            traverser.traverse(pathway, jenaModel);
            Set<pathwayComponent> components = pathway.getPATHWAY_COMPONENTS();
           
            for (pathwayComponent component : components) {
                System.out.println(component.getClass().getCanonicalName().toString());
            }
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
