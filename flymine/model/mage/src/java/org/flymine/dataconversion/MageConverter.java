package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
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
import java.util.LinkedHashSet;
import java.lang.reflect.Method;
import java.util.StringTokenizer;
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

import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;
import org.intermine.model.fulldata.ReferenceList;
import org.intermine.util.TypeUtil;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.FileConverter;

import org.apache.log4j.Logger;

/**
 * Convert MAGE-ML to InterMine Full Data Xml via MAGE-OM objects.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class MageConverter extends FileConverter
{
    private static final Logger LOG = Logger.getLogger(MageConverter.class);

    protected static final String MAGE_NS = "http://www.flymine.org/model/mage#";

    protected HashMap seenMap;
    protected HashSet dataItems;
    protected HashMap padIdentifiers = new HashMap();
    protected HashMap featureIdentifiers = new HashMap();
    protected int id = 0;

    /**
     * @see FileConverter#FileConverter
     */
    public MageConverter(ItemWriter writer) {
        super(writer);
    }

    /**
     * @see FileConverter#process
     */
    public void process(Reader reader) throws Exception {
        seenMap = new LinkedHashMap();
        dataItems = new LinkedHashSet();
        id = 0;
        File f = new File("build/tmp/mageconvert.xml");
        try {
            Writer fileWriter = new FileWriter(f);
            int c;
            while ((c = reader.read()) > 0) {
                fileWriter.write(c);
            }
            fileWriter.close();
            MAGEReader mageReader = new MAGEReader(f.getPath());
            seenMap = new LinkedHashMap();
            createItem(mageReader.getMAGEobj());
        } finally {
            f.delete();
        }
        writer.storeAll(seenMap.values());
        writer.storeAll(dataItems);
        writer.storeAll(padIdentifiers.values());
        writer.storeAll(featureIdentifiers.values());
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

        Class cls = obj.getClass();
        String className = TypeUtil.unqualifiedName(cls.getName());

        Item item = new Item();
        item.setClassName(MAGE_NS + className);
        item.setImplementations("");

        if (!cls.getName().equals("org.biomage.Common.MAGEJava")
            && !className.endsWith("_package")) {
            if (!cls.getName().equals("org.biomage.ArrayDesign.PhysicalArrayDesign") &&
                !cls.getName().equals("org.biomage.DesignElement.Feature")) {
                item.setIdentifier(alias(className) + "_" + (id++));
                seenMap.put(obj, item);
            }
        }

        for (Iterator i = TypeUtil.getFieldInfos(cls).values().iterator(); i.hasNext();) {
            TypeUtil.FieldInfo info = (TypeUtil.FieldInfo) i.next();
            Method m = info.getGetter();
            if (m.getParameterTypes().length == 0) {
                Object value = m.invoke(obj, null);
                if (value != null) {
                    if (Collection.class.isAssignableFrom(m.getReturnType())) {
                        // collection
                        ReferenceList refs = new ReferenceList();
                        refs.setName(info.getName());
                        StringBuffer sb = new StringBuffer();
                        for (Iterator j = ((Collection) value).iterator(); j.hasNext();) {
                            sb.append(createItem(j.next()).getIdentifier() + " ");
                        }
                        if (sb.length() > 0) {
                            refs.setRefIds(sb.toString().trim());
                            item.addCollections(refs);
                        }
                    } else if (m.getReturnType().getName().startsWith("org.biomage")) {
                        if (m.getReturnType().getName().startsWith(cls.getName() + "$")) {
                            //attribute
                            Attribute attr = new Attribute();
                            attr.setName(info.getName());
                            Method getName = value.getClass().getMethod("getName", null);
                            String attValue = (String) getName.invoke(value, null);
                            if (attValue != null) {
                                attr.setValue(escapeQuotes(attValue));
                                item.addAttributes(attr);
                            } else {
                                LOG.warn("Null value for attribute " + info.getName() + " in Item "
                                         + item.getClassName() + " (" + item.getIdentifier() + ")");
                            }
                        } else {
                            //reference
                            Reference ref = new Reference();
                            ref.setName(info.getName());
                            ref.setRefId(createItem(value).getIdentifier());
                            item.addReferences(ref);
                        }
                    } else if (!info.getName().equals("identifier")) {
                        // attribute
                        Attribute attr = new Attribute();
                        attr.setName(info.getName());
                        attr.setValue(escapeQuotes(value.toString()));
                        item.addAttributes(attr);
                        // TODO handle dates?
                    }
                }
            }
        }



        if (className.equals("PhysicalArrayDesign")) {
            // if item does not have name set it is a placeholder
            // do not want to store
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

        } else if (className.equals("Feature")) {
            // as above but where check pad.getName() check
            // if feature.getZone()... from mage object
            Feature feature = (Feature) obj;
            String fid = feature.getIdentifier();
            Item fItem = (Item) featureIdentifiers.get(fid);

            if (fItem == null) {
                item.setIdentifier(alias(className) + "_" + (id++));
                featureIdentifiers.put(fid, item);
            } else if ( feature.getZone() != null) {
                item.setIdentifier(fItem.getIdentifier());
                featureIdentifiers.put(fid, item);
            } else {
                item = fItem;
            }
        } else if (className.equals("MeasuredBioAssayData")
            || className.equals("DerivedBioAssayData")) {
            boolean normalised = false;
            if (className.equals("DerivedBioAssayData")) {
                normalised = true;
            }
            BioAssayData bad = (BioAssayData) obj;
            String fileName = ((BioDataCube) bad.getBioDataValues()).getDataExternal()
                .getFilenameURI();
            List colTypes = bad.getQuantitationTypeDimension().getQuantitationTypes();
            List rowNames = ((FeatureDimension) bad.getDesignElementDimension())
                .getContainedFeatures();
            BufferedReader br = new BufferedReader(new
                InputStreamReader(getClass().getClassLoader().getResourceAsStream(fileName)));
            Item bdt = new Item();
            bdt.setClassName(MAGE_NS + "BioDataTuples");
            bdt.setImplementations("");
            bdt.setIdentifier(alias(className) + "_" + (id++));

            ReferenceList rl = new ReferenceList();
            rl.setName("bioAssayTupleData");
            StringBuffer sb = new StringBuffer();
            for (Iterator i = rowNames.iterator(); i.hasNext();) {
                Feature f = (Feature) i.next();
                String s = br.readLine();
                StringTokenizer st = new StringTokenizer(s, "\t");
                for (Iterator j = colTypes.iterator(); j.hasNext();) {
                    QuantitationType qt = (QuantitationType) j.next();
                    String value = st.nextToken();
                    Item data = new Item();
                    data.setClassName(MAGE_NS + "BioAssayDatum");
                    data.setImplementations("");
                    data.setIdentifier(alias(className) + "_" + (id++));

                    sb.append(data.getIdentifier() + " ");

                    //reference
                    Reference ref = new Reference();
                    ref.setName("quantitationType");
                    ref.setRefId(createItem(qt).getIdentifier());
                    data.addReferences(ref);

                    ref = new Reference();
                    ref.setName("designElement");
                    ref.setRefId(f.getIdentifier());
                    // ref.setRefId(createItem(f).getIdentifier());
                    data.addReferences(ref);

                    Attribute attr = new Attribute();
                    attr.setName("value");
                    attr.setValue(value);
                    data.addAttributes(attr);

                    // add normalised attribute - not actually in MAGE model,
                    // will be removed by MageDataTranslator before validating
                    // against model
                    Attribute norm = new Attribute();
                    norm.setName("normalised");
                    norm.setValue(normalised ? "true" : "false");
                    data.addAttributes(norm);

                    //should create reference for bioAssay
                    //ref = new Reference();
                    //ref.setName("bioAssay");
                    //ref.setRefId(createItem(obj).getIdentifier());
                    //data.addReferences(ref);
                    dataItems.add(data);

                }
            }
            rl.setRefIds(sb.toString().trim());
            bdt.addCollections(rl);
            dataItems.add(bdt);

            Reference bdtRef = new Reference();
            bdtRef.setName("bioDataValues");
            bdtRef.setRefId(bdt.getIdentifier());

            Set newRefs = new HashSet();
            Iterator refIter = item.getReferences().iterator();
            while (refIter.hasNext()) {
                Reference ref = (Reference) refIter.next();
                if (!ref.getName().equals("bioDataValues")) {
                    newRefs.add(ref);
                }
            }
            newRefs.add(bdtRef);
            item.setReferences(newRefs);
        }
        return item;
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
