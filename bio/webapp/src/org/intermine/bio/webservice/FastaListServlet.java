package org.intermine.bio.webservice;

import org.intermine.webservice.server.WebService;

public class FastaListServlet extends BioExportServlet
{
    private static final long serialVersionUID = -3833573628668268495L;

    @Override
    protected WebService getService() {
        return new FastaListService(api);
    }
}
