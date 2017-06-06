/**
 *
 */
package org.intermine.template;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;
import org.junit.Test;


/**
 * @author sc
 *
 */
public class TemplateComparatorTest extends TestCase{

    private static final String TEMPLATE_NAME = "TEST_TEMPLATE";
    private static final String TEMPLATE_TITLE = "TEMPLATE --> TESTING";
    private static final String COMMENT = "A TEMPLATE THAT WE ARE USING FOR TESTING";


    private static final String ATEMPLATE_NAME = "ANOTHER_TEST_TEMPLATE";
    private static final String ATEMPLATE_TITLE = "TEMPLATE --> TESTING";
    private static final String ACOMMENT = "ANOTHER TEMPLATE THAT WE ARE USING FOR TESTING";

    @Test
    public static void testComparison() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery pq = new PathQuery(model);

        TemplateComparator comparator = new TemplateComparator();
        TemplateQuery template = new TemplateQuery(TEMPLATE_NAME, TEMPLATE_TITLE, COMMENT, pq);
        TemplateQuery anotherTemplate = new TemplateQuery(ATEMPLATE_NAME, ATEMPLATE_TITLE, ACOMMENT, pq);

        int comparison = comparator.compare(template, anotherTemplate);
        assertTrue(comparison >1);

    }
}
