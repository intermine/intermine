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

import java.util.Set;

import org.apache.log4j.Logger;

/**
 * A facade class to provide IdResolver service.
 * Notes:
 * 1. user can configure which resolver they want to use in a properties file?
 *    e.g. NCBI and flybase both can resolver fly genes, set priority?
 * 2. resolver reads specific data format, the code is depended on data file. It
 *    need to reach to a consensus with MODs. In IdResolverFactory, the method
 *    createFromFile will do the parsing. One way to generalise the process is
 *    to configure in a properties file to let the class know the identifier
 *    information by column. e.g. type=tab, column.0=mainId, etc.
 * 3. any instance of resolver should be caching during build
 * 4. data sync issue: NCBI entrez info might be out of sync with other MOD datasets
 * 5. how to add new resolver? By reflection?
 * 6. Resolver is cached within a source? smart caching cross sources?
 *
 * @author Fengyuan Hu
 */
public class IdResolverService
{
    protected static final Logger LOG = Logger.getLogger(IdResolverService.class);

    private IdResolverService() {
    }

    /**
     * Create a Entrez Gene Id Resolver by given taxonId
     * @param taxonId taxon id as a string
     * @return an IdResolver
     */
    public static IdResolver getIdResolverByOrganism(String taxonId) {
        return new EntrezGeneIdResolverFactory().getIdResolver(taxonId);
    }

    /**
     * Create a Entrez Gene Id Resolver by given taxonId set
     * @param taxonIda set of taxon ids
     * @return an IdResolver
     */
    public static IdResolver getIdResolverByOrganism(Set<String> taxonIds) {
        return new EntrezGeneIdResolverFactory().getIdResolver(taxonIds);
    }

    /**
     * Create a Entrez Gene Id Resolver by given taxonId
     * @param taxonId taxon id as a string
     * @param failOnError if false swallow any exceptions and return null
     * @return an IdResolver
     */
    public static IdResolver getIdResolverByTaxonId(String taxonId, boolean failOnError) {
        return new EntrezGeneIdResolverFactory().getIdResolver(taxonId, failOnError);
    }

    /**
     * Create a Entrez Gene Id Resolver by given taxonId set
     * @param taxonId a set of taxon ids
     * @param failOnError if false swallow any exceptions and return null
     * @return an IdResolver
     */
    public static IdResolver getIdResolverByTaxonId(Set<String> taxonIds, boolean failOnError) {
        return new EntrezGeneIdResolverFactory().getIdResolver(taxonIds, failOnError);
    }

    /**
     * Create a fly id resolver
     * @return an IdResolver
     */
    public static IdResolver getFlyIdResolver() {
        return new FlyBaseIdResolverFactory().getIdResolver(false);
    }

    /**
     * Create a fly id resolver
     * @param clsName SO term
     * @return an IdResolver
     */
    public static IdResolver getFlyIdResolver(String clsName) {
        return new FlyBaseIdResolverFactory(clsName).getIdResolver(false);
    }

    /**
     * Create a fly id resolver
     * @param clsName SO term
     * @param failOnError if false swallow any exceptions and return null
     * @return an IdResolver
     */
    public static IdResolver getFlyIdResolver(String clsName, boolean failOnError) {
        return new FlyBaseIdResolverFactory(clsName).getIdResolver(failOnError);
    }

    /**
     * Create a worm id resolver
     * @return an IdResolver
     */
    public static IdResolver getWormIdResolver() {
        return new WormBaseChadoIdResolverFactory().getIdResolver(false);
    }

    /**
     * Create a worm id resolver
     * @param clsName SO term
     * @return an IdResolver
     */
    public static IdResolver getWormIdResolver(String clsName) {
        return new WormBaseChadoIdResolverFactory(clsName).getIdResolver(false);
    }

    /**
     * Create a worm id resolver
     * @param clsName SO term
     * @param failOnError if false swallow any exceptions and return null
     * @return an IdResolver
     */
    public static IdResolver getWormIdResolver(String clsName, boolean failOnError) {
        return new WormBaseChadoIdResolverFactory(clsName).getIdResolver(failOnError);
    }

