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
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang.StringUtils;
import org.intermine.common.swing.ButtonPanel;
import org.intermine.common.swing.GridBagHelper;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.StandardJDialog;
import org.intermine.common.swing.SystemProcessProgressDialog;
import org.intermine.common.swing.SystemProcessSwingWorker;
import org.intermine.common.swing.WindowUtils;
import org.intermine.common.swing.text.RestrictedInputDocument;


/**
 * Dialog to control the <code>project_build</code> Intermine script and to
 * display its output as it runs.
 */
public class BuildProjectDialog extends StandardJDialog
{
    private static final long serialVersionUID = -523173830816907855L;


    /**
     * Start policy group label.
     * @serial
     */
    private JLabel startPolicyLabel =
        new JLabel(Messages.getMessage("build.project.start.policy"));

    /**
     * Restart with last dump file radio button.
     * @serial
     */
    private JRadioButton restart1RadioButton
        = new JRadioButton(Messages.getMessage("build.project.flag.l"));

    /**
     * Restart without loading dump radio button.
     * @serial
     */
    private JRadioButton restart2RadioButton
        = new JRadioButton(Messages.getMessage("build.project.flag.r"));

    /**
     * Run <code>build-db</code> before build radio button.
     * @serial
     */
    private JRadioButton buildDbRadioButton
        = new JRadioButton(Messages.getMessage("build.project.flag.b"), true);

    /**
     * Test run radio button.
     * @serial
     */
    private JRadioButton testRadioButton
        = new JRadioButton(Messages.getMessage("build.project.flag.n"));

    /**
     * Set release number check box.
     * @serial
     */
    private JCheckBox releaseNumberCheckBox =
        new JCheckBox(Messages.getMessage("build.project.flag.V"));

    /**
     * Release number text field label.  TODO I think this be deleted
     *
     * @serial
     */
    private JLabel releaseNumberLabel =
        new JLabel(Messages.getMessage("build.project.release.number"));

    /**
     * Release number text field.
     *
     * @serial
     */
    private JTextField releaseNumberTextField =
        new JTextField(
                new RestrictedInputDocument(
                        RestrictedInputDocument.WORD_CHARACTERS + ".+/"), "", 20);

    /**
     * Make database backups check box.
     * @serial
     */
//    private JCheckBox serverBackupCheckBox =
//        new JCheckBox(Messages.getMessage("build.project.flag.T"));

    /**
     * Create destination database check box.
     * @serial
     */
    private JCheckBox destinationCheckBox =
        new JCheckBox(Messages.getMessage("build.project.flag.D"));

    /**
      * Create label for userprofile options group
      * @serial
      */
    private JLabel userDBOptionsLabel =
        new JLabel(Messages.getMessage("build.projectuserprofile.label"));

    /**
     * Create write userprofile database without override option radio button
     * @serial
     */
    private JRadioButton writeUserDbRadio =
        new JRadioButton(Messages.getMessage("build.project.flag.u"));

    /**
      * Create force write userprofile database option radio button
      * @serial
      */
    private JRadioButton overwriteUserDbRadio =
        new JRadioButton(Messages.getMessage("build.project.flag.U"));

   /**
     * Create do not write userprofile database option radio button
     * @serial
     */
    private JRadioButton nowriteUserDbRadio =
        new JRadioButton(Messages.getMessage("build.project.NOWRITE"), true);

   /** 
    * Database encoding drop down list. 
    */  
    private JComboBox encodingDropdown; 
    
    private String encodings[] = { "UTF8", "ASCII" }; 
    
    /**
      * Database encoding text field label
      * @serial
      */
    private JLabel encodingLabel =
        new JLabel(Messages.getMessage("build.project.db.encoding"));

    /**
      * Database encoding text field
      * @serial
      */
    private JTextField encodingTextField =
        new JTextField(
                new RestrictedInputDocument(
                    RestrictedInputDocument.UPPER_CASE
                    + RestrictedInputDocument.DIGITS + "_", true), "UTF8", 20);
    /**
     * Destination database text field label.
     * @serial
     */
    private JLabel destinationLabel =
        new JLabel(Messages.getMessage("build.project.destination.db"));

    /**
     * Destination database text field.
     * @serial
     */
    private JTextField destinationTextField =
        new JTextField(
                new RestrictedInputDocument(
                        RestrictedInputDocument.WORD_CHARACTERS), "", 20);

