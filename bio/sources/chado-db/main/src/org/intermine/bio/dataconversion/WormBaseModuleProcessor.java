package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
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

import org.intermine.bio.chado.config.ConfigAction;
import org.intermine.bio.chado.config.SetFieldConfigAction;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 * A converter for chado that handles WormBase specific configuration.
 * @author Kim Rutherford
 */
public class WormBaseModuleProcessor extends ChadoSequenceProcessor
{
    private Map<MultiKey, List<ConfigAction>> config;

    /**
     * Create a new WormBaseChadoDBConverter.
     * @param chadoDBConverter the converter that created this object
     */
    public WormBaseModuleProcessor(ChadoDBConverter chadoDBConverter) {
        super(chadoDBConverter);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<MultiKey, List<ConfigAction>> getConfig(@SuppressWarnings("unused") int taxonId) {
       if (config == null) {
           config = new MultiKeyMap();
           config.put(new MultiKey("feature", "Gene", "WormBase", "uniquename"),
                      Arrays.asList(new SetFieldConfigAction("primaryIdentifier"),
                                    new SetFieldConfigAction("secondaryIdentifier"),
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
     * @param type the InterMine type of the feature that this identifier came from
     * @param identifier the identifier
     * @return a cleaned identifier
     */
    @Override
    protected String fixIdentifier(String type, String identifier) {
        if (identifier.startsWith(type + ":")) {
            return identifier.substring(type.length() + 1);
        } else {
            return identifier;
        }
    }
}
