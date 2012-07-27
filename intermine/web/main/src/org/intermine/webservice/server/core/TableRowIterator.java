package org.intermine.webservice.server.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.FastPathObject;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.webservice.server.core.Either.Left;
import org.intermine.webservice.server.core.Either.Right;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.exceptions.NotImplementedException;

/**
 * Iterator to present data directly from Results objects when data 
 * has been requested from path-queries. This iterator:
 * <ul>
 *   <li>Preserves the data structure of the underlying results, where
 *    each cell in each row may be either a value, or a sub-table in its own
 *    right, with its own columns.
 *   <li>Expands, however, the objects in the results to cells representing
 *    values for view paths.
 *   <li>Annotates cells with the view path they represent.
 *   <li>Annotates sub-tables with the columns they hold, and the joined collection
 *    they represent.
 * </ul>
 * @author Alex Kalderimis
 *
 */
public class TableRowIterator
    implements Iterator<List<Either<TableCell, SubTable>>>,
    Iterable<List<Either<TableCell, SubTable>>>
{

    private final static Logger LOG = Logger.getLogger(TableRowIterator.class);
    private final PathQuery pathQuery;
    private final Results results;
    private final Map<String, QuerySelectable> nodeForPath;
    private final List<Path> paths = new ArrayList<Path>();
    protected final Map<Path, Integer> columnForView = new HashMap<Path, Integer>();
    protected final Map<Path, List<Path>> rowTemplates = new HashMap<Path, List<Path>>(); // For debugging...
    private final List<Set<Path>> nodesForThisCell = new ArrayList<Set<Path>>();
    private final List<List<Path>> pathsForThisCell = new ArrayList<List<Path>>();
    private final Page page;
    private final InterMineAPI im;

    private Iterator<Object> osIter;
    private Path root;
    private int counter = 0;

    public TableRowIterator(
            PathQuery pathQuery,
            Results results,
            Map<String, QuerySelectable> nodeForPath,
            Page page,
            InterMineAPI im) {
        this.page = page;
        this.pathQuery = pathQuery;
        this.results = results;
        this.nodeForPath = nodeForPath;
        this.im = im; // Watch out, may be null!
        try {
            init();
        } catch (PathException e) {
            throw new RuntimeException("Failed to initialise", e);
        }
    }
    
    private boolean pathMayInherit(Path p) throws PathException {
        Path parent = p.getPrefix();
        if (parent.isRootPath()) {
            return false;
        }
        boolean thisMayInherit
            = pathQuery.getOuterJoinStatus(parent.getNoConstraintsString()) != OuterJoinStatus.OUTER;
        if (thisMayInherit) {
            return true;
        } else {
            for (Path v: paths) {
                Path vParent = v.getPrefix();
                if (!vParent.isRootPath()) {
                    if (vParent.getPrefix().equals(parent.getPrefix())) {
                        if (pathQuery.getOuterJoinStatus(vParent.getNoConstraintsString())
                                != OuterJoinStatus.OUTER) {
                            return true;
                        }
                    }
                }
            }
            for (Path subp: p.decomposePath()) {
                if (subp.endIsCollection() &&
                        pathQuery.getOuterJoinStatus(subp.getNoConstraintsString()) == OuterJoinStatus.OUTER) {
                   return true;
                }
            }
        }
        return false;
    }
    
    private void initPaths() throws PathException {
        for (String view: pathQuery.getView()) {
            paths.add(pathQuery.makePath(view));
        }
    }
    
    private void initColumnForView() throws PathException {
        int colNo = 0;
        for (QuerySelectable selected: results.getQuery().getSelect()) {
        	
            for (Path p: paths) {

                Path parent = p.getPrefix();
                QuerySelectable qs = nodeForPath.get(parent.toStringNoConstraints());

                if (selected.equals(qs)) {
                    columnForView.put(p, new Integer(colNo));
                    columnForView.put(parent, new Integer(colNo));
                }
            }
            colNo++;
        }
        Queue<Integer> unassigned = new LinkedList<Integer>();
        for (int i = 0; i < colNo; i++) {
            Collection<Integer> assigned = columnForView.values();
            if (!assigned.contains(i)) {
                unassigned.add(i);
            }
        }
        for (Path p: paths) {
            String pojg = pathQuery.getOuterJoinGroup(p.toStringNoConstraints());
            Path pojgp = pathQuery.makePath(pojg);
            boolean mayInherit = pathMayInherit(p);
            SEARCH: for (Path sought = p; !columnForView.containsKey(p); sought = sought.getPrefix()) {
                if (sought.isRootPath()) {
                    if (columnForView.containsKey(p.getPrefix())) {
                        columnForView.put(p, columnForView.get(p.getPrefix()));
                    } else {
                        try {
                            Integer i = unassigned.remove();
                            columnForView.put(p, i);
                            columnForView.put(p.getPrefix(), i);
                        } catch (NoSuchElementException e) {
                            throw new InternalErrorException(
                                String.format("Expected %s to be one of the unassigned columns", p));
                        }
                    }
                    break SEARCH;
                }
                String sojg = pathQuery.getOuterJoinGroup(sought.toStringNoConstraints());
                Path sojgp = pathQuery.makePath(sojg);
                if (mayInherit && !sojgp.isRootPath() && columnForView.containsKey(sojgp)) {
                    columnForView.put(p, columnForView.get(sojgp));
                } else if (pathQuery.getOuterJoinStatus(sought.getNoConstraintsString()) == OuterJoinStatus.OUTER) {
                    if ((sought.endIsCollection() || (sought.endIsReference() && !sojgp.isRootPath()))) {
                        for (Entry<Path, Integer> pair: columnForView.entrySet()) {
                            Path key = pair.getKey();
                            if (sojg.equals(pojg) && key.equals(sought)) {
                                columnForView.put(p, pair.getValue());
                                if (!sought.isRootPath()) {
                                    columnForView.put(sought, pair.getValue());
                                }
                                break SEARCH;
                            }
                        }
                    }
                }
            }
        }
    }

    private void init() throws PathException {
        osIter = results.iteratorFrom(page.getStart());
        counter = page.getStart();
        root = pathQuery.makePath(pathQuery.getRootClass());
        initPaths();
        initColumnForView();
        initRowTemplates();
        initCellPaths();
    }
    
    private void initCellPaths() {
        for (int i = 0; i < results.getQuery().getSelect().size(); i++) {
            Set<Path> nodesForCell = new HashSet<Path>();
            List<Path> pathsForCell = new ArrayList<Path>();
            for (Path p: paths) {
                if (columnForView.get(p).equals(i)) {
                    nodesForCell.add(p.getPrefix());
                    pathsForCell.add(p);
                }
            }
            pathsForThisCell.add(pathsForCell);
            nodesForThisCell.add(nodesForCell);
        }
    }
    
    private void initRowTemplates() throws PathException {
        rowTemplates.put(root, new ArrayList<Path>());
        for (Path p: paths) {
        	String ojg = pathQuery.getOuterJoinGroup(p.getNoConstraintsString());
            Path node = pathQuery.makePath(ojg);
            
            if (!rowTemplates.containsKey(node)) {
            	rowTemplates.put(node, new ArrayList<Path>());
//            	if ( node.endIsCollection()) {
//            		rowTemplates.put(node, new ArrayList<Path>());
//            	} else if (node.endIsReference() && pathQuery.isPathCompletelyInner(node.getPrefix().getNoConstraintsString())) {
//            		System.out.println("REFERENCE: " + node);
//                	rowTemplates.put(node, new ArrayList<Path>());
//            	}
            }
            rowTemplates.get(node).add(p);
            if (!columnForView.containsKey(node)) {
                columnForView.put(node, columnForView.get(p));
            }
            if (!root.equals(node)) {
            	Path nodeParent = node.getPrefix();
            	String nodeGroup = pathQuery.getOuterJoinGroup(nodeParent.getNoConstraintsString());
            	
            	Path ngp = pathQuery.makePath(nodeGroup);
            	List<Path> ngpPaths = rowTemplates.get(ngp);
            	if (ngpPaths == null) {
            		ngpPaths = new ArrayList<Path>();
            		rowTemplates.put(ngp, ngpPaths);
            	}
            	if (!ngpPaths.contains(node)) {
            		ngpPaths.add(node);
            	}
            }
        }
//        for (Path p: paths) {
//            String ojg = pathQuery.getOuterJoinGroup(p.getNoConstraintsString());
//            Path ojgPath = pathQuery.makePath(ojg);
//            if (ojgPath.isRootPath()) {
//                rowTemplates.get(ojgPath).add(p);
//            } else {
//                // Find the closest node and add it there.
//                while (!rowTemplates.containsKey(ojgPath)) {
//                    ojgPath = ojgPath.getPrefix();
//                }
//                rowTemplates.get(ojgPath).add(p);
//                // Now find a place to add the group path.
//                if (!ojgPath.isRootPath()) {
//                    Path groupOfGroup =
//                        pathQuery.makePath(pathQuery.getOuterJoinGroup(ojgPath.getPrefix().getNoConstraintsString()));
//                    while (!rowTemplates.containsKey(groupOfGroup)) {
//                        if (groupOfGroup.isRootPath()) {
//                            break;
//                        }
//                        groupOfGroup = groupOfGroup.getPrefix();
//                    }
//                    List<Path> rowTemplate = rowTemplates.get(groupOfGroup);
//                    if (!rowTemplate.contains(ojgPath)) {
//                    	rowTemplate.add(ojgPath);
//                    }
//                }
//            }
//        }
    }

    @Override
    public boolean hasNext() {
        return page.withinRange(counter) && osIter.hasNext();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<Either<TableCell, SubTable>> next() {
        List row = (List) osIter.next();
        List<Either<TableCell, SubTable>> data = new ArrayList<Either<TableCell, SubTable>>();
        Set<Integer> skipList = new HashSet<Integer>();
        List<Path> rowTemplate = rowTemplates.get(root);
        for (Path p: rowTemplate) {
            int idx = columnForView.get(p);
            //if (skipList.contains(idx)) {
            //    continue;
            //}
            Object o = row.get(idx);
            
            if (p.endIsAttribute()) {
            	makeCells(data, o, Arrays.asList(p));
            } else {
            	if (p.endIsReference()) {
            		processReference(row, data, o, rowTemplates.get(p), p);
            	} else {
            		processCollection(row, data, o, rowTemplates.get(p), p);
            	}
//                if (nodesForThisCell.get(idx).size() > 1) {
//                    List<List<Object>> rows = (List<List<Object>>) o;
//                    data.add(new Right(makeSubTable(rows, p.getPrefix(), pathsForThisCell.get(idx))));
//                    //skipList.add(idx);
//                } else {
//	                List<List<Object>> rows = (List<List<Object>>) o;
//	                data.add(new Right(makeSubTable(rows, p, rowTemplates.get(p))));
//                }
            }
        }
        counter++;
        return data;
    }
    
    private void processReference(
    		List<Object> osRow,
    		List<Either<TableCell, SubTable>> data,
            Object o,
            List<Path> views,
            Path referencePath) {
    	if (o instanceof List) {
    		processCollection(osRow, data, o, views, referencePath);
    	} else if (allAreAttributes(views)) {
    		makeCells(data, o, views);
    	} else {
			for (int i = 0; i < views.size(); i++) {
				Path p = views.get(i);
				Object o2 = o;
				if (osRow != null) {
					o2 = osRow.get(columnForView.get(p));
				}
				
	    		if (p.endIsAttribute()) {
	    			makeCells(data, o2, Arrays.asList(p));
	    		} else if (p.endIsReference()) {
	    			processReference(osRow, data, o2, rowTemplates.get(p), p);
	    		} else {
	    			processCollection(osRow, data, o2, rowTemplates.get(p), p);
	    		}
	    	}
    	}
    }
    
    private boolean allAreAttributes(Collection<Path> paths) {
    	for (Path p: paths) {
    		if (!p.endIsAttribute()) {
    			return false;
    		}
    	}
    	return true;
    }
    
    private void processCollection(
    		List<Object> osRow,
    		List<Either<TableCell, SubTable>> data,
    		Object o,
    		List<Path> views,
    		Path collectionPath) {
    	List<List<Object>> subRows = (List<List<Object>>) o;
    	data.add(new Right(makeSubTable(subRows, collectionPath, rowTemplates.get(collectionPath))));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void makeCells(
            List<Either<TableCell, SubTable>> currentRow,
            Object o,
            List<Path> views) {
        if (o == null) {
            for (Path p: views) {
                currentRow.add(new Left(new TableCell(p)));
            }
        } else if (o instanceof FastPathObject) {
            FastPathObject fpo = (FastPathObject) o;
            for (Path p: views) {
	            String cls = TypeUtil.unqualifiedName(DynamicUtil.getSimpleClassName(o.getClass()));
	            boolean isKeyField = false;
	            if (im != null) {
	                List<FieldDescriptor> keyFields = im.getClassKeys().get(cls);
	                if (keyFields != null) {
	                    isKeyField = keyFields.contains(p.getEndFieldDescriptor());
	                }
	            }
	            currentRow.add(new Left(new TableCell(fpo, p, isKeyField)));
            }
        } else {
            throw new RuntimeException(String.format(
                "Table row iteration only supported over InterMine Objects, not %s instances. Got %s",
                o.getClass().getName(), o
            ));
        }
    }
    
    private static List<Entry<Integer, List<Path>>> groupColumns(List<Path> columns) {
        final List<List<Path>> groupedColumns = new ArrayList<List<Path>>();
        List<Path> currentGroup = new LinkedList<Path>();
        groupedColumns.add(currentGroup);
        ClassDescriptor currentType = columns.get(0).getLastClassDescriptor();
        for (Path p: columns) {
            if (p.endIsCollection() || !p.getLastClassDescriptor().equals(currentType)) {
                currentType = p.getLastClassDescriptor();
                currentGroup = new ArrayList<Path>();
                groupedColumns.add(currentGroup);
            }
            currentGroup.add(p);
        }
        List<Integer> columnIndices = new ArrayList<Integer>();
        for (int i = 0; i < groupedColumns.size(); i++) {
            columnIndices.add(new Integer(i));
        }
        
        // Required if the columns are reversed within the outer-join groups (aka. the devil's tables).
        // Get the indices in the order we want to select them from the row.
        /*
        Collections.sort(columnIndices, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                Integer depth1 = new Integer(groupedColumns.get(o1).get(0).getElements().size());
                boolean isAttr1 = groupedColumns.get(o1).get(0).endIsAttribute();
                Integer depth2 = new Integer(groupedColumns.get(o2).get(0).getElements().size());
                boolean isAttr2 = groupedColumns.get(o2).get(0).endIsAttribute();
                if (isAttr1 ^ isAttr2) {
	            	if (isAttr1) {
	                	return 1;
	                } else if (isAttr1) {
	                	return -1;
	                }
                }
                return depth1.compareTo(depth2);
                
            }
        });
        
        // Get the paths in an order that matches the indices.
        Collections.sort(groupedColumns, new Comparator<List<Path>>() {
            @Override
           public int compare(List<Path> o1, List<Path> o2) {
                // Give groups based around shorter paths precedence.
                Integer length1 = new Integer(o1.get(0).getElements().size());
                boolean isAttr1 = o1.get(0).endIsAttribute();
                Integer length2 = new Integer(o2.get(0).getElements().size());
                boolean isAttr2 = o2.get(0).endIsAttribute();
                if (isAttr1 ^ isAttr2) {
	            	if (isAttr1) {
	                	return 1;
	                } else if (isAttr1) {
	                	return -1;
	                }
                }
                return length1.compareTo(length2);
            }
        });
        */
        
        List<Entry<Integer, List<Path>>> ret = new LinkedList<Entry<Integer, List<Path>>>();
        for (Integer index: columnIndices) {
            ret.add(new Pair<Integer, List<Path>>(index, groupedColumns.get(index)));
        }
        return ret;
    }
    
    private List<Path> refsToEnd(List<Path> columns) {
    	List<Path> ret = new ArrayList<Path>();
    	Queue<Path> refs = new LinkedList<Path>(); 
    	for (Path p: columns) {
    		if (p.endIsAttribute()) {
    			ret.add(p);
    		} else {
    			refs.add(p);
    		}
    	}
    	while (!refs.isEmpty()) {
    		ret.add(refs.remove());
    	}
    	return ret;
    }
    
    private SubTable makeSubTable(
            List<List<Object>> rows,
            Path nodePath,
            List<Path> columns) {
        List<List<Either<TableCell, SubTable>>> data = new ArrayList<List<Either<TableCell, SubTable>>>();
        
        columns = refsToEnd(columns);
        List<Entry<Integer, List<Path>>> indicesToPaths = groupColumns(columns);
        
        List<Path> subtablePaths = new ArrayList<Path>();
        boolean doneOneRow = false;
        for (List<Object> row: rows) {
            List<Either<TableCell, SubTable>> subTableRow = new ArrayList<Either<TableCell, SubTable>>();
            for (Entry<Integer, List<Path>> idxAndPaths: indicesToPaths) {
                Object o = row.get(idxAndPaths.getKey());
                
                List<Path> paths = idxAndPaths.getValue();
                if (paths.size() == 1 && paths.get(0).endIsReference()) {
                	Path refPath = paths.get(0);
                	paths = rowTemplates.get(refPath);
                }
                
                if (o instanceof List) {
                    List<List<Object>> subrows = (List<List<Object>>) o;
                    Path subNodePath = paths.get(0);
                    List<Path> subcols = rowTemplates.get(subNodePath);
                    subTableRow.add(new Right(makeSubTable(subrows, subNodePath, subcols)));
                    if (!doneOneRow) {
                    	subtablePaths.add(subNodePath);
                    }
                } else {
                    makeCells(subTableRow, o, paths);
                    if (!doneOneRow) {
                    	subtablePaths.addAll(paths);
                    }
                }
            }
            data.add(subTableRow);
            doneOneRow = true;
        }
        return new SubTable(nodePath, subtablePaths, data);
    }


    @Override
    public void remove() {
        throw new NotImplementedException(getClass(), "remove");
    }

    @Override
    public Iterator<List<Either<TableCell, SubTable>>> iterator() {
        if (counter == 0) { // Unused, we can iterate directly.
            return this;
        } else {
            return new TableRowIterator(pathQuery, results, nodeForPath, page, im);
        }
    }

}
