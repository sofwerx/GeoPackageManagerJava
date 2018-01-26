package org.sofwerx.geo.GeoPackageManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageCore;
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

public class GPkgManager {
	GeoPackage geoPackage; //All Feature Table Names
	List<String> features; //All Feature Table Names
	List<String> tiles; //All Tile Table Names
	HashMap<Integer,HashMap<FeatureDao,Boolean>> featureZoomLevels; //All Feature Levels Categorized by zoom
	HashMap<Integer,HashMap<TileDao,Boolean>> tileZoomLevels; //All Tile Levels Categorized by zoom
	Scanner scan = new Scanner(System.in);
	boolean isActive = true; //This basically records if we've closed the manager
	File originalFile;	File workableFile;
	GPkgManager(){
		originalFile = new File("res/example.gpkg");//defaults to example
		workableFile = new File("res/tmp.gpkg");
		if(workableFile.exists()) { //checking if the Tmp File already has information
			workableFile.delete();
		}
		System.out.println("Default File loaded");
		loadGeoPackage(originalFile);
	}
	GPkgManager(String fileName){
		originalFile = new File(fileName);//defaults to example
		workableFile = new File("res/tmp.gpkg");
		if(!originalFile.exists()) {
			System.out.println("File " + fileName +" does not exist");
			System.exit(0);
			closeGeoPackage();
			isActive = false;
		}

		System.out.println("File " + fileName +" loaded");
		
		if(workableFile.exists()) { //checking if the Tmp File already has information
			workableFile.delete();
		}
		loadGeoPackage(originalFile);
	}
	
	/**
	 * This will retrive the Daos for the GUI to Visualize
	 */
	public HashMap<Integer,HashMap<FeatureDao,Boolean>> getFeatures(){
		return featureZoomLevels;
	}
	public HashMap<Integer,HashMap<TileDao,Boolean>> getTiles(){
		return tileZoomLevels;
	}
	
	//This retrieves all of the Feature information from the DAO (Data Access Object)
	//The Dao handles Abstraction between the Database and the Application Layers
	//There are other Attributes that can be Gathered from the Dao like Bounding Box, and Spatial Reference Systems (SRS)
	//Reference The Repo and Docs for more info: https://github.com/ngageoint/geopackage-java
	/**
	 * @param featureDao
	 * @return A Hashmap of Information about the Feature that you passed, By Default, it passes ID, Name, and the Size in Bytes of the Geometries
	 */
	private HashMap<String,Object> getFeatureDaoInfo(FeatureDao featureDao){
		HashMap<String,Object> retMap = new HashMap<>();
        FeatureResultSet featureCursor = featureDao.queryForAll();
        int geometrySize = 0;
        GeometryColumns gColumns = featureDao.getGeometryColumns();
        retMap.put("ID",gColumns.getId());
        retMap.put("Name",gColumns.getColumnName());
		try {
            while (featureCursor.moveToNext()) {
                FeatureRow featureRow = featureCursor.getRow();
                GeoPackageGeometryData geometryData = featureRow.getGeometry();
                Geometry geometry = geometryData.getGeometry();
                geometrySize += geometryData.getBytes().length;
            }
        } finally {
            featureCursor.close();
           
        }

        retMap.put("Size",geometrySize);
		return retMap;
	}
	/**
	 * @param tileDao
	 * @return A Hashmap of Information about the Tile that you passed, By Default, it passes ID, Name, and the Size in Bytes of the Geometries
	 */
	private HashMap<String,Object> getTileDaoInfo(TileDao tileDao){
		HashMap<String,Object> retMap = new HashMap<>();
        TileResultSet featureCursor = tileDao.queryForAll();
        int geometrySize = 0;
        retMap.put("ID",tileDao.getTable().getPkColumnIndex());
        retMap.put("Name",tileDao.getTable().getTableName());
        try{
            while(featureCursor.moveToNext()){
                TileRow tileRow = featureCursor.getRow();
                byte[] tileBytes = tileRow.getTileData();
                geometrySize += tileBytes.length;
            }
        }finally{
            featureCursor.close();
        }

        retMap.put("Size",geometrySize);
		return retMap;
	}

