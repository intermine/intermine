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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Window-related utility methods.
 */
public final class WindowUtils
{
    /**
     * Centre the given window on the screen.
     * 
     * @param win The Window to position.
     */
    public static void centreOnScreen(Window win) {
        Dimension screenRes = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension windowSize = win.getSize();

        int x = Math.max(0, (screenRes.width - windowSize.width) / 2);
        int y = Math.max(0, (screenRes.height - windowSize.height) / 2);

        win.setLocation(x, y);
    }

    /**
     * Centre the window <code>win</code> over the window <code>parent</code>.
     * 
     * @param win The Window to position.
     * @param parent The reference Window.
     */
    public static void centreOverWindow(Window win, Window parent) {
        Rectangle parentBounds = parent.getBounds();
        Dimension windowSize = win.getSize();

        int x = (parentBounds.width - windowSize.width) / 2;
        int y = (parentBounds.height - windowSize.height) / 2;

        x = Math.max(0, x + parentBounds.x);
        y = Math.max(0, y + parentBounds.y);

        win.setLocation(x, y);
    }

    /**
     * Get the internal frame parent of the given component.
     * 
     * @param c The child component.
     * 
     * @return <code>c</code>'s parent JInternalFrame, or <code>null</code> if there
     * is no such parent.
     */
    public static JInternalFrame getInternalFrameAncestor(Component c) {
        return (JInternalFrame) SwingUtilities.getAncestorOfClass(JInternalFrame.class, c);
    }

    /**
     * Get the viewport parent of the given component.
     * 
     * @param c The child component.
     * 
     * @return <code>c</code>'s parent JViewPort, or <code>null</code> if there
     * is no such parent.
     */
    public static JViewport getViewportAncestor(Component c) {
        return (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, c);
    }
    
    /**
     * Centre the internal frame <code>win</code> over the internal
     * frame <code>parent</code>.
     * 
     * @param win The JInternalFrame to position.
     * @param parent The reference JInternalFrame.
     */
    public static void centreOverFrame(JInternalFrame win, JInternalFrame parent) {
        Rectangle parentBounds = parent.getBounds();
        Dimension windowSize = win.getSize();
        
        int x = (parentBounds.width - windowSize.width) / 2;
        int y = (parentBounds.height - windowSize.height) / 2;
        
        x = Math.max(0, x + parentBounds.x);
        y = Math.max(0, y + parentBounds.y);
        
        win.setLocation(x, y);
    }

    /**
     * Show a standard exception dialog.
     * 
     * @param comp The Component launching the exception.
     * @param error The exception.
     * @param titleMessageKey Message key for the title of the window.
     * @param bodyMessageKey Message key for the body of the window.
     * @param logger The logger to write the error message to.
     */
    public static void showExceptionDialog(Component comp, Throwable error,
                                           String titleMessageKey, String bodyMessageKey,
                                           Log logger) {
        if (comp instanceof Window) {
            showExceptionDialog((Window) comp, error, titleMessageKey, bodyMessageKey, logger);
        } else {
            showExceptionDialog(SwingUtilities.getWindowAncestor(comp), error,
                                titleMessageKey, bodyMessageKey, logger);
        }
    }
    
    /**
     * Show a standard exception dialog.
     * 
     * @param parent The parent Window.
     * @param error The exception.
     * @param titleMessageKey Message key for the title of the window.
     * @param bodyMessageKey Message key for the body of the window.
     * @param logger The logger to write the error message to.
     */
    public static void showExceptionDialog(Window parent, Throwable error,
                                           String titleMessageKey, String bodyMessageKey,
                                           Log logger) {
        if (logger == null) {
            logger = LogFactory.getLog(WindowUtils.class);
        }
        
        String displayMessage;
        if (error.getCause() != null) {
            logger.error(Messages.getMessage(bodyMessageKey, ""), error.getCause());
            StringWriter swriter = new StringWriter();
            PrintWriter writer = new PrintWriter(swriter);
            error.getCause().printStackTrace(writer);
            writer.close();
            
            displayMessage = Messages.getMessage(bodyMessageKey, swriter.toString());
        } else {
            logger.error(Messages.getMessage(bodyMessageKey, ""), error);
            displayMessage = Messages.getMessage(bodyMessageKey, error.getMessage());
        }
        
        JOptionPane.showMessageDialog(parent,
                                      displayMessage,
                                      Messages.getMessage(titleMessageKey),
                                      JOptionPane.ERROR_MESSAGE);
    }
    
}
