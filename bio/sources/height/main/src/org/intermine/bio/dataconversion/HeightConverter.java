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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

/**
 * DataConverter for height data
 * @author Dominik Grimm
 *
 */
public class HeightConverter extends FileConverter
{

    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    
    /**
     * Constructor
     * @param writer itemWriter
     * @param model model
     */
    public HeightConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }
    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        HeightHandler handler = new HeightHandler(getItemWriter());

        try {
            handler.parseCVS(reader);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
    
    private class HeightHandler
    {
        //private data fields
        private ItemWriter writer;
        private BufferedReader in;
        private List<String> fields;
        private List<String> values;
        private List<List<String>> valueList;
        //fields in the CSV file
        private final String snp = "snp";
        private final String chr = "primaryIdentifier";
        private final String startPosition = "start";
        private final String endPosition = "end";
        private final String z = "z";
        private final String p = "p";
        private final String a1 = "allele1";
        private final String a2 = "allele2";
        private final String nStudies = "nStudies";
        private final String nSubjects = "nSubjects";
        private final String metaBeta = "metaBeta";
        private final String metaSe = "metaSe";
        private final String geneName = "geneName";
        private final String description = "description";
        private final String details = "details";
        private final String mousePhenotype = "mousePhenotype";
        private final String humanPhenotype = "humanPhenotype";
        private final String location = "location";
        private final String geneSymbol = "symbol";
        //reference string
        //private String geneRef = null;
        
        //Items
        private Item geneItem = null;
        private Item lociResultsItem = null;
        private Item snpItem = null;
        private Item locationItem = null;
        private Item chromosomeItem = null;
        private Item organism = null;
        
        //Map
        private Map<String, String> geneMap = new HashMap<String, String>();
        
        //ReferenceLists
        ReferenceList genesRefLst = new ReferenceList("genes", new ArrayList<String>());
        
        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         */
        public HeightHandler(ItemWriter writer) {
            this.writer = writer;
        }
        
        public void parseCVS(Reader reader) throws IOException, ObjectStoreException {
            
            in = new BufferedReader(reader);
            
            fields = new Vector<String>();
            values = new Vector<String>();
            valueList = new Vector<List<String>>();
            
            boolean fieldStatus = false;
            
            String delimiter = "\\|";
            
            String readString;
            
            //read fields and values
            while ((readString = in.readLine()) != null) {
                if (!fieldStatus) {
                    String[] tmp = readString.split(delimiter);
                    for (int i = 0; i < tmp.length; i++) {
                        fields.add(tmp[i]);
                    }
                    fieldStatus = true;
                } else {
                    String[] tmp = readString.split(delimiter);
                    values = new Vector<String>();
                    for (int i = 0; i < tmp.length; i++) {
                        values.add(tmp[i]);
                    }
                    if (values.size() <= 17) {
                        for (int i = values.size(); i <= 17; i++) {
                            values.add("");
                        }
                    }
                    //store every row of values in list
                    valueList.add(values);
                }
            }
            
            //filling values in Items and store them
            organism = createItem("Organism");
            organism.setAttribute("taxonId", "9606");
            writer.store(ItemHelper.convert(organism));
            valuesInItems();
        }
        
        private void createItems() {
            geneItem = createItem("Gene");
            lociResultsItem = createItem("LociResult");
            snpItem = createItem("SNP");
            locationItem = createItem("Location");
            chromosomeItem = createItem("Chromosome");
        }
        
        private void valuesInItems() throws ObjectStoreException {
            
            for (int i = 0; i < valueList.size(); i++) {
                values = valueList.get(i);
                //Create new items
                createItems();
                //create collection
                if (genesRefLst.getRefIds().isEmpty()) {
                    lociResultsItem.addCollection(genesRefLst);
                }
                //snp
                snpItem.setAttribute(snp, values.get(0));
                snpItem.setAttribute(a1, values.get(5));
                snpItem.setAttribute(a2, values.get(6));
                //Chromosome
                chromosomeItem.setAttribute(chr, values.get(1));
                //Location
                locationItem.setAttribute(startPosition, values.get(2));
                locationItem.setAttribute(endPosition, values.get(2));
                //LociResults
                lociResultsItem.setAttribute(z, values.get(3));
                lociResultsItem.setAttribute(p, values.get(4));
                lociResultsItem.setAttribute(nStudies, values.get(7));
                lociResultsItem.setAttribute(nSubjects, values.get(8));
                lociResultsItem.setAttribute(metaBeta, values.get(9));
                lociResultsItem.setAttribute(metaSe, values.get(10));
                if (!(values.get(11) == null || values.get(11).equals(""))) {
                    lociResultsItem.setAttribute(geneName, values.get(11));
                }
                if (!(values.get(12) == null || values.get(12).equals(""))) {
                    lociResultsItem.setAttribute(description, values.get(12));
                }
                if (!(values.get(13) == null || values.get(13).equals(""))) {
                    lociResultsItem.setAttribute(details, values.get(13));
                }
                if (!(values.get(14) == null || values.get(14).equals(""))) {
                    lociResultsItem.setAttribute(mousePhenotype, values.get(14));
                }
                if (!(values.get(15) == null || values.get(15).equals(""))) {
                    lociResultsItem.setAttribute(humanPhenotype, values.get(15));
                }
                if (!(values.get(16) == null || values.get(16).equals(""))) {
                    lociResultsItem.setAttribute(location, values.get(16));
                }
                //Gene
                String geneString = values.get(17);
                if (!(geneString == null || geneString.equals(""))) {
                    String[] parsedGenes = geneString.split(";");
                    for (int k = 0; k < parsedGenes.length; k++) {
                        String refId = null;
                        geneItem = createItem("Gene");
                        if (geneMap.get(parsedGenes[k]) == null) {
                            geneItem.setAttribute(geneSymbol, parsedGenes[k]);
                            refId = geneItem.getIdentifier();
                            genesRefLst.addRefId(refId);
                            geneMap.put(parsedGenes[k], refId);
                        } else {
                            refId = geneMap.get(parsedGenes[k]);
                            genesRefLst.addRefId(geneMap.get(parsedGenes[k]));
                        }
                        geneItem.addReference(new 
                                Reference("lociResult", lociResultsItem.getIdentifier()));
                        geneItem.addReference(new Reference("organism", organism.getIdentifier()));
                        writer.store(ItemHelper.convert(geneItem));
                    }
                }
                //add references
                lociResultsItem.addReference(new Reference("snp", snpItem.getIdentifier()));
                snpItem.addReference(new Reference("location", locationItem.getIdentifier()));
                locationItem.addReference(new 
                        Reference("chromosome", chromosomeItem.getIdentifier()));
                //write Items
                writer.store(ItemHelper.convert(snpItem));
                writer.store(ItemHelper.convert(chromosomeItem));
                writer.store(ItemHelper.convert(locationItem));
                writer.store(ItemHelper.convert(lociResultsItem));
            }
        }
        
        protected Item createItem(String className) {
            return HeightConverter.this.createItem(className);
        }
    }
}