	//This is unused for now. May come back to it
	private HashMap<String,Object> getGeometryDaoInfo(GeometryColumnsDao geometryDao){
		HashMap<String,Object> retMap = new HashMap<>();
        List<GeometryColumns> geometryColumns = null;
		try {
			geometryColumns = geometryDao.queryForAll();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        int geometrySize = 0;
        retMap.put("Name",geometryDao.getTableName());
        for(GeometryColumns gColumn : geometryColumns) {
        	retMap.put("ID",gColumn.getId());
            //retMap.put("Name",gColumn.getColumnName());
    		//gColumn.;
        }
        retMap.put("Size",geometrySize);
		return retMap;
	}
	
	/**
	 * @param 
	 * @return This Method Displays the Information contained in the FeatureDao and TileDao and Separates them by Zoom Levels
	 */
	public void displayZoomLevelInfo() {//HashMap<Integer,HashMap<FeatureDao,Boolean>> featureZoomLevels

		System.out.println("Feature Layers:");
		for (Integer key : featureZoomLevels.keySet()) {
			System.out.println("\tZoom Level: " + key);
			HashMap<FeatureDao,Boolean> featureMap = featureZoomLevels.get(key);
			int layerSize = 0;

	    	System.out.print("\t\tSubLayers Enabled:");
		    for(FeatureDao featureDao: featureMap.keySet()) {
		    	HashMap<String,Object> info = getFeatureDaoInfo(featureDao);
		    	System.out.print((featureMap.get(featureDao)?info.get("Name") + "\t":""));
		    	if((featureMap.get(featureDao))) {
		    		layerSize += (Integer)info.get("Size");
		    	}
		    }
		    System.out.println("\n\t\tThis Layer is " + GeoPackageIOUtils.formatBytes(layerSize));
		}

		System.out.println("Tile Layers:");
		for (Integer key : tileZoomLevels.keySet()) {
			System.out.println("\tZoom Level: " + key);
			HashMap<TileDao,Boolean> tileMap = tileZoomLevels.get(key);
			int layerSize = 0;
	    	System.out.print("\t\tSubLayers Enabled:");
		    for(TileDao tileDao: tileMap.keySet()) {
		    	HashMap<String,Object> info = getTileDaoInfo(tileDao);
		    	System.out.print(((tileMap.get(tileDao)?info.get("Name") + "\t":"")));
		    	if((tileMap.get(tileDao))) {
		    		layerSize += (Integer)info.get("Size");
		    	}
		    }
		    System.out.println("\n\t\tThis Layer is " + GeoPackageIOUtils.formatBytes(layerSize) );
		}
	}
	
	/**
	 * @param 
	 * @return This Method Will Parse the geopackage and get any relevant data from them. Storing them in object variables
	 */
	private void retrieveGeoPackageInformation() {
		features = geoPackage.getFeatureTables();
		tiles = geoPackage.getTileTables();
		GeometryColumnsDao geometryDao = geoPackage.getGeometryColumnsDao();
		featureZoomLevels = new HashMap<>();
		tileZoomLevels = new HashMap<>();
		for(String feature : features) {
			FeatureDao featureDao = geoPackage.getFeatureDao(feature);
			int currentZoomLevel = featureDao.getZoomLevel();//String result = mng.newQuery(featureDao);	
			HashMap<FeatureDao,Boolean> featureSubSet = featureZoomLevels.get(currentZoomLevel);
			if (featureSubSet == null) {
			    featureSubSet = new HashMap<>();
			    featureZoomLevels.put(currentZoomLevel, featureSubSet);
			}
		    featureSubSet.put(featureDao,true);//System.out.println(featureSubSet.size());//System.out.println(featureDao.getGeometryColumnName());//System.out.println(featureDao.getZoomLevel());
		}
		for(String tile : tiles) {
			TileDao tileDao = geoPackage.getTileDao(tile);
			int currentZoomLevel = tileDao.getZoomLevel();//String result = mng.newQuery(featureDao);	
			HashMap<TileDao,Boolean> tileSubSet = tileZoomLevels.get(currentZoomLevel);
			if (tileSubSet == null) {
				tileSubSet = new HashMap<>();
				tileZoomLevels.put(currentZoomLevel, tileSubSet);
			}
			tileSubSet.put(tileDao,true);
		}
	}

	/**
	 * @param 
	 * @return This Method Returns the open state of the Manager
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * This will copy the Geopackage from a file and load it into a temporary geopackage. It will then save back into a geopackage when it's finished and delete the temporary geopackage
	 * @param File, the name of the file to be loaded
	 * @return True if the Geopackage was loaded and copied, False if otherwise.
	 */
	public boolean loadGeoPackage(File file) {
		if(workableFile.exists()) {
			workableFile.delete();
		}
		try {
			GeoPackageIOUtils.copyFile(originalFile,workableFile );
			this.geoPackage =  GeoPackageManager.open(workableFile);
			retrieveGeoPackageInformation();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
		}
		return true;
	}

	/**
	 * This will copy the Geopackage from a file and load it into a temporary geopackage. It will then save back into a geopackage when it's finished and delete the temporary geopackage
	 * @param File, the name of the file to be saved
	 * @return True if the Geopackage was saved, False if otherwise.
	 */
	public boolean saveGeoPackage(File newFile) {
		try {
			GeoPackageIOUtils.copyFile(workableFile,newFile );
			this.geoPackage =  GeoPackageManager.open(workableFile);
			retrieveGeoPackageInformation();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
		}
		return true;
	}
	/**
	 * This will save and close the Geopackage from a file. It deletes the temporary geopackage
	 * @param 
	 * @return True if the Geopackage was loaded and copied, False if otherwise.
	 */
	private boolean closeGeoPackage() {
		try {
			GeoPackageIOUtils.copyFile(workableFile,originalFile);
			workableFile.delete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
		}
		return true;
	}
	
	/**
	 * Manager Interfaces abstract functions from the geopackage manager.
	 * @return True if the Interface was successful.
	 */
	private boolean SaveInterface() {
		System.out.print("What is the Name of you File (e.g. MyMap): ");
		String fileName = scan.nextLine();
		File newFile= new File("res/"+fileName+".gpkg");
		GeoPackageManager.create(newFile);
		
		//GeoPackage newGeoPackage = GeoPackageManager.open(new File("res/"+fileName+".gpkg"));
		GeoPackage newGeoPackage = GeoPackageManager.open(newFile);
		//newGeoPackage.createTileTable();
		try {
			GeoPackageIOUtils.copyFile(workableFile,newFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	/**
	 * Load Interface Will load a file based on the string given to it;
	 * @return True if the Interface was successful.
	 */
	private boolean LoadInterface() {
		System.out.print("What is the Name of your Geopackage(e.g. example.gpkg): ");
		String fileName = scan.nextLine();
		if(loadGeoPackage(new File(fileName))) {
			System.out.println("GeoPackage Loaded");
		}else {
			return false;
		}
		return true;
	}
	
	private void saveEdits() {
		for (Integer key : tileZoomLevels.keySet()) {
			HashMap<TileDao,Boolean> maniDao = tileZoomLevels.get(key);
			for(TileDao tDao: maniDao.keySet()) {
				if(!maniDao.get(tDao)) {
					TileResultSet tRows = tDao.queryForAll();
					try{
					    while(tRows.moveToNext()){
					    	TileRow featureRow = tRows.getRow();
							tDao.deleteById(featureRow.getId());
					    }
					}finally{
						tRows.close();
					}
				}
			}
		}
		for (Integer key : featureZoomLevels.keySet()) {
			HashMap<FeatureDao,Boolean> maniDao = featureZoomLevels.get(key);
			for(FeatureDao tDao: maniDao.keySet()) {

				if(!maniDao.get(tDao)) {
					FeatureResultSet tRows = tDao.queryForAll();
					try{
					    while(tRows.moveToNext()){
					        FeatureRow featureRow = tRows.getRow();
							tDao.deleteById(featureRow.getId());
					    }
					}finally{
						tRows.close();
					}
				}
			}
		}
		try {
			GeoPackageIOUtils.copyFile(workableFile,originalFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private boolean EditInterface() {
		String InfoSet = "";
		Integer LayerNumber = 0;
		ArrayList<Integer> layerNums = new ArrayList<>();
		HashMap<Integer, Object> sublayerNums = new HashMap<>();
		String[] arrayCommands = new String[3]; 
		String input;
		boolean interfaceOpen = true;
		
		while(interfaceOpen) {
			System.out.print("Enter Input (Type 'Help' for more instructions): ");
			
		    for(int i=0;i<3;i++){//for reading array
		    	if(scan.hasNext()) {
		    		
		    		arrayCommands[i]=scan.next();

		    		if(arrayCommands[i].equals("Back")) {

		    		    if(scan.hasNextLine()) {
		    		    	scan.nextLine();
		    		    }
		    			saveEdits();
						return true;
					}
		    		if(arrayCommands[i].equals("Save")) {
		    			saveEdits();
						return true;
					}
					if(arrayCommands[i].equals("Help")) {
						System.out.println("Commands can be issued to Select and Layer and Enable/Disable Their Features");
						System.out.println("Syntaxes are as Follows");
						System.out.println("Help");
						System.out.println("Back");
						System.out.println("Save");
						System.out.println("[select enable disable] set [Tile Feature]");
						System.out.println("[select enable disable] [layer sublayer] [#]");
						break;
					}
		    	}else {
		    		arrayCommands[i] = "";
		    		break;
		    	}
		    }
		    if(scan.hasNextLine()) {
		    	scan.nextLine();
		    }

			if(arrayCommands[0].equals("select")) {
				if(arrayCommands[1].equals("set")) {
					if(arrayCommands[2].equals("Tile")) {
						InfoSet = "Tile";
						System.out.println("Tile Set Selected");
					}
					else if(arrayCommands[2].equals("Feature")) {
						InfoSet = "Feature";
						System.out.println("Feature Set Selected");
					}else {
						System.out.println("The Name was not able to be selected");
					}
					layerNums = new ArrayList<>();
					if(InfoSet.equals("Tile")) {
						for (Integer key : tileZoomLevels.keySet()) {
							layerNums.add(key);
						}
					}
					if(InfoSet.equals("Feature")) {
						for (Integer key : featureZoomLevels.keySet()) {
							layerNums.add(key);
						}
					}
				}
				else if(arrayCommands[1].equals("layer")) {
					if(InfoSet.equals("Tile") || InfoSet.equals("Feature")) {
						for(Integer lComparison :  layerNums) {
							if(lComparison == Integer.parseInt(arrayCommands[2])) {
								LayerNumber = Integer.parseInt(arrayCommands[2]);
								sublayerNums = new HashMap<>();								
								System.out.println("Layer Number " + LayerNumber + " selected");
								break;
							}
						}
					}else {
						System.out.println("You must Select your Information Set First");
						System.out.println("select set [Tile Feature]");
					}
				}
				else if(arrayCommands[1].equals("sublayer")) {
					if(InfoSet.equals("Tile") || InfoSet.equals("Feature")) {
						System.out.println("Sublayer Functionality Coming Soon");
					}else {
						System.out.println("You must Select your Information Set First");
						System.out.println("select set [Tile Feature]");
					}
				}
			}

			if(arrayCommands[0].equals("enable")) {
				if(arrayCommands[1].equals("set")) {
					if(arrayCommands[2].equals("Tile")) {
						InfoSet = "Tile";
						System.out.println("Tile Set Enabled");
					}
					else if(arrayCommands[2].equals("Feature")) {
						InfoSet = "Feature";
						System.out.println("Feature Set Enabled");
					}else {
						System.out.println("The Name was not able to be enabled");
					}
					layerNums = new ArrayList<>();
					if(InfoSet.equals("Tile")) {
						for (Integer key : tileZoomLevels.keySet()) {
							HashMap<TileDao,Boolean> maniDao = tileZoomLevels.get(key);
							for(TileDao tDao: maniDao.keySet()) {
								maniDao.put(tDao, true);
							}
						}
					}
					if(InfoSet.equals("Feature")) {
						for (Integer key : featureZoomLevels.keySet()) {
							HashMap<FeatureDao,Boolean> maniDao = featureZoomLevels.get(key);
							for(FeatureDao tDao: maniDao.keySet()) {
								maniDao.put(tDao, true);
							}
						}
					}
				}
				else if(arrayCommands[1].equals("layer")) {
					if(InfoSet.equals("Tile") || InfoSet.equals("Feature")) {
						for(Integer lComparison :  layerNums) {
							if(lComparison == Integer.parseInt(arrayCommands[2])) {
								LayerNumber = Integer.parseInt(arrayCommands[2]);
								System.out.println("Layer Number " + LayerNumber + " enabled");
								if(InfoSet.equals("Tile")) {
									HashMap<TileDao,Boolean> maniDao = tileZoomLevels.get(LayerNumber);
									for(TileDao tDao: maniDao.keySet()) {
										maniDao.put(tDao, true);
									}
								}
								if(InfoSet.equals("Feature")) {
									HashMap<FeatureDao,Boolean> maniDao = featureZoomLevels.get(LayerNumber);
									for(FeatureDao tDao: maniDao.keySet()) {
										maniDao.put(tDao, true);
									}
								}
								break;
							}
						}
					}else {
						System.out.println("You must Select your Information Set First");
						System.out.println("select set [Tile Feature]");
					}
				}
				else if(arrayCommands[1].equals("sublayer")) {
					if(InfoSet.equals("Tile") || InfoSet.equals("Feature")) {
						System.out.println("Sublayer Functionality Coming Soon");
					}else {
						System.out.println("You must Select your Information Set First");
						System.out.println("select set [Tile Feature]");
					}
				}
			}
			if(arrayCommands[0].equals("disable")) {
				if(arrayCommands[1].equals("set")) {
					if(arrayCommands[2].equals("Tile")) {
						InfoSet = "Tile";
						System.out.println("Tile Set Disabled");
					}
					else if(arrayCommands[2].equals("Feature")) {
						InfoSet = "Feature";
						System.out.println("Feature Set Disabled");
					}else {
						System.out.println("The Name was not able to be enabled");
					}
					layerNums = new ArrayList<>();
					if(InfoSet.equals("Tile")) {
						for (Integer key : tileZoomLevels.keySet()) {
							HashMap<TileDao,Boolean> maniDao = tileZoomLevels.get(key);
							for(TileDao tDao: maniDao.keySet()) {
								maniDao.put(tDao, false);
							}
						}
					}
					if(InfoSet.equals("Feature")) {
						for (Integer key : featureZoomLevels.keySet()) {
							HashMap<FeatureDao,Boolean> maniDao = featureZoomLevels.get(key);
							for(FeatureDao tDao: maniDao.keySet()) {
								maniDao.put(tDao, false);
							}
						}
					}
				}
				else if(arrayCommands[1].equals("layer")) {
					if(InfoSet.equals("Tile") || InfoSet.equals("Feature")) {
						for(Integer lComparison :  layerNums) {
							if(lComparison == Integer.parseInt(arrayCommands[2])) {
								LayerNumber = Integer.parseInt(arrayCommands[2]);
								System.out.println("Layer Number " + LayerNumber + " disabled");
								if(InfoSet.equals("Tile")) {
									HashMap<TileDao,Boolean> maniDao = tileZoomLevels.get(LayerNumber);
									for(TileDao tDao: maniDao.keySet()) {
										maniDao.put(tDao, false);
									}
								}
								if(InfoSet.equals("Feature")) {
									HashMap<FeatureDao,Boolean> maniDao = featureZoomLevels.get(LayerNumber);
									for(FeatureDao tDao: maniDao.keySet()) {
										maniDao.put(tDao, false);
									}
								}
								break;
							}
						}
					}else {
						System.out.println("You must Select your Information Set First");
						System.out.println("select set [Tile Feature]");
					}
				}
				else if(arrayCommands[1].equals("sublayer")) {
					if(InfoSet.equals("Tile") || InfoSet.equals("Feature")) {
						System.out.println("Sublayer Functionality Coming Soon");
					}else {
						System.out.println("You must Select your Information Set First");
						System.out.println("select set [Tile Feature]");
					}
				}
			}
		}

		
		return true;
	}
	
	/**
	 * @param String input
	 * @return This Method returns True if input is successful, and false if it raises an error. 
	 */
	public boolean consoleInput(String input) {
		if(input.equals("exit")) {
			closeGeoPackage();
			System.out.println("Good Bye");
			isActive = false;
		}
		if(input.equals("help")) {
			System.out.println("Current Commands are: 'display' 'edit' 'save' 'load' 'help' 'exit'");
		}
		if(input.equals("display")) {
			displayZoomLevelInfo();
		}
		if(input.equals("save")) {
			SaveInterface();
			System.out.println("You Saved");
		}
		if(input.equals("edit")) {
			EditInterface();
			System.out.println("You Selected");
		}
		if(input.equals("load")) {
			LoadInterface();
			System.out.println("The GeoPackage was Loaded");
		}
		return true;
	}
	
	//This function does not make sense atm. Please avoid using
	private String newQuery(FeatureDao featureDao) {

        
        HashMap<String,Object> fieldValues = new HashMap<>();
        //FeatureCursor featureCursor = featureDao.queryForEq("STATE_NAME","Nevada");
        FeatureResultSet featureCursor = featureDao.queryForAll();

        String informationString = "";

        List<String> columnNames = new ArrayList<>();
        HashMap<String, Set<String>> featureMap = new HashMap<>();

        for (int inforColumnInit = 0; inforColumnInit < columnNames.size(); inforColumnInit++) {
            featureMap.put(columnNames.get(inforColumnInit), new HashSet<String>());
        }
        int geometrySize = 0;
        //System.out.println(featureDao.count());
        if (featureDao.count() != 0) {
        	//)
            //layerSize.setText("(" + featureCursor.getColumnName(0)+ ")");
            try {
                while (featureCursor.moveToNext()) {
                    FeatureRow featureRow = featureCursor.getRow();
                    GeoPackageGeometryData geometryData = featureRow.getGeometry();
                    Geometry geometry = geometryData.getGeometry();
                    geometrySize += geometryData.getBytes().length;

                    for (int inforColumnInit = 0; inforColumnInit < columnNames.size(); inforColumnInit++) {
                        //featureMap.put(columnNames.keySet().toArray()[inforColumnInit].toString(),new HashSet<String>());
                        String currentAttr = columnNames.get(inforColumnInit);
                        featureMap.get(currentAttr).add(featureRow.getValue(currentAttr).toString());
                    }
                }
            } finally {
                featureCursor.close();
                //Prints out the HashMap by Key Values
                for (String Attrs : featureMap.keySet()) {
                    Set<String> tmpStringSet = featureMap.get(Attrs);
                            informationString += "\t\t" + Attrs + "\n";
                            for(String Vals: tmpStringSet){
                                informationString += "\t\t\t\t" + Vals + "\n";
                            }/**/
                }
            }
        }
        informationString += ("\tSize: " + GeoPackageIOUtils.formatBytes(geometrySize) + "\n\n");
        return informationString;
    }
}
