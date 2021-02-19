package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.mines.FriendlyMineManager;
import org.intermine.api.mines.Mine;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ServiceException;

import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import static org.intermine.web.context.InterMineContext.getInterMineAPI;

/**
 * Exports selected web.properties.
 *
 * @author sc
 */
public class OtherMinesService extends JSONService
{
    protected static final Logger LOG = Logger.getLogger(OtherMinesService.class);

    /** @param im The InterMine state object. **/
    public OtherMinesService(InterMineAPI im) {
        super(im);
    }

    static final String JSON_KEY = "links";

    @SuppressWarnings("serial")
    private class ConfigMap extends HashMap<String, Object>
    {
        // empty
    }

    @Override
    protected void execute() throws Exception {

        ReportObject reportObject = null;
        InterMineObject imo = null;

        InterMineAPI im = getInterMineAPI();
        ObjectStore os = im.getObjectStore();
        Model model = im.getModel();

        Integer interMineID = Integer.valueOf(request.getParameter("id"));
        imo = im.getObjectStore().getObjectById(interMineID);
        if (imo == null) {
            // returns error (and empty string)
            addResultEntry(JSON_KEY, StringUtils.EMPTY, false);
            throw new ServiceException("no object with ID " + interMineID, 400);
        } else {
            //String pid = String.valueOf(imo.getFieldValue("primaryIdentifier"));

            // TODO: use instead?
            // type = DynamicUtil.getSimpleClass(imo).getSimpleName();
            Set<ClassDescriptor> classDescriptors;
            classDescriptors = model.getClassDescriptorsForClass(imo.getClass());

            ClassDescriptor gene = model.getClassDescriptorByName("Gene");

            Map<String, OtherMinesService.ConfigMap> linkConfigs =
                    new HashMap<String, OtherMinesService.ConfigMap>();

            Properties props =
                    (Properties) request.getServletContext().getAttribute(Constants.WEB_PROPERTIES);

            final FriendlyMineManager linkManager = FriendlyMineManager.getInstance(im, props);
            Collection<Mine> mines = linkManager.getFriendlyMines();
            Mine localMine = linkManager.getLocalMine();

//            Collection<PartnerLink> flinks =
//                    FriendlyMineManager.getInstance(im, props).getLinks("RatMine", interMineID);
            OtherMinesService.ConfigMap config;

            for (Mine m : mines) {
                String name = m.getName();
                if (linkConfigs.containsKey(name)) {
                    config = linkConfigs.get(name);
                } else {
                    config = new OtherMinesService.ConfigMap();
                    config.put("url", m.getUrl());
                    config.put("fgCol", m.getFrontcolor());
                    config.put("bgCol", m.getBgcolor());
                    config.put("default", m.getDefaultValue());
                    config.put("logo", m.getLogo());
                    linkConfigs.put(name, config);
                }
            }

            addResultEntry("otherMines", linkConfigs, true);
            addResultEntry("localMine", localMine.getName(), true);
            addResultEntry("mayHaveLinks", classDescriptors.contains(gene), false);

        }
    }
}
