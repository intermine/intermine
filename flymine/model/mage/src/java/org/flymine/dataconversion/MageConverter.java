package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Method;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.Writer;
import java.io.Reader;

import org.biomage.tools.xmlutils.MAGEReader;
import org.biomage.QuantitationType.QuantitationType;
import org.biomage.DesignElement.Feature;
import org.biomage.BioAssayData.BioAssayData;
import org.biomage.BioAssayData.BioDataCube;
import org.biomage.BioAssayData.FeatureDimension;
import org.biomage.ArrayDesign.PhysicalArrayDesign;

import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.util.TypeUtil;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.FileConverter;
import org.intermine.metadata.Model;

import org.apache.log4j.Logger;

/**
 * Convert MAGE-ML to InterMine Full Data Xml via MAGE-OM objects.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class MageConverter extends FileConverter
{
    private long opCount;
    private long time;
    private long start;

    private static final Logger LOG = Logger.getLogger(MageConverter.class);

    protected static final String MAGE_NS = "http://www.flymine.org/model/mage#";

    protected HashMap seenMap;
    protected HashMap padIdentifiers = new HashMap();
    protected HashMap featureIdentifiers = new HashMap();
    protected ItemFactory itemFactory;
    protected Set qTypes = new HashSet();
    protected int id = 0;

    /**
     * @see FileConverter#FileConverter
     */
