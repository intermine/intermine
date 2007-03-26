package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.Map;
import java.util.List;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * Specific behaviour for Arbeitman et al Drosophila timecourse experiment.
 * 1) reset primary characteristic of assay with a clearer description of
 * stage and time point.  Order assays by stage and time point.
 *
 * @author Richard Smith
 */
public class ArbeitmanAssayHandler extends DefaultAssayHandler
{

    /**
     * @see DefaultAssayHandler
     */
    public ArbeitmanAssayHandler(MageDataTranslator translator) {
        super(translator);
    }

    /**
     * For Arbeitman assays we want to create a new Sample characteristic to simplify
     * developmental stage information.  Also set the primaryCharacteristic to be
     * this plus the time into the stage.
     * type="stage" value="Embryo|Larvae|Metamorphosis|Adult
     * @param assay a particular assay
     */
    public void process(Item assay) {
        Item sample = null;
        Iterator sampleIter = ((List) translator.assayToSamples
                               .get(assay.getIdentifier())).iterator();
        while (sampleIter.hasNext()) {
            Item item = (Item) translator.samplesById.get((String) sampleIter.next());
            if (!(item.getAttribute("primaryCharacteristic").getValue().equals("Reference"))) {
                sample = item;
            }
        }

        // reset primary characteristic (is a hack, should be set correctly in first place)
        Summary summary = findCharacteristics(assay);
        String stageStr = summary.stage;
        // adult data is duplicated for male and female, also final time point in
        // in pupal but makes graph look little confusing if split.
        if (summary.sex != null && summary.stage.equalsIgnoreCase("adult")) {
            stageStr = stageStr + " " + summary.sex;
        }
        sample.setAttribute("primaryCharacteristicType", "stage");
        sample.setAttribute("primaryCharacteristic", stageStr + " - " + summary.age
                            + " " + summary.unit);
    }

    private Summary findCharacteristics(Item assay) {
        // TODO this method could cache Summary
        Item sample = null;
        Iterator sampleIter = ((List) translator.assayToSamples
                               .get(assay.getIdentifier())).iterator();
        while (sampleIter.hasNext()) {
            Item item = (Item) translator.samplesById.get((String) sampleIter.next());
            if (!(item.getAttribute("primaryCharacteristic").getValue().equals("Reference"))) {
                sample = item;
            }
        }

        String ageStr = null, unit = null, stageStr = null, sex = null;

        Map chars = (Map) translator.sampleToChars.get(sample.getIdentifier());
        if (chars != null) {
            Iterator charIter = chars.entrySet().iterator();
            while (charIter.hasNext()) {
                Map.Entry entry = (Map.Entry) charIter.next();
                String type = (String) entry.getKey();
                String value = (String) entry.getValue();
                if (type.equalsIgnoreCase("age")) {
                    ageStr = value;
                } else if (type.equalsIgnoreCase("timeunit")) {
                    unit = value;
                } else if (type.equalsIgnoreCase("developmentalstage")) {
                    stageStr = value;
                } else if (type.equalsIgnoreCase("sex")) {
                    sex = value;
                }
            }
        }

        if (ageStr == null || unit == null || stageStr == null) {
            throw new IllegalArgumentException("Unable to find enough information about assay: "
                                               + assay.getAttribute("name").getValue()
                                               + ",  ageStr = " + ageStr
                                               + ", unit = " + unit
                                               + ", stage = " + stageStr);
        }

        // four broad stages of development, these correspond to graphs
        // on Kevin White's Yale site presenting this data.
        String stage;
        if (stageStr.indexOf("embryonic") >= 0) {
            stage = "Embryo";
        } else if (stageStr.indexOf("larval") >= 0) {
            stage = "Larvae";
        } else if (stageStr.indexOf("pupal") >= 0) {
            stage = "Metamorphosis";
        } else if (stageStr.indexOf("adult") >= 0) {
            stage = "Adult";
        } else {
            throw new IllegalArgumentException("Unable to work out stage for assay: "
                                               + assay.getAttribute("name").getValue()
                                               + " stageStr was: " + stageStr);
        }

        return new Summary(stage, ageStr, unit, sex);
    }


    /**
     * Order assays according to time points within four broad stages of
     * development.  This needs to be inferred from several sample characteristics.
     * @param assay the ass in question
     * @return a Double that defines order of this assay in experiment
     * @throws ObjectStoreException if a problem accessing database
     */
    public Object getAssayOrderable(Item assay) throws ObjectStoreException {
        Summary summary = findCharacteristics(assay);

        // some ages have the format 'x To y' we want to average x and y
        double age;
        String ageStr = summary.age.toLowerCase();
        if (ageStr.indexOf("to") > 0) {
            int first = ageStr.indexOf(' ');
            double val1 = Double.parseDouble(ageStr.substring(0, first));
            double val2 = Double.parseDouble(ageStr.substring(ageStr.indexOf(' ', first + 1)));
            age = (val1 + val2) / 2.0;
        } else {
            age = Double.parseDouble(ageStr);
        }

        // convert all ages into hours
        if (summary.unit.equalsIgnoreCase("days")) {
            age *= 24.0;
        }

        // avoid multiplying by zero
        age += 1.0;

        // times are given from the beginning of the stage rather than an
        // absoulute value.  Need to adjust to order of stages.
        // stages:  embryonic
        //          larval
        //          metamorphosis = prepupal and pupal
        //          adult
        double embryo = 1.0;
        double larvae = 1000000.0;
        double metam = 1000000000.0;
        double adult = 1000000000000000.0;
        String stage = summary.stage;
        if (stage.equals("Embryo")) {
            age = age * embryo;
        } else if (stage.equals("Larvae")) {
            age = age * larvae;
        } else if (stage.equals("Metamorphosis")) {
            age = age * metam;
        } else if (stage.equals("Adult")) {
            age = age * adult;
        } else {
            throw new IllegalArgumentException("Unable to work out stage for assay: "
                                               + assay.getAttribute("name").getValue());
        }
        return new Double(age);
    }


    private class Summary
    {
        protected String stage, age, unit, sex;

        public Summary(String stage, String age, String unit, String sex) {
            this.stage = stage;
            this.age = age;
            this.unit = unit;
            this.sex = sex;
        }

        public String toString() {
            return "stage = " + stage + " sex = " + sex + ", age = "
                + age + ", unit = " + unit;
        }
    }
}
