package org.intermine.install;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;
import javax.swing.UIManager.LookAndFeelInfo;

import org.intermine.common.swing.Messages;
import org.intermine.common.swing.WindowUtils;
import org.intermine.install.project.postprocessing.PostProcessorLoader;
import org.intermine.install.project.source.SourceInfoLoader;
import org.intermine.install.swing.ProjectEditor;
import org.intermine.modelviewer.store.MineManagerBackingStore;

/**
 * The main application launcher for the Intermine installer.
 */
public class IntermineInstaller
{
    /**
     * Main start method. Creates a new ProjectEditor, sets up any previously used
     * look and feel, initialises messages and source types.
     * 
     * @param args Command line arguments - not used.
     * 
     * @throws Exception if there is a failure during initialisation.
     */
    public static void main(String[] args) throws Exception {
        
        // Give installer messages precedence over model viewer messages.
        try {
            ResourceBundle messages =
                ResourceBundle.getBundle("org/intermine/install/messages");
            Messages.addResourceBundle(messages);
        } catch (MissingResourceException e) {
            System.err.println("Cannot locate installer message resource bundle.");
            return;
        }
        try {
            ResourceBundle messages =
                ResourceBundle.getBundle("org/intermine/modelviewer/messages");
            Messages.addResourceBundle(messages);
        } catch (MissingResourceException e) {
            System.err.println("Cannot locate model viewer message resource bundle.");
            return;
        }
        
        SourceInfoLoader.getInstance().initialise();
        PostProcessorLoader.getInstance().initialise();
        
        final ProjectEditor frame = new ProjectEditor();
        WindowUtils.centreOnScreen(frame);
        
        Runnable start = new Runnable() {
            public void run() {
                String lookAndFeel = MineManagerBackingStore.getInstance().getLookAndFeel();
                LookAndFeelInfo lfInfo = frame.getLookAndFeelInfo(lookAndFeel);
                if (lfInfo != null) {
                    frame.changeLookAndFeel(lfInfo);
                }
                frame.setVisible(true);
            }
        };
        
        SwingUtilities.invokeAndWait(start);
    }
}