    /**
     * Create a fish id resolver
     * @return an IdResolver
     */
    public static IdResolver getFishIdResolver() {
        return new ZfinIdentifiersResolverFactory().getIdResolver(false);
    }

    /**
     * Create a fish id resolver
     * @param clsName SO term
     * @return an IdResolver
     */
    public static IdResolver getFishIdResolver(String clsName) {
        return new ZfinIdentifiersResolverFactory(clsName).getIdResolver(false);
    }

    /**
     * Create a fish id resolver
     * @param clsName SO term
     * @param failOnError if false swallow any exceptions and return null
     * @return an IdResolver
     */
    public static IdResolver getFishIdResolver(String clsName, boolean failOnError) {
        return new ZfinIdentifiersResolverFactory(clsName).getIdResolver(failOnError);
    }

    /**
     * Create a mouse id resolver
     * @return an IdResolver
     */
    public static IdResolver getMouseIdResolver() {
        return new MgiIdentifiersResolverFactory().getIdResolver(false);
    }

    /**
     * Create a mouse id resolver
     * @param clsName SO term
     * @return a IdResolver
     */
    public static IdResolver getMouseIdResolver(String clsName) {
        return new MgiIdentifiersResolverFactory(clsName).getIdResolver(false);
    }

    /**
     * Create a mouse id resolver
     * @param clsName SO term
     * @param failOnError if false swallow any exceptions and return null
     * @return an IdResolver
     */
    public static IdResolver getMouseIdResolver(String clsName, boolean failOnError) {
        return new MgiIdentifiersResolverFactory(clsName).getIdResolver(failOnError);
    }

    /**
     * Create a rat id resolver
     * @return an IdResolver
     */
    public static IdResolver getRatIdResolver() {
        return new RgdIdentifiersResolverFactory().getIdResolver(false);
    }

    /**
     * Create a rat id resolver
     * @param clsName SO term
     * @return an IdResolver
     */
    public static IdResolver getRatIdResolver(String clsName) {
        return new RgdIdentifiersResolverFactory(clsName).getIdResolver(false);
    }

    /**
     * Create a rat id resolver
     * @param clsName SO term
     * @param failOnError if false swallow any exceptions and return null
     * @return an IdResolver
     */
    public static IdResolver getRatIdResolver(String clsName, boolean failOnError) {
        return new RgdIdentifiersResolverFactory(clsName).getIdResolver(failOnError);
    }

    /**
     * Create a HGNC human gene id resolver
     * @return an IdResolver
     */
    public static IdResolver getHgncIdResolver() {
        return new HgncIdResolverFactory().getIdResolver(false);
    }

    /**
     * Create a HGNC human gene resolver
     * @param failOnError if false swallow any exceptions and return null
     * @return an IdResolver
     */
    public static IdResolver getHgncIdResolver(boolean failOnError) {
        return new HgncIdResolverFactory().getIdResolver(failOnError);
    }

    /**
     * Create a Ensembl gene id resolver
     * @return an IdResolver
     */
    public static IdResolver getEnsemblIdResolver() {
        return new EnsemblIdResolverFactory().getIdResolver(false);
    }

    /**
     * Create a Ensembl gene resolver
     * @param failOnError if false swallow any exceptions and return null
     * @return an IdResolver
     */
    public static IdResolver getEnsemblIdResolver(boolean failOnError) {
        return new EnsemblIdResolverFactory().getIdResolver(failOnError);
    }

    /**
     * Create a GO id resolver
     * @param ontology SO term
     * @return an IdResolver
     */
    public static IdResolver getGoIdResolver(String ontology) {
        return new OntologyIdResolverFactory(ontology).getIdResolver(false);
    }

    /**
     * Create a GO id resolver
     * @param ontology SO term
     * @param failOnError if false swallow any exceptions and return null
     * @return an IdResolver
     */
    public static IdResolver getGoIdResolver(String ontology, boolean failOnError) {
        return new OntologyIdResolverFactory(ontology).getIdResolver(failOnError);
    }

    /**
     * Create a mock id resolver for unit test
     * @param clsName SO term
     * @return an IdResolver
     */
    public static IdResolver getMockIdResolver(String clsName) {
        return new MockIdResolverFactory(clsName).getIdResolver();
    }
}
