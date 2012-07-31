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

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.apache.commons.lang.StringUtils;
import org.intermine.common.swing.ButtonPanel;
import org.intermine.common.swing.GridBagHelper;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.StandardJDialog;
import org.intermine.modelviewer.store.MineManagerBackingStore;


/**
 * Dialog for changing preferences for the Intermine installer application.
 * At the time of writing, this is limited to setting the look and feel.
 */
public class PreferencesDialog extends StandardJDialog
{
    private static final long serialVersionUID = 3513043663040148969L;

    /**
     * Combo box model for the look and feel choices.
     * @serial
     */
    private DefaultComboBoxModel lfComboModel = new DefaultComboBoxModel();
    
    /**
     * Combo box for the look and feel choices.
     * @serial
     */
    private JComboBox lfCombo = new JComboBox(lfComboModel);
    
    /**
     * Label to display when the look and feel is not available on the current
     * machine.
     * @serial
     */
    private JLabel wrongPlaformLabel =
        new JLabel(Messages.getMessage("preferences.lookandfeel.wrongplatform",
                   System.getProperty("os.name", "")));

    /**
     * Map of look and feel names to LookAndFeelInfo objects.
     * @serial
     */
    private Map<String, LookAndFeelInfo> installedLookAndFeels
        = new HashMap<String, LookAndFeelInfo>();

    /**
     * The preference properties, loaded from <code>preferences.properties</code>.
     * @serial
     */
    private Properties properties;
    
    /**
     * The ProjectEditor frame that owns this dialog.
     * @serial
     */
    private ProjectEditor projectEditor;

    
    /**
     * Initialise with the parent ProjectEditor.
     * @param editor The ProjectEditor.
     */
    public PreferencesDialog(ProjectEditor editor) {
        super(editor);
        projectEditor = editor;
        init();
        initLookAndFeels();
    }

    /**
     * Common initialisation: lays out the child components and wires up the necessary
     * event listeners. 
     */
    private void init() {
        setName("Preferences Dialog");
        setTitle(Messages.getMessage("preferences.title"));

        lfCombo.setRenderer(new LookAndFeelComboCellRenderer());

        Container cp = getContentPane();

        GridBagConstraints cons = GridBagHelper.setup(cp);
        cons.weightx = 0.0;
        cons.gridwidth = 1;
        cp.add(new JLabel(Messages.getMessage("preferences.lookandfeel")), cons);

        cons.gridx++;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.weightx = 1.0;
        cp.add(lfCombo, cons);

        cons.gridx = 0;
        cons.gridy++;
        cp.add(wrongPlaformLabel, cons);

        cons.gridy++;
        cp.add(new ButtonPanel(new OkAction(), new CancelAction()), cons);

        pack();
        
        wrongPlaformLabel.setVisible(false);
    }

    /**
     * Initialise the look and feel choices by reading all the possible look and
     * feel names from <code>preferences.properties</code>, populating the combo
     * box accordingly and highlighting the last selected look and feel.
     */
    private void initLookAndFeels() {
        InputStream propsStream =
            getClass().getResourceAsStream("/org/intermine/install/preferences.properties");
        if (propsStream == null) {
            throw new RuntimeException(
                    "The file 'preferences.properties' cannot be found on the class path.");
        }
        properties = new Properties();
        try {
            try {
                properties.load(propsStream);
            } finally {
                propsStream.close();
            }
        } catch (IOException e) {
            logger.warn("Could not load from preferences.properties. The options may be limited.");
        }

        String lookAndFeelNameList = properties.getProperty("lookandfeel.namelist", "Metal");
        String[] lookAndFeelNames = lookAndFeelNameList.split(",");

        Arrays.sort(lookAndFeelNames, Collator.getInstance());

        for (LookAndFeelInfo lf : UIManager.getInstalledLookAndFeels()) {
            installedLookAndFeels.put(lf.getName(), lf);
        }

        for (String lf : lookAndFeelNames) {
            lfComboModel.addElement(lf);
        }

        lfCombo.addActionListener(new LookAndFeelComboListener());
        
        String lookAndFeel = MineManagerBackingStore.getInstance().getLookAndFeel();

        if (StringUtils.isNotEmpty(lookAndFeel)) {
            lfCombo.setSelectedItem(lookAndFeel);
        }
    }

    /**
     * Action to change the look and feel.
     */
    private class OkAction extends AbstractAction
    {
        private static final long serialVersionUID = -2327514794404835822L;

        /**
         * Constructor.
         */
        public OkAction() {
            super(Messages.getMessage("ok"));
        }

        /**
         * Called to select a look and feel. The application's UI components are
         * changed to the selected look and feel and the chosen option saved to
         * be reused next time.
         * 
         * @param event The action event.
         */
        public void actionPerformed(ActionEvent event) {
            String selected = (String) lfCombo.getSelectedItem();
            if (selected != null) {
                LookAndFeelInfo lf = installedLookAndFeels.get(selected);
                if (lf != null) {
                    boolean changeOk = projectEditor.changeLookAndFeel(lf);
                    if (changeOk) {
                        MineManagerBackingStore.getInstance().setLookAndFeel(selected);
                    }
                } else {
                    MineManagerBackingStore.getInstance().setLookAndFeel(selected);
                }
            }
        }
    }

    /**
     * Listener to the look and feel combo box to show or hide the wrong platform
     * message according to the availability of the current machine.
     */
    protected class LookAndFeelComboListener implements ActionListener
    {
        /**
         * Called as the look and feel selection changed. Shows or hides the
         * warning message.
         * 
         * @param event The action event.
         */
        public void actionPerformed(ActionEvent event) {
            String selected = (String) lfCombo.getSelectedItem();
            if (selected != null) {
                if (installedLookAndFeels.containsKey(selected)) {
                    wrongPlaformLabel.setVisible(false);
                } else {
                    wrongPlaformLabel.setVisible(true);
                }
            } else {
                wrongPlaformLabel.setVisible(false);
            }
        }
    }

    /**
     * Renderer for the look and feel combo box. This specialisation draws look and
     * feel options that are unavailable in an italic font.
     */
    protected class LookAndFeelComboCellRenderer extends DefaultListCellRenderer
    {
        private static final long serialVersionUID = 5343576003481127345L;

        /**
         * The regular font.
         */
        private transient Font mainFont;
        
        /**
         * The italic font.
         */
        private transient Font italicFont;

        /**
         * Constructor.
         */
        public LookAndFeelComboCellRenderer() {
        }

        /**
         * Prepare this renderer for plotting the given value.
         * 
         * @param list The list.
         * @param value The current value.
         * @param index The cell index.
         * @param isSelected Whether the cell is selected or not.
         * @param cellHasFocus Whether the cell has keyboard focus.
         * 
         * @return <code>this</code>, suitably prepared.
         * 
         * @see DefaultListCellRenderer#getListCellRendererComponent
         */
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            String name = (String) value;

            if (!installedLookAndFeels.containsKey(name)) {
                if (getFont() != mainFont) {
                    mainFont = getFont();
                    italicFont = mainFont.deriveFont(Font.ITALIC);
                }

                setFont(italicFont);
            }

            return this;
        }
    }
}
