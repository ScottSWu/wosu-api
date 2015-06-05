package funnytrees.osu.api.data;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import org.apache.commons.lang3.StringEscapeUtils;
import funnytrees.osu.api.util.HashUtils;

public class SongFolder {
	public String hash;
	public File file;
	public Path path;
	public File data;
	public Set<BeatmapFile> beatmaps;
	public ArrayList<String> fileList = null;
	
	public SongFolder(File f, File c) {
		hash = HashUtils.md5(f.getName());
		file = f;
		path = f.toPath();
		data = new File(c, hash + ".json");
		beatmaps = new HashSet<BeatmapFile>();
	}
	
	public void parseFolder() {
		parseFolder(false);
	}
	
	public void parseFolder(boolean force) {
		if (force || fileList==null) {
			fileList = new ArrayList<String>();
			
			Queue<File> search = new LinkedList<File>();
			search.add(file);
			
			while (!search.isEmpty()) {
				File current = search.poll();
				if (current.isDirectory()) {
					for (File f : current.listFiles()) {
						search.add(f);
					}
				}
				else {
					fileList.add(
						StringEscapeUtils.escapeJava(
							path.relativize(current.toPath()).toString().replaceAll("\\\\", "/")
						)
					);
				}
			}
		}
	}
}
