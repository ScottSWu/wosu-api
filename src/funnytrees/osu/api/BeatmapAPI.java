package funnytrees.osu.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.freeutils.httpserver.HTTPServer;
import net.freeutils.httpserver.HTTPServer.VirtualHost;
import funnytrees.osu.api.data.BeatmapFile;
import funnytrees.osu.api.data.SongFolder;
import funnytrees.osu.api.www.WebHandler;

public class BeatmapAPI {
	private final static int		DEFAULT_PORT			= 9678;
	private final static String		DEFAULT_ROOT_DIRECTORY	= ".";
	private final static String		DEFAULT_CACHE_DIRECTORY	= "./BeatmapAPICache";
	private final static String		DEFAULT_CACHE_FILE		= "./BeatmapAPICache.txt";
	
	private CacheManager			cache;
	private File					rootFolder;
	private File					cacheFile;
	private File					cacheFolder;
	private int						port;
	private HTTPServer				server;
	private VirtualHost				apihost;
	
	/**
	 * 
	 */
	public Map<String,BeatmapFile>	beatmapTable;
	public Map<String,SongFolder>	songTable;
	
	public BeatmapAPI() {
		this(DEFAULT_PORT);
	}
	
	public BeatmapAPI(int p) {
		this(p, new File(DEFAULT_ROOT_DIRECTORY));
	}
	
	public BeatmapAPI(File r) {
		this(DEFAULT_PORT, r);
	}
	
	public BeatmapAPI(int p, File r) {
		this(p, r, new File(DEFAULT_CACHE_FILE), new File(DEFAULT_CACHE_DIRECTORY));
	}
	
	public BeatmapAPI(int p, File r, File f) {
		this(p, r, new File(DEFAULT_CACHE_FILE), f);
	}
	
	public BeatmapAPI(File r, File f) {
		this(DEFAULT_PORT, r, new File(DEFAULT_CACHE_FILE), f);
	}
	
	public BeatmapAPI(File r, File c, File f) {
		this(DEFAULT_PORT, r, c, f);
	}
	
