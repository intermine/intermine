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
        fq = new FqlQuery("select c, d from Company as c, Department as d", "org.flymine.model.testmodel");
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
        assertEquals(2, dr.getColumns().size());
    }

    public void testSizeExact() throws Exception {
        DisplayableResults dr = getExactResults();
        assertFalse(dr.isSizeEstimate());
        assertEquals(15, dr.getSize());
    }

    public void testSizeHigh() throws Exception {
        DisplayableResults dr = getEstimateTooHighResults();
        assertTrue(dr.isSizeEstimate());
        assertEquals(25, dr.getSize());
    }

    public void testSizeLow() throws Exception {
        DisplayableResults dr = getEstimateTooLowResults();
        assertTrue(dr.isSizeEstimate());
        assertEquals(10, dr.getSize());
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


}
