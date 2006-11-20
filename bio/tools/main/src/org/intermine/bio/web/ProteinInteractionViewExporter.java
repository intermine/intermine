package org.intermine.bio.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.KeySpec;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.flymine.model.genomic.ProteinInteraction;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.web.SessionMethods;
import org.intermine.web.TableExporter;
import org.intermine.web.results.PagedTable;

/**
 * An implementation of TableExporter that starts cytoscape network viewer
 * as java webstart application
 * 
 * @author Florian Reisinger
 */
public class ProteinInteractionViewExporter implements TableExporter
{
    // TODO: check with ProteinInteractionViewExporter for similarity of code
    static final boolean DEBUG = false;

    static String alias, tmpJar, signedJar, keystore, passwd;

    static String sifPath, sifFile, path2BJared;

    protected static File[] files2BJared;

    /**
     * Method called to export a PagedTable object using the BioJava sequence
     * and feature writers.
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward export(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        HttpSession session = request.getSession();

        response.setContentType("application/x-java-jnlp-file");
        response.setHeader("Content-Disposition ",
                "inline; filename=interaction" + StringUtil.uniqueString()
                        + ".jnlp"); // flo

        OutputStream outputStream = null;
        FileOutputStream sifOutStream = null;
        PagedTable pt = SessionMethods.getResultsTable(session, request
                .getParameter("table"));

        List columns = pt.getColumns();
        int realFeatureIndex = PIUtil.getValidColumnIndex(columns);

        int writtenInteractionsCount = 0; // flo
        jarInit();

        try {
            // alle Datenreihen holen
            List rowList = pt.getAllRows();
            sifOutStream = new FileOutputStream(sifPath.concat(sifFile));
            PrintWriter sifWriter = new PrintWriter(sifOutStream, true);

            // durch alle reihen durchlaufen
            for (int rowIndex = 0; rowIndex < rowList.size()
                    && rowIndex <= pt.getMaxRetrievableIndex(); rowIndex++) {
                List row;
                try {
                    row = (List) rowList.get(rowIndex); // einzelne reihe des
                    // resultset
                } catch (RuntimeException e) {
                    // re-throw as a more specific exception
                    if (e.getCause() instanceof ObjectStoreException) {
                        throw (ObjectStoreException) e.getCause();
                    } else {
                        throw e;
                    }
                }

                // get object of interest - ProteinInteraction
                InterMineObject object = (InterMineObject) row
                        .get(realFeatureIndex);
                ProteinInteraction feature = (ProteinInteraction) object;

                // retrieve the interacting proteins from the feature
                Collection interactors = feature.getInteractors();

                // write all found interactions in sif format to file sifFile
                sifWriter.write(ProteinInteractionExporter
                        .getSifLines(interactors));
                sifWriter.flush();

                writtenInteractionsCount++; // flo
            }
            // close outputstream to sif file
            if (sifOutStream != null) {
                sifOutStream.close();
            }
            if (writtenInteractionsCount == 0) {
                ActionErrors messages = new ActionErrors();
                ActionError error = new ActionError(
                        "errors.export.nothingtoexport");
                messages.add(ActionErrors.GLOBAL_ERROR, error);
                request.setAttribute(Globals.ERROR_KEY, messages);
                return mapping.findForward("results");
            }
            // create jar file containing sif file and properties
            createJar(new File(tmpJar), sifFile);
            //            signJar(new File(keystore), passwd, alias, new JarFile(tmpJar),
            //                    new FileOutputStream(signedJar));
            signJar2(new File(keystore), passwd);

            // open outputstream to servlet
            if (outputStream == null) {
                // try to avoid opening the OutputStream until we know that
                // the query is going to work - this avoids some problems that
                // occur when getOutputStream() is called twice (once by this
                // method and again to write the error)
                outputStream = response.getOutputStream();
            }
            // write jnlp file
            PrintWriter writer = new PrintWriter(outputStream, true);
            writer.write(PIUtil.buildJNLP(sifFile));
            writer.flush();
            // close outputstream to servlet
            if (outputStream != null) {
                outputStream.close();
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
     * @param pt the PagedTable containing the results 
     * @return true if exportable results were found
     * @see org.intermine.web.TableExporter#canExport
     * @see org.intermine.bio.web.PIUtil#canExport
     */
    public boolean canExport(PagedTable pt) {
        return PIUtil.canExport(pt);
    }

