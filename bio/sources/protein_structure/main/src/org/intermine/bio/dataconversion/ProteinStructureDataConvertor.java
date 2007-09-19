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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.intermine.InterMineException;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Xavier Watkins
 *
 */
public class ProteinStructureDataConvertor extends FileConverter
{
    private static final Logger LOG = Logger.getLogger(ProteinStructureDataConvertor.class);
    private String dataLocation;
    protected static final String ENDL = System.getProperty("line.separator");
    private Item proteinStructureExperiment;
    private Item dataSet;
    private final Map<String, Item> featureMap = new HashMap<String, Item>();
    private final Map<String, String> proteinMap = new HashMap<String, String>();
    private String parentDir;
    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public ProteinStructureDataConvertor(ItemWriter writer, Model model) {
        super(writer, model);
        proteinStructureExperiment = createItem("ProteinStructureExperiment");
        proteinStructureExperiment.setAttribute("type", "Computer prediction");
        dataSet = createItem("DataSet");
        dataSet.setAttribute("title", "Kenji Mizuguchi - NIBIO, Japan");
        dataSet.setAttribute("url", "http://www.nibio.go.jp");
        try {
            store(proteinStructureExperiment);
            store(dataSet);
        } catch (ObjectStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Pick up the data location from the ant, the translator needs to open some more files.
     * @param srcdatadir location of the source data
     */
    public void setSrcDataDir(String srcdatadir) {
        this.dataLocation = srcdatadir;
    }
    
    /**
     * @see FileConverter#process(Reader)
     */
    @Override
    public void process(Reader reader) throws Exception {
        File currentFile = getCurrentFile();
        parentDir = currentFile.getParent();
        if (currentFile.getName().endsWith(".xml")) {
            if (dataLocation == null || dataLocation.startsWith("$")) {
                throw new IllegalArgumentException("No data location specified, required"
                                  + "for finding .atm structure files (was: " + dataLocation + ")");
            }
            ProteinStructureHandler handler =
                new ProteinStructureHandler (getItemWriter(), proteinMap, featureMap);
            try {
                SAXParser.parse(new InputSource(reader), handler);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws ObjectStoreException {
        store(featureMap.values());
    }
    
    protected String getFileContent(String fileName, String extention) throws InterMineException {
        String str;
        StringBuffer fileBuffer = new StringBuffer();
        try {
            String filename =
                    ((parentDir.lastIndexOf("/") == (parentDir.length() - 1))
                            ? parentDir
                            : parentDir + "/")
                            + fileName;
            
            if (new File(filename).exists()) {
                BufferedReader in = new BufferedReader(new FileReader(filename));
                boolean firstLine = true;
                while ((str = in.readLine()) != null) {
                    if (!firstLine) {
                        fileBuffer.append(ENDL);
                    }
                    fileBuffer.append(str);
                    firstLine = false;
                }
                in.close();
            } else {
                LOG.warn("HTML or ATM FILE NOT FOUND:" + filename);
            }
        } catch (IOException e) {
            throw new InterMineException(e);
        }
        return fileBuffer.toString();
    }

    /**
     * Extension of PathQueryHandler to handle parsing TemplateQueries
     */
    class ProteinStructureHandler extends DefaultHandler
    {

        private Item proteinStructure;
        private String proteinItemIdentifier;
        private Item proteinFeature;
        private String attName = null;
        private StringBuffer attValue = null;
        private ItemWriter writer;
        private boolean alignmentFile = false;
        private Stack stack = new Stack();
        private Map<String, Item> featureMap;
        private Map<String, String> proteinMap;
        private String protId, strId, pfamId;
        
        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         */
        public ProteinStructureHandler (ItemWriter writer, Map proteinMap, Map featureMap)
            throws ObjectStoreException {
            this.writer = writer;
            this.proteinMap = proteinMap;
            this.featureMap = featureMap;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            super.startElement(uri, localName, qName, attrs);
            if (qName.equals("fragment")) {
                proteinStructure = createItem("ProteinStructure");
                strId = attrs.getValue("id");
            } else if (qName.equals("alignment_file")
                            && attrs.getValue("format").equals("joy_html")
                            && stack.peek().equals("model")) {
                            alignmentFile = true;
            }

            attName = qName;
            attValue = new StringBuffer();
            stack.push(attName);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            try {
                stack.pop();
                if (qName.equals("uniprot_id")) {
                    //Reference my be to proteinItemIdentifier accession
                    protId = attValue.toString();
                    proteinItemIdentifier = getProtein(protId);
                    proteinStructure.setReference("protein", proteinItemIdentifier);
                } else if (qName.equals("pfam_id")) {
                    pfamId = attValue.toString();
                    proteinFeature = getFeature(pfamId);
                    proteinStructure.setReference("proteinFeature", proteinFeature);
                } else if (qName.equals("begin")) {
                    proteinStructure.setAttribute("start", attValue.toString());
                } else if (qName.equals("end")) {
                    proteinStructure.setAttribute("end", attValue.toString());
                } else if (qName.equals("atomic_coordinate_file")) {
                    String atm;
                    try {
                        atm = getFileContent(attValue.toString(), ".atm");
                    } catch (InterMineException e) {
                        throw new SAXException(e);
                    }
                    proteinStructure.setAttribute("atm", atm);
                } else if (qName.equals("alignment_file") && alignmentFile) {
                    String html;
                    try {
                        html = getFileContent(attValue.toString(), ".html");
                    } catch (InterMineException e) {
                        throw new SAXException(e);
                    }
                    proteinStructure.setAttribute("alignment", html.toString());
                    alignmentFile = false;
                } else if (qName.equals("prosa_z_score")) {
                    proteinStructure.setAttribute("zScore", attValue.toString());
                } else if (qName.equals("prosa_q_score")) {
                    proteinStructure.setAttribute("qScore", attValue.toString());
                } else if (qName.equals("protein_structure")) {
                    proteinStructure.setReference("experiment", proteinStructureExperiment);
                    proteinStructure.setAttribute("identifier", protId + "_" + pfamId);
                    proteinStructure.setCollection("evidence",
                        new ArrayList(Collections.singleton(dataSet.getIdentifier())));
                    writer.store(ItemHelper.convert(proteinStructure));
                    protId = null;
                    pfamId = null;
                }
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        
        private String getProtein(String identifier) {
            String proteinIdentifier = proteinMap.get(identifier);
            if (proteinIdentifier == null) {
                Item protein = createItem("Protein");
                protein.setAttribute("primaryAccession", identifier);
                proteinMap.put(identifier, protein.getIdentifier());
                try {
                    writer.store(ItemHelper.convert(protein));
                } catch (ObjectStoreException e) {
                    throw new RuntimeException("error while storing: " + proteinItemIdentifier, e);
                }
                return protein.getIdentifier();
            } else {
                return proteinIdentifier;
            }
        }

        private Item getFeature(String identifier) {
            Item feature = featureMap.get(identifier);
            if (feature == null) {
                feature = createItem("ProteinFeature");
                feature.setAttribute("identifier", attValue.toString());
                featureMap.put(identifier, feature);
            }
            return feature;
        }

        /**
         * @see DefaultHandler#endElement
         */
        public void characters(char[] ch, int start, int length) throws SAXException
        {

            if (attName != null) {

                // DefaultHandler may call this method more than once for a single
                // attribute content -> hold text & create attribute in endElement
                while (length > 0) {
                    boolean whitespace = false;
                    switch(ch[start]) {
                    case ' ':
                    case '\r':
                    case '\n':
                    case '\t':
                        whitespace = true;
                        break;
                    default:
                        break;
                    }
                    if (!whitespace) {
                        break;
                    }
                    ++start;
                    --length;
                }

                if (length > 0) {
                    StringBuffer s = new StringBuffer();
                    s.append(ch, start, length);
                    attValue.append(s);
                }
            }
        }
    }
}
