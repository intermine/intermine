package org.intermine.bio.dataconversion;

import java.io.BufferedReader;
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

public class OMIMConverter extends FileConverter{

    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    
    public OMIMConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }
    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        OMIMHandler handler = new OMIMHandler(getItemWriter());

        try {
            handler.parseCVS(reader);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
    
    private class OMIMHandler
    {
        //private data fields
        private ItemWriter writer;
        private BufferedReader in;
        private List<String> values;
        private List<List<String>> valueList;
        private Map<String, String> geneMap = new HashMap<String, String>();
        private Item gene = null;
        private Item omimDisease = null;
        private Item organism = null;
        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         */
        public OMIMHandler(ItemWriter writer) {
            this.writer = writer;
        }
        
        public void parseCVS(Reader reader) throws IOException, ObjectStoreException {
            
            in = new BufferedReader(reader);
            
            values = new Vector<String>();
            valueList = new Vector<List<String>>();
            
            String delimiter = "\\|";
            
            String readString;
            
            //read fields and values
            while((readString = in.readLine()) != null) {
                String[] tmp = readString.split(delimiter);
                String disorders = null;
                values = new Vector<String>();
                for(int i = 0; i < tmp.length; i++) {
                    switch(i) {
                        case 5: { //gene symbols seperated by ,
                            values.add(tmp[i]);
                            break;
                        }
                        case 7: { //title
                            values.add(tmp[i]);
                            break;
                        }
                        case 9: { //mimId
                            values.add(tmp[i]);
                            break;
                        }
                        case 13: { //Disorders1
                            if(!tmp[i].equals("")) {
                                disorders = tmp[i];
                            }
                            break;
                        }
                        case 14: { //Disorders2
                            if(!tmp[i].equals("")) {
                                disorders += tmp[i];
                            }
                            break;
                        }
                        case 15: { //Disorders3
                            if(!tmp[i].equals("")) {
                                disorders += tmp[i];
                            }
                            break;
                        }
                    }
                }
                if(disorders != null) {
                    values.add(disorders);
                }
                valueList.add(values);
            }
            organism = createItem("Organism");
            organism.setAttribute("taxonId", "9606");
            writer.store(ItemHelper.convert(organism));
            
            storeValues();
        }
        
       
        private void storeValues() throws ObjectStoreException {
            for (int i = 0; i < valueList.size(); i++) {
                values = valueList.get(i);
                
                String geneSymbols = values.get(0);
                String[] geneSymbol = geneSymbols.split(",");
                
                String geneRefId = null;
                
                omimDisease = createItem("OMIM");
           
                for (int j = 0; j < geneSymbol.length; j++) {
                    if (geneMap.get(geneSymbol[j]) == null) {
                        gene = createItem("Gene");
                        gene.setAttribute("symbol", geneSymbol[j]);
                        geneRefId = gene.getIdentifier();
                        gene.addReference(new Reference("organism", organism.getIdentifier()));
                        writer.store(ItemHelper.convert(gene));
                        geneMap.put(geneSymbol[j], geneRefId);
                    } else {
                        geneRefId = geneMap.get(geneSymbol[j]);
                    }
                    omimDisease.addToCollection("genes", gene);
                }
                
                if (values.get(1) != null) {
                    omimDisease.setAttribute("title", values.get(1));
                }
                if (values.get(2) != null && !values.get(2).equals("")) {
                    omimDisease.setAttribute("omimId", values.get(2));
                }
                if (values.get(3) != null) {
                    omimDisease.setAttribute("description", values.get(3));
                }
                
                writer.store(ItemHelper.convert(omimDisease));
                
            }
        }
        
        protected Item createItem(String className) {
            return OMIMConverter.this.createItem(className);
        }
    }
}
