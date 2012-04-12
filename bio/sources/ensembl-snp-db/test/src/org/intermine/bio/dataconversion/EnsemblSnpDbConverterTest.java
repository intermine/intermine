package org.intermine.bio.dataconversion;

import java.io.StringReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;
import org.intermine.sql.Database;
import org.intermine.util.FormattedTextParser;

import com.mockrunner.mock.jdbc.MockResultSet;

public class EnsemblSnpDbConverterTest extends ItemsTestCase
{
    private EnsemblSnpDbConverter converter;
    private MockItemWriter itemWriter;

    private static final int HUMAN = 9606;

    private List<String> variationHeader =
        new ArrayList<String>(Arrays.asList("variation_feature_id",
                                            "variation_name",
                                            "variation_id",
                                            "allele_string",
                                            "sr.name",
                                            "map_weight",
                                            "seq_region_start",
                                            "seq_region_end",
                                            "seq_region_strand",
                                            "s.name",
                                            "validation_status",
                                            "vf.consequence_type",
                                            "cdna_start",
                                            "tv.consequence_types",
                                            "pep_allele_string",
                                            "feature_stable_id",
                                            "sift_prediction",
                                            "sift_score",
                                            "polyphen_prediction",
                                            "polyphen_score"));

    private String chrName = "1"; // fixed
    private String variationRawDataFileName = "dup_consequence_9606";

    public EnsemblSnpDbConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        Database db = null;
        // A homebrew mysql database will also do
//        Database db = DatabaseFactory.getDatabase("db.ensembl.9606.variation");
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new EnsemblSnpDbConverter(db, Model.getInstanceByName("genomic"), itemWriter);
        super.setUp();
    }

    /**
     * Test case: duplicated consequences for same transcript
     * @throws Exception e
     */
    public void testProcessDuplicateConsequence() throws Exception {

        converter.setOrganism(HUMAN);
        converter.process(
                mockResultSet(variationHeader,
                        parseVariationRawData(variationRawDataFileName)),
                chrName);
        converter.close();

//        writeItemsFile(itemWriter.getItems(), "ensembl-snp-db-dup-consequence-tgt-items.xml");
//        Set<org.intermine.xml.full.Item> expected =
//            readItemSet("EnsemblComparaConverterTest_tgt.xml");

        // There should be ONLY ONE consequence
        assertEquals(1, countItemByClass(itemWriter.getItems(), "Consequence"));
        assertEquals(readItemSet("EnsemblSnpDbDupConsequence-tgt-items.xml"),
                itemWriter.getItems());
    }

    public void testDetermineType() throws Exception {
        assertEquals("snp", converter.determineType("A|G"));
        assertEquals("snp", converter.determineType("A\\G"));
        assertEquals("snp", converter.determineType("A/G"));
        assertEquals("snp", converter.determineType("A|G|C"));
        assertEquals("snp", converter.determineType("a|g"));

        assertEquals("cnv", converter.determineType("CNV"));
        assertEquals("cnv", converter.determineType("cnv"));

        assertEquals("cnv probe", converter.determineType("cnv_probe"));
        assertEquals("cnv probe", converter.determineType("CNV_PROBE"));

        assertEquals("cnv", converter.determineType("CNV"));
        assertEquals("cnv", converter.determineType("cnv"));

        assertEquals("hgmd_mutation", converter.determineType("HGMD_MUTATION"));
        assertEquals("hgmd_mutation", converter.determineType("hgmd_mutation"));

        assertEquals("het", converter.determineType("A"));
        assertEquals("het", converter.determineType("GGG"));

        assertEquals("in-del", converter.determineType("A/-"));
        assertEquals("in-del", converter.determineType("-/C"));
        assertEquals("in-del", converter.determineType("-/GAC"));
        assertEquals("in-del", converter.determineType("TN/-"));

        assertEquals("named", converter.determineType("LARGE/-"));
        assertEquals("named", converter.determineType("-/INSERTION"));
        assertEquals("named", converter.determineType("INS/-"));
        assertEquals("named", converter.determineType("-/DEL"));
        assertEquals("named", converter.determineType("-/(LARGEINSERTION)"));
        assertEquals("named", converter.determineType("-/(224 BP INSERTION)"));

        assertEquals("substitution", converter.determineType("AA/GC"));

        assertEquals("microsat", converter.determineType("(CA)14/25/26"));

        assertEquals("mixed", converter.determineType("-/A/T/TTA"));
        assertEquals("mixed", converter.determineType("C/A/-/TTA"));
    }

    /**
     *
     * @param headers
     * @param data
     * @return
     * @throws Exception
     */
    public ResultSet mockResultSet(List<String> headers, List<List<Object>> data) throws Exception {

        // validation
        if (headers == null || data == null) {
            throw new Exception("null data");
        }

        if (headers.size() != data.get(0).size()) {
            throw new Exception("column sizes are not equal");
        }

        // create a mock result set
        MockResultSet mockResultSet = new MockResultSet("aMockedResultSet");

        // add header
        for (String string : headers) {
            mockResultSet.addColumn(string);
        }

        // add data
        for (List<Object> list : data) {
            mockResultSet.addRow(list);
        }

        return mockResultSet;
    }

    private List<List<Object>> parseVariationRawData(String fileName) throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        String input = IOUtils.toString(loader.getResourceAsStream(fileName));

        Iterator<String[]> lineIter = FormattedTextParser
                .parseTabDelimitedReader(new StringReader(input));

        List<List<Object>> dataList = new ArrayList<List<Object>>();
        while (lineIter.hasNext()) {
            String[] line = lineIter.next();
            List<Object> listOfLine = new ArrayList<Object>();

            listOfLine.add(Integer.parseInt(line[0]));
            listOfLine.add(line[1]);
            listOfLine.add(Integer.parseInt(line[2]));
            listOfLine.add(line[3]);
            listOfLine.add(line[4]);
            listOfLine.add(Integer.parseInt(line[5]));
            listOfLine.add(Integer.parseInt(line[6]));
            listOfLine.add(Integer.parseInt(line[7]));
            listOfLine.add(Integer.parseInt(line[8]));
            listOfLine.add(line[9]);
            listOfLine.add(line[10]);
            listOfLine.add(line[11]);
            listOfLine.add(Integer.parseInt(line[12]));
            listOfLine.add(line[13]);
            listOfLine.add(line[14]);
            listOfLine.add(line[15]);
            listOfLine.add(line[16]);
            listOfLine.add(line[17]);
            listOfLine.add(line[18]);
            listOfLine.add(line[19]);

            dataList.add(listOfLine);
        }

        return dataList;
    }
}
