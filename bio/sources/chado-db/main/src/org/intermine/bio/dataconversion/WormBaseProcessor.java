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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.chado.config.ConfigAction;
import org.intermine.bio.chado.config.SetFieldConfigAction;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * A converter for chado that handles WormBase specific configuration.
 * @author Kim Rutherford
 */
public class WormBaseProcessor extends SequenceProcessor
{
    private static final Logger LOG = Logger.getLogger(WormBaseProcessor.class);
    private Map<MultiKey, List<ConfigAction>> config;

    /**
     * Create a new WormBaseChadoDBConverter.
     * @param chadoDBConverter the converter that created this object
     */
    public WormBaseProcessor(ChadoDBConverter chadoDBConverter) {
        super(chadoDBConverter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Integer store(Item feature, int taxonId) throws ObjectStoreException {
        processItem(feature, new Integer(taxonId));
        Integer itemId = super.store(feature, taxonId);
        return itemId;
    }

    /**
     * Method to add dataSets and DataSources to items before storing
     */
    private void processItem(Item item, Integer taxonId) {
        if ("DataSource".equals(item.getClassName())
                || "DataSet".equals(item.getClassName())
                || "Organism".equals(item.getClassName())
                || "Sequence".equals(item.getClassName())) {
            return;
        }

        if (taxonId == null) {
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader classLoader = getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                throw new RuntimeException("getCurrentTaxonId() returned null while processing "
                        + item);
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
        }
        ChadoDBConverter converter = getChadoDBConverter();
        BioStoreHook.setDataSets(getModel(), item,  converter.getDataSetItem(
                taxonId.intValue()).getIdentifier(), converter.getDataSourceItem().getIdentifier());

    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<MultiKey, List<ConfigAction>> getConfig(int taxonId) {
        if (config == null) {
            config = new MultiKeyMap();
            config.put(new MultiKey("feature", "Gene", "WormBase", "uniquename"),
                    Arrays.asList(new SetFieldConfigAction("primaryIdentifier"),
                            CREATE_SYNONYM_ACTION));
            config.put(new MultiKey("feature", "Gene", "WormBase", "name"),
                    Arrays.asList(new SetFieldConfigAction("symbol"),
                            CREATE_SYNONYM_ACTION));
            config.put(new MultiKey("prop", "Gene", "cds"),
                    Arrays.asList(new SetFieldConfigAction("secondaryIdentifier"),
                            CREATE_SYNONYM_ACTION));
            // sequence names -> secondaryIdentifier.  These are synonyms, for some reason they have
            // is_current set to false and type 'exact'
            config.put(new MultiKey("synonym", "Gene", "exact", Boolean.FALSE),
                    Arrays.asList(new SetFieldConfigAction("secondaryIdentifier"),
                            CREATE_SYNONYM_ACTION));
        }

        return config;
    }

    private static final List<String> FEATURES = Arrays.asList(
            "gene", "mRNA", "transcript", "intron", "exon", "five_prime_untranslated_region",
            "five_prime_UTR", "three_prime_untranslated_region", "three_prime_UTR"
    );

    /**
     * Get a list of the chado/so types of the LocatedSequenceFeatures we wish to load.  The list
     * will not include chromosome-like features.
     * @return the list of features
     */
    @Override
    protected List<String> getFeatures() {
        return FEATURES;
    }

    /**
     * Process the identifier and return a "cleaned" version.  For WormBase, remove the class name
     * prefix on identifiers from the uniqueName in the feature table ("Gene:WBGene00023466" ->
     * "WBGene00023466")
     * @param fdat the FeatureData object
     * @param identifier the identifier
     * @return a cleaned identifier
     */
    @Override
    protected String fixIdentifier(FeatureData fdat, String identifier) {

        String uniqueName = fdat.getChadoFeatureUniqueName();
        String type = fdat.getInterMineType();

        // the function is used without check for null only for uniquename and name
        // in SequenceProcessor.
        // so we assume that uniquename is never null and that if null it is a name.
        if (StringUtils.isEmpty(identifier)) {
            identifier = uniqueName;
            LOG.debug("Found NULL name for feature: " + uniqueName);
        }

        if (identifier.startsWith(type + ":")) {
            return identifier.substring(type.length() + 1);
        }
        return identifier;
    }

    /**
     * Wormbase chado has pmid prefixed to pubmed identifiers
     * @param pubmedStr id fetched from databaase
     * @return the pubmed id
     */
    protected Integer fixPubMedId(String pubmedStr) {
        String prefix = "pmid";
        if (pubmedStr.startsWith(prefix)) {
            pubmedStr = pubmedStr.substring(prefix.length());
        }
        return Integer.parseInt(pubmedStr);
    }
}
