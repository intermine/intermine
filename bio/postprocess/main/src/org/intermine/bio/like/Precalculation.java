package org.intermine.bio.like;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.intermine.Coordinates;
import org.intermine.api.query.MainHelper;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;

/**
 * Precalculation() is used to set up the foundation for later calculations made in Matrices().
 * It reads in the property file and the data (genes with related items).
 * It arranges this information well in matrices (1 matrix per aspect).
 *
 * @author selma
 *
 */
public class Precalculation
{
    // The gene ID is always in column zero
    private static final int SUBJECT_ID_COLUMN = 0;

    private ObjectStore os;

    /**
     * constructor
     *
     * @param os ObjectStore to get the genes with subjects
     */
    public Precalculation(ObjectStore os) {
        this.os = os;
    }

    /**
     * Get the wanted aspects from an external configuration file.
     *
     * @return a list of all aspects, which shall be calculated with their additional information
     * (how to calculate and which number to save with)
     * @throws IOException
     */
    public static Map<Coordinates, String> getProperties() throws IOException {
        Map<Coordinates, String> views = new HashMap<Coordinates, String>();

        Properties prop = new Properties();
        String configFileName = "like_config.properties";
        ClassLoader classLoader = Precalculation.class.getClassLoader();
        InputStream configStream = classLoader.getResourceAsStream(configFileName);
        prop.load(configStream);

        int queryNumber = 0;
        for (int i = 0; i < 100; i++) {
            if (prop.getProperty("query." + i + ".number") != null) {
                queryNumber += 1;
            }
            else {
                break;
            }
        }
        views.put(new Coordinates(0, 0), String.valueOf(queryNumber));

        views.put(new Coordinates(1, 0), prop.getProperty("query.all.constraint.path"));
        views.put(new Coordinates(1, 1), prop.getProperty("query.all.constraint.op"));
        views.put(new Coordinates(1, 2), prop.getProperty("query.all.constraint.value"));

        int countViews = 2;
        for (int i = 0; i < queryNumber; i++) {
            views.put(new Coordinates(countViews, 0),
                    prop.getProperty("query." + i + ".number"));
            views.put(new Coordinates(countViews, 1), prop.getProperty("query." + i + ".id"));
            views.put(new Coordinates(countViews, 2), prop.getProperty("query." + i + ".type"));

            if (prop.getProperty("query." + i + ".constraint.a.path") != null) {
                views.put(new Coordinates(countViews, 3),
                        prop.getProperty("query." + i + ".constraint.a.path"));
                views.put(new Coordinates(countViews, 4),
                        prop.getProperty("query." + i + ".constraint.a.op"));
                views.put(new Coordinates(countViews, 5),
                        prop.getProperty("query." + i + ".constraint.a.value"));
                views.put(new Coordinates(countViews, 6),
                        prop.getProperty("query." + i + ".constraint.b.path"));
                views.put(new Coordinates(countViews, 7),
                        prop.getProperty("query." + i + ".constraint.b.op"));
                views.put(new Coordinates(countViews, 8),
                        prop.getProperty("query." + i + ".constraint.b.value"));
            }

            countViews += 1;
        }
        return views;
    }

