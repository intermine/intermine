package org.intermine.modelviewer.swing;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.modelviewer.swing.CustomisedMxGraph.COLLECTION_EDGE_STYLE;
import static org.intermine.modelviewer.swing.CustomisedMxGraph.INHERITANCE_EDGE_STYLE;
import static org.intermine.modelviewer.swing.CustomisedMxGraph.REFERENCE_EDGE_STYLE;

import java.awt.BorderLayout;
import java.io.File;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.GridBagHelper;
import org.intermine.common.swing.SystemProcessProgressDialog;
import org.intermine.common.swing.SystemProcessSwingWorker;
import org.intermine.modelviewer.ProjectLoader;
import org.intermine.modelviewer.model.ForeignKey;
import org.intermine.modelviewer.model.Model;
import org.intermine.modelviewer.model.ModelClass;
import org.intermine.modelviewer.store.MineManagerBackingStore;
import org.intermine.modelviewer.swing.attributetable.AttributeTable;
import org.intermine.modelviewer.swing.attributetable.AttributeTableModel;
import org.intermine.modelviewer.swing.classtree.ClassTreeCellRenderer;
import org.intermine.modelviewer.swing.classtree.ClassTreeModel;
import org.intermine.modelviewer.swing.classtree.ClassTreeNode;
import org.intermine.modelviewer.swing.referencetable.ReferenceTable;
import org.intermine.modelviewer.swing.referencetable.ReferenceTableModel;

import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;


/**
 * The main Swing component containing the panels that make up the model viewer.
 * <p>This component is made up of three functional areas:</p>
 * 
 * <ol>
 * <li><b>The class selector</b> - This is a list of model classes to the left
 * of the component. Users use this to select which class is displayed.</li>
 * <li><b>The attribute and collection display</b> - This is a tabbed pane listing
 * attributes (on one tab) and references + collections (on a second tab) of the
 * selected class.</li>
 * <li><b>The class graph</b> - This is the centre of the component and shows the
 * inter-class relationships of the selected class.</li>
 * </ol>
 * 
 * <p>This implementation performs its own layout of the classes around the selected
 * model class for the graph. It would be recommended to investigate JGraphX's
 * supplied layout mechanisms to get a better display before too much effort is spent
 * on improving the algorithm here.</p>
 */
public class ModelViewer extends JPanel
{
    private static final long serialVersionUID = 1764086264698226094L;

    /**
     * Logger.
     */
    private static Log logger = LogFactory.getLog(ModelViewer.class);

    /**
     * A standard Swing file chooser for selecting project files.
     * @serial
     */
    protected JFileChooser projectFileChooser;

    /**
     * The tree model for the class selector.
     * @serial
     */
    private ClassTreeModel classTreeModel;
    
    /**
     * The class selector JTree.
     * @serial
     */
    private JTree classTree;
    
    /**
      * Progress dialog to display the output from <code>ant build-db</code>
      */
    private SystemProcessProgressDialog processDialog;
    /**
     * The table model for the attribute table.
     * @serial
     */
    private AttributeTableModel attributeTableModel;
    
    /**
     * The attribute table.
     * @serial
     */
    private JTable attributeTable;
    
    /**
     * The table model for the inter-class reference table.
     * @serial
     */
    private ReferenceTableModel referenceTableModel;
    
    /**
     * The inter-class reference table.
     * @serial
     */
    private JTable referenceTable;
    
    /**
     * The class relationship graph model.
     * @serial
     */
    private mxGraphModel graphModel;
    
    /**
     * The class relationship graph.
     * @serial
     */
    private mxGraph graph;
    
    /**
     * The class relationship graph Swing component.
     * @serial
     */
    private mxGraphComponent graphComponent;
    
    
    /**
     * An internal map of class name to ModelClass object.
     * @serial
     */
    private Map<String, ModelClass> modelClasses = new HashMap<String, ModelClass>();
    
    /**
     * An internal map of class name to mxICell graph node object.
     * @serial
     */
    private Map<String, mxICell> graphNodes = new HashMap<String, mxICell>();

