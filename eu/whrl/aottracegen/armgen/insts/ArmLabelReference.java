package eu.whrl.aottracegen.armgen.insts;

public class ArmLabelReference {
	private String name;
	private boolean localLabel;
	
	public ArmLabelReference(String label) {
		if (label.startsWith(".")) {
			localLabel = true;
			name = label.substring(1, label.length());
		} else {
			localLabel = false;
			name = label;
		}
	}
	
	public String getLabelAsString() {
		String returnString = "";
		if (localLabel) {
			returnString = ".";
		}
		returnString += name;
		return returnString;
	}
	
	public String getLabelNameOnly() {
		return name;
	}
	
	public void rename(String label) {
		name = label;
	}
	
	public void setLocal(boolean local) {
		localLabel = local;
	}
	
	public boolean isLocal() {
		return localLabel;
	}
}