    /**
     * Fetches genes from the ObjectStore.
     * Is called for each aspect separately.
     *
     * @param views list of aspects
     * @param i gives information which aspect in the list (views) is looked at
     * @return List of Genes with their related items (e.g. pathways)
     * @throws ObjectStoreException
     */
    public List<Object> fetchDMelGenes(Map<Coordinates, String> views, int i)
        throws ObjectStoreException {
        // Build the query
        Model model = os.getModel();
        PathQuery pq = new PathQuery(model);
        String relationShip = views.get(new Coordinates(i, 1));
        String path = views.get(new Coordinates(1, 0));
        String op = views.get(new Coordinates(1, 1));
        String value = views.get(new Coordinates(1, 2));
        // add views
        pq.addViews("Gene.id", relationShip);

        // Add order by
        pq.addOrderBy("Gene.primaryIdentifier", OrderDirection.ASC);

        // Filter the results with the following constraints:
        pq.addConstraint(Constraints.eq(path, value), "A");

        if ("Gene.goAnnotation.ontologyTerm.parents.id".equals(relationShip)) {
            pq.addConstraint(Constraints.neq(views.get(new Coordinates(i, 3)),
                    views.get(new Coordinates(i, 5))), "B");
            pq.addConstraint(Constraints.eq(views.get(new Coordinates(i, 6)),
                    views.get(new Coordinates(i, 8))), "C");
//          pq.addConstraint(Constraints.neq("Gene.symbol", "*a*"), "D");
//            pq.addConstraint(Constraints.eq("Gene.symbol", "*z*"), "D");
            // Specify how these constraints should be combined.
            pq.setConstraintLogic("A and B and C");
//            pq.setConstraintLogic("A and B and C and D");
        }
//        else {
//
////          pq.addConstraint(Constraints.neq("Gene.symbol", "*a*"), "B");
//            pq.addConstraint(Constraints.eq("Gene.symbol", "*z*"), "B");
//            pq.setConstraintLogic("A and B");
//        }

        // Outer Joins
        // Show all information about these relationships if they exist, but do not require that
        // they exist.
        pq.setOuterJoinStatus(relationShip.substring(0, StringUtils.ordinalIndexOf(relationShip,
                ".", 2)), OuterJoinStatus.OUTER);

        System.out.println(pq.toXml());
        Query q = MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, new HashMap());

