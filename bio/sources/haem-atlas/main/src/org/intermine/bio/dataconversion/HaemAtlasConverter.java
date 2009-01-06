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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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

/**
 * DataConverter to parse Haem-Atlas data into items
 * @author Dominik Grimm
 */
public class HaemAtlasConverter extends FileConverter
{

    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public HaemAtlasConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }
    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        HaemAtlasHandler handler = new HaemAtlasHandler(getItemWriter());

        try {
            handler.parseCVS(reader);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
    
    private class HaemAtlasHandler
    {
        private ItemWriter writer;
        private BufferedReader in;
        private List<String> fields;
        private List<String> values;
        private List<List<String>> valueList;
        private List<String> sampleFields;
        private List<String> sampleValues;
        private List<List<String>> sampleValueList;
        //Items
        private Item organism;
        private Item gene;
        private Item haemProbeSet;
        private Item haemAtlasResult;
        //Maps
        private Map<String, String> geneMap = new HashMap<String, String>();
        
        private String geneRefId;
        
        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         */
        public HaemAtlasHandler(ItemWriter writer) {
            this.writer = writer;
        }
        
        /**
         * file parser method
         * @param reader 
         */
        public void parseCVS(Reader reader) throws IOException, ObjectStoreException {
            
            in = new BufferedReader(reader);
            
            fields = new Vector<String>();
            values = new Vector<String>();
            valueList = new Vector<List<String>>();
            sampleFields = new Vector<String>();
            sampleValues = new Vector<String>();
            sampleValueList = new Vector<List<String>>();

            boolean fieldStatus = false;
            
            String delimiter = "\\t";
            
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
                    //store every row of values in list
                    valueList.add(values);
                }
            }
            
            //read Samplefile
            BufferedReader sampleFile = new BufferedReader(
            new FileReader(new File("/shared/data/heightmine/haem-atlas/current/SampleInfo.txt")));
 
            fieldStatus = false;           
          //read fields and values
            while ((readString = sampleFile.readLine()) != null) {
                if (!fieldStatus) {
                    String[] tmp = readString.split(delimiter);
                    for (int i = 0; i < tmp.length; i++) {
                        sampleFields.add(tmp[i]);
                    }
                    fieldStatus = true;
                } else {
                    String[] tmp = readString.split(delimiter);
                    sampleValues = new Vector<String>();
                    for (int i = 0; i < tmp.length; i++) {
                        sampleValues.add(tmp[i]);
                    }
                    //store every row of values in list
                    sampleValueList.add(sampleValues);
                }
            }
            
            //filling values in Items and store them
            organism = createItem("Organism");
            organism.setAttribute("taxonId", "9606");
            writer.store(ItemHelper.convert(organism));
            
            //write Items
            valuesInItems();
        }
        
        /**
         * creates all items 
         */
        private void createItems() {
            haemProbeSet = createItem("HaemAtlasProbeSet");
        }
        
        /**
         * created the gene item
         */
        private void createGene() {
            gene = createItem("Gene");
        }
        
        /**
         * stores the values into items 
         */
        private void valuesInItems() throws ObjectStoreException {
            for (int i = 0; i < valueList.size(); i++) {
                values = valueList.get(i);
                
                createItems();
                
                //Gene
                String[] tmp;
                
                if (!values.get(1).equals("NA")) {
                    tmp = values.get(1).split("\"");
                    if (geneMap.get(tmp[1]) == null) {
                        createGene();
                        gene.setAttribute("symbol", tmp[1]);
                        gene.addReference(new Reference("organism", organism.getIdentifier()));
                        writer.store(ItemHelper.convert(gene));
                        geneMap.put(tmp[1], gene.getIdentifier());
                        geneRefId = gene.getIdentifier();
                    } else {
                        geneRefId = geneMap.get(tmp[1]);
                    }
                }
                
              //ProbeSet
                tmp = values.get(0).split("\"");
                
                haemProbeSet.setAttribute("illuId", tmp[1]);
                if (geneRefId != null) {
                    haemProbeSet.addReference(new Reference("gene", geneRefId));
                    geneRefId = null;
                }
                writer.store(ItemHelper.convert(haemProbeSet));
                
                //HaemResults
                for (int k = 0; k < 8; k++) {
                    switch(k) {
                        case 0: { // CD 14
                            sampleValues = sampleValueList.get(14);
                            break;
                        }
                        case 1: { // CD 19
                            sampleValues = sampleValueList.get(21);
                            break;
                        }
                        case 2: { // CD 4
                            sampleValues = sampleValueList.get(0);
                            break;
                        }
                        case 3: { // CD 56
                            sampleValues = sampleValueList.get(28);
                            break;
                        }
                        case 4: { // CD 66b
                            sampleValues = sampleValueList.get(35);
                            break;
                        }
                        case 5: { // CD 8
                            sampleValues = sampleValueList.get(7);
                            break;
                        }
                        case 6: { // EB
                            sampleValues = sampleValueList.get(46);
                            break;
                        }
                        case 7: { // MK
                            sampleValues = sampleValueList.get(42);
                            break;
                        }
                    }
                    
                    //set attributes and store the item
                    haemAtlasResult = createItem("HaemAtlasResult");
                    tmp = sampleValues.get(1).split("\"");
                    haemAtlasResult.setAttribute("group", tmp[1]);
                    tmp = sampleValues.get(2).split("\"");
                    haemAtlasResult.setAttribute("sample", tmp[1]);
                    tmp = sampleValues.get(0).split("\"");
                    haemAtlasResult.setAttribute("sampleName", tmp[1]);
                    haemAtlasResult.setAttribute("averageIntensity", values.get(2 + k));
                    haemAtlasResult.setAttribute("detectionProbabilities", values.get(10 + k));
                    haemAtlasResult.addReference(new 
                            Reference("probeSet", haemProbeSet.getIdentifier()));
                    writer.store(ItemHelper.convert(haemAtlasResult));
                }
            }
        }
        
        protected Item createItem(String className) {
            return HaemAtlasConverter.this.createItem(className);
        }
    }
}
