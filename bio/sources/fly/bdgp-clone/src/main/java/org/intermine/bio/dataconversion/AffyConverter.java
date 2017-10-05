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

import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * DataConverter to load flat file linking BDGP clones to Flybase genes.
 * @author Wenyan Ji
 */
public class AffyConverter extends CDNACloneConverter
{
//    protected static final Logger LOG = Logger.getLogger(AffyConverter.class);

    protected Map<String, Item> geneMap = new HashMap<String, Item>();
    private static final String PROBEPREFIX = "Affymetrix:CompositeSequence:HG-U133A:";
    private static final String PROBEURL = "https://www.affymetrix.com/LinkServlet?probeset=";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public AffyConverter(ItemWriter writer, Model model)
        throws ObjectStoreException, MetaDataException {
        super(writer, model, "Affymetrix GeneChip", "Affymetrix HG-U133A annotation data set");

        organism = createItem("Organism");
        organism.setAttribute("abbreviation", "HS");
        store(organism);
    }


    /**
     * Read each line from flat file.
     *
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {

        BufferedReader br = new BufferedReader(reader);
        //intentionally throw away first line
        String line = br.readLine();

        while ((line = br.readLine()) != null) {
            String[] array = line.split("\",\"", -1); //keep trailing empty Strings

            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }

            String probeId = array[0].substring(1);
            //String geneSymbol= array[14];//this is hugo identifier
            String geneEnsembl = array[17];
            //don't create probe if no ensembl id is given in the file
            if (geneEnsembl.startsWith("ENSG")) {
                Item probe = createProbe(probeId.trim());
                StringTokenizer st = new StringTokenizer(geneEnsembl, "///");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    Item gene = createGene(token.trim());
                    probe.addToCollection("genes", gene);
                }
                store(probe);
            }
        }
    }

    /**
     * @param clsName = target class name
     * @param ordId = ref id for organism
     * @param geneEnsembl = ensembl identifier used for gene primaryIdentifier
     * @return item
     */
    private Item createGene(String geneEnsembl)
        throws ObjectStoreException {
        Item gene = geneMap.get(geneEnsembl);
        if (gene == null) {
            gene = createItem("Gene");
            gene.setReference("organism", organism);
            gene.setAttribute("primaryIdentifier", geneEnsembl);
            geneMap.put(geneEnsembl, gene);
            store(gene);
        }
        return gene;
    }

    /**
     * @param id = identifier
     * @return item
     */
    private Item createProbe(String id) {
        Item probe = createItem("CompositeSequence");
        probe.setAttribute("primaryIdentifier", PROBEPREFIX + id);
        probe.setAttribute("name", id);
        probe.setAttribute("url", PROBEURL + id);
        probe.setReference("organism", organism);
        return probe;
    }
}
