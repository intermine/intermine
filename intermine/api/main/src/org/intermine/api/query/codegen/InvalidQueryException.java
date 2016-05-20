package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A class representing a failure to generate the code for a query because
 * the query is invalid. This class is associated with a list of reasons
 * why the query is invalid.
 * @author Alex Kalderimis
 *
 */
public class InvalidQueryException extends Exception
{

    private final List<String> problems = new LinkedList<String>();

    /**
     * Construct an exception with a single problem.
     * @param msg The reason why this query is invalid.
     */
    public InvalidQueryException(String msg) {
        super(msg);
        problems.add(msg);
    }

    /**
     * Construct an exception with a list of problems.
     * @param problems The reasons why this query is invalid.
     */
    public InvalidQueryException(Collection<? extends String> problems) {
        super(problems.toString());
        this.problems.addAll(problems);
    }

    /**
     * Generated Serial ID
     */
    private static final long serialVersionUID = -8340541541736991617L;

    /**
     * @return The reasons why this query is invalid.
     */
    public List<String> getProblems() {
        return problems;
    }

}
