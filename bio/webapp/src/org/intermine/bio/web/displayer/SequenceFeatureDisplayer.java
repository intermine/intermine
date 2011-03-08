package org.intermine.bio.web.displayer;

import javax.servlet.http.HttpServletRequest;

import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.DisplayObject;

public class SequenceFeatureDisplayer extends CustomDisplayer {

    public SequenceFeatureDisplayer(ReportDisplayerConfig config) {
        super(config);
    }
    
    @Override
    public void display(HttpServletRequest request, DisplayObject displayObject) {
    }
}
