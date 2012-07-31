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

import static org.intermine.install.swing.AddSourceDialog.NAME_CHARACTERS;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.intermine.common.swing.ButtonPanel;
import org.intermine.common.swing.GridBagHelper;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.StandardJDialog;
import org.intermine.common.swing.SystemProcessProgressDialog;
import org.intermine.common.swing.SystemProcessSwingWorker;
import org.intermine.common.swing.WindowUtils;
import org.intermine.common.swing.text.RestrictedInputDocument;
import org.intermine.install.project.event.ProjectListener;
import org.intermine.install.project.event.ProjectListenerSupport;
import org.intermine.install.project.source.PropertyDescriptor;
import org.intermine.install.project.source.SourceInfo;
import org.intermine.install.project.source.SourceTypeCreator;
import org.intermine.install.project.source.SourceTypeDerivation;
import org.intermine.install.swing.source.PropertyComponentCreator;
import org.intermine.install.swing.source.PropertyComponentWrapper;
import org.intermine.modelviewer.project.ObjectFactory;
import org.intermine.modelviewer.project.Project;
import org.intermine.modelviewer.project.Property;
import org.intermine.modelviewer.project.Source;


/**
 * Dialog for creating new derived data source types.
 */
public class NewDerivedTypeDialog extends StandardJDialog
{
    private static final long serialVersionUID = -6285785120096796643L;

    /**
     * Read-only text field for the name of the data source.
     * @serial
     */
    private JTextField nameField = new JTextField(24);
    
    /**
     * Read-only text field for the name of the parent source type.
     * @serial
     */
    private JTextField typeField = new JTextField(24);
    
    /**
     * Text field for the name for the derived source type.
     * @serial 
     */
    private JTextField derivedTypeField =
        new JTextField(new RestrictedInputDocument(NAME_CHARACTERS, true), "", 24);
    
    /**
     * Empty JPanel used to allow elastic spacing below the fields in the
     * dialog.
     * @serial
     */
    private JPanel spacerPanel = new JPanel();
    
    /**
     * The panel containing the dialog's buttons.
     * @serial
     */
    private ButtonPanel buttonPanel;
    
    
    /**
     * Progress dialog to monitor the <code>make_source</code> script.
     * @serial
     */
    private SystemProcessProgressDialog progressDialog;
    
    /**
     * Action to create the data source.
     * @serial
     */
    private Action createAction = new CreateAction();
    
    /**
     * A list of the individual components added to this dialog for each property
     * required from the data source type's <i>derivation</i> section.
     * @serial
     */
    private List<JComponent> additionalFields = new ArrayList<JComponent>();
    
    /**
     * A list of the wrappers around all fields added per property
     * from the data source type's <i>derivation</i> section.
     * @serial
     */
    private List<PropertyComponentWrapper> wrappers = new ArrayList<PropertyComponentWrapper>();
    
    /**
     * Support for firing <code>ProjectEvent</code>s.
     * @serial
     */
    private ProjectListenerSupport projectListenerSupport = new ProjectListenerSupport(this);
    
    /**
     * The Project object being edited.
     */
    private transient Project project;
    
    /**
     * The parent data source's information.
     */
    private transient SourceInfo parentInfo;
    
    /**
     * A handler for dealing with existing source types.
     */
    private transient SourceTypeCreator sourceTypeCreator;


    /**
     * Initialise with a parent Frame.
     * @param owner The parent Frame.
     */
    public NewDerivedTypeDialog(Frame owner) {
        super(owner);
        progressDialog = new SystemProcessProgressDialog(owner);
        init();
    }

    /**
     * Initialise with a parent Dialog.
     * @param owner The parent Dialog.
     */
    public NewDerivedTypeDialog(Dialog owner) {
        super(owner);
        progressDialog = new SystemProcessProgressDialog(owner);
        init();
    }

    /**
     * Initialise with a parent Window.
     * @param owner The parent Window.
     */
    public NewDerivedTypeDialog(Window owner) {
        super(owner);
        progressDialog = new SystemProcessProgressDialog(owner);
        init();
    }

