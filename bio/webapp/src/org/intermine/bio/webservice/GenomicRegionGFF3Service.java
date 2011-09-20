package org.intermine.bio.webservice;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.bio.web.export.GFF3Exporter;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.Exporter;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.exceptions.InternalErrorException;

public class GenomicRegionGFF3Service extends AbstractRegionExportService {

    protected static final String SUFFIX = ".gff3";

    public GenomicRegionGFF3Service(InterMineAPI im) {
        super(im);
    }
    
    protected void export(PathQuery pq, Profile profile) {
        Exporter exporter;
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();        // get the project title to be written in GFF3 records
        Properties props = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String sourceName = props.getProperty("project.title");
        Set<Integer> organisms = null;
        try {
            List<Integer> indexes = new ArrayList<Integer>();
            List<String> viewColumns = new ArrayList<String>(pq.getView());
            for (int i = 0; i < viewColumns.size(); i++) {
                indexes.add(Integer.valueOf(i));
            }

            GFFQueryService.removeFirstItemInPaths(viewColumns);
            exporter = new GFF3Exporter(pw, indexes,
                    GFFQueryService.getSoClassNames(servletContext), viewColumns,
                    sourceName, organisms, false);
            ExportResultsIterator iter = null;
            try {
                PathQueryExecutor executor = this.im.getPathQueryExecutor(profile);
                iter = executor.execute(pq, 0, WebServiceRequestParser.DEFAULT_MAX_COUNT);
                iter.goFaster();
                exporter.export(iter);
            } finally {
                if (iter != null) {
                    iter.releaseGoFaster();
                }
            }
        } catch (Exception e) {
            throw new InternalErrorException("Service failed:" + e, e);
        }
    }
}
