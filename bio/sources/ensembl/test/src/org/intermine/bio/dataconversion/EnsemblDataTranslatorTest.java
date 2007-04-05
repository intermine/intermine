package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;


public class EnsemblDataTranslatorTest extends DataTranslatorTestCase {

    protected static final Logger LOG=Logger.getLogger(EnsemblDataTranslatorTest.class);
    private String tgtNs = "http://www.flymine.org/model/genomic#";
    private Properties ensemblProperties;

    public EnsemblDataTranslatorTest(String arg) throws Exception {
        super(arg, "osw.bio-fulldata-test");
        ensemblProperties = getEnsemblProperties();
    }

    public void setUp() throws Exception {
        super.setUp();
        //InterMineModelParser parser = new InterMineModelParser();
        srcModel = Model.getInstanceByName("ensembl");
    }

    public void testTranslate() throws Exception {
        Map itemMap = writeItems(getSrcItems());

        EnsemblDataTranslator translator = new EnsemblDataTranslator(
                new MockItemReader(itemMap), mapping, srcModel, getTargetModel(tgtNs), ensemblProperties, "AGP");

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        // uncomment to write out a new target items file
        //writeItemsFile(tgtIw.getItems(), "ensembl-tgt-items.xml");

        assertEquals(readItemSet("EnsemblDataTranslatorFunctionalTest_tgt.xml"), tgtIw.getItems());
    }

    protected Collection getSrcItems() throws Exception {
        return readItemSet("EnsemblDataTranslatorFunctionalTest_src.xml");
    }

    protected String getModelName() {
        return "genomic";
    }

    protected String getSrcModelName() {
        return "ensembl";
    }

    private Properties getEnsemblProperties() throws IOException {
        Properties ensemblProps = new Properties();
        InputStream epis = getClass().getClassLoader().getResourceAsStream("ensembl_config.properties");
        ensemblProps.load(epis);
        return ensemblProps;
    }
}

