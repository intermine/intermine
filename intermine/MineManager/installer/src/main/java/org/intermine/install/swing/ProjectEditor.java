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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.MouseInfo;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.Box;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.intermine.common.swing.GridBagHelper;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.WindowUtils;
import org.intermine.install.project.event.ProjectAdapter;
import org.intermine.install.project.event.ProjectEvent;
import org.intermine.install.project.event.ProjectListener;
import org.intermine.install.project.event.ProjectListenerSupport;
import org.intermine.install.swing.source.SourceListModel;
import org.intermine.install.swing.source.SourceListRenderer;
import org.intermine.install.swing.source.SourcePanel;
import org.intermine.modelviewer.ProjectLoader;
import org.intermine.modelviewer.model.Model;
import org.intermine.modelviewer.project.Project;
import org.intermine.modelviewer.project.Source;
import org.intermine.modelviewer.store.MineManagerBackingStore;
import org.intermine.modelviewer.swing.ModelViewer;


/**
 * Frame and main Swing class for the project editor.
 */
public class ProjectEditor extends JFrame
{
    /**
     * The colour for fields in error.
     */
    public static final Color ERROR_FIELD_COLOR = new Color(255, 224, 224);
    
    private static final long serialVersionUID = -2323752501684920361L;

    /**
     * Logger.
     */
    protected transient Log logger = LogFactory.getLog(getClass());
    
    /**
     * Shortcut for the value for the action command key mask.
     * @see Toolkit#getMenuShortcutKeyMask()
     */
    private final transient int menuActionMask =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    
    /**
     * New mine dialog.
     * @serial
     */
    private NewMineDialog newMineDialog;
    
    /**
     * Create database dialog.
     * @serial
     */
    private CreateDatabaseDialog createDatabaseDialog;
    
    /**
     * Create properties dialog.
     * @serial
     */
    private CreatePropertiesDialog createPropertiesDialog;
    
    /**
     * Make mine dialog.
     * @serial
     */
    private MakeMineDialog makeMineDialog;
    
    
    /**
     * Add source dialog.
     * @serial
     */
    private AddSourceDialog addSourceDialog;
    
    /**
     * New derived type dialog.
     * @serial
     */
    private NewDerivedTypeDialog newDerivedSourceDialog;
    
    /**
     * Post processor dialog.
     * @serial
     */
    private PostProcessorDialog postProcessorDialog;
    
    
    /**
     * Build project dialog.
     * @serial
     */
    private BuildProjectDialog buildProjectDialog;
    
    /**
     * Preferences dialog.
     * @serial
     */
    private PreferencesDialog preferencesDialog;
    
    
    /**
     * Frame for the model viewer.
     * @serial
     */
    private JFrame modelViewerFrame;
    
    /**
     * The model viewer component.
     * @serial
     */
    private ModelViewer modelViewer;
    
    
    /**
     * The source list model.
     * @serial
     */
    private SourceListModel sourceListModel = new SourceListModel();
    
    /**
     * The source list.
     * @serial
     */
    private JList sourceList = new JList(sourceListModel);
    
    /**
     * The source panel.
     * @serial
     */
    private SourcePanel sourcePanel = new SourcePanel();
    
    
    /**
     * Status message panel.
     * @serial
     */
    private JPanel statusPanel = new JPanel();
    
    /**
     * Button panel 
     * @serial
     */
    private JPanel buttonPanel = new JPanel();
    /**
     * Status message label.
     * @serial
     */
    private JLabel statusLabel = new JLabel();
    
    /**
     * Modified state label.
     * @serial
     */
    private JLabel modifiedLabel = new JLabel(Messages.getMessage("modified"));
    
    
    /**
     * Save project action.
     * @serial
     */
    private Action saveAction = new SaveAction();
    
    /**
     * Build project action.
     * @serial
     */
    private Action buildProjectAction = new BuildProjectAction();
    
    
    /**
     * Add source action.
     * @serial
     */
    private Action addSourceAction = new AddSourceAction();
    
    /**
     * Delete source action.
     * @serial
     */
    private Action deleteSourceAction = new DeleteSourceAction();
    
    /**
     * Action to display the post processor dialog.
     * @serial
     */
    private Action postProcessorAction = new EditPostProcessorAction();
    
    /**
      * Create load action
      */
    private Action loadAction = new OpenAction();

