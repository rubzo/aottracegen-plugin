package eu.whrl.aottracegen.armgen.insts;

import java.util.List;
import java.util.LinkedList;

public class ArmInstPseudoDirectiveList extends ArmInstPseudoDirective implements ArmInstPrintable {
	public List<String> argsList;

	public ArmInstPseudoDirectiveList(String name) {
		super(name);
		this.argsList = new LinkedList<String>();
	}

	public void addElement(String arg) {
		argsList.add(arg);
	}

	public String print() {
		String listString = "";
		for (int i = 0; i < argsList.size(); i++) {
			listString += argsList.get(i);
			if (i != argsList.size()-1) {
				listString += ", ";
			}
		}
		return "." + name + " " + listString;
	}
}
