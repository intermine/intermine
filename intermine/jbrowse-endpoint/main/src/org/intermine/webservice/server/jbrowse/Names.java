package org.intermine.webservice.server.jbrowse;

import static org.intermine.webservice.server.jbrowse.Queries.pathQueryToOSQ;
import static org.intermine.webservice.server.jbrowse.Queries.resolveValue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.FastPathObject;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.jbrowse.util.ArrayFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;

public class Names extends JSONService {

    private String prefix;
    private String featureClass;
    private String refPath;
    private String startPath;
    private String endPath;
    private String identPath;
    private String[] namePaths;
    private ClassDescriptor fcd;

    public Names(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Output makeJSONOutput(PrintWriter out, String separator) {
        return new StreamedOutput(out, new ArrayFormatter(), separator);
    }

    @Override
    protected void execute() throws ServiceException {
        String domain = getDomain();
        String searchTerm = getSearchTerm();
        ConstraintOp op = getOp();

        PathQuery pq = getQuery(domain, op, searchTerm);

        Query q = pathQueryToOSQ(pq);
        Iterator<Object> it = im.getObjectStore().executeSingleton(q).iterator();

        while (it.hasNext()) {
            FastPathObject o = (FastPathObject) it.next();
            Map<String, Object> searchHit = makeRecord(o, searchTerm);
            addResultItem(searchHit, it.hasNext());
        }
    }

    // Helpers

    private ConstraintOp getOp() {
        if (StringUtils.isBlank(request.getParameter("equals"))) {
            return ConstraintOp.MATCHES;
        } else {
            return ConstraintOp.EXACT_MATCH;
        }
    }

    private String getDomain() {
        String domain = StringUtils.substring(request.getPathInfo(), 1);
        if (StringUtils.isBlank(domain)) {
            throw new BadRequestException("No domain provided");
        }
        return domain;
    }

    private String getSearchTerm() {
        String searchTerm
             , equals = request.getParameter("equals")
             , startswith = request.getParameter("startswith");

        boolean hasEquals = !StringUtils.isBlank(equals)
               , hasStart = !StringUtils.isBlank(startswith);

        if (hasEquals && hasStart) {
            throw new BadRequestException("Either 'startswith' or 'equals' parameter is required");
        }
        if (!hasEquals && !hasStart) {
            throw new BadRequestException("Only one of 'startswith' or 'equals' parameters is allowed");
        }

        if (hasEquals) {
            searchTerm = equals;
        } else {
            searchTerm = startswith + "*";
        }
        return searchTerm;
    }

    private String[] getNamePaths() {
        return namePaths;
    }

    @Override
    protected void initState() {
        super.initState();
        prefix = getPropertyPrefix();
        featureClass = webProperties.getProperty(prefix + "featureClass");
        refPath   = webProperties.getProperty(prefix + "paths.ref");
        startPath = webProperties.getProperty(prefix + "paths.start");
        endPath   = webProperties.getProperty(prefix + "paths.end");
        identPath = webProperties.getProperty(prefix + "paths.ident");
        namePaths = webProperties.getProperty(prefix + "paths.names").split("\\|");
        fcd = im.getModel().getClassDescriptorByName(featureClass);
    }

    private Map<String, Object> makeRecord(final FastPathObject o, String searchTerm) {
        Map<String, Object> record = new HashMap<String, Object>();
        Map<String, Object> location = new HashMap<String, Object>();
        List<String> tracks = new ArrayList<String>();

        searchTerm = searchTerm.replace("*", "");

        record.put("location", location);
        location.put("tracks", tracks);

        location.put("ref", resolveValue(o, refPath));
        location.put("start", resolveValue(o, startPath));
        location.put("end", resolveValue(o, endPath));
        location.put("objectName", resolveValue(o, identPath));

        for (ClassDescriptor cd: im.getModel().getClassDescriptors()) {
            if (cd.getUnqualifiedName().equals("InterMineObject")) continue;
            if (!fcd.getType().isAssignableFrom(cd.getType())) continue; // Upper bound.
            if (cd.getType().isAssignableFrom(o.getClass())) {
                tracks.add(getTrackName(cd));
            }
        }

        // Accept the first non null that matches the search term.
        for (Object name: getNames(o, getNamePaths())) {
            record.put("name", name);
            if (name != null && StringUtils.containsIgnoreCase(name.toString(), searchTerm)) break;
        }

        return record;
    }

    private Iterable<Object> getNames(final FastPathObject o, final String[] namePaths) {
        return new Iterable<Object>() {
            FastPathObject root = o;
            String featureClass = webProperties.getProperty(getPropertyPrefix() + "featureClass");
            @Override
            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    private int current = 0;
                    private List subCol = null;
                    private String subPath = null;
                    private int subIdx = 0;

                    @Override
                    public boolean hasNext() {
                        if (subCol != null) {
                            return subIdx < subCol.size();
                        }
                        return current < namePaths.length;
                    }

                    private Object nextFromSubCol() {
                        FastPathObject subRoot = (FastPathObject) subCol.get(subIdx);
                        subIdx++;
                        if (subIdx >= subCol.size()) {
                            subCol = null;
                            subIdx = 0;
                        }
                        return resolveValue(subRoot, subPath);
                    }

                    @Override
                    public Object next() {
                        if (subCol != null) return nextFromSubCol();
                        String path = namePaths[current];
                        current++;
                        try {
                            Path p = new Path(im.getModel(), featureClass + "." + path);
                            if (!p.containsCollections()) {
                                return resolveValue(root, path);
                            } else {
                                String upToCollection = "";
                                for (Path pp: p.decomposePath()) {
                                    upToCollection = pp.toStringNoConstraints();
                                    if (pp.endIsCollection()) {
                                        break;
                                    }
                                }
                                Collection things = (Collection) resolveValue(root, upToCollection.replaceAll("^[^\\.]+\\.", ""));
                                if (things.isEmpty()) {
                                    return null;
                                } else {
                                    subCol = new ArrayList(things);
                                    subIdx = 0;
                                    subPath = p.toStringNoConstraints().replace(upToCollection + ".", "");
                                    return nextFromSubCol();
                                }
                            }
                        } catch (PathException e) {
                            throw new RuntimeException("Bad path: " + path);
                        }
                    }

                    @Override
                    public void remove() {
                        throw new RuntimeException("Not implemented.");
                    }
                };
            }
            
        };
    }

    private String getTrackName(ClassDescriptor cd) {
        return cd.getUnqualifiedName();
    }


    private String getPropertyPrefix() {
        String modelName = im.getModel().getName();
        String prefix = "org.intermine.webservice.server.jbrowse." + modelName + ".";
        return prefix;
    }

    private PathQuery getQuery(String domain, ConstraintOp op, String searchTerm) {
        PathQuery pq = new PathQuery(im.getModel());
        String prefix = getPropertyPrefix();
        String[] namePaths = getNamePaths();

        String featureClass = webProperties.getProperty(prefix + "featureClass");
        String domainPath = webProperties.getProperty(prefix + "domain");
        String locationPath = webProperties.getProperty(prefix + "location");
        pq.addView(featureClass + ".id");

        StringBuffer logic = new StringBuffer();

        // must have location
        logic.append(pq.addConstraint(Constraints.isNotNull(featureClass + "." + locationPath + ".id")));
        logic.append(" AND "); // and be on the right domain.
        logic.append(pq.addConstraint(Constraints.eq(featureClass + "." + domainPath, domain)));
        logic.append(" AND ");

        logic.append(" (");
        for (int i = 0; i < namePaths.length; i++) {
            PathConstraint c = new PathConstraintAttribute(featureClass + "." + namePaths[i], op, searchTerm);
            logic.append(pq.addConstraint(c));
            if (i + 1 < namePaths.length) {
                logic.append(" OR ");
            }
        }
        logic.append(")");
        pq.setConstraintLogic(logic.toString());

        return pq;
    }

}
