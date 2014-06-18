package org.intermine.webservice.server.recommendations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.naming.ConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.like.request.LikeRequest;
import org.intermine.like.request.LikeService;
import org.intermine.like.request.MatrixStore;
import org.intermine.like.response.LikeResult;
import org.intermine.Coordinates;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.api.util.PathUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.web.context.InterMineContext;

/**
 * A service that returns the result of similarity calculations.
 * @author selma
 *
 */
public class SimilarityService extends JSONService
{

    private static final Logger LOG = Logger.getLogger(SimilarityService.class);

    /** there you go
     * @param im The injected parameter.
     **/
    public SimilarityService(InterMineAPI im) {
        super(im);
    }

    @Override
    public String getResultsKey() {
        return "results";
    }

    @Override
    protected void execute() throws ServiceException, ConfigurationException, IOException, ClassNotFoundException {
        LikeService engine =
                LikeService.getInstance(new OSMatrixStore(im.getObjectStore()), webProperties);

        String id = request.getParameter("id");
        if (StringUtils.isEmpty(id)) {
            throw new BadRequestException("One or more ids are required");
        }
        String[] idsStrings = id.split(",");
//        File file2 = new File(id);
//        FileInputStream f = new FileInputStream(file2);
//        ObjectInputStream s = new ObjectInputStream(f);
//        String[] idsStrings = (String[]) s.readObject();
//        s.close();

        LikeRequest request = new LikeRequest();

        getObjectIds(idsStrings, request);

        LikeResult result;
        try {
            result = engine.search(request);
        } catch (IOException e) {
            throw new ServiceException("Everything is broken");
        } catch (ClassNotFoundException e) {
            throw new ServiceException("I don't even");
        }

        int[][] totalRatingSet = result.getTotalRatingSet();
        Map<Integer, Map<Integer, Map<Integer, Integer>>> similarGenes = result.getsimilarGenes();
        Map<Integer, Map<Integer, ArrayList<Integer>>> commonItems = result.getCommonItems();

        List<Object> rets = new ArrayList<Object>();
        // get result gene Ids
//        rets.addAll(similarGenes.keySet());

     // get result gene Ids with ordered total ratings
      for (int i = 0; i < 3; i++) {
          for (int j = 0; j < 2; j++) {
              //rets.add(totalRatingSet[i][j]);
          }
      }

      Logger LOG = Logger.getLogger(SimilarityService.class);

      for (Map.Entry<Integer, Map<Integer, ArrayList<Integer>>> entry : commonItems.entrySet()) {

        // Get ID of the next similar gene
        Map<String, Object> similarGeneDetails = getObjectDetails(entry.getKey());
        similarGeneDetails.put("id", entry.getKey());

        // Get the score for this gene:
        Map<Integer, Map<Integer, Integer>> val = similarGenes.get(entry.getKey());
        Map.Entry<Integer, Map<Integer, Integer>> firstEntry = val.entrySet().iterator().next();




        similarGeneDetails.put("totalscore", firstEntry.getKey());





        Object singleGene = similarGenes.get(entry.getKey());

        // Now get the original search genes:
        Map<Integer, ArrayList<Integer>> originalSearchGenes = entry.getValue();

        // Get the details of the original search genes:

        ArrayList originalGenesCollection = new ArrayList();

        for (Map.Entry<Integer, ArrayList<Integer>> originalGene : originalSearchGenes.entrySet()) {

            Map<String, Object> originalGeneDetails = getObjectDetails(originalGene.getKey());
            originalGeneDetails.put("id", originalGene.getKey());

            // Now loop through the items that make them common
            ArrayList commonItemsCollection = new ArrayList();
            ArrayList<Integer> commonItemIds = originalGene.getValue();
            for (Integer commonItemId : commonItemIds) {
                Map<String, Object> commonItemDetails = getObjectDetails(commonItemId);
                commonItemDetails.put("id", commonItemId);
                commonItemsCollection.add(commonItemDetails);

            }

            originalGeneDetails.put("commonItems", commonItemsCollection);
            originalGenesCollection.add(originalGeneDetails);
            
        }


        similarGeneDetails.put("originalGenes", originalGenesCollection);

        rets.add(similarGeneDetails);

      }

      // rets = new ArrayList<Object>();
      // rets.add(similarGenes);

        // transmit object.
        addResultItem(rets, false);
    }

    private void getObjectIds(String[] idsStrings, LikeRequest request) {
        ObjectStore os = im.getObjectStore();
        for (int i = 0; i < idsStrings.length; i++) {
            Integer givenId = Integer.parseInt(idsStrings[i]);
            try { // Check object existence.
                if (os.getObjectById(givenId) == null) {
                    throw new BadRequestException("The id " + givenId + " is not in the db.");
                }
            } catch (ObjectStoreException e) {
                throw new ServiceException("Database error", e);
            }
            request.addID(givenId);
        }
    }

    //////////////////////////

    private Map<String, Object> getObjectDetails(Integer objId) {
        InterMineObject imo;
        if (objId == null) throw new IllegalArgumentException("obj cannot be null");
        try {
            imo = im.getObjectStore().getObjectById(objId);
        } catch (ObjectStoreException e) {
            throw new IllegalStateException("Could not retrieve object reported as match", e);
        }
        return getObjectDetails(imo);
        //return imo;
    }

