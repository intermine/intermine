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
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Method;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.Writer;
import java.io.Reader;
import java.io.IOException;

import org.biomage.tools.xmlutils.MAGEReader;
import org.biomage.QuantitationType.QuantitationType;
import org.biomage.DesignElement.Feature;
import org.biomage.DesignElement.Reporter;
import org.biomage.DesignElement.CompositeSequence;
import org.biomage.BioAssay.BioAssay;
import org.biomage.BioAssayData.BioAssayData;
import org.biomage.BioAssayData.BioDataCube;
import org.biomage.BioAssayData.BioAssayDimension;
import org.biomage.BioAssayData.DesignElementDimension;
import org.biomage.BioAssayData.FeatureDimension;
import org.biomage.BioAssayData.ReporterDimension;
import org.biomage.BioAssayData.CompositeSequenceDimension;
import org.biomage.BioAssayData.QuantitationTypeDimension;
import org.biomage.Interface.HasBioAssays;
import org.biomage.DesignElement.DesignElement;
import org.biomage.Common.MAGEJava;
import org.biomage.Common.Identifiable;

import org.intermine.xml.full.Item;
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

    protected HashMap seenMap = new LinkedHashMap();
    protected HashMap refMap = new LinkedHashMap();
    protected ItemFactory itemFactory;
    protected Set qTypes = new HashSet();
    protected int id = 0;

    /**
     * @see FileConverter#FileConverter
     */
    public MageConverter(ItemWriter writer) throws Exception {
        super(writer);
        this.itemFactory = new ItemFactory(Model.getInstanceByName("mage"));
    }

    /**
     * @see FileConverter#process
     */
    public void process(Reader reader) throws Exception {
        opCount = 0;
        time = System.currentTimeMillis();
        start = time;

        createItem(MageConverter.readMage(reader), true);
        LOG.error("refMap " + refMap);
    }

    /**
     * Build MAGE java objects from a given XML file
     * @param reader access to MAGE-ML file
     * @return MAGE Java representation
     * @throws IOException if problems reading file
     */
    protected static MAGEJava readMage(Reader reader) throws IOException {
        File f = new File(new File(System.getProperty("java.io.tmpdir")), "mageconvert.xml");
        File dtd = new File(new File(System.getProperty("java.io.tmpdir")), "MAGE-ML.dtd");
        System .out.println(f.getAbsolutePath());
        BufferedReader dtdReader =  new BufferedReader(new InputStreamReader(
            MageConverter.class.getClassLoader().getResourceAsStream("MAGE-ML.dtd")));
        MAGEReader mageReader = null;
        try {
            // write temporary file, MAGEreader wants write access for some reason
            Writer fileWriter = new BufferedWriter(new FileWriter(f));
            int c;
            while ((c = reader.read()) > 0) {
                fileWriter.write(c);
            }
            fileWriter.close();

            // copy MAGE-ML.dtd to the same place
            fileWriter = new BufferedWriter(new FileWriter(dtd));
            while ((c = dtdReader.read()) > 0) {
                fileWriter.write(c);
            }
            fileWriter.close();

            mageReader = new MAGEReader(f.getAbsolutePath());

        } finally {
             f.delete();
             dtd.delete();
        }
        //LOG.info("mageReader.getMAGEobj "+ mageReader.getMAGEobj());
        return mageReader.getMAGEobj();
    }


    /**
     * Only create BioAssayDatum items for results with a QuantitationType specified
     * in this comma separated list.
     * @param types comma separated list of quantitation types
     */
    public void setQuantitationtypes(String types) {
        StringTokenizer st = new StringTokenizer(types, ",");
        while (st.hasMoreTokens()) {
            qTypes.add(((String) st.nextToken()).trim());
        }
    }

    /**
     * @see FileConverter#process
     */
    public void close() throws Exception {

    }


    /**
     * Create an item and associated fields, references and collections
     * given a MAGE object.
     * @param obj a MAGE object to create items for
     * @param create boolean to create item or not
     * @return the created item
     * @throws Exception if reflection problems occur
     */
    protected String createItem(Object obj, boolean create) throws Exception {
        boolean storeItem = true;
        String objId = null;
        String itemIdentifier = null;

        if (!create) {
            return null;
        }

        if (obj instanceof Identifiable) {
            objId = ((Identifiable) obj).getIdentifier();
        }

        if (objId != null && seenMap.containsKey(objId)) {
            return  (String) seenMap.get(objId);
        } else if (objId == null && seenMap.containsKey(obj)) {
            return  (String) seenMap.get(obj);
        }

        Class cls = obj.getClass();
        String className = TypeUtil.unqualifiedName(cls.getName());
        Item item = new Item();
        item.setClassName(MAGE_NS + className);
        item.setImplementations("");

        if (!cls.getName().equals("org.biomage.Common.MAGEJava")
            && !className.endsWith("_package")) {
            //refMap: key=objId, value=itemIdentifier this map is only used
            //for storing those objects
            // are reffed in one xml file but not defined in the same file
            if (objId != null && refMap.containsKey(objId)) {
                itemIdentifier = (String) refMap.get(objId);
            } else {
                itemIdentifier = alias(className) + "_" + (id++);
            }

            item.setIdentifier(itemIdentifier);
            //seenMap: key=objId/Obj value=item.identifier
            //seenMap only store item that is defined.
            //objId will be removed in later stage if not defined
            if (objId != null) {
                seenMap.put(objId, item.getIdentifier());
            } else {
                seenMap.put(obj, item.getIdentifier());
            }

        } else {
            // don't store the MAGEJava object
            storeItem = false;
        }

        opCount++;
        if (opCount % 10000 == 0) {
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
                        for (Iterator j = ((Collection) value).iterator(); j.hasNext();) {
                            Object mageObj = j.next();
                            if (mageObj.getClass().getName().endsWith("NameValueType")
                                && info.getName().equals("propertySets")) {
                                HashMap map = new HashMap(TypeUtil.getFieldInfos(
                                    mageObj.getClass()));
                                if (map.containsKey("name")
                                    && checkNameValueType(mageObj, map, "name").equals("Placeholder")
                                    && map.containsKey("value")
                                    && checkNameValueType(mageObj, map, "value") == null
                                    && map.containsKey("type")
                                    && checkNameValueType(mageObj, map, "type") == null) {
                                    refMap.put(objId, itemIdentifier);
                                    seenMap.remove(objId);
                                    createItem(mageObj, false);
                                }
                            } else {
                                col.addRefId(createItem(mageObj, true));
                            }
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
                            item.setReference(info.getName(), createItem(value, true));

                        }
                    } else { // if (!info.getName().equals("identifier")) {
                        item.setAttribute(info.getName(), escapeQuotes(value.toString()));
                        // TODO handle dates?
                    }
                }
            }

        }


        if (className.equals("DerivedBioAssayData")) {
            BioAssayData bad = (BioAssayData) obj;
            String fileName = ((BioDataCube) bad.getBioDataValues()).getDataExternal()
                .getFilenameURI();

            // TODO throw an exception if all dimensions are > 1
            BioDataCube bdc = (BioDataCube)  bad.getBioDataValues();
            BioDataCube.Order order = bdc.getOrder(); //DBQ
            // Order 2 = DBQ, if anyhting different this code *may* not work so throw
            // an exception just in case.
            System.err .println("BioDataCube.Order " + order.getValue());
            if (order.getValue() != 2) {
                throw new IllegalArgumentException("BioDataCube has order other than DBQ "
                                                   + "(was: " + order.getValue() + ")."
                                                   + " Current code may not work.");
            }
            List rowNames = null; //D
            DesignElementDimension ddimension =
                  (DesignElementDimension) bad.getDesignElementDimension();

            if (ddimension instanceof FeatureDimension) {
                rowNames = ((FeatureDimension) ddimension).getContainedFeatures();
            } else if (ddimension instanceof ReporterDimension) {
                rowNames = ((ReporterDimension) ddimension).getReporters();
            } else if (ddimension instanceof CompositeSequenceDimension) {
                rowNames = ((CompositeSequenceDimension) ddimension).getCompositeSequences();
            }

            QuantitationTypeDimension qdimension =
                (QuantitationTypeDimension) bad.getQuantitationTypeDimension(); //Q
            List qtList = qdimension.getQuantitationTypes();


            BioAssayDimension bdimension = (BioAssayDimension) bad.getBioAssayDimension();  //B
            HasBioAssays.BioAssays_list colTypes = bdimension.getBioAssays();

            boolean emptyFile = false;
            InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
            if (is.available() == 0) {
                emptyFile = true;
                LOG.warn("Ignoring empty data file: " + fileName);
            }

            System.err .println("Reading data from: " + fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            Item bdt = makeItem("BioDataTuples");
            ReferenceList dataList = new ReferenceList("bioAssayTupleData");
            boolean storeTuple = false;

            // if all three dimensions are greater than 1 then this code will not work,
            // need to look into re-writing with better access to contents of BioDataCube.
            if ((rowNames.size() > 1) && (colTypes.size() > 1) && (qtList.size() > 1)) {
                throw new IllegalArgumentException("All dimensions of BioDataCube were > 1 "
                                                   + "it is unlikely that the current code will "
                                                   + "work.");
            }

            for (Iterator i = rowNames.iterator(); i.hasNext() && !emptyFile;) {
                DesignElement feature = (DesignElement) i.next();
                String s = br.readLine();
                StringTokenizer st = new StringTokenizer(s, "\t");
                for (Iterator j = colTypes.iterator(); j.hasNext();) {
                    BioAssay ba = (BioAssay) j.next();
                    for (Iterator k = qtList.iterator(); k.hasNext();) {
                        QuantitationType qt = (QuantitationType) k.next();
                        String value = st.nextToken();

                        if (qTypes.contains(qt.getName())) {
                            storeTuple = true;
                            Item datum = makeItem("BioAssayDatum");
                            dataList.addRefId(datum.getIdentifier());
                            datum.setReference("quantitationType", createItem(qt, true));
                            if (feature instanceof Feature) {
                                datum.setReference("designElement", createItem(feature, true));
                            } else if (feature instanceof Reporter) {
                                datum.setReference("reporter", createItem(feature, true));
                            } else if (feature instanceof CompositeSequence) {
                                datum.setReference("compositeSequence", createItem(feature, true));
                            }
                            datum.setAttribute("value", value);
                            datum.setReference("bioAssay", createItem(ba, true));
                            storeItem(datum);
                        }
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
        return item.getIdentifier();
     }


    private String checkNameValueType(Object obj, HashMap map, String key) throws Exception  {

        TypeUtil.FieldInfo info = (TypeUtil.FieldInfo) map.get(key);
        Method m = info.getGetter();
        if (m.getParameterTypes().length == 0) {
            Object value = m.invoke(obj, null);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }


    private static String nextMageIdentifier(Map mageIds, String clsName) {
        Integer nextId = (Integer) mageIds.get(clsName);
        if (nextId == null) {
            nextId = new Integer(0);
        }
        nextId = new Integer(nextId.intValue() + 1);
        mageIds.put(clsName, nextId);
        return clsName + ":FlyMine:" + nextId.intValue();
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
