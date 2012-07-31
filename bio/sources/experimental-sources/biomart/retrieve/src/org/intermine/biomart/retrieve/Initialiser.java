package org.intermine.biomart.retrieve;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.util.FormattedTextParser;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * @author "Xavier Watkins"
 */
public class Initialiser extends Task
{

    private Map<String, BioMartDataSet> datasets = new HashMap<String, BioMartDataSet>();

    /**
     * {@inheritDoc}
     */
    public void execute() throws BuildException {
        try {
            XmlHandler handler = new XmlHandler();
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            // Get all the marts
            String config = getBioMartResponse(
            "http://www.biomart.org/biomart/martservice?type=datasets&mart=uniprot");
            StringReader sr = new StringReader(config);
            BufferedReader br = new BufferedReader(sr);
            Iterator lineIter = FormattedTextParser.parseTabDelimitedReader((Reader) br);
            while (lineIter.hasNext()) {
                String[] line = (String[]) lineIter.next();
                if (line.length > 0 && !line[0].equals(" ")) {
                    String dataset = line[1];
                    String xml = getBioMartResponse(
                    "http://www.biomart.org/biomart/martservice?type=configuration&dataset="
                                                    + dataset);
                    xr.parse(new InputSource(new StringReader(xml)));
                }
            }
            datasets = handler.getDatasets();
            saveBioMartModelXml(datasets);
            // StringReader sr = new StringReader(config);
            // BufferedReader br = new BufferedReader(sr);
            // Iterator lineIter = FormattedTextParser.parseTabDelimitedReader((Reader) br);
            // while (lineIter.hasNext()) {
            // String[] line = (String[]) lineIter.next();
            // if (line.length > 0 && !line[0].equals(" ")) {
            // String dataset = line[1];
            // String attrConf =
         // getBioMartResponse("http://www.biomart.org/biomart/martservice?type=attributes&dataset="
            // + dataset);
            // StringReader sr2 = new StringReader(attrConf);
            // BufferedReader br2 = new BufferedReader(sr2);
            // Iterator lineIter2 = FormattedTextParser.parseTabDelimitedReader(br2);
            // List<String> attributes = new ArrayList<String>();
            // while (lineIter2.hasNext()) {
            // String[] line2 = (String[]) lineIter2.next();
            // if (line2.length > 0 && !line2[0].equals(" ") && !line2[0].equals("")) {
            // attributes.add(line2[0]);
            // }
            // }
            // datasets.put(dataset, attributes);
            // }
            // }
            // saveBioMartModelXml(datasets);
        } catch (SAXException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * @param urlString the url for the service as a string
     * @return the String returned by the BioMart service
     */
    public String getBioMartResponse(String urlString) {
        StringBuffer sb = new StringBuffer();
        try {
            URL url = new URL(urlString);
            URLConnection urlCon;
            try {
                urlCon = url.openConnection();
                urlCon.setRequestProperty("Connection", "close");
                urlCon.getInputStream();

                // retrieve result
                BufferedReader br = new BufferedReader(new InputStreamReader(urlCon
                                .getInputStream()));
                String str;
                while ((str = br.readLine()) != null) {
                    sb.append(str);
                    sb.append("\n");
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return (sb.toString());
    }

    /**
     * @param datasets A Map of dataset names to their BioMartDataSets
     * @throws IOException exception
     * @throws SAXException exception
     */
    public void saveBioMartModelXml(Map<String, BioMartDataSet> datasets) throws IOException,
                    SAXException {
        FileOutputStream fos = new FileOutputStream("build/biomart-model.xml");
        OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
        of.setIndent(1);
        of.setIndenting(true);
        XMLSerializer serializer = new XMLSerializer(fos, of);
        ContentHandler hd = serializer.asContentHandler();
        hd.startDocument();
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "", "name", "CDATA", "biomart");
        atts.addAttribute("", "", "namespace", "CDATA", "http://www.biomart.org/biomart#");
        hd.startElement("", "", "model", atts);
        for (String dataset : datasets.keySet()) {
            atts.clear();
            atts.addAttribute("", "", "name", "CDATA", "org.biomart.model." + dataset);
            hd.startElement("", "", "class", atts);
            List<BioMartField> attrs = datasets.get(dataset).getAttributes();
            for (BioMartField field: attrs) {
                atts.clear();
                atts.addAttribute("", "", "name", "CDATA", field.getInternalName());
                atts.addAttribute("", "", "nametype", "CDATA", "java.lang.String");
                hd.startElement("", "", "attribute", atts);
                hd.endElement("", "", "attribute");
            }
            hd.endElement("", "", "class");
        }
        hd.endElement("", "", "model");
        hd.endDocument();
        fos.close();
    }
}
