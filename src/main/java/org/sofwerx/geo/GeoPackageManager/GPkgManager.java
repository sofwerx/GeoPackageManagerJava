package org.sofwerx.geo.GeoPackageManager;

import java.io.File;
import java.io.IOException;
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
	File originalFile;
	GPkgManager(){
		originalFile = new File("res/example.gpkg");
	}
	//This retrieves all of the Feature information from the DAO (Data Access Object)
	//The Dao handles Abstraction between the Database and the Application Layers
	//There are other Attributes that can be Gathered from the Dao like Bounding Box, and Spatial Reference Systems (SRS)
	//Reference The Repo and Docs for more info: https://github.com/ngageoint/geopackage-java
	/**
	 * @param featureDao
	 * @return A Hashmap of Information about the Feature that you passed, By Default, it passes ID, Name, and the Size in Bytes of the Geometries
	 */
	public HashMap<String,Object> getFeatureDaoInfo(FeatureDao featureDao){
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
	public HashMap<String,Object> getTileDaoInfo(TileDao tileDao){
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
	public HashMap<String,Object> getGeometryDaoInfo(GeometryColumnsDao geometryDao){
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
	 * @param GeoPackage
	 * @return This Method Will Parse the geopackage and get any relevant data from them. Storing them in object variables
	 */
	public void retrieveGeoPackageInformation(GeoPackage geoPackage) {
		this.geoPackage = geoPackage;
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

	public boolean SaveInterface() {
		System.out.print("What is the Name of you File (e.g. MyMap): ");
		String fileName = scan.nextLine();
		GeoPackageManager.create(new File("res/"+fileName+".gpkg"));
		//GeoPackage newGeoPackage = GeoPackageManager.open(new File("res/"+fileName+".gpkg"));
		try {
			GeoPackageIOUtils.copyFile(originalFile,new File("res/"+fileName+".gpkg") );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	

	public boolean SelectInterface() {
		System.out.print("What is the Name of you File (e.g. MyMap): ");
		String fileName = scan.nextLine();
		GeoPackageManager.create(new File("res/"+fileName+".gpkg"));
		//GeoPackage newGeoPackage = GeoPackageManager.open(new File("res/"+fileName+".gpkg"));
		try {
			GeoPackageIOUtils.copyFile(originalFile,new File("res/"+fileName+".gpkg") );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	/**
	 * @param String input
	 * @return This Method returns True if input is successful, and false if it raises an error. 
	 */
	public boolean consoleInput(String input) {
		if(input.equals("exit")) {
			System.out.println("Good Bye");
			isActive = false;
		}
		if(input.equals("help")) {
			System.out.println("Current Commands are: 'select' 'save' 'help' 'exit'");
		}
		if(input.equals("save")) {
			SaveInterface();
			System.out.println("You Saved");
		}
		if(input.equals("select")) {
			SelectInterface();
			System.out.println("You Selected");
		}
		return true;
	}
	
	//This function does not make sense atm. Please avoid using
	public String newQuery(FeatureDao featureDao) {

        
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