    /**
     * Support for firing <code>ProjectEvent</code>s.
     * @serial
     */
    private ProjectListenerSupport projectListenerSupport = new ProjectListenerSupport(this);
    
    
    /**
     * Timer for clearing the status message after a period of time.
     * @serial
     */
    private Timer statusMessageClearTimer;
    
    
    /**
     * A project loader.
     */
    private transient ProjectLoader projectLoader;
    
    /**
     * The main file for the project being edited.
     */
    private transient File projectFile;
    
    /**
     * The Project being edited.
     */
    private transient Project project;
    
    /**
     * Flag indicating the project has been modified since loading or the
     * last save.
     */
    private transient boolean projectModified;

    /**
      * Create Build Button
      */
    private JButton buildButton = new JButton(Messages.getMessage("buildproject"));
   
    /**
      * Create Load Button
      */
    private JButton loadButton = new JButton(Messages.getMessage("loadmine"));

    /**
      * Create exit Button
      */
    private JButton exitButton = new JButton(Messages.getMessage("exit"));

    /**
     * Initialise by default.
     * 
     * @see JFrame#JFrame()
     */
    public ProjectEditor() {
        init();
    }

    /**
     * Initialise with a graphics configuration.
     * @param gc The GraphicsConfiguration.
     * 
     * @see JFrame#JFrame(GraphicsConfiguration)
     */
    public ProjectEditor(GraphicsConfiguration gc) {
        super(gc);
        init();
    }

    /**
     * Common initialisation: lays out the child components and wires up the necessary
     * event listeners. 
     */
    private void init() {

        setName("Project Editor Frame");
        setTitle(Messages.getMessage("projecteditor.title"));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new MyWindowListener());
        
        modelViewerFrame = new JFrame();
        modelViewerFrame.setName("Model Viewer Frame");
        
        modelViewer = new ModelViewer();
        modelViewerFrame.setContentPane(modelViewer);
        modelViewerFrame.setTitle(Messages.getMessage("modelviewer.title"));
        modelViewerFrame.setSize(800, 600);
        
        newMineDialog = new NewMineDialog(this);
        createDatabaseDialog = new CreateDatabaseDialog(this);
        newMineDialog.setCreateDatabaseDialog(createDatabaseDialog);
        createPropertiesDialog = new CreatePropertiesDialog(this);
        createDatabaseDialog.setCreatePropertiesDialog(createPropertiesDialog);
        makeMineDialog = new MakeMineDialog(this);
        createPropertiesDialog.setMakeMineDialog(makeMineDialog);
        
        addSourceDialog = new AddSourceDialog(this);
        newDerivedSourceDialog = new NewDerivedTypeDialog(this);
        addSourceDialog.setNewDerivedDialog(newDerivedSourceDialog);
        
        postProcessorDialog = new PostProcessorDialog(this);
        
        buildProjectDialog = new BuildProjectDialog(this);
        
        preferencesDialog = new PreferencesDialog(this);
        
        ProjectListener projectListener = new MyProjectListener();
        addSourceDialog.addProjectListener(projectListener);
        newDerivedSourceDialog.addProjectListener(projectListener);
        postProcessorDialog.addProjectListener(projectListener);
        sourcePanel.addProjectListener(projectListener);
        makeMineDialog.addProjectListener(projectListener);
        addProjectListener(projectListener);
        
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        
        JMenu fileMenu = new JMenu(Messages.getMessage("file"));
        fileMenu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(fileMenu);
        
        fileMenu.add(new NewMineAction());
        fileMenu.add(new OpenAction());
        fileMenu.addSeparator();
        fileMenu.add(saveAction);
        fileMenu.addSeparator();
        fileMenu.add(buildProjectAction);
        
        JMenu editMenu = new JMenu(Messages.getMessage("edit"));
        editMenu.setMnemonic(KeyEvent.VK_E);
        menuBar.add(editMenu);
        
        editMenu.add(addSourceAction);
        editMenu.add(deleteSourceAction);
        editMenu.addSeparator();
        editMenu.add(postProcessorAction);
        
        JMenu viewMenu = new JMenu(Messages.getMessage("view"));
        viewMenu.setMnemonic(KeyEvent.VK_M);
        menuBar.add(viewMenu);
        
        viewMenu.add(new ViewModelAction());
        
