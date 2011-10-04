package org.intermine.bio.webservice;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.bio.web.export.SequenceExporter;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.export.Exporter;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.exceptions.InternalErrorException;

public class GenomicRegionFastaService extends AbstractRegionExportService {

    protected static final String SUFFIX = ".fasta";

    public GenomicRegionFastaService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void export(PathQuery pq, Profile profile) {
        int index = 0;
        Exporter exporter;
        try {
            ObjectStore objStore = im.getObjectStore();
            exporter = new SequenceExporter(objStore, os, index, im.getClassKeys());
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
