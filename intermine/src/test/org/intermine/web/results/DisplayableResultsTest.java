package org.flymine.web.results;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.LinkedList;

import junit.framework.TestCase;

import org.flymine.objectstore.dummy.ObjectStoreDummyImpl;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.objectstore.query.Results;

public class DisplayableResultsTest extends TestCase
{
    public DisplayableResultsTest(String arg) {
        super(arg);
    }

    private ObjectStoreDummyImpl os;
    private FqlQuery fq;

    public void setUp() throws Exception {
        os = new ObjectStoreDummyImpl();
        os.setResultsSize(15);
        fq = new FqlQuery("select c1, c2, d1, d2 from Company as c1, Company as c2, Department as d1, Department as d2", "org.flymine.model.testmodel");
    }

    private DisplayableResults getExactResults() throws Exception {
        Results results = os.execute(fq.toQuery());
        // Make sure we definitely know the end
        results.setBatchSize(20);
        results.get(0);
        return new DisplayableResults(results);
    }

    private DisplayableResults getEstimateTooHighResults() throws Exception {
        os.setEstimatedResultsSize(25);
        Results results = os.execute(fq.toQuery());
        results.setBatchSize(1);
        return new DisplayableResults(results);
    }

    private DisplayableResults getEstimateTooLowResults() throws Exception {
        os.setEstimatedResultsSize(10);
        Results results = os.execute(fq.toQuery());
        results.setBatchSize(1);
        return new DisplayableResults(results);
    }

    public void testConstructor() throws Exception {
        DisplayableResults dr = getExactResults();
        assertEquals(4, dr.getColumns().size());
    }

    public void testSizeExact() throws Exception {
        DisplayableResults dr = getExactResults();
        dr.setPageSize(10);
        dr.setStart(0);
        assertFalse(dr.isSizeEstimate());
        assertEquals(15, dr.getSize());
    }

    public void testSizeHigh() throws Exception {
        DisplayableResults dr = getEstimateTooHighResults();
        dr.setPageSize(10);
        dr.setStart(0);
        assertTrue(dr.isSizeEstimate());
        assertEquals(25, dr.getSize());
    }

    public void testSizeLow() throws Exception {
        DisplayableResults dr = getEstimateTooLowResults();
        dr.setPageSize(10);
        dr.setStart(0);
        assertTrue(dr.isSizeEstimate());
        // Calling size() affects the estimate as it tries to fetch
        // more rows.  I think the best thing to do here is to check
        // that the size is greater than 10 and less than 15 to prove
        // that the size is not stuck at the estimate
        assertTrue(dr.getSize() > 10);
        assertTrue(dr.getSize() <= 15);
    }

    public void testEndExact() throws Exception {
        // At the beginning
        DisplayableResults dr = getExactResults();
        dr.setPageSize(10);
        dr.setStart(0);
        assertEquals(9, dr.getEnd());
        // Abutting the end
        dr = getExactResults();
        dr.setPageSize(10);
        dr.setStart(5);
        assertEquals(14, dr.getEnd());
        // Overlapping the end
        dr = getExactResults();
        dr.setPageSize(10);
        dr.setStart(10);
        assertEquals(14, dr.getEnd());
    }

    public void testEndHigh() throws Exception {
        // At the beginning
        DisplayableResults dr = getEstimateTooHighResults();
        dr.setPageSize(10);
        dr.setStart(0);
        assertEquals(9, dr.getEnd());
        // Abutting the end
        dr = getEstimateTooHighResults();
        dr.setPageSize(10);
        dr.setStart(5);
        assertEquals(14, dr.getEnd());
        // Overlapping the end
        dr = getEstimateTooHighResults();
        dr.setPageSize(10);
        dr.setStart(10);
        assertEquals(14, dr.getEnd());
    }

    public void testEndLow() throws Exception {
        // At the beginning
        DisplayableResults dr = getEstimateTooLowResults();
        dr.setPageSize(10);
        dr.setStart(0);
        assertEquals(9, dr.getEnd());
        // Abutting the end
        dr = getEstimateTooLowResults();
        dr.setPageSize(10);
        dr.setStart(5);
        assertEquals(14, dr.getEnd());
        // Overlapping the end
        dr = getEstimateTooLowResults();
        dr.setPageSize(10);
        dr.setStart(10);
        assertEquals(14, dr.getEnd());
    }

    public void testButtonsExact() throws Exception {
        DisplayableResults dr = getExactResults();
        dr.setPageSize(10);
        // At the beginning
        dr.setStart(0);
        assertFalse(dr.isPreviousButton());
        assertTrue(dr.isNextButton());
        // Abutting the end
        dr.setStart(5);
        assertTrue(dr.isPreviousButton());
        assertFalse(dr.isNextButton());
        // Overlapping the end
        dr.setStart(10);
        assertTrue(dr.isPreviousButton());
        assertFalse(dr.isNextButton());
    }

