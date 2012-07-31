package org.intermine.common.swing;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.MissingResourceException;

import javax.swing.AbstractAction;
import javax.swing.JDialog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Subclass of <code>JDialog</code> providing a logger and a default cancel
 * Action that simply hides the dialog.
 */
public class StandardJDialog extends JDialog
{
    private static final long serialVersionUID = -1569066413625175923L;

    /**
     * The logger, based on the final class of this object.
     */
    protected transient Log logger = LogFactory.getLog(getClass());
    
    
    /**
     * Create a modeless StandardJDialog with no parent window or title.
     * 
     * @see JDialog#JDialog()
     */
    public StandardJDialog() {
    }

    /**
     * Create a modeless StandardJDialog with a parent Frame and no title.
     * 
     * @param owner The parent Frame.
     * 
     * @see JDialog#JDialog(Frame)
     */
    public StandardJDialog(Frame owner) {
        super(owner);
    }

    /**
     * Create a modeless StandardJDialog with a parent Dialog and no title.
     * 
     * @param owner The parent Dialog.
     * 
     * @see JDialog#JDialog(Dialog)
     */
    public StandardJDialog(Dialog owner) {
        super(owner);
    }

    /**
     * Create a modeless StandardJDialog with a parent Window and no title.
     * 
     * @param owner The parent Window.
     * 
     * @see JDialog#JDialog(Window)
     */
    public StandardJDialog(Window owner) {
        super(owner);
    }

    /**
     * Create a StandardJDialog with a parent Frame and no title.
     * 
     * @param owner The parent Frame.
     * @param modal Whether to make this dialog modal.
     * 
     * @see JDialog#JDialog(Frame, boolean)
     */
    public StandardJDialog(Frame owner, boolean modal) {
        super(owner, modal);
    }

    /**
     * Create a modeless StandardJDialog with a parent Frame and the
     * given title.
     * 
     * @param owner The parent Frame.
     * @param title The title for this dialog.
     * 
     * @see JDialog#JDialog(Frame, String)
     */
    public StandardJDialog(Frame owner, String title) {
        super(owner, title);
    }

    /**
     * Create a StandardJDialog with a parent Dialog and no title.
     * 
     * @param owner The parent Dialog.
     * @param modal Whether to make this dialog modal.
     * 
     * @see JDialog#JDialog(Dialog, boolean)
     */
    public StandardJDialog(Dialog owner, boolean modal) {
        super(owner, modal);
    }

    /**
     * Create a modeless StandardJDialog with a parent Dialog and the
     * given title.
     * 
     * @param owner The parent Dialog.
     * @param title The title for this dialog.
     * 
     * @see JDialog#JDialog(Dialog, String)
     */
    public StandardJDialog(Dialog owner, String title) {
        super(owner, title);
    }

    /**
     * Create a StandardJDialog with a parent Window, no title and the
     * given modality mode.
     * 
     * @param owner The parent Window.
     * @param modalityType The modality type of this dialog.
     * 
     * @see JDialog#JDialog(Window, Dialog.ModalityType)
     */
    public StandardJDialog(Window owner, ModalityType modalityType) {
        super(owner, modalityType);
    }

    /**
     * Create a modeless StandardJDialog with a parent Window and the
     * given title.
     * 
     * @param owner The parent Window.
     * @param title The title for this dialog.
     * 
     * @see JDialog#JDialog(Window, String)
     */
    public StandardJDialog(Window owner, String title) {
        super(owner, title);
    }