    /**
      * The parent fram of the ModelViewer pane
      */
    private JFrame parentFrame;

    /**
      * Button panel
      */
    private JPanel buttonPanel = new JPanel();

    /**
      * Create the build model button
      */
    private JButton buildButton = new JButton(Messages.getMessage("build.model"));
    /**
     * Initialises this component.
     */
    public ModelViewer() {
        init();
    }
    
    public ModelViewer(JFrame frame) {
        parentFrame = frame;
        init();
    }

    private File projectHome;
    
    /**
    * Progress dialog to display the output from <code>ant build-db</code>. 
    * @serial  
    */ 
    private SystemProcessProgressDialog progressDialog; 
  
    /** 
     * Build model action. 
     * @serial 
     */ 
    private Action buildModelAction = new BuildModelAction(); 

    /**
     * Lays out the components within this panel and wires up the relevant
     * event listeners.
     */
    private void init() {
        
        FileFilter xmlFilter = new XmlFileFilter();
        projectFileChooser = new JFileChooser();
        projectFileChooser.addChoosableFileFilter(xmlFilter);
        projectFileChooser.setAcceptAllFileFilterUsed(false);
        projectFileChooser.setFileFilter(xmlFilter);
        
        File lastProjectFile = MineManagerBackingStore.getInstance().getLastProjectFile();
        if (lastProjectFile != null) {
            projectFileChooser.setSelectedFile(lastProjectFile);
        }

        initButtonPanel();
        
        classTreeModel = new ClassTreeModel();
        classTree = new JTree(classTreeModel);
        classTree.setCellRenderer(new ClassTreeCellRenderer());
        classTree.setRootVisible(false);
        classTree.setShowsRootHandles(true);
        
        Box vbox = Box.createVerticalBox();
        vbox.add(new JScrollPane(classTree));
        vbox.add(buttonPanel);

        DefaultTreeSelectionModel selectionModel = new DefaultTreeSelectionModel();
        selectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        classTree.setSelectionModel(selectionModel);
        
        classTree.addTreeSelectionListener(new ClassTreeSelectionListener());
        
        attributeTableModel = new AttributeTableModel();
        attributeTable = new AttributeTable(attributeTableModel);

        referenceTableModel = new ReferenceTableModel();
        referenceTable = new ReferenceTable(referenceTableModel);

        graphModel = new mxGraphModel();
        graph = new CustomisedMxGraph(graphModel);
        
        graphComponent = new mxGraphComponent(graph);
        graphComponent.setEscapeEnabled(true);

        JTabbedPane tableTab = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tableTab.add(Messages.getMessage("tab.attributes"), new JScrollPane(attributeTable));
        tableTab.add(Messages.getMessage("tab.references"), new JScrollPane(referenceTable));
        
        JSplitPane rightSplit =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableTab, graphComponent);
        
