package org.intermine.webservice.server.template;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.TemplateManager;
import org.intermine.template.TemplateQuery;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;

public class SingleTemplateService extends JSONService {

    public SingleTemplateService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws ServiceException {
        String name = StringUtils.defaultString(request.getPathInfo(), "");
        name = name.replaceAll("^/", "");
        if (StringUtils.isBlank(name)) {
            throw new BadRequestException("No name provided");
        }
        Profile p = getPermission().getProfile();
        TemplateManager tm = im.getTemplateManager();
        TemplateQuery t = tm.getUserOrGlobalTemplate(p, name);
        if (t == null) {
            throw new ResourceNotFoundException("No template found called " + name);
        }
        output.addResultItem(Arrays.asList(t.toJSON()));
    }

    @Override
    public String getResultsKey() {
        return "template";
    }

}
