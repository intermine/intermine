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

import org.biopax.paxtools.io.jena.JenaIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;


/**
 * 
 * @author
 */
public class BioPAXConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "BioPAX";
    private static final String DATA_SOURCE_NAME = "BioPAX data set";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public BioPAXConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        JenaIOHandler jenaIOHandler = new JenaIOHandler();
        File file = getCurrentFile();
        org.biopax.paxtools.model.Model jenaModel 
            = jenaIOHandler.convertFromOWL(new FileInputStream(file));
        Set<BioPAXElement> elements = jenaModel.getObjects();
        for (BioPAXElement element : elements) {
            element.getRDFId();
        }

//        model.getObjects();
//        or you can use the filtering feature in order to get only 
//           model.getObjects(interaction.class);
//        When you have an interaction, getting its participants i
//           Set<InteractionParticipant> participant

        
        
//        A Model object contains multiple BioPAX elements. Each element within a model
//        to have a unique id. A model can have multiple namespaces and these namespaces
//        be obtained as a map in which key is the prefix and value is the namespace:
//           Map<String, String> nspMap = model.getNameSpacePrefixMap();

        
        
    }
    
  }