        JSplitPane mainSplit =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, vbox, rightSplit);
        
        setOpaque(true);
        setLayout(new BorderLayout());
        add(mainSplit, BorderLayout.CENTER);
        
        rightSplit.setDividerLocation(150);
        mainSplit.setDividerLocation(200);

    }
    
    /**
      * Initialise the button panel
      */
    private void initButtonPanel() {
        GridBagConstraints cons = GridBagHelper.setup(buttonPanel); 
        cons.weightx = 1; 
        buttonPanel.add(buildButton, cons); 
        buildButton.setMnemonic('b'); 
        buildButton.addActionListener(buildModelAction); 
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50)); 
    } 

    /**
     * Get the project file chooser component.
     * @return The project file JFileChooser.
     */
    public JFileChooser getProjectFileChooser() {
        return projectFileChooser;
    }

   /** 
    * Initialise the Model Viewer with the project directory 
    * 
    * @param model The model to display 
    * @param projectDir The project directory 
    */ 
    public void initialise(Model model, File projectDir) { 
       projectHome = projectDir; 
       initialise(model); 
    } 
    /**
     * Reinitialise this component to display the given model.
     * 
     * @param model The model to display
     */
    public void initialise(Model model) {

        modelClasses.clear();
        graphNodes.clear();
        
        graphModel.clear();
        classTreeModel = new ClassTreeModel(model.getClasses().values());
        classTree.setModel(classTreeModel);
        
        for (ModelClass mc : model.getClasses().values()) {
            modelClasses.put(mc.getName(), mc);
        }
    }
    
    /**
     * Display the given class in the main window. This does not
     * change the selection in the class tree.
     * 
     * @param modelClass The class to display.
     */
    public void displayClass(ModelClass modelClass) {
    
        graphNodes.clear();
        
        final int width = 80;
        final int height = 36;
        final int xcentre = 200;
        final int yboundary = 20;
        final int ycentre = 200;
        
        graphModel.beginUpdate();
        try {
            graphModel.clear();
            
            //int maxDepth = modelClass.getDepth();
            
            Object parent = graph.getDefaultParent();
            
            ModelClass mc = modelClass;
            mxICell mainCell = hierarchyNodes(xcentre, ycentre, mc, width, height, yboundary);
            
            List<ForeignKey> allRefs =
                new ArrayList<ForeignKey>(modelClass.getCollections().values());
            allRefs.addAll(modelClass.getReferences().values());
            
            int refCount = allRefs.size();
            int collectionCount = modelClass.getCollections().size();
            double step, startAngle;
            if (refCount == 1) {
                step = 0;
                startAngle = Math.PI;
            } else if (refCount <= 5) {
                step = Math.PI * 0.9 / (refCount - 1);
                startAngle = Math.PI * 0.55;
            } else {
                step = Math.PI * 1.5 / (refCount - 1);
                startAngle = Math.PI / 4;
            }
            Iterator<ForeignKey> refIter = allRefs.iterator();
            
            for (int collNo = 0; refIter.hasNext(); collNo++) {
                ForeignKey key = refIter.next();
                boolean isCollection = collNo < collectionCount;
                
                int x = xcentre + (int) Math.rint(2 * width * Math.sin(startAngle + step * collNo));
                int y = (int) mainCell.getGeometry().getY()
                        - (int) Math.rint(2 * width * Math.cos(startAngle + step * collNo));
                
                /*
                ModelClass otherClass = modelClasses.get(key.getReferencedType().getName());
                if (modelClass.equals(otherClass)) {
                    String style =
                        isCollection ? SELF_COLLECTION_EDGE_STYLE : SELF_REFERENCE_EDGE_STYLE;
                    graph.insertEdge(parent, null, key.getName(), mainCell, mainCell, style);
                } else {
                */
                    mxICell otherCell =
                        (mxICell) graph.createVertex(parent, key.getReferencedType().getName(),
                                                     key.getReferencedType(), x, y,
                                                     width, height, null);
                    graph.addCell(otherCell);
                    graphNodes.put(key.getReferencedType().getName(), otherCell);
                
                    String style = isCollection ? COLLECTION_EDGE_STYLE : REFERENCE_EDGE_STYLE;
                    graph.insertEdge(parent, null, key.getName(), mainCell, otherCell, style);
                //}
            }
        } finally {
            graphModel.endUpdate();
        }
        
        attributeTableModel.setModelClass(modelClass);
        referenceTableModel.setModelClass(modelClass);
        graphComponent.scrollCellToVisible(graphNodes.get(modelClass.getName()));
    }
    
    /**
     * Plot the class hierarchy nodes from the given class back to the top level
     * class. This recursively traces back to the top of the hierarchy and then
     * adds graph nodes for each class back to the originally requested class.
     * 
     * @param xpos The horizontal position for the node.
     * @param ypos The vertical position for this level of node.
     * @param mc The ModelClass to add.
     * @param width The width for the class's cell.
     * @param height The height for the class's cell.
     * @param yboundary The margin at the top edge to leave.
     * 
     * @return The graph cell created for <code>mc</code>.
     */
    private mxICell hierarchyNodes(int xpos, int ypos, ModelClass mc,
                                   int width, int height, int yboundary) {
        mxICell parentCell = null;
        
        if (mc.getSuperclass() != null) {
            parentCell =
                hierarchyNodes(xpos, ypos - height - 40,
                               mc.getSuperclass(), width, height, yboundary);
            
            ypos = (int) parentCell.getGeometry().getY() + height + 40;
        }
        
        ypos = Math.max(yboundary, ypos);
        
        Object parent = graph.getDefaultParent();
        mxICell cell =
            (mxICell) graph.createVertex(parent, mc.getName(), mc, xpos, ypos, width, height, null);
        graph.addCell(cell);
        graphNodes.put(mc.getName(), cell);
        
        if (parentCell != null) {
            graph.insertEdge(parent, null, "extends", cell, parentCell,
                             INHERITANCE_EDGE_STYLE);
        }
        
        return cell;
    }
    
    /**
     * Display the project file chooser and set the project file if it
     * indicates a file has been selected.
     * 
     * @return The selected project file, or <code>null</code> if none
     * was selected.
     */
    public File chooseProjectFile() {
        File projectFile = null;
        int choice = projectFileChooser.showOpenDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            projectFile = projectFileChooser.getSelectedFile();
        }
        return projectFile;
    }
    
    /**
     * Load the model from the given project file and display it.
     * <p>Any exception raised while reading the file is logged and displayed in a
     * pop up dialog.</p>
     * 
     * @param projectFile The project file to load.
     */
    public void loadProject(File projectFile) {
        try {
            Model model = new ProjectLoader().loadModel(projectFile);
            
            initialise(model);
            
            MineManagerBackingStore.getInstance().setLastProjectFile(projectFile);
            
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
     * Class to listen for selection events in the class tree.
     */
    private class ClassTreeSelectionListener implements TreeSelectionListener, Serializable
    {
        private static final long serialVersionUID = 4845437302614347210L;

        /**
         * Determine the selected class from the event and change the other
         * components to display that class.
         * 
         * @param event The event from the JTree.
         */
        @Override
        public void valueChanged(TreeSelectionEvent event) {
            ClassTreeNode selected = (ClassTreeNode) event.getPath().getLastPathComponent();
            if (selected != null) {
                displayClass(selected.getModelClass());
            }
        }
    }
    
    /**
     * File filter for the project chooser dialog to filter for XML files.
     */
    protected static class XmlFileFilter extends FileFilter implements Serializable
    {
        private static final long serialVersionUID = 1374212258605568664L;

        /**
         * Check whether the given file is acceptable.
         * 
         * @param f The file to check.
         * 
         * @return <code>true</code> if <code>f</code> is a directory or its
         * filename ends with ".xml". <code>false</code> otherwise.
         */
        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
        }

        /**
         * Get the display text for this filter.
         * 
         * @return A suitable string for display.
         */
        @Override
        public String getDescription() {
            return Messages.getMessage("filter.project");
        }
    }
    
    /**
      * Action to build the model.
      */
    private class BuildModelAction extends AbstractAction {
        private static final long serialVersionUID = 1764086264698226094L;
    
        /**
          * Constructor.
          */
        public BuildModelAction() {

        }
    
        /** 
         * Called when the action fires, this method builds the model. 
         *  
         * @param event The action event. 
         */ 
        @Override 
        public void actionPerformed(ActionEvent event) { 
            File execDir = new File(projectHome, "/dbmodel/");

            List<String> commands = new ArrayList<String>(); 
            commands.add("ant"); 
            commands.add("build-db"); 
        
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
              new SystemProcessSwingWorker(commands, execDir, true); 

            progressDialog = new SystemProcessProgressDialog(parentFrame); 
            progressDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL); 
            progressDialog.setTitle(Messages.getMessage("build.model.title")); 
            progressDialog.setInformationLabel(Messages.getMessage("build.model.message"));

            progressDialog.setWorker(worker);
            progressDialog.writeOutput(b + "\n\n");
            worker.execute();
            progressDialog.setVisible(true);
        }
    }
}
