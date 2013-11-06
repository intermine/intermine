package org.intermine.webservice.server.idresolution;

import java.util.Map;

import org.intermine.api.bag.BagQueryResult;

public interface BagResultFormatter {

    public abstract Map<String, Object> format(BagQueryResult bqr);

}