/**
 * 
 */
package org.intermine.webservice.server.query.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.profile.InterMineBag;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathLengthComparator;
import org.intermine.pathquery.PathQuery;

/**
 * @author Alexis Kalderimis
 *
 */
public class PathQueryBuilderForJSONObj extends PathQueryBuilder {

	
	protected PathQueryBuilderForJSONObj() {
		// empty constructor for testing
	}
	public PathQueryBuilderForJSONObj(String xml, String schemaUrl,
			Map<String, InterMineBag> savedBags) {
		super(xml, schemaUrl, savedBags);
	}
	
	/**
	 * For the purposes of exporting into JSON objects the view must be:
	 * <ul>
	 * 	<li>Ordered by length, such that Company.departments.name precedes
	 *      Company.departments.employees.name</li>
	 *  <li>Be constituted so that every class has an attribute on it, root class
	 *      included, and every reference along the way. So a path such as
	 *      Departments.employees.address.address is illegal unless it is preceded
	 *      by Departments.id and Departments.employees.id (id is the default if 
	 *      none is supplied).</li>
	 *  </ul>
	 *  The purpose of this method is to perform the necessary transformations.
	 *  @return a PathQuery with an appropriately mangled view. 
	 */
	@Override
	public PathQuery getQuery() {
		PathQuery beforeChanges = super.getQuery();
		
		PathQuery afterChanges = beforeChanges.clone();
		afterChanges.clearView();
		afterChanges.addViews(getAlteredViews(beforeChanges));
				
		return afterChanges;
		
	}
	
	public static List<String> getAlteredViews(PathQuery pq) {
		List<String> originalViews = pq.getView();
		List<Path> viewPaths = new ArrayList<Path>();
		for (String v : originalViews) {
			try {
				viewPaths.add(pq.makePath(v));
			} catch (PathException e) {
				throw new RuntimeException("Problem making path " + v, e);
			}
		}
		Collections.sort(viewPaths, PathLengthComparator.getInstance());
		List<String> newViews = new ArrayList<String>();
		Set<Path> classesWithAttributes = new HashSet<Path>();

		for (Path p : viewPaths) {
			if (! p.endIsAttribute()) {
				throw new RuntimeException("The view can only contain attribute paths - Got: '" + p.toStringNoConstraints() + "'");
			}
			newViews.addAll(getNewViewStrings(classesWithAttributes, p));
			
		}		
		return newViews;
	}
	
	public static List<String> getNewViewStrings(Set<Path> classesWithAttributes, Path p) {
		// The prefix automatically has an attribute, since its child is in the view
		classesWithAttributes.add(p.getPrefix());
		List<Path> composingPaths = p.decomposePath();
		List<String> newParts = new ArrayList<String>();
		for (Path cp : composingPaths) {
			if (! cp.endIsAttribute() && ! classesWithAttributes.contains(cp)) {
				newParts.add(getNewAttributeNode(classesWithAttributes, cp));
			}
		}
		newParts.add(p.toStringNoConstraints());
		return newParts;
	}
	
	public static String getNewAttributeNode(Set<Path> classesWithAttributes, Path p) {
		String retVal;
		try {
			retVal = p.append("id").toStringNoConstraints();
			classesWithAttributes.add(p);
		} catch (PathException e) {
			// This should be frankly impossible
			throw new RuntimeException("Couldn't extend " 
					+ p.toStringNoConstraints() + " with 'id'", e);
		}
		return retVal;
	}
	
}
