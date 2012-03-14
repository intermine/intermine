package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.biojava.bio.Annotation;
import org.biojava.bio.seq.io.FastaFormat;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.utils.ChangeVetoException;
import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.biojava.BioSequence;
import org.intermine.bio.web.biojava.BioSequenceFactory;
import org.intermine.bio.web.biojava.BioSequenceFactory.SequenceType;
import org.intermine.bio.web.export.ResidueFieldExporter;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Sequence;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;

/**
 * Exports sequence.
 *
 * @author Kim Rutherford
 */
public class SequenceExportAction extends InterMineAction
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SequenceExportAction.class);

    /**
     * This action is invoked directly to export SequenceFeatures.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ObjectStore os = im.getObjectStore();
        BioSequence bioSequence = null;

        //SequenceHttpExporter.setSequenceExportHeader(response);

        Properties webProps = (Properties) session.getServletContext().
            getAttribute(Constants.WEB_PROPERTIES);
        Integer objectId = new Integer(request.getParameter("object"));
        InterMineObject obj = getObject(os, webProps, objectId);

        if (obj instanceof SequenceFeature || obj instanceof Protein) {
            bioSequence = createBioSequence(obj);

            response.setContentType("text/plain");
            if (bioSequence != null) {
                OutputStream out = response.getOutputStream();
                SeqIOTools.writeFasta(out, bioSequence);
            } else {
                PrintWriter out = response.getWriter();
                out.write("Sequence information not availble for this sequence feature...");
                out.flush();
            }
        }

        return null;
    }

    private BioSequence createBioSequence(InterMineObject obj)
        throws IllegalSymbolException, IllegalAccessException, ChangeVetoException {
        BioSequence bioSequence;
        BioEntity bioEntity = (BioEntity) obj;
        bioSequence = BioSequenceFactory.make(bioEntity, SequenceType.DNA);
        if (bioSequence == null) {
            return null;
        }
        Annotation annotation = bioSequence.getAnnotation();
        // try hard to find an identifier
        String identifier = bioEntity.getPrimaryIdentifier();
        if (identifier == null) {
            identifier = bioEntity.getSecondaryIdentifier();
            if (identifier == null) {
                identifier = bioEntity.getName();
                if (identifier == null) {
                    try {
                        identifier = (String) bioEntity.getFieldValue("primaryAccession");
                    } catch (RuntimeException e) {
                        // ignore
                    }
                    if (identifier == null) {
                        identifier = "[no_identifier]";
                    }
                }
            }
        }
        annotation.setProperty(FastaFormat.PROPERTY_DESCRIPTIONLINE, identifier);
        return bioSequence;
    }

    private InterMineObject getObject(ObjectStore os, Properties webProps,
            Integer objectId) throws ObjectStoreException {
        String classNames = webProps.getProperty("fasta.export.classes");
        List<Class<?>> classList = new ArrayList<Class<?>>();
        if (classNames != null && classNames.length() != 0) {
            String [] classArray = classNames.split(",");
            for (int i = 0; i < classArray.length; i++) {
                classList.add(TypeUtil.instantiate(os.getModel().getPackageName() + "."
                                                   + classArray[i]));
            }
        } else {
            classList.addAll(Arrays.asList(new Class<?>[] {
                Protein.class,
                SequenceFeature.class
            }));
        }

        InterMineObject obj = os.getObjectById(objectId);
        if (obj instanceof Sequence) {
            Sequence sequence = (Sequence) obj;
            for (Class<?> clazz : classList) {
                obj = ResidueFieldExporter.getIMObjectForSequence(os, clazz,
                                                                  sequence);
                if (obj != null) {
                    break;
                }
            }
        }
        return obj;
    }
}