    /**
     * Common initialisation: lays out the child components and wires up the necessary
     * event listeners. 
     */
    private void init() {
        setName("New Derived Type Dialog");
        setTitle(Messages.getMessage("source.derived.title"));
        progressDialog.setTitle(Messages.getMessage("makesource.title"));
        
        nameField.setEnabled(false);
        typeField.setEnabled(false);
        
        buttonPanel = new ButtonPanel(createAction, new CancelAction());
        
        progressDialog.setInformationLabel(Messages.getMessage("makesource.message"));
        progressDialog.pack();
        
        Container cp = getContentPane();
        GridBagConstraints cons = GridBagHelper.setup(cp);
        
        cp.add(new JLabel(Messages.getMessage("source.name")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("source.derived.parent")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("source.derived.type")), cons);
        
        cons.gridy = 0;
        cons.gridx++;
        cons.weightx = 1;
        cp.add(nameField, cons);
        
        cons.gridy++;
        cp.add(typeField, cons);
        
        cons.gridy++;
        cp.add(derivedTypeField, cons);
        
        cons.gridx = 0;
        cons.gridy++;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.weighty = 1;
        cons.fill = GridBagConstraints.BOTH;
        cp.add(spacerPanel, cons);
        
        cons.gridy++;
        cons.weighty = 0;
        cons.fill = GridBagConstraints.HORIZONTAL;
        cp.add(buttonPanel, cons);

        derivedTypeField.getDocument().addDocumentListener(new DerivedTypeDocumentListener());
        
        pack();
    }
    
    /**
     * Set the Intermine home directory.
     * 
     * @param intermineHome The Intermine home directory.
     */
    public void setIntermineHome(File intermineHome) {
        sourceTypeCreator = new SourceTypeCreator(intermineHome);
    }
    
    /**
     * Initialise to show the properties needed to complete to create the
     * derived source from the given parent.
     * 
     * @param project The Project being edited.
     * @param parentType The parent data source type descriptor.
     * @param name The name of the actual data source to add to the project.
     */
    public void initialise(Project project, SourceInfo parentType, String name) {
        Container cp = getContentPane();
        
        this.project = project;
        parentInfo = parentType;
        
        nameField.setText(name);
        typeField.setText(parentType.getSource().getType());
        derivedTypeField.setText(name);
        
        for (JComponent c : additionalFields) {
            cp.remove(c);
        }
        cp.remove(spacerPanel);
        cp.remove(buttonPanel);
        
        additionalFields.clear();
        wrappers.clear();
        
        GridBagConstraints cons = GridBagHelper.defaultConstraints();
        
        cons.gridy = 3;
        
        SourceTypeDerivation derivation = parentInfo.getSource().getDerivation();
        assert derivation != null : "No derivation on source";
        for (PropertyDescriptor descriptor : derivation.getProperty()) {
            
            JLabel label = new JLabel(descriptor.getName());
            cons.gridx = 0;
            cons.weightx = 0;
            cp.add(label, cons);
            additionalFields.add(label);
            
            PropertyComponentWrapper wrapper =
                PropertyComponentCreator.createComponentFor(descriptor.getName(), descriptor, "");
            cons.weightx = 1;
            cons.gridx++;
            cp.add(wrapper.getDisplayComponent(), cons);
            additionalFields.add(wrapper.getDisplayComponent());
            wrappers.add(wrapper);
            
            cons.gridy++;
        }
        
        cons.gridx = 0;
        cons.weightx = 1;
        cons.weighty = 1;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.fill = GridBagConstraints.BOTH;
        cp.add(spacerPanel, cons);
        
        cons.gridy++;
        cons.weighty = 0;
        cons.fill = GridBagConstraints.HORIZONTAL;
        cp.add(buttonPanel, cons);
        
        pack();
    }
    