    /**
     * Dump host text field.
     * @serial
     */
    private JTextField hostTextField =
        new JTextField(
                new RestrictedInputDocument(
                        RestrictedInputDocument.LOWER_CASE
                        + RestrictedInputDocument.DIGITS + "_-.", true), "", 20);

    /**
     * Dump prefix text field.
     * @serial
     */
    private JTextField prefixTextField
        = new JTextField(Messages.getMessage("build.project.dump.prefix.default"), 20);

    /**
     * Action to start running <code>project_build</code>.
     * @serial
     */
    private Action buildAction = new BuildProjectAction();

    /**
     * Progress dialog to display the output from <code>project_build</code>.
     * @serial
     */
    private SystemProcessProgressDialog progressDialog;

    /**
     * The project file for the mine.
     */
    private transient File projectFile;


    /**
     * Initialise with a parent Frame.
     * @param owner The parent Frame.
     */
    public BuildProjectDialog(Frame owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Dialog.
     * @param owner The parent Dialog.
     */
    public BuildProjectDialog(Dialog owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Window.
     * @param owner The parent Window.
     */
    public BuildProjectDialog(Window owner) {
        super(owner);
        init();
    }

    /**
     * Common initialisation: lays out the child components and wires up the necessary
     * event listeners.
     */
    private void init() {


        setName("Build Project Dialog");
        setTitle(Messages.getMessage("build.project.title"));

        releaseNumberLabel.setLabelFor(releaseNumberTextField);
        destinationLabel.setLabelFor(destinationTextField);

        Container cp = getContentPane();

        GridBagConstraints cons = GridBagHelper.setup(cp);

        JLabel infoLabel = new JLabel(Messages.getMessage("build.project.info.message"));
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN));
        cons.gridwidth = 2;
        cons.weightx = 1;
        cp.add(infoLabel, cons);

        JPanel startPolicyPanel = new JPanel();
//        startPolicyPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        GridBagConstraints startPolicyPanelCons = GridBagHelper.setup(startPolicyPanel);
        startPolicyPanelCons.gridwidth = GridBagConstraints.REMAINDER;
        startPolicyPanel.add(startPolicyLabel, startPolicyPanelCons);

        startPolicyPanelCons.gridy++;
        startPolicyPanelCons.gridwidth = 2;
        buildDbRadioButton.setFont(infoLabel.getFont().deriveFont(Font.PLAIN));
        startPolicyPanel.add(buildDbRadioButton, startPolicyPanelCons);

        startPolicyPanelCons.gridy++;
        restart1RadioButton.setFont(infoLabel.getFont().deriveFont(Font.PLAIN));
        startPolicyPanel.add(restart1RadioButton, startPolicyPanelCons);

//        startPolicyPanelCons.gridy++;
//        restart2RadioButton.setFont(infoLabel.getFont().deriveFont(Font.PLAIN));
//        startPolicyPanel.add(restart2RadioButton, startPolicyPanelCons);
//
//        startPolicyPanelCons.gridy++;
//        testRadioButton.setFont(infoLabel.getFont().deriveFont(Font.PLAIN));
//        startPolicyPanel.add(testRadioButton, startPolicyPanelCons);

        cons.gridy++;
        cp.add(startPolicyPanel, cons);

        JPanel userDBPanel = new JPanel();
//        userDBPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        GridBagConstraints userDBCons = GridBagHelper.setup(userDBPanel);
        userDBPanel.setAlignmentY(LEFT_ALIGNMENT);
//        cons.gridwidth = GridBagConstraints.REMAINDER;

        userDBPanel.add(userDBOptionsLabel, userDBCons);

//        userDBCons.gridy++;
//        nowriteUserDbRadio.setFont(infoLabel.getFont().deriveFont(Font.PLAIN));
//        userDBPanel.add(nowriteUserDbRadio, userDBCons);
//
//        userDBCons.gridy++;
//        writeUserDbRadio.setFont(infoLabel.getFont().deriveFont(Font.PLAIN));
//        userDBPanel.add(writeUserDbRadio, userDBCons);
//
//        userDBCons.gridy++;
//        overwriteUserDbRadio.setFont(infoLabel.getFont().deriveFont(Font.PLAIN));
//        userDBPanel.add(overwriteUserDbRadio, userDBCons);
//
//        cons.gridy++;
//        cp.add(userDBPanel, cons);

        cons.gridy++;
        cons.gridx = 0;
        cons.weightx = 0;
        cons.gridwidth = 1;
        cp.add(encodingLabel, cons);

        cons.gridx++;
        cons.weightx = 0.5;
        encodingDropdown = new JComboBox(encodings);
        cp.add(encodingDropdown, cons);
        //cp.add(encodingTextField, cons);

//        cons.gridy++;
//        cons.gridx = 0;
//        cons.gridwidth = 3;
//        cons.weightx = 1;
//        cp.add(serverBackupCheckBox, cons);

//        cons.gridy++;
//        cons.gridx = 0;
//        cons.weightx = 0;
//        cons.gridwidth = 1;
//        cp.add(destinationCheckBox, cons);

//        cons.gridy++;
//        cons.weightx = 0;
//        cons.gridwidth = 1;
//        cp.add(destinationLabel, cons);

//        cons.gridx++;
//        cons.weightx = 0.75;
//        cons.gridwidth = 2;
//        cp.add(destinationTextField, cons);

        cons.gridy++;
        cons.gridx = 0;
        cons.weightx = 0;
        cons.gridwidth = 1;
        cp.add(new JLabel(Messages.getMessage("build.project.dump.host")), cons);

        cons.gridx++;
        cons.weightx = 0.75;
        cons.gridwidth = 1;
        cp.add(hostTextField, cons);

        cons.gridy++;
        cons.gridx = 0;
        cons.weightx = 0;
        cons.gridwidth = 1;
        cp.add(new JLabel(Messages.getMessage("build.project.dump.prefix")), cons);

        cons.gridx++;
        cons.weightx = 0.75;
        cons.gridwidth = 1;
        cp.add(prefixTextField, cons);

        cons.gridy++;
        cons.gridx = 0;
        cp.add(releaseNumberCheckBox, cons);

//        cons.gridy++;
//        cons.weightx = 0;
//        cons.gridwidth = 1;
//        cp.add(releaseNumberLabel, cons);

        cons.gridx++;
        cons.weightx = 0.5;
        cp.add(releaseNumberTextField, cons);

        cons.gridy++;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.gridx = 0;
        cons.weightx = 1;
        cp.add(new ButtonPanel(buildAction, new CancelAction()), cons);

       // encodingTextField.setText("SQL_ASCII");
//        releaseNumberLabel.setEnabled(false);
        releaseNumberTextField.setEnabled(false);
//        destinationLabel.setEnabled(false);
//        destinationTextField.setEnabled(false);


        /**
            * Create Button group for userdb radio buttons
            * @serial
        */
        ButtonGroup userDBButtonGroup =
            new ButtonGroup();
        userDBButtonGroup.add(writeUserDbRadio);
        userDBButtonGroup.add(overwriteUserDbRadio);
        userDBButtonGroup.add(nowriteUserDbRadio);

        ButtonGroup startPolicyGroup =
            new ButtonGroup();
        startPolicyGroup.add(restart1RadioButton);
        startPolicyGroup.add(restart2RadioButton);
        startPolicyGroup.add(buildDbRadioButton);
        startPolicyGroup.add(testRadioButton);

        releaseNumberCheckBox.addActionListener(
                new LinkedFieldListener(releaseNumberCheckBox, releaseNumberLabel, releaseNumberTextField));
        destinationCheckBox.addActionListener(
                new LinkedFieldListener(destinationCheckBox, destinationLabel,
                                        destinationTextField));

        DocumentListener requiredTextListener = new RequiredTextListener();
        releaseNumberTextField.getDocument().addDocumentListener(requiredTextListener);
//        destinationTextField.getDocument().addDocumentListener(requiredTextListener);
        hostTextField.getDocument().addDocumentListener(requiredTextListener);
        prefixTextField.getDocument().addDocumentListener(requiredTextListener);

        hostTextField.setToolTipText(Messages.getMessage("build.project.dump.host.tooltip"));
        hostTextField.setText("localhost");

        pack();

        progressDialog = new SystemProcessProgressDialog(this);
        progressDialog.setModalityType(ModalityType.APPLICATION_MODAL);
        progressDialog.setTitle(Messages.getMessage("build.project.title"));
        progressDialog.setInformationLabel(Messages.getMessage("build.project.message"));
    }

