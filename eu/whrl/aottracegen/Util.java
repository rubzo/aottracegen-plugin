package eu.whrl.aottracegen;

import java.util.List;
import java.util.Set;

public class Util {
	public static String toHexString(int[] array) {
		String result = "[";
		for (int i = 0; i < array.length; i++) {
			if (i != array.length - 1) {
				result += String.format("%#x,",array[i]);
			} else {
				result += String.format("%#x",array[i]);
			}
		}
		result += "]";
		return result;
	}
	
	public static String toHexString(List<Integer> list) {
		String result = "[";
		for (int i = 0; i < list.size(); i++) {
			if (i != list.size() - 1) {
				result += String.format("%#x,", list.get(i));
			} else {
				result += String.format("%#x", list.get(i));
			}
		}
		result += "]";
		return result;
	}
	
	public static String toHexString(Set<Integer> set) {
		String result = "[";
		int i = 0;
		for (int v : set) {
			if (i != set.size() - 1) {
				result += String.format("%#x,", v);
			} else {
				result += String.format("%#x", v);
			}
			i++;
		}
		result += "]";
		return result;
	}
}
