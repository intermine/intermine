package org.intermine.modelviewer.store;

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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.intermine.modelviewer.IntermineModelViewer;


/**
 * A class to store user-specific properties via Java's backing store mechanism.
 * 
 * @see java.util.prefs java.util.prefs
 */
public class MineManagerBackingStore
{
    /**
     * Key for storing the path of the last accessed project file.
     */
    private static final String LAST_PROJECT_FILE_KEY = "lastProjectFile";
    
    /**
     * Key for storing the last selected Intermine home directory path.
     */
    private static final String INTERMINE_HOME_KEY = "intermineHome";
    
    /**
     * Key for storing the user's preferred look and feel on this machine.
     */
    private static final String LOOK_AND_FEEL_KEY = "lookAndFeel";
    
    /**
     * Singleton instance of this class.
     */
    private static MineManagerBackingStore instance = new MineManagerBackingStore();
    
    /**
     * Logger.
     */
    private Log logger = LogFactory.getLog(MineManagerBackingStore.class);
    
    /**
     * The node into which the attributes are stored.
     */
    private Preferences prefs;
    
    
    /**
     * Get the singleton instance of this class.
     * @return The instance of MineManagerBackingStore.
     */
    public static MineManagerBackingStore getInstance() {
        return instance;
    }
    
    /**
     * Create a new instance of MineManagerBackingStore.
     */
    protected MineManagerBackingStore() {
        prefs = Preferences.userNodeForPackage(IntermineModelViewer.class);
    }
    
    /**
     * Fetch the last opened project file.
     * 
     * @return The last used project file, or <code>null</code> if this is not
     * set or the file no longer exists.
     */
    public File getLastProjectFile() {
        File file = null;
        String path = prefs.get(LAST_PROJECT_FILE_KEY, null);
        if (path != null) {
            file = new File(path);
            if (!file.exists()) {
                file = null;
            }
        }
        return file;
    }
    
    /**
     * Set the last opened project file.
     * 
     * @param file The project file.
     */
    public void setLastProjectFile(File file) {
        if (file != null) {
            prefs.put(LAST_PROJECT_FILE_KEY, file.getAbsolutePath());
        }
        flush();
    }
    
    /**
     * Fetch the previously selected Intermine home directory.
     * 
     * @return The last home directory selected, or <code>null</code> if this is not
     * set or the directory no longer exists.
     */
    public File getIntermineHome() {
        File file = null;
        String path = prefs.get(INTERMINE_HOME_KEY, null);
        if (path != null) {
            file = new File(path);
            if (!file.exists() || !file.isDirectory()) {
                file = null;
            }
        }
        return file;
    }
    
    /**
     * Set the Intermine home directory.
     * 
     * @param file The home directory.
     */
    public void setIntermineHome(File file) {
        if (file != null) {
            prefs.put(INTERMINE_HOME_KEY, file.getAbsolutePath());
        }
        flush();
    }
    
    /**
     * Get the name of the look and feel implementation last used.
     * 
     * @return The last selected look and feel name. Defaults to the
     * Java default of "Metal".
     */
    public String getLookAndFeel() {
        return prefs.get(LOOK_AND_FEEL_KEY, "Metal");
    }
    
    /**
     * Set (or clear) the preferred look and feel.
     * 
     * @param lookAndFeel The look and feel name, or <code>null</code> to
     * clear.
     */
    public void setLookAndFeel(String lookAndFeel) {
        if (lookAndFeel != null) {
            prefs.put(LOOK_AND_FEEL_KEY, lookAndFeel);
        } else {
            prefs.remove(LOOK_AND_FEEL_KEY);
        }
        flush();
    }
    
    /**
     * Attempt to write the preferences to the backing store. Any exception thrown
     * is logged (at warning level) but otherwise ignored.
     */
    protected void flush() {
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            logger.warn("Failed to write values to backing store: " + e.getMessage());
        }
    }
}
