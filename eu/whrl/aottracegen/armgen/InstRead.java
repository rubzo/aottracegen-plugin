package eu.whrl.aottracegen.armgen;


import java.util.List;
import java.util.LinkedList;
import eu.whrl.aottracegen.armgen.insts.*;
import java.lang.reflect.InvocationTargetException;

public class InstRead {
	private static List<Class> parsers = new LinkedList<Class>();

	public static void parse(List<String> traceBody) {
		ArmInst tail = new ArmInstComment("-- Begin parsed ASM --");
		ArmInst head = tail;

		for (String line : traceBody) {
			if (!line.startsWith("@")) {
				try {
					ArmInst newInst = ArmInstParser.parse(line);
					newInst.linkToPrevious(tail);
					tail = newInst;
				} catch (NotParsableException e) {
					System.out.println("Unable to parse: " + line);
				}
			}
		}

		System.out.println("FINAL PARSE:");
		while (head.next != null) {
			System.out.println(((ArmInstPrintable) head).print());
			head = head.next;
		}
		System.out.println(((ArmInstPrintable) head).print());
	}
	
}
