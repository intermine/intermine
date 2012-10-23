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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * @author "Xavier Watkins"
 */
public class MirandaGFF3RecordHandler extends GFF3RecordHandler
{
    private Map<String, Item> targets = new HashMap<String, Item>();
    private Map<String, Item> miRNAgenes = new HashMap<String, Item>();
    private Set<String> problems = new HashSet<String>();
    protected IdResolverFactory geneResolverFactory;
    protected IdResolverFactory mrnaResolverFactory;

    protected static final Logger LOG = Logger.getLogger(MirandaGFF3RecordHandler.class);

    /**
     * Create a new MirandaGFF3RecordHandler
     * @param tgtModel the model for which items will be created
     */
    public MirandaGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);
        geneResolverFactory = new FlyBaseIdResolverFactory("gene");
        mrnaResolverFactory = new FlyBaseIdResolverFactory("mRNA");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
        Item feature = getFeature();
        feature.setClassName("MiRNATarget");
        String geneName = record.getAttributes().get("Name").iterator().next();
        String targetName = record.getAttributes().get("target").iterator().next();
        feature.setAttribute("pvalue", record.getAttributes().get("pvalue").iterator().next());
        Item gene = getMiRNAGene(geneName);
        Item target = getTarget(targetName);
        if (gene != null) {
            feature.setReference("mirnagene", gene);
        }
        if (target != null) {
            feature.setReference("target", target);
        }
    }

    private Item getTarget(String targetName) {
        Item target = null;
        IdResolver resolver = mrnaResolverFactory.getIdResolver();
        String primaryIdentifier = null;
        int resCount = resolver.countResolutions("7227", targetName);
        if (resCount == 1) {
            primaryIdentifier = resolver.resolveId("7227", targetName).iterator().next();
            target = targets.get(primaryIdentifier);
            if (target == null) {
                target = converter.createItem("MRNA");
                target.setAttribute("primaryIdentifier", primaryIdentifier);
                target.setReference("organism", getOrganism().getIdentifier());
                targets.put(primaryIdentifier, target);
                addEarlyItem(target);
            }
        } else {
            LOG.info("RESOLVER: failed to resolve mRNA to one identifier, ignoring mRNA: "
                     + targetName + " count: " + resCount);
        }
        return target;
    }

    private Item getMiRNAGene(String geneName) {
        String geneNameToUse = (geneName.startsWith("dme")) ? geneName.substring(geneName
                        .indexOf("-") + 1) : geneName;
        // in FlyBase symbols are e.g. mir-5 not miR-5
        String symbol = geneNameToUse.toLowerCase();
        String taxonId = getOrganism().getAttribute("taxonId").getValue();
        IdResolver resolver = geneResolverFactory.getIdResolver();
        if (resolver.hasTaxon(taxonId)) {
            int resCount = resolver.countResolutions(taxonId, symbol);
            if (resCount != 1) {
                if (!problems.contains(symbol)) {
                    LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                             + symbol + " count: " + resCount + " FBgn: "
                             + resolver.resolveId(taxonId, symbol));
                    problems.add(symbol);
                }
                return null;
            }
            String primaryIdentifier = resolver.resolveId(taxonId, symbol).iterator().next();
            Item gene = miRNAgenes.get(primaryIdentifier);
            if (gene == null) {
                gene = converter.createItem("Gene");
                gene.setAttribute("primaryIdentifier", primaryIdentifier);
                miRNAgenes.put(primaryIdentifier, gene);
                addItem(gene);
            }
            return gene;
        }
        // no resolver available so use gene symbol
        Item gene = miRNAgenes.get(symbol);
        if (gene == null) {
            gene = converter.createItem("Gene");
            gene.setAttribute("symbol", symbol);
            gene.setReference("organism", getOrganism());
            miRNAgenes.put(symbol, gene);
            addItem(gene);
        }
        return gene;
    }
}
