package org.intermine.web.struts;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.config.ForwardConfig;
import org.intermine.api.InterMineAPI;
import org.intermine.api.results.WebTable;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.results.PagedTable;
import org.junit.Test;


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
