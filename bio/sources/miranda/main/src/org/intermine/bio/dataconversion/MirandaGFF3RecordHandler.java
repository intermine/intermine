package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * @author "Xavier Watkins"
 */
public class MirandaGFF3RecordHandler extends GFF3RecordHandler
{
    private Map<String, Item> targets = new HashMap<String, Item>();

    private Map<String, Item> miRNAgenes = new HashMap<String, Item>();

    private Set<String> problems = new HashSet<String>();

    private URI nameSpace;

    protected IdResolverFactory resolverFactory;

    protected static final Logger LOG = Logger.getLogger(MirandaGFF3RecordHandler.class);

    public MirandaGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);
        resolverFactory = new FlyBaseIdResolverFactory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
        Item feature = getFeature();
        nameSpace = getTargetModel().getNameSpace();
        feature.setClassName(nameSpace + "MiRNATarget");
        String geneName = record.getAttributes().get("Name").iterator().next();
        String targetName = record.getAttributes().get("target").iterator().next();
        feature.setAttribute("pvalue", record.getAttributes().get("pvalue").iterator().next());
        try {
            Item gene = getMiRNAGene(geneName);
            Item target = getTarget(targetName);
            if (gene != null) {
                feature.setReference("mirnagene", gene);
                feature.setReference("target", target);
            }
        } catch (ObjectStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Item getTarget(String targetName) throws ObjectStoreException {
        Item target = targets.get(targetName);
        if (target == null) {
            target = createItem("MRNA");
            target.setAttribute("secondaryIdentifier", targetName);
            target.addToCollection("dataSets", getDataSet());
            targets.put(targetName, target);
            addEarlyItem(target);
        }
        return target;
    }

    private Item getMiRNAGene(String geneName) throws ObjectStoreException {
        String geneNameToUse = (geneName.startsWith("dme")) ? geneName.substring(geneName
                        .indexOf("-") + 1) : geneName;
        // in FlyBase symbols are e.g. mir-5 not miR-5
        String symbol = geneNameToUse.toLowerCase();
        String taxonId = getOrganism().getAttribute("taxonId").getValue();
        IdResolver resolver = resolverFactory.getIdResolver();
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
                gene = createItem("Gene");
                gene.setAttribute("primaryIdentifier", primaryIdentifier);
                gene.addToCollection("dataSets", getDataSet());
                miRNAgenes.put(primaryIdentifier, gene);
                addItem(gene);
            }
            return gene;
        } else {
            // no resolver available so use gene symbol
            Item gene = miRNAgenes.get(symbol);
            if (gene == null) {
                gene = createItem("Gene");
                gene.setAttribute("symbol", symbol);
                gene.setReference("organism", getOrganism());
                gene.addToCollection("dataSets", getDataSet());
                miRNAgenes.put(symbol, gene);
                addItem(gene);
            }
            return gene;
        }
    }

}