        JMenu toolsMenu = new JMenu(Messages.getMessage("tools"));
        toolsMenu.setMnemonic(KeyEvent.VK_T);
        menuBar.add(toolsMenu);
        
        toolsMenu.add(new PreferencesAction());
        
        sourceListModel = new SourceListModel();
        sourceList = new JList(sourceListModel);
        
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        cp.add(splitPane, BorderLayout.CENTER);
        
        initButtonPanel();

        Box vbox = Box.createVerticalBox();
        vbox.add(sourcePanel);
        vbox.add(buttonPanel);

        splitPane.setLeftComponent(new JScrollPane(sourceList));
        splitPane.setRightComponent(vbox);
        
        splitPane.setDividerLocation(200);
        
        initStatusPanel();
        cp.add(statusPanel, BorderLayout.SOUTH);
        
        sourceList.setCellRenderer(new SourceListRenderer());
        sourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sourceList.addListSelectionListener(new SourceListSelectionListener());
        
        statusMessageClearTimer = new Timer(4000, new StatusMessageClearer());
        statusMessageClearTimer.setInitialDelay(4000);
        statusMessageClearTimer.setRepeats(false);
        
        setSize(800, 600);
    }
    /**
      * Initialise the button panel
      */
    private void initButtonPanel() {
        GridBagConstraints cons = GridBagHelper.setup(buttonPanel);
        cons.weightx = 1;
        buttonPanel.add(loadButton, cons);

        cons.gridx++;
        buttonPanel.add(buildButton, cons);

        cons.gridx++;
        buttonPanel.add(exitButton, cons);

        buildButton.setEnabled(false);

        loadButton.setMnemonic('l');
        buildButton.setMnemonic('b');
        exitButton.setMnemonic('x');

        loadButton.addActionListener(loadAction);
        buildButton.addActionListener(buildProjectAction);
        exitButton.addActionListener(new ExitAction());

        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
    }
    /**
     * Initialise the status panel and its children. 
     */
    private void initStatusPanel() {
        Font f = statusLabel.getFont().deriveFont(Font.PLAIN);
        statusLabel.setFont(f);
        modifiedLabel.setFont(f);
        
        GridBagConstraints cons = GridBagHelper.setup(statusPanel);
        
        cons.weightx = 1;
        statusPanel.add(statusLabel, cons);
        
        cons.gridx++;
        cons.weightx = 0;
        statusPanel.add(modifiedLabel, cons);
        
        int height = modifiedLabel.getPreferredSize().height;
        
        modifiedLabel.setMinimumSize(modifiedLabel.getPreferredSize());
        statusLabel.setMinimumSize(new Dimension(statusLabel.getMinimumSize().width, height));
        statusLabel.setPreferredSize(new Dimension(statusLabel.getPreferredSize().width, height));
        
        modifiedLabel.setVisible(false);
    }
    
    private void switchToLoadedButtonState() {
        loadButton.setText(Messages.getMessage("savemine"));
        loadButton.setMnemonic('s');
        loadButton.removeActionListener(loadAction);
        loadButton.addActionListener(saveAction);
        buildButton.setEnabled(true);
    }
    /**
     * Overridden so that on disposal, the related model viewer frame is also disposed.
     * 
     * @see Window#dispose()
     */
    @Override
    public void dispose() {
        modelViewerFrame.dispose();
        super.dispose();
    }
    
    /**
     * Get a look and feel by name, if it is supported on this platform.
     *
     * @param name The look and feel name.
     *
     * @return The LookAndFeelInfo object if the named L&F is supported
     * on this platform, or null if not.
     */
    public LookAndFeelInfo getLookAndFeelInfo(String name) {
        for (LookAndFeelInfo lfi : UIManager.getInstalledLookAndFeels()) {
            if (lfi.getName().equals(name)) {
                return lfi;
            }
        }
        return null;
    }

    /**
     * Change the look and feel of the application.
     *
     * @param laf The new Look and Feel information.
     *
     * @return <code>true</code> if the look and feel changed ok, <code>false</code>
     * if not.
     *
     * @see <a href="http://java.sun.com/docs/books/tutorial/uiswing/lookandfeel/plaf.html#dynamic">
     * The Swing Tutorial</a>
     */
    public boolean changeLookAndFeel(LookAndFeelInfo laf) {
        assert SwingUtilities.isEventDispatchThread() : "Can only change L&F from event thread";

        if (laf.getName().equals(UIManager.getLookAndFeel().getName())) {
            return true;
        }
        
        boolean changeOk = false;
        try {
            UIManager.setLookAndFeel(laf.getClassName());
            SwingUtilities.updateComponentTreeUI(this);
            SwingUtilities.updateComponentTreeUI(modelViewerFrame);
            for (Window w : getOwnedWindows()) {
                SwingUtilities.updateComponentTreeUI(w);
            }

            changeOk = true;
        } catch (Exception e) {
            logger.error("Failed to change look and feel: " + e.getMessage());
            logger.debug("", e);

            int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        Messages.getMessage("preferences.lookandfeel.changefailed.message",
                                            e.getMessage()),
                        Messages.getMessage("preferences.lookandfeel.changefailed.title"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

            changeOk = choice == JOptionPane.OK_OPTION;
        }

        return changeOk;
    }

    /**
     * Load the project from the given <code>project.xml</code> file.
     * 
     * @param projectFile The file to load.
     * 
     * @see ProjectLoader#loadProject(File)
     */
    public void loadProject(File projectFile) {
        try {
            if (projectLoader == null) {
                projectLoader = new ProjectLoader();
            }
            
            project = projectLoader.loadProject(projectFile);
            
            initialise(projectFile.getCanonicalFile(), project);
            
            MineManagerBackingStore.getInstance().setLastProjectFile(this.projectFile);
            
            loadModel();
            switchToLoadedButtonState();

            if (project.getSources().getSource().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                                              Messages.getMessage("project.empty.message"),
                                              Messages.getMessage("project.empty.title"),
                                              JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (Exception e) {
            logger.error(Messages.getMessage("project.load.failed.message"), e);
            
            StringWriter swriter = new StringWriter();
            PrintWriter writer = new PrintWriter(swriter);
            writer.println(Messages.getMessage("project.load.failed.message"));
            e.printStackTrace(writer);
            writer.close();
            
            JOptionPane.showMessageDialog(this, swriter.toString(),
                                          Messages.getMessage("project.load.failed.title"),
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Initialise to display the given project.
     * 
     * @param projectFile The project file.
     * @param project The project object.
     */
    public void initialise(File projectFile, Project project) {
        this.projectFile = projectFile;
        this.project = project;
        setModified(false);
        
        saveAction.setEnabled(project != null);
        buildProjectAction.setEnabled(projectFile != null);
        
        closeAllDialogs();
        
        sourceListModel.setSources(project.getSources().getSource());
        addSourceAction.setEnabled(true);
        postProcessorAction.setEnabled(true);
        if (sourceListModel.getSize() > 0) {
            sourceList.setSelectedIndex(0);
        }
        
        addSourceDialog.initialise(project);
        newDerivedSourceDialog.setIntermineHome(projectFile.getParentFile().getParentFile());
        postProcessorDialog.initialise(project);
        
        statusLabel.setText(Messages.getMessage("project.loaded"));
        switchToLoadedButtonState();
        statusMessageClearTimer.restart();
    }
    
    /**
     * Load the model defined by the loaded project in the background.
     * 
     * @see ModelLoader
     */
    public void loadModel() {
        new ModelLoader().execute();
    }
    
    /**
     * Close the dialogs involved in the create mine process.
     */
    protected void closeNewMineDialogs() {
        newMineDialog.setVisible(false);
        createDatabaseDialog.setVisible(false);
        createPropertiesDialog.setVisible(false);
        makeMineDialog.setVisible(false);
    }
    
    /**
     * Close all child dialogs.
     */
    protected void closeAllDialogs() {
        closeNewMineDialogs();
        
        addSourceDialog.setVisible(false);
        newDerivedSourceDialog.setVisible(false);
        postProcessorDialog.setVisible(false);
        
        buildProjectDialog.setVisible(false);
        
        preferencesDialog.setVisible(false);
    }
    
    /**
     * Add a ProjectListener to this frame.
     * @param l The ProjectListener.
     */
    public void addProjectListener(ProjectListener l) {
        projectListenerSupport.addProjectListener(l);
    }
    
    /**
     * Remove a ProjectListener from this frame.
     * @param l The ProjectListener.
     */
    public void removeProjectListener(ProjectListener l) {
        projectListenerSupport.removeProjectListener(l);
    }
    
    /**
     * Set the project modified flag. Shows or hides the modified
     * message accordingly.
     * 
     * @param modified The new state for the modified flag.
     */
    protected void setModified(boolean modified) {
        if (projectModified != modified) {
            modifiedLabel.setVisible(modified);
            projectModified = modified;
            statusPanel.repaint();
        }
    }
    
    /**
     * Prompt the user to ask whether to save the modified project before
     * proceeding.
     * 
     * @return <code>false</code> if the user chose to save the project,
     * <code>true</code> if the project has not been saved. 
     */
    protected boolean promptedSave() {
        boolean cancel = false;
        if (projectModified) {
            int choice =
                JOptionPane.showConfirmDialog(this,
                        Messages.getMessage("project.modified.save.message"),
                        Messages.getMessage("project.modified.save.title"),
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
            
            switch (choice) {
                case JOptionPane.CANCEL_OPTION:
                    cancel = true;
                    break;
                    
                case JOptionPane.YES_OPTION:
                    saveProject();
                    break;
            }
        }
        return cancel;
    }
    
    /**
     * Save the currently edited project.
     */
    public void saveProject() {

        // Make sure current fields are in the object.
        sourcePanel.saveCurrentSource();
        
        File backupFile =
            new File(projectFile.getParentFile(), projectFile.getName() + "~");
        
        backupFile.delete();
        
        boolean success = false;
        
        boolean rollback = false;
        if (projectFile.exists()) {
            projectFile.renameTo(backupFile);
            rollback = true;
        }
        
        try {
            if (projectLoader == null) {
                projectLoader = new ProjectLoader();
            }
            
            projectLoader.saveProject(project, projectFile);
            
            rollback = false;
            success = true;
            
            setModified(false);
            
            logger.info("Saved project " + projectFile);

        } catch (Exception e) {
            WindowUtils.showExceptionDialog(
                    this, e,
                    "project.save.failure.title", "project.save.failure.message",
                    logger);
        }
        
        if (rollback) {
            projectFile.delete();
            boolean ok = backupFile.renameTo(projectFile);
            if (!ok) {
                logger.warn("Failed to restore project file " + projectFile.getAbsolutePath());
            }
        }
        
        if (success) {
            statusLabel.setText(Messages.getMessage("project.saved"));
            statusMessageClearTimer.restart();
        } else {
            statusMessageClearTimer.stop();
            statusLabel.setText(Messages.getMessage("project.save.failed"));
        }
    }
    
    /**
     * Action to open the load project file chooser and start loading the project.
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
            putValue(ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_L, menuActionMask));
        }

        /**
         * Called when the action fires, pops up the load project file chooser.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            boolean cancel = promptedSave();
            if (!cancel) {
                modelViewer.getProjectFileChooser().setLocation(
                        MouseInfo.getPointerInfo().getLocation());

                File projectFile = modelViewer.chooseProjectFile();
                if (projectFile != null) {
                    loadProject(projectFile);
                }
            }
        }
    }
    
    /**
     * Action to start the new mine chain.
     */
    private class NewMineAction extends AbstractAction
    {
        private static final long serialVersionUID = -1555700465796516151L;

        /**
         * Constructor.
         */
        public NewMineAction() {
            super(Messages.getMessage("newmine"));
            putValue(MNEMONIC_KEY, KeyEvent.VK_N);
        }

        /**
         * Called when the action fires, this opens the new mine dialog.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            boolean cancel = promptedSave();
            if (!cancel) {
                closeNewMineDialogs();
                
                newMineDialog.setLocation(MouseInfo.getPointerInfo().getLocation());
                newMineDialog.setVisible(true);
            }
        }
    }
    
    /**
     * Action to open the add source dialog.
     */
    private class AddSourceAction extends AbstractAction
    {
        private static final long serialVersionUID = 4369965050592631280L;

        /**
         * Constructor.
         */
        public AddSourceAction() {
            super(Messages.getMessage("addsource"));
            putValue(MNEMONIC_KEY, KeyEvent.VK_A);
            putValue(ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_A,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            setEnabled(false);
        }

        /**
         * Called when the action fires, this method opens the add source dialog.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            assert project != null : "Project not loaded";
            
            if (!addSourceDialog.isVisible()) {
                addSourceDialog.clearNameField();
                addSourceDialog.setLocation(MouseInfo.getPointerInfo().getLocation());
                addSourceDialog.setVisible(true);
            } else {
                addSourceDialog.toFront();
            }
        }
    }

    /**
     * Action to delete a source from the project.
     */
    private class DeleteSourceAction extends AbstractAction
    {
        private static final long serialVersionUID = 6557202208652306709L;

        /**
         * Constructor.
         */
        public DeleteSourceAction() {
            super(Messages.getMessage("deletesource"));
            putValue(MNEMONIC_KEY, KeyEvent.VK_D);
            putValue(ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
            setEnabled(false);
        }

        /**
         * Called when the action fires, this method prompts for confirmation of the
         * delete and if confirmed, the source is removed and a <code>ProjectEvent</code>
         * is fired via {@link ProjectListener#sourceDeleted}.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            Source s = (Source) sourceList.getSelectedValue();
            
            assert s != null : "No source selected";
            
            int choice =
                JOptionPane.showConfirmDialog(
                        ProjectEditor.this,
                        Messages.getMessage("source.delete.confirm.message", s.getName()),
                        Messages.getMessage("source.delete.confirm.title"),
                        JOptionPane.YES_NO_OPTION);
            
            if (choice == JOptionPane.YES_OPTION) {
                project.getSources().getSource().remove(s);
                
                projectListenerSupport.fireSourceDeleted(project, s);
            }
        }
    }

    /**
     * Action to open the post processor dialog.
     */
    private class EditPostProcessorAction extends AbstractAction
    {
        private static final long serialVersionUID = 161910996784310151L;

        /**
         * Constructor.
         */
        public EditPostProcessorAction() {
            super(Messages.getMessage("postprocessors"));
            putValue(MNEMONIC_KEY, KeyEvent.VK_P);
            putValue(ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_P, menuActionMask));
            setEnabled(false);
        }
        
        /**
         * Called when the action fires, this method opens the post processor
         * dialog.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            if (!postProcessorDialog.isVisible()) {
                postProcessorDialog.setLocation(MouseInfo.getPointerInfo().getLocation());
                postProcessorDialog.setVisible(true);
            } else {
                postProcessorDialog.toFront();
            }
        }
    }
    
    /**
     * Action to display the model viewer frame.
     */
    private class ViewModelAction extends AbstractAction
    {
        private static final long serialVersionUID = -5443685330952992817L;

        /**
         * Constructor.
         */
        public ViewModelAction() {
            super(Messages.getMessage("viewmodel"));
            putValue(MNEMONIC_KEY, KeyEvent.VK_M);
            putValue(ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_M, menuActionMask));
        }

        /**
         * Called when the action fires, this method makes the model viewer frame
         * visible or brings it to the front of the stack.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            if (modelViewerFrame.isVisible()) {
                modelViewerFrame.toFront();
            } else {
                modelViewerFrame.setLocation(MouseInfo.getPointerInfo().getLocation());
                modelViewerFrame.setVisible(true);
            }
        }
    }
    
    /**
     * Source list listener to change the source displayed in the source panel
     * as the user selects other sources.
     */
    private class SourceListSelectionListener implements ListSelectionListener
    {
        /**
         * Called as the list selection changes, this method changes the source
         * displayed in the source panel.
         * 
         * @param event The list selection event.
         */
        @Override
        public void valueChanged(ListSelectionEvent event) {
            if (!event.getValueIsAdjusting()) {
                Source s = (Source) sourceList.getSelectedValue();
                sourcePanel.setSource(projectFile.getParentFile(), project, s);
                deleteSourceAction.setEnabled(s != null);
            }
        }
    }
    
    /**
     * Project listener implementation. Makes various changes to the display
     * as the project changes.
     */
    private class MyProjectListener extends ProjectAdapter
    {
        private static final long serialVersionUID = -3811920416621327724L;

        /**
         * When the project is loaded, the project editor is updated to display
         * the newly loaded project.
         * 
         * @param event The project event.
         * 
         * @see ProjectEditor#initialise(File, Project)
         * @see ProjectEditor#loadModel()
         */
        @Override
        public void projectLoaded(ProjectEvent event) {
            logger.debug("Project loaded: " + event.getProjectFile());
            try {
                closeAllDialogs();
                
                project = event.getProject();
                projectFile = event.getProjectFile().getCanonicalFile();
                
                initialise(projectFile, project);
                
                MineManagerBackingStore.getInstance().setLastProjectFile(projectFile);

                loadModel();

                if (project.getSources().getSource().isEmpty()) {
                    JOptionPane.showMessageDialog(ProjectEditor.this,
                                                  Messages.getMessage("project.empty.message"),
                                                  Messages.getMessage("project.empty.title"),
                                                  JOptionPane.INFORMATION_MESSAGE);
                }
                
            } catch (Exception e) {
                logger.error(Messages.getMessage("model.load.failed.message"), e);
                
                StringWriter swriter = new StringWriter();
                PrintWriter writer = new PrintWriter(swriter);
                writer.println(Messages.getMessage("model.load.failed.message"));
                e.printStackTrace(writer);
                writer.close();
                
                JOptionPane.showMessageDialog(ProjectEditor.this,
                                              swriter.toString(),
                                              Messages.getMessage("model.load.failed.title"),
                                              JOptionPane.ERROR_MESSAGE);
            }
        }

        /**
         * Called when a source is added to the project. The source is selected in the
         * selected list, the project marked as modified and the genomic model reloaded.
         * 
         * @param event The project event.
         * 
         * @see ProjectEditor#setModified(boolean)
         * @see ProjectEditor#loadModel()
         */
        @Override
        public void sourceAdded(ProjectEvent event) {
            Project p = event.getProject();
            Source s = event.getProjectSource();
            
            Iterator<Source> iter = p.getSources().getSource().iterator();
            int index = -1;
            for (int i = 0; iter.hasNext(); i++) {
                Source compare = iter.next();
                if (compare == s) {
                    index = i;
                    break;
                }
            }
            assert index >= 0 : "Source not found in its project";
            
            sourceListModel.addSource(s, index);
            sourceList.setSelectedValue(s, true);
            
            setModified(true);
            
            statusLabel.setText(Messages.getMessage("source.added", s.getName()));

            loadModel();
        }

        /**
         * Called when a source is modified. The modified flag for the project is
         * set to <code>true</code>.
         * 
         * @param event The project event.
         * 
         * @see ProjectEditor#setModified(boolean)
         * @see ProjectEditor#loadModel()
         */
        @Override
        public void sourceModified(ProjectEvent event) {
            setModified(true);
            sourceList.repaint();
        }

        /**
         * Called when a source is deleted. Removes the source from the source list,
         * sets the project modified flag and reloads the genomic model.
         * 
         * @param event The project event.
         * 
         * @see ProjectEditor#setModified(boolean)
         */
        @Override
        public void sourceDeleted(ProjectEvent event) {
            Source s = event.getProjectSource();
            int index = sourceListModel.indexOf(s);
            sourceListModel.removeSource(event.getProjectSource());
            if (index >= 0 && !project.getSources().getSource().isEmpty()) {
                sourceList.setSelectedIndex(Math.min(index, sourceListModel.getSize() - 1));
            }
            
            setModified(true);
            
            statusLabel.setText(Messages.getMessage("source.deleted", s.getName()));
            
            loadModel();
        }
    }
    
    /**
     * Action to save the project.
     */
    private class SaveAction extends AbstractAction
    {
        private static final long serialVersionUID = -5121864068655762163L;

        /**
         * Constructor.
         */
        public SaveAction() {
            super(Messages.getMessage("savemine"));
            putValue(MNEMONIC_KEY, KeyEvent.VK_S);
            putValue(ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_S, menuActionMask));
            setEnabled(false);
        }

        /**
         * Called when the action fires, this method saves the project.
         * 
         * @param event The action event.
         * 
         * @see ProjectEditor#saveProject()
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            saveProject();
        }
    }
    
    /**
     * Action to exit the project.
     */
    private class ExitAction extends AbstractAction
    {
        private static final long serialVersionUID = -5121864068655762163L;

        /**
         * Constructor.
         */
        public ExitAction() {
            super(Messages.getMessage("cancel"));
            putValue(MNEMONIC_KEY, KeyEvent.VK_Q);
            setEnabled(false);
        }

        /**
         * Called when the action fires, this method exits project.
         * prompting first for saving
         * 
         * @param event The action event.
         * 
         * @see ProjectEditor#exitProject()
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            boolean close = true;
            
            if (projectModified) {
                boolean cancel = promptedSave();
                close = !cancel;
            }
            
            if (close) {
                setVisible(false);
                dispose();
            }
        }
    }

    /**
     * Action to display the build project dialog.
     */
    private class BuildProjectAction extends AbstractAction
    {
        private static final long serialVersionUID = -6752372757098098746L;

        /**
         * Constructor.
         */
        public BuildProjectAction() {
            super(Messages.getMessage("buildproject"));
            putValue(MNEMONIC_KEY, KeyEvent.VK_B);
            setEnabled(false);
        }

        /**
         * Called when the action fires, this method opens the build project dialog.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            
            boolean cancel = promptedSave();
            if (!cancel) {
                buildProjectDialog.initialise(projectFile);
                if (!buildProjectDialog.isVisible()) {
                    buildProjectDialog.setLocation(MouseInfo.getPointerInfo().getLocation());
                    buildProjectDialog.setVisible(true);
                } else {
                    buildProjectDialog.toFront();
                }
            }
        }
    }
    
    /**
     * Action to open the preferences dialog.
     */
    private class PreferencesAction extends AbstractAction
    {
        private static final long serialVersionUID = -7169681733117671717L;

        /**
         * Constructor.
         */
        public PreferencesAction() {
            super(Messages.getMessage("preferences"));
            putValue(MNEMONIC_KEY, KeyEvent.VK_P);
        }

        /**
         * Called when the action fires, this method opens the preferences dialog.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            if (!preferencesDialog.isVisible()) {
                preferencesDialog.setLocation(MouseInfo.getPointerInfo().getLocation());
                preferencesDialog.setVisible(true);
            } else {
                preferencesDialog.toFront();
            }
        }
    }
    
    /**
     * Action listener to receive notifications from the timer to clear the status message.
     */
    private class StatusMessageClearer implements ActionListener
    {
        /**
         * Called when the timer fires, this method clears the text of the status label.
         * 
         * @param event The action event.
         */
        public void actionPerformed(ActionEvent event) {
            statusLabel.setText("");
        }
    }
    
    
    /**
     * Swing worker implementation to load the genomic model in a background thread.
     */
    public class ModelLoader extends SwingWorker<Model, Void>
    {
        /**
         * An exception if the loading fails.
         */
        private Exception failure;
        
        /**
         * The loaded model object.
         */
        private Model newModel;
        
        
        /**
         * Perform the model load in a worker thread.
         * 
         * @return The Model object loaded.
         */
        @Override
        protected Model doInBackground() {
            try {
                if (projectLoader == null) {
                    projectLoader = new ProjectLoader();
                }
                
                newModel = projectLoader.loadModel(projectFile, project);

            } catch (Exception e) {
                failure = e;
            }
            
            return newModel;
        }

        /**
         * Called in the Swing event thread when the model load has finished.
         * Sets the model in the model viewer, or displays an error dialog if
         * the load failed.
         */
        @Override
        protected void done() {
            if (failure == null) {
                File projectHome = projectFile.getParentFile();
                modelViewer.initialise(newModel, projectHome);

            } else {
                logger.error(Messages.getMessage("model.load.failed.message"), failure);
                
                StringWriter swriter = new StringWriter();
                PrintWriter writer = new PrintWriter(swriter);
                writer.println(Messages.getMessage("model.load.failed.message"));
                failure.printStackTrace(writer);
                writer.close();
                
                JOptionPane.showMessageDialog(ProjectEditor.this, swriter.toString(),
                                              Messages.getMessage("model.load.failed.title"),
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Window listener to prompt to save the project if the user tries to close the
     * window with a modified project.
     */
    private class MyWindowListener extends WindowAdapter
    {
        /**
         * Called as the window close request is made. Prompts to save the project if
         * it is modified and only closes the window if the project is saved or the
         * user explicitly declines the save.
         * 
         * @param event The window event.
         * 
         * @see ProjectEditor#promptedSave()
         */
        @Override
        public void windowClosing(WindowEvent event) {
            boolean close = true;
            
            if (projectModified) {
                boolean cancel = promptedSave();
                close = !cancel;
            }
            
            if (close) {
                setVisible(false);
                dispose();
            }
        }
    }
}
