package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ResultCell;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.FastPathObject;
import org.intermine.objectstore.query.PathExpressionField;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.webservice.server.core.DisjointRecursiveList.Eacher;
import org.intermine.webservice.server.exceptions.NotImplementedException;

/**
 * A class for iterating over rows of results returned from the object store in a way that is very
 * similar in shape to the raw results, but more tractable and more easily serialised and handled in
 * other places.
 * @author Alex Kalderimis
 */
public class TableRowIterator implements
        Iterator<List<Either<ResultCell, SubTable>>>,
        Iterable<List<Either<ResultCell, SubTable>>>
{

    private static final String EXPECTED_SUBTABLE =
            "Expected this result element to be a subtable of the form %s, but was %s";

    private static final String NOT_INTERMINE_OBJECT =
            "Table row iteration only supported over InterMine Objects, not %s instances. Got %s";

    /** Apache Log4J Logger **/
    private static final Logger LOG = Logger.getLogger(TableRowIterator.class);

    /* Initialisation variables */

    /** The path-query as provided by the user. **/
    private final PathQuery pathQuery;

    /**
     * Object-store version of this query
     * You may well ask "Why not just read the query from the results parameter
     * via {@link Results#getQuery()}?" Indeed, that would make a lot of sense, and
     * was the initial approach - but:
     * <ul>
     *   <li>The {@link TableRowIterator#determineLevels(DisjointRecursiveList, Map)} routine
     *       requires equivalence testing between QueryClasses and other
     *       QuerySelectables, and they only allow object identity testing.</li>
     *   <li>The query object returned from {@link Results#getQuery()} is
     *       the <em>original</em> query used, and not the same as
     *       the one passed to {@link ObjectStore#execute()} if the query hits
     *       the results cache.</li>
     *   <li>Welcome to the world of leaky abstractions!</li>
     *   <li>Also, in other news, the world sucks.</li>
     * </ul>
     * **/
    private final Query query;
    /** The results returned from the object-store. MAY BE FROM CACHE **/
    private final Results results;
    private final Map<String, QuerySelectable> nodeForPath = new HashMap<String, QuerySelectable>();
    private final List<Path> paths = new ArrayList<Path>();
    private final Page page;
    private final InterMineAPI im; // May be null.

    /**
     * The shape of the results. Each list of paths represents one of the objects returned from
     * the object-store and the fields that we are interested in reading from it. As well as
     * individual objects, the results may also contain nested result sets, which themselves
     * contain objects returned from the object store. This recursive data-structure is different in
     * precise shape for each query, and so this object provides a
     * template for returning a row to the user.
     */
    private DisjointRecursiveList<List<Path>> resultsShape;

    /**
     * The view as it would have been if we didn't have to make any changes.
     */
    private final List<Path> effectiveView = new ArrayList<Path>();

    /**
     * Counter for keeping track of which row we are on.
     */
    private int counter = 0;
    private Path root;
    private Iterator<Object> osIter;
    private Map<DisjointRecursiveList<List<Path>>, Path> levels;

    /** The mechanism for mapping an element of results to the path it logically represents **/
    private static final EitherVisitor<ResultCell, SubTable, Path> PATH_GETTER
        = new EitherVisitor<ResultCell, SubTable, Path>() {
            @Override public Path visitLeft(ResultCell a) {
                return a.getPath();
            }
            @Override public Path visitRight(SubTable b) {
                return b.getJoinPath();
            }
        };

    /**
     * The comparator for reordering the results prior to being returned,
     * making sure they are as close as possible to the original view order.
     */
    private Comparator<Either<ResultCell, SubTable>> reorderer;

    /**
     * The comparator for reordering the view list prior to flattening,
     * making sure the new outer-joined grouped view is still in the
     * preferred view order as defined by the path-query.
     */
    private Comparator<Either<Path, DisjointRecursiveList<Path>>> pathReorderer;

    /**
     * Constructor.
     * @param pathQuery The path-query these results represent an answer to.
     * @param query The object store query this path-query compiles to.
     * @param results The object-store results.
     * @param nodeForPath The map from selectable to the path it represents.
     * @param page The section of the results required.
     * @param im A reference to the API (MAY BE NULL!).
     */
    public TableRowIterator(
            PathQuery pathQuery,
            Query query,
            Results results,
            Map<String, QuerySelectable> nodeForPath,
            Page page,
            InterMineAPI im) {
        this.page = page;
        this.query = query;
        this.pathQuery = pathQuery;
        this.results = results;
        this.nodeForPath.putAll(nodeForPath);
        this.im = im; // Watch out, may be null!

        try {
            init();
        } catch (PathException e) {
            throw new RuntimeException("Failed to initialise", e);
        }
    }

    /**
     * Perform initialisation that depends on computation involving values provided in the
     * constructor.
     * @throws PathException If the world has gone completely bonkers.
     */
    private void init() throws PathException {

        osIter = results.iteratorFrom(page.getStart());
        counter = page.getStart();
        root = pathQuery.makePath(pathQuery.getRootClass());

        for (String view: pathQuery.getView()) {
            paths.add(pathQuery.makePath(view));
        }

        ColumnConversionInput cci = new ColumnConversionInput();
        cci.paths = paths;
        cci.pathToQueryNode = nodeForPath;
        cci.select = new ArrayList<QuerySelectable>(query.getSelect());

        resultsShape = determineResultShape(cci);
        levels = getLevelMap(resultsShape);
        levels.put(resultsShape, root);

        reorderer = new Comparator<Either<ResultCell, SubTable>>() {

            private Integer pathToViewIndex(Path path) {
                String pString = path.toStringNoConstraints(), view = pString;

                if (!path.endIsAttribute()) {
                    for (String v: pathQuery.getView()) {
                        if (v.startsWith(pString)) {
                            view = v;
                            break;
                        }
                    }
                }
                return Integer.valueOf(pathQuery.getView().indexOf(view));
            }

            @Override
            public int compare(Either<ResultCell, SubTable> o1, Either<ResultCell, SubTable> o2) {
                Path path1 = o1.accept(PATH_GETTER);
                Path path2 = o2.accept(PATH_GETTER);
                return pathToViewIndex(path1).compareTo(pathToViewIndex(path2));
            }
        };
        pathReorderer = new Comparator<Either<Path, DisjointRecursiveList<Path>>>() {
            private EitherVisitor<Path, DisjointRecursiveList<Path>, Integer> toViewIndex =
                    new EitherVisitor<Path, DisjointRecursiveList<Path>, Integer>() {
                        @Override
                        public Integer visitLeft(Path a) {
                            return pathQuery.getView().indexOf(a.toStringNoConstraints());
                        }
                        @Override
                        public Integer visitRight(DisjointRecursiveList<Path> b) {
                            return Integer.valueOf(
                                    pathQuery.getView().indexOf(
                                            b.flatten().get(0).toStringNoConstraints()));
                        }
                    };
            @Override
            public int compare(Either<Path, DisjointRecursiveList<Path>> o1,
                    Either<Path, DisjointRecursiveList<Path>> o2) {
                return o1.accept(toViewIndex).compareTo(o2.accept(toViewIndex));
            }
        };

        effectiveView.addAll(determineEffectiveView());
    }

    /**
     * @return a read-only view over the effective view.
     */
    public List<Path> getEffectiveView() {
        return Collections.unmodifiableList(effectiveView);
    }

    /**
     * Merge lists of views at the same level, keeping them separate from the
     * nested sub-tables while merging them too.
     * @param shape The list to flatten out.
     * @return The flattened list.
     */
    private DisjointRecursiveList<Path> consolidateLevels(
            final DisjointRecursiveList<List<Path>> shape) {
        final DisjointRecursiveList<Path> consolidated = new DisjointRecursiveList<Path>();
        shape.forEach(new Eacher<List<Path>>() {
            @Override
            public Void visitLeft(List<Path> a) {
                for (Path p: a) {
                    consolidated.addNode(p);
                }
                return null;
            }
            @Override
            public Void visitRight(DisjointRecursiveList<List<Path>> b) {
                consolidated.addList(consolidateLevels(b));
                return null;
            }
        });
        return consolidated;
    }

    /**
     * A routine that sorts the consolidated list IN PLACE on each level. The same sorting
     * routine is invoked on each level of the data-structure.
     * @param consolidated A pre-consolidated recursive list.
     */
    private void orderListOnEachLevel(final DisjointRecursiveList<Path> consolidated) {
        consolidated.forEach(new Eacher<Path>() {
            @Override
            public Void visitLeft(Path a) {
                return null; // no-op
            }
            @Override
            public Void visitRight(DisjointRecursiveList<Path> b) {
                orderListOnEachLevel(b);
                return null;
            }
        });
        Collections.sort(consolidated.items, pathReorderer);
    }

    /**
     * Since this iterator doesn't always respect the view as provided by the query, lumping
     * together various view elements so that they are in groups according to their
     * outer-join groups, this method returns the view that the path-query is taken to have had
     * in terms of the results that are actually returned.
     * @return The view list as it should have been.
     */
    private List<Path> determineEffectiveView() {
        DisjointRecursiveList<Path> consolidated = consolidateLevels(resultsShape);
        orderListOnEachLevel(consolidated);
        return consolidated.flatten();
    }

    /**
     *  Utility class to make recursive shape determination more tractable.
     */
    private class ColumnConversionInput
    {
        List<? extends QuerySelectable> select;
        Map<String, QuerySelectable> pathToQueryNode;
        List<Path> paths;

        /**
         * Get the input for the next level of recursion.
         * @param newSelect The select list on the next level.
         * @return A new parameter object.
         */
        ColumnConversionInput getNextLevelInput(List<? extends QuerySelectable> newSelect) {
            ColumnConversionInput cci = new ColumnConversionInput();
            cci.paths = paths;
            cci.pathToQueryNode = pathToQueryNode;
            cci.select = newSelect;
            return cci;
        }
    }

    /**
     * Start recursing through the data-structure, building up a map of table to
     * outer-join group, from root on down. The purpose of this routine is to be
     * able later to know what each sub-table refers to logically within the results
     * in terms of the view of the path-query as provided by the user.
     * @param shape The pattern of the results we are operating over.
     * @return A map from table (including the top level) to its outer-join group.
     */
    private Map<DisjointRecursiveList<List<Path>>, Path> getLevelMap(
            final DisjointRecursiveList<List<Path>> shape) {
        final Map<DisjointRecursiveList<List<Path>>, Path> retVal =
                new HashMap<DisjointRecursiveList<List<Path>>, Path>();
        determineLevels(shape, retVal);
        return retVal;
    }

    /**
     * A recursive routine to determine the level (outer-join group) for each table
     * within a table of results. This routine uses the information within each table
     * to establish its level, or if this level has no paths of its own then it reads
     * the level of the sub-tables below it to determine what this one must be. At each
     * level it populates the map provided as an argument.
     * @param shape The pattern for the table at this level.
     * @param shapeToLevel The map from table template to the outer-join group it
     *                      represents (including the root group).
     * @return The level of this shape.
     */
    private Path determineLevels(
            final DisjointRecursiveList<List<Path>> shape,
            final Map<DisjointRecursiveList<List<Path>>, Path> shapeToLevel) {
        final Set<Path> pathsAtThisLevel = new HashSet<Path>();
        final List<Path> pathsBelowThisLevel = new ArrayList<Path>();
        if (shape.items.isEmpty()) {
            // Should totally never happen...
            return null;
        }

        shape.forEach(new TreeWalker<List<Path>>() {
            @Override
            public Void visitLeft(List<Path> views) {
                pathsAtThisLevel.addAll(views);
                return null;
            }
            @Override
            public Void visitRight(DisjointRecursiveList<List<Path>> b) {
                pathsBelowThisLevel.add(determineLevels(b, shapeToLevel));
                return null;
            }
        });

        Path retVal = null;
        try {
            if (!pathsAtThisLevel.isEmpty()) {
                Path oneOfThisLevel = pathsAtThisLevel.iterator().next();
                retVal = getOJG(oneOfThisLevel);
            } else if (!pathsBelowThisLevel.isEmpty()) {
                Path oneBelowThisLevel = pathsBelowThisLevel.iterator().next();
                retVal = getOJG(oneBelowThisLevel.getPrefix());
            } else {
                throw new RuntimeException("no paths found in this shape: " + shape);
            }
            // Make sure we get the sub-table's OJG.
            while (!(retVal.endIsCollection() || retVal.isRootPath())) {
                retVal = getOJG(retVal.getPrefix());
            }
        } catch (PathException e) {
            throw new RuntimeException("This should never have happened.", e);
        }
        shapeToLevel.put(shape, retVal);
        return retVal;
    }

    /**
     * Simple wrapper around pathQuery methods to prevent RSI.
     */
    private Path getOJG(Path somePath) throws PathException {
        return pathQuery.makePath(pathQuery.getOuterJoinGroup(somePath.getNoConstraintsString()));
    }

    /**
     * Stolen wholesale from the mechanism in Export results iterator, and adapted to return a
     * properly typed object rather than a raw collection, and to collect lists of paths rather than
     * bothering with the indices.
     * @param cci An input object
     * @return A list containing items that are either lists of paths or lists of the
     *            same type as this list.
     */
    private DisjointRecursiveList<List<Path>> determineResultShape(ColumnConversionInput cci) {
        DisjointRecursiveList<List<Path>> retval = new DisjointRecursiveList<List<Path>>();
        for (QuerySelectable qs : cci.select) {
            boolean done = false;
            while (!done) {
                if (qs instanceof QueryObjectPathExpression) {
                    QueryObjectPathExpression qope = (QueryObjectPathExpression) qs;
                    List<QuerySelectable> subSelect = qope.getSelect();
                    if (!subSelect.isEmpty()) {
                        qs = subSelect.get(0);
                        if (qs.equals(qope.getDefaultClass())) {
                            qs = qope;
                            done = true;
                        }
                    } else {
                        done = true;
                    }
                } else if (qs instanceof PathExpressionField) {
                    PathExpressionField pef = (PathExpressionField) qs;
                    QueryObjectPathExpression qope = pef.getQope();
                    qs = qope.getSelect().get(pef.getFieldNumber());
                    if (qs.equals(qope.getDefaultClass())) {
                        qs = qope;
                        done = true;
                    }
                } else {
                    done = true;
                }
            }
            if (qs instanceof QueryCollectionPathExpression) {
                QueryCollectionPathExpression qc = (QueryCollectionPathExpression) qs;
                List<QuerySelectable> subSelect = qc.getSelect();

                if (subSelect.isEmpty()) {
                    subSelect = Collections.singletonList((QuerySelectable) qc.getDefaultClass());
                }
                retval.addList(determineResultShape(cci.getNextLevelInput(subSelect)));
            } else {
                final List<Path> fieldsForObject = new LinkedList<Path>();
                for (Path path : cci.paths) {
                    Path parent = path.getPrefix();
                    QuerySelectable selectableForPath =
                            cci.pathToQueryNode.get(parent.toStringNoConstraints());
                    if (selectableForPath instanceof QueryCollectionPathExpression) {
                        selectableForPath = ((QueryCollectionPathExpression) selectableForPath)
                            .getDefaultClass();
                    }
                    if (qs.equals(selectableForPath)) {
                        fieldsForObject.add(path);
                    }
                }
                if (fieldsForObject.isEmpty()) {
                    LOG.error("Couldn't find any paths for " + qs + " from amongst " + cci.paths);
                }
                retval.addNode(fieldsForObject);
            }
        }
        return retval;
    }

    @Override
    public Iterator<List<Either<ResultCell, SubTable>>> iterator() {
        // Return a new iterator reset to the beginning of the results set.
        return new TableRowIterator(pathQuery, query, results, nodeForPath, page, im);
    }

    @Override
    public boolean hasNext() {
        return page.withinRange(counter) && osIter.hasNext();
    }

    @Override
    public void remove() {
        throw new NotImplementedException(getClass(), "remove");
    }

    @Override
    public List<Either<ResultCell, SubTable>> next() {
        @SuppressWarnings("rawtypes") // os iters don't support genericity.
        List row = (List) osIter.next();
        counter++;
        return recursiveNext(row, resultsShape);
    }

    /**
     * Actually fetch the next element and return it. This routine uses the results templates
     * to map to results rows of similar shapes.
     * @param row The data we are reading.
     * @param shape The pattern of the data we are reading, and what it all means.
     * @return A results row suitable for external consumption.
     */
    private List<Either<ResultCell, SubTable>> recursiveNext(
            @SuppressWarnings("rawtypes") final List row,
            final DisjointRecursiveList<List<Path>> shape) {

        final DisjointList<ResultCell, SubTable> retVal = new DisjointList<ResultCell, SubTable>();
        @SuppressWarnings("unchecked") // Object is the top type.
        final Iterator<Object> iter = row.iterator();

        shape.forEach(new TreeWalker<List<Path>>() {
            @Override
            public Void visitLeft(List<Path> views) {
                Object o = iter.next();
                if (o == null) {
                    for (Path p: views) {
                        retVal.addLeft(new TableCell(p));
                    }
                } else if (o instanceof FastPathObject) {
                    FastPathObject fpo = (FastPathObject) o;
                    for (Path p: views) {
                        String cls = TypeUtil.unqualifiedName(
                                DynamicUtil.getSimpleClassName(o.getClass()));
                        boolean isKeyField = false;
                        if (im != null) {
                            List<FieldDescriptor> keyFields = im.getClassKeys().get(cls);
                            if (keyFields != null) {
                                isKeyField = keyFields.contains(p.getEndFieldDescriptor());
                            }
                        }
                        retVal.addLeft(new TableCell(fpo, p, isKeyField));
                    }
                } else {
                    throw new RuntimeException(String.format(
                        NOT_INTERMINE_OBJECT, o.getClass().getName(), o
                    ));
                }
                return null;
            }
            @SuppressWarnings("rawtypes")
            @Override
            public Void visitRight(DisjointRecursiveList<List<Path>> b) {
                Object o = iter.next();
                if (o != null && !(o instanceof List)) {
                    throw new RuntimeException(String.format(EXPECTED_SUBTABLE, b, o));
                }
                List<List<Either<ResultCell, SubTable>>> data
                    = new ArrayList<List<Either<ResultCell, SubTable>>>();
                if (o != null) {
                    List ol = (List) o;
                    for (Object elem: ol) {
                        // Recursion happens here!!
                        data.add(recursiveNext((List) elem, b));
                    }
                }
                final List<Path> columns = new ArrayList<Path>();
                b.forEach(new TreeWalker<List<Path>>() {
                    @Override
                    public Void visitLeft(List<Path> views) {
                        columns.addAll(views);
                        return null;
                    }
                    @Override
                    public Void visitRight(DisjointRecursiveList<List<Path>> b) {
                        return null;
                    }
                });
                retVal.addRight(new SubTable(levels.get(b), columns, data));
                return null;
            }
        });

        // Make sure the elements within each table are in the same order as the view.
        Collections.sort(retVal, reorderer);

        return retVal;
    }

}
