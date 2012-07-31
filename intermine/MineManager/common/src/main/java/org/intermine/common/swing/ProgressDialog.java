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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.MissingResourceException;
import java.util.concurrent.RunnableFuture;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Two-bar progress dialog to display the progress of background tasks.
 * The dialog display either a single progress bar or a pair, in the typical
 * task progress + overall progress model.
 */
public class ProgressDialog extends JDialog
{
    private static final long serialVersionUID = -1825683872846077368L;

    /**
     * The label that shows information text.
     * @serial
     */
    private JLabel informationLabel = new JLabel();
    
    /**
     * The task progress bar.
     * @serial
     */
    private JProgressBar taskProgressBar = new JProgressBar(SwingConstants.HORIZONTAL);
    
    /**
     * The overall progress bar.
     * @serial
     */
    private JProgressBar overallProgressBar = new JProgressBar(SwingConstants.HORIZONTAL);
    
    /**
     * The cancel button.
     * @serial
     */
    private JButton cancelButton = new JButton();
    
    
    /**
     * The task this dialog is watching.
     * Only used to issue the cancel call on the button press.
     * @serial
     */
    private RunnableFuture<?> task;
    
    /**
     * Flag indicating whether <code>task</code> can have its thread interrupted.
     * @serial
     * @see java.util.concurrent.Future#cancel(boolean)
     */
    private boolean mayInterrupt;

    
    /**
     * Create a ProgressDialog with no parent or title.
     * 
     * @param overallBar <code>true</code> to show both progress bars, <code>false</code>
     * to show only the task progress.
     * 
     * @param canCancel If <code>true</code>, a cancel button will be available to allow
     * the background process to be stopped.
     */
    public ProgressDialog(boolean overallBar, boolean canCancel) {
        init(overallBar, canCancel);
    }

    /**
     * Create a ProgressDialog with the given parent Frame and no title.
     * 
     * @param owner The parent Frame.
     * 
     * @param overallBar <code>true</code> to show both progress bars, <code>false</code>
     * to show only the task progress.
     * 
     * @param canCancel If <code>true</code>, a cancel button will be available to allow
     * the background process to be stopped.
     */
    public ProgressDialog(Frame owner, boolean overallBar, boolean canCancel) {
        super(owner);
        init(overallBar, canCancel);
    }

    /**
     * Create a ProgressDialog with the given parent Dialog and no title.
     * 
     * @param owner The parent Dialog.
     * 
     * @param overallBar <code>true</code> to show both progress bars, <code>false</code>
     * to show only the task progress.
     * 
     * @param canCancel If <code>true</code>, a cancel button will be available to allow
     * the background process to be stopped.
     */
    public ProgressDialog(Dialog owner, boolean overallBar, boolean canCancel) {
        super(owner);
        init(overallBar, canCancel);
    }

    /**
     * Create a ProgressDialog with the given parent Window and no title.
     * 
     * @param owner The parent Window.
     * 
     * @param overallBar <code>true</code> to show both progress bars, <code>false</code>
     * to show only the task progress.
     * 
     * @param canCancel If <code>true</code>, a cancel button will be available to allow
     * the background process to be stopped.
     */
    public ProgressDialog(Window owner, boolean overallBar, boolean canCancel) {
        super(owner);
        init(overallBar, canCancel);
    }

    /**
     * Create a ProgressDialog with the given parent Frame and title.
     * 
     * @param owner The parent Frame.
     * @param title The title for the dialog.
     * 
     * @param overallBar <code>true</code> to show both progress bars, <code>false</code>
     * to show only the task progress.
     * 
     * @param canCancel If <code>true</code>, a cancel button will be available to allow
     * the background process to be stopped.
     */
    public ProgressDialog(Frame owner, String title, boolean overallBar, boolean canCancel) {
        super(owner, title);
        init(overallBar, canCancel);
    }

    /**
     * Create a ProgressDialog with the given parent Dialog and title.
     * 
     * @param owner The parent Dialog.
     * @param title The title for the dialog.
     * 
     * @param overallBar <code>true</code> to show both progress bars, <code>false</code>
     * to show only the task progress.
     * 
     * @param canCancel If <code>true</code>, a cancel button will be available to allow
     * the background process to be stopped.
     */
    public ProgressDialog(Dialog owner, String title, boolean overallBar, boolean canCancel) {
        super(owner, title);
        init(overallBar, canCancel);
    }

