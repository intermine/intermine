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

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.MissingResourceException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


/**
 * Progress dialog displaying the output from the system process of a
 * <code>SystemProcessSwingWorker</code>. This dialog presents one or two
 * read-only text areas that display the standard output and where relevant
 * error output from the child process. It also provides an "Ok" button, available
 * once the process has finished, and a "cancel" button that will attempt to
 * cancel the process.
 */
public class SystemProcessProgressDialog extends StandardJDialog
{
    private static final long serialVersionUID = -367426748732877748L;
    
    /**
     * Label displaying information text.
     * @serial
     */
    protected JLabel informationLabel = new JLabel();
    
    /**
     * The text area for standard output.
     * @serial
     */
    protected JTextArea outputArea = new JTextArea();
    
    /**
     * The scroll pane around <code>outputArea</code>.
     * @serial
     */
    protected JScrollPane outputAreaScrollPane = new JScrollPane(outputArea);

    /**
     * The text area for error output.
     * @serial
     */
    protected JTextArea errorArea = new JTextArea();
    
    /**
     * The scroll pane around <code>errorArea</code>.
     * @serial
     */
    protected JScrollPane errorAreaScrollPane = new JScrollPane(errorArea);
    
    /**
     * The "ok" action.
     * @serial
     */
    protected Action okAction = new OkAction();
    
    /**
     * The "cancel" action.
     * @serial
     */
    protected Action cancelAction = new CancelProcessAction();
    
    /**
     * The SystemProcessSwingWorker currently being watched.
     */
    protected transient SystemProcessSwingWorker worker;
    
