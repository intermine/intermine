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

public class Ws220CDSGFF3Converter extends GFF3Converter {
    private static final Logger LOG = Logger.getLogger(Ws220CDSGFF3Converter.class);

    LOG.info ("INWS220!! FILE SYSTEM");


    /**
     * process GFF3 record and give a xml presentation
     * @param record GFF3Record
     * @throws ObjectStoreException if an error occurs storing items
     */
    public void process(GFF3Record record) throws ObjectStoreException {
        String term = record.getType();

    LOG.info ("INWS220!! ====");

        if (config_term != null && !config_term.isEmpty()) { // otherwise all terms are processed
            if (config_term.containsKey(this.orgTaxonId)) {
                if (!config_term.get(this.orgTaxonId).contains(term)) {
                    return;
                }
            }
        }

        // By default, use ID field in attributes
        String primaryIdentifier = record.getId();
        // If pid set in gff_config.properties, look for the attribute field, e.g. locus_tag
        if (config_attr.containsKey(this.orgTaxonId)) {
            if (config_attr.get(this.orgTaxonId).containsKey("primaryIdentifier")) {
                String pidAttr = config_attr.get(this.orgTaxonId).get("primaryIdentifier");
                if (pidAttr.contains("Dbxref")) {
                    String pidAttrPrefix = pidAttr.split("\\.")[1];
                    for (Iterator<?> i = record.getDbxrefs().iterator(); i.hasNext(); ) {
                        String xref = (String) i.next();
                        if (xref.contains(pidAttrPrefix)) {
                            primaryIdentifier = xref.split(":")[1];
                            break;
                        }
                    }
                } else {
                    primaryIdentifier = record.getAttributes().get(pidAttr).get(0);
                }
            }
        }

        String refId = identifierMap.get(primaryIdentifier);

        // get rid of previous record Items from handler
        handler.clear();

        Item seq = getSeq(record.getSequenceID());

        String className = TypeUtil.javaiseClassName(term);
        String fullClassName = tgtModel.getPackageName() + "." + className;

        ClassDescriptor cd = tgtModel.getClassDescriptorByName(fullClassName);

        if (cd == null) {
            throw new IllegalArgumentException("no class found in model for: " + className
                    + " (original GFF record type: " + term + ") for "
                    + "record: " + record);
        }

        Set<Item> synonymsToAdd = new HashSet<Item>();

        Item feature = null;

        // new feature
        if (refId == null) {
            feature = createItem(className);
            refId = feature.getIdentifier();
        }

        if (!"chromosome".equals(term) && seq != null) {
            boolean makeLocation = record.getStart() >= 1 && record.getEnd() >= 1
                && !dontCreateLocations
                && handler.createLocations(record);
            if (makeLocation) {
                Item location = getLocation(record, refId, seq, cd);
                if (feature == null) {
                    // this feature has already been created and stored
                    // we only wanted the location, we're done here.
                    store(location);
                    return;
                }
                int length = getLength(record);
                feature.setAttribute("length", String.valueOf(length));
                handler.setLocation(location);
                if ("Chromosome".equals(seqClsName)
                        && (cd.getFieldDescriptorByName("chromosome") != null)) {
                    feature.setReference("chromosome", seq.getIdentifier());
                    feature.setReference("chromosomeLocation", location);
                }
            }
        }

        if (feature == null) {
            // this feature has already been created and stored
            // feature with discontinous location, this location wasn't valid for some reason
            return;
        }

        if (primaryIdentifier != null) {
            feature.setAttribute("primaryIdentifier", primaryIdentifier);
        }
        handler.setFeature(feature);
        identifierMap.put(primaryIdentifier, feature.getIdentifier());

        // for secondaryIdentifier
        if (config_attr.containsKey(this.orgTaxonId)) {
            if (config_attr.get(this.orgTaxonId).containsKey("secondaryIdentifier")) {
                String siAttr = config_attr.get(this.orgTaxonId).get("secondaryIdentifier");
                if (record.getAttributes().get(siAttr) != null) {
                    String secondaryIdentifier = record.getAttributes().get(siAttr).get(0);
                    if (secondaryIdentifier != null) {
                        feature.setAttribute("secondaryIdentifier", secondaryIdentifier);
                    }
                }
            }
        }

        List<?> names = record.getNames();
        String symbol = null;
        List<String> synonyms = null;

        // get the attribute set for symbol
        if (config_attr.containsKey(this.orgTaxonId)) {
            if (config_attr.get(this.orgTaxonId).containsKey("symbol")) {
                String symbolAttr = config_attr.get(this.orgTaxonId).get("symbol");
                if (record.getAttributes().get(symbolAttr) != null) {
                    symbol = record.getAttributes().get(symbolAttr).get(0);
                }
            }
        }

        // get the attribute set for synonym
        if (config_attr.containsKey(this.orgTaxonId)) {
            if (config_attr.get(this.orgTaxonId).containsKey("synonym")) {
                String synonymAttr = config_attr.get(this.orgTaxonId).get("synonym");
                synonyms = record.getAttributes().get(synonymAttr);
            }
        }

        if (names != null) {
            setNames(names, symbol, synonyms, synonymsToAdd, primaryIdentifier, feature, cd);
        }

        List<String> parents = record.getParents();
        if (parents != null && !parents.isEmpty()) {
            setRefsAndCollections(parents, feature);
        }

        feature.addReference(getOrgRef());
        feature.addToCollection("dataSets", dataSet);

        handler.addDataSet(dataSet);
        Double score = record.getScore();
        if (score != null && !"".equals(String.valueOf(score))) {
            feature.setAttribute("score", String.valueOf(score));
            feature.setAttribute("scoreType", record.getSource());
        }
        for (Item synonym : synonymsToAdd) {
            handler.addItem(synonym);
        }
        handler.process(record);
        if (handler.getDataSetReferenceList().getRefIds().size() > 0) {
            feature.addCollection(handler.getDataSetReferenceList());
        }
        handler.clearDataSetReferenceList();
        if (handler.getPublicationReferenceList().getRefIds().size() > 0) {
            feature.addCollection(handler.getPublicationReferenceList());
        }
        handler.clearPublicationReferenceList();

        try {
            Iterator<Item> iter = handler.getItems().iterator();
            while (iter.hasNext()) {
                store(iter.next());
            }
        } catch (ObjectStoreException e) {
            LOG.error("Problem writing item to the itemwriter");
            throw e;
        }
    }




}