	public BeatmapAPI(int p, File r, File c, File f) {
		cache = new CacheManager(this);
		
		port = p;
		
		rootFolder = r;
		if (!rootFolder.isDirectory()) rootFolder.mkdirs();
		cacheFile = c;
		cacheFolder = f;
		if (!cacheFolder.isDirectory()) cacheFolder.mkdirs();
		
		beatmapTable = new HashMap<String,BeatmapFile>();
		songTable = new HashMap<String,SongFolder>();
		
		try {
			server = new HTTPServer(p);
			
			apihost = new VirtualHost(null);
			apihost.setAllowGeneratedIndex(false);
			apihost.addContext("/", new WebHandler(this));
			
			server.addVirtualHost(apihost);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void loadCache() throws FileNotFoundException {
		loadCache(cacheFile);
	}
	
	public void loadCache(File c) throws FileNotFoundException {
		cacheFile = c;
		
		Scanner fin = new Scanner(cacheFile);
		
		beatmapTable.clear();
		songTable.clear();
		
		while (fin.hasNextLine()) {
			String line = fin.nextLine();
			try {
				String[] parts = line.substring(2).split(">");
				if (line.charAt(0) == 'V') {
					try {
						if (parts[0].equalsIgnoreCase("port")) {
							port = Integer.parseInt(parts[1]);
						}
						else if (parts[0].equalsIgnoreCase("root")) {
							rootFolder = new File(parts[1]);
						}
					}
					catch (Exception e) {
						
					}
				}
				else if (line.charAt(0) == 'S') {
					parts = line.substring(2).split(">");
					File f = new File(rootFolder, parts[1]);
					if (f.isDirectory()) {
						songTable.put(parts[0], new SongFolder(f, cacheFolder));
					}
				}
				else if (line.charAt(0) == 'B') {
					parts = line.substring(2).split(">");
					File f = new File(rootFolder, parts[2]);
					if (f.exists()) {
						beatmapTable.put(parts[0], new BeatmapFile(parts[0], f, cacheFolder, songTable.get(parts[1])));
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		fin.close();
	}
	
	/**
	 * Save data to the default cache location
	 */
	public void saveCache() {
		saveCache(cacheFile);
	}
	
	/**
	 * Save data to a specified cache location
	 * 
	 * @param cache
	 */
	public void saveCache(File cache) {
		try {
			FileWriter fout = new FileWriter(cache);
			fout.write("V>port>" + port + "\n");
			fout.write("V>root>" + rootFolder.getAbsolutePath() + "\n");
			
			Set<String> keys;
			String path;
			Path rootPath = Paths.get(rootFolder.toURI());
			
			keys = songTable.keySet();
			
			for (String hash : keys) {
				path = rootPath.relativize(Paths.get(songTable.get(hash).file.getAbsolutePath())).toString();
				fout.write("S>" + hash + ">" + path + "\n");
			}
			
			keys = beatmapTable.keySet();
			for (String hash : keys) {
				BeatmapFile f = beatmapTable.get(hash);
				path = rootPath.relativize(Paths.get(f.file.getAbsolutePath())).toString();
				fout.write("B>" + hash + ">" + f.song.hash + ">" + path + "\n");
			}
			fout.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void loadBeatmaps() {
		loadBeatmaps(false);
	}
	
	/**
	 * Recursively load all songs and beatmaps in the root directory
	 */
	public void loadBeatmaps(boolean force) {
		Stack<File> toLoad = new Stack<File>();
		Stack<SongFolder> parent = new Stack<SongFolder>();
		toLoad.add(rootFolder);
		parent.add(new SongFolder(rootFolder, cacheFolder));
		int total = 1;
		int folders = 0;
		int done = 0;
		int skip = 0;
		int unzip = 0;
		long lastTime = System.currentTimeMillis();
		long currentTime;
		
		while (!toLoad.isEmpty()) {
			File f = toLoad.pop();
			SongFolder p = parent.pop();
			
			if (f.isDirectory()) { // Recursive searching
				SongFolder sf = new SongFolder(f, cacheFolder);
				for (File t : f.listFiles()) {
					if (!t.getName().startsWith(".")) {
						toLoad.add(t);
						parent.add(sf);
					}
					else {
						skip++;
					}
					total++;
				}
				folders++;
			}
			else if (f.getName().endsWith(".osz")) { // Extract beatmap
				try {
					String name = f.getName();
					File target = new File(f.getParentFile(), name.substring(0, name.length() - 4));
					if (!target.isDirectory() || force) {
						target.mkdir();
						
						try {
							byte[] buffer = new byte[1024 * 10];
							ZipInputStream zipis = new ZipInputStream(new FileInputStream(f));
							ZipEntry zipe;
							
							while ((zipe = zipis.getNextEntry()) != null) {
								File extract = new File(target, zipe.getName());
								File extractParent = new File(extract.getParent());
								extractParent.mkdirs();
								
								FileOutputStream fex = new FileOutputStream(extract);
								
								int read = 0;
								while ((read = zipis.read(buffer)) > 0) {
									fex.write(buffer, 0, read);
								}
								fex.close();
							}
							zipis.closeEntry();
							zipis.close();
							
							unzip++;
						}
						catch (IllegalArgumentException e) {
							e.printStackTrace();
						}
						
						// Add all recursed items to the queue
						SongFolder sf = new SongFolder(target, cacheFolder);
						for (File t : target.listFiles()) {
							if (!t.getName().startsWith(".")) {
								toLoad.add(t);
								parent.add(sf);
							}
							else {
								skip++;
							}
							total++;
						}
						folders++;
					}
				}
				catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if (f.getName().endsWith(".osu")) { // Parse the beatmap
				BeatmapFile b = new BeatmapFile(f, cacheFolder, p);
				if (!beatmapTable.containsKey(b.hash)) {
					b.parseBeatmap(force);
					beatmapTable.put(b.hash, b);
				}
				if (!songTable.containsKey(p.hash)) {
					songTable.put(p.hash, p);
				}
				done++;
			}
			else {
				skip++;
			}
			
			// Logging
			currentTime = System.currentTimeMillis();
			if (currentTime - lastTime > 5000) {
				LogUtils.log(total + " discovered, " + folders + " folders, " + done + " parsed, " + unzip
					+ " unziped, " + skip + " skipped, " + toLoad.size() + " left");
				lastTime = currentTime;
			}
		}
		
		LogUtils.log(total + " discovered, " + folders + " folders, " + done + " parsed, " + unzip + " unziped, "
			+ skip + " skipped, " + toLoad.size() + " left");
	}
	
	public void startServer() {
		if (server != null) {
			try {
				server.start();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stopServer() {
		if (server != null) {
			server.stop();
		}
	}
}
