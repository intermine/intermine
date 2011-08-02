package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.IntPresentSet;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.Exporter;

/**
 * Exports LocatedSequenceFeature objects in GFF3 format.
 * @author Kim Rutherford
 * @author Jakub Kulaviak
 */
public class GFF3Exporter implements Exporter
{
    private static final Logger LOG = Logger.getLogger(GFF3Exporter.class);


    //    public static final Set<String> GFF_FIELDS = Collections
    //    .unmodifiableSet(new HashSet<String>(Arrays.asList("chromosome.primaryIdentifier",
    //            "chromosomeLocation.start", "chromosomeLocation.end",
    //            "chromosomeLocation.strand", "primaryIdentifier", "score",
    //            "start", "end", "strand")));

    /**
     * the fields we don't want to display as attributes
     */
    public static final Set<String> GFF_FIELDS = Collections
    .unmodifiableSet(new HashSet<String>(Arrays.asList("chromosome.primaryIdentifier",
            "primaryIdentifier", "score")));
    /**
     * for the gff header, link to taxomony
     */
    public static final String WORM_LINK =
        "http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=6239";
    /**
     * for the gff header, link to taxomony
     */
    public static final String FLY_LINK =
        "http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=7227";

    PrintWriter out;
    private List<Integer> featureIndexes;
    private Map<String, String> soClassNames;
    private int writtenResultsCount = 0;
    private boolean headerPrinted = false;
    private IntPresentSet exportedIds = new IntPresentSet();
    private List<String> attributesNames;
    private String sourceName;
    private Set<Integer> organisms;
    // this one to store the lower case class names of  soClassNames,
    // for comparison with path elements classes.
    private Set<String> cNames = new HashSet<String>();
    private boolean makeUcscCompatible = false;

    /**
     * Constructor.
     * @param out output stream
     * @param indexes index of column with exported sequence
     * @param soClassNames mapping
     * @param attributesNames names of attributes that are printed in record,
     *  they are names of columns in results table, they are in the same order
     *  as corresponding columns in results table
     * @param sourceName name of Mine to put in GFF source column
     * @param organisms taxon id of the organisms
     * @param makeUcscCompatible true if chromosome ids should be prefixed by 'chr'
     */
    public GFF3Exporter(PrintWriter out, List<Integer> indexes, Map<String, String> soClassNames,
            List<String> attributesNames, String sourceName, Set<Integer> organisms,
            boolean makeUcscCompatible) {
        this.out = out;
        this.featureIndexes = indexes;
        this.soClassNames = soClassNames;
        this.attributesNames = attributesNames;
        this.sourceName = sourceName;
        this.organisms = organisms;
        this.makeUcscCompatible = makeUcscCompatible;

        for (String s : soClassNames.keySet()) {
            this.cNames.add(s.toLowerCase());
        }
    }


    /**
     * to read genome and project versions
     * @return header further info about versions
     */

    private String getHeaderParts() {
        StringBuffer header = new StringBuffer();
        Properties props = PropertiesUtil.getProperties();

        if (organisms != null) {
            for (Integer taxId : organisms) {
                if (taxId == 7227) {
                    String fV = props.getProperty("genomeVersion.fly");
                    if (fV != null && fV.length() > 0) {
                        header.append("\n##species " + FLY_LINK);
                        header.append("\n##genome-build FlyBase r" + fV);
                    }
                }
            }
            for (Integer taxId : organisms) {
                if (taxId == 6239) {
                    String wV = props.getProperty("genomeVersion.worm");
                    if (wV != null && wV.length() > 0) {
                        header.append("\n##species " + WORM_LINK);
                        header.append("\n##genome-build WormBase ws" + wV);
                    }
                }
            }

            // display only if organism is set
            String mV = props.getProperty("project.releaseVersion");
            if (mV != null && mV.length() > 0) {
                header.append("\n#" + this.sourceName + " " + mV);
                header.append("\n# #index-subfeatures 1");
            }

        }

        return header.toString();
    }

    private String getHeader() {
        return "##gff-version 3" + getHeaderParts();
    }


    /**
     * {@inheritDoc}
     */