    /**
     * Create a ProgressDialog with the given parent Window and title.
     * 
     * @param owner The parent Window.
     * @param title The title for the dialog.
     * 
     * @param overallBar <code>true</code> to show both progress bars, <code>false</code>
     * to show only the task progress.
     * 
     * @param canCancel If <code>true</code>, a cancel button will be available to allow
     * the background process to be stopped.
     */
    public ProgressDialog(Window owner, String title, boolean overallBar, boolean canCancel) {
        super(owner, title);
        init(overallBar, canCancel);
    }

    /**
     * Create a ProgressDialog with the given parent Frame and title.
     * 
     * @param owner The parent Frame.
     * @param title The title for the dialog.
     * 
     * @param overallBar <code>true</code> to show both progress bars, <code>false</code>
     * to show only the task progress.
     * 
     * @param canCancel If <code>true</code>, a cancel button will be available to allow
     * the background process to be stopped.
     * 
     * @param gc The <code>GraphicsConfiguration</code> of the target screen device.
     * If <code>gc</code> is <code>null</code>, the same <code>GraphicsConfiguration</code>
     * as the owning Frame is used.
     */
    public ProgressDialog(Frame owner, String title, boolean overallBar, boolean canCancel,
                          GraphicsConfiguration gc) {
        super(owner, title, false, gc);
        init(overallBar, canCancel);
    }

    /**
     * Create a ProgressDialog with the given parent Dialog and title.
     * 
     * @param owner The parent Dialog.
     * @param title The title for the dialog.
     * 
     * @param overallBar <code>true</code> to show both progress bars, <code>false</code>
     * to show only the task progress.
     * 
     * @param canCancel If <code>true</code>, a cancel button will be available to allow
     * the background process to be stopped.
     * 
     * @param gc The <code>GraphicsConfiguration</code> of the target screen device.
     * If <code>gc</code> is <code>null</code>, the same <code>GraphicsConfiguration</code>
     * as the owning Dialog is used.
     */
    public ProgressDialog(Dialog owner, String title, boolean overallBar, boolean canCancel,
                          GraphicsConfiguration gc) {
        super(owner, title, false, gc);
        init(overallBar, canCancel);
    }

    /**
     * Create a ProgressDialog with the given parent Window and title.
     * 
     * @param owner The parent Window.
     * @param title The title for the dialog.
     * 
     * @param overallBar <code>true</code> to show both progress bars, <code>false</code>
     * to show only the task progress.
     * 
     * @param canCancel If <code>true</code>, a cancel button will be available to allow
     * the background process to be stopped.
     * 
     * @param gc The <code>GraphicsConfiguration</code> of the target screen device.
     * If <code>gc</code> is <code>null</code>, the same <code>GraphicsConfiguration</code>
     * as the owning Window is used.
     */
    public ProgressDialog(Window owner, String title, boolean overallBar, boolean canCancel,
                          GraphicsConfiguration gc) {
        super(owner, title, ModalityType.MODELESS, gc);
        init(overallBar, canCancel);
    }

    /**
     * Internal common initialisation. Lays out the dialog.
     * 
     * @param overallBar Flag indicating whether the overall bar should be displayed.
     * @param canCancel Flag indicating that the cancel button should be available.
     */
    private void init(boolean overallBar, boolean canCancel) {
        
        try {
            cancelButton.setText(Messages.getMessage("cancel"));
        } catch (MissingResourceException e) {
            cancelButton.setText("Cancel");
        }
        
        taskProgressBar.setForeground(Color.green.darker().darker());
        overallProgressBar.setForeground(Color.blue.darker().darker());
        
        Container cp = getContentPane();
        cp.setLayout(new GridBagLayout());
        
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = 0;
        cons.gridy = 0;
        cons.gridheight = 1;
        cons.gridwidth = 1;
        cons.fill = GridBagConstraints.BOTH;
        cons.weightx = 1;
        cons.weighty = 0;
        cons.insets = new Insets(4, 4, 4, 4);
        
        cp.add(informationLabel, cons);
        
        cons.gridy++;
        cons.weighty = 0.5;
        cp.add(taskProgressBar, cons);
        
        cons.gridy++;
        cp.add(overallProgressBar, cons);
        
        cons.gridy++;
        cons.fill = GridBagConstraints.NONE;
        cons.weighty = 0;
        cp.add(cancelButton, cons);
        
        overallProgressBar.setVisible(overallBar);
        
        cancelButton.addActionListener(new CancelListener());
        cancelButton.setVisible(canCancel);
        
        int height = 80;
        if (overallBar) {
            height += overallProgressBar.getPreferredSize().height + 8;
        }
        if (canCancel) {
            height += cancelButton.getPreferredSize().height + 8;
        }
        setSize(200, height);
    }
    
