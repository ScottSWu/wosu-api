package funnytrees.osu.api;

public class LogUtils {
	private static int level = 0;
	
	public static void logHead(Object o) {
		log(o);
		level++;
	}
	
	public static void logTail(Object o) {
		log(o);
		if (level>0) level--;
	}
	
	public static void log(Object o) {
		for (int i=0; i<level; i++) {
			System.out.print(".\t");
		}
		System.out.println(o);
	}
}
