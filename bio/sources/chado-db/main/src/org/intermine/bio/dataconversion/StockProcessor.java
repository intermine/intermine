package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.bio.dataconversion.ChadoSequenceProcessor.FeatureData;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * A ChadoProcessor for the chado stock module.
 * @author Kim Rutherford
 */
public class StockProcessor extends ChadoProcessor
{
    private static final Logger LOG = Logger.getLogger(ChadoSequenceProcessor.class);
    private Map<String, Item> stockItems = new HashMap<String, Item>();

    /**
     * Create a new ChadoProcessor
     * @param chadoDBConverter the Parent ChadoDBConverter
     */
    public StockProcessor(ChadoDBConverter chadoDBConverter) {
        super(chadoDBConverter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Connection connection) throws Exception {
        processSocks(connection);
    }

    /**
     * Process the stocks and genotypes tables in a chado database
     * @param connection
     */
    private void processSocks(Connection connection)
        throws SQLException, ObjectStoreException {
        Map<Integer, FeatureData> features = getFeatures();

        ResultSet res = getStocksResultSet(connection);
        int count = 0;
        Integer lastFeatureId = null;
        List<Item> stocks = new ArrayList<Item>();
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            if (!features.containsKey(featureId)) {
                // probably an allele of an unlocated genes
                continue;
            }

            String stockUniqueName = res.getString("stock_uniquename");
            String stockDescription = res.getString("stock_description");
            String stockCenterUniquename = res.getString("stock_center_uniquename");
            String stockType = res.getString("stock_type_name");

            Item stock = makeStock(stockUniqueName, stockDescription, stockType,
                                   stockCenterUniquename);

            stocks.add(stock);
            if (lastFeatureId != null && !featureId.equals(lastFeatureId)) {
                storeStocks(features, lastFeatureId, stocks);
                stocks = new ArrayList<Item>();
            }
            lastFeatureId = featureId;
        }
        if (lastFeatureId != null) {
            storeStocks(features, lastFeatureId, stocks);
        }
        LOG.info("created " + count + " stocks");
        res.close();
    }

    private Map<Integer, FeatureData> getFeatures() {
        Class<ChadoSequenceProcessor> seqProcessorClass = ChadoSequenceProcessor.class;
        ChadoSequenceProcessor sequenceProcessor =
            (ChadoSequenceProcessor) getChadoDBConverter().findProcessor(seqProcessorClass);

        Map<Integer, FeatureData> features = sequenceProcessor.getFeatureMap();
        return features;
    }

    private Item makeStock(String uniqueName, String description, String stockType,
                           String stockCenterUniqueName) throws ObjectStoreException {
        if (stockItems.containsKey(uniqueName)) {
            return stockItems.get(uniqueName);
        } else {
            Item stock = getChadoDBConverter().createItem("Stock");
            stock.setAttribute("primaryIdentifier", uniqueName);
            stock.setAttribute("secondaryIdentifier", description);
            stock.setAttribute("type", stockType);
            stock.setAttribute("stockCenter", stockCenterUniqueName);
            getChadoDBConverter().store(stock);
            stockItems.put(uniqueName, stock);
            return stock;
        }
    }

    private void storeStocks(Map<Integer, FeatureData> features, Integer lastFeatureId,
                             List<Item> stocks) throws ObjectStoreException {
        FeatureData featureData = features.get(lastFeatureId);
        if (featureData == null) {
            throw new RuntimeException("can't find feature data for: " + lastFeatureId);
        }
        Integer intermineObjectId = featureData.getIntermineObjectId();
        ReferenceList referenceList = new ReferenceList();
        referenceList.setName("stocks");
        for (Item stock: stocks) {
            referenceList.addRefId(stock.getIdentifier());
        }
        getChadoDBConverter().store(referenceList, intermineObjectId);
    }

    /**
     * Return the interesting rows from the features table.
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getStocksResultSet(Connection connection)
        throws SQLException {
        String query =
             "SELECT feature.feature_id, stock.uniquename AS stock_uniquename, "
            + "      stock.description AS stock_description, type_cvterm.name AS stock_type_name, "
            + "      (SELECT stockcollection.uniquename "
            + "         FROM stockcollection, stockcollection_stock join_table "
            + "        WHERE stockcollection.stockcollection_id = join_table.stockcollection_id "
            + "          AND join_table.stock_id = stock.stock_id) "
            + "       AS stock_center_uniquename "
            + " FROM stock_genotype, feature, stock, feature_genotype, cvterm type_cvterm "
            + "WHERE stock.stock_id = stock_genotype.stock_id "
            + "AND feature_genotype.feature_id = feature.feature_id "
            + "AND feature_genotype.genotype_id = stock_genotype.genotype_id "
            + "AND feature.uniquename LIKE 'FBal%' "
            + "AND stock.type_id = type_cvterm.cvterm_id "
            + "ORDER BY feature.feature_id";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
}
