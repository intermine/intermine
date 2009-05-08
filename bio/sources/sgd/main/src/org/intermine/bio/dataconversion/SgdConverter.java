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

import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.xml.sax.SAXException;


/**
 *
 * @author
 */
public class SgdConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(SgdConverter.class);
    private static final String DATASET_TITLE = "SGD";
    private static final String DATA_SOURCE_NAME = "SGD dataset";
    private static final String TAXON_ID = "4932";
    private Item organism;
    private Map<String, Integer> counts = new HashMap();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public SgdConverter(ItemWriter writer, Model model)
    throws ObjectStoreException {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);

        organism = createItem("Organism");
        organism.setAttribute("taxonId", TAXON_ID);
        store(organism);

    }

//    feature_no
//    feature_name
//    dbxref_id
//    feature_type
//    source
//    coord_version
//    stop_coord
//    start_coord
//    strand
//    gene_name
//    name_description
//    genetic_position
//    headline
//    date_created
//    created_by



//1
//YHR051W
//S000001093
//ORF
//SGD
//2005-11-07
//210145
//209699
//W
//COX6
//Cytochrome c
//OXidase
//Subunit VI of cytochrome c oxidase, which is the terminal member of the mitochondrial inner membrane electron transport chain; expression is regulated by oxygen levels
//2000-05-19
//OTTO



    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = lineIter.next();

            if (line.length != 15) {
                LOG.error("Line not processed:" + line);
            }

            String secondaryIdentifier = line[1];
            String primaryIdentifier = line[2];
            String a = line[3];
            String end = line[6];
            String start = line[7];
            String symbol = line[9];
            String name = line[10];

            incrementCount(a);

            // gene
            if (line[3].equals("ORF")) {
                Item gene = createItem("Gene");
                gene.setAttribute("secondaryIdentifier", secondaryIdentifier);
                gene.setAttribute("primaryIdentifier", primaryIdentifier);
                gene.setAttribute("symbol", symbol);
                gene.setAttribute("name", name);
                gene.setAttribute("length", getLength(start, end));
                gene.setReference("organism", organism);
                try {
                    store(gene);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            LOG.error("~~~ " + entry.getKey() + ":" + entry.getValue());
        }
    }

    private void incrementCount(String a) {
        if (counts.get(a) == null) {
            counts.put(a, new Integer(1));
        } else {
            Integer N = counts.get(a);
            int n = N.intValue();
            counts.put(a, n++);
        }
    }

    private String getLength(String start, String end)
    throws NumberFormatException {
        Integer a = new Integer(start);
        Integer b = new Integer(end);
        Integer length = new Integer(b.intValue() - a.intValue());
        return length.toString();
    }

//    private String getDataSource(String title)
//    throws SAXException {
//        String refId = datasources.get(title);
//        if (refId == null) {
//            Item item = createItem("DataSource");
//            item.setAttribute("name", title);
//            refId = item.getIdentifier();
//            datasources.put(title, refId);
//            try {
//                store(item);
//            } catch (ObjectStoreException e) {
//                throw new SAXException(e);
//            }
//        }
//        return refId;
//    }

}
