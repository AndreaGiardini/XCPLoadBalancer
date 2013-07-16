package main;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;

import com.xensource.xenapi.DataSource.Record;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VmPowerState;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;

public class VmOperation {
	/*
	 * TODO:
	 *  - Improve error checking
	 */
	private DecimalFormat df = new DecimalFormat("#0.00");
	private com.xensource.xenapi.Connection xenConn;
	private java.sql.Connection sqlConn;
	private Set<VM> VmList;
	
	public VmOperation( com.xensource.xenapi.Connection xenConn, java.sql.Connection sqlConn ) throws BadServerResponse, XenAPIException, XmlRpcException{
		this.xenConn=xenConn; this.sqlConn=sqlConn;
		VmList = VM.getAll(xenConn);
	}
	
	public Set<VM> getVmList(){
		return VmList;
	}
	
	/*
	 * Checks if the VM is already in the database
	 * If true the VM is in the database
	 * If false the VM is not in the database
	 * @param	vm	the VM object
	 * @return		boolean value
	 */
	
	public boolean isVmInDatabase(VM vm){
		PreparedStatement ps = null;
		ResultSet rs = null;
		try{
			ps = sqlConn.prepareStatement("SELECT EXISTS(select * from Vm where UUID = ? )");
			ps.setString(1, vm.getUuid(xenConn));
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
	 * Returns the correspondent INSERT query for the selected VM
	 * @param	vm	the VM object
	 * @return		corresponding PreparedStatement
	 */
	
	public PreparedStatement getInsertVMQuery(VM vm){
		PreparedStatement addVMQuery=null;
		try{
			addVMQuery = sqlConn.prepareStatement("INSERT INTO Vm VALUES(?,?,?,?,?,?,?,?,?,?)");
			if(vm.getPowerState(xenConn).toString().equals("Running")){
				addVMQuery.setString(1, vm.getUuid(xenConn));
				addVMQuery.setInt(2, (int)Math.round(vm.queryDataSource(xenConn, "memory")/1024/1024));
				/*
				 * Control Domains don't have some parameters
				 * Some of their arguments are null
				 */
				if(!vm.getIsControlDomain(xenConn)){
					addVMQuery.setInt(3, (int)Math.round(vm.queryDataSource(xenConn, "memory_internal_free")/1024));
					addVMQuery.setInt(4, (int)Math.round(vm.queryDataSource(xenConn, "vif_0_tx")/1024));
					addVMQuery.setInt(5, (int)Math.round(vm.queryDataSource(xenConn, "vif_0_rx")/1024));
				} else {
					addVMQuery.setNull(3, Types.INTEGER);
					addVMQuery.setNull(4, Types.INTEGER);
					addVMQuery.setNull(5, Types.INTEGER);
				}
				addVMQuery.setDouble(6, getVMAverageLoad(vm));
				addVMQuery.setInt(7, (int)Math.round(vm.getMetrics(xenConn).getVCPUsNumber(xenConn)));
				addVMQuery.setString(8, vm.getPowerState(xenConn).toString());
				addVMQuery.setBoolean(9, vm.getIsControlDomain(xenConn));
				addVMQuery.setString(10, vm.getNameLabel(xenConn));
			}else {
				addVMQuery.setString(1, vm.getUuid(xenConn));
				addVMQuery.setInt(2, (int)Math.round(vm.getMemoryDynamicMax(xenConn)/1024/1024));
				addVMQuery.setNull(3, Types.INTEGER);
				addVMQuery.setNull(4, Types.INTEGER);
				addVMQuery.setNull(5, Types.INTEGER);
				addVMQuery.setNull(6, Types.INTEGER);
				addVMQuery.setInt(7, (int)Math.round(vm.getVCPUsMax(xenConn)));
				addVMQuery.setString(8, vm.getPowerState(xenConn).toString());
				addVMQuery.setBoolean(9, vm.getIsControlDomain(xenConn));
				addVMQuery.setString(10, vm.getNameLabel(xenConn));
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return addVMQuery;
	}
	
	/*
	 * Return VM's Load average in percentage (%)
	 * @param	vm	the VM object
	 * @return		load average in percentage
	 */
	
	public Double getVMAverageLoad(VM vm){
		try{
			long cpus = vm.getMetrics(xenConn).getVCPUsNumber(xenConn);
			double tmp=0;
			for(int i=0; i<cpus; i++){
				tmp = tmp + vm.queryDataSource(xenConn, "cpu" + i);
			}
			return (double)(tmp*100/cpus);
		} catch (Exception e){
			e.printStackTrace();
			return 0.0;
		}
	}
	
	/*
	 * For each Vm is prints all his DataSource list
	 * This method doesn't return anything, it only prints informations
	 */
	
	public void printVmDataSources(){
		try{
			for (VM vm : VmList){
				if (!vm.getIsASnapshot(xenConn) && !vm.getIsATemplate(xenConn)&& vm.getPowerState(xenConn).equals(VmPowerState.RUNNING)){
					Iterator<Record> iter = vm.getDataSources(xenConn).iterator();
					System.out.println("\nVM Name : " + vm.getNameLabel(xenConn));
					while(iter.hasNext()){
						Record rd = iter.next();
						if(rd.enabled){
							System.out.println(rd.nameLabel + " - " + rd.nameDescription + " : " + vm.queryDataSource(xenConn, rd.nameLabel));
						}
					}
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/*
	 * For each Vm prints:
	 *  - Load of each VCPU Core
	 *  - Load Average (%)
	 */
	
	public void printVmLoad(){
		try {
			for(VM vm : VmList){
				if (!vm.getIsASnapshot(xenConn) && !vm.getIsATemplate(xenConn) && !vm.getIsControlDomain(xenConn) && vm.getPowerState(xenConn).equals(VmPowerState.RUNNING)){
					Long vcpu = vm.getMetrics(xenConn).getVCPUsNumber(xenConn);
					System.out.println("\nNome: " + vm.getNameLabel(xenConn));
					double tmp=0;
					for(int i=0; i<vcpu; i++){
						System.out.println("CPU" + i + ": " + vm.queryDataSource(xenConn, "cpu" + i));
						tmp = tmp + vm.queryDataSource(xenConn, "cpu" + i);
					}
					System.out.println("CPU AVERAGE: " + df.format(tmp*100/vcpu) + "%");
					
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * Print the Vms list reporting some useful informations
	 * about Disks, Networks, ecc
	 */
	
	public void printVmInformation() {
		try {
			System.out.println("\nInformazioni VM:");
			for(VM vm : VmList){
				
				if (!vm.getIsASnapshot(xenConn) && !vm.getIsATemplate(xenConn) && !vm.getIsControlDomain(xenConn)){
					System.out.println("\n - Nome: " + vm.getNameLabel(xenConn) +
							"\n\tUUID: " + vm.getUuid(xenConn) +
							"\n\tDescrizione: " + vm.getNameDescription(xenConn) + 
							"\n\tMaxMemory: " + vm.getMemoryDynamicMax(xenConn) +
							"\n\tMinMemory: " + vm.getMemoryDynamicMin(xenConn) +
							"\n\tStartupVCPU: " + vm.getVCPUsAtStartup(xenConn) +
							"\n\tMaxVCPU: " + vm.getVCPUsMax(xenConn) +
							"\n\tPower State: " + vm.getPowerState(xenConn));
							
					System.out.println("\n\tVBDs (Disks Connections): ");
					for(VBD diskConn : vm.getVBDs(xenConn)){
						VDI disk = diskConn.getVDI(xenConn);
						System.out.println("\n\t\t - UUID: " + diskConn.getUuid(xenConn) +
								"\n\t\t - Attached: " + diskConn.getCurrentlyAttached(xenConn));
						if(diskConn.getCurrentlyAttached(xenConn)){
							System.out.println("\t\t\t - VDI UUID: " + disk.getUuid(xenConn) +
									"\n\t\t\t - Nome: " + disk.getNameLabel(xenConn) +
									"\n\t\t\t - Description: " + disk.getNameDescription(xenConn) +
									"\n\t\t\t - Size: " + disk.getVirtualSize(xenConn) + 
									"\n\t\t\t - Stored on: " + disk.getSR(xenConn).getNameLabel(xenConn));
						}
					}

					System.out.println("\n\tVIFs (Networks): ");
					for(VIF netConn : vm.getVIFs(xenConn)){
						System.out.println( "\n\t\t - Device: " + netConn.getUuid(xenConn) + 
								"\n\t\t - MAC: " + netConn.getMAC(xenConn) + 
								"\n\t\t - MTU: " + netConn.getMTU(xenConn));				
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Impossibile ottenere la lista template");
		}
		
	}
	
	/*
	 * Print the name list of the Vm
	 */
	
	public void printVmList() {
		try {
			System.out.println("\nLista VM:");
			for(VM vm : VmList){
				if (!vm.getIsASnapshot(xenConn) && !vm.getIsATemplate(xenConn) && !vm.getIsControlDomain(xenConn)){
					System.out.println(" - " + vm.getNameLabel(xenConn));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Impossibile ottenere la lista template");
		}	
		
	}
	
	/*
	 * Print the controllers domain name
	 */
	
	public void printControlDomainList() {
		try {
			System.out.println("\nLista Control Domain:");
			for(VM vm : VmList){
				if (vm.getIsControlDomain(xenConn)){
					System.out.println(" - " + vm.getNameLabel(xenConn));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Impossibile ottenere la lista template");
		}	
		
	}
	
	/*
	 * Print the list of installable templates
	 */
	
	public void printTemplateList() {
		try {
			System.out.println("\nLista Template:");
			for(VM vm : VmList){
				if (vm.getIsATemplate(xenConn)){
					System.out.println(" - " + vm.getNameLabel(xenConn));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Impossibile ottenere la lista template");
		}		
	}

	/*
	 * 	Returns the update query for a VM
	 */
	
	public PreparedStatement getUpdateVMQuery(VM vm) {
		PreparedStatement updateVMQuery=null;
		try{
			updateVMQuery = sqlConn.prepareStatement("UPDATE Vm " +
					"SET MemoryMb=?, " +
					"MemoryInternalFreeMb=?, " +
					"Vif0TxKb=?, " +
					"Vif0RxKb=?, " +
					"VcpuAverage=?, " +
					"VcpuNum=?, " +
					"PowerState=?, " +
					"IsControlDomain=?, " +
					"Name=? " +
					"WHERE UUID=?");
			if(vm.getPowerState(xenConn).toString().equals("Running")){
				/*
				 * The VM is Running
				 * To prevents errors during the migration (Exceptions) it's a good practise
				 * to repeat the query after an interval (default=4sec) since no exception.
				 */
				try{
					updateVMQuery.setInt(1, (int)Math.round(vm.queryDataSource(xenConn, "memory")/1024/1024));
					if(!vm.getIsControlDomain(xenConn)){
						updateVMQuery.setInt(2, (int)Math.round(vm.queryDataSource(xenConn, "memory_internal_free")/1024));
						updateVMQuery.setInt(3, (int)Math.round(vm.queryDataSource(xenConn, "vif_0_tx")/1024));
						updateVMQuery.setInt(4, (int)Math.round(vm.queryDataSource(xenConn, "vif_0_rx")/1024));
					} else {
						updateVMQuery.setNull(2, Types.INTEGER);
						updateVMQuery.setNull(3, Types.INTEGER);
						updateVMQuery.setNull(4, Types.INTEGER);
					}
					updateVMQuery.setDouble(5, getVMAverageLoad(vm));
					updateVMQuery.setInt(6, (int)Math.round(vm.getMetrics(xenConn).getVCPUsNumber(xenConn)));
					updateVMQuery.setString(7, vm.getPowerState(xenConn).toString());
					updateVMQuery.setBoolean(8, vm.getIsControlDomain(xenConn));
					updateVMQuery.setString(9, vm.getNameLabel(xenConn));
					updateVMQuery.setString(10, vm.getUuid(xenConn));
						
				} catch (Exception e){
					//Se la migrazione è in corso e si scatena una eccezione chiudo
					//Al prossimo passaggio completerò il database
					System.out.println("Migrazione in corso!");
					System.exit(0);
				}
			} else {
				//The VM is Halted
				updateVMQuery.setInt(1, (int)Math.round(vm.getMemoryDynamicMax(xenConn)/1024/1024));
				updateVMQuery.setNull(2, Types.INTEGER);
				updateVMQuery.setNull(3, Types.INTEGER);
				updateVMQuery.setNull(4, Types.INTEGER);
				updateVMQuery.setNull(5, Types.INTEGER);
				updateVMQuery.setInt(6, (int)Math.round(vm.getVCPUsMax(xenConn)));
				updateVMQuery.setString(7, vm.getPowerState(xenConn).toString());
				updateVMQuery.setBoolean(8, vm.getIsControlDomain(xenConn));
				updateVMQuery.setString(9, vm.getNameLabel(xenConn));
				updateVMQuery.setString(10, vm.getUuid(xenConn));
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return updateVMQuery;
	}

	/*
	 * Vm list in String format
	 * useful to put it in a Query string
	 */
	
	public String getAllVmString() {
		String ret = "";
		try{
			for(VM vm : getVmList()){
				if (!vm.getIsASnapshot(xenConn) && !vm.getIsATemplate(xenConn)){
					ret=ret.concat("( '" + vm.getUuid(xenConn) + "' ) , ");
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return ret.substring(0, ret.length()-3);
	}
	
	/*
	 * Return a query to populate the Map table
	 */
	
	public PreparedStatement getCorrespondanceQuery(VM vm){
		PreparedStatement ps = null;
		try {
			ps = sqlConn.prepareStatement("REPLACE INTO Map VALUES(?,?)");
			
			if(vm.getPowerState(xenConn).equals(VmPowerState.RUNNING)){
				ps.setString(1, vm.getResidentOn(xenConn).getUuid(xenConn));
			}else{
				ps.setString(1, vm.getAffinity(xenConn).getUuid(xenConn));
			}
			ps.setString(2, vm.getUuid(xenConn));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ps;
	}
	
}
