package org.intermine.webservice.server.idresolution;

import java.util.Map;

import org.intermine.api.idresolution.Job;

public interface BagResultFormatter {

    public abstract Map<String, Object> format(Job job);

}