    /**
     * Updates the enabled state of the "create" action by checking whether
     * all required fields are valid.
     */
    protected void updateState() {
        boolean ok = StringUtils.isNotEmpty(derivedTypeField.getText());
        for (PropertyComponentWrapper wrapper : wrappers) {
            ok = ok && wrapper.isSet();
        }
        createAction.setEnabled(ok);
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
     * Create a new <code>Source</code> object and add it to the edited <code>Project</code>.
     * Fires a <code>ProjectEvent</code> via {@link ProjectListener#sourceAdded}.
     */
    protected void addSourceToProject() {
        ObjectFactory objFactory = new ObjectFactory();
        
        Source newSource = objFactory.createSource();
        newSource.setName(nameField.getText());
        newSource.setType(derivedTypeField.getText());
        newSource.setDump(Boolean.FALSE);
        project.getSources().getSource().add(newSource);
        
        for (PropertyDescriptor descriptor : parentInfo.getSource().getProperty()) {
            String defaultValue = parentInfo.getDefaults().getProperty(descriptor.getName());
            
            if (defaultValue != null || descriptor.isRequired()) {
                if (defaultValue == null) {
                    defaultValue = "";
                }
                
                Property p = objFactory.createProperty();
                p.setName(descriptor.getName());
                switch (descriptor.getType()) {
                    case FILE:
                    case DIRECTORY:
                        p.setLocation(defaultValue);
                        break;
                        
                    default:
                        p.setValue(defaultValue);
                        break;
                }
                newSource.getProperty().add(p);
            }
        }
        
        projectListenerSupport.fireSourceAdded(project, newSource);
    }
    
    /**
     * Action to create the new derived source by executing the
     * <code>make_source</code> script. Uses a <code>SystemProcessSwingWorker</code>
     * to run the script in the background.
     *
     * @see SystemProcessSwingWorker
     */
    private class CreateAction extends AbstractAction
    {
        private static final long serialVersionUID = 3846676598542792464L;

        /**
         * Constructor.
         */
        public CreateAction() {
            super(Messages.getMessage("create"));
            setEnabled(false);
        }

        /**
         * Called to run the <code>make_source</code> script and to create
         * the derived source.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            String sourceTypeName = derivedTypeField.getText();
            if (sourceTypeCreator.doesSourceExist(sourceTypeName)) {
                
                boolean correct =
                    sourceTypeCreator.isSourceCorrectType(sourceTypeName, parentInfo);

                if (!correct) {
                    int choice =
                        JOptionPane.showConfirmDialog(
                                NewDerivedTypeDialog.this,
                                Messages.getMessage("makesource.incorrecttype.message",
                                                    sourceTypeName, parentInfo.getType()),
                                Messages.getMessage("makesource.incorrecttype.title"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                    
                    if (choice == JOptionPane.YES_OPTION) {
                        try {
                            FileUtils.deleteDirectory(
                                    sourceTypeCreator.getSourceDirectory(sourceTypeName));
                        } catch (IOException e) {
                            showExceptionDialog(e, "makesource.exists.cleanup.title",
                                                "makesource.exists.cleanup.message");
                            return;
                        }
                    }
                } else {
                    // If correct type, use this type and return.
                    logger.info("Source type " + sourceTypeName
                                + " already exists and is the correct type.");
                    addSourceToProject();
                    setVisible(false);
                    return;
                }                    
            }
            
            List<String> commands = new ArrayList<String>();
            commands.add("bio/scripts/make_source");
            commands.add(derivedTypeField.getText());
            commands.add(parentInfo.getType());
            
            for (PropertyComponentWrapper wrapper : wrappers) {
                commands.add(wrapper.getValue());
            }
            
            StringBuilder b = new StringBuilder();
            Iterator<String> iter = commands.iterator();
            while (iter.hasNext()) {
                b.append(iter.next());
                if (iter.hasNext()) {
                    b.append(' ');
                }
            }
            logger.debug(b);
            
            SystemProcessSwingWorker worker =
                new SystemProcessSwingWorker(commands, sourceTypeCreator.getIntermineHome(), true);
            worker.addPropertyChangeListener(new MakeSourcePropertyListener());
            progressDialog.setWorker(worker);
            progressDialog.writeOutput(b + "\n\n");
            
            WindowUtils.centreOverWindow(progressDialog, NewDerivedTypeDialog.this);
            
            logger.info("Creating source type " + sourceTypeName
                        + " from the parent type " + parentInfo.getType());
            
            worker.execute();
            progressDialog.setVisible(true);
            
            setEnabled(false);
        }
    }
    
    /**
     * Property change listener to listen to the worker running the
     * <code>make_source</code> script.
     * 
     * @see SystemProcessSwingWorker
     */
    private class MakeSourcePropertyListener implements PropertyChangeListener
    {
        /**
         * Property change listener method. When the "complete" event arrives,
         * see if the process was successful. If it was, add a Source to the
         * Project. If not, display an error message as to why.
         * 
         * @param event The property change event.
         * 
         * @see NewDerivedTypeDialog#addSourceToProject()
         */
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            SystemProcessSwingWorker worker =
                (SystemProcessSwingWorker) event.getSource();
            
            if (SystemProcessSwingWorker.COMPLETE.equals(event.getPropertyName())) {
                updateState();
                
                boolean failed = true;
                Throwable failure = worker.getHaltingException();
                if (failure == null) {
                    String innerMessage = null;
                    Integer exitCode = worker.getExitCode();
                    if (exitCode == null || exitCode.intValue() != 0) {
                        innerMessage = Messages.getMessage("makesource.failure.exitcode", exitCode);
                    } else {
                        failed = false;
                    }
                    
                    if (failed) {
                        String errorMessage =
                            Messages.getMessage("makesource.failure.message", innerMessage);
                        
                        JOptionPane.showMessageDialog(
                                progressDialog,
                                errorMessage,
                                Messages.getMessage("makesource.failure.title"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    showExceptionDialog(failure, "makemine.failure.title",
                                        "makemine.failure.message");
                }
                
                if (!failed) {
                    addSourceToProject();
                    
                    progressDialog.setVisible(false);
                    setVisible(false);
                }
            }
        }
    }
    
    /**
     * Document listener for required text fields to make sure the "create" action
     * cannot be launched if the text fields are not valid.
     * 
     * @see NewDerivedTypeDialog#updateState()
     */
    private class DerivedTypeDocumentListener implements DocumentListener
    {
        @Override
        public void changedUpdate(DocumentEvent e) {
            updateState();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateState();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateState();
        }
    }
}
