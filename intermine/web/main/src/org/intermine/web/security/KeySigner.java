package org.intermine.web.security;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/**
 * Issue certificates for keys we trust.
 *
 * Kindly pilfered from http://stackoverflow.com/questions/1615871/creating-an-x509-certificate-in-java-without-bouncycastle
 * 
 * This class makes use of a bunch of internal and undocumented
 * APIs (everything in sun.security), which will cause warnings
 * and may indeed be removed in the future. If that does happen
 * (and this class stops compiling), then you can replace this
 * logic with code using the BouncyCastle APIs, at the cost of
 * adding a new dependency.
 * 
 * @author Alex Kalderimis
 *
 */
public class KeySigner {

	/** The default algorithm to use **/
	public static final String DEFAULT_ALGORITHM = "SHA256withRSA";

	private PrivateKey signingKey;
	private String dn;
	private int days;
	private String algorithm;
	
	/**
	 * Build a new signer.
	 * @param signingKey Our key that we use to sign the other keys with.
	 * @param dn the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB"
	 * @param days The number of days we issue certificates for.
	 * @param algorithm The signing algorithm we use.
	 */
	public KeySigner(PrivateKey signingKey, String dn, int days, String algorithm) {
		this.signingKey = signingKey;
		this.dn = dn;
		this.days = days;
		this.algorithm = algorithm;
	}
	
	/** 
	 * Create a self-signed X.509 Certificate
	 * @param dn the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB"
	 * @param pair the KeyPair
	 * @param days how many days from now the Certificate is valid for
	 * @param algorithm the signing algorithm, eg "SHA1withRSA"
	 */ 
	public X509Certificate generateCertificate(PublicKey key) throws GeneralSecurityException, IOException {
	  X509CertInfo info = new X509CertInfo();
	  Date from = new Date();
	  Date to = new Date(from.getTime() + days * 86400000l);
	  CertificateValidity interval = new CertificateValidity(from, to);
	  BigInteger sn = new BigInteger(64, new SecureRandom());
	  X500Name owner = new X500Name(dn);
	 
	  info.set(X509CertInfo.VALIDITY, interval);
	  info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
	  info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
	  info.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner));
	  info.set(X509CertInfo.KEY, new CertificateX509Key(key));
	  info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
	  AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
	  info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));
	 
	  // Sign the cert to identify the algorithm that's used.
	  X509CertImpl cert = new X509CertImpl(info);
	  cert.sign(signingKey, algorithm);
	 
	  // Update the algorith, and resign.
	  algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
	  info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
	  cert = new X509CertImpl(info);
	  cert.sign(signingKey, algorithm);
	  
	  return cert;
	}
	
	/* If/when you do need to replace the JCE implementation with the bouncy-castle
	 * one, it would look like this:
    private X509Certificate generateCertificate(PublicKey key) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        X509V3CertificateGenerator cert = new X509V3CertificateGenerator();
        cert.setSerialNumber(BigInteger.valueOf(1));   //or generate a random number
        cert.setSubjectDN(new X509Principal("CN=localhost"));  //see examples to add O,OU etc
        cert.setIssuerDN(new X509Principal("CN=localhost")); //same since it is self-signed
        cert.setPublicKey(keyPair.getPublic());
        cert.setNotBefore(new Date());
        cert.setNotAfter(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365));
        cert.setSignatureAlgorithm("SHA1WithRSAEncryption");

        return cert.generate(signingKey, "BC");
     }	
	 */
}