    public void export(Iterator<? extends List<ResultElement>> resultIt) {
        if (featureIndexes.size() == 0) {
            throw new ExportException("No columns with sequence");
        }
        try {
            // LOG.info("SOO:" + cNames.toString());
            while (resultIt.hasNext()) {
                List<ResultElement> row = resultIt.next();
                exportRow(row);
            }
            finishLastRow();

            if (writtenResultsCount == 0) {
                out.println("Nothing was found for export");
            }

            out.flush();
        } catch (Exception ex) {
            throw new ExportException("Export failed", ex);
        }
    }

    /* State for the exportRow method, to allow several rows to be merged. */
    private Map<String, Integer> attributeVersions = new HashMap<String, Integer>();
    private Integer lastLsfId = null;
    private SequenceFeature lastLsf = null;
    private Map<String, List<String>> attributes = null;
    private Map<String, Set<Integer>> seenAttributes = new HashMap<String, Set<Integer>>();


    private void exportRow(List<ResultElement> row)
        throws ObjectStoreException,
    IllegalAccessException {

        List<ResultElement> elWithObject = getResultElements(row);
        if (elWithObject == null) {
            return;
        }

        // loop through all the objects in a row
        for (ResultElement re : elWithObject) {
            try {
                SequenceFeature lsf = (SequenceFeature) re.getObject();
                //             LOG.info("GFFrePath: " + re.getPath());
                boolean isCollection = re.getPath().containsCollections();

                if (exportedIds.contains(lsf.getId()) && !(lsf.getId().equals(lastLsfId))) {
                    continue;
                }

                if ((lastLsfId != null) && !(lsf.getId().equals(lastLsfId))) {
                    makeRecord();
                }

                if (lastLsfId == null) {
                    attributes = new LinkedHashMap<String, List<String>>();
                }

                String parent = null;
                String parentClass = null;

                for (int i = 0; i < row.size(); i++) {
                    ResultElement el = row.get(i);

                    if (el == null) {
                        continue;
                    }

                    if (i == 0) { // this is the beginning of the path
                        parentClass = el.getPath().getStartClassDescriptor().getUnqualifiedName();
//                        LOG.info("PAR: " + parentClass);
                        if (cNames.contains(parentClass.toLowerCase())) {
                            parent = el.getField().toString();
//                            LOG.info("PARent: " + parent);
                        }
                    }

                    // checks for attributes:
                    if (isCollection && !el.getPath().containsCollections()) {
                        // one is collection, the other is not: do not show
                        continue;
                    }
                    if (!isCollection && el.getPath().containsCollections()
                            && soClassNames.containsKey(el.getType())) {
                        // show attributes only if they are not linked to features
                        // (they will be displayed with the relevant one, see below)
                        continue;
                    }

                    if (isCollection && el.getPath().containsCollections()) {
                        // show only if of the same class
                        Class<?> reType = re.getPath().getLastClassDescriptor().getType();
                        Class<?> elType = el.getPath().getLastClassDescriptor().getType();
                        if (!reType.isAssignableFrom(elType)) {
                            continue;
                        }
                    }

                    if (el.getPath().getLastClassDescriptor().getUnqualifiedName().
                            equalsIgnoreCase("location")) {
                        // don't show locations (they are already displayed parts of the element)
                        continue;
                    }

                    if (el.getField() != null) {
                        String unqualName = el.getPath()
                                .getLastClassDescriptor().getUnqualifiedName();
                        String attributeName = trimAttribute(attributesNames.get(i), unqualName);
                        //                    LOG.info("IN: " + attributeName+"|"+ unqualName);
                        checkAttribute(el, attributeName);
                    }

                    // TEMP out (fm release)
                    // add the parent
                    if (i >= 1 && parent != null) {
                        if (!parentClass.equalsIgnoreCase(
                                re.getPath().getLastClassDescriptor().getUnqualifiedName())) {
//                    LOG.info("PAR: " + parentClass + " -> "
//                    + re.getPath().getLastClassDescriptor().getUnqualifiedName()
//                    + re.getPath().getLastClassDescriptor().getType()
//                    + re.getPath().getLastClassDescriptor().getName()
//                    + "|." + re.getPath().getLastClassDescriptor().getSubDescriptors()
//                    + "|.." + re.getPath().getLastClassDescriptor().getAllCollectionDescriptors()
//                    + "|..." + re.getPath().getLastClassDescriptor().getAllReferenceDescriptors()
//                        );
                            List<String> addPar = new ArrayList<String>();
                            addPar.add(parent);
                            attributes.put("Parent", addPar);
                        }
                    }
                }
                lastLsfId = lsf.getId();
                lastLsf = lsf;
            } catch (Exception ex) {
                LOG.error("Exception: " + ex);
                continue;
            }
        }
    }

