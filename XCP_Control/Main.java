package main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.xensource.xenapi.Host;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.VM;

public class Main {
	
	private static DecimalFormat df = new DecimalFormat("#0.00");
	private static java.sql.Connection sqlConn;
	private static com.xensource.xenapi.Connection xenConn;
	private static PreparedStatement ps;
	private static ResultSet rs;
	private static CorrespondenceList variables;

	public static void main(String[] args) {
		String XenHost = "";
		String XenUsr = "";
		String XenPass = "";
		String MysqlHost = "";
		String MysqlUsr = "";
		String MysqlPass = "";
		
		/*
		 * "variables" contains all the correspondences between
		 * Hosts values and Vms values
		 */
        variables = new CorrespondenceList();
        {
        	variables.add(new Correspondence("PifEth0RxKb", "Vif0RxKb"));
        	variables.add(new Correspondence("PifEth0TxKb", "Vif0TxKb"));
        	variables.add(new Correspondence("CpuAverage", "VcpuAverage"));
        	variables.add(new Correspondence("MemoryTotalMb-MemoryFreeMb", "MemoryMb-MemoryInternalFreeMb"));
        }
		
		/*
		 * Get settings file
		 */
		Properties configFile = new Properties();
		try {
			configFile.load(new FileInputStream(System.getProperty("user.dir") + "\\..\\settings"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		XenHost = configFile.getProperty("XenHost");
		XenUsr = configFile.getProperty("XenUsr");
		XenPass = configFile.getProperty("XenPass");
		MysqlHost = configFile.getProperty("MysqlHost");
		MysqlUsr = configFile.getProperty("MysqlUsr");
		MysqlPass = configFile.getProperty("MysqlPass");
		
		 
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
	    
	    /*
	     * Subject - Contains the object that we want to monitor
	     * Threshold - Contains the limit of that object
	     */
	    
	    String Subject;
		Double Threshold;
		PreparedStatement subPs;
		ResultSet subRs;
	    
	    try {
	    	/*
	    	 * Monitoring rules for Vms
	    	 * Example:
	    	 *  - Migrate a Vm if [OccupiedInternalMemoryMb/Vif0TxKb/Vif0RxKb/VcpuAverage] is more than X
	    	 *  
	    	 * Each row contains the subject in the first column and the threshold in the second one
	    	 * 
	    	 * Extract from the database all the vms rules
	    	 */
	    	System.out.println("ESECUZIONE REGOLE VM\n");
			ps=sqlConn.prepareStatement("SELECT * "
					+ "FROM VmRule");
			
			ps.execute();
			rs=ps.getResultSet();
			while(rs.next()){
				/*
				 * For each rule
				 */
				String result = "";
			
				Subject = rs.getString(1);
				Threshold = rs.getDouble(2);
				
				{
					/*
					 * Print rule on screen
					 */
					result=result.concat(" - When " + Subject + " is more than ");
					result=result.concat(df.parse(Threshold + "") + " migrate to the less loaded host");
					System.out.println(result);
				}
				
				/*
				 * Second query:
				 * Obtain UUID and Subject of the rule of each vm, grouped by decreasing Subject
				 */
				//100*(MemoryMb-MemoryInternalFreeMb)/MemoryMb
				String querySql = "SELECT UUID, " + Subject + " "
						+ "FROM Vm "
						+ "WHERE " + Subject + " IS NOT NULL "
						+ "GROUP BY " + Subject + " DESC";
				
				String VmUUID;
				subPs=sqlConn.prepareStatement(querySql);
				//System.out.println(subPs.toString());
				subPs.execute();
				subRs=subPs.getResultSet();
				
				while(subRs.next()){
					/*
					 * For each Vm check if it's breaking the rule or not
					 */
					
					VmUUID=subRs.getString(1);
					System.out.println("\nVmUUID: " + VmUUID + "\nSubject: " + Subject + "\nValue: " + subRs.getDouble(2) + "\nThreshold: " + Threshold);
					/*
					 * If the Vm value is above the threshold the Vm is breaking the rule -> migrate
					 */
					if(subRs.getDouble(2) > Threshold){
						//migrate
						System.out.println("CIAOOO - " + Subject);
						String HostUUID=getDestHostBy(variables.get(Subject));
						//Pool Connection
						Server srv = new Server(XenHost,XenUsr,XenPass);
						srv.connect();
						xenConn = srv.getConnection();
						
						Host hs = Host.getByUuid(xenConn, HostUUID);
						VM vm =VM.getByUuid(xenConn, VmUUID);
						
						System.out.println("Start Migration to host " + HostUUID);
//						Task tk = vm.poolMigrateAsync(xenConn, hs, new HashMap<String,String>());
//						while(tk.getProgress(xenConn) != 1){
//							System.out.println("Progress: " + tk.getProgress(xenConn));
//							Thread.sleep(2000);
//						}
						System.out.println("End Migration");
						
						srv.getSession().logout(xenConn);
					}		
				}				
			}
			
			/*
	    	 * Monitoring rules for Hosts
	    	 * Example:
	    	 *  - Migrate a Vm if the [PifEth0RxKb/PifEth0TxKb/OccupiedMemoryMb/CpuAverage] is X more the average
	    	 *  
	    	 * Each row contains the subject in the first column and the threshold in the second one
	    	 * 
	    	 * Extract from the database all the hosts rules
	    	 */
			
			System.out.println("\n\nESECUZIONE REGOLE HOST\n");
			
			ps=sqlConn.prepareStatement("SELECT * "
					+ "FROM HostRule");
			
			ps.execute();
			rs=ps.getResultSet();
			while(rs.next()){
				/*
				 * For each rule
				 */
				String measure = "";
				if(rs.getString(1).equals("PifEth0RxKb") || rs.getString(1).equals("PifEth0TxKb")){
					measure="Kb/s";
				} else if (rs.getString(1).equals("MemoryFreeMb")){
					measure="MB";
				} else if (rs.getString(1).equals("CpuAverage")){
					measure="%";
				}
				{
					/*
					 * Print rule on screen
					 */
					System.out.print("\n\t - " + rs.getString(1) + " is more than ");
					System.out.print(df.parse(rs.getString(2)) + " " + measure + "  above the pool medium\n");
				}
				Subject=rs.getString(1);
				Threshold=rs.getDouble(2);
				double poolMedium = getPoolMedium(Subject);
				
				/*
				 * Second query:
				 * Obtain UUID and Subject of the rule of each Host, grouped by decreasing Subject
				 * 
				 * For the host rules we have a different logic pool-wide:
				 *  - Find the average value of Subject pool-wide, querying each host
				 *  - Check if each host is above the average or not
				 *  - If it's above the threshold migrate the vm to balance the pool
				 */
				
				subPs=sqlConn.prepareStatement("SELECT UUID, " + Subject + " "
						+ "FROM Host");
				subPs.execute();
				subRs=subPs.getResultSet();
				while (subRs.next()){
					String HostUUID = subRs.getString(1);
					double hostSubject = subRs.getDouble(2);
					
					/*
					 * Print on video:
					 *  - Host's UUID
					 *  - Monitoring subject (Cpu, ram, ecc)
					 *  - Rules threshold
					 *  - Average load of the monitored object pool-wide
					 *  - Object value of the single host
					 *  
					 *  If the difference between the value of the monitored subjet and the average pool values
					 *  goes over the rule's threshold migrate to the less loaded host
					 * 
					 */
					System.out.println("\nHostUUID: " + HostUUID);
					System.out.println("Subject: " + Subject);
					System.out.println("Threshold: " + Threshold);
					System.out.println("poolMedium: " + poolMedium);
					System.out.println("hostSubject: " + hostSubject);
					System.out.println("hostSubject-hostMedium: " + (hostSubject-poolMedium));
					System.out.println("");
					
					
					if(hostSubject-poolMedium > Threshold){
						//migrazione
						System.out.println("MIGRAZIONE");
						System.out.println("Host destinatario: " + getLessLoadedHostBy(Subject));
						System.out.println("Vm sorgente: " + getMostLoadedVmBy(variables.get(Subject)));
					}
				}
				
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    

	}

	private static String getMostLoadedVmBy(String subject) {
		PreparedStatement ps;
		ResultSet rs;
		String ret;
		double tmpVal = 0;
		
		try {
			ps=sqlConn.prepareStatement("SELECT UUID, " + subject + " "
					+ "FROM Vm "
					+ "GROUP BY " + subject + " DESC" );
			ps.execute();
			rs=ps.getResultSet();
			
			while(rs.next()){
				return rs.getString(1);
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
		
		return null;
	}

	/*
	 * Devo ritornare l'UUID dell'host più sotto la media per subject
	 */
	private static String getLessLoadedHostBy(String subject){
		PreparedStatement ps;
		ResultSet rs;
		double medium = getPoolMedium(subject);
		String ret = "";
		double tmpVal = 0;
		
		try {
			ps=sqlConn.prepareStatement("SELECT UUID, " + subject + " "
					+ "FROM Host");
			ps.execute();
			rs=ps.getResultSet();
			while(rs.next()){
				
				if(ret.equals("")){
					ret=rs.getString(1);
					tmpVal=rs.getDouble(2)-medium;
				}else{
					if(rs.getDouble(2)-medium < tmpVal){
						ret=rs.getString(1);
						tmpVal=rs.getDouble(2)-medium;
					}
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ret;
	}
	
	private static double getPoolMedium(String subject) {
		PreparedStatement ps;
		ResultSet rs;
		
		try {
			ps=sqlConn.prepareStatement("SELECT AVG(" + subject + ") " 
					+ "FROM Host");
			ps.execute();
			rs=ps.getResultSet();
			rs.next();
			return rs.getDouble(1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return 0;
	}
	
	private static String getDestHostBy(String string) {
		
		/*
		 * Returns the UUID of the most unloaded host for the string parameter
		 */
		
		PreparedStatement ps;
		ResultSet rs;
		System.out.println("LOL - " + string);
		
		String queryString = "SELECT UUID, " + string + " FROM Host GROUP BY " + string;
		
		try {
			ps=sqlConn.prepareStatement(queryString);
			ps.execute();
			rs=ps.getResultSet();
			rs.next();
			return rs.getString(1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		return null;
	}

}
