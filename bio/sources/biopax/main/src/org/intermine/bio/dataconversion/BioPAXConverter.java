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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.Traverser;
import org.biopax.paxtools.controller.Visitor;
import org.biopax.paxtools.io.jena.JenaIOHandler;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level2.pathway;
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
/**
 *
 * @author
 */
public class BioPAXConverter extends FileConverter implements Visitor
{
    private static final Logger LOG = Logger.getLogger(BioPAXConverter.class);
    private static final String PROP_FILE = "biopax_config.properties";
    private static final String DEFAULT_DB_NAME = "UniProt";
    protected IdResolverFactory resolverFactory;
    private Map<String, Item> genes = new HashMap();
    private Traverser traverser;
    private Set<BioPAXElement> visited = new HashSet();
    private int depth = 0;
    private Item organism, dataset;
    private String pathwayRefId = null;
    private List<MultiKey> synonyms = new ArrayList();
    private Set<String> taxonIds = new HashSet();
    private Map<String, String[]> configs = new HashMap();
    private OrganismRepository or;
    private String dbName, identifierField;
    private String dataSourceRefId = null, dataSourceName = null;
    private String curated = "false";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param intermineModel the Model
     * @throws ObjectStoreException if something goes horribly wrong
     */
    public BioPAXConverter(ItemWriter writer, org.intermine.metadata.Model intermineModel)
    throws ObjectStoreException {
        super(writer, intermineModel);
        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory("gene");
        traverser = new Traverser(new SimpleEditorMap(BioPAXLevel.L2), this);
        readConfig();
        or = OrganismRepository.getOrganismRepository();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void process(@SuppressWarnings("unused") Reader reader) throws Exception {
        String taxonId = getTaxonId();
        if (taxonId == null) {
            // this file isn't from an organism specified in the project file
            return;
        }
        setDataset();
        setOrganism(taxonId);
        setConfig(taxonId);

        // navigate through the owl file
        JenaIOHandler jenaIOHandler = new JenaIOHandler(null, BioPAXLevel.L2);
        Model model = jenaIOHandler.convertFromOWL(new FileInputStream(getCurrentFile()));
        Set<pathway> pathwaySet = model.getObjects(pathway.class);
        for (pathway pathwayObj : pathwaySet) {
            try {
                pathwayRefId = getPathway(pathwayObj);
            } catch (ObjectStoreException e) {
                pathwayRefId = null;
                continue;
            }
            visited = new HashSet();
            traverser.traverse(pathwayObj, model);
        }
    }

    /**
     * Sets the list of taxonIds that should be imported if using split input files.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setBiopaxOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtils.split(taxonIds, " ")));
        LOG.info("Setting list of organisms to " + this.taxonIds);
    }

    /**
     * @param curated true or false
     */
    public void setBiopaxCurated(String curated) {
        this.curated = curated;
    }

    /**
     * @param name name of datasource
     * @throws ObjectStoreException if storing datasource fails
     */
    public void setBiopaxDatasourcename(String name)
    throws ObjectStoreException {
        this.dataSourceName = name;
        Item datasource = createItem("DataSource");
        datasource.setAttribute("name", name);
        try {
            store(datasource);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException(e);
        }
        dataSourceRefId = datasource.getIdentifier();
    }

    /**
     * @param title name of dataset
     * @throws ObjectStoreException if storing datasource fails
     */
    public void setBiopaxDatasetname(String title)
    throws ObjectStoreException {
        dataset = createItem("DataSet");
        dataset.setAttribute("title", title);

    }

    private void readConfig() {
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Problem loading properties '" + PROP_FILE + "'", e);
        }
        for (Map.Entry<Object, Object> entry: props.entrySet()) {
            String key = (String) entry.getKey();
            String value = ((String) entry.getValue()).trim();

            String[] attributes = key.split("\\.");
            if (attributes.length != 2) {
                throw new RuntimeException("Problem loading properties '" + PROP_FILE + "' on line "
                                           + key);
            }
            String taxonId = attributes[0];
            String identifier = attributes[1];

            String[] bits = new String[2];
            bits[0] = value;
            bits[1] = identifier;

            if (configs.get(taxonId) == null) {
                configs.put(taxonId, bits);
            } else {
                throw new RuntimeException("Problem loading properties '" + PROP_FILE + "': "
                                           + " duplicate entries for organism " + taxonId);
            }
        }
    }

