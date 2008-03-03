package org.intermine.webservice.lists;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.webservice.WebService;
import org.intermine.webservice.WebServiceConstants;
import org.intermine.webservice.WebServiceException;
import org.intermine.webservice.core.ListManager;
import org.intermine.webservice.output.HTMLTable;
import org.intermine.webservice.output.MemoryOutput;
import org.intermine.webservice.output.Output;


/**
 * Web service that returns all public lists (bags) that contain object with
 * specified id.
 * See {@link ListsRequestProcessor} for parameter description
 * @author Jakub Kulaviak
 **/
public class ListsService extends WebService
{
    
    /**
     * Executes service specific logic. 
     * @param request request
     * @param response response
     */
    @Override
    protected void execute(HttpServletRequest request, HttpServletResponse response) {
        
        ListsServiceInput input = getInput();
        if (!validate(input)) {
            return;
        }
        
        List<String> listNames = new ListManager(request).getListsNames(input.getObjectId());
        for (String name : listNames) {
            List<String> result = new ArrayList<String>();
            result.add(name);
            output.addResultItem(result);
        }
        forward(input, output);
    }

    private void forward(ListsServiceInput input, Output output) {
        if (getFormat() == HTML_FORMAT) {
            MemoryOutput mout = (MemoryOutput) output;
            HTMLTable table = new HTMLTable();
            table.setRows(mout.getResults());
            List<String> columnNames = new ArrayList<String>();
            columnNames.add("List");
            table.setColumnNames(columnNames);
            table.setTitle("Lists with " + input.getObjectId());
            request.setAttribute(WebServiceConstants.HTML_TABLE_ATTRIBUTE, table);
            try {
                getHtmlForward().forward(request, response);
            } catch (Exception e) {
                throw new WebServiceException(WebServiceConstants.SERVICE_FAILED_MSG, e);
            } 
        }
    }

    private ListsServiceInput getInput() {
        return new ListsRequestProcessor(request).getInput();
    }
}
