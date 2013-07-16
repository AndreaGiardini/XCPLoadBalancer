package main;

import java.io.FileInputStream;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Properties;

import org.apache.xmlrpc.XmlRpcException;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;


public class Main {
	/*
	 * TODO:
	 *  - Improve error checking
	 */
	private static com.xensource.xenapi.Connection xenConn;
	private static java.sql.Connection sqlConn;
	private static DecimalFormat df = new DecimalFormat("#0.00");
	private static VmOperation VmOp = null;
	private static HostOperation HostOp = null;
	private static PreparedStatement ps = null;


	public static void main(String[] args) throws Exception {
		String XenHost = "";
		String XenUsr = "";
		String XenPass = "";
		String MysqlHost = "";
		String MysqlUsr = "";
		String MysqlPass = "";
			
		/*
		 * Get settings file
		 */
		Properties configFile = null;
		try{
			configFile = new Properties();
			configFile.load(new FileInputStream(System.getProperty("user.dir") + "\\..\\settings"));
		} catch (Exception e){
			System.out.println("Unable to locate settings file");
			System.exit(1);
		}
		
		XenHost = configFile.getProperty("XenHost");
		XenUsr = configFile.getProperty("XenUsr");
		XenPass = configFile.getProperty("XenPass");
		MysqlHost = configFile.getProperty("MysqlHost");
		MysqlUsr = configFile.getProperty("MysqlUsr");
		MysqlPass = configFile.getProperty("MysqlPass");
		
		/*
		 * Connection to Xen Pool
		 */
		Pool srv = new Pool(XenHost,XenUsr,XenPass);
		srv.connect();
		xenConn = srv.getConnection();
				 
		/*
		 * Connection to Mysql Db
		 */
	    String url = "jdbc:mysql://" + MysqlHost + ":3306/";
	    String dbName = "Xen";
	    String driver = "com.mysql.jdbc.Driver";
	    try {
	    	Class.forName(driver).newInstance();
	    	sqlConn = DriverManager.getConnection(url+dbName,MysqlUsr,MysqlPass);
	    } catch (Exception e) {
	    	e.printStackTrace();
		}
		        
	    System.out.println("Connected to the database");
	    
	    /*
	     * Initialize VmOperation, HostOperation and PreparedStatement classes
	     */
	    
	    VmOp = new VmOperation(xenConn, sqlConn);
	    HostOp = new HostOperation(xenConn, sqlConn);
	    
	    /*
	     * Updating informations
	     */
//	    try {
//	    	updateVmInfo();
//	    	updateHostInfo();
//	    } catch (MySQLIntegrityConstraintViolationException e){
//	    	/*
//	    	 * If a Vm or a Host is deleted a MySQLIntegrityConstraintViolationException
//	    	 * is launched because the program it's trying to delete a value in the Vm/Host
//	    	 * table while there is a foreign reference in the table Map
//	    	 * 
//	    	 * SOLUTION: delete first the Map reference and then the Vm/Host one
//	    	 */
//	    	updateMapInfo();
//	    	ps=sqlConn.prepareStatement("DROP TABLE IF EXISTS deleteVm");
//	    	System.out.println(ps.toString());
//	    	ps.execute();
//	    	ps=sqlConn.prepareStatement("DROP TABLE IF EXISTS deleteMap");
//	    	System.out.println(ps.toString());
//	    	ps.execute();
//	    	ps=sqlConn.prepareStatement("DROP TABLE IF EXISTS deleteHost");
//	    	System.out.println(ps.toString());
//	    	ps.execute();
//	    	updateVmInfo();
//	    	updateHostInfo();
//	    } finally {
//	    	updateMapInfo();
//	    }
	    
	    try {
	    	updateVmInfo();
    		updateHostInfo();
    		updateMapInfo();
	    } catch (MySQLIntegrityConstraintViolationException e){
	    	/*
	    	 * If a Vm or a Host is deleted a MySQLIntegrityConstraintViolationException
	    	 * is launched because the program it's trying to delete a value in the Vm/Host
	    	 * table while there is a foreign reference in the table Map
	    	 * 
	    	 * SOLUTION: delete first the Map reference and then the Vm/Host one
	    	 */
	    	updateMapInfo();
	    	ps=sqlConn.prepareStatement("DROP TABLE IF EXISTS deleteVm");
	    	System.out.println(ps.toString());
	    	ps.execute();
	    	ps=sqlConn.prepareStatement("DROP TABLE IF EXISTS deleteMap");
	    	System.out.println(ps.toString());
	    	ps.execute();
	    	ps=sqlConn.prepareStatement("DROP TABLE IF EXISTS deleteHost");
	    	System.out.println(ps.toString());
	    	ps.execute();
	    	updateVmInfo();
	    	updateHostInfo();
	    }
	    
	    System.out.println("Disconnected from database");

		
		/*
		 * Extra Informations
		 */
	    
//		printApiVersion();
//		VmOp.printTemplateList();
//		VmOp.printControlDomainList();
//		VmOp.printVmList();
//		VmOp.printVmInformation();
//		HostOp.printHostLoad();
//		VmOp.printVmLoad();
//	    HostOp.printHostDataSources();
//		VmOp.printVmDataSources();

	    /*
	     * Closing sessions and exiting
	     */
	    
	    sqlConn.close();
		srv.getSession().logout(xenConn);
		System.exit(0);
		
	}
	