    // set which identifier to set for genes
    private void setConfig(String taxonId) {
        String[] bits = configs.get(taxonId);
        if (bits != null) {
            dbName = bits[0];
            identifierField = bits[1];
        } else {
            dbName = DEFAULT_DB_NAME;
            identifierField = "primaryIdentifier";
        }
    }

    /**
     * Adds the BioPAX element into the model and traverses the element for its dependent elements.
     *
     * @param bpe    the BioPAX element to be added into the model
     * @param model  model into which the element will be added
     * @param editor editor that is going to be used for traversing functionallity
     * @see org.biopax.paxtools.controller.Traverser
     */
    public void visit(BioPAXElement bpe, Model model, PropertyEditor editor) {
        if (bpe != null) {
            if (bpe instanceof org.biopax.paxtools.model.level2.entity) {
                org.biopax.paxtools.model.level2.entity entity
                = (org.biopax.paxtools.model.level2.entity) bpe;
                String className = entity.getModelInterface().getSimpleName();
                if (className.equalsIgnoreCase("protein") && StringUtils.isNotEmpty(pathwayRefId)) {
                    processProteinEntry(entity);
                }
            }
            if (!visited.contains(bpe)) {
                visited.add(bpe);
                depth++;
                traverser.traverse(bpe, model);
                depth--;
            }
        }
    }

    private void processProteinEntry(org.biopax.paxtools.model.level2.entity entity) {
        String identifier = entity.getRDFId();

        // there is only one gene
        if (identifier.contains(dbName) || identifier.contains(DEFAULT_DB_NAME)) {
            processGene(identifier, pathwayRefId);

        // there are multiple genes
        } else {
            Set<org.biopax.paxtools.model.level2.xref> xrefs = entity.getXREF();
            for (org.biopax.paxtools.model.level2.xref xref : xrefs) {
                identifier = xref.getRDFId();
                if (identifier.contains(dbName) || identifier.contains(DEFAULT_DB_NAME)) {
                    processGene(identifier, pathwayRefId);
                }
            }
        }
    }

    private void processGene(String xref, String pathway) {

        // db source for this identifier, eg. UniProt, FlyBase
        String identifierSource = (xref.contains(dbName) ? dbName : DEFAULT_DB_NAME);

        // remove prefix, eg. UniProt or ENSEMBL
        String identifier = StringUtils.substringAfter(xref, identifierSource + "_");

        // which gene field to set
        String fieldName = identifierField;

        if (identifier.contains("_")) {
            if (identifierSource.equals(DEFAULT_DB_NAME)) {
                // eg. P38132-CDC47
                identifier = identifier.split("_")[1];
                fieldName = "symbol";
            } else {
                // eg. CG1234-PA
                identifier = identifier.split("_")[0];
            }
        } else if (identifierSource.equals(DEFAULT_DB_NAME)) {
            // this is a uniprot entry without a gene symbol.  there is nothing to be done.
            LOG.warn("Gene not stored:" + xref);
            return;
        }

        if (organism.getAttribute("taxonId").getValue().equals("7227")) {
            identifier = resolveGene("7227", identifier);
            fieldName = "primaryIdentifier";
        }

        if (identifier == null || identifier.length() < 2) {
            LOG.warn("Gene not stored:" + xref);
            return;
        }

        Item item = getGene(fieldName, identifier);
        item.addToCollection("pathways", pathway);
        return;
    }


