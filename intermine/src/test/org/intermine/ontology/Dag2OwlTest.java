package org.flymine.ontology;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;

public class Dag2OwlTest extends TestCase{
    public void testProcess() throws Exception {
        DagTerm a = new DagTerm("A", "This is term A");
        DagTerm b = new DagTerm("B", "This is term B");
        DagTerm c = new DagTerm("C", "This is term C");
        DagTerm d = new DagTerm("D", "This is term D");
        a.getChildren().add(b);
        a.getChildren().add(c);        
        d.getChildren().add(b);
        List rootTerms = new ArrayList();
        rootTerms.add(a);
        rootTerms.add(d);
        Dag2Owl.process(rootTerms).write(System.out);
    }
}
