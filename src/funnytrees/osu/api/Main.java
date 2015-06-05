package funnytrees.osu.api;

import java.io.File;
import java.io.IOException;

public class Main {
	public static void main(String args[]) throws IOException {
		if (args.length<3) {
			System.out.println("Usage:");
			System.out.println("\tjava -jar BeatmapAPI.jar <Songs Folder> <Cache File> <Cache Folder>");
			System.out.println();
			System.exit(0);
		}
		
		File r = new File(args[0]);
		File c = new File(args[1]);
		File f = new File(args[2]);
		
		/* Cache testing
		BeatmapAPI api = new BeatmapAPI(r,c,f);
		
		LogUtils.logHead("Loading beatmaps...");
		api.loadBeatmaps(true);
		LogUtils.logTail("done");
		
		LogUtils.logHead("Saving to cache...");
		api.saveCache();
		LogUtils.logTail("done");
		/*/
		BeatmapAPI api = new BeatmapAPI(r,c,f);
		LogUtils.logHead("Reading cache...");
		//api.loadCache(c);
		api.loadBeatmaps();
		api.saveCache();
		LogUtils.logTail("done");
		
		LogUtils.log("Starting server");
		api.startServer();
		//*/
	}
}
