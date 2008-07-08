package org.intermine.webservice.client.services;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.TabTableResult;
import org.intermine.webservice.client.core.TableResult;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.template.TemplateParameter;
import org.intermine.webservice.client.util.HttpConnection;

/**
 * The TemplateService returns results of public templates and number of results. Template is 
 * predefined query with some parameters. These parameters are variables that enable parameterized
 * queries. For example the same template can be run for Drosophila organism or Caenorhabditis. 
 * It depends just at the specified organism parameter.  
 * 
 * @author Jakub Kulaviak
 **/
public class TemplateService extends Service 
{
    
    private static final String SERVICE_RELATIVE_URL = "template/results";
        
    /**
     * Use {@link ServiceFactory} for creating this service instead of constructor.
     * @param rootUrl root URL
     * @param applicationName application name
     */
    public TemplateService(String rootUrl, String applicationName) {
        super(rootUrl, SERVICE_RELATIVE_URL, applicationName);
    }
    
    private static class TemplateRequest extends RequestImpl 
    {

        public TemplateRequest(RequestType type, String serviceUrl, ContentType contentType) {
            super(type, serviceUrl, contentType);
        }

        public void setStart(int start) {
            setParameter("start", start + "");
        }
        
        public void setMaxCount(int maxCount) {
            setParameter("size", maxCount + "");
        }
        
        public void setName(String xml) {
            setParameter("name", xml);
        }
        
        public void setTemplateParameters(List<TemplateParameter> parameters) {
            for (int i = 0; i < parameters.size(); i++) {
                TemplateParameter par = parameters.get(i);
                int index = i + 1;
                addParameter("op" + index, par.getOperation());
                addParameter("value" + index, par.getValue());
            }
        }
    }

    /**
     * Returns count of template results for given parameters.  
     * @param templateName template name
     * @param parameters parameters of template 
     * @see TemplateService
     * @return count
     */
    public int getCount(String templateName, List<TemplateParameter> parameters) {
        TemplateRequest request = new TemplateRequest(RequestType.POST, getUrl(), 
                ContentType.TEXT_TAB);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        request.setParameter("tcount", "");
        HttpConnection connection = executeRequest(request);
        String body = connection.getResponseBodyAsString().trim();
        if (body.length() == 0) {
            throw new ServiceException("Service didn't return any result");
        }
        try {
            return Integer.parseInt(body);
        }  catch (NumberFormatException e) {
            throw new ServiceException("Service returned invalid result. It is not number: " 
                    + body, e);
        }        
    }
    
    /**
     * Returns template results for given parameters.  
     * @param templateName template name
     * @param parameters parameters of template
     * @param start index of first result, that should be returned
     * @param maxCount maximum number of returned results 
     * @see TemplateService
     * @return results
     */
    public TableResult getResult(String templateName, List<TemplateParameter> parameters, 
            int start, int maxCount) {
        TemplateRequest request = new TemplateRequest(RequestType.POST, getUrl(), 
                ContentType.TEXT_TAB);
        request.setStart(start);
        request.setMaxCount(maxCount);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        HttpConnection connection = executeRequest(request);
        return new TabTableResult(connection);
    } 
}