    private String trimAttribute(String attribute, String unqualName) {
        if (!attribute.contains(".")) {
            return attribute;
        }
        // check if a feature attribute (display only name) or not (display all path)
        if (cNames.contains(unqualName.toLowerCase())) {
            String plainAttribute = attribute.substring(attribute.lastIndexOf('.') + 1);
            //            LOG.info("LCC: " +attribute+"->"+unqualName +"|"+ plainAttribute );
            return plainAttribute;
        }
        //        LOG.info("LCCno: " + attribute + "|" + unqualName);
        return attribute;
    }


    /**
     *
     */
    private void makeRecord() {

        GFF3Record gff3Record = GFF3Util.makeGFF3Record(lastLsf, soClassNames, sourceName,
                attributes, makeUcscCompatible);

        if (gff3Record != null) {
            // have a chromosome ref and chromosomeLocation ref
            if (!headerPrinted) {
                out.println(getHeader());
                headerPrinted = true;
            }

            out.println(gff3Record.toGFF3());
            exportedIds.add(lastLsf.getId());
            writtenResultsCount++;
        }
        lastLsfId = null;
        attributeVersions.clear();
        seenAttributes.clear();
    }


    /**
     * @param el
     * @param attributeName
     */
    private void checkAttribute(ResultElement el, String attributeName) {
        if (!GFF_FIELDS.contains(attributeName)) {
            Set<Integer> seenAttributeValues = seenAttributes.get(attributeName);
            if (seenAttributeValues == null) {
                seenAttributeValues = new HashSet<Integer>();
                seenAttributes.put(attributeName, seenAttributeValues);
            }
            if (!seenAttributeValues.contains(el.getId())) {
                seenAttributeValues.add(el.getId());
                Integer version = attributeVersions.get(attributeName);
                if (version == null) {
                    version = new Integer(1);
                    attributes.put(attributeName, formatElementValue(el));
                } else {
                    attributes.put(attributeName + version, formatElementValue(el));
                }
                attributeVersions.put(attributeName, new Integer(version.intValue() + 1));
            }
        }
    }



    private List<String> formatElementValue(ResultElement el) {
        List<String> ret = new ArrayList<String>();
        String s;
        if (el == null) {
            s = "-";
        } else {
            Object obj = el.getField();
            if (obj == null) {
                s = "-";
            } else {
                s = obj.toString();
            }
        }
        ret.add(s);
        return ret;
    }


    private List<ResultElement> getResultElements(List<ResultElement> row) {
        List<ResultElement> els = new ArrayList<ResultElement>();
        for (Integer index : featureIndexes) {
            if (row.get(index) != null) {
                els.add(row.get(index));
            }
        }
        return els;
    }


    /**
     * {@inheritDoc}
     */
    public boolean canExport(List<Class<?>> clazzes) {
        return canExportStatic(clazzes);
    }

    /* Method must have different name than canExport because canExport() method
     * is inherited from Exporter interface */
    /**
     * @param clazzes classes of result row
     * @return true if this exporter can export result composed of specified classes
     */
    public static boolean canExportStatic(List<Class<?>> clazzes) {
        return ExportHelper.getClassIndex(clazzes, SequenceFeature.class) >= 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getWrittenResultsCount() {
        return writtenResultsCount;
    }

    private void finishLastRow() {
        GFF3Record gff3Record = GFF3Util.makeGFF3Record(lastLsf, soClassNames, sourceName,
                attributes, makeUcscCompatible);

        if (gff3Record != null) {
            // have a chromsome ref and chromosomeLocation ref
            if (!headerPrinted) {
                out.println(getHeader());
                headerPrinted = true;
            }

            out.println(gff3Record.toGFF3());
            writtenResultsCount++;
        }
        lastLsfId = null;
    }
}
