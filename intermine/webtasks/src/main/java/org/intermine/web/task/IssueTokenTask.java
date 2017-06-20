package org.intermine.web.task;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.InvalidBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.util.DevNullMap;
import org.intermine.webservice.server.JWTBuilder;

/**
 * A task that will issue a signed JWT token for the current configuration.
 *
 * @author Alex Kalderimis
 *
 */
public class IssueTokenTask extends KeyStoreTask
{

    // The following three properties are required in the project properties.

    /** The identity of the issuing entity (i.e. us) **/
    public static final String PUBLICIDENTITY = "jwt.publicidentity";
    /** The password of the private key. **/
    public static final String PRIVATEKEY_PASSWORD = "security.privatekey.password";
    /** The alias of the private key **/
    public static final String PRIVATEKEY_ALIAS = "security.privatekey.alias";

    private static final String[] REQUIRED_PROPS = {
        PUBLICIDENTITY, PRIVATEKEY_ALIAS, PRIVATEKEY_PASSWORD
    };

    private int expiry = 0;
    private String buildDir, identity, email;

    /**
     * Bean-style setter, as per the ant spec.
     * @param expiry The number of seconds the token will be valid for.
     */
    public void setExpiry(String expiry) {
        this.expiry = Integer.parseInt(expiry, 10);
    }

    /**
     * Bean-style setter, as per the ant spec.
     * @param buildDir The directory to save the token in.
     */
    public void setBuildDir(String buildDir) {
        this.buildDir = buildDir;
    }

    /**
     * Bean-style setter, as per the ant spec.
     * @param identity The subject to issue a claim for.
     */
    public void setSubject(String identity) {
        this.identity = identity;
    }

    /**
     * Bean-style setter, as per the ant spec.
     * @param email The email address passed as a parameter
     */
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public void execute() {
        try {
            unsafeExecute();
        } catch (Exception e) {
            if (e instanceof BuildException) {
                throw (BuildException) e;
            } else {
                throw new BuildException(e);
            }
        }
    }

    // Main logic wrapped in throws Exception so it can be handled at execute()
    private void unsafeExecute() throws Exception {
        FileWriter tokenWriter = null;

        Properties opts = getOptions();
        PrivateKey key = getKey(opts);
        String token = generateToken(opts, key);

        try {
            tokenWriter = getWriter();
            tokenWriter.append(token);
        } finally {
            if (tokenWriter != null) {
                tokenWriter.close();
            }
        }

    }

    @Override
    protected Properties getOptions() {
        Properties options = super.getOptions();
        for (String name: REQUIRED_PROPS) {
            if (!options.containsKey(name)) {
                throw new BuildException("Missing required project property: " + name);
            }
        }
        return options;
    }

    private PrivateKey getKey(Properties opts) throws KeyStoreException,
            NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore ks = createKeyStore();

        Key key = ks.getKey(
                opts.getProperty(PRIVATEKEY_ALIAS),
                opts.getProperty(PRIVATEKEY_PASSWORD).toCharArray());

        if (!(key instanceof PrivateKey)) {
            throw new BuildException(PRIVATEKEY_ALIAS + " must refer to a private key");
        }
        return (PrivateKey) key;
    }

    private String generateToken(Properties opts, PrivateKey key)
        throws InvalidKeyException, SignatureException {
        JWTBuilder builder = new JWTBuilder(key, opts.getProperty(PUBLICIDENTITY));
        Profile profile = new FakeProfile(identity, email);
        String token = builder.issueToken(profile, expiry);
        return token;
    }

    private FileWriter getWriter() throws IOException {
        File outputFile = new File(buildDir, identity + ".jwt");
        logMessage("Writing token to " + outputFile.getAbsolutePath());
        return new FileWriter(outputFile);
    }

    // Implementation for issuing tokens to users by name.
    private class FakeProfile extends Profile
    {

        private String username, email;

        /**
         * Construct a fake profile so we can pass it to the JWTBuilder
         * @param username The of the user
         * @param email The email of the user
         */
        public FakeProfile(String username, String email) {
            super(null, null, null, null,
                    new HashMap<String, SavedQuery>(), new HashMap<String, InterMineBag>(),
                    new HashMap<String, ApiTemplate>(), null, true, false);
            savedQueries = new DevNullMap<String, SavedQuery>();
            savedBags = new DevNullMap<String, InterMineBag>();
            savedTemplates = new DevNullMap<String, ApiTemplate>();
            savedInvalidBags = new DevNullMap<String, InvalidBag>();
            queryHistory = new DevNullMap<String, SavedQuery>();
            savingDisabled = true;

            this.username = username;
            this.email = email;
        }

        @Override
        public String getUsername() {
            return this.username;
        }

        @Override
        public String getEmailAddress() {
            return this.email;
        }
    }
}

