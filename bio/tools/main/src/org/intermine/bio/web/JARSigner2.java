package org.intermine.bio.web;

/*
 * Original file by:
 * Raffi Krikorian 04/12/2001
 * 
 * modified by FlyMine
 */

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import sun.misc.BASE64Encoder;
import sun.security.util.ManifestDigester;

/** 
 * A class which can be used for creating securely signed Jar files.
 * You create an instance of a JARSigner by calling the getInstance method. You can
 * either supply details of the key to use when calling this method, or it will
 * prompt the user for details.
 * @author joshuapo
 */
public class JARSigner2 extends Object
{
    // the alias for the signing key, the private key to sign with,
    // and the certificate chain
    private String alias;

    private PrivateKey privateKey;

    private X509Certificate[] certChain;

    /**
     * Constructor
     * @param alias alias to use with keystore
     * @param privateKey the private key to use
     * @param certChain the chain of certificates 
     */
    public JARSigner2(String alias, PrivateKey privateKey, X509Certificate[] certChain) {
        this.alias = alias;
        this.privateKey = privateKey;
        this.certChain = certChain;

    }

    /**
     * retrieve the manifest from a jar file -- this will either
     * load a pre-existing META-INF/MANIFEST.MF, or create a new one
     * @param jarFile the jar file to get the manifest from
     * @return the manifest of this jar file
     * @throws IOException
     */
    private static Manifest getManifestFile(JarFile jarFile) throws IOException {
        JarEntry je = jarFile.getJarEntry("META-INF/MANIFEST.MF");
        if (je != null) {
            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                je = (JarEntry) entries.nextElement();
                if ("META-INF/MANIFEST.MF".equalsIgnoreCase(je.getName())) {
                    break;
                } else {
                    je = null;
                }
            }
        }

