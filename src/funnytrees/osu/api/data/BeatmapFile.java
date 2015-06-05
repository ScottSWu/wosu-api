package funnytrees.osu.api.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Scanner;
import funnytrees.osu.api.util.HashUtils;

public class BeatmapFile {
	public String hash;
	public File file;
	public Path path;
	public File data;
	public SongFolder song;
	
	public BeatmapFile(File f,File c,SongFolder p) {
		this(HashUtils.md5(f),f,c,p);
	}
	
	public BeatmapFile(String h,File f,File c,SongFolder p) {
		hash = h;
		file = f;
		path = f.toPath();
		data = new File(c,hash + ".json");
		song = p;
		song.beatmaps.add(this);
	}
	
	public void parseBeatmap() {
		parseBeatmap(false);
	}
	
	final static String[] properties = {
		"AudioFilename",
		"Mode",
		"Title",
		"TitleUnicode",
		"Artist",
		"ArtistUnicode",
		"Creator",
		"Version",
		"Source",
		
		"HPDrainRate",
		"CircleSize",
		"OverallDifficulty",
		"ApproachRate"
	};
	final static boolean[] quotes = {
		true,
		false,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		
		false,
		false,
		false,
		false
	};
	public void parseBeatmap(boolean force) {
		if (force || !data.exists()) {
			try {
				Scanner fin = new Scanner(file, "UTF-8");
				Writer fout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(data), "UTF-8"));
				
				fout.write("{");
				fout.write("\"map\":\"" + this.hash + "\",");
				fout.write("\"song\":\"" + song.hash + "\"");
				
				String line;
				String key,value;
				int split;
				while (fin.hasNextLine()) {
					line = fin.nextLine();
					split = line.indexOf(":");
					if (split>=0) {
						key = line.substring(0,split).trim();
						value = line.substring(split+1).trim();
						for (int i=0; i<properties.length; i++) {
							if (key.equalsIgnoreCase(properties[i])) {
								fout.write(",");
								fout.write("\"" + properties[i] + "\":" + (quotes[i]?"\"":"") + value + (quotes[i]?"\"":""));
								fout.flush();
							}
						}
					}
				}
				
				fout.write("}");
				
				fin.close();
				fout.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public int hashCode() {
		return this.file.hashCode();
	}
	
	public boolean equals(Object o) {
		return o instanceof BeatmapFile && ((BeatmapFile) o).file.equals(this.file);
	}
}
