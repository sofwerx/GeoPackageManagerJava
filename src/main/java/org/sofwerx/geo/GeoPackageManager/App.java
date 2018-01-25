package org.sofwerx.geo.GeoPackageManager;

import sof.works.intern.GeoPackageManager.*;

import java.awt.Button;
import java.awt.Component;
import java.awt.Frame;  
import java.awt.Panel;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class App extends Frame
{	
	
	public App() {  
		GPkgManager mng = new GPkgManager();
		Scanner scan = new Scanner(System.in);
		GeoPackage geoPackage = GeoPackageManager.open(new File("res/example.gpkg"));
		mng.retrieveGeoPackageInformation(geoPackage);
		mng.displayZoomLevelInfo();
		while(mng.isActive()) {	
			System.out.print("Input ('help' for List): ");
			String input = scan.nextLine();
			mng.consoleInput(input);
		}
	}
    public static void main( String[] args )
    {
	      new App();
    }
}
