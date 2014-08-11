package org.intermine.web.struts;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForward;
import org.junit.Before;
import org.junit.Test;

public class AvailableColumnsControllerTest {

    private AvailableColumnsController controller;

    private static final String QUERY_XML = "<query model=\"testmodel\" view=\"Employee.name\"/>";

    @Before
    public void setup() {
        this.controller = new AvailableColumnsController();
    }

    @Test
    public void testExecute() throws Exception {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        Map<String, String> expectedColumns = new HashMap<String, String>();
        expectedColumns.put("Employee.fullTime", "Employee&nbsp;&gt; fullTime");
        expectedColumns.put("Employee.age", "Employee&nbsp;&gt; age");
        expectedColumns.put("Employee.end", "Employee&nbsp;&gt; end");
        HttpSession session = null;
        expect(request.getSession()).andReturn(session);
        expect(request.getAttribute("queryXML")).andReturn(QUERY_XML);
        expect(request.getAttribute("table")).andReturn("FOO");
        request.setAttribute("availableColumns", expectedColumns);
        replay(request);
        ActionForward fwd = controller.execute(null, null, request, null);
        assertNull(fwd);
        verify(request);
    }

}
