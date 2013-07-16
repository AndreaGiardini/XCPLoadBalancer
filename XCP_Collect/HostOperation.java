package main;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;

import com.xensource.xenapi.Host;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.DataSource.Record;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;

public class HostOperation {
	/*
	 * TODO:
	 *  - Improve error checking
	 */
	private DecimalFormat df = new DecimalFormat("#0.00");
	private com.xensource.xenapi.Connection xenConn;
	private java.sql.Connection sqlConn;
	private Set<Host> HostList;
	
	public HostOperation( com.xensource.xenapi.Connection xenConn, java.sql.Connection sqlConn ) throws BadServerResponse, XenAPIException, XmlRpcException{
		this.xenConn=xenConn; this.sqlConn=sqlConn;
		HostList = Host.getAll(xenConn);
	}
	
	public Set<Host> getHostList(){
		return HostList;
	}
	
	/*
	 * Correspondance list in String format
	 * useful to put it in a Query string
	 */
	
	public String getAllMapString(){
		String ret = "";
		try{
			for(Host hs : getHostList()){
				for(VM vm : hs.getResidentVMs(xenConn)){
					//( 'eb966de6-276a-0428-2ecb-d1b2a69eff57' , '0a511f3b-ca5f-2c5d-7751-55b6a138f86e' ) , 
					ret=ret.concat("( '" + hs.getUuid(xenConn) + "' , '" + vm.getUuid(xenConn) + "' ) , ");
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return ret.substring(0, ret.length()-3);
	}
	
	/*
	 * Return a list of queries to populate the Map table
	 */
	
	public Set<PreparedStatement> getCorrespondanceQueries(Host hs){
		Set<PreparedStatement> returnSet = new HashSet<PreparedStatement>();
		try{
			for (VM vm : hs.getResidentVMs(xenConn)){
				PreparedStatement tmpStat = sqlConn.prepareStatement("REPLACE INTO Map VALUES(?,?)");
				tmpStat.setString(1, hs.getUuid(xenConn));
				tmpStat.setString(2, vm.getUuid(xenConn));
				returnSet.add(tmpStat);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return returnSet;
	}

	/*
	 * INSERT query for an Host
	 */
	
	public PreparedStatement getInsertHostQuery(Host hs){
		PreparedStatement addHostQuery=null;
	    try{
			addHostQuery = sqlConn.prepareStatement("INSERT INTO Host VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		      
	    	addHostQuery.setString(1, hs.getUuid(xenConn));
	    	addHostQuery.setInt(2, (int)Math.round(hs.queryDataSource(xenConn, "memory_total_kib")/1024));
	    	addHostQuery.setInt(3, (int)Math.round(hs.queryDataSource(xenConn, "memory_free_kib")/1024));
	    	addHostQuery.setInt(4, (int)Math.round(hs.queryDataSource(xenConn, "xapi_memory_usage_kib")/1024));
	    	addHostQuery.setInt(5, (int)Math.round(hs.queryDataSource(xenConn, "xapi_free_memory_kib")/1024));
	    	addHostQuery.setInt(6, (int)Math.round(hs.queryDataSource(xenConn, "pif_lo_rx")/1024));
	    	addHostQuery.setInt(7, (int)Math.round(hs.queryDataSource(xenConn, "pif_lo_tx")/1024));
	    	addHostQuery.setInt(8, (int)Math.round(hs.queryDataSource(xenConn, "pif_eth0_rx")/1024));
	    	addHostQuery.setInt(9, (int)Math.round(hs.queryDataSource(xenConn, "pif_eth0_tx")/1024));
	    	addHostQuery.setDouble(10, hs.queryDataSource(xenConn, "loadavg"));
	    	addHostQuery.setDouble(11, getHostAverageLoad(hs));
	    	addHostQuery.setString(12, hs.getAddress(xenConn));
	    	addHostQuery.setInt(13, hs.getHostCPUs(xenConn).size());
	    	addHostQuery.setString(14, hs.getNameLabel(xenConn));
	    } catch (Exception e){
	    	e.printStackTrace();
	    }
	      
	    return addHostQuery;
	}
	
	/*
	 * Host load average (%)
	 */
	
	public Double getHostAverageLoad(Host hs){
		try{
			int cpus = hs.getHostCPUs(xenConn).size();
			double tmp=0;
			for(int i=0; i<cpus; i++){
				tmp = tmp + hs.queryDataSource(xenConn, "cpu" + i);
			}
			return (double)(tmp*100/cpus);
		} catch (Exception e){
			e.printStackTrace();
			return 0.0;
		}
	}
	
	/*
	 * For each Host print his DataSource list
	 */
	
	public void printHostDataSources(){
		try {
			for (Host hs : HostList){
				Iterator<Record> iter;
				iter = hs.getDataSources(xenConn).iterator();
				
				System.out.println("\nHost Name : " + hs.getNameLabel(xenConn) +
						"\nUUID : " + hs.getUuid(xenConn) +
						"\nIP : " + hs.getAddress(xenConn));
				while(iter.hasNext()){
					Record rd = iter.next();
					try{
						System.out.println(rd.nameLabel + " - " + rd.nameDescription + " : " + hs.queryDataSource(xenConn, rd.nameLabel));
					} catch (Exception e){
						System.out.println("Unable to get : " + rd.nameLabel);
					}
				}
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	/*
	 * For each host print:
	 *  - Load of each core
	 *  - total Load average (%)
	 */
	
	public void printHostLoad(){
		try {
			for(Host hs : HostList){
				int cpus = hs.getHostCPUs(xenConn).size();
				System.out.println("\nNome: " + hs.getNameLabel(xenConn));
				double tmp=0;
				for(int i=0; i<cpus; i++){
					System.out.println("CPU" + i + ": " + hs.queryDataSource(xenConn, "cpu" + i));
					tmp = tmp + hs.queryDataSource(xenConn, "cpu" + i);
				}
				System.out.println("CPU AVERAGE: " + df.format(tmp*100/cpus) + "%");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * Checks if an Host is still present in the database
	 */
	
	public boolean isHostInDatabase(Host hs){
		PreparedStatement ps = null;
		ResultSet rs = null;
		try{
			ps = sqlConn.prepareStatement("SELECT EXISTS(select 1 from Host where UUID = ? )");
			ps.setString(1, hs.getUuid(xenConn));
			rs = ps.executeQuery();
			rs.first();
			if(rs.getInt(1)==1){
				return true;
			} else {
				return false;
			}
		} catch (Exception e){
			e.printStackTrace();
			return false;
		}

	}

	/*
	 * UPDATE query for an Host
	 */
	
	public PreparedStatement getUpdateHostQuery(Host hs) {
		PreparedStatement updateHostQuery=null;
		try{
			updateHostQuery = sqlConn.prepareStatement("UPDATE Host " +
					"SET MemoryTotalMb=?, " +
					"MemoryFreeMb=?, " +
					"XapiMemoryUsageMb=?, " +
					"XapiFreeMemoryMb=?, " +
					"PifLoRxKb=?, " +
					"PifLoTxKb=?, " +
					"PifEth0RxKb=?, " +
					"PifEth0TxKb=?, " +
					"Dom0LoadAvg=?, " +
					"CpuAverage=?, " +
					"Ip=?, " +
					"CpuNum=?, " +
					"Name=? " +
					"WHERE UUID=?");
			
			updateHostQuery.setInt(1, (int)Math.round(hs.queryDataSource(xenConn, "memory_total_kib")/1024));
			updateHostQuery.setInt(2, (int)Math.round(hs.queryDataSource(xenConn, "memory_free_kib")/1024));
			updateHostQuery.setInt(3, (int)Math.round(hs.queryDataSource(xenConn, "xapi_memory_usage_kib")/1024));
			updateHostQuery.setInt(4, (int)Math.round(hs.queryDataSource(xenConn, "xapi_free_memory_kib")/1024));
			updateHostQuery.setInt(5, (int)Math.round(hs.queryDataSource(xenConn, "pif_lo_rx")/1024));
			updateHostQuery.setInt(6, (int)Math.round(hs.queryDataSource(xenConn, "pif_lo_tx")/1024));
			updateHostQuery.setInt(7, (int)Math.round(hs.queryDataSource(xenConn, "pif_eth0_rx")/1024));
			updateHostQuery.setInt(8, (int)Math.round(hs.queryDataSource(xenConn, "pif_eth0_tx")/1024));
			updateHostQuery.setDouble(9, hs.queryDataSource(xenConn, "loadavg"));
			updateHostQuery.setDouble(10, getHostAverageLoad(hs));
			updateHostQuery.setString(11, hs.getAddress(xenConn));
			updateHostQuery.setInt(12, hs.getHostCPUs(xenConn).size());
			updateHostQuery.setString(13, hs.getNameLabel(xenConn));
			updateHostQuery.setString(14, hs.getUuid(xenConn));
		} catch (Exception e){
			e.printStackTrace();
		}
		return updateHostQuery;
	}

	/*
	 * Host list in String format
	 * useful to put it in a Query string
	 */
	
	public String getAllHostString() {
		String ret = "";
		try{
			for(Host hs : getHostList()){
				ret=ret.concat("( '" + hs.getUuid(xenConn) + "' ) , ");
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return ret.substring(0, ret.length()-3);
	}

}
