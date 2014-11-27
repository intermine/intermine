package org.intermine.web.struts;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.struts.action.*;
import org.apache.struts.config.*;
import org.intermine.api.*;
import org.intermine.api.results.*;
import org.intermine.metadata.*;
import org.intermine.pathquery.*;
import org.intermine.web.logic.results.*;
import org.junit.*;


public class AddColumnActionTest {

    @Test
    public void testExecute() throws Exception {
        // Mock objects.
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpSession session = createMock(HttpSession.class);
        InterMineAPI api = createMock(InterMineAPI.class);
        ServletContext servletContext = createMock(ServletContext.class);
        WebTable webTable = createMock(WebTable.class);

        // Real objects.
        @SuppressWarnings("deprecation")
        PagedTable table = new PagedTable(webTable);
        @SuppressWarnings("deprecation")
        Map<String, PagedTable> tableMap = new HashMap<String, PagedTable>();
        tableMap.put("y", table);
        Model model = Model.getInstanceByName("testmodel");
        PathQuery pathQuery = new PathQuery(model);
        pathQuery.addView("Employee.name");
        ActionMapping mapping = new ActionMapping();
        AddColumnAction action = new AddColumnAction();
        List<Path> columnsPath = new ArrayList<Path>();
        ForwardConfig conf = new ActionForward();
        conf.setName("results");
        conf.setPath("thataway");
        mapping.addForwardConfig(conf);

        // Mock behaviours.
        expect(request.getParameter("columnToAdd")).andReturn("Employee.age");
        expect(request.getSession()).andReturn(session).times(2);
        expect(request.getParameter("table")).andReturn("y");
        expect(request.getParameter("trail")).andReturn("zoom");
        expect(session.getAttribute("TABLE_MAP")).andReturn(tableMap);
        expect(webTable.getPathQuery()).andReturn(pathQuery).times(2);
        expect(session.getServletContext()).andReturn(servletContext);
        expect(servletContext.getAttribute("INTERMINE_API")).andReturn(api);
        expect(api.getModel()).andReturn(model);
        expect(webTable.getColumnsPath()).andReturn(columnsPath);
        webTable.addColumns(Arrays.asList(new Path(model, "Employee.age")));

        // Move to replay mode.
        replay(request);
        replay(session);
        replay(webTable);
        replay(api);
        replay(servletContext);

        ActionForward fwd = action.execute(mapping, null, request, null);

        // Check all the mocks.
        verify(request);
        verify(session);
        verify(webTable);
        verify(api);
        verify(servletContext);

        assertEquals(Arrays.asList("Employee.name", "Employee.age"), pathQuery.getView());
        assertEquals("thataway?table=y&page=0&trail=zoom", fwd.getPath());
    }

}
