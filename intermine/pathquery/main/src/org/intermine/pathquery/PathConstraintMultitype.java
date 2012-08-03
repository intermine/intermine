package org.intermine.pathquery;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.intermine.objectstore.query.ConstraintOp;

public class PathConstraintMultitype extends PathConstraintMultiValue {

    /** List of valid ops for this type of constraint */
    public static final Set<ConstraintOp> VALID_OPS = new HashSet<ConstraintOp>(Arrays.asList(
                ConstraintOp.ISA, ConstraintOp.ISNT));
    
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