    private String getPathway(org.biopax.paxtools.model.level2.pathway pathway)
    throws ObjectStoreException {
        Item item = createItem("Pathway");
        item.setAttribute("name", pathway.getNAME());
        item.setAttribute("curated", curated);
        item.addToCollection("dataSets", dataset);
        for (org.biopax.paxtools.model.level2.xref xref : pathway.getXREF()) {
            String xrefId = xref.getRDFId();
            // xrefIds look like:  Reactome12345
            if (xrefId.contains(dataSourceName)) {
                String identifier = StringUtils.substringAfter(xrefId, dataSourceName);
                item.setAttribute("identifier", identifier);
                try {
                    store(item);
                } catch (ObjectStoreException e) {
                    throw new ObjectStoreException(e);
                }
                return item.getIdentifier();
            }
        }
        return null;
    }

    private Item getGene(String fieldName, String identifier) {
       Item item = genes.get(identifier);
        if (item == null) {
            item = createItem("Gene");
            item.setAttribute(fieldName, identifier);
            item.setReference("organism", organism);
            item.addToCollection("dataSets", dataset);
            try {
                setSynonym(item.getIdentifier(), identifier);
            } catch (ObjectStoreException e) {
                // nothing
            }
            genes.put(identifier, item);
        }
        return item;
    }

    private void setSynonym(String subjectId, String value)
    throws ObjectStoreException {
        MultiKey key = new MultiKey(subjectId, value);
        if (!synonyms.contains(key)) {
            Item syn = createItem("Synonym");
            syn.setReference("subject", subjectId);
            syn.setAttribute("value", value);
            syn.setAttribute("type", "identifier");
            syn.addToCollection("dataSets", dataset);
            synonyms.add(key);
            try {
                store(syn);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
        }
    }

    private void setOrganism(String taxonId)
    throws ObjectStoreException {
        organism = createItem("Organism");
        organism.setAttribute("taxonId", taxonId);
        try {
            store(organism);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException(e);
        }
    }

    private void setDataset()
    throws ObjectStoreException {
        if (dataset.getReference("dataSource") == null) {
            dataset.setReference("dataSource", dataSourceRefId);
            try {
                store(dataset);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
        }
    }


    /**
     * Use the file name currently being processed to divine the name of the organism.  Return null
     * if this taxonId is not in our list of taxonIds to be processed.
     * @return the taxonId of current organism
     */
    private String getTaxonId() {

        File file = getCurrentFile();
        String filename = file.getName();
        String[] bits = filename.split(" ");

        // bad filename eg `Human immunodeficiency virus 1.owl`,
        // expecting "Drosophila melanogaster.owl"
        if (bits.length != 2) {
            String msg = "Bad filename:  '" + filename + "'.  Expecting filename in the format "
                + "'Drosophila melanogaster.owl'";
            LOG.error(msg);
            return null;
        }

        String genus = bits[0];
        String species = bits[1].split("\\.")[0];
        String organismName = genus + " " + species;
        OrganismData od = or.getOrganismDataByGenusSpecies(genus, species);
        if (od == null) {
            LOG.error("No data for " + organismName + ".  Please add to repository.");
            return null;
        }

        int taxonId = od.getTaxonId();
        String taxonIdString = String.valueOf(taxonId);

        // only process the taxonids set in the project XML file - if any
        if (!taxonIds.isEmpty() && !taxonIds.contains(taxonIdString)) {
            return null;
        }

        return taxonIdString;
    }

    /**
     * resolve dmel genes
     * @param taxonId id of organism for this gene
     * @param ih interactor holder
     * @throws ObjectStoreException
     */
    private String resolveGene(String taxonId, String identifier) {
        IdResolver resolver = resolverFactory.getIdResolver(false);
        String id = identifier;
        if (taxonId.equals("7227") && resolver != null) {
            int resCount = resolver.countResolutions(taxonId, identifier);
            if (resCount != 1) {
                return null;
            }
            id = resolver.resolveId(taxonId, identifier).iterator().next();
        }
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close()
    throws ObjectStoreException {
        for (Item item : genes.values()) {
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
        }
    }
}
