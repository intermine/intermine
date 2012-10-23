package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Comparator;

/**
 * @author Alexis Kalderimis
 *
 */
public class PathLengthComparator implements Comparator<Path>
{
    private static PathLengthComparator instance = new PathLengthComparator();

    /**
     * protected constructor
     */
    protected PathLengthComparator() {
        // protected constructor
    }

    /**
     * @return comparator
     */
    public static PathLengthComparator getInstance() {
        return instance;
    }

    /**
     * Compare paths by number elements they contain, shortest paths come first. If the number of
     * elements is the same sort by text length of paths.
     * {@inheritDoc}
     */
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