    private Map<String, Object> getObjectDetails(InterMineObject imo) {
        WebConfig webConfig = InterMineContext.getWebConfig();
        Model m = im.getModel();
        Map<String, Object> objectDetails = new HashMap<String, Object>();
        String className = DynamicUtil.getSimpleClassName(imo.getClass());
        ClassDescriptor cd = m.getClassDescriptorByName(className);
        objectDetails.put("class", cd.getUnqualifiedName());
        for (FieldConfig fc : FieldConfigHelper.getClassFieldConfigs(webConfig, cd)) {
            try {
                Path p = new Path(m, cd.getUnqualifiedName() + "." + fc.getFieldExpr());
                if (p.endIsAttribute() && fc.getShowInSummary()) {
                    objectDetails.put(
                            p.getNoConstraintsString().replaceAll("^[^.]*\\.", ""),
                            PathUtil.resolvePath(p, imo));
                }
            } catch (PathException e) {
                LOG.error("Configuration error", e);
            }
        }
        return objectDetails;
    }

    //////////////////////////////

    private static class OSMatrixStore implements MatrixStore
    {
        private ObjectStore os;

        private Map<Coordinates, Integer> normMat;
        private Map<Coordinates, ArrayList<Integer>> commonMat;

        private static final Logger LOG = Logger.getLogger(OSMatrixStore.class);

        public OSMatrixStore(ObjectStore os) {
            this.os = os;
        }

        public List<Integer> getSimilarityRow(String aspectNumber, int index) {
            List<Integer> similarityRow = new ArrayList<Integer>();
            // not used atm

            return similarityRow;
        }

        public List<List<Integer>> getCommonItemsRow(String aspectNumber, int index) {
            List<List<Integer>> similarityRow = new ArrayList<List<Integer>>();
            // not used atm

            return similarityRow;
        }

        public Map<Coordinates, Integer> getSimilarityMatrix(String aspectNumber,
                String geneId) {

            long time = System.currentTimeMillis();
            LOG.debug("Attempting to restore search index from database...");
            if (os instanceof ObjectStoreInterMineImpl) {
                Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
                try {
                    InputStream is = MetadataManager.readLargeBinary(db, aspectNumber
                            + MetadataManager.LIKE_SIMILARITY_MATRIX + geneId);

                    if (is != null) {
                        GZIPInputStream gzipInput = new GZIPInputStream(is);
                        ObjectInputStream objectInput = new ObjectInputStream(gzipInput);

                        try {
                            Object object = objectInput.readObject();

                            if (object instanceof Map<?, ?>) {
                                normMat = (Map<Coordinates, Integer>) object;

                                LOG.info("Successfully restored search index information"
                                        + " from database in " + (System.currentTimeMillis() - time)
                                        + " ms");
                                LOG.debug("Index: " + normMat);
                            } else {
                                LOG.warn("Object from DB has wrong class:"
                                        + object.getClass().getName());
                            }
                        } finally {
                            objectInput.close();
                            gzipInput.close();
                        }
                    } else {
//                        LOG.warn("IS is null");
                        throw new ConfigurationException("IS is null");
                    }
                } catch (ConfigurationException e) {
                    LOG.error("IS is null?");
                } catch (ClassNotFoundException e) {
                    LOG.error("Could not load similarity matrix", e);
                } catch (SQLException e) {
                    LOG.error("Could not load similarity matrix", e);
                } catch (IOException e) {
                    LOG.error("Could not load similarity matrix", e);
                }
            } else {
                LOG.error("ObjectStore is of wrong type!");
            }
            return normMat;
        }

        public Map<Coordinates, ArrayList<Integer>> getCommonItemsMatrix(
                String aspectNumber, String geneId) {
            long time = System.currentTimeMillis();
            LOG.debug("Attempting to restore search index from database...");
            if (os instanceof ObjectStoreInterMineImpl) {
                Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
                try {
                    InputStream is = MetadataManager.readLargeBinary(db, aspectNumber
                            + MetadataManager.LIKE_COMMON_MATRIX + geneId);
                    if (is != null) {
                        GZIPInputStream gzipInput = new GZIPInputStream(is);
                        ObjectInputStream objectInput = new ObjectInputStream(gzipInput);

                        try {
                            Object object = objectInput.readObject();

                            if (object instanceof Map<?, ?>) {
                                commonMat = (Map<Coordinates, ArrayList<Integer>>) object;

                                LOG.info("Successfully restored search index information"
                                        + " from database in " + (System.currentTimeMillis() - time)
                                        + " ms");
                                LOG.debug("Index: " + commonMat);
                            } else {
                                LOG.warn("Object from DB has wrong class:"
                                        + object.getClass().getName());
                            }
                        } finally {
                            objectInput.close();
                            gzipInput.close();
                        }
                    } else {
                        LOG.warn("IS is null");
                    }
                } catch (ClassNotFoundException e) {
                    LOG.error("Could not load search index", e);
                } catch (SQLException e) {
                    LOG.error("Could not load search index", e);
                } catch (IOException e) {
                    LOG.error("Could not load search index", e);
                }
            } else {
                LOG.error("ObjectStore is of wrong type!");
            }
            return commonMat;
        }
    }

}
