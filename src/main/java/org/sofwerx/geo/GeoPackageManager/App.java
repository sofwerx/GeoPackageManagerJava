package org.sofwerx.geo.GeoPackageManager;

import sof.works.intern.GeoPackageManager.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.*;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.columns.GeometryColumnsDao;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureResultSet;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.io.GeoPackageIOUtils;
import mil.nga.geopackage.manager.GeoPackageManager;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.tiles.user.TileResultSet;
import mil.nga.geopackage.tiles.user.TileRow;
import mil.nga.wkb.geom.Geometry;
import java.util.Scanner; 
/**
 * This is an App to render and Manage Geopackages.
 * Extra Functionality may be included to download GPkgs and maybe to create custom GPkgs
 */
public class App extends JPanel implements ActionListener
{	
	JButton openButton, saveButton;
    JTextArea log;
    JFileChooser fc;
    JPanel editor;
	GPkgManager mng;
    JTextField input;
    private static String ENTER = "Enter";
	public void drawWindow(){
		//Drawing the Log and the Text Input for console Commands
        //Create the log first, because the action listeners
        //need to refer to it.
        log = new JTextArea(5,20);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);
        input = new JTextField(20);
        input.setActionCommand(ENTER);
        JPanel BottomPanel = new JPanel();
        BottomPanel.setLayout(new BoxLayout(BottomPanel, BoxLayout.Y_AXIS));
        BottomPanel.add(logScrollPane);
        BottomPanel.add(input);
        //Create a file chooser
        fc = new JFileChooser();
        fc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              //System.out.println(fc.getSelectedFile().getPath());
              mng.loadGeoPackage(fc.getSelectedFile());
              HashMap<Integer,HashMap<FeatureDao,Boolean>> features = mng.getFeatures();
              HashMap<Integer,HashMap<TileDao,Boolean>> tiles = mng.getTiles();
              editor.removeAll();
        	  SortedSet<Integer> keys = new TreeSet<Integer>(features.keySet());
              for(Integer retrieveKeys: keys) {
            	  JPanel ZoomLayerPanel = new JPanel();
            	  JLabel ZoomLayerLabel = new JLabel("Zoom Layer: " + retrieveKeys.toString());
            	  JPanel ZoomLayerDetails = new JPanel();
            	  ZoomLayerPanel.add(ZoomLayerLabel);
            	  ZoomLayerDetails.setLayout(new BoxLayout(ZoomLayerDetails, BoxLayout.Y_AXIS));
            	  HashMap<FeatureDao,Boolean> featureMap = features.get(retrieveKeys);
            	  for(FeatureDao fDao : featureMap.keySet()) {
	                  final JCheckBox chkBox = new JCheckBox(fDao.getGeometryColumnName());
	                  chkBox.setSelected(featureMap.get(fDao));
	                  ZoomLayerDetails.add(chkBox);
	                  
            	  }
            	  ZoomLayerPanel.add(ZoomLayerDetails);
            	  editor.add(ZoomLayerPanel);
            	  revalidate();
              }
            }
          });

        //Uncomment one of the following lines to try a different
        //file selection mode.  The first allows just directories
        //to be selected (and, at least in the Java look and feel,
        //shown).  The second allows both files and directories
        //to be selected.  If you leave these lines commented out,
        //then the default mode (FILES_ONLY) will be used.
        //
        //fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        //Create the open button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        openButton = new JButton("Open a GeoPackage...");
                                 //createImageIcon("images\\Open16.gif"));
        openButton.addActionListener(this);

        //Create the save button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        saveButton = new JButton("Save a GeoPackage...");
                                 //createImageIcon("images\\Save16.gif"));
        saveButton.addActionListener(this);

        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(openButton);
        buttonPanel.add(saveButton);


        //For layout purposes, put the buttons in a separate panel
        editor = new JPanel(); //use FlowLayout
        editor.setLayout(new BoxLayout(editor, BoxLayout.Y_AXIS));
        //Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(editor, BorderLayout.CENTER);
        add(logScrollPane, BorderLayout.PAGE_END);
	}
	public App() {

		super(new BorderLayout());
		mng = new GPkgManager();
		drawWindow();
		Scanner scan = new Scanner(System.in);
		JFrame frame = new JFrame("Simple GUI");
	}
    public static void main( String[] args )
    { 
    	//Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE); 
                createAndShowGUI();
            }
        });
	    //new App();
	     
			
    }
    public void actionPerformed(ActionEvent e) {

        //Handle open button action.
        if (e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(App.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                //This is where a real application would open the file.
                log.append("Opening: " + file.getName() + "." + "\n");
            } else {
                log.append("Open command cancelled by user." + "\n");
            }
            log.setCaretPosition(log.getDocument().getLength());

        //Handle save button action.
        } else if (e.getSource() == saveButton) {
            int returnVal = fc.showSaveDialog(App.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                //This is where a real application would save the file.
                log.append("Saving: " + file.getName() + "." + "\n");
            } else {
                log.append("Save command cancelled by user." + "\n");
            }
            log.setCaretPosition(log.getDocument().getLength());
        }
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = App.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("FileChooserDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Add content to the window.
        frame.add(new App());

        //Display the window.      
        frame.setSize(1000, 800);
        frame.setVisible(true);
    }
}
