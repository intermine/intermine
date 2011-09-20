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
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.lists.ListInput;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.TabFormatter;

public abstract class AbstractRegionExportService extends GenomicRegionSearchService {

    public AbstractRegionExportService(InterMineAPI im) {
        super(im);
    }

    @Override
    public boolean isAuthenticated() {
        // Allow anyone to use this service, even though it 
        // uses a list to do its dirty work.
        return true;
    }
    
    @Override
    protected void makeList(ListInput input, String type, Profile profile,
            Set<String> temporaryBagNamesAccumulator) throws Exception {
        // Delete the list on end.
        temporaryBagNamesAccumulator.add(input.getListName());
        GenomicRegionSearchListInput searchInput = (GenomicRegionSearchListInput) input;
        InterMineBag tempBag = doListCreation(searchInput, profile, type);
        
        PathQuery pq = makePathQuery(tempBag);
        export(pq, profile);
    }
    
    protected PathQuery makePathQuery(InterMineBag tempBag) {
        PathQuery pq = new PathQuery(im.getModel());
        pq.addView(tempBag.getType() + ".primaryIdentifier");
        pq.addConstraint(Constraints.in(tempBag.getType(), tempBag.getName()));
        return pq;
    }
    
    protected abstract void export(PathQuery pq, Profile profile);
    
    protected static String SUFFIX;
    
    @Override
    protected String getDefaultFileName() {
        return "results" + StringUtil.uniqueString() + SUFFIX;
    }

    
    protected PrintWriter pw;
    protected OutputStream os;
    
    @Override
    protected Output getDefaultOutput(PrintWriter out, OutputStream os) {
        this.pw = out;
        this.os = os;
        output = new StreamedOutput(out, new TabFormatter());
        if (isUncompressed()) {
            ResponseUtil.setPlainTextHeader(response,
                    getDefaultFileName());
        }
        return output;
    }

    @Override
    public int getFormat() {
        return UNKNOWN_FORMAT;
    }


}
