package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.FieldExporter;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Provide an attribute value as a file
 * @author Mark Woodbridge
 */
public class GetAttributeAsFileAction extends Action
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ActionMapping mapping,
            @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ObjectStore os = im.getObjectStore();
        WebConfig webConfig = SessionMethods.getWebConfig(request);
        Integer objectId = new Integer(request.getParameter("object"));
        String fieldName = request.getParameter("field");
        String fileType = request.getParameter("type");
        InterMineObject object = os.getObjectById(objectId);

        FieldExporter fieldExporter = null;

        Set classes = DynamicUtil.decomposeClass(object.getClass());

        Iterator classIter = classes.iterator();

        while (classIter.hasNext()) {
            Class c = (Class) classIter.next();

            Type thisTypeConfig = webConfig.getTypes().get(c.getName());

            FieldConfig fc = thisTypeConfig.getFieldConfigMap().get(fieldName);

            if (fc != null) {
                String fieldExporterClassName = fc.getFieldExporter();
                if (fieldExporterClassName != null) {
                    fieldExporter =
                        (FieldExporter) Class.forName(fieldExporterClassName).newInstance();
                    break;
                }
            }
        }

        if (fieldExporter == null) {
            Object fieldValue = object.getFieldValue(fieldName);
            if (fileType == null || fileType.length() == 0) {
                response.setContentType("text/plain; charset=UTF-8");
                response.setHeader("Content-Disposition ",
                                   "inline; filename=" + fieldName + ".txt");
            } else {
                response.setContentType("text/" + fileType);
                response.setHeader("Content-Disposition ", "inline; filename="
                                   + fieldName + "." + fileType);
            }
            PrintStream out = new PrintStream(response.getOutputStream());
            if (fieldValue instanceof ClobAccess) {
                ((ClobAccess) fieldValue).drainToPrintStream(out);
            } else {
                out.print(fieldValue);
            }
            out.flush();
        } else {
            fieldExporter.exportField(object, fieldName, os, response);
        }
        return null;
    }
}
