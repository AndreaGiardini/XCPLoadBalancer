package main;

import java.util.HashSet;
import java.util.Set;

public class CorrespondenceList {
	
	public Set<Correspondence> variables;

	public CorrespondenceList(){
		variables = new HashSet<Correspondence>();
	}
	
	public void add(Correspondence corrispondance) {
		variables.add(corrispondance);		
	}
	
	public String get(String srt){
		for(Correspondence csd : variables){
			if (csd.getHostCorrispondance().equals(srt)){
				return csd.getVmCorrispondance();
			}
			if (csd.getVmCorrispondance().equals(srt) ){
				return csd.getHostCorrispondance();
			}
		}
		return null;
	}

}