    /**
     * Initialise this dialog to set the project file.
     *
     * @param projectFile The project file.
     */
    public void initialise(File projectFile) {
        this.projectFile = projectFile;
    }

    /**
     * Update the state of the "build project" action based on fields that require
     * input being completed correctly.
     */
    protected void updateState() {
        boolean ok = StringUtils.isNotEmpty(hostTextField.getText());
        ok = ok && StringUtils.isNotEmpty(prefixTextField.getText());
        if (ok && releaseNumberCheckBox.isSelected()) {
            ok = ok && StringUtils.isNotEmpty(releaseNumberTextField.getText());
        }
        if (ok && destinationCheckBox.isSelected()) {
            ok = ok && StringUtils.isNotEmpty(destinationTextField.getText());
        }
        buildAction.setEnabled(ok);
    }


    /**
     * Action listener for check boxes controlling whether other components are relevant
     * (and so enabled).
     */
    private class LinkedFieldListener implements ActionListener
    {
        /**
         * The controlling check box.
         */
        private JCheckBox checkBox;

        /**
         * The components to enable and disable as a result of the check box change.
         */
        private JComponent[] components;

        /**
         * Initialise with the source check box and the components it is controlling.
         *
         * @param checkBox The source check box.
         * @param linked The components under control.
         */
        private LinkedFieldListener(JCheckBox checkBox, JComponent... linked) {
            this.checkBox = checkBox;
            components = linked;
        }

        /**
         * Turn the linked components on or off as a result of this event from the
         * source check box.
         *
         * @param event The action event object.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            assert event.getSource() == checkBox : "Unexpected source of the event";

            boolean on = checkBox.isSelected();
            for (JComponent c : components) {
                c.setEnabled(on);
            }
        }
    }

    /**
     * Document listener for all text fields that are required for the project build to
     * take place.
     *
     * @see BuildProjectDialog#updateState()
     */
    private class RequiredTextListener implements DocumentListener
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

    /**
     * Action to start the <code>project_build</code> executing and to launch the progress dialog
     * so the user can see its output.
     */
    private class BuildProjectAction extends AbstractAction
    {
        private static final long serialVersionUID = -6752372757098098746L;

        /**
         * Constructor.
         */
        public BuildProjectAction() {
            super(Messages.getMessage("buildproject"));
            setEnabled(false);
        }

        /**
         * Called to start the execution of <code>project_build</code>. Starts the worker
         * thread and displays the progress dialog.
         *
         * @param event The action event object.
         */
        @Override
        public void actionPerformed(ActionEvent event) {

            File projectHome = projectFile.getParentFile();
            File intermineHome = projectHome.getParentFile();

            File projectBuildScript = new File(intermineHome, "bio/scripts/project_build");

            List<String> commands = new ArrayList<String>();
            commands.add(projectBuildScript.getAbsolutePath());
            commands.add("-v"); // always be verbose

            if (restart1RadioButton.isSelected()) {
                commands.add("-l");
            }
            else if (restart2RadioButton.isSelected()) {
                commands.add("-r");
            }
            else if (buildDbRadioButton.isSelected()) {
                commands.add("-b");
            }
            else if (testRadioButton.isSelected()) {
                commands.add("-n");
            }

            if (writeUserDbRadio.isSelected()) {
                commands.add("-u");
            }
            else if (overwriteUserDbRadio.isSelected()) {
                commands.add("-U");
            }

            if (releaseNumberCheckBox.isSelected()) {
                assert StringUtils.isNotEmpty(releaseNumberTextField.getText())
                       : "No release number set";
                commands.add("-V");
                commands.add(releaseNumberTextField.getText());
            }
//            if (serverBackupCheckBox.isSelected()) {
//                commands.add("-T");
//            }
            if (destinationCheckBox.isSelected()) {
                assert StringUtils.isNotEmpty(destinationTextField.getText())
                       : "No destination database set";
                commands.add("-D");
                commands.add(destinationTextField.getText());
            }

//            assert StringUtils.isNotEmpty(encodingTextField.getText()) : "No encoding value";
            commands.add("-E");
//            commands.add(encodingTextField.getText());
            String chosenEncoding = (String) encodingDropdown.getSelectedItem();
            commands.add(chosenEncoding);

            assert StringUtils.isNotEmpty(hostTextField.getText()) : "No host value";
            commands.add(hostTextField.getText());

            assert StringUtils.isNotEmpty(prefixTextField.getText()) : "No prefix";
            commands.add('"' + prefixTextField.getText() + '"');

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
                new SystemProcessSwingWorker(commands, projectHome, true);

            progressDialog.setWorker(worker);
            progressDialog.writeOutput(b + "\n\n");
            WindowUtils.centreOverWindow(progressDialog, BuildProjectDialog.this);
            worker.execute();
            progressDialog.setVisible(true);
        }
    }
}
