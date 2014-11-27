package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Read ArrayExpress .json files retrieved from web service.
 * @author Richard Smith
 */
public class ArrayexpressAtlasConverter extends BioDirectoryConverter
{
    //
    private static final String DATASET_TITLE_PREFIX = "ArrayExpress accession: ";
    private static final String DATA_SOURCE_NAME = "ArrayExpress";
    private Map<String, String> genes = new HashMap<String, String>();
    private boolean isDatasetTitleAssigned = false;
    private String datasetTitle;
    private List<String> datasets = new ArrayList<String>();

    private String taxonId = "9606";
    private static final Logger LOG = Logger.getLogger(ArrayexpressAtlasConverter.class);

    //String[] types = new String[] {"organism_part", "disease_state", "cell_type", "cell_line"};
    static String[] types = new String[] {"organism_part", "disease_state", "cell_type"};
    private static final Set<String> EXPRESSION_TYPES = new HashSet<String>(Arrays.asList(types));
    private static final String NA_EXPRESSION = "NA";
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ArrayexpressAtlasConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, null);
    }

    @Override
    public void process(File dataDir) throws Exception {
        List<File> files = readFilesInDir(dataDir);

        for (File f : files) {
            String fileName = f.getName();
            if (fileName.endsWith("json")) {
                LOG.info("Reading file: " + fileName);
                processFile(new FileReader(f));
            }
        }
    }

    private List<File> readFilesInDir(File dir) {
        List<File> files = new ArrayList<File>();
        for (File file : dir.listFiles()) {
            files.add(file);
        }
        return files;
    }

    private void processFile(Reader reader) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        bufferedReader.close();

        JSONObject json = new JSONObject(sb.toString());

        JSONArray results = json.getJSONArray("results");

        JSONObject result = results.getJSONObject(0);

        if (!isDatasetTitleAssigned) {
            JSONObject experimentInfo = result.getJSONObject("experimentInfo");
            String accession = experimentInfo.getString("accession");
            datasetTitle = DATASET_TITLE_PREFIX + accession;
            String dataSetRefId = getDataSet(datasetTitle, getDataSource(DATA_SOURCE_NAME));
            setDataSet(dataSetRefId);
            datasets.add(dataSetRefId);
            isDatasetTitleAssigned = true;
        }

        String arrayDesign = result.getString("arrayDesign");

        JSONObject geneExpressionStatistics = result.getJSONObject("geneExpressionStatistics");
        JSONObject ensemblIds =
            geneExpressionStatistics.getJSONObject(arrayDesign).getJSONObject("genes");
        for (String ensemblId : JSONObject.getNames(ensemblIds)) {
            JSONObject probes = ensemblIds.getJSONObject(ensemblId);
            for (String probeId : JSONObject.getNames(probes)) {
                JSONArray expressionResults = probes.getJSONArray(probeId);
                for (int i = 0; i < expressionResults.length(); i++) {
                    JSONObject expressionResult = expressionResults.getJSONObject(i);
                    try {
                        Item expressionItem = createItem("AtlasExpression");
                        expressionItem.setReference("gene", getGeneId(ensemblId));
                        String type = expressionResult.get("ef").toString();
                        if (!EXPRESSION_TYPES.contains(type)) {
                            continue;
                        }
                        String condition = expressionResult.get("efv").toString();

                        JSONObject stat = expressionResult.getJSONObject("stat");
                        String expression = stat.get("expression").toString();
                        if (NA_EXPRESSION.equals(expression)) {
                            continue;
                        }
                        Double pValue = stat.getDouble("pvalue");
                        if (pValue > 1) {
                            continue;
                        }

                        Double tStatistic = stat.getDouble("tstat");
                        tStatistic = round(tStatistic, 1); // round double to 1 digit

                        expressionItem.setAttribute("type", type);
                        expressionItem.setAttribute("condition", condition);
                        expressionItem.setAttribute("expression", expression);
                        expressionItem.setAttribute("pValue", pValue.toString());
                        expressionItem.setAttribute("tStatistic", tStatistic.toString());
                        expressionItem.setCollection("dataSets", datasets);
                        store(expressionItem);
                    } catch (JSONException e) {
                        LOG.warn("JSON object missing some values: "
                                + expressionResult.toString(2), e);
                    }
                }
            }
        }
    }

    private String getGeneId(String primaryIdentifier) throws ObjectStoreException {
        String geneId = genes.get(primaryIdentifier);
        if (geneId == null) {
            Item gene = createItem("Gene");
            gene.setAttribute("primaryIdentifier", primaryIdentifier);
            gene.setReference("organism", getOrganism(taxonId));
            store(gene);
            geneId = gene.getIdentifier();
            genes.put(primaryIdentifier, geneId);
        }
        return geneId;
    }

    private static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        long factor = (long) Math.pow(10, places);
        long tmp = Math.round(value * factor);
        return (double) tmp / factor;
    }
}
