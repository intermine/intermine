package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Properties;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.FormattedTextParser;

import org.intermine.objectstore.ObjectStoreException;

/**
 * 
 * @author
 */
public class ZfinOrthologsConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "ZFIN orthologs";
    private static final String DATA_SOURCE_NAME = "ZFIN";

    private Properties props = new Properties();
    private static final String PROP_FILE = "zfin-orthologs_config.properties";
    private static final Logger LOG = Logger.getLogger(ZfinOrthologsConverter.class);
    private Map<String, String> organisms = new HashMap();
    private Map<String, String[]> config = new HashMap();
    private Set<String> synonyms = new HashSet();
    private Map<String, String> genes = new HashMap();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ZfinOrthologsConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
	readConfig();
    }

    private void readConfig() {
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Problem loading properties '" + PROP_FILE + "'", e);
        }

        for (Map.Entry<Object, Object> entry: props.entrySet()) {

            String key = (String) entry.getKey();
            String value = ((String) entry.getValue()).trim();

            String[] attributes = key.split("\\.");
            if (attributes.length == 0) {
                throw new RuntimeException("Problem loading properties '" + PROP_FILE + "' on line "
                                           + key);
            }
            String organismName = attributes[0];
            String attribute = attributes[1];

            if (config.get(organismName) == null) {                
                config.put(organismName, new String[2]);
            }

	    if (attribute.equals("taxonId")) {
                config.get(organismName)[0] = value;
            } else if (attribute.equals("identifierField")) {
                config.get(organismName)[1] = value;
            } else {
                String msg = "Problem processing properties '" + PROP_FILE + "' on line " + key
                    + ".  This line has not been processed.";
                LOG.error(msg);
            }            
        }
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String bits[] = lineIter.next();
            if (bits.length < 4) {
                continue;
            }	
	    String organismName = parseFileName();
	    if (config.get(organismName) == null) {
		LOG.error(organismName + " not found in configuration file.  Skipping file");
		return;
	    }
	    String taxonId = config.get(organismName)[0];
	    if (taxonId == null) {
		LOG.error("taxonId not found for " + organismName + ".  Skipping file");
		return;
	    } 
	    String organismRefId = getOrganism(taxonId);
	    String identifierField = config.get(organismName)[1];

            String gene1identifier = bits[0];	    
            String gene2identifier = bits[4];

	    if (StringUtils.isEmpty(gene1identifier) || StringUtils.isEmpty(gene2identifier)) {
		LOG.error("gene has no name, line not processed.  "  + bits[0]);
		continue;
	    }
                    
	    String gene1RefId = getGene(gene1identifier, identifierField, organismRefId);
	    String gene2RefId = getGene(gene2identifier, identifierField, organismRefId);
 
	    processHomologues(gene1RefId, gene2RefId);
	    processHomologues(gene2RefId, gene1RefId);
	}
    }

private String parseFileName() {
    
    String fileName = getCurrentFile().getName();
    String[] bits = fileName.split("_");
    return bits[0];
}

private void processHomologues(String gene1, String gene2)
    throws ObjectStoreException {

    Item homologue = createItem("Homologue");
    homologue.setAttribute("evidenceCode", "AA - Amino acid sequence comparison");
    homologue.setReference("gene", gene1);
    homologue.setReference("homologue", gene2);
    homologue.setAttribute("type", "ortholog");
    //gene1.addToCollection("homologues", homologue);
    try {
	store(homologue);
    } catch (ObjectStoreException e) {
	throw new ObjectStoreException(e);
    }
}

private String getGene(String identifier, String identifierField, String organismRefId)
    throws ObjectStoreException {    
    String refId = genes.get(identifier);
    if (refId == null) {
	Item gene = createItem("Gene");
	refId = gene.getIdentifier();
	gene.setAttribute(identifierField, identifier);
	gene.setReference("organism", organismRefId);
	genes.put(identifier, refId);
	setSynonym(refId, "identifier", identifier);
	try {
	    store(gene);
	} catch (ObjectStoreException e) {
	    throw new ObjectStoreException(e);
	}
    }
    return refId;
}

private void setSynonym(String subjectId, String type, String value)
    throws ObjectStoreException {
    String key = subjectId + type + value;
    if (StringUtils.isEmpty(value)) {
	return;
    }
    if (!synonyms.contains(key)) {
	Item syn = createItem("Synonym");
	syn.setReference("subject", subjectId);
	syn.setAttribute("type", type);
	syn.setAttribute("value", value);
	synonyms.add(key);
	try {
	    store(syn);
	} catch (ObjectStoreException e) {
	    throw new ObjectStoreException(e);
	}
    }
}

private String getOrganism(String taxonId)
    throws ObjectStoreException {
    String refId = organisms.get(taxonId);
    if (refId == null) {
	Item item = createItem("Organism");
	item.setAttribute("taxonId", taxonId);
	refId = item.getIdentifier();
	organisms.put(taxonId, refId);
	try {
	    store(item);
	} catch (ObjectStoreException e) {
	    throw new ObjectStoreException(e);
	}
    }
    return refId;
}
}