    /**
     * Create a StandardJDialog with a parent Frame and the
     * given title.
     * 
     * @param owner The parent Frame.
     * @param title The title for this dialog.
     * @param modal Whether to make this dialog modal.
     * 
     * @see JDialog#JDialog(Frame, String, boolean)
     */
    public StandardJDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
    }

    /**
     * Create a StandardJDialog with a parent Dialog and the
     * given title.
     * 
     * @param owner The parent Dialog.
     * @param title The title for this dialog.
     * @param modal Whether to make this dialog modal.
     * 
     * @see JDialog#JDialog(Dialog, String, boolean)
     */
    public StandardJDialog(Dialog owner, String title, boolean modal) {
        super(owner, title, modal);
    }

    /**
     * Create a StandardJDialog with a parent Window and the
     * given title.
     * 
     * @param owner The parent Window.
     * @param title The title for this dialog.
     * @param modalityType The modality type of this dialog.
     * 
     * @see JDialog#JDialog(Window, String, Dialog.ModalityType)
     */
    public StandardJDialog(Window owner, String title, ModalityType modalityType) {
        super(owner, title, modalityType);
    }

    /**
     * Create a StandardJDialog with a parent Frame and the
     * given title.
     * 
     * @param owner The parent Frame.
     * @param title The title for this dialog.
     * @param modal Whether to make this dialog modal.
     * @param gc The <code>GraphicsConfiguration</code> of the target screen device.
     * If <code>gc</code> is <code>null</code>, the same <code>GraphicsConfiguration</code>
     * as the owning Frame is used.
     * 
     * @see JDialog#JDialog(Frame, String, boolean, GraphicsConfiguration)
     */
    public StandardJDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
    }

    /**
     * Create a StandardJDialog with a parent Dialog and the
     * given title.
     * 
     * @param owner The parent Dialog.
     * @param title The title for this dialog.
     * @param modal Whether to make this dialog modal.
     * @param gc The <code>GraphicsConfiguration</code> of the target screen device.
     * If <code>gc</code> is <code>null</code>, the same <code>GraphicsConfiguration</code>
     * as the owning Dialog is used.
     * 
     * @see JDialog#JDialog(Dialog, String, boolean, GraphicsConfiguration)
     */
    public StandardJDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
    }

    /**
     * Create a StandardJDialog with a parent Window and the
     * given title.
     * 
     * @param owner The parent Dialog.
     * @param title The title for this dialog.
     * @param modalityType The modality type of this dialog.
     * @param gc The <code>GraphicsConfiguration</code> of the target screen device.
     * If <code>gc</code> is <code>null</code>, the same <code>GraphicsConfiguration</code>
     * as the owning Window is used.
     * 
     * @see JDialog#JDialog(Window, String, Dialog.ModalityType, GraphicsConfiguration)
     */
    public StandardJDialog(Window owner, String title, ModalityType modalityType,
                          GraphicsConfiguration gc) {
        super(owner, title, modalityType, gc);
    }
    
    
    /**
     * Deserialisation method. Recreates the logger after this object has been read.
     * 
     * @param in The ObjectInputStream doing the reading.
     * 
     * @throws ClassNotFoundException if the class of a deserialised object cannot
     * be found. 
     * @throws IOException if there is a problem reading from the stream.
     * 
     * @serialData Recreates the transient logger after deserialisation.
     */
    private void readObject(ObjectInputStream in)
    throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        logger = LogFactory.getLog(getClass());
    }

    /**
     * Convenience method for showing a standard exception dialog with this dialog
     * as its parent.
     * 
     * @param error The exception.
     * @param titleMessageKey Message key for the title of the window.
     * @param bodyMessageKey Message key for the body of the window.
     * 
     * @see WindowUtils#showExceptionDialog(Window, Throwable, String, String, Log)
     */
    protected void showExceptionDialog(Throwable error, String titleMessageKey,
                                       String bodyMessageKey) {
        WindowUtils.showExceptionDialog(this, error, titleMessageKey, bodyMessageKey, logger);
    }
    
    /**
     * Default cancel action that simply hides the parent StandardJDialog.
     */
    protected class CancelAction extends AbstractAction
    {
        private static final long serialVersionUID = 7764968755253853210L;

        /**
         * Initialise, using the message "cancel" from {@link Messages}
         * to provide the action text.
         */
        public CancelAction() {
            String name;
            try {
                name = Messages.getMessage("cancel");
            } catch (MissingResourceException e) {
                name = "Cancel";
            }
            putValue(NAME, name);
        }
        
        /**
         * Initialise with the given String as the action text.
         * 
         * @param text The name for this Action.
         */
        public CancelAction(String text) {
            super(text);
        }

        /**
         * Hides the parent dialog.
         * 
         * @param event The ActionEvent.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            setVisible(false);
        }
    }
}