    /**
     * Set the text in the information label.
     * 
     * @param text The text to display.
     */
    public void setInformationText(String text) {
        informationLabel.setText(text);
    }
    
    /**
     * Reset the task progress bar with the given minimum and maximum.
     * The progress is set to the minimum value.
     * 
     * @param min The minimum value.
     * @param max The maximum value.
     * 
     * @see JProgressBar#setMinimum(int)
     * @see JProgressBar#setMaximum(int)
     */
    public void resetTaskProgress(int min, int max) {
        taskProgressBar.setMinimum(min);
        taskProgressBar.setMaximum(max);
        taskProgressBar.setValue(min);
    }
    
    /**
     * Sets the task progress to be indeterminate.
     * 
     * @param indeterminate <code>true</code> for indeterminate task progress,
     * <code>false</code> for standard progress.
     * 
     * @see JProgressBar#setIndeterminate(boolean)
     */
    public void setTaskProgressIndeterminate(boolean indeterminate) {
        taskProgressBar.setIndeterminate(indeterminate);
    }
    
    /**
     * Set the current task progress.
     * 
     * @param progress The progress value.
     * 
     * @see JProgressBar#setValue(int)
     */
    public void setTaskProgress(int progress) {
        taskProgressBar.setValue(progress);
    }
    
    /**
     * Reset the overall progress bar with the given minimum and maximum.
     * The progress is set to the minimum value.
     * 
     * @param min The minimum value.
     * @param max The maximum value.
     * 
     * @see JProgressBar#setMinimum(int)
     * @see JProgressBar#setMaximum(int)
     */
    public void resetOverallProgress(int min, int max) {
        overallProgressBar.setMinimum(min);
        overallProgressBar.setMaximum(max);
        overallProgressBar.setValue(min);
    }
    
    /**
     * Sets the overall progress to be indeterminate.
     * 
     * @param indeterminate <code>true</code> for indeterminate overall progress,
     * <code>false</code> for standard progress.
     * 
     * @see JProgressBar#setIndeterminate(boolean)
     */
    public void setOverallProgressIndeterminate(boolean indeterminate) {
        taskProgressBar.setIndeterminate(indeterminate);
    }
    
    /**
     * Set the current overall progress.
     * 
     * @param progress The progress value.
     * 
     * @see JProgressBar#setValue(int)
     */
    public void setOverallProgress(int progress) {
        overallProgressBar.setValue(progress);
    }
    
    /**
     * Position this dialog over the given component (or rather centred
     * over the component's parent Window). Also makes this dialog visible
     * and raises it to the top of the window stack.
     * 
     * @param component The component.
     */
    public void positionOver(Component component) {
        Window window;
        if (component instanceof Window) {
            window = (Window) component;
        } else {
            window = SwingUtilities.getWindowAncestor(component);
        }
        
        if (window != null) {
            WindowUtils.centreOverWindow(this, window);
        }
        if (isVisible()) {
            toFront();
        } else {
            setVisible(true);
        }
    }
    
    /**
     * Set the task this dialog is watching.
     * 
     * @param task The task.
     * @param mayInterrupt Flag indicating that the task's thread can be interrupted
     * if the cancel button is pressed.
     */
    public void setTask(RunnableFuture<?> task, boolean mayInterrupt) {
        this.task = task;
        this.mayInterrupt = mayInterrupt;
    }
    
    /**
     * Listener for the cancel button. Simply calls <code>cancel</code> on the task.
     * @see java.util.concurrent.Future#cancel(boolean)
     */
    private class CancelListener implements ActionListener
    {
        public void actionPerformed(ActionEvent event) {
            if (task != null) {
                task.cancel(mayInterrupt);
            }
        }
    }
}
