package org.intermine.web.results;

/*
 * Copyright (C) 2002-2004 FlyMine
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

import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.query.Results;

public class DisplayableResultsTest extends TestCase
{
    public DisplayableResultsTest(String arg) {
        super(arg);
    }

    private ObjectStoreDummyImpl os;
    private IqlQuery fq;

    public void setUp() throws Exception {
        os = new ObjectStoreDummyImpl();
        os.setResultsSize(15);
        fq = new IqlQuery("select c1, c2, d1, d2 from Company as c1, Company as c2, Department as d1, Department as d2", "org.intermine.model.testmodel");
    }

    private DisplayableResults getEmptyResults() throws Exception {
        os.setResultsSize(0);
        Results results = os.execute(fq.toQuery());
        try {
            results.setBatchSize(20);
            results.get(0);
        } catch (IndexOutOfBoundsException e) {
        }
        return new DisplayableResults(results);
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

    public void testSizeEmpty() throws Exception {
        DisplayableResults dr = getEmptyResults();
        dr.setPageSize(10);
        dr.setStart(0);
        assertFalse(dr.isSizeEstimate());
        assertEquals(0, dr.getSize());
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

    // For the moment the end is -1 if the underlying results is empty
    // Anything using DisplayableResults should call getSize() first
    public void testEndEmpty() throws Exception {
        DisplayableResults dr = getEmptyResults();
        dr.setPageSize(10);
        dr.setStart(0);
        assertEquals(-1, dr.getEnd());
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


    public void testUpdate1() throws Exception {
        DisplayableResults dr1 = getExactResults();

        // Set some attributes of this DisplayableResults
        dr1.setStart(10);
        dr1.setPageSize(25);

        dr1.getColumn("c1").setVisible(true);
        dr1.getColumn("c2").setVisible(false);
        dr1.getColumn("d1").setVisible(true);
        dr1.getColumn("d2").setVisible(false);

        DisplayableResults dr2 = getExactResults();

        // Set some attributes of this DisplayableResults
        dr2.setStart(5);
        dr2.setPageSize(16);

        dr2.getColumn("c1").setVisible(false);
        dr2.getColumn("c2").setVisible(false);
        dr2.getColumn("d1").setVisible(false);
        dr2.getColumn("d2").setVisible(false);

        dr2.update(dr1);

        assertEquals(10, dr2.getStart());
        assertEquals(25, dr2.getPageSize());

        assertTrue(dr2.getColumn("c1").isVisible());
        assertFalse(dr2.getColumn("c2").isVisible());
        assertTrue(dr2.getColumn("d1").isVisible());
        assertFalse(dr2.getColumn("d2").isVisible());
    }

    public void testUpdate2() throws Exception {
        DisplayableResults dr1 = getExactResults();

        // Set some attributes of this DisplayableResults
        dr1.setStart(10);
        dr1.setPageSize(25);

        dr1.getColumn("c1").setVisible(true);
        dr1.getColumn("c2").setVisible(false);
        dr1.getColumn("d1").setVisible(true);
        dr1.getColumn("d2").setVisible(false);

        // Now update from another that contains some columns we don't have
        IqlQuery fq1 = new IqlQuery("select c1, d1 from Company as c1, Department as d1", "org.intermine.model.testmodel");
        DisplayableResults dr2 = new DisplayableResults(os.execute(fq1.toQuery()));

        dr2.setStart(5);
        dr2.setPageSize(16);

        dr2.getColumn("c1").setVisible(false);
        dr2.getColumn("d1").setVisible(false);

        dr2.update(dr1);

        assertEquals(10, dr2.getStart());
        assertEquals(25, dr2.getPageSize());

        assertFalse(dr2.getColumn("c1").isVisible());
        assertFalse(dr2.getColumn("d1").isVisible());

    }

    public void testUpdate3() throws Exception {
        DisplayableResults dr1 = getExactResults();

        // Set some attributes of this DisplayableResults
        dr1.setStart(10);
        dr1.setPageSize(25);

        dr1.getColumn("c1").setVisible(true);
        dr1.getColumn("c2").setVisible(false);
        dr1.getColumn("d1").setVisible(true);
        dr1.getColumn("d2").setVisible(false);

        // Now update from another that contains some columns we don't have
        IqlQuery fq2 = new IqlQuery("select c1, c2, c3, d1, d2, d3 from Company as c1, Company as c2, Company as c3, Department as d1, Department as d2, Department as d3", "org.intermine.model.testmodel");
        DisplayableResults dr2 = new DisplayableResults(os.execute(fq2.toQuery()));

        dr2.setStart(5);
        dr2.setPageSize(16);

        dr2.getColumn("c1").setVisible(false);
        dr2.getColumn("c2").setVisible(false);
        dr2.getColumn("c3").setVisible(true);
        dr2.getColumn("d1").setVisible(false);
        dr2.getColumn("d2").setVisible(false);
        dr2.getColumn("d3").setVisible(false);

        dr2.update(dr1);

        assertEquals(10, dr2.getStart());
        assertEquals(25, dr2.getPageSize());

        assertFalse(dr2.getColumn("c1").isVisible());
        assertFalse(dr2.getColumn("c2").isVisible());
        assertTrue(dr2.getColumn("c3").isVisible());
        assertFalse(dr2.getColumn("d1").isVisible());
        assertFalse(dr2.getColumn("d2").isVisible());
        assertFalse(dr2.getColumn("d3").isVisible());

    }

    public void testUpdate4() throws Exception {
        DisplayableResults dr1 = getExactResults();

        // Set some attributes of this DisplayableResults
        dr1.setStart(10);
        dr1.setPageSize(25);

        dr1.getColumn("c1").setVisible(true);
        dr1.getColumn("c2").setVisible(false);
        dr1.getColumn("d1").setVisible(true);
        dr1.getColumn("d2").setVisible(false);
        dr1.moveColumnUp("c2");

        DisplayableResults dr2 = getEstimateTooLowResults();

        dr2.setStart(5);
        dr2.setPageSize(16);

        dr2.getColumn("c1").setVisible(false);
        dr2.getColumn("d1").setVisible(false);

        dr2.update(dr1);

        assertEquals(10, dr2.getStart());
        assertEquals(25, dr2.getPageSize());

        assertTrue(dr2.getColumn("c1").isVisible());
        assertTrue(dr2.getColumn("d1").isVisible());
        assertEquals(dr2.getColumn("c2"), dr2.getColumns().get(0));
        assertEquals(dr2.getColumn("c1"), dr2.getColumns().get(1));
        assertEquals(dr2.getColumn("d1"), dr2.getColumns().get(2));
        assertEquals(dr2.getColumn("d2"), dr2.getColumns().get(3));


    }



}
