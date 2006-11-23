package org.flymine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.io.FastaFormat;
import org.biojava.bio.seq.io.SeqIOTools;
import org.flymine.biojava.FlyMineSequence;
import org.flymine.biojava.FlyMineSequenceFactory;
import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.Sequence;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.web.Constants;
import org.intermine.web.InterMineAction;
import org.intermine.web.SessionMethods;
import org.intermine.web.TableExporter;
import org.intermine.web.results.Column;
import org.intermine.web.results.PagedTable;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * An implementation of TableExporter that exports sequence objects using the BioJava sequence and
 * feature writers.
 *
 * @author Kim Rutherford
 */
public class SequenceExporter extends InterMineAction implements TableExporter
{
    /**
     * This action is invoked directly to export LocatedSequenceFeatures.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ObjectStore os =
            (ObjectStore) session.getServletContext().getAttribute(Constants.OBJECTSTORE);
        FlyMineSequence flyMineSequence = null;
        
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ",
                           "inline; filename=sequence" + StringUtil.uniqueString() + ".txt");
        
        InterMineObject obj = os.getObjectById(new Integer(request.getParameter("object")));
        
        if (obj instanceof Sequence) {
            Sequence sequence = (Sequence) obj;
            obj = ResidueFieldExporter.getLocatedSequenceFeatureForSequence(os, sequence);
            if (obj == null) {
                obj = ResidueFieldExporter.getProteinForSequence(os, sequence);
            }
        }
        if (obj instanceof LocatedSequenceFeature || obj instanceof Protein) {
            if (obj instanceof LocatedSequenceFeature) {
                flyMineSequence = FlyMineSequenceFactory.make((LocatedSequenceFeature) obj);
            } else {
                flyMineSequence = FlyMineSequenceFactory.make((Protein) obj);
            }
            if (flyMineSequence == null) {
                return null;
            }
            Annotation annotation = flyMineSequence.getAnnotation();
            BioEntity bioEntity = (BioEntity) obj;
            String identifier = bioEntity.getIdentifier();
            if (identifier == null) {
                identifier = bioEntity.getName();
                if (identifier == null) {
                    if (bioEntity instanceof Gene) {
                        Gene gene = ((Gene) bioEntity);
                        identifier = gene.getOrganismDbId();
                        if (identifier == null) {
                            identifier = gene.getAccession();
                            if (identifier == null) {
                                identifier = "[no_identifier]";
                            }
                        }
                    }
                }
            }
            annotation.setProperty(FastaFormat.PROPERTY_DESCRIPTIONLINE, identifier);
            OutputStream out = response.getOutputStream();
            SeqIOTools.writeFasta(out, flyMineSequence);
        }
        
        return null;
    }
    
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
        ObjectStore os =
            (ObjectStore) session.getServletContext().getAttribute(Constants.OBJECTSTORE);
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ",
                           "inline; filename=sequence" + StringUtil.uniqueString() + ".txt");

        OutputStream outputStream = null;

        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));

        List columns = pt.getColumns();

        // the first column that contains exportable features
        Column featureColumn = getFeatureColumn(pt);

        int realFeatureIndex = featureColumn.getIndex();

        int writtenSequencesCount = 0;

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

                FlyMineSequence flyMineSequence;

                if (object instanceof Sequence) {
                    Sequence sequence = (Sequence) object;
                    object =
                        ResidueFieldExporter.getLocatedSequenceFeatureForSequence(os, sequence);
                    if (object == null) {
                        // no LocatedSequenceFeature found
                        object = ResidueFieldExporter.getProteinForSequence(os, sequence);
                        if (object == null) {
                            // no Protein either
                            continue;
                        }
                    }
                }
                if (object instanceof LocatedSequenceFeature) {
                    LocatedSequenceFeature feature = (LocatedSequenceFeature) object;
                    flyMineSequence = FlyMineSequenceFactory.make(feature);
                } else {
                    if (object instanceof Protein) {
                        Protein protein = (Protein) object;
                        flyMineSequence = FlyMineSequenceFactory.make(protein);
                    } else {
                        // just ignore other objects
                        continue;
                    }
                }

                if (flyMineSequence == null) {
                    // the object doesn't have a sequence
                    continue;
                }

                Annotation annotation = flyMineSequence.getAnnotation();

                String headerString = header.toString();

                if (row.size() > 1 && headerString.length() > 0) {
                    annotation.setProperty(FastaFormat.PROPERTY_DESCRIPTIONLINE, headerString);
                } else {
                    if (object instanceof BioEntity) {
                        annotation.setProperty(FastaFormat.PROPERTY_DESCRIPTIONLINE,
                                               ((BioEntity) object).getIdentifier());
                    } else {
                        // last resort
                        annotation.setProperty(FastaFormat.PROPERTY_DESCRIPTIONLINE,
                                               "sequence_" + writtenSequencesCount);
                    }
                }

                if (outputStream == null) {
                    // try to avoid opening the OutputStream until we know that the query is
                    // going to work - this avoids some problems that occur when
                    // getOutputStream() is called twice (once by this method and again to
                    // write the error)
                    outputStream = response.getOutputStream();
                }
                SeqIOTools.writeFasta(outputStream, flyMineSequence);

                writtenSequencesCount++;
            }

            if (outputStream != null) {
                outputStream.close();
            }

            if (writtenSequencesCount == 0) {
                ActionErrors messages = new ActionErrors();
                ActionError error = new ActionError("errors.export.nothingtoexport");
                messages.add(ActionErrors.GLOBAL_ERROR, error);
                request.setAttribute(Globals.ERROR_KEY, messages);

                return mapping.findForward("results");
            }
        } catch (ObjectStoreException e) {
            ActionErrors messages = new ActionErrors();
            ActionError error = new ActionError("errors.query.objectstoreerror");
            messages.add(ActionErrors.GLOBAL_ERROR, error);
            request.setAttribute(Globals.ERROR_KEY, messages);
        }

        return null;
    }

    /**
     * @see org.intermine.web.TableExporter#canExport(PagedTable)
     */
    public boolean canExport(PagedTable pt) {
        return getFeatureColumn(pt) != null;
    }

