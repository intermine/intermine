package org.flymine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.OutputStream;

import java.util.List;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.Globals;

import org.biojava.bio.seq.io.FastaFormat;
import org.biojava.bio.seq.io.SeqIOTools;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.util.StringUtil;
import org.intermine.web.Constants;
import org.intermine.web.TableExporter;
import org.intermine.web.results.Column;
import org.intermine.web.results.PagedTable;
import org.intermine.model.InterMineObject;

import org.flymine.model.genomic.Contig;
import org.flymine.web.biojava.FlyMineSequence;
import org.flymine.web.biojava.FlyMineSequenceFactory;

/**
 * An implementation of TableExporter that exports sequence objects using the BioJava sequence and
 * feature writers.
 *
 * @author Kim Rutherford
 */
public class SequenceExporter implements TableExporter
{
    /**
     * Method called to export a PagedTable object using the BioJava sequence and feature writers.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward export(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ",
                           "inline; filename=sequence" + StringUtil.uniqueString() + ".txt");

        OutputStream outputStream = response.getOutputStream();

        PagedTable pt = (PagedTable) session.getAttribute(Constants.RESULTS_TABLE);

        List columns = pt.getColumns();

        Column featureColumn = null;

        // find and remember the first valid column
        for (int i = 0; i < columns.size(); i++) {
            Column column = (Column) columns.get(i);
            if (column.isVisible()) {
                Object columnType = ((Column) columns.get(i)).getType();
                if (columnType instanceof ClassDescriptor) {
                    if (validType(((ClassDescriptor) columnType).getType())) {
                        featureColumn = column;
                        break;
                    }
                }
            }
        }
        
        int realFeatureIndex = featureColumn.getIndex();

        try {
            List rowList = pt.getAllRows();

            for (int rowIndex = 0;
                 rowIndex < rowList.size() && rowIndex <= pt.getMaxRetrievableIndex();
                 rowIndex++) {
                List row;
                try {
                    row = (List) rowList.get(rowIndex);
                } catch (RuntimeException e) {
                    // re-throw as a more specific exception
                    if (e.getCause() instanceof ObjectStoreException) {
                        throw (ObjectStoreException) e.getCause();
                    } else {
                        throw e;
                    }
                }

                InterMineObject object = (InterMineObject) row.get(realFeatureIndex);

                StringBuffer header = new StringBuffer();

                for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                    Column thisColumn = (Column) columns.get(columnIndex);

                    if (!thisColumn.isVisible()) {
                        continue;
                    }
                    
                    // the column order from PagedTable.getList() isn't necessarily the order that
                    // the user has chosen for the columns
                    int realColumnIndex = thisColumn.getIndex();

                    if (realColumnIndex == realFeatureIndex) {
                        // skip - this is the column containing the valid type
                        continue;
                    }

                    header.append(row.get(realColumnIndex));
                    header.append(" ");
                }

                FlyMineSequence sequence = null;

                if (object instanceof Contig) {
                    Contig feature = (Contig) object;
                    if (feature.getResidues() != null) {
                        sequence = FlyMineSequenceFactory.make(feature);
                    }
                } //else {
//                     Protein protein = (Protein) object;
//                     if (protein.getAminoAcids() != null) {
//                         sequence = FlyMineSequenceFactory.make(protein);
//                     }
//                 }

                if (sequence != null) {
                    if (row.size() > 1) {
                        sequence.getAnnotation().setProperty(FastaFormat.PROPERTY_DESCRIPTIONLINE,
                                                             header.toString());
                    }

                    SeqIOTools.writeFasta(outputStream, sequence);
                }
            }

            outputStream.close();
        } catch (ObjectStoreException e) {
            ActionErrors messages = new ActionErrors();
            ActionError error = new ActionError("errors.query.objectstoreerror");
            messages.add(ActionErrors.GLOBAL_ERROR, error);
            request.setAttribute(Globals.ERROR_KEY, messages);
        }

        return null;
    }

    /**
     * @see org.intermine.web.TableExporter#canExport
     */
    public boolean canExport(PagedTable pt) {
        List columns = pt.getColumns();
        int sequenceCount = 0;

        for (int i = 0; i < columns.size(); i++) {
            Column column = (Column) columns.get(i);
            if (column.isVisible()) {
                Object columnType = ((Column) columns.get(i)).getType();
                
                if (columnType instanceof ClassDescriptor) {
                    ClassDescriptor cd = (ClassDescriptor) columnType;
                    if (validType(cd.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check whether the argument is one of the types we handle
     * @param type the type
     * @return true if we handle the type
     */
    protected boolean validType(Class type) {
        return Contig.class.isAssignableFrom(type);
    }
}
