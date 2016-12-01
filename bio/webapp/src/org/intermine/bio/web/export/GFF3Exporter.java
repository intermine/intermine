package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.bio.ontology.SequenceOntology;
import org.intermine.bio.ontology.SequenceOntologyFactory;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.pathquery.Path;
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
        "https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=6239";
    /**
     * for the gff header, link to taxomony
     */
    public static final String FLY_LINK =
        "https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=7227";

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
    private List<Path> paths = Collections.emptyList();

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
     * @param paths paths
     */
    public GFF3Exporter(PrintWriter out, List<Integer> indexes, Map<String, String> soClassNames,
            List<String> attributesNames, String sourceName, Set<Integer> organisms,
            boolean makeUcscCompatible, List<Path> paths) {
        this.out = out;
        this.featureIndexes = indexes;
        this.soClassNames = soClassNames;
        this.attributesNames = attributesNames;
        this.sourceName = sourceName;
        this.organisms = organisms;
        this.makeUcscCompatible = makeUcscCompatible;
        this.paths = paths;

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

        if (organisms != null && !organisms.isEmpty()) {
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
    @Override
    public void export(Iterator<? extends List<ResultElement>> resultIt,
            Collection<Path> unionPathCollection, Collection<Path> newPathCollection) {
        if (featureIndexes.size() == 0) {
            throw new ExportException("No columns with sequence");
        }
        try {
            // LOG.info("SOO:" + cNames.toString());
            while (resultIt.hasNext()) {
                List<ResultElement> row = resultIt.next();
                exportRow(row, unionPathCollection, newPathCollection);
            }

            if (writtenResultsCount == 0) {
                out.println("Nothing to export. Sequence features might miss some information, "
                        + "e.g. chromosome location, etc.");
            }

            out.flush();
        } catch (Exception ex) {
            throw new ExportException("Export failed", ex);
        }
    }

    @Override
    public void export(Iterator<? extends List<ResultElement>> resultIt) {
        export(resultIt, paths, paths);
    }

    /* State for the exportRow method, to allow several rows to be merged. */
    private Map<String, Integer> attributeVersions = new HashMap<String, Integer>();
    private Integer lastLsfId = null;
    private SequenceFeature lastLsf = null;
    private Map<String, List<String>> attributes = null;
    private Map<String, Set<Integer>> seenAttributes = new HashMap<String, Set<Integer>>();

    private void exportRow(List<ResultElement> row,
            Collection<Path> unionPathCollection, Collection<Path> newPathCollection) {

        List<ResultElement> elWithObject = getResultElements(row);
        if (elWithObject == null) {
            return;
        }

        // In order to find the parents of a feature, e.g.
        // Gene.interactions.interactingGenes.exons.transcripts, we assuse each
        // feature's parents have unique path, e.g. transcript's parents paths
        // are interactingGene.exons and interactingGene, but exon is not a
        // parent of transcript, correct this by using SO.
        Map<String, Integer> pathToIdMap = new HashMap<String, Integer>();
        Map<Integer, List<String>> idToParentPathMap = new HashMap<Integer, List<String>>();
        Map<Integer, ResultElement> idToResultElementMap = new HashMap<Integer, ResultElement>();
        for (ResultElement el : elWithObject) {
            if (el == null) {
                continue;
            }

            List<Path> pathList = el.getPath().decomposePath();
            // remove the last element in the list, it is a field
            pathList.remove(pathList.size() - 1);
            String lastClassPath = pathList.get(pathList.size() - 1).toStringNoConstraints();

            Integer id = null;
            try {
                id = ((SequenceFeature) el.getObject()).getId();
            } catch (Exception e) {
                LOG.info("Object cannot be cast to SequenceFeature");
                continue;
            }

            idToResultElementMap.put(id, el);

            // from last sequence feature path to intermine id
            pathToIdMap.put(lastClassPath, id);

            // clsdList should have the same size as pathList
            List<ClassDescriptor> clsdList = el.getPath()
                    .getElementClassDescriptors();
            List<String> parentPaths = new LinkedList<String>();
            for (int i = clsdList.size() - 2; i >= 0; i--) {
                if (SequenceFeature.class.isAssignableFrom(clsdList.get(i)
                        .getType())) {
                    parentPaths.add(pathList.get(i).toStringNoConstraints());
                } else {
                    break;
                }
            }
            idToParentPathMap.put(id, parentPaths);
        }

        SequenceOntology so = SequenceOntologyFactory.getSequenceOntology();
        Path lastLsfPath = null;
        for (ResultElement re : elWithObject) {
            try {
                SequenceFeature lsf = (SequenceFeature) re.getObject();

                if (exportedIds.contains(lsf.getId()) && !(lsf.getId().equals(lastLsfId))) {
                    continue;
                }

                // processing parent for last cell
                if ((lastLsfId != null) && !(lsf.getId().equals(lastLsfId))) {
                    processAttributes(row, unionPathCollection, newPathCollection, re.getPath());
                    processParent(pathToIdMap, idToParentPathMap, idToResultElementMap, so);
                    makeRecord();
                }

                if (lastLsfId == null) {
                    attributes = new LinkedHashMap<String, List<String>>();
                }

                // NB:
                // processAttributes should run here for collection attributes,
                // e.g. Gene.pathways.name,
                // ticket: https://github.com/intermine/intermine/issues/218

                lastLsfId = lsf.getId();
                lastLsf = lsf;
                lastLsfPath = re.getPath();
            } catch (Exception ex) {
                LOG.error("Failed to write GFF3 file: " + ex);
                continue;
            }
        }

        // Export the last feature in the row
        if (lastLsfId != null && !exportedIds.contains(lastLsfId)) {
            processAttributes(row, unionPathCollection, newPathCollection, lastLsfPath);
            processParent(pathToIdMap, idToParentPathMap, idToResultElementMap, so);
            makeRecord();
        }
    }

    private void processAttributes(List<ResultElement> row,
            Collection<Path> unionPathCollection,
            Collection<Path> newPathCollection, Path p) {
        List<ResultElement> newRow = filterResultRow(row, unionPathCollection,
                newPathCollection);

        boolean isCollection = p.containsCollections();

        for (int i = 0; i < newRow.size(); i++) {
            ResultElement el = newRow.get(i);

            if (el == null) {
                continue;
            }

            // Disable collection export until further bug diagnose
            if (isCollection || el.getPath().containsCollections()) {
                continue;
            }
            //---------------------------------------------------------------
            // checks for attributes:
//            if (isCollection && !el.getPath().containsCollections()) {
//                // one is collection, the other is not: do not show
//                continue;
//            }
//            if (!isCollection && el.getPath().containsCollections()
//                    && soClassNames.containsKey(el.getType())) {
//                // show attributes only if they are not linked to features
//                // (they will be displayed with the relevant one, see below)
//                continue;
//            }
//
//            if (isCollection && el.getPath().containsCollections()) {
//                // show only if of the same class
//                Class<?> reType = p.getLastClassDescriptor().getType();
//                Class<?> elType = el.getPath().getLastClassDescriptor().getType();
//                if (!reType.isAssignableFrom(elType)) {
//                    continue;
//                }
//            }
            //---------------------------------------------------------------

            if ("location".equalsIgnoreCase(el.getPath()
                    .getLastClassDescriptor().getUnqualifiedName())) {
                // don't show locations (they are already displayed
                // parts of the element)
                continue;
            }

            if (el.getField() != null) {
                String unqualName = el.getPath()
                        .getLastClassDescriptor().getUnqualifiedName();
                String attributeName = trimAttribute(
                        attributesNames.get(i), unqualName);
                checkAttribute(el, attributeName);
            }
        }
    }

    private void processParent(Map<String, Integer> pathToIdMap,
            Map<Integer, List<String>> idToParentPathMap,
            Map<Integer, ResultElement> idToResultElementMap, SequenceOntology so) {

        Set<String> addPar = new LinkedHashSet<String>();
        List<String> parentPaths = idToParentPathMap.get(lastLsfId);
        if (!parentPaths.isEmpty()) {
            for (String parentPath : parentPaths) {
                Integer parentId = pathToIdMap.get(parentPath);
                // parents not in view, e.g. Gene.transcripts.exons, gene or/and
                // transcripts not display, thus Gene or/and Gene.transcripts
                // will not be in pathToIdMap
                if (parentId == null) {
                    continue;
                }
                SequenceFeature parent = (SequenceFeature) idToResultElementMap
                        .get(parentId).getObject();
                String parentClassName = idToResultElementMap.get(parentId)
                        .getPath().getLastClassDescriptor()
                        .getUnqualifiedName().toLowerCase();
                String featureClassName = idToResultElementMap.get(lastLsfId)
                        .getPath().getLastClassDescriptor()
                        .getUnqualifiedName().toLowerCase();
                // reverse relationship, e.g. Gene.exons.transcripts, exon
                // is not parent of transcript, use SO to validate the correct
                // parents
                // TODO Limitation - exon doesn't know transcript is a parent
                // since the parents are found reversely from path string, but
                // transcript is behind it
                Set<String> parentsSOTerms = so.getAllPartOfs(featureClassName);
                if (parentsSOTerms.contains(parentClassName)) {
                    addPar.add(parent.getPrimaryIdentifier());
                }
            }
        }
        if (addPar.size() > 0) {
            attributes.put("Parent", new ArrayList<String>(addPar));
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

    private static List<String> formatElementValue(ResultElement el) {
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
    @Override
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
    @Override
    public int getWrittenResultsCount() {
        return writtenResultsCount;
    }

    /**
     * Remove the elements from row which are not in pathCollection
     */
    private static List<ResultElement> filterResultRow(List<ResultElement> row,
            Collection<Path> unionPathCollection, Collection<Path> newPathCollection) {

        List<ResultElement> newRow = new ArrayList<ResultElement>();

        if (unionPathCollection == null && newPathCollection == null) {
            return row;
        }

        if (unionPathCollection.containsAll(newPathCollection)) {
            for (Path p : newPathCollection) {
                ResultElement el = null;
                if (!p.toString().endsWith(".id")) {
                    el = row.get(((List<Path>) unionPathCollection).indexOf(p));
                }
                newRow.add(el);
            }
            return newRow;
        } else {
            throw new RuntimeException("pathCollection: " + newPathCollection
                    + ", elPathList contains pathCollection: "
                    + unionPathCollection.containsAll(newPathCollection));
        }
    }
}
