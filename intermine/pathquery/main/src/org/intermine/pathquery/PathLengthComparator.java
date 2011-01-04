/**
 * 
 */
package org.intermine.pathquery;

import java.util.Comparator;
import java.util.List;

/**
 * @author Alexis Kalderimis
 *
 */
public class PathLengthComparator implements Comparator<Path> {
	
	private static PathLengthComparator instance = new PathLengthComparator();
	
	protected PathLengthComparator() {
		// protected constructor
	}
	
	public static PathLengthComparator getInstance() {
		return instance;
	}
	
	@Override
	public int compare(Path arg0, Path arg1) {
		if (arg0 == null || arg1 == null) {
			throw new RuntimeException("Paths must not be null");
		}
		int length0 = arg0.getElements().size();
		int length1 = arg1.getElements().size();
		
		if (length0 < length1) {
			return -1;
		}
		if (length0 > length1) {
			return 1;
		}
		return arg0.toStringNoConstraints().compareTo(
				arg1.toStringNoConstraints());
	}

}
