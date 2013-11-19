package org.intermine.webservice.server.idresolution;

import java.util.Map;

public interface BagResultFormatter {

    public abstract Map<String, Object> format(Job job);

}
