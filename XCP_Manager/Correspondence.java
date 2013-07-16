package main;

public class Correspondence {
	String HostCorr;
	String VmCorr;
	
	public Correspondence(String hsCorr, String vmCorr){
		this.HostCorr=hsCorr; this.VmCorr=vmCorr;
	}
	
	public String getVmCorrispondance(){
		return VmCorr;
	}
	
	public String getHostCorrispondance(){
		return HostCorr;
	}
	
	
}
