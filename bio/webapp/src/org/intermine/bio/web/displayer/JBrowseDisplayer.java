package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Collection;
import java.util.Properties;
import org.intermine.api.InterMineAPI;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.model.genomic.Gene;
import org.intermine.model.genomic.SequenceFeature;

/**
 * Displayer for JBrowse
 * @author rns
 */
public class JBrowseDisplayer extends ReportDisplayer
{

    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public JBrowseDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        Properties props = InterMineContext.getWebProperties():
        SequenceFeature sf = (SequenceFeature) reportObject.getObject();
        String jbrowseInstall = props.getProperty("jbrowse.install.url");
        String chr = sf.getChromosomeLocation().getLocatedOn().getPrimaryIdentifier();
        Integer offset = Integer.valueOf(sf.getLength() * 0.1f);
        Integer start = sf.getChromosomeLocation().getStart() - offset;
        Integer end = sf.getChromosomeLocation().getEnd() - offset;
        String project = props.getProperty("project.title");
        String base = props.getProperty("webapp.baseUrl");
        String path = props.getProperty("webapp.path");
        String taxonId = String.valueOf(sf.getOrganism().getTaxonId());
        String data = String.format("%s/%s/service/jbrowse/config/%s", base, path, taxonId);
        Collection<String> tracks = new HashSet<String>();
        tracks.add(String.format("%s-%s-%s", project, taxonId, reportObject.getClassDescriptor().getUnqualifiedName()));
        if (!sf instanceof Gene) {
            tracks.add(String.format("%s-%s-Gene", project, taxonId));
        }
        String segment = String.format("%s:%d..%d", chr, start, end);

        request.setAttribute("segment", segment);
        request.setAttribute("jbrowseInstall", jbrowseInstall);
        request.setAttribute("dataLoc", URLEncoder.encode(data));
        request.setAttribute("tracks", StringUtils.join(",", tracks));

    }
}
