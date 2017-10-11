package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.export.SequenceExporter;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.export.Exporter;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * A service for exporting query results as fasta.
 * @author Alexis Kalderimis.
 *
 */
public class FastaQueryService extends BioQueryService
{
    private static final String EXT = "extension";
    private static final String TOO_MANY_COLUMNS =
            "Queries for this webservice may only have one output column";
    private static final int COLUMN = 0;

    /**
     * Constructor.
     * @param im A reference to an InterMine API settings bundle.
     */
    public FastaQueryService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getSuffix() {
        return ".fa";
    }

    @Override
    protected String getContentType() {
        return "text/x-fasta";
    }

    @Override
    protected Exporter getExporter(PathQuery pq) {
        int extension = parseExtension(getOptionalParameter(EXT, "0"));
        ObjectStore objStore = im.getObjectStore();
        return new SequenceExporter(objStore, getOutputStream(), COLUMN,
                im.getClassKeys(), extension, getQueryPaths(pq));
    }

    /**
     * Make the path-query to run, and check it has the right number of columns.
     * @return A suitable pathquery for getting FASTA data from.
     */
    @Override
    protected PathQuery getQuery() {
        PathQuery pq = super.getQuery();

        if (pq.getView().size() > 1) {
            throw new BadRequestException(TOO_MANY_COLUMNS);
        }

        return pq;
    }

    /**
     * A method for parsing the value of the extension parameter. Static and protected for
     * testing purposes.
     *
     * @param extension The extension as provided by the user.
     * @return An integer representing the number of base pairs.
     * @throws BadRequestException If there is a problem interpreting the extension string.
     */
    protected static int parseExtension(final String extension) throws BadRequestException {
        if (StringUtils.isBlank(extension)) {
            return 0;
        }
        final String ext = extension.toLowerCase().trim();

        if (!ext.matches("^((\\d+)|(\\d+(\\.\\d+)?(k|m)))(bp?)?$")) {
            throw new BadRequestException("Illegal extension format: " + ext);
        }

        final String justTheNumber = ext.replaceAll("[kmbp]", "");
        final int scale = (ext.contains("k") ? 1000 : ext.contains("m") ? 1000000 : 1);
        final float number;
        try {
            number = Float.parseFloat(justTheNumber) * scale;
        } catch (NumberFormatException e) {
            throw new BadRequestException("Illegal number: " + justTheNumber, e);
        }
        if (number < 0) {
            throw new BadRequestException("Negative extensions are not allowed.");
        }
        if (number != Math.ceil(number)) {
            throw new BadRequestException("The extension must be a whole number of base pairs. "
                    + "I got: " + number + "bp");
        }
        return Math.round(number);
    }


    @Override
    protected void checkPathQuery(PathQuery pq) throws Exception {
        if (pq.getView().size() > 1) {
            throw new BadRequestException("Queries to this service may only have one view.");
        }
        Path path = pq.makePath(pq.getView().get(0));
        ClassDescriptor klazz = path.getLastClassDescriptor();
        ClassDescriptor sf = im.getModel().getClassDescriptorByName("SequenceFeature");
        ClassDescriptor protein = im.getModel().getClassDescriptorByName("Protein");
        if (sf == klazz || protein == klazz || klazz.getAllSuperDescriptors().contains(sf)
                || klazz.getAllSuperDescriptors().contains(protein)) {
            return; // OK
        } else {
            throw new BadRequestException("Unsuitable type for export: " + klazz);
        }
    }
}
