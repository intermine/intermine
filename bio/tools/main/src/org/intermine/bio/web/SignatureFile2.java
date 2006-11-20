package org.intermine.bio.web;

/*
 * Original file by:
 * Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov. 
 * Jad home page: http://www.geocities.com/kpdus/jad.html 
 * Decompiler options: packimports(3)  
 * Source File Name:   SignatureFile.java 
 * 
 * modified by FlyMine
 */

import sun.misc.BASE64Encoder;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.util.ManifestDigester;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Referenced classes of package sun.security.util: ManifestDigester
 * @author 
 *
 */
public class SignatureFile2
{
    /**
     * @author 
     *
     */
    public static class Block
    {

        /**
         * @return blockFileName
         */
        public String getMetaName() {
            return blockFileName;
        }

        /**
         * write to outputstream
         * @param out OutputStream
         * @throws IOException IOException
         */
        public void write(OutputStream out) throws IOException {
            block.encodeSignedData(out);
        }

        private PKCS7 block;

        private String blockFileName;

        /**
         * Block
         * @param sfg signature file
         * @param privateKey private key
         * @param certChain chain of certificates
         * @param externalSF external file
         * @throws NoSuchAlgorithmException NoSuchAlgorithmException
         * @throws InvalidKeyException InvalidKeyException
         * @throws IOException IOException
         * @throws SignatureException SignatureException
         * @throws CertificateException CertificateException
         */
        Block(SignatureFile2 sfg, PrivateKey privateKey, X509Certificate certChain[],
                boolean externalSF) throws NoSuchAlgorithmException, InvalidKeyException,
                IOException, SignatureException, CertificateException {
            Principal issuerName = certChain[0].getIssuerDN();
            if (!(issuerName instanceof X500Name)) {
                X509CertInfo tbsCert = new X509CertInfo(certChain[0].getTBSCertificate());
                issuerName = (Principal) tbsCert.get("issuer.dname");
            }
            java.math.BigInteger serial = certChain[0].getSerialNumber();
            String keyAlgorithm = privateKey.getAlgorithm();
            String digestAlgorithm;
            if (keyAlgorithm.equalsIgnoreCase("DSA")) {
                digestAlgorithm = "SHA1";
            } else if (keyAlgorithm.equalsIgnoreCase("RSA")) {
                digestAlgorithm = "MD5";
            } else {
                throw new RuntimeException("private key is not a DSA or RSA key");
            }
            String signatureAlgorithm = digestAlgorithm + "with" + keyAlgorithm;
            blockFileName = "META-INF/" + sfg.getBaseName() + "." + keyAlgorithm;
            AlgorithmId digestAlg = AlgorithmId.get(digestAlgorithm);
            //AlgorithmId sigAlg = AlgorithmId.get(signatureAlgorithm);
            AlgorithmId digEncrAlg = AlgorithmId.get(keyAlgorithm);
            Signature sig = Signature.getInstance(signatureAlgorithm);
            sig.initSign(privateKey);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            sfg.write(baos);
            byte bytes[] = baos.toByteArray();
            ContentInfo contentInfo;
            if (externalSF) {
                contentInfo = new ContentInfo(ContentInfo.DATA_OID, null);
            } else {
                contentInfo = new ContentInfo(bytes);
            }
            sig.update(bytes);
            byte signature[] = sig.sign();
            SignerInfo signerInfo = new SignerInfo((X500Name) issuerName, serial,
                    digestAlg, digEncrAlg, signature);
            AlgorithmId algs[] = {digestAlg};
            SignerInfo infos[] = {signerInfo};
            block = new PKCS7(algs, contentInfo, certChain, infos);
        }
    }

    /**
     * Constructor
     * @param digests message digest
     * @param mf manifest
     * @param md manifest digester
     * @param baseName basename
     * @param signManifest flag wether to sign manifest
     */
    public SignatureFile2(MessageDigest digests[], Manifest mf, ManifestDigester md,
            String baseName, boolean signManifest) {
        this.baseName = baseName;
        String version = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        sf = new Manifest();
        Attributes mattr = sf.getMainAttributes();
        BASE64Encoder encoder = new BASE64Encoder();
        mattr.putValue(java.util.jar.Attributes.Name.SIGNATURE_VERSION.toString(), "1.0");
        mattr.putValue("Created-By", version + " (" + javaVendor + ")");
        if (signManifest) {
            for (int i = 0; i < digests.length; i++) {
                mattr.putValue(digests[i].getAlgorithm() + "-Digest-Manifest", 
                        encoder.encode(md.manifestDigest(digests[i])));
            }

        }
        Map entries = sf.getEntries();
        Iterator mit = mf.getEntries().entrySet().iterator();
        do {
            if (!mit.hasNext()) {
                break;
            }
            java.util.Map.Entry e = (java.util.Map.Entry) mit.next();
            String name = (String) e.getKey();
            ManifestDigester.Entry mde = md.get(name, false);
            if (mde != null) {
                Attributes attr = new Attributes();
                for (int i = 0; i < digests.length; i++) {
                    attr.putValue(digests[i].getAlgorithm() + "-Digest", encoder
                            .encode(mde.digest(digests[i])));
                }
                entries.put(name, attr);
            }
        } while (true);
    }

    /**
     * @param out OutputStream to write to 
     * @throws IOException IOException
     */
    public void write(OutputStream out) throws IOException {
        sf.write(out);
    }

    /**
     * @return METAINF file name
     */
    public String getMetaName() {
        return "META-INF/" + baseName + ".SF";
    }

    /**
     * @return baseName
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * @param privateKey private key
     * @param certChain chain of certificates
     * @param externalSF flag if external file
     * @return a Block
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     * @throws InvalidKeyException InvalidKeyException
     * @throws IOException IOException
     * @throws SignatureException SignatureException
     * @throws CertificateException CertificateException
     */
    public Block generateBlock(PrivateKey privateKey, X509Certificate certChain[],
            boolean externalSF) throws NoSuchAlgorithmException, InvalidKeyException,
            IOException, SignatureException, CertificateException {
        return new Block(this, privateKey, certChain, externalSF);
    }

    Manifest sf;

    String baseName;
}
