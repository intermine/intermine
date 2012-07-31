package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import org.apache.log4j.Logger;

/**
 * A facade class to provide IdResolver service
 *
 * @author Fengyuan Hu
 */
public class IdResolverService
{
    protected static final Logger LOG = Logger.getLogger(IdResolverService.class);

    private EnsemblIdResolverFactory ensemblIdResolverFactory;
    private EntrezGeneIdResolverFactory entrezGeneIdResolverFactory; // default
    private FlyBaseIdResolverFactory flyBaseIdResolverFactory;
    private HgncIdResolverFactory hgncIdResolverFactory;
    private WormBaseChadoIdResolverFactory wormBaseChadoIdResolverFactory;
    private ZfinGeneIdResolverFactory zfinGeneIdResolverFactory;

    // Map from taxonid to factory, refer to MetadataCacheQueryService.java

    private IdResolverService() {

    }

    public static IdResolver getIdResolverByOrganism(Integer taxonId) {

        return getIdResolverByOrganism(taxonId, true);
    }

    public static IdResolver getIdResolverByOrganism(Integer taxonId, boolean failOnError) {

        return null;
    }

    public static IdResolver getFlyBaseIdResolver() {

        return null;
    }
}
