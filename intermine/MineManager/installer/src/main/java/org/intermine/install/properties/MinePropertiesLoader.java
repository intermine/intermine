package org.intermine.install.properties;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class to load and save the properties held in the
 * <code>$HOME/.intermine/&lt;mine name&gt.properties</code> file: the mine's
 * user configuration.
 * 
 * <p><b>Important Note</b> - This class may need to be adapted slightly to deal
 * with property values split over more than one line and connected with the continuation
 * character ('\').</p>
 */
public abstract class MinePropertiesLoader
{
    /**
     * The resource path for the template and default property values files.
     */
    protected static final String RESOURCE_PATH = "/org/intermine/install/templates/";
    
    /**
     * The full path to the default mine property values property bundle.
     */
    protected static final String DEFAULT_PROPERTIES = RESOURCE_PATH + "defaultmine.properties";
    
    /**
     * The full path to the mine property file template.
     */
    protected static final String PROPERTIES_TEMPLATE = RESOURCE_PATH + "mineproperties.template";
    
    
    /**
     * A regular expression to detect key value pairs in the file, and extract the property key.
     */
    protected static final Pattern PROPERTY_PATTERN = Pattern.compile("^\\s*([\\w.-]+)\\s*=.*$");
    
    /**
     * Logger.
     */
    protected static Log logger = LogFactory.getLog(MinePropertiesLoader.class);
    
    /**
     * The Intermine user home directory.
     */
    protected static File intermineUserHome;
    
    
    /**
     * Restricted constructor.
     */
    private MinePropertiesLoader() {
    }
    
    /**
     * Get the Intermine user home directory.
     * <p>This is <code>$HOME/.intermine</code>.</p>
     * 
     * @return The Intermine user home directory.
     */
    public static File getIntermineUserHome() {
        if (intermineUserHome == null) {
            intermineUserHome = new File(System.getProperty("user.home") + "/.intermine");
        }
        return intermineUserHome;
    }

    /**
     * Get the Intermine configuration properties file for the given mine.
     * This is the lower case mine name in the Intermine user home directory.
     * 
     * @param mineName The name of the mine.
     * 
     * @return The mine configuration properties file.
     */
    public static File getMinePropertiesFile(String mineName) {
        String lmine = mineName.toLowerCase();
        return new File(getIntermineUserHome(), lmine + ".properties");
    }

    /**
     * Read the property values from the Intermine user configuration file for the
     * named mine. Standard default values are also read in case of missing properties
     * in the user file.
     * 
     * @param mineName The name of the mine.
     * 
     * @return The properties read from the mine's configuration.
     */
    public static Properties readPreviousProperties(String mineName) {
        
        Properties props = new Properties();
        try {
            InputStream in = MinePropertiesLoader.class.getResourceAsStream(DEFAULT_PROPERTIES);
            if (in == null) {
                throw new FileNotFoundException(DEFAULT_PROPERTIES);
            } else {
                try {
                    props.load(in);
                } finally {
                    in.close();
                }
                
                String mailSubject = props.getProperty(InterminePropertyKeys.MAIL_SUBJECT);
                if (mailSubject != null) {
                    mailSubject = MessageFormat.format(mailSubject, mineName);
                    props.put(InterminePropertyKeys.MAIL_SUBJECT, mailSubject);
                }
                
                String mailText = props.getProperty(InterminePropertyKeys.MAIL_TEXT);
                if (mailText != null) {
                    mailText = MessageFormat.format(mailText, mineName, "{0}");
                    props.put(InterminePropertyKeys.MAIL_TEXT, mailText);
                }
            }
        } catch (IOException e) {
            logger.warn("Cannot read default properties from \"defaultmine.properties\".");
        }
        
        File minePropertiesFile = getMinePropertiesFile(mineName);
        try {
            Reader reader = new FileReader(minePropertiesFile);
            try {
                props.load(reader);
                logger.info("Taking values from " + minePropertiesFile.getName());
            } finally {
                reader.close();
            }
        } catch (FileNotFoundException e) {
            // Fine.
        } catch (IOException e) {
            logger.warn("Could not read previous properties file "
                        + minePropertiesFile.getName() + ": " + e.getMessage());
        }
        
        return props;
    }

    /**
     * Write out the mine's user configuration properties file. This bases its format
     * on the file as it exists in the Intermine user home directory, writing lines verbatim
     * except for those where it has been supplied a property value. If no such file exists,
     * a standard template is used as the basis.
     * 
     * @param mineName The name of the mine.
     * @param props The mine's user configuration properties.
     * 
     * @throws IOException if there is a problem performing the write.
     */
    public static void saveProperties(String mineName, Properties props)
    throws IOException {
        File minePropertiesFile = getMinePropertiesFile(mineName);
        File mineBackupFile = null;
        
        Reader sourceReader;
        
        if (minePropertiesFile.exists()) {
            
            mineBackupFile =
                new File(minePropertiesFile.getParentFile(), minePropertiesFile.getName() + "~");
            
            if (mineBackupFile.exists()) {
                mineBackupFile.delete();
            }
            
            boolean ok = minePropertiesFile.renameTo(mineBackupFile);
            if (!ok) {
                throw new IOException("Failed to create backup file for "
                                      + minePropertiesFile.getAbsolutePath());
            }
            
            sourceReader = new FileReader(mineBackupFile);
            
        } else {
            if (intermineUserHome.exists()) {
                if (!intermineUserHome.isDirectory()) {
                    throw new IOException(intermineUserHome.getAbsolutePath()
                                          + " is not a directory.");
                }
            } else {
                boolean ok = intermineUserHome.mkdir();
                if (!ok) {
                    throw new IOException("Failed to create directory "
                                          + intermineUserHome.getAbsolutePath());
                }
            }
            
            InputStream in = MinePropertiesLoader.class.getResourceAsStream(PROPERTIES_TEMPLATE);
            if (in == null) {
                throw new FileNotFoundException(PROPERTIES_TEMPLATE);
            }
            sourceReader = new InputStreamReader(in);
        }
        
        LineNumberReader reader = new LineNumberReader(sourceReader);
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(minePropertiesFile));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher m = PROPERTY_PATTERN.matcher(line);
                    if (m.matches()) {
                        String key = m.group(1);
                        if (props.containsKey(key)) {
                            writer.print(key);
                            writer.print('=');
                            writer.println(props.get(key));
                        } else {
                            writer.println(line);
                        }
                    } else {
                        writer.println(line);
                    }
                }
            } finally {
                writer.close();
            }
        } finally {
            reader.close();
        }
    }
}
