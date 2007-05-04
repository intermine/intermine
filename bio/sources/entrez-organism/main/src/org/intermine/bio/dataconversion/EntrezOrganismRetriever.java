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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

import org.flymine.model.genomic.Organism;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Class to fill in organism information using Entrez.
 * @author Mark Woodbridge
 * @author Kim Rutherford
 */
public class EntrezOrganismRetriever extends Task
{
    protected static final Logger LOG = Logger.getLogger(EntrezOrganismRetriever.class);
    // see http://eutils.ncbi.nlm.nih.gov/entrez/query/static/esummary_help.html for details
    protected static final String ESUMMARY_URL =
        "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=taxonomy&retmode=xml&id=";
    // number of summaries to retrieve per request
    protected static final int BATCH_SIZE = 50;
    private String osAlias = null;
    private String outputFile = null;

    static final String TARGET_NS = "http://www.flymine.org/model/genomic#";

    /**
     * Set the ObjectStore alias.
     * @param osAlias The ObjectStore alias
     */
    public void setOsAlias(String osAlias) {
        this.osAlias = osAlias;
    }

    /**
     * Set the output file name
     * @param outputFile The output file name
     */
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * For each Organism in the objectstore, retreive it's details from entrez using the taxon and
     * fill in the details in the organism object.
     * @throws BuildException if an error occurs
     */
    public void execute() throws BuildException {
        // Needed so that STAX can find it's implementation classes
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        if (osAlias == null) {
            throw new BuildException("osAlias attribute is not set");
        }
        if (outputFile == null) {
            throw new BuildException("outputFile attribute is not set");
        }

        LOG.info("Starting EntrezOrganismRetriever");

        Writer writer = null;

        try {
            writer = new FileWriter(outputFile);

            ObjectStore os = ObjectStoreFactory.getObjectStore(osAlias);

            Map orgMap = getOrganisms(os);

            Set taxonIds = new HashSet();
            Set toStore = new HashSet();

            ItemFactory itemFactory = new ItemFactory(os.getModel(), "-1_");
            writer.write(FullRenderer.getHeader() + "\n");
            for (Iterator i = orgMap.keySet().iterator(); i.hasNext();) {
                Integer taxonId = (Integer) i.next();
                taxonIds.add(taxonId);
                if (taxonIds.size() == BATCH_SIZE || !i.hasNext()) {
                    SAXParser.parse(new InputSource(getReader(taxonIds)),
                                    new Handler(toStore, itemFactory));
                    for (Iterator j = toStore.iterator(); j.hasNext();) {
                        Item item = (Item) j.next();
                        writer.write(FullRenderer.render(item));
                    }
                    taxonIds.clear();
                    toStore.clear();
                }
            }
            writer.write(FullRenderer.getFooter() + "\n");
        } catch (Exception e) {
            throw new BuildException("exception while retrieving organisms", e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Retrieve the organisms to be updated
     * @param os the ObjectStore to read from
     * @return a Map from taxonid to Organism object
     */
    protected Map getOrganisms(ObjectStore os) {
        Query q = new Query();
        QueryClass qc = new QueryClass(Organism.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        List results = os.executeSingleton(q);

        Map retMap = new HashMap();

        Iterator resIter = results.iterator();

        while (resIter.hasNext()) {
            Organism organism = (Organism) resIter.next();
            retMap.put(organism.getTaxonId(), organism);
        }

        return retMap;
    }

    /**
     * Obtain the pubmed esummary information for the organisms
     * @param ids the taxon ids of the organisms
     * @return a Reader for the information
     * @throws Exception if an error occurs
     */
    protected Reader getReader(Set ids) throws Exception {
        URL url = new URL(ESUMMARY_URL + StringUtil.join(ids, ","));
        return new BufferedReader(new InputStreamReader(url.openStream()));
    }

/*
Example

  <?xml version="1.0"?>
  <!DOCTYPE eSummaryResult PUBLIC "-//NLM//DTD eSummaryResult, 29 October 2004//EN" 
            "http://www.ncbi.nlm.nih.gov/entrez/query/DTD/eSummary_041029.dtd">
  <eSummaryResult>
    <DocSum>
      <Id>7227</Id>
      <Item Name="Rank" Type="String">species</Item>
      <Item Name="Division" Type="String">flies</Item>
      <Item Name="ScientificName" Type="String">Drosophila melanogaster</Item>
      <Item Name="CommonName" Type="String">fruit fly</Item>

      <Item Name="TaxId" Type="Integer">7227</Item>
      <Item Name="NucNumber" Type="Integer">566345</Item>
      <Item Name="ProtNumber" Type="Integer">74568</Item>
      <Item Name="StructNumber" Type="Integer">196</Item>
      <Item Name="GenNumber" Type="Integer">7</Item>
      <Item Name="GeneNumber" Type="Integer">21146</Item>

    </DocSum>
  </eSummaryResult>
*/

    /**
     * Extension of DefaultHandler to handle an esummary for an Organism
     */
    class Handler extends DefaultHandler
    {
        Set toStore;
        Item organism;
        String name;
        StringBuffer characters;
        ItemFactory itemFactory;

        /**
         * Constructor
         * @param toStore a set in which the new Organism items are stored
         * @param itemFactory the factory
         */
        public Handler(Set toStore, ItemFactory itemFactory) {
            this.toStore = toStore;
            this.itemFactory = itemFactory;
        }

        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            if ("ERROR".equals(qName)) {
                name = qName;
            } else if ("Id".equals(qName)) {
                name = "Id";
            } else {
                name = attrs.getValue("Name");
            }
            characters = new StringBuffer();
        }

        /**
         * @see DefaultHandler#characters
         */
        public void characters(char[] ch, int start, int length) {
            characters.append(new String(ch, start, length));
        }

        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) {
            if ("ERROR".equals(name)) {
                LOG.error("Unable to retrieve taxonomy record: " + characters);
            } else if ("Id".equals(name)) {
                organism = itemFactory.makeItemForClass(TARGET_NS + "Organism");
                toStore.add(organism);
                organism.setAttribute("taxonId", characters.toString());
            } else if ("ScientificName".equals(name)) {
                String text = characters.toString();
                organism.setAttribute("name", text);
                int spaceIndex = text.indexOf(" ");
                organism.setAttribute("genus", text.substring(0, spaceIndex));
                organism.setAttribute("species", text.substring(spaceIndex + 1));
                organism.setAttribute("shortName", text.charAt(0) + ". "
                                      + text.substring(spaceIndex + 1));
            }
            name = null;
        }
    }
}