        // create the manifest object
        Manifest manifest = new Manifest();
        if (je != null) {
            manifest.read(jarFile.getInputStream(je));
        }
        return manifest;

    }

    /**
     * given a manifest file and given a jar file, make sure that
     * the contents of the manifest file is correct and return a
     * map of all the valid entries from the manifest
     * @param manifest the manifest file in question
     * @param jarFile the jar file to test the manifest with
     * @return a Map of all the valid entries from the manifest
     * @throws IOException
     */
    private static Map pruneManifest(Manifest manifest, JarFile jarFile)
            throws IOException {
        Map map = manifest.getEntries();
        Iterator elements = map.keySet().iterator();
        while (elements.hasNext()) {
            String element = (String) elements.next();
            if (jarFile.getEntry(element) == null) {
                elements.remove();
            }
        }
        return map;

    }

    /**
     * make sure that the manifest entries are ready for the signed
     * JAR manifest file. if we already have a manifest, then we
     * make sure that all the elements are valid. if we do not
     * have a manifest, then we create a new signed JAR manifest
     * file by adding the appropriate headers
     * @param manifest the manifest
     * @param jarFile the jar file
     * @return Map of all the valid entries
     * @throws IOException
     */
    private static Map createEntries(Manifest manifest, JarFile jarFile)
            throws IOException {
        Map entries = null;
        if (manifest.getEntries().size() > 0) {
            entries = pruneManifest(manifest, jarFile);
        } else {
            // if there are no pre-existing entries in the manifest,
            // then we put a few default ones in
            Attributes attributes = manifest.getMainAttributes();
            attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
            attributes.putValue("Created-By", System.getProperty("java.version") + " ("
                    + System.getProperty("java.vendor") + ")");
            entries = manifest.getEntries();
        }
        return entries;

    }

    // helper function to update the digest
    private static BASE64Encoder b64Encoder = new BASE64Encoder();

    /**
     * @param digest the MessageDigest
     * @param inputStream
     * @return encoded digest
     * @throws IOException
     */
    private static String updateDigest(MessageDigest digest, InputStream inputStream)
            throws IOException {
        byte[] buffer = new byte[2048];
        int read = 0;
        while ((read = inputStream.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }
        inputStream.close();

        return b64Encoder.encode(digest.digest());

    }

    /**
     * update the attributes in the manifest to have the
     * appropriate message digests. we store the new entries into
     * the entries Map and return it (we do not compute the digests
     * for those entries in the META-INF directory)
     * @param manifest
     * @param jarFile
     * @param messageDigest
     * @param entries
     * @return
     * @throws IOException
     */
    private static Map updateManifestDigest(Manifest manifest, JarFile jarFile,
            MessageDigest messageDigest, Map entries) throws IOException {
        Enumeration jarElements = jarFile.entries();
        while (jarElements.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) jarElements.nextElement();
            if (jarEntry.getName().startsWith("META-INF")) {
                continue;
            } else if (manifest.getAttributes(jarEntry.getName()) != null) {
                // update the digest and record the base 64 version of
                // it into the attribute list
                Attributes attributes = manifest.getAttributes(jarEntry.getName());
                attributes.putValue("SHA1-Digest", updateDigest(messageDigest, jarFile
                        .getInputStream(jarEntry)));

            } else if (!jarEntry.isDirectory()) {
                // store away the digest into a new Attribute
                // because we don't already have an attribute list
                // for this entry. we do not store attributes for
                // directories within the JAR
                Attributes attributes = new Attributes();
                attributes.putValue("SHA1-Digest", updateDigest(messageDigest, jarFile
                        .getInputStream(jarEntry)));
                entries.put(jarEntry.getName(), attributes);

            }

        }
        return entries;

    }

    /**
     * a small helper function that will convert a manifest into an
     * array of bytes
     * @param manifest the manifest to convert
     * @return the resulting byte array
     * @throws IOException
     */
    private byte[] serialiseManifest(Manifest manifest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        manifest.write(baos);
        baos.flush();
        baos.close();
        return baos.toByteArray();

    }

    /**
     * create a signature file object out of the manifest and the message digest
     * @param manifest
     * @param messageDigest
     * @return the SignatureFile
     * @throws IOException
     */
    private SignatureFile2 createSignatureFile(Manifest manifest,
            MessageDigest messageDigest) throws IOException {
        // construct the signature file and the signature block for
        // this manifest
        ManifestDigester manifestDigester = new ManifestDigester(
                serialiseManifest(manifest));
        return new SignatureFile2(new MessageDigest[] {messageDigest}, manifest,
                manifestDigester, this.alias, true);

    }

    /**
     * a helper function that can take entries from one jar file and
     * write it to another jar stream
     * @param je a JarEntry
     * @param jarFile a jar file
     * @param jos JarOutputStream
     * @throws IOException
     */
    private static void writeJarEntry(JarEntry je, JarFile jarFile, JarOutputStream jos)
            throws IOException {
        jos.putNextEntry(je);
        byte[] buffer = new byte[2048];
        int read = 0;
        InputStream is = jarFile.getInputStream(je);
        while ((read = is.read(buffer)) > 0) {
            jos.write(buffer, 0, read);
        }
        jos.closeEntry();

    }

    /**
     * the actual JAR signing method -- this is the method which
     * will be called by those wrapping the JARSigner class
     * @param jarFile the unsigned jar file to sign
     * @param outputStream OutputStream to signed jar file
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     * @throws InvalidKeyException InvalidKeyException
     * @throws SignatureException SignatureException
     * @throws IOException IOException
     */
    public void signJarFile(JarFile jarFile, OutputStream outputStream)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException,
            IOException {

        // calculate the necessary files for the signed jAR

        // get the manifest out of the jar and verify that
        // all the entries in the manifest are correct
        Manifest manifest = getManifestFile(jarFile);
        Map entries = createEntries(manifest, jarFile);

        // create the message digest and start updating the
        // the attributes in the manifest to contain the SHA1
        // digests
        MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
        updateManifestDigest(manifest, jarFile, messageDigest, entries);

        // construct the signature file object and the
        // signature block objects
        SignatureFile2 signatureFile2 = createSignatureFile(manifest, messageDigest);
        SignatureFile2.Block block = null;
        try {
            block = signatureFile2.generateBlock(privateKey, certChain, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // start writing out the signed JAR file

        // write out the manifest to the output jar stream
        String manifestFileName = "META-INF/MANIFEST.MF";
        JarOutputStream jos = new JarOutputStream(outputStream);
        JarEntry manifestFile = new JarEntry(manifestFileName);
        jos.putNextEntry(manifestFile);
        byte[] manifestBytes = serialiseManifest(manifest);
        jos.write(manifestBytes, 0, manifestBytes.length);
        jos.closeEntry();

        // write out the signature file -- the signatureFile
        // object will name itself appropriately
        String signatureFileName = signatureFile2.getMetaName();
        JarEntry signatureFileEntry = new JarEntry(signatureFileName);
        jos.putNextEntry(signatureFileEntry);
        signatureFile2.write(jos);
        jos.closeEntry();

        // write out the signature block file -- again, the block
        // will name itself appropriately
        String signatureBlockName = block.getMetaName();
        JarEntry signatureBlockEntry = new JarEntry(signatureBlockName);
        jos.putNextEntry(signatureBlockEntry);
        block.write(jos);
        jos.closeEntry();

        // commit the rest of the original entries in the
        // META-INF directory. if any of their names conflict
        // with one that we created for the signed JAR file, then
        // we simply ignore it
        Enumeration metaEntries = jarFile.entries();
        while (metaEntries.hasMoreElements()) {
            JarEntry metaEntry = (JarEntry) metaEntries.nextElement();
            if (metaEntry.getName().startsWith("META-INF")
                    && !(manifestFileName.equalsIgnoreCase(metaEntry.getName())
                            || signatureFileName.equalsIgnoreCase(metaEntry.getName()) 
                            || signatureBlockName.equalsIgnoreCase(metaEntry.getName()))) {
                writeJarEntry(metaEntry, jarFile, jos);
            }
        }

        // now write out the rest of the files to the stream
        Enumeration allEntries = jarFile.entries();
        while (allEntries.hasMoreElements()) {
            JarEntry entry = (JarEntry) allEntries.nextElement();
            if (!entry.getName().startsWith("META-INF")) {
                writeJarEntry(entry, jarFile, jos);
            }
        }

        // finish the stream that we have been writing to
        jos.flush();
        jos.finish();

        // close the JAR file that we have been using
        jarFile.close();

    }

}
