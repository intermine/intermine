package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.intermine.metadata.ConstraintOp;

/**
 * A constraint that restricts a path to inhabiting one of a list of types.
 * @author Alex Kalderimis.
 *
 */
public class PathConstraintMultitype extends PathConstraintMultiValue
{

    /** List of valid ops for this type of constraint */
    public static final Set<ConstraintOp> VALID_OPS = new HashSet<ConstraintOp>(Arrays.asList(
                ConstraintOp.ISA, ConstraintOp.ISNT));

    /**
     * Construct a multi-type constraint.
     * @param path The path.
     * @param op The operator.
     * @param typeNames The names of the types.
     */
    public PathConstraintMultitype(String path, ConstraintOp op, Collection<String> typeNames) {
        // Nasty hacky workaround for class initialisation order issues.
        super(path, ConstraintOp.ONE_OF, typeNames);

        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("PathConstraints on multiple types must use an op"
                    + " in the following list \"" + VALID_OPS + "\"");
        }
        this.op = op;
    }

}