	/*
	 * For each corrispondance Vm-Host in the pool create and execute an INSERT or UPDATE query
	 */
	
	private static void updateMapInfo() throws SQLException, BadServerResponse, XenAPIException, XmlRpcException{
	    /*
	     * Remove the no-longer existing matches:
	     *  - Create a temporary table (deleteMap)
	     *  - Insert in deleteMap all the old matches that no longer exist
	     *  - Delete all those matches in the main one
	     * 	TABLE: Map
	     */
	      
	    ps = sqlConn.prepareStatement("CREATE TEMPORARY TABLE deleteMap (" +
	    												"HostUuid CHAR(36)," + 
	    												"VmUuid CHAR(36)" + 
	    		  										")");
	    System.out.println(ps.toString());
	    ps.execute();
	    
	    /*
	     * TODO: Find a better way to do this query in JDBC-style
	     * Parameter number it's a limit for PreparedStatement
	     */
	    ps = sqlConn.prepareStatement("INSERT INTO deleteMap " +
	    				"SELECT * " +
	    				"FROM Map " +
	    				"WHERE (`HostUuid`, `VmUuid` ) " +
	    				"NOT IN (" + HostOp.getAllMapString() + ")" );
	    System.out.println(ps.toString());
	    ps.execute();
	      
	    ps=sqlConn.prepareStatement("DELETE " +
	    		  "FROM Map " +
	    		  "WHERE (`HostUuid`, `VmUuid` ) " +
	    		  "IN ( SELECT * " +  
	    		  		"FROM deleteMap)");
	    System.out.println(ps.toString());
	    ps.execute();
	    
	    /*
	     * Insert or update any correspondance info
		 * 	TABLE: Map
		 */
	    
	    for (VM vm : VmOp.getVmList()){
		   	//Exclude snapshots and templates
			if (!vm.getIsASnapshot(xenConn) && !vm.getIsATemplate(xenConn)){
				ps = VmOp.getCorrespondanceQuery(vm);
				System.out.println(ps.toString());
				ps.execute();
		    }
		}
	}
	
	/*
	 * For each Host in the pool create and execute an INSERT or UPDATE query
	 */
	