    /**
     * Check whether the argument is one of the types we handle
     * @param type the type
     * @return true if we handle the type
     */
    protected boolean validType(Class type) {
        return (LocatedSequenceFeature.class.isAssignableFrom(type)
                || Protein.class.isAssignableFrom(type)
                || Sequence.class.isAssignableFrom(type));
    }

    /**
     * Return the first column that contains features that can be exported.  It first checks the
     * Column objects from the PagedTable.
     * 
     * If there are no LocatedSequenceFeature or Protein columns it checks the visible rows of the
     * Results objects and returns the first Column that contains a LocatedSequenceFeature or
     * Protein.  This is needed, for example, when the user select BioEntity and constrains the
     * identifier to "zen" - the Results contain only Genes, but the column type is BioEntity.
     *
     * If there is more than one LocatedSequenceFeature or Protein column, return null to prevent
     * confusion about which column is being exported.
     * @throws ObjectStoreException 
     */
    private Column getFeatureColumn(PagedTable pt) {
        List columns = pt.getColumns();

        Column returnColumn = null;
        
        // find and remember the first valid Sequence-containing column
        for (int i = 0; i < columns.size(); i++) {
            Column column = (Column) columns.get(i);
            if (column.isVisible()) {
                Object columnType = ((Column) columns.get(i)).getType();
                if (columnType instanceof ClassDescriptor) {
                    if (validType(((ClassDescriptor) columnType).getType())) {
                        if (returnColumn == null) {
                            returnColumn = column;
                        } else {
                            // there are two or more sequence columns
                            return null;
                        }
                    }
                }
            }
        }

        // search the visible rows for any validType()s
        List rowList = pt.getRows();

        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            Column thisColumn = (Column) columns.get(columnIndex);

            if (!thisColumn.isVisible()) {
                continue;
            }

            // the column order from PagedTable.getList() isn't necessarily the order that
            // the user has chosen for the columns
            int realColumnIndex = thisColumn.getIndex();

            Iterator rowListIter = rowList.iterator();

            while (rowListIter.hasNext()) {
                List row = (List) rowListIter.next();
          
                Object o = row.get(realColumnIndex);

                if (o != null && validType(o.getClass())) {
                    if (returnColumn == null) {
                        returnColumn = thisColumn;
                    } else {
                        // there are two or more sequence columns
                        return null;
                    }
                }
            }
        }
        
        return returnColumn;
    }
}