    public void testButtonsHigh() throws Exception {
        DisplayableResults dr = getEstimateTooHighResults();
        dr.setPageSize(10);
        // At the beginning
        dr.setStart(0);
        assertFalse(dr.isPreviousButton());
        assertTrue(dr.isNextButton());
        // Abutting the end
        dr.setStart(5);
        assertTrue(dr.isPreviousButton());
        assertFalse(dr.isNextButton());
        // Overlapping the end
        dr.setStart(10);
        assertTrue(dr.isPreviousButton());
        assertFalse(dr.isNextButton());
    }

    public void testButtonsLow() throws Exception {
        DisplayableResults dr = getEstimateTooLowResults();
        dr.setPageSize(10);
        // At the beginning (this abuts the estimated end)
        dr.setStart(0);
        assertFalse(dr.isPreviousButton());
        assertTrue(dr.isNextButton());
        // Abutting the end
        dr.setStart(5);
        assertTrue(dr.isPreviousButton());
        assertFalse(dr.isNextButton());
        // Overlapping the end
        dr.setStart(10);
        assertTrue(dr.isPreviousButton());
        assertFalse(dr.isNextButton());

    }

    public void testMoveColumnUp1() throws Exception {
        DisplayableResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setAlias("c1");
        columns.add(col);
        col = new Column();
        col.setAlias("c2");
        columns.add(col);
        col = new Column();
        col.setAlias("d1");
        columns.add(col);
        col = new Column();
        col.setAlias("d2");
        columns.add(col);

        dr.moveColumnUp("c1");
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnUp2() throws Exception {
        DisplayableResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setAlias("c2");
        columns.add(col);
        col = new Column();
        col.setAlias("c1");
        columns.add(col);
        col = new Column();
        col.setAlias("d1");
        columns.add(col);
        col = new Column();
        col.setAlias("d2");
        columns.add(col);

        dr.moveColumnUp("c2");
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnUp3() throws Exception {
        DisplayableResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setAlias("c1");
        columns.add(col);
        col = new Column();
        col.setAlias("d1");
        columns.add(col);
        col = new Column();
        col.setAlias("c2");
        columns.add(col);
        col = new Column();
        col.setAlias("d2");
        columns.add(col);

        dr.moveColumnUp("d1");
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnUp4() throws Exception {
        DisplayableResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setAlias("c1");
        columns.add(col);
        col = new Column();
        col.setAlias("c2");
        columns.add(col);
        col = new Column();
        col.setAlias("d2");
        columns.add(col);
        col = new Column();
        col.setAlias("d1");
        columns.add(col);

        dr.moveColumnUp("d2");
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnUp5() throws Exception {
        DisplayableResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setAlias("c1");
        columns.add(col);
        col = new Column();
        col.setAlias("c2");
        columns.add(col);
        col = new Column();
        col.setAlias("d1");
        columns.add(col);
        col = new Column();
        col.setAlias("d2");
        columns.add(col);

        dr.moveColumnUp("d3");
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnDown1() throws Exception {
        DisplayableResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setAlias("c2");
        columns.add(col);
        col = new Column();
        col.setAlias("c1");
        columns.add(col);
        col = new Column();
        col.setAlias("d1");
        columns.add(col);
        col = new Column();
        col.setAlias("d2");
        columns.add(col);

        dr.moveColumnDown("c1");
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnDown2() throws Exception {
        DisplayableResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setAlias("c1");
        columns.add(col);
        col = new Column();
        col.setAlias("d1");
        columns.add(col);
        col = new Column();
        col.setAlias("c2");
        columns.add(col);
        col = new Column();
        col.setAlias("d2");
        columns.add(col);

        dr.moveColumnDown("c2");
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnDown3() throws Exception {
        DisplayableResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setAlias("c1");
        columns.add(col);
        col = new Column();
        col.setAlias("c2");
        columns.add(col);
        col = new Column();
        col.setAlias("d2");
        columns.add(col);
        col = new Column();
        col.setAlias("d1");
        columns.add(col);

        dr.moveColumnDown("d1");
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnDown4() throws Exception {
        DisplayableResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setAlias("c1");
        columns.add(col);
        col = new Column();
        col.setAlias("c2");
        columns.add(col);
        col = new Column();
        col.setAlias("d1");
        columns.add(col);
        col = new Column();
        col.setAlias("d2");
        columns.add(col);

        dr.moveColumnDown("d2");
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnDown5() throws Exception {
        DisplayableResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setAlias("c1");
        columns.add(col);
        col = new Column();
        col.setAlias("c2");
        columns.add(col);
        col = new Column();
        col.setAlias("d1");
        columns.add(col);
        col = new Column();
        col.setAlias("d2");
        columns.add(col);

        dr.moveColumnDown("d3");
        assertEquals(columns, dr.getColumns());
    }

}
