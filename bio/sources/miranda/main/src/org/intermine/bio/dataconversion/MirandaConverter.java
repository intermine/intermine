package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * @author "Xavier Watkins"
 */
public class MirandaConverter extends FileConverter
{
    private Map<String, Item> targets = new HashMap<String, Item>();

    private Map<String, Item> miRNAgenes = new HashMap<String, Item>();

    private Map<String, Item> organisms = new HashMap<String, Item>();
    private Set<String> problems = new HashSet<String>();
    private Item dataSource, dataSet;

    private Map<String, String> mirandaToTaxonId = new HashMap<String, String>();
    protected IdResolverFactory resolverFactory;
    protected static final Logger LOG = Logger.getLogger(MirandaConverter.class);
    
    public MirandaConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model);
        mirandaToTaxonId.put("drosophila_melanogaster", "7227");
        mirandaToTaxonId.put("drosophila_pseudoobscura", "7237");
        mirandaToTaxonId.put("aedes_aegypti", "7159");
        mirandaToTaxonId.put("anopheles_gambiae", "7165");
        mirandaToTaxonId.put("dme", "7227");
        mirandaToTaxonId.put("aga", "7165");
        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "Memorial Sloan-Kettering Cancer Center");
        dataSet = createItem("DataSet");
        dataSet.setAttribute("title", "Computer prediction by miRanda");
        dataSet.setAttribute("url", "http://www.microrna.org/microrna/getDownloads.do");
        store(dataSource);
        dataSet.setReference("dataSource", dataSource.getIdentifier());
        store(dataSet);
        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory();
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        File currentFile = getCurrentFile();
        String fileName = currentFile.getName();
        String taxonId = mirandaToTaxonId.get(fileName.substring(fileName.lastIndexOf(".")+1));
        Item organism = getOrganism(taxonId);
        for (Iterator i = GFF3Parser.parse(new BufferedReader(reader)); i.hasNext();) {
            GFF3Record record = (GFF3Record) i.next();
            String geneName = record.getSequenceID();
            Item gene = getMiRNAGene(geneName);
            if (gene != null) {
                Item miRNAtarget = createItem("MiRNATarget");
                miRNAtarget.setAttribute("score", Double.toString(record.getScore()));
                miRNAtarget.setAttribute("start", Integer.toString(record.getStart()));
                miRNAtarget.setAttribute("end", Integer.toString(record.getEnd()));
                // TODO error in file score should be pvalue
                miRNAtarget.setAttribute("pvalue", record.getAttributes().get("score").iterator()
                                         .next());
                miRNAtarget.setReference("mirnagene", gene);
                miRNAtarget.setReference("target", getTarget(record.getAttributes().get("target")
                                                             .iterator().next(), organism));
                miRNAtarget.setReference("dataset", dataSet);
                store(miRNAtarget);
            }
        }
    }

    private Item getTarget(String targetName, Item organism) throws ObjectStoreException {
        Item target = targets.get(targetName);
        if (target == null) {
            target = createItem("MRNA");
            target.setAttribute("secondaryIdentifier", targetName);
            target.setReference("organism", organism);
            targets.put(targetName, target);
            store(target);
        }
        return target;
    }

    private Item getMiRNAGene(String geneName) throws ObjectStoreException {
        String geneNameToUse = (geneName.startsWith("dme")) ? geneName.substring(geneName
                                                                                 .indexOf("-") + 1) : geneName;
        // in FlyBase symbols are e.g. mir-5 not miR-5
        String symbol = geneNameToUse.toLowerCase();
        String taxonId = mirandaToTaxonId.get(geneName.substring(0, geneName.indexOf("-")));
        Item organism = getOrganism(taxonId);
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
                gene.setReference("organism", organism);
                miRNAgenes.put(primaryIdentifier, gene);
                store(gene);
            }
            return gene;
        } else {
            // no resolver available so use gene symbol
            Item gene = miRNAgenes.get(symbol);
            if (gene == null) {
                gene = createItem("Gene");
                gene.setAttribute("symbol", symbol);
                gene.setReference("organism", organism);
                miRNAgenes.put(symbol, gene);
                store(gene);
            }
            return gene;
        }
    }
    
    private Item getOrganism(String taxonId) throws ObjectStoreException {
        Item organism = organisms.get(taxonId);
        if (organism == null) {
            organism = createItem("Organism");
            organism.setAttribute("taxonId", taxonId);
            organisms.put(taxonId, organism);
            store(organism);
        }
        return organism;
    }
    
}
