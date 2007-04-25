package org.flymine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.biojava.bio.Annotation;
import org.biojava.bio.seq.io.FastaFormat;
import org.biojava.bio.seq.io.SeqIOTools;
import org.flymine.biojava.FlyMineSequence;
import org.flymine.biojava.FlyMineSequenceFactory;
import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.Sequence;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.path.Path;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.TableExporter;
import org.intermine.web.logic.results.Column;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;
import org.intermine.web.struts.WebCollection;

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
                            try {
                                identifier = (String) TypeUtil.getFieldValue(gene, "accession");
                            } catch (RuntimeException e) {
                                // ignore
                            }
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

        // the first column that contains exportable features
        Column featureColumn = getFeatureColumn(pt);

        int realFeatureIndex = featureColumn.getIndex();

        // IDs of the features we have successfully output - used to avoid duplicates
        Set exportedIDs = new HashSet();

        try {
            List rowList = pt.getAllRows();

            for (int rowIndex = 0;
                 rowIndex < rowList.size() && rowIndex <= pt.getMaxRetrievableIndex();
                 rowIndex++) {
                List row;
                try {
                    if (rowList instanceof WebResults) {
                        row = ((WebResults) rowList).getResultElements(rowIndex);
                    } else if (rowList instanceof WebCollection) {
                        row = ((WebCollection) rowList).getResultElements(rowIndex);
                    } else {
                        row = (List) rowList.get(rowIndex);
                    }
                } catch (RuntimeException e) {
                    // re-throw as a more specific exception
                    if (e.getCause() instanceof ObjectStoreException) {
                        throw (ObjectStoreException) e.getCause();
                    } else {
                        throw e;
                    }
                }

                StringBuffer header = new StringBuffer();

                Object resultObject = row.get(realFeatureIndex);
                
                ResultElement resultElement;
                if (resultObject instanceof ResultElement) {
                    resultElement = (ResultElement) resultObject;
                } else {
                    // ignore other objects
                    continue;
                }

                FlyMineSequence flyMineSequence;

                Object object = os.getObjectById(resultElement.getId());
                
                if (!(object instanceof InterMineObject)) {
                    continue;
                }

                Integer objectId = ((InterMineObject) object).getId();
                if (exportedIDs.contains(objectId)) {
                    // exported already
                    continue;
                }
                
                if (object instanceof LocatedSequenceFeature) {
                    LocatedSequenceFeature feature = (LocatedSequenceFeature) object;
                    flyMineSequence = FlyMineSequenceFactory.make(feature);
                    if (feature.getIdentifier() == null) {
                        if (feature instanceof Gene) {
                            header.append(((Gene) feature).getOrganismDbId());
                        } else {
                            header.append("[unknown_identifier]");
                        }
                    } else {
                        header.append(feature.getIdentifier());
                    }
                    header.append(' ');
                    if (feature.getName() == null) {
                        header.append("[unknown_name]");
                    } else {
                        header.append(feature.getName());
                    }
                    if (feature.getChromosomeLocation() != null) {
                        header.append(' ');
                        header.append(feature.getChromosome().getIdentifier());
                        header.append(':');
                        header.append(feature.getChromosomeLocation().getStart());
                        header.append('-');
                        header.append(feature.getChromosomeLocation().getEnd());
                        header.append(' ');
                        header.append(feature.getLength());
                    }
                    try {
                        Gene gene = (Gene) TypeUtil.getFieldValue(feature, "gene");
                        if (gene != null) {
                            String geneIdentifier = gene.getIdentifier();
                            if (geneIdentifier != null) {
                                header.append(' ');
                                header.append("gene:");
                                header.append(geneIdentifier);
                            }
                        }
                    } catch (IllegalAccessException e) {
                        // ignore
                    }
                } else {
                    if (object instanceof Protein) {
                        Protein protein = (Protein) object;
                        flyMineSequence = FlyMineSequenceFactory.make(protein);
                        header.append(protein.getIdentifier());
                        header.append(' ');
                        if (protein.getName() == null) {
                            header.append("[unknown_name]");
                        } else {
                            header.append(protein.getName());
                        }
                        Iterator iter = protein.getGenes().iterator();
                        while (iter.hasNext()) {
                            Gene gene = (Gene) iter.next();
                            String geneIdentifier = gene.getIdentifier();
                            if (geneIdentifier != null) {
                                header.append(' ');
                                header.append("gene:");
                                header.append(geneIdentifier);
                            }

                        }
                    } else {
                        // ignore other objects
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
                                               "sequence_" + exportedIDs.size());
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

                exportedIDs.add(objectId);
            }

            if (outputStream != null) {
                outputStream.close();
            }

            if (exportedIDs.size() == 0) {
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
     * @see org.intermine.web.logic.export.TableExporter#canExport(PagedTable)
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

    private boolean validType(String className) {
        try {
            if (className.indexOf('.') == -1) {
                // FIXME this is a HACK - the ResultElement should include type package name
                return validType(Class.forName("org.flymine.model.genomic." + className));
            } else {
                return validType(Class.forName(className));
            }
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private boolean validType(InterMineObject imo) {
        Set classes = DynamicUtil.decomposeClass(imo.getClass());
        Iterator iter = classes.iterator();
        while (iter.hasNext()) {
            if (validType((Class) iter.next())) {
                return true;
            }
        }
        return false;
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
                Path columnPath = column.getPath();
                if (columnPath != null && columnPath.endIsAttribute()) {
                    ClassDescriptor columnCD = columnPath.getLastClassDescriptor();
                    if (validType(columnCD.getName())) {
                        if (returnColumn == null) {
                            returnColumn = column;
                        } else {
                            // there are two or more sequence columns
                            return null;
                        }
                    }
                }

                // only consider the first visible column
                break;
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
          
                if (realColumnIndex < row.size()) {
                    Object o = row.get(realColumnIndex);

                    if (o != null && o instanceof ResultElement) {
                        ResultElement resultElement = (ResultElement) o;
                        InterMineObject imo;
                        try {
                            imo = resultElement.getInterMineObject();
                        } catch (ObjectStoreException e) {
                            // give up 
                            return null;
                        }
                        
                        if (resultElement.isKeyField() && validType(imo)) {
                            return thisColumn;
                        }
                    }
                }
            }
        }
        
        return returnColumn;
    }
}
