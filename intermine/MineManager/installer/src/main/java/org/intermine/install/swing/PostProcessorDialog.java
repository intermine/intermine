package org.intermine.install.swing;

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
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.intermine.common.swing.ButtonPanel;
import org.intermine.common.swing.GridBagHelper;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.StandardJDialog;
import org.intermine.install.project.event.ProjectListener;
import org.intermine.install.project.event.ProjectListenerSupport;
import org.intermine.install.project.postprocessing.PostProcessorInfo;
import org.intermine.install.project.postprocessing.PostProcessorLoader;
import org.intermine.modelviewer.project.ObjectFactory;
import org.intermine.modelviewer.project.PostProcess;
import org.intermine.modelviewer.project.Project;


/**
 * Dialog for selecting post processors of a project.
 */
public class PostProcessorDialog extends StandardJDialog
{
    private static final long serialVersionUID = 6513988414915957335L;

    /**
     * A map of post processor name to its on/off check box.
     * @serial
     */
    private Map<String, JCheckBox> selectedCheckBoxes = new HashMap<String, JCheckBox>();
    
    /**
     * A map of post processor name to its "dump" chek box.
     * @serial
     */
    private Map<String, JCheckBox> dumpCheckBoxes = new HashMap<String, JCheckBox>();

    /**
     * Action to update the Project with the post processors from this dialog.
     * @serial
     */
    private Action saveAction = new SaveAction();
    
