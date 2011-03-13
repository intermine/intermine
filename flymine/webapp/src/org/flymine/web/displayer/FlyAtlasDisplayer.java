package org.flymine.web.displayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.output.JSONRowIterator;

public class FlyAtlasDisplayer extends CustomDisplayer {



    public FlyAtlasDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, DisplayObject displayObject) {

        PathQuery q = new PathQuery(im.getModel());
        q.addViews("Gene.microArrayResults.material.primaryIdentifier",
                "Gene.microArrayResults.tissue.name",
                "Gene.microArrayResults.affyCall",
                "Gene.microArrayResults.enrichment",
                "Gene.microArrayResults.mRNASignal",
                "Gene.microArrayResults.presentCall");
        Integer objectId = displayObject.getId();
        q.addConstraint(Constraints.eq("Gene.id", objectId.toString()));
        q.addConstraint(Constraints.type("Gene.microArrayResults", "FlyAtlasResult"));

        Profile profile = SessionMethods.getProfile(request.getSession());
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);

        StringBuilder output = new StringBuilder();

        // Using a JSONResultsIterator gave this error:
        //       <a href="mailto:support@flymine.org?body=I found this error on http://localhost:8080/flymine/layout.jsp.%0D%0A%0D%0A---- Error Found ----%0D%0A%0D%0Aorg.intermine.webservice.server.output.JSONFormattingException: This array is empty - is the view in the wrong order?
        //at org.intermine.webservice.server.output.JSONResultsIterator.setCurrentMapFromCurrentArray(JSONResultsIterator.java:358)
        //at org.intermine.webservice.server.output.JSONResultsIterator.addReferenceToCurrentNode(JSONResultsIterator.java:378)
        //at org.intermine.webservice.server.output.JSONResultsIterator.addReferencedCellToJsonMap(JSONResultsIterator.java:309)
        //at org.intermine.webservice.server.output.JSONResultsIterator.addCellToJsonMap(JSONResultsIterator.java:241)
        //at org.intermine.webservice.server.output.JSONResultsIterator.addRowToJsonMap(JSONResultsIterator.java:114)
        //at org.intermine.webservice.server.output.JSONResultsIterator.next(JSONResultsIterator.java:96)
        //at org.flymine.web.displayer.FlyAtlasDisplayer.display(FlyAtlasDisplayer.java:48)


        JSONRowIterator jsonIterator = new JSONRowIterator(executor.execute(q), im);
        while (jsonIterator.hasNext()) {
            Object next = jsonIterator.next();
            List<String> outputLine = new ArrayList<String>(Arrays.asList(next.toString()));
            if (jsonIterator.hasNext()) {
                outputLine.add("");
            }
            output.append(outputLine);
        }

        request.setAttribute("jsonresults", output);
    }

}
