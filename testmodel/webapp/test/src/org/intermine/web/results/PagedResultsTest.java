package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedList;
import java.util.List;

import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.iql.IqlQuery;

import org.intermine.metadata.Model;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;

import junit.framework.TestCase;

public class PagedResultsTest extends TestCase
{
    public PagedResultsTest(String arg) {
        super(arg);
    }

    private ObjectStoreDummyImpl os;
    private IqlQuery fq;

    public void setUp() throws Exception {
        os = new ObjectStoreDummyImpl();
        os.setResultsSize(15);
        fq = new IqlQuery("select c1, c2, d1, d2 from Company as c1, Company as c2, Department as d1, Department as d2", "org.intermine.model.testmodel");
    }

    private PagedResults getEmptyResults() throws Exception {
        os.setResultsSize(0);
        Results results = os.execute(fq.toQuery());
        try {
            results.setBatchSize(20);
            results.get(0);
        } catch (IndexOutOfBoundsException e) {
        }
        Model model = Model.getInstanceByName("testmodel");
        return new PagedResults(results, model);
    }

    private PagedResults getExactResults() throws Exception {
        Results results = os.execute(fq.toQuery());
        // Make sure we definitely know the end
        results.setBatchSize(20);
        results.get(0);
        Model model = Model.getInstanceByName("testmodel");
        return new PagedResults(results, model);
    }

    private PagedResults getEstimateTooHighResults() throws Exception {
        os.setEstimatedResultsSize(25);
        Results results = os.execute(fq.toQuery());
        results.setBatchSize(1);
        Model model = Model.getInstanceByName("testmodel");
        return new PagedResults(results, model);
    }

    private PagedResults getEstimateTooLowResults() throws Exception {
        os.setEstimatedResultsSize(10);
        Results results = os.execute(fq.toQuery());
        results.setBatchSize(1);
        Model model = Model.getInstanceByName("testmodel");
        return new PagedResults(results, model);
    }

    public void testConstructor() throws Exception {
        PagedResults dr = getExactResults();
        assertEquals(4, dr.getColumns().size());
    }

    public void testSizeExact() throws Exception {
        PagedResults dr = getExactResults();
        dr.setPageSize(10);
        assertEquals(15, dr.getSize());
    }

    public void testSizeHigh() throws Exception {
        PagedResults dr = getEstimateTooHighResults();
        dr.setPageSize(10);
        assertEquals(25, dr.getSize());
    }

//     public void testSizeLow() throws Exception {
//         PagedResults dr = getEstimateTooLowResults();
//         dr.setPageSize(10);
//         // Calling size() affects the estimate as it tries to fetch
//         // more rows.  I think the best thing to do here is to check
//         // that the size is greater than 10 and less than 15 to prove
//         // that the size is not stuck at the estimate
//         assertTrue(dr.getSize() > 10);
//         assertTrue(dr.getSize() <= 15);
//     }

//     public void testSizeEmpty() throws Exception {
//         PagedResults dr = getEmptyResults();
//         dr.setPageSize(10);
//         assertEquals(0, dr.getSize());
//     }

//     public void testEndExact() throws Exception {
//         // At the beginning
//         PagedResults dr = getExactResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(0);
//         assertEquals(9, dr.getEndIndex());
//         // Abutting the end
//         dr = getExactResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(5);
//         assertEquals(14, dr.getEndIndex());
//         // Overlapping the end
//         dr = getExactResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(10);
//         assertEquals(14, dr.getEndIndex());
//     }

//     public void testEndHigh() throws Exception {
//         // At the beginning
//         PagedResults dr = getEstimateTooHighResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(0);
//         assertEquals(9, dr.getEndIndex());
//         // Abutting the end
//         dr = getEstimateTooHighResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(5);
//         assertEquals(14, dr.getEndIndex());
//         // Overlapping the end
//         dr = getEstimateTooHighResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(10);
//         assertEquals(14, dr.getEndIndex());
//     }

//     public void testEndLow() throws Exception {
//         // At the beginning
//         PagedResults dr = getEstimateTooLowResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(0);
//         assertEquals(9, dr.getEndIndex());
//         // Abutting the end
//         dr = getEstimateTooLowResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(5);
//         assertEquals(14, dr.getEndIndex());
//         // Overlapping the end
//         dr = getEstimateTooLowResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(10);
//         assertEquals(14, dr.getEndIndex());
//     }

    // For the moment the end is -1 if the underlying results is empty
    // Anything using PagedResults should call getSize() first
//     public void testEndEmpty() throws Exception {
//         PagedResults dr = getEmptyResults();
//         dr.setPageSize(10);
//         assertEquals(-1, dr.getEndIndex());
//     }

//     public void testButtonsExact() throws Exception {
//         PagedResults dr = getExactResults();
//         dr.setPageSize(10);
//         // At the beginning
//         dr.setStartIndex(0);
//         assertTrue(dr.isFirstPage());
//         assertFalse(dr.isLastPage());
//         // Abutting the end
//         dr.setStartIndex(5);
//         assertFalse(dr.isFirstPage());
//         assertTrue(dr.isLastPage());
//         // Overlapping the end
//         dr.setStartIndex(10);
//         assertFalse(dr.isFirstPage());
//         assertTrue(dr.isLastPage());
//     }

//     public void testButtonsHigh() throws Exception {
//         PagedResults dr = getEstimateTooHighResults();
//         dr.setPageSize(10);
//         // At the beginning
//         dr.setStartIndex(0);
//         assertTrue(dr.isFirstPage());
//         assertFalse(dr.isLastPage());
//         // Abutting the end
//         dr.setStartIndex(5);
//         assertFalse(dr.isFirstPage());
//         assertTrue(dr.isLastPage());
//         // Overlapping the end
//         dr.setStartIndex(10);
//         assertFalse(dr.isFirstPage());
//         assertTrue(dr.isLastPage());
//     }

//     public void testButtonsLow() throws Exception {
//         PagedResults dr = getEstimateTooLowResults();
//         dr.setPageSize(10);
//         // At the beginning (this abuts the estimated end)
//         dr.setStartIndex(0);
//         assertTrue(dr.isFirstPage());
//         assertFalse(dr.isLastPage());
//         // Abutting the end
//         dr.setStartIndex(5);
//         assertFalse(dr.isFirstPage());
//         assertTrue(dr.isLastPage());
//         // Overlapping the end
//         dr.setStartIndex(10);
//         assertFalse(dr.isFirstPage());
//         assertTrue(dr.isLastPage());

//     }

    public void testMoveColumnLeft1() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnLeft(0);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnLeft2() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnLeft(1);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnLeft3() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnLeft(2);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnLeft4() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);

        dr.moveColumnLeft(3);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnLeft5() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnLeft(4);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnRight1() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnRight(0);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnRight2() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnRight(1);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnRight3() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);

        dr.moveColumnRight(2);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnRight4() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnRight(3);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnRight5() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnRight(4);
        assertEquals(columns, dr.getColumns());
    }
}
