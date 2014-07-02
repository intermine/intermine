/**
 *
 */
package org.intermine.template;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;
import org.junit.Before;
import org.junit.Test;


/**
 * @author sergio
 *
 */
public class TemplateComparatorTest extends TestCase{


    private static final String TEMPLATE_NAME = "TEST_TEMPLATE";
    private static final String TEMPLATE_TITLE = "TEMPLATE --> TESTING";
    private static final String COMMENT = "A TEMPLATE THAT WE ARE USING FOR TESTING";


    private static final String ATEMPLATE_NAME = "ANOTHER_TEST_TEMPLATE";
    private static final String ATEMPLATE_TITLE = "TEMPLATE --> TESTING";
    private static final String ACOMMENT = "ANOTHER TEMPLATE THAT WE ARE USING FOR TESTING";

    static TemplateQuery template, anotherTemplate;

    @Before
    public void setup() {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery pq = new PathQuery(model);

        template = new TemplateQuery(TEMPLATE_NAME, TEMPLATE_TITLE, COMMENT, pq);
        anotherTemplate = new TemplateQuery(ATEMPLATE_NAME, ATEMPLATE_TITLE, ACOMMENT, pq);

    }
    @Test
    public static void testComparison() throws Exception {

        TemplateComparator comparator = new TemplateComparator();
        int comparison = comparator.compare(template, anotherTemplate);
        // it should return template name, with anotherTemplate coming first
        assertTrue(comparison >1);

    }
}
