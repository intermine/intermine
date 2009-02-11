package org.intermine.webservice.client.services;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;

import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.TabTableResult;
import org.intermine.webservice.client.core.Request.RequestType;
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
     * Use {@link ServiceFactory} instead of constructor for creating this service.
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
                addParameter("constraint" + index, par.getPathId());
                addParameter("op" + index, par.getOperation());
                addParameter("value" + index, par.getValue());
                if (par.getExtraValue() != null) {
                    addParameter("extra" + index, par.getExtraValue());
                }
                if (par.getCode() != null) {
                    addParameter("code" + index, par.getCode());
                }
            }
        }
    }
    
    /**
     * Returns template results for given parameters. If you expect a lot of results 
     * use getResultIterator() method.  
     * @param templateName template name
     * @param parameters parameters of template
     * @param maxCount maximum number of returned results 
     * @see TemplateService
     * @return results
     */
    public List<List<String>> getResult(String templateName, List<TemplateParameter> parameters, 
            int maxCount) {
        TemplateRequest request = new TemplateRequest(RequestType.POST, getUrl(), 
                ContentType.TEXT_TAB);
        request.setMaxCount(maxCount);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        System.out.println(request);
        HttpConnection connection = executeRequest(request);
        return new TabTableResult(connection).getData();
    } 
    
    /**
     * Returns template results for given parameters. Use this method if you expects a lot 
     * of results and you would run out of memory.   
     * @param templateName template name
     * @param parameters parameters of template
     * @param maxCount maximum number of returned results 
     * @see TemplateService
     * @return results
     */
    public Iterator<List<String>> getResultIterator(String templateName, 
            List<TemplateParameter> parameters, int maxCount) {
        TemplateRequest request = new TemplateRequest(RequestType.POST, getUrl(), 
                ContentType.TEXT_TAB);
        request.setMaxCount(maxCount);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        HttpConnection connection = executeRequest(request);
        return new TabTableResult(connection).getIterator();
    }     
}