        return os.execute(q);
    }

    /**
    * Take a set of genes of interest and generate the matrix for the type "category".
    * Whereas the first column contains the gene IDs. The other columns comprehend the corresponding
    * information about the gene. So, one row contains one gene ID and all its related items.
    * Also, each unique related item ID has its own column.
    *
    * Is called for each aspect separately.
    *
    * @param rows a query result
    * @return map of gene object ID to list of annotations, e.g. the matrix
    */
    public Map<Coordinates, Integer> runQueryCategory(List<Object> rows) {
        Map<Coordinates, Integer> matrix = new HashMap<Coordinates, Integer>();
        int currentRow = 0;
        int highestIndex = -1;
        int latestColumn = 0;

        for (Object o: rows) {
            List<Object> row = (List<Object>) o;
            InterMineObject subject = (InterMineObject) row.get(0);
            // single annotation, e.g. pathway
            Collection<?> relatedItems = (Collection<?>) row.get(1);

            // looking for gene that we've seen before
            boolean isSaved = false;
            for (Map.Entry<Coordinates, Integer> entry : matrix.entrySet()) {
                Coordinates coordinates = entry.getKey();
                int xCoordinate = coordinates.getKey();
                int yCoordinate = coordinates.getValue();
                // if this is a gene
                if (yCoordinate == SUBJECT_ID_COLUMN) {
                    Integer subjectID = entry.getValue();
                    // gene is already in matrix
                    if (subjectID == subject.getId()) {
                        // stay on same row, don't increment
                        currentRow = xCoordinate;
                        isSaved = true;
                        break;
                    }
                }
                // if this is the biggest row number we've seen
                if (xCoordinate > highestIndex) {
                    highestIndex = xCoordinate;
                }
            }
            if (!isSaved) {
                // if new gene, increment, we are done with previous row
                currentRow = highestIndex + 1;
                matrix.put(new Coordinates(currentRow, SUBJECT_ID_COLUMN),
                        subject.getId());
            }

            // add columns after column zero
            // for this gene, relatedItems is all pathways (or protein domains, etc)
            for (Object rawRow : relatedItems) {
                List<InterMineObject> subRow = (List<InterMineObject>) rawRow;
                for (InterMineObject related: subRow) {
                    if (related != null) {
                        boolean saved = false;
                        int currentColumn = latestColumn + 1;
                        for (Map.Entry<Coordinates, Integer> entry : matrix.entrySet()) {
                            if (!saved && related.getId() == entry.getValue()) {
                                Coordinates coordinate = entry.getKey();
                                currentColumn = coordinate.getValue();
                                saved = true;
                            }
                        }
                        if (!saved) {
                            // new column so put at the end
                            latestColumn += 1;
                        }
                        matrix.put(new Coordinates(currentRow, currentColumn), related.getId());
                    }
                }
            }
        }
        return matrix;
    }

    /**
     * Take a set of genes of interest and generate the matrix for the type "count".
     *
     * @param rows a query result
     * @return map of gene object ID to list of annotations, e.g. the matrix
     */
    public Map<Coordinates, Integer> runQueryCount(List<Object> rows) {
        Map<Coordinates, Integer> matrix = new HashMap<Coordinates, Integer>();
        int currentRow = 0;
        int highestIndex = -1;
        int latestColumn = 0;

        for (Object o: rows) {
            List<Object> row = (List<Object>) o;
            InterMineObject subject = (InterMineObject) row.get(0);
            Collection<?> relatedItems = (Collection<?>) row.get(1);

            boolean isSaved = false;
            for (Map.Entry<Coordinates, Integer> entry : matrix.entrySet()) {
                if (entry.getKey().getKey() == 0) {
                    if (entry.getValue() == subject.getId()) {
                        currentRow = entry.getKey().getKey();
                        isSaved = true;
                        break;
                    }
                }
                if (entry.getKey().getKey() > highestIndex) {
                    highestIndex = entry.getKey().getKey();
                }
            }
            if (!isSaved) {
                currentRow = highestIndex + 1;
                latestColumn = 1;
                matrix.put(new Coordinates(currentRow, 0), subject.getId());
            }

            for (Object rawRow : relatedItems) {
                List<InterMineObject> subRow = (List<InterMineObject>) rawRow;
                for (InterMineObject related: subRow) {
                    if (related != null) {
                        matrix.put(new Coordinates(currentRow, latestColumn), related.getId());
                        latestColumn += 1;
                    }
                }
            }
        }
        return matrix;
    }

    /**
     * Take a set of genes of interest and generate the matrix for the type "presence".
     *
     * @param rows a query result
     * @return map of gene object ID to list of annotations, e.g. the matrix
     */
    public Map<Coordinates, Integer> runQueryPresence(List<Object> rows){
        Map<Coordinates, Integer> matrix = new HashMap<Coordinates, Integer>();
        int currentRow = 0;
        int highestIndex = -1;
        int latestColumn = 0;

        for (Object o: rows) {
            List<Object> row = (List<Object>) o;
            InterMineObject item = (InterMineObject) row.get(0);
            Collection<?> relatedItems = (Collection<?>) row.get(1);

            boolean isSaved = false;
            for (Map.Entry<Coordinates, Integer> entry : matrix.entrySet()) {
                if (entry.getKey().getValue() == 0) {
                    if (entry.getValue() == item.getId()) {
                        currentRow = entry.getKey().getKey();
                        isSaved = true;
                        break;
                    }
                }
                if (entry.getKey().getKey() > highestIndex) {
                    highestIndex = entry.getKey().getKey();
                }
            }
            if (!isSaved) {
                currentRow = highestIndex + 1;
                latestColumn = 1;
                matrix.put(new Coordinates(currentRow, 0), item.getId());
            }

            for (Object rawRow : relatedItems) {
                List<InterMineObject> subRow = (List<InterMineObject>) rawRow;
                for (InterMineObject related: subRow) {
                    if (related != null) {
                        matrix.put(new Coordinates(currentRow, latestColumn), related.getId());
                        latestColumn += 1;
                    }
                }
            }
        }
        return matrix;
    }
}
