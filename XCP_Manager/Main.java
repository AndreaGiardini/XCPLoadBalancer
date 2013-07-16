package main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


public class Main
{
	
	private static DecimalFormat df = new DecimalFormat("#0.00");
	private static java.sql.Connection sqlConn;
	private static PreparedStatement ps;
	private static ResultSet rs;
	private static CorrespondenceList variables;
	private static BufferedReader console;

	public static void main(String[] args) throws java.io.IOException {

		String MysqlHost = "";
		String MysqlUsr = "";
		String MysqlPass = "";
        String commandLine, words[];
        console = new BufferedReader(new InputStreamReader(System.in));
        boolean loop = true;
        variables = new CorrespondenceList();
        {
        	variables.add(new Correspondence("PifEth0RxKb", "Vif0RxKb"));
        	variables.add(new Correspondence("PifEth0TxKb", "Vif0TxKb"));
        	variables.add(new Correspondence("CpuAverage", "VcpuAverage"));
        	variables.add(new Correspondence("OccupiedMemoryMb", "OccupiedInternalMemoryMb"));
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
		
		MysqlHost = configFile.getProperty("MysqlHost");
		MysqlUsr = configFile.getProperty("MysqlUsr");
		MysqlPass = configFile.getProperty("MysqlPass");
		
	    String url = "jdbc:mysql://" + MysqlHost + ":3306/";
	    String dbName = "Xen";
	    String driver = "com.mysql.jdbc.Driver";
	    try {
	    	Class.forName(driver).newInstance();
	    	sqlConn = DriverManager.getConnection(url+dbName,MysqlUsr,MysqlPass);
	    } catch (Exception e) {
	    	e.printStackTrace();
		}

        while (loop) {
            //read the command
            System.out.print("\nXCP_Manager> ");
            commandLine = console.readLine();
            words = commandLine.split(" ");
            
            switch(words[0].toUpperCase()){
            	case "CREATE":
            		
            		switch(words[1].toUpperCase()){
            			case "RULE":
            				if(words.length == 3){
	                       		switch(words[2].toUpperCase()){
	                			case "VM":
	                				createVmRule();
	                				break;
	                			case "HOST":
	                				createHostRule();
	                				break;
	                			default:
	                				break;
	                       		}
            				} else {
            					System.out.println("Usage : CREATE RULE [HOST/VM]");
            				}
            				break;
            			default:
            				System.out.println("Command not recognized");
            				break;
            		}
            		break;
            	case "DELETE":
            		switch(words[1].toUpperCase()){
        			case "RULE":
        				if(words.length == 3){
                       		switch(words[2].toUpperCase()){
                			case "VM":
                				deleteVmRule();
                				break;
                			case "HOST":
                				deleteHostRule();
                				break;
                			default:
                				break;
                       		}
        				} else {
        					System.out.println("Usage : CREATE RULE [HOST/VM]");
        				}
        				break;
        			default:
        				System.out.println("Command not recognized");
        				break;
        		}
        		break;
            	case "SHOW":
            		
            		switch(words[1].toUpperCase()){
            			case "RULES":
            				//SHOW RULES
            				if(words.length == 3){
	            				switch(words[2].toUpperCase()){
	                			case "VM":
	                				showVmRules();
	                				break;
	                			case "HOST":
	                				showHostRules();
	                				break;
	                			default:
	                				System.out.println("Command not recognized");
	                				break;
	                       		}
            				} else {
            					System.out.println("Usage : SHOW RULES [HOST/VM]");
            				}
            				break;
            			case "VMS":
            				//SHOW VMS
            				showVMs();
            				break;
            			case "HOSTS":
            				//SHOW HOSTS
            				showHosts();
            				break;
            			case "VM":
            				//SHOW VM
            				if(words.length == 3){
            					showVM( words[2] );
            				}else{
            					System.out.println("Usage : SHOW VM $Vm_UUID");
            				}
            				break;
            			case "HOST":
            				//SHOW HOST
            				
            				if(words.length == 3){
            					showHost( words[2] );
            				}else{
            					System.out.println("Usage : SHOW HOST $Host_UUID");
            				}
            				break;
            			default:
            				System.out.println("Command not recognized");
            				break;
            		}
            		break;
            	case "QUIT":
            		//QUIT
            		System.out.println("\nExiting...");
            		loop=false;
            		break;            		
            	default:
            		break;
            }
            
        }
        
        try {
			sqlConn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
        System.exit(0);

    }


	private static void deleteHostRule() {
		// TODO Auto-generated method stub
		try {
			ps=sqlConn.prepareStatement("SELECT * " +
					"FROM HostRule ");
				
			ps.execute();
			rs=ps.getResultSet();
			//[PifEth0RxKb/PifEth0TxKb/MemoryFreeMb/CpuAverage]
			System.out.println("\n\tRules - Balance host when:");
			String Subject, orgSubject;
			double Threshold;
			while(rs.next()){
				String measure = "";
				Subject = rs.getString(1);
				orgSubject = Subject;
				Threshold = rs.getDouble(2);
				
				if(Subject.equals("MemoryTotalMb-MemoryFreeMb")){
					Subject="OccupiedMemoryMb";
				}
				if(Subject.equals("PifEth0RxKb") || Subject.equals("PifEth0TxKb")){
					measure="Kb/s";
				} else if (Subject.equals("OccupiedMemoryMb")){
					measure="MB";
				} else if (Subject.equals("CpuAverage")){
					measure="%";
				}
				System.out.print("\n\t - " + Subject + " is more than ");
				System.out.print(df.parse(Threshold + "") + " " + measure + " above the medium");
				System.out.println("\n\t Do you want to delete this rule? [Y/N] ");
				if(console.readLine().trim().toUpperCase().equals("Y")){
					PreparedStatement subPs = sqlConn.prepareStatement("DELETE FROM HostRule "
							+"WHERE Subject = \"" + orgSubject + "\" AND Threshold = " + Threshold);
					subPs.execute();
					System.out.println("Rule deleted");
				}
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}


	private static void deleteVmRule() {
		// TODO Auto-generated method stub
		
		try {
			ps=sqlConn.prepareStatement("SELECT * " +
					"FROM VmRule ");
				
			ps.execute();
			rs=ps.getResultSet();
			String Subject, orgSubject;
			double Threshold;
			
			System.out.println("\n\tRules - Migrate Vm when:");
			while(rs.next()){
				Subject = rs.getString(1);
				orgSubject = Subject;
				Threshold = rs.getDouble(2);
				String measure = "";
				
				if(rs.getString(1).equals("Vif0TxKb") || rs.getString(1).equals("Vif0RxKb")){
					measure="Kb/s";
				} else {
					measure="%";
				}
				System.out.print("\n\t - " + Subject + " is more than ");
				System.out.print(df.parse(Threshold + "") + " " + measure + " to the less loaded Host");
				System.out.println("\n\t Do you want to delete this rule? [Y/N] ");
				if(console.readLine().trim().toUpperCase().equals("Y")){
					PreparedStatement subPs = sqlConn.prepareStatement("DELETE FROM VmRule "
							+"WHERE Subject = \"" + orgSubject + "\" AND Threshold = " + Threshold);
					subPs.execute();
					System.out.println("Rule deleted");
				}
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}


	private static void showHostRules() {
		// TODO Auto-generated method stub
		
		try {
			ps=sqlConn.prepareStatement("SELECT * " +
					"FROM HostRule ");
				
			ps.execute();
			rs=ps.getResultSet();
			//[PifEth0RxKb/PifEth0TxKb/MemoryFreeMb/CpuAverage]
			System.out.println("\n\tRules - Balance host when:");
			String Subject;
			double Threshold;
			while(rs.next()){
				String measure = "";
				Subject = rs.getString(1);
				Threshold = rs.getDouble(2);
				
				if(Subject.equals("MemoryTotalMb-MemoryFreeMb")){
					Subject="OccupiedMemoryMb";
				}
				if(Subject.equals("PifEth0RxKb") || Subject.equals("PifEth0TxKb")){
					measure="Kb/s";
				} else if (Subject.equals("OccupiedMemoryMb")){
					measure="MB";
				} else if (Subject.equals("CpuAverage")){
					measure="%";
				}
				System.out.print("\n\t - " + Subject + " is more than ");
				System.out.print(df.parse(Threshold + "") + " " + measure + " above the medium");
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}


	private static void createHostRule() {
		try {
			/*
			 * Starts the wizard to create a new Host rule
			 */
			
			String Subject;
			double Threshold;
			String tmp;
			
			System.out.println("\nAdd a new Host Rule (Pool wide):");
			/*
			 * Looking for the Subject
			 */
			
			//MemoryFreeMb
			System.out.println("\nWhen [PifEth0RxKb/PifEth0TxKb/OccupiedMemoryMb/CpuAverage]: ");
			Subject=console.readLine().trim();
			while(variables.get(Subject) == null){
				System.out.println("Errore");
				System.out.println("When [PifEth0RxKb/PifEth0TxKb/OccupiedMemoryMb/CpuAverage]: ");
				Subject=console.readLine().toUpperCase().trim();
			}
			
			/*
			 * Looking for the Threshold
			 */
			String measure = "";
			if(Subject.equals("PifEth0RxKb") || Subject.equals("PifEth0TxKb")){
				measure="Kb/s";
			} else if (Subject.equals("OccupiedMemoryMb")){
				measure="MB";
			} else if (Subject.equals("CpuAverage")){
				measure="%";
			}
			System.out.println("Is more than [" + measure + "]: ");
			while(true){
				try{
					tmp=console.readLine().trim();
					Threshold=Double.parseDouble(tmp);
					break;
				} catch (NumberFormatException e){
					System.out.println("Error");
					System.out.println("Is more than [" + measure + "]: ");
				}
			}

			System.out.println("above the medium migrate vms to the less loaded host ");

			/*
			 * Final Review
			 * 	String Subject;
			 *	double Threshold;
			 */
			String result = "";
			//when [PifEth0RxKb/PifEth0TxKb/OccupiedMemoryMb/CpuAverage] is MORE than X migrate to the LESS loaded host
			result=result.concat("\nWhen " + Subject + " is more than ");
			result=result.concat(df.parse(Threshold + "") + " " + measure + " above the medium migrate to the less loaded host");
			System.out.println(result);
			System.out.println("Confirm ? [Y/N] ");
			tmp=console.readLine().toUpperCase().trim();
			while(!tmp.equals("Y") && !tmp.equals("N")){
				System.out.println("Error");
				System.out.println("Confirm ? [Y/N] ");
				tmp=console.readLine().toUpperCase().trim();
			}
			
			if(tmp.equals("Y")){
				//Salva regola nel DB
				ps=sqlConn.prepareStatement("INSERT INTO HostRule "
						+ "VALUES(?,?)" );
				if(Subject.equals("OccupiedMemoryMb")){
					ps.setString(1, "MemoryTotalMb-MemoryFreeMb");
				} else {
					ps.setString(1, Subject);
				}
				ps.setDouble(2, Threshold);
				
				ps.execute();
				
			} else {
				System.out.println("Operation aborted");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	private static void createVmRule() {
		try {
			/*
			 * Starts the wizard to create a new rule
			 */
			
			String Subject;
			double Threshold;
			String tmp;
			
			System.out.println("\nAdd a new Vm Rule:");
			/*
			 * Looking for the Subjext
			 */
			System.out.println("\nWhen [OccupiedInternalMemoryMb/Vif0TxKb/Vif0RxKb/VcpuAverage]: ");
			Subject=console.readLine().trim();
			while(variables.get(Subject) == null){
				System.out.println("Errore");
				System.out.println("When [OccupiedInternalMemoryMb/Vif0TxKb/Vif0RxKb/VcpuAverage]: ");
				Subject=console.readLine().toUpperCase().trim();
			}
			
			/*
			 * Looking for the Threshold
			 */
			String measure = "";
			if(Subject.equals("Vif0TxKb") || Subject.equals("Vif0RxKb")){
				measure="Kb/s";
			} else {
				measure="%";
			}
			System.out.println("Is more than [" + measure + "]: ");
			while(true){
				try{
					tmp=console.readLine().trim();
					Threshold=Double.parseDouble(tmp);
					break;
				} catch (NumberFormatException e){
					System.out.println("Errore");
					System.out.println("Is more than [" + measure + "]: ");
				}
			}

			System.out.println("Migrate to the less loaded host ");

			if(Subject.equals("OccupiedInternalMemoryMb")){
				Subject="100*(MemoryMb-MemoryInternalFreeMb)/MemoryMb";
			}
				
			
			/*
			 * Final Review
			 * 	String Subject;
			 *	double Threshold;
			 */
			String result = "";
			//when [OccupiedInternalMemoryMb/Vif0TxKb/Vif0RxKb/VcpuAverage] is MORE than X migrate to the LESS loaded host
			result=result.concat("\nWhen " + Subject + " is more than ");
			result=result.concat(df.parse(Threshold + "") + " " + measure + " migrate to the less loaded host");
			System.out.println(result);
			System.out.println("Confirm ? [Y/N] ");
			tmp=console.readLine().toUpperCase().trim();
			while(!tmp.equals("Y") && !tmp.equals("N")){
				System.out.println("Errore");
				System.out.println("Confirm ? [Y/N] ");
				tmp=console.readLine().toUpperCase().trim();
			}
			
			if(tmp.equals("Y")){
				//Salva regola nel DB
				ps=sqlConn.prepareStatement("INSERT INTO VmRule "
						+ "VALUES(?,?)" );
				ps.setString(1, Subject);
				ps.setDouble(2, Threshold);
				
				ps.execute();
				
			} else {
				System.out.println("Operation aborted");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private static void showVmRules() {
		
		try {
			ps=sqlConn.prepareStatement("SELECT * " +
					"FROM VmRule ");
				
			ps.execute();
			rs=ps.getResultSet();
			
			System.out.println("\n\tRules - Migrate Vm when:");
			while(rs.next()){
				String measure = "";
				if(rs.getString(1).equals("Vif0TxKb") || rs.getString(1).equals("Vif0RxKb")){
					measure="Kb/s";
				} else if (rs.getString(1).equals("MemoryInternalFreeMb")){
					measure="MB";
				} else if (rs.getString(1).equals("VcpuAverage")){
					measure="%";
				}
				System.out.print("\n\t - " + rs.getString(1) + " is more than ");
				System.out.print(df.parse(rs.getString(2)) + " " + measure + " to the less loaded Host");
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}


	private static void showHost(String UUID) {
		
		try {
			ps=sqlConn.prepareStatement("SELECT * " +
					"FROM Host " +
					"WHERE UUID=?");
			ps.setString(1, UUID);
				
			ps.execute();
			rs=ps.getResultSet();
			
			while(rs.next()){
//			    UUID CHAR(36) PRIMARY KEY, --Host UUID
//				MemoryTotalMb INT, --Total Physical Memory (MB)
//				MemoryFreeMb INT, --Total Free Memory (MB)
//				XapiMemoryUsageMb INT, --Memory allocated by the Xapi daemon (MB)
//				XapiFreeMemoryMb INT, --Free Momory for the Xapi daemon (MB)
//				PifLoRxKb INT, --Received data on the physic interface lo (Kb/s)
//				PifLoTxKb INT, --Transmitted data on the physic interface lo (Kb/s)
//				PifEth0RxKb INT, --Received data on the physic interface eth0 (Kb/s)
//				PifEth0TxKb INT, --Transmitted data on the physic interface eth0 (Kb/s)
//				Dom0LoadAvg DOUBLE, --LoadAvg of Dom0
//				CpuAverage DOUBLE, --Cpu Average (%)
//				Ip VARCHAR(15), --Host Ip
//				CpuNum INT, --Cpus number
//				Name VARCHAR(100) --Hostname
				System.out.println("\n\tUUID - " + rs.getString(1));
				System.out.println("\tTotal Memory (MB) - " + rs.getInt(2));
				if(rs.getString(8).equals("Halted")){
					System.out.println("\tFree Memory (MB) - Unavailable");
					System.out.println("\tXapi Memory Usage (MB) - Unavailable");
					System.out.println("\tXapi Free Memory (MB) - Unavailable");
					System.out.println("\tReceived data on the Physical Interface Lo (Kb/s) - Unavailable");
					System.out.println("\tTransmitted data on the Physical Interface Lo (Kb/s) - Unavailable");
					System.out.println("\tReceived data on the Physical Interface Eth0 (Kb/s) - Unavailable");
					System.out.println("\tTransmitted data on the Physical Interface Eth0 (Kb/s) - Unavailable");
					System.out.println("\tDom0 Load Average - Unavailable");
					System.out.println("\tCpu Load Average - Unavailable");
					System.out.println("\tIp - Unavailable");
				} else {
					System.out.println("\tFree Memory (MB) - " + rs.getInt(3));
					System.out.println("\tXapi Memory Usage (MB) - " + rs.getInt(4));
					System.out.println("\tXapi Free Memory (MB) - " + rs.getInt(5));
					System.out.println("\tReceived data on the Physical Interface Lo (Kb/s) - " + rs.getInt(6));
					System.out.println("\tTransmitted data on the Physical Interface Lo (Kb/s) - " + rs.getInt(7));
					System.out.println("\tReceived data on the Physical Interface Eth0 (Kb/s) - " + rs.getInt(8));
					System.out.println("\tTransmitted data on the Physical Interface Eth0 (Kb/s) - " + rs.getInt(9));
					System.out.println("\tDom0 Load Average - " + df.parse(rs.getString(10)) + "%");
					System.out.println("\tCpu Load Average - " + df.parse(rs.getString(11)) + "%");
					System.out.println("\tIp - " + rs.getString(12));
				}
				System.out.println("\tCpu Number - " + rs.getInt(13));
				System.out.println("\tName - " + rs.getString(14));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void showHosts() {
		// TODO Auto-generated method stub
		try {
			ps=sqlConn.prepareStatement("SELECT * " +
										"FROM Host");
			ps.execute();
			rs=ps.getResultSet();
			
			System.out.println("");
			while(rs.next()){
				System.out.println("\t - " + rs.getString(1) + " - " + rs.getString(14));
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void showVM(String UUID) {
		
//		UUID CHAR(36) PRIMARY KEY, --Vm UUID
//		MemoryMb INT, --Total Virtual Memory (MB)
//		MemoryInternalFreeMb INT, --Free Virtual Memory (MB)
//		Vif0TxKb INT, --Transmitted data on the virtual interface (Kb/s)
//		Vif0RxKb INT, --Received datas on the virtual interface (Kb/s)
//		VcpuAverage DOUBLE, --VCpu Average (%)
//		VcpuNum INT, --VCpus number
//		PowerState VARCHAR(20), --Vm's Power State
//		IsControlDomain BOOLEAN, --Check if it's a control domain
//		Name VARCHAR(100) --Hostname
		
		try {
			ps=sqlConn.prepareStatement("SELECT * " +
					"FROM Vm " +
					"WHERE UUID=?");
			ps.setString(1, UUID);
				
			ps.execute();
			rs=ps.getResultSet();
			
			while(rs.next()){
				System.out.println("\n\tUUID - " + rs.getString(1));
				System.out.println("\tTotal Memory (MB) - " + rs.getInt(2));
				if(rs.getString(8).equals("Halted")){
					System.out.println("\tFree Memory (MB) - Unavailable");
					System.out.println("\tTransmitted data on the Virtual Interface (Kb/s)  - Unavailable");
					System.out.println("\tReceived data on the Virtual Interface (Kb/s) - Unavailable");
					System.out.println("\tVCpu Average - Unavailable");
				} else {
					System.out.println("\tFree Memory (MB) - " + rs.getInt(3));
					System.out.println("\tTransmitted data on the Virtual Interface (Kb/s)  - " + rs.getInt(4));
					System.out.println("\tReceived data on the Virtual Interface (Kb/s) - " + rs.getInt(5));
					System.out.println("\tVCpu Average - " + df.parse(rs.getString(6)) + "%");
				}
				System.out.println("\tVCpu Number - " + rs.getInt(7));
				System.out.println("\tPower State - " + rs.getString(8));
				System.out.println("\tControl Domain - " + rs.getBoolean(9));
				System.out.println("\tName - " + rs.getString(10));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private static void showVMs() {
		try {
			ps=sqlConn.prepareStatement("SELECT * " +
										"FROM Vm");
			ps.execute();
			rs=ps.getResultSet();
			
			System.out.println("");
			while(rs.next()){
				System.out.println("\t - " + rs.getString(1) + " - " + rs.getString(10));
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
