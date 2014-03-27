package org.intermine.webservice.server.data;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.MainHelper;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;

public class DataService extends JSONService {

    public DataService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getResultsKey() {
        return "results";
    }

    @Override
    protected boolean lazyList() {
        return true;
    }

    @Override
    protected void execute() throws ServiceException {
        String pathInfo = request.getPathInfo();
        String rangeHeader = request.getHeader("Range");
        int start = -1, end = -1;
        if (StringUtils.isNotBlank(rangeHeader)) {
            String[] parts = rangeHeader.replace("records=", "").split("-");
            start = (StringUtils.isBlank(parts[0]) ? 0 : Integer.parseInt(parts[0], 10));
            end = (StringUtils.isBlank(parts[1]) ? -1 : Integer.parseInt(parts[1], 10));
        }
        if (StringUtils.isBlank(pathInfo)) {
            throw new ResourceNotFoundException(pathInfo + " not found.");
        }
        String className = pathInfo.substring(1);
        Model m = im.getModel();
        ClassDescriptor cd = m.getClassDescriptorByName(className);
        if (cd == null) {
            throw new ResourceNotFoundException(className + " not found.");
        }
        PathQuery pq = new PathQuery(m);
        pq.addView(className + ".id");
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String param = params.nextElement();
            String pathString = String.format("%s.%s", className, param);
            Path p;
            try {
                p = new Path(m, pathString);
            } catch (PathException e) {
                throw new BadRequestException(pathString + " is not a valid relationship.");
            }
            if (p.endIsAttribute()) {
                pq.addConstraint(Constraints.equalsExactly(pathString, request.getParameter(param)));
            } else {
                pq.addConstraint(Constraints.lookup(pathString, request.getParameter(param), ""));
            }
        }
        Profile p = getPermission().getProfile();
        Query q;
        try {
            q = MainHelper.makeQuery(pq, p.getCurrentSavedBags(), null, im.getBagQueryRunner(), null);
        } catch (ObjectStoreException e) {
            throw new ServiceException("Could not make query.", e);
        }

        ObjectStore os = im.getObjectStore();
        SingletonResults results = os.executeSingleton(q);

        if (results.size() == 0) {
            throw new ResourceNotFoundException("No " + className + "s found.");
        }

        Iterator<Object> iter;
        if (start >= 0) {
            try {
                if (end < 0 || end >= results.size()) {
                    end = results.size() - 1;
                }
                if (start >= results.size()) {
                    ServiceException e = new ServiceException("Range not satisfiable: Start exceeds size.");
                    e.setHttpErrorCode(416);
                    throw e;
                }
                if (end < start) {
                    ServiceException e = new ServiceException("Range not satisfiable: end is less than start.");
                    e.setHttpErrorCode(416);
                    throw e;
                }
                iter = results.range(start, end).iterator();
                response.setStatus(206); // Partial content.
            } catch (ObjectStoreException e) {
                throw new ServiceException("Could not retrieve results.", e);
            }
        } else {
            iter = results.iterator();
        }
        Set<AttributeDescriptor> attrs = cd.getAllAttributeDescriptors();
        while (iter.hasNext()) {
            FastPathObject result = (FastPathObject) iter.next();
            Map<String, Object> item = new HashMap<String, Object>();
            for (AttributeDescriptor ad: attrs) {
                String field = ad.getName();
                try {
                    item.put(field, result.getFieldValue(field));
                } catch (IllegalAccessException e) {
                    throw new ServiceException("Could not read " + field, e);
                }
            }
            addResultItem(item, iter.hasNext());
        }
    }

}