    /**
     * intialize fields
     */
    protected static void jarInit() {
        sifPath = "/opt/apache-tomcat-5.5.12/webapps/cytoscape/data/";
        sifFile = "interaction.sif";    // TODO: use File.createTempFile("interaction", ".sif")

        keystore = "/home/flo/.keystore";
        alias = "cytoscape";
        passwd = "secret";
        
        tmpJar = "/tmp/data_tmp.jar"; // TODO: use File.createTempFile("data-tmp", ".jar")
        signedJar = "/opt/apache-tomcat-5.5.12/webapps/cytoscape/data.jar";
        path2BJared = "/opt/apache-tomcat-5.5.12/webapps/cytoscape/data";

    }

    /**
     * create the jar file
     * @param jarOutFile file to wirte to 
     * @param sifFile sif file to be included in jar file
     */
    protected static void createJar(File jarOutFile, String sifFile) {

        File file = new File(path2BJared);
        if (file.isDirectory()) {
            files2BJared = file.listFiles();
        }
        if (files2BJared == null) {
            // use log4j
            return;
        }
        try {
            byte buffer[] = new byte[10240];

            Manifest mf = new Manifest();
            Attributes atts = mf.getMainAttributes();
            atts.putValue("Manifest-Version", "1.0");
            mf.getEntries().put(sifFile, new Attributes());
            // TODO: lookup filenames instead of static names
            mf.getEntries().put("cytoscape.props", new Attributes());
            mf.getEntries().put("vizmap.props", new Attributes());
            mf.getEntries().put("projectFile", new Attributes());

            // Open archive file
            FileOutputStream stream = new FileOutputStream(jarOutFile);
            JarOutputStream out = new JarOutputStream(stream, mf);

            for (int i = 0; i < files2BJared.length; i++) {
                if (files2BJared[i] == null || !files2BJared[i].exists()
                        || files2BJared[i].isDirectory()) {
                    continue; // Just in case...
                }

                // Add archive entry
                JarEntry jarAdd = new JarEntry(files2BJared[i].getName());
                jarAdd.setTime(files2BJared[i].lastModified());
                out.putNextEntry(jarAdd);

                // Write file to archive
                FileInputStream in = new FileInputStream(files2BJared[i]);
                while (true) {
                    int nRead = in.read(buffer, 0, buffer.length);
                    if (nRead <= 0) {
                        break;
                    }
                    out.write(buffer, 0, nRead);
                }
                in.close();
            }

            out.close();
            stream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * sign the created jar file
     * @param kstore the keystore to use
     * @param pw the password to use
     */
    protected static void signJar2(File kstore, String pw) {
        try {
            FileInputStream fileIn = new FileInputStream(kstore);
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(fileIn, pw.toCharArray());
            Certificate[] chain = keyStore.getCertificateChain(alias);
            X509Certificate certChain[] = new X509Certificate[chain.length];

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            for (int count = 0; count < chain.length; count++) {
                ByteArrayInputStream certIn = new ByteArrayInputStream(chain[0]
                        .getEncoded());
                X509Certificate cert = (X509Certificate) cf.generateCertificate(certIn);
                certChain[count] = cert;
            }

            Key key = keyStore.getKey(alias, pw.toCharArray());
            KeyFactory keyFactory = KeyFactory.getInstance(key.getAlgorithm());
            KeySpec keySpec = keyFactory.getKeySpec(key, DSAPrivateKeySpec.class);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            JARSigner2 jarSigner = new JARSigner2(alias, privateKey, certChain);

            JarFile jarFile = new JarFile(tmpJar);
            OutputStream outStream = new FileOutputStream(signedJar);
            jarSigner.signJarFile(jarFile, outStream);
            fileIn.close();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

}
