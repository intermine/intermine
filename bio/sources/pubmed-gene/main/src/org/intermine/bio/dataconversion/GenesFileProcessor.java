package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.dataconversion.DataConverter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;


/**
 * Processor of file with information about genes. Format of file:
 * <tt>
 * tax_id GeneID Symbol LocusTag Synonyms dbXrefs chromosome map_location description 
 * type_of_gene Symbol_from_nomenclature_authority Full_name_from_nomenclature_authority 
 * Nomenclature_status Other_designations Modification_date (tab is used as a separator, 
 * pound sign - start of a comment)
 * </tt> 
 * @author Jakub Kulaviak
 **/
public class GenesFileProcessor 
{

    private BufferedReader infoReader;
    
    private int lineCounter = 0;

    private List<Item> genes = new ArrayList<Item>();
    
    private String lastLine = null;
    
    private DataConverter converter;

    private Integer checkOrganismId = null;
    
    /**
     * Constructor. 
     * @param fileReader file reader, this class is not responsible for closing fileReader
     * @param converter associated converter that is used for creating and saving items
     */
    public GenesFileProcessor(Reader fileReader, DataConverter converter) {
        // reference to parent converter is there because is used in createItem method
        // all converters must used one central converter for creating items because 
        // to be sure, that created items will have unique id
        this.converter = converter;
        initReader(fileReader);
    }
    
    private void initReader(Reader fileReader) {
         infoReader = new BufferedReader(fileReader);
    }

    /**
     * 
     * @param geneToPub map between gene and list of publication that mentions this gene
     * @param orgToProcessId organism to be processed id
     * @param orgToProcess organism to be processed
     * @throws IOException when 
     */
    public void processGenes(Map<Integer, List<Item>> geneToPub, 
            Integer orgToProcessId, Item orgToProcess) throws IOException {
        String line;
        // use taxonID to get correct type of data where available
        while ((line = getLine()) != null) {
            lineCounter++;
            line = line.trim();
            if (line.startsWith("#") || line.length() == 0) {
                continue;
            }
            String[] parts = line.split("\\t");
            if (parts.length < 6) {
                throw new GenesProcessorException("Invalid " + lineCounter 
                        + " line. There isn't enough items at the line.");
            }
            Integer organismId, ncbiGeneId;
            try {
                organismId = new Integer(parts[0].trim());
                ncbiGeneId = new Integer(parts[1].trim());
            } catch (NumberFormatException ex) {
                throw new GenesProcessorException("Invalid identifiers at line " + line);
            }
            checkFileIsSorted(organismId);
            String identifier = parts[3].trim();
            String dbId = parts[5].trim();
            if (orgToProcessId.intValue() == organismId.intValue()) {
                processGeneInfo(ncbiGeneId, identifier, organismId, dbId, 
                        geneToPub.get(ncbiGeneId), orgToProcess);
                geneToPub.remove(ncbiGeneId);
            } else if (organismId.intValue() > orgToProcessId.intValue()) {
                lastLine = line;
                storeGenes();
                if (geneToPub.size() != 0) {
                    throw new GenesProcessorException("There isn't information for "
                            + "following genes: " + formatGeneNames(geneToPub.keySet()));
                }
                return;
            } else {
                continue;
            }                        
        }
        storeGenes();
    }
    
    private void checkFileIsSorted(Integer organismId) {
        if (checkOrganismId != null) {
            if (organismId.intValue() < checkOrganismId.intValue()) {
                throw new GenesProcessorException("This file processor expects that "
                            + "file is sorted according to the organism id else the "
                            + "behaviour is undefined.");
            }
        }
        checkOrganismId = organismId;
    }

    private String formatGeneNames(Set<Integer> keySet) {
        StringBuilder sb = new StringBuilder();
        for (Integer id : keySet) {
            sb.append(id + ", ");
        }
        return sb.toString();
    }

    private String getLine() throws IOException {
        if (lastLine != null) {
            String tmp = lastLine;
            lastLine = null;
            return tmp;
        } else {
            return infoReader.readLine();
        }
    }

    private void storeGenes()  {
        try {
            store(genes);
        } catch (ObjectStoreException e) {
            throw new GenesProcessorException(e);
        }
        genes = new ArrayList<Item>();
    }

    private void store(List<Item> genes2) throws ObjectStoreException {
        converter.store(genes2);
    }

    private void processGeneInfo(Integer ncbiGeneId, String identifier, 
            Integer organismId, String dbId, List<Item> publications, Item organism) {
        // use taxonID to get correct type of data where available
        Item gene = createGene(ncbiGeneId);
        if (!dbId.equals("-")) {
            dbId = removeDatabasePrefix(dbId);
            setGene(gene, dbId, organism);
        }
        if (publications != null) {
            for (Item pub : publications) {
                gene.addToCollection("publications", pub);
            }
        }
        genes.add(gene);
    }

    private void setGene(Item gene, String dbId, Item organism) {
        gene.setAttribute("primaryIdentifier", dbId);
        gene.setReference("organism", organism);
    }
    
    private Item createGene(Integer ncbiGeneId) {
        Item gene = createItem("Gene");
        gene.setAttribute("ncbiGeneNumber", ncbiGeneId.toString());
        return gene;
    }

    private String removeDatabasePrefix(String dbId) {
        if (dbId.toUpperCase().startsWith("SGD:")) {
            dbId = dbId.substring(4);
        } else if (dbId.toUpperCase().startsWith("WORMBASE:")) {
            dbId = dbId.substring(9);
        } else if (dbId.toUpperCase().startsWith("FLYBASE:")) {
            dbId = dbId.substring(8);
        } else if (dbId.startsWith("VECTORBASE:")) {
            dbId = dbId.substring(11);
        }
        return dbId;
    }
    
    private Item createItem(String className) {
        return converter.createItem(className);
    }
}