//     public MageConverter(ItemWriter writer) {
//         super(writer);
//     }


    public MageConverter(ItemWriter writer) throws Exception {
        super(writer);
        this.itemFactory = new ItemFactory(Model.getInstanceByName("mage"));
    }

    /**
     * @see FileConverter#process
     */
    public void process(Reader reader) throws Exception {

        setQTypes();

        seenMap = new LinkedHashMap();
        opCount = 0;
        time = System.currentTimeMillis();
        start = time;
        File f = new File(new File(System.getProperty("java.io.tmpdir")), "mageconvert.xml");
        File dtd = new File(new File(System.getProperty("java.io.tmpdir")), "MAGE-ML.dtd");
        BufferedReader dtdReader =  new BufferedReader(new
                InputStreamReader(getClass().getClassLoader().getResourceAsStream("MAGE-ML.dtd")));
        try {
            // write temporary file, MAGEreader wants write access for some reason
            Writer fileWriter = new FileWriter(f);
            int c;
            while ((c = reader.read()) > 0) {
                fileWriter.write(c);
            }
            fileWriter.close();

            // copy MAGE-ML.dtd to the same place
            fileWriter = new FileWriter(dtd);
            while ((c = dtdReader.read()) > 0) {
                fileWriter.write(c);
            }
            fileWriter.close();

            MAGEReader mageReader = new MAGEReader(f.getPath());
            createItem(mageReader.getMAGEobj());
        } finally {
            f.delete();
            dtd.delete();
        }
    }


    protected void setQTypes() {
        // param1 is a comma separated list of QuantitationTypes to include,
        // turn it into a set
        if (param1 != null) {
            StringTokenizer st = new StringTokenizer(param1, ",");
            while (st.hasMoreTokens()) {
                qTypes.add(((String) st.nextToken()).trim());
            }
        }
    }

    /**
     * @see FileConverter#process
     */
    public void close() throws Exception {
        long now = System.currentTimeMillis();
        storeItems(padIdentifiers.values());
        LOG.info("store padIdentifiers " + padIdentifiers.values().size()
                 + " in " + (System.currentTimeMillis() - now) + " ms");

        now = System.currentTimeMillis();
        storeItems(featureIdentifiers.values());
        LOG.info("store featureIdentifiers " + featureIdentifiers.values().size()
                 + " in " + (System.currentTimeMillis() - now) + " ms");
    }


    /**
     * Create an item and associated fields, references and collections
     * given a MAGE object.
     * @param obj a MAGE object to create items for
     * @return the created item
     * @throws Exception if reflection problems occur
     */
    protected Item createItem(Object obj) throws Exception {
        if (seenMap.containsKey(obj)) {
            return (Item) seenMap.get(obj);
        }
        boolean storeItem = true;
        Class cls = obj.getClass();
        String className = TypeUtil.unqualifiedName(cls.getName());
        Item item = new Item();
        item.setClassName(MAGE_NS + className);
        item.setImplementations("");
        if (!cls.getName().equals("org.biomage.Common.MAGEJava")
            && !className.endsWith("_package")) {
            if (!cls.getName().equals("org.biomage.ArrayDesign.PhysicalArrayDesign")
                && !cls.getName().equals("org.biomage.DesignElement.Feature")) {
                item.setIdentifier(alias(className) + "_" + (id++));
                seenMap.put(obj, item);
            }
        } else {
            // don't store the MAGEJava object
            storeItem = false;
        }

        opCount++;
        if (opCount % 1000 == 0) {
            long now = System.currentTimeMillis();
            LOG.info("Converted " + opCount + " objects - running at "
                     + (60000000 / (now - time)) + " (avg "
                     + ((60000L * opCount) / (now - start))
                     + ") objects per minute -- now on "
                     + item.getClassName());

            time = now;
        }

        for (Iterator i = TypeUtil.getFieldInfos(cls).values().iterator(); i.hasNext();) {
            TypeUtil.FieldInfo info = (TypeUtil.FieldInfo) i.next();
            Method m = info.getGetter();
            if (m.getParameterTypes().length == 0) {
                Object value = m.invoke(obj, null);
                if (value != null) {
                    if (Collection.class.isAssignableFrom(m.getReturnType())) {
                        ReferenceList col = new ReferenceList(info.getName());
                        StringBuffer sb = new StringBuffer();
                        for (Iterator j = ((Collection) value).iterator(); j.hasNext();) {
                            col.addRefId(createItem(j.next()).getIdentifier());
                        }
                        if (col.getRefIds().size() > 0) {
                            item.addCollection(col);
                        }
                    } else if (m.getReturnType().getName().startsWith("org.biomage")) {
                        if (m.getReturnType().getName().startsWith(cls.getName() + "$")) {
                            Method getName = value.getClass().getMethod("getName", null);
                            String attValue = (String) getName.invoke(value, null);
                            if (attValue != null) {
                                item.setAttribute(info.getName(), escapeQuotes(attValue));
                            } else {
                                LOG.warn("Null value for attribute " + info.getName() + " in Item "
                                         + item.getClassName() + " (" + item.getIdentifier() + ")");
                            }
                        } else {
                            item.setReference(info.getName(), createItem(value).getIdentifier());
                        }
                    } else if (!info.getName().equals("identifier")) {
                        item.setAttribute(info.getName(), escapeQuotes(value.toString()));
                        // TODO handle dates?
                    }
                }
            }
        }

        if (className.equals("PhysicalArrayDesign")) {
            // if item does not have name set it is a placeholder do not want to store
            PhysicalArrayDesign pad = (PhysicalArrayDesign) obj;
            String padId = pad.getIdentifier();
            Item padItem = (Item) padIdentifiers.get(padId);
            if (padItem == null) {
                item.setIdentifier(alias(className) + "_" + (id++));
                padIdentifiers.put(padId, item);
            } else if (pad.getName() != null) {
                item.setIdentifier(padItem.getIdentifier());
                padIdentifiers.put(padId, item);
            } else {
                item = padItem;
            }
            storeItem = false;
        } else if (className.equals("Feature")) {
            Feature feature = (Feature) obj;
            String fid = feature.getIdentifier();
            Item fItem = (Item) featureIdentifiers.get(fid);
            if (fItem == null) {
                item.setIdentifier(alias(className) + "_" + (id++));
                featureIdentifiers.put(fid, item);
            } else if (feature.getZone() != null) {
                item.setIdentifier(fItem.getIdentifier());
                featureIdentifiers.put(fid, item);
            } else {
                item = fItem;
            }
            storeItem = false;
        } else if (className.equals("DerivedBioAssayData")) {
            BioAssayData bad = (BioAssayData) obj;
            String fileName = ((BioDataCube) bad.getBioDataValues()).getDataExternal()
                .getFilenameURI();
            List colTypes = bad.getQuantitationTypeDimension().getQuantitationTypes();
            List rowNames = ((FeatureDimension) bad.getDesignElementDimension())
                .getContainedFeatures();
            BufferedReader br = new BufferedReader(new
                InputStreamReader(getClass().getClassLoader().getResourceAsStream(fileName)));
            Item bdt = makeItem("BioDataTuples");
            ReferenceList dataList = new ReferenceList("bioAssayTupleData");
            boolean storeTuple = false;
            for (Iterator i = rowNames.iterator(); i.hasNext();) {
                Feature feature = (Feature) i.next();
                String s = br.readLine();
                StringTokenizer st = new StringTokenizer(s, "\t");
                for (Iterator j = colTypes.iterator(); j.hasNext();) {
                    QuantitationType qt = (QuantitationType) j.next();
                    String value = st.nextToken();
                    if (qTypes.contains(qt.getName())) {
                        storeTuple = true;
                        Item datum = makeItem("BioAssayDatum");
                        dataList.addRefId(datum.getIdentifier());
                        datum.setReference("quantitationType", createItem(qt).getIdentifier());
                        datum.setReference("designElement", createItem(feature).getIdentifier());
                        datum.setAttribute("value", value);
                        // reference to BioAssayData is not in model but adding it makes translation
                        // easier, avoids enormous amount of prefetch
                        datum.setReference("bioAssayData", item.getIdentifier());
                        storeItem(datum);
                    }
                }
            }
            if (storeTuple) {
                bdt.addCollection(dataList);
                storeItem(bdt); // store BioDataTuple item
                item.setReference("bioDataValues", bdt.getIdentifier());
            }
        }
        if (storeItem) {
            storeItem(item);
        }
        return item;
    }


    private Item makeItem(String className) {
        return new Item(alias(className) + "_" + id++, MAGE_NS + className, "");
        // TODO get this to use an item factory.  Problem is with a class DataExternal
        // which appears in object model but is not defined in XMI.
        //return itemFactory.makeItem(alias(className) + "_" + id++, MAGE_NS + className, "");
    }

    private void storeItems(Collection items) throws Exception {
        writer.storeAll(ItemHelper.convertToFullDataItems(new ArrayList(items)));
    }

    private void storeItem(Item item) throws Exception {
        writer.store(ItemHelper.convert(item));
    }

    /**
     * escape quotes
     * @param s for input String
     * @return String
     */
    protected  String escapeQuotes(String s) {
        if (s.indexOf('\"') == -1) {
            return s;
        } else {
            return s.replaceAll("\"", "\\\\\"");
        }
    }
}
