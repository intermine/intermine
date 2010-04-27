package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.api.template.TemplateManager;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.InternalErrorException;

/**
 * Fetch the names of public template queries for use with the Templates web service.
 * @author Richard Smith
 */
public class AvailableTemplatesService extends WebService {

	private static final String ENDL = System.getProperty("line.separator");

	
	public AvailableTemplatesService(InterMineAPI im) {
        super(im);
    }
	
	@Override
	protected void execute(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		TemplateManager templateManager = im.getTemplateManager();
		Set<String> templateNames = 
			new TreeSet<String>(templateManager.getGlobalTemplates().keySet());
        try {
        	for (String templateName : templateNames) {
        		response.getWriter().append(templateName + ENDL);
        	}
        } catch (IOException e) {
            throw new InternalErrorException(e);
        }
	}
}