    /**
     * The listener for events from <code>worker</code>.
     * @serial
     */
    protected PropertyChangeListener listener = new WorkerListener();
    
    
    /**
     * Initialise with the given parent Dialog.
     * 
     * @param owner The parent Dialog.
     * 
     * @see javax.swing.JDialog#JDialog(Dialog)
     */
    public SystemProcessProgressDialog(Dialog owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with the given parent Frame.
     * 
     * @param owner The parent Frame.
     * 
     * @see javax.swing.JDialog#JDialog(Frame)
     */
    public SystemProcessProgressDialog(Frame owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with the given parent Window.
     * 
     * @param owner The parent Window.
     * 
     * @see javax.swing.JDialog#JDialog(Window)
     */
    public SystemProcessProgressDialog(Window owner) {
        super(owner);
        init();
    }

    /**
     * Internal initialisation method. Lays out the child components.
     */
    private void init() {
        Dimension preferredSize = new Dimension(600, 320);
        
        outputArea.setEditable(false);
        outputAreaScrollPane.setPreferredSize(preferredSize);
        errorArea.setEditable(false);
        errorAreaScrollPane.setPreferredSize(preferredSize);
        
        JComponent buttonBox = initButtons();
        
        Container cp = getContentPane();
        cp.setLayout(new GridBagLayout());
        
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = 0;
        cons.gridy = 1;
        cons.gridheight = 1;
        cons.gridwidth = 1;
        cons.fill = GridBagConstraints.HORIZONTAL;
        cons.weightx = 1;
        cons.weighty = 0;
        cons.insets = new Insets(4, 4, 4, 4);
        
        cp.add(informationLabel, cons);
        
        cons.gridy++;
        cons.fill = GridBagConstraints.BOTH;
        cons.weighty = 0.5;
        cp.add(outputAreaScrollPane, cons);
        
        outputArea.setFont(new Font("monospaced", Font.PLAIN, 10));

        cons.gridy++;
        cp.add(errorAreaScrollPane, cons);
        
        errorArea.setFont(new Font("monospaced", Font.PLAIN, 10));

        cons.gridy++;
        cons.fill = GridBagConstraints.HORIZONTAL;
        cons.weighty = 0;
        cp.add(buttonBox, cons);
        
        pack();
    }
    
    /**
     * Initialise a component that contains the buttons relevant to this
     * dialog. This implementation adds "ok" and "cancel" buttons, but this
     * can be overridden to add any buttons required.
     * 
     * @return The JComponent containing the action buttons.
     */
    protected JComponent initButtons() {
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(new JButton(okAction));
        buttonBox.add(Box.createHorizontalStrut(8));
        buttonBox.add(new JButton(cancelAction));
        buttonBox.add(Box.createHorizontalGlue());
        return buttonBox;
    }
    
    /**
     * Set the text on the information label.
     * 
     * @param message The text to display.
     */
    public void setInformationLabel(String message) {
        informationLabel.setText(message);
    }

    /**
     * Set the SystemProcessSwingWorker being watched.
     * 
     * @param worker The SystemProcessSwingWorker.
     */
    public void setWorker(SystemProcessSwingWorker worker) {
        if (this.worker != null) {
            this.worker.removePropertyChangeListener(listener);
        }
        
        this.worker = worker;
        resetOutput();
        
        okAction.setEnabled(false);
        cancelAction.setEnabled(false);
        
        if (worker != null) {
            boolean changed = errorAreaScrollPane.isVisible() == worker.areStreamsLinked();
            if (changed) {
                errorAreaScrollPane.setVisible(!worker.areStreamsLinked());
                pack();
            }
            worker.addPropertyChangeListener(listener);
        }
    }
    
    /**
     * Clear the output text areas.
     */
    public void resetOutput() {
        outputArea.setText("");
        errorArea.setText("");
    }
    
    /**
     * Write the given text to the standard output text area. This is additive:
     * the given text is appended to the text already displayed.
     * 
     * @param text The text to append.
     */
    public void writeOutput(String text) {
        outputArea.append(text);
    }

    /**
     * Write the given text to the error output text area. This is additive:
     * the given text is appended to the text already displayed.
     * 
     * @param text The text to append.
     */
    public void writeError(String text) {
        errorArea.append(text);
    }

    /**
     * Default "ok" action that simply hides this dialog when the action
     * is invoked.
     */
    protected class OkAction extends AbstractAction
    {
        private static final long serialVersionUID = -8362658808102333841L;

        /**
         * Initialise this dialog with using the "ok" message key via
         * {@link Messages}.
         */
        public OkAction() {
            String name;
            try {
                name = Messages.getMessage("ok");
            } catch (MissingResourceException e) {
                name = "Ok";
            }
            putValue(NAME, name);
            setEnabled(false);
        }

        /**
         * When the action is performed, hide the dialog.
         * 
         * @param event The ActionEvent.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            //setVisible(false);
            dispose();
        }
    }
    
    /**
     * Default "cancel" action that calls <code>cancel</code> on the watched
     * SystemProcessSwingWorker. The cancel call is made allowing the worker
     * thread to be interrupted.
     *
     * @see java.util.concurrent.Future#cancel(boolean)
     */
    protected class CancelProcessAction extends AbstractAction
    {
        private static final long serialVersionUID = 6168285542925861150L;

        /**
         * Initialise this dialog with using the "cancel" message key via
         * {@link Messages}.
         */
        public CancelProcessAction() {
            String name;
            try {
                name = Messages.getMessage("cancel");
            } catch (MissingResourceException e) {
                name = "Cancel";
            }
            putValue(NAME, name);
            setEnabled(false);
        }

        /**
         * When the action is performed, call <code>cancel</code> on the watched
         * worker.
         * 
         * @param event The ActionEvent.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            worker.cancel(true);
        }
    }
    
    /**
     * Listener for properties fired from the watched SystemProcessSwingWorker.
     * Responds to the events thus:
     * 
     * <ol>
     * <li><code>STARTED</code>: enables the cancel button and disables the ok button.
     * <li><code>OUTPUT</code>: appends the given String to the output text area.
     * <li><code>ERROR</code>: appends the given String to the error output text area.
     * <li><code>COMPLETE</code>: disables the cancel button and enables the ok button.
     * </ol>
     */
    private class WorkerListener implements PropertyChangeListener
    {
        /**
         * Listener method.
         * 
         * @param event The PropertyChangeEvent from the watched SystemProcessSwingWorker.
         */
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            
            if (SystemProcessSwingWorker.OUTPUT.equals(event.getPropertyName())) {
                String output = (String) event.getNewValue();
                outputArea.append(output);
                outputArea.repaint();
            } else if (SystemProcessSwingWorker.ERROR.equals(event.getPropertyName())) {
                String output = (String) event.getNewValue();
                errorArea.append(output);
                errorArea.repaint();
            } else if (SystemProcessSwingWorker.STARTED.equals(event.getPropertyName())) {
                cancelAction.setEnabled(true);
                okAction.setEnabled(false);
            } else if (SystemProcessSwingWorker.COMPLETE.equals(event.getPropertyName())) {
                cancelAction.setEnabled(false);
                okAction.setEnabled(true);
                worker.removePropertyChangeListener(this);
                worker = null;
            }
        }
    }
}
