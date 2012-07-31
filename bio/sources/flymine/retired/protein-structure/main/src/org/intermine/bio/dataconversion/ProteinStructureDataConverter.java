package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.InterMineException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Item;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Xavier Watkins
 *
 */
public class ProteinStructureDataConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(ProteinStructureDataConverter.class);
    private String dataLocation;
    protected static final String ENDL = System.getProperty("line.separator");
    private final Map<String, Item> featureMap = new HashMap<String, Item>();
    private final Map<String, String> proteinMap = new HashMap<String, String>();
    private String parentDir;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ProteinStructureDataConverter(ItemWriter writer, Model model) {
        super(writer, model, "Kenji Mizuguchi", "Mizuguchi protein structure predictions", null);
    }

    /**
     * Pick up the data location from the ant, the translator needs to open some more files.
     * @param srcdatadir location of the source data
     */
    public void setSrcDataDir(String srcdatadir) {
        this.dataLocation = srcdatadir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader)
        throws Exception {
        File currentFile = getCurrentFile();
        parentDir = currentFile.getParent();
        if (currentFile.getName().endsWith(".xml")) {
            if (dataLocation == null || dataLocation.startsWith("$")) {
                throw new IllegalArgumentException("No data location specified, required"
                                  + "for finding .atm structure files (was: " + dataLocation + ")");
            }
            ProteinStructureHandler handler = new ProteinStructureHandler();
            try {
                SAXParser.parse(new InputSource(reader), handler);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

        }
    }

    /**
     * Get the content of the file as a String
     *
     * @param fileName the name of the file
     * @param extention the file extension
     * @return a String
     * @throws InterMineException an exception
     */
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
     * @author Xavier Watkins
     *
     */
    class ProteinStructureHandler extends DefaultHandler
    {

        private Item proteinStructure;
        private String proteinItemIdentifier;
        private Item proteinFeature;
        private String attName = null;
        private StringBuffer attValue = null;
        private boolean alignmentFile = false;
        private Stack<String> stack = new Stack<String>();
        private String protId, strId, pfamId;

        /**
         * Constructor
         */
        public ProteinStructureHandler () {
            // nothing to do
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            super.startElement(uri, localName, qName, attrs);
            if ("fragment".equals(qName)) {
                proteinStructure = createItem("ProteinStructure");
                strId = attrs.getValue("id");
            } else if ("alignment_file".equals(qName)
                            && "joy_html".equals(attrs.getValue("format"))
                            && "model".equals(stack.peek())) {
                alignmentFile = true;
            }

            attName = qName;
            attValue = new StringBuffer();
            stack.push(attName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            try {
                stack.pop();
                if ("uniprot_id".equals(qName)) {
                    //Reference my be to proteinItemIdentifier accession
                    protId = attValue.toString();
                    proteinItemIdentifier = getProtein(protId);
                    proteinStructure.setCollection("proteins", new ArrayList<String>(
                                                    Collections.singleton(proteinItemIdentifier)));
                } else if ("pfam_id".equals(qName)) {
                    pfamId = attValue.toString();
                    proteinFeature = getFeature(pfamId);
                    proteinStructure.setReference("proteinDomain", proteinFeature);
                } else if ("begin".equals(qName)) {
                    proteinStructure.setAttribute("start", attValue.toString());
                } else if ("end".equals(qName)) {
                    proteinStructure.setAttribute("end", attValue.toString());
                } else if ("atomic_coordinate_file".equals(qName)) {
                    String atm;
                    try {
                        atm = getFileContent(attValue.toString(), ".atm");
                    } catch (InterMineException e) {
                        throw new SAXException(e);
                    }
                    if (StringUtils.isEmpty(atm)) {
                        LOG.warn("found an empty atm for: " + strId);
                    } else {
                        proteinStructure.setAttribute("atm", atm);
                    }
                } else if ("alignment_file".equals(qName) && alignmentFile) {
                    String html;
                    try {
                        html = getFileContent(attValue.toString(), ".html");
                        // Strip some HTML off
                        html = html.replaceAll("<HTML>", "");
                        html = html.replaceAll("<BODY[^>]*>", "");
                        html = html.replaceAll("<META[^>]*>", "");
                        html = html.replaceAll("<HEAD>", "");
                        html = html.replaceAll("</HEAD>", "");
                        html = html.replaceAll("</BODY>", "");
                        html = html.replaceAll("</HTML>", "");
                    } catch (InterMineException e) {
                        throw new SAXException(e);
                    }
                    if (StringUtils.isEmpty(html)) {
                        LOG.warn("found an empty alignment for: " + strId);
                    } else {
                        proteinStructure.setAttribute("alignment", html.toString());
                    }
                    alignmentFile = false;
                } else if ("prosa_z_score".equals(qName)) {
                    proteinStructure.setAttribute("prosaZScore", attValue.toString());
                } else if ("prosa_q_score".equals(qName)) {
                    proteinStructure.setAttribute("prosaQScore", attValue.toString());
                } else if ("protein_structure".equals(qName)) {
                    proteinStructure.setAttribute("technique", "Computer prediction");
                    proteinStructure.setAttribute("identifier", protId + "_" + pfamId);
                    store(proteinStructure);
                    protId = null;
                    pfamId = null;
                }
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }

        private String getProtein(String identifier)
            throws ObjectStoreException {
            String proteinIdentifier = proteinMap.get(identifier);
            if (proteinIdentifier == null) {
                Item protein = createItem("Protein");
                protein.setAttribute("primaryAccession", identifier);
                protein.setAttribute("uniprotAccession", identifier);
                proteinMap.put(identifier, protein.getIdentifier());
                store(protein);
                return protein.getIdentifier();
            }
            return proteinIdentifier;
        }

        private Item getFeature(String identifier) throws ObjectStoreException {
            Item feature = featureMap.get(identifier);
            if (feature == null) {
                feature = createItem("ProteinDomain");
                feature.setAttribute("primaryIdentifier", attValue.toString());
                featureMap.put(identifier, feature);
                store(feature);
            }
            return feature;
        }

        /**
         * {@inheritDoc}
         */
        public void characters(char[] ch, int start, int length) {
            int st = start;
            int l = length;
            if (attName != null) {

                // DefaultHandler may call this method more than once for a single
                // attribute content -> hold text & create attribute in endElement
                while (l > 0) {
                    boolean whitespace = false;
                    switch(ch[st]) {
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
                    ++st;
                    --l;
                }

                if (l > 0) {
                    StringBuffer s = new StringBuffer();
                    s.append(ch, st, l);
                    attValue.append(s);
                }
            }
        }
    }
}
