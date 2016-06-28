package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Loads disease name and identifier from orphanet. the annotations are set in the OMIM source.
 *
 * @author Julie Sullivan
 */
public class OrphanetConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "Orphanet data set";
    private static final String DATA_SOURCE_NAME = "Orphanet";
    private static final String PREFIX = "ORPHANET:";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public OrphanetConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        File currentFile = getCurrentFile();
        if ("en_product1.xml".equals(currentFile.getName())) {
            processXML(currentFile);
        } else {
            throw new RuntimeException("Don't know how to process file: " + currentFile.getName());
        }
    }

    private void processXML(File file)
            throws SAXException, IOException, ParserConfigurationException, ObjectStoreException {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();

        NodeList entryList = doc.getElementsByTagName("Disorder");

        for (int i = 0; i < entryList.getLength(); i++) {
            Element entry = (Element) entryList.item(i);
            String orphaNumber = entry.getElementsByTagName("OrphaNumber").item(0).getTextContent();
            String diseaseName = entry.getElementsByTagName("Name").item(0).getTextContent();
            Item item = createItem("Disease");
            item.setAttribute("identifier", PREFIX + orphaNumber);
            item.setAttribute("name", diseaseName);
            store(item);
        }
    }
}
