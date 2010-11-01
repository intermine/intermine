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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;
import org.intermine.xml.full.Item;

/**
 * 
 * @author
 */
public class EnsemblGwasDbConverter extends BioDBConverter
{
    // 
    private static final String DATASET_TITLE = "Add DataSet.title here";
    private static final String DATA_SOURCE_NAME = "Add DataSource.name here";

    private Map<String, String> snps = new HashMap<String, String>();
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, String> sources = new HashMap<String, String>();
    private int taxonId = 9606;


    /**
     * Construct a new EnsemblGwasDbConverter.
     * @param database the database to read from
     * @param model the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle Items created
     */
    public EnsemblGwasDbConverter(Database database, Model model, ItemWriter writer) {
        super(database, model, writer, DATA_SOURCE_NAME, DATASET_TITLE);
    }


    /**
     * {@inheritDoc}
     */
    public void process() throws Exception {
        // a database has been initialised from properties starting with db.ensembl-gwas-db

        Connection connection = getDatabase().getConnection();

        
        // TODO move this to a parser arguement

        int counter = 0;
        int snpCounter = 0;
        Item currentSnp = null;
        String currentRsNumber = null;
        Set<String> consequenceIdentifiers = new HashSet<String>();
        ResultSet res = queryVariationAnnotation(connection);
        while (res.next()) {
            String sourceName = res.getString("s.name");
            if ("HGMD-PUBLIC".equals(sourceName)) {
                continue;
            }

            counter++;
            String rsNumber = res.getString("variation_name");
            String snpIdentifier = getSNPIdentifier(rsNumber);

            String associatedGene = res.getString("associated_gene");

            Item result = createItem("GWASResult");
            result.setReference("SNP", snpIdentifier);

            setAttributeIfPresent(result, "phenotype", res.getString("p.description"));
            setAttributeIfPresent(result, "riskAlleleFreqInControls",
                    "" + res.getDouble("risk_allele_freq_in_controls"));
            setAttributeIfPresent(result, "pValue", res.getString("p_value"));
            setAttributeIfPresent(result, "associatedVariantRiskAllele",
                    res.getString("associated_variant_risk_allele"));
            String study = res.getString("study");
            String pubIdentifier = getPubIdentifier(study);
            if (pubIdentifier != null) {
                result.setReference("publication", pubIdentifier);
            }
            String source = res.getString("s.name");
            result.setReference("source", getSourceIdentifier(source));


            store(result);
            System.out.println("SNP: " + rsNumber);
        }
    }

    private void setAttributeIfPresent(Item item, String attName, String attValue) {
        if (!StringUtils.isBlank(attValue)) {
            item.setAttribute(attName, attValue);
        }
    }

    private String getSNPIdentifier(String rsNumber) throws ObjectStoreException {
        String snpIdentifier = snps.get(rsNumber);
        if (snpIdentifier == null) {
            Item snp = createItem("SNP");
            snp.setAttribute("primaryIdentifier", rsNumber);
            store(snp);
            snpIdentifier = snp.getIdentifier();
            snps.put(rsNumber, snpIdentifier);
        }
        return snpIdentifier;
    }

    private String getPubIdentifier(String study) throws ObjectStoreException {
        String pubmedIdentifier = null;
        if (!StringUtils.isBlank(study)) {
            pubmedIdentifier = pubs.get(study);
            if (pubmedIdentifier == null) {
                String pubmedId = study.substring("pubmed/".length());
                Item pub = createItem("Publication");
                pub.setAttribute("pubMedId", pubmedId);
                store(pub);
                pubmedIdentifier = pub.getIdentifier();
                pubs.put(study, pubmedIdentifier);
            }
        }
        return pubmedIdentifier;
    }

    private String getGeneIdentifier(String symbol) throws ObjectStoreException {
        String geneIdentifier = genes.get(symbol);
        if (geneIdentifier == null) {
            Item gene = createItem("Gene");
            gene.setAttribute("symbol", symbol);
            gene.setReference("organism", getOrganismItem(taxonId));
            store(gene);
            geneIdentifier = gene.getIdentifier();
            snps.put(symbol, geneIdentifier);
        }
        return geneIdentifier;
    }
    
    private List<String> getGeneCollection(String input) throws ObjectStoreException {
        List<String> stateIdentifiers = new ArrayList<String>();
        if (!StringUtils.isBlank(input)) {
            for (String state : input.split(",")) {
                stateIdentifiers.add(getStateIdentifier(state));
            }
        }
        return stateIdentifiers;
    }

    private String getStateIdentifier(String name) throws ObjectStoreException {
        String stateIdentifier = genes.get(name);
        if (stateIdentifier == null) {
            Item state = createItem("ValidationState");
            state.setAttribute("name", name);
            store(state);
            stateIdentifier = state.getIdentifier();
            genes.put(name, stateIdentifier);
        }
        return stateIdentifier;
    }

    private String getSourceIdentifier(String name) throws ObjectStoreException {
        String sourceIdentifier = sources.get(name);
        if (sourceIdentifier == null) {
            Item source = createItem("Source");
            source.setAttribute("name", name);
            store(source);
            sourceIdentifier = source.getIdentifier();
            sources.put(name, sourceIdentifier);
        }
        return sourceIdentifier;
    }
    
    private ResultSet queryVariationAnnotation(Connection connection) throws SQLException {
        String query = "SELECT vf.variation_name, "
            + " va.study, va.study_type, va.local_stable_id, va.associated_gene, "
            + " va.associated_variant_risk_allele, va.risk_allele_freq_in_controls, va.p_value, "
            + " p.description,"
            + " s.name"
            + " FROM variation_annotation va, variation_feature vf, phenotype p, source s"
            + " WHERE va.variation_id = vf.variation_id"
            + " AND va.source_id = s.source_id"
            + " AND va.phenotype_id = p.phenotype_id"
            + " ORDER BY va.variation_id"
            + " LIMIT 100";

        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDataSetTitle(int taxonId) {
        return DATASET_TITLE;
    }
}
