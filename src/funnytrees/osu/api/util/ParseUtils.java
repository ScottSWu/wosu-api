package funnytrees.osu.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseUtils {
	public static String[] parseLine(String argline) {
		ArrayList<String> args = new ArrayList<String>();
		Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(argline);
		
		while (m.find()) {
			args.add(m.group(1).replace("\"", ""));
		}
		
		String[] arr = new String[args.size()];
		args.toArray(arr);
		return arr;
	}
	
	public static Map<String,String> parseArgs(String[] args) {
		HashMap<String,String> list = new HashMap<String,String>();
		
		for (int i=0; i<args.length; i++) {
			if (args[i].charAt(0)==':' && i<args.length-1) {
				if (args[i+1].charAt(0)==':') {
					list.put(args[i].substring(1), null);
				}
				else {
					list.put(args[i].substring(1), args[i+1]);
					i++;
				}
			}
		}
		
		return list;
	}
	
	public static Map<String,String> parseArgs(String argline) {
		return parseArgs(parseLine(argline));
	}
}