    /**
     * Support for firing <code>ProjectEvent</code>s.
     * @serial
     */
    private ProjectListenerSupport projectListenerSupport = new ProjectListenerSupport(this);
    
    
    /**
     * The Project being edited.
     */
    private transient Project project;
    
    
    /**
     * Initialise with a parent Frame.
     * @param owner The parent Frame.
     */
    public PostProcessorDialog(Frame owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Dialog.
     * @param owner The parent Dialog.
     */
    public PostProcessorDialog(Dialog owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Window.
     * @param owner The parent Window.
     */
    public PostProcessorDialog(Window owner) {
        super(owner);
        init();
    }

    /**
     * Common initialisation: lays out the child components and wires up the necessary
     * event listeners. 
     */
    private void init() {
        setName("Post Processor Dialog");
        setTitle(Messages.getMessage("postprocess.dialog.title"));
        Container cp = getContentPane();
        
        GridBagConstraints cons = GridBagHelper.setup(cp);
        
        List<PostProcessorInfo> infoList = PostProcessorLoader.getInstance().getPostProcessorInfo();
        
        ItemListener selectedBoxListener = new SelectedCheckBoxListener();
        boolean recommendedLabel = false, otherLabel = false;
        
        for (PostProcessorInfo info : infoList) {
            if (info.isRecommended() && !recommendedLabel) {
                cons.gridx = 0;
                cons.gridwidth = GridBagConstraints.REMAINDER;
                cons.weightx = 1;
                cp.add(new JLabel(Messages.getMessage("postprocess.recommended")), cons);
                cons.gridy++;
                recommendedLabel = true;
            }
            if (!info.isRecommended() && !otherLabel) {
                cons.gridx = 0;
                cons.gridwidth = GridBagConstraints.REMAINDER;
                cons.weightx = 1;
                cp.add(new JLabel(Messages.getMessage("postprocess.others")), cons);
                cons.gridy++;
                otherLabel = true;
            }
            
            cons.gridwidth = 1;
            cons.weightx = 1;
            cons.gridx = 0;
            JCheckBox cbox = new JCheckBox(info.getName());
            cbox.setName(info.getName());
            cbox.addItemListener(selectedBoxListener);
            selectedCheckBoxes.put(info.getName(), cbox);
            cp.add(cbox, cons);
            
            cons.weightx = 0;
            cons.gridx = 1;
            JCheckBox dbox = new JCheckBox(Messages.getMessage("postprocess.dump"));
            dbox.setEnabled(false);
            dumpCheckBoxes.put(info.getName(), dbox);
            cp.add(dbox, cons);
            
            cons.gridy++;
        }
        
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.weightx = 1;
        cp.add(new ButtonPanel(saveAction, new CancelAction()), cons);
        
        pack();
    }
    
    /**
     * Initialise by setting the field of the dialog from the post processors of the
     * given Project.
     * 
     * @param project The Project.
     */
    public void initialise(Project project) {
        this.project = project;
        if (project != null) {
            for (JCheckBox cbox : selectedCheckBoxes.values()) {
                cbox.setSelected(false);
            }
            for (JCheckBox cbox : dumpCheckBoxes.values()) {
                cbox.setSelected(false);
                cbox.setEnabled(false);
            }
            
            for (PostProcess pp : project.getPostProcessing().getPostProcess()) {
                JCheckBox cbox = selectedCheckBoxes.get(pp.getName());
                if (cbox != null) {
                    cbox.setSelected(true);
                    
                    JCheckBox dbox = dumpCheckBoxes.get(pp.getName());
                    assert dbox != null;
                    dbox.setSelected(Boolean.TRUE.equals(pp.isDump()));
                }
            }
        }
        
        saveAction.setEnabled(project != null);
    }
    
    /**
     * Update the edited project's post processors from the check boxes of
     * this dialog.
     * <p>Fires a <code>ProjectEvent</code> via {@link ProjectListener#postProcessorsChanged}.</p>
     */
    public void save() {
        if (project != null) {
            ObjectFactory factory = new ObjectFactory();
            
            List<PostProcess> ppList = project.getPostProcessing().getPostProcess();
            Map<String, PostProcess> currentPPMap = new HashMap<String, PostProcess>();
            for (PostProcess pp : ppList) {
                currentPPMap.put(pp.getName(), pp);
            }
            
            boolean changed = false; 
            for (JCheckBox ppBox : selectedCheckBoxes.values()) {
                String pp = ppBox.getName();
                
                JCheckBox dbox = dumpCheckBoxes.get(pp);
                PostProcess currentPP = currentPPMap.get(pp);
                
                if (ppBox.isSelected()) {
                    if (currentPP == null) {
                        currentPP = factory.createPostProcess();
                        currentPP.setName(pp);
                        ppList.add(currentPP);
                        
                        changed = true;
                    }
                    
                    boolean previousDump = Boolean.TRUE.equals(currentPP.isDump());
                    changed = changed || dbox.isSelected() == previousDump;
                    
                    currentPP.setDump(dbox.isSelected());
                } else {
                    if (currentPP != null) {
                        ppList.remove(currentPP);
                        
                        changed = true;
                    }
                }
            }
            
            if (changed) {
                projectListenerSupport.firePostProcessorsChanged(project);
            }
        }
    }
    
    /**
     * Add a ProjectListener to this dialog.
     * @param l The ProjectListener.
     */
    public void addProjectListener(ProjectListener l) {
        projectListenerSupport.addProjectListener(l);
    }
    
    /**
     * Remove a ProjectListener from this dialog.
     * @param l The ProjectListener.
     */
    public void removeProjectListener(ProjectListener l) {
        projectListenerSupport.removeProjectListener(l);
    }
    
    /**
     * Action to update the post processors of the edited Project.
     */
    private class SaveAction extends AbstractAction
    {
        private static final long serialVersionUID = -8303263351934533082L;

        /**
         * Constructor.
         */
        public SaveAction() {
            super(Messages.getMessage("save"));
            setEnabled(false);
        }

        /**
         * Called to update the project's post processors. 
         * 
         * @param event The action event.
         * 
         * @see PostProcessorDialog#save()
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            assert project != null;
            save();
            setVisible(false);
        }
    }
    
    /**
     * Check box to enable or disable the "dump" check box for a post
     * processor depending on whether that post processor is selected.
     */
    private class SelectedCheckBoxListener implements ItemListener
    {
        /**
         * Called when the post processor is selected or deselected. Updates
         * the enabled state of the corresponding "dump" check box accordingly.
         * 
         * @param event The item event.
         */
        @Override
        public void itemStateChanged(ItemEvent event) {
            JCheckBox source = (JCheckBox) event.getSource();
            JCheckBox dump = dumpCheckBoxes.get(source.getName());
            assert dump != null;
            dump.setEnabled(source.isSelected());
        }
    }
}
