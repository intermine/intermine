package org.intermine.modelviewer;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import org.intermine.common.swing.Messages;
import org.intermine.common.swing.WindowUtils;
import org.intermine.modelviewer.swing.ModelViewer;

/**
 * Main class for the model viewer as a stand alone application. This
 * JFrame subclass simply contains the viewer component and provides
 * menu actions.
 */
public class IntermineModelViewer extends JFrame
{
    private static final long serialVersionUID = 1603402118222210830L;
    
    /**
     * Local version of the action keyboard modifier mask.
     * @serial
     */
    private final int menuActionKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    
    /**
     * The model viewer component.
     * @serial
     */
    protected ModelViewer modelViewer;
    
    /**
     * Initialise with no title.
     */
    public IntermineModelViewer() {
        init();
    }
    
    /**
     * Initialise with the given title.
     * 
     * @param title The window title.
     */
    public IntermineModelViewer(String title) {
        super(title);
        init();
    }
    
    /**
     * Lay out the components of the frame.
     */
    private void init() {
        modelViewer = new ModelViewer();
        
        setContentPane(modelViewer);
        
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        
        JMenu fileMenu = new JMenu(Messages.getMessage("file"));
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);
        
        fileMenu.add(new OpenAction());
        
        setSize(800, 600);
    }
    
    /**
     * Main method: creates and shows a new IntermineModelViewer frame.
     * 
     * @param args Command line arguments. Unused.
     */
    public static void main(String[] args) {
        try {
            // Initialise the Messages object.
            ResourceBundle messages =
                ResourceBundle.getBundle("org/intermine/modelviewer/messages");
            Messages.addResourceBundle(messages);
        } catch (MissingResourceException e) {
            System.err.println("Cannot locate model viewer message resource bundle.");
            return;
        }
        
        IntermineModelViewer frame =
            new IntermineModelViewer(Messages.getMessage("interminemodelviewer.title"));
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        WindowUtils.centreOnScreen(frame);
        frame.setVisible(true);
    }

    /**
     * Action for opening project files.
     * Calls {@link ModelViewer#loadProject(File)}.
     */
    private class OpenAction extends AbstractAction
    {
        private static final long serialVersionUID = -358871024508151421L;

        /**
         * Constructor.
         */
        public OpenAction() {
            super(Messages.getMessage("loadmine"));
            putValue(MNEMONIC_KEY, KeyEvent.VK_L);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, menuActionKeyMask));
        }

        /**
         * Action call back. Opens a prompt for the project file via
         * {@link ModelViewer#loadProject(File)}.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            File projectFile = modelViewer.chooseProjectFile();
            if (projectFile != null) {
                Cursor current = getCursor();
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    modelViewer.loadProject(projectFile);
                } finally {
                    setCursor(current);
                }
            }
        }
    }
}