	private static void updateHostInfo() throws SQLException{
		 /*
	     * Remove the no-longer existing Hosts:
	     *  - Create a temporary table (deleteHost)
	     *  - Insert in deleteMap all the old UUIDs that no longer exist
	     *  - Delete all those records in the main one
	     * 	TABLE: Vm
	     */
	    
	    ps = sqlConn.prepareStatement("CREATE TEMPORARY TABLE deleteHost (" +
									"HostUuid CHAR(36)" + 
									")");
	    System.out.println(ps.toString());
	    ps.execute();
	    
	    /*
	     * TODO: Find a better way to do this query in JDBC-style
	     * Parameter number it's a limit for PreparedStatement
	     */
	    ps = sqlConn.prepareStatement("INSERT INTO deleteHost " +
				"SELECT UUID " +
				"FROM Host " +
				"WHERE ( `UUID` ) " +
				"NOT IN (" + HostOp.getAllHostString() + ")" );
	    System.out.println(ps.toString());
	    ps.execute();
	    
	    ps=sqlConn.prepareStatement("DELETE " +
	    		  "FROM Host " +
	    		  "WHERE ( `UUID` ) " +
	    		  "IN ( SELECT * " +  
	    		  		"FROM deleteHost)");
	    System.out.println(ps.toString());
	    ps.execute();
		
	    /*
	     * Adds or updates an Host in the database
	     * 	Table: Host
	     */
	    for (Host hs : HostOp.getHostList()){
    		if(HostOp.isHostInDatabase(hs)){
    			/*
    			 * The host is already in the db : UPDATE
    			 */
    			ps = HostOp.getUpdateHostQuery(hs);
	    		System.out.println(ps);
	    		ps.execute();
    		} else {
    			/*
    			 * The host isn't in the db : INSERT
    			 */
	    		ps = HostOp.getInsertHostQuery(hs);
	    		System.out.println(ps);
	    		ps.execute();
    		}
	    }
	}
	
	/*
	 * For each Vm in the pool create and execute an INSERT or UPDATE query
	 */
	
	private static void updateVmInfo() throws SQLException, BadServerResponse, XenAPIException, XmlRpcException{
	    
		/*
	     * Remove the no-longer existing Vms:
	     *  - Create a temporary table (deleteVm)
	     *  - Insert in deleteMap all the old UUIDs that no longer exist
	     *  - Delete all those records in the main one
	     * 	TABLE: Vm
	     */
	    
	    ps = sqlConn.prepareStatement("CREATE TEMPORARY TABLE deleteVm (" +
									"VmUuid CHAR(36)" + 
									")");
	    System.out.println(ps.toString());
	    ps.execute();
	    
	    /*
	     * TODO: Find a better way to do this query in JDBC-style
	     * Parameter number it's a limit for PreparedStatement
	     */
	    ps = sqlConn.prepareStatement("INSERT INTO deleteVm " +
				"SELECT UUID " +
				"FROM Vm " +
				"WHERE ( `UUID` ) " +
				"NOT IN (" + VmOp.getAllVmString() + ")" );
	    System.out.println(ps.toString());
	    ps.execute();
	    
	    ps=sqlConn.prepareStatement("DELETE " +
	    		  "FROM Vm " +
	    		  "WHERE ( `UUID` ) " +
	    		  "IN ( SELECT * " +  
	    		  		"FROM deleteVm)");
	    System.out.println(ps.toString());
	    ps.execute();
	     
	    /*
	     * Insert or update any Vm's info
		 * 	TABLE: Vm
		 */
		for (VM vm : VmOp.getVmList()){
		   	//Exclude snapshots and templates
			if (!vm.getIsASnapshot(xenConn) && !vm.getIsATemplate(xenConn)){
		    		if(VmOp.isVmInDatabase(vm)){
		    			/*
		    			 * The Vm is already in the db : UPDATE
		    			 */
		    			ps = VmOp.getUpdateVMQuery(vm);
			    		System.out.println(ps);
			    		ps.execute();
		    		} else {
		    			/*
		    			 * The host is already in the db : INSERT
		    			 */
			    		ps = VmOp.getInsertVMQuery(vm);
			    		System.out.println(ps);
			    		ps.execute();
		    		}
		    }
		}
	}

	/*
	 * Print API Version
	 */
	
	private static void printApiVersion() {
		System.out.println("\nVersione Api: " + xenConn.getAPIVersion());		
	}

}

