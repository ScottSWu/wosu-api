package funnytrees.osu.api.www;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.freeutils.httpserver.HTTPServer.ContextHandler;
import net.freeutils.httpserver.HTTPServer.Headers;
import net.freeutils.httpserver.HTTPServer.Request;
import net.freeutils.httpserver.HTTPServer.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import funnytrees.osu.api.BeatmapAPI;
import funnytrees.osu.api.data.BeatmapFile;
import funnytrees.osu.api.data.SongFolder;

public class WebHandler implements ContextHandler {
	final static Map<String,String>	contentTypes	= new HashMap<String,String>();
	final Pattern					md5Regex		= Pattern.compile("[a-f0-9]{32}");
	final String					indexFile		= "/funnytrees/osu/api/www/index.html";
	final String					faviconFile		= "/funnytrees/osu/api/www/favicon.ico";
	final String					errorFile		= "/funnytrees/osu/api/www/error.html";
	
	BeatmapAPI						api;
	
	static {
		// Text
		contentTypes.put(".html", "text/html");
		contentTypes.put(".txt", "text/plain; charset=\"UTF-8\"");
		contentTypes.put(".osu", "text/plain; charset=\"UTF-8\"");
		contentTypes.put(".osz", "text/plain; charset=\"UTF-8\"");
		
		// Image
		contentTypes.put(".gif", "image/gif");
		contentTypes.put(".jpg", "image/jpeg");
		contentTypes.put(".png", "image/png");
		
		// Audio
		contentTypes.put(".mp3", "audio/mpeg");
		contentTypes.put(".ogg", "audio/ogg");
		contentTypes.put(".wav", "audio/wave");
		
		// Video
		contentTypes.put(".avi", "video/avi");
		contentTypes.put(".mpeg", "video/mpeg");
		contentTypes.put(".mp4", "video/mp4");
		contentTypes.put(".ogg", "video/ogg");
		contentTypes.put(".webm", "video/webm");
		contentTypes.put(".flv", "video/x-flv");
	}
	
	public WebHandler(BeatmapAPI a) {
		api = a;
	}
	
	@Override
	public int serve(Request req, Response resp) throws IOException {
		String path = req.getPath();
		
		System.out.println("> " + path);
		
		if (path.equals("/")) {
			return serve(req, resp, indexFile);
		}
		else if (path.equals("/favicon.ico")) {
			return serve(req, resp, faviconFile);
		}
		else {
			String[] request = path.split("/", 3);
			
			if (request.length > 0) {
				if (request[1].equals("get")) {
					if (request.length > 2) {
						String location = new String(Base64.decodeBase64(request[2]));
						return serve(req, resp, new URL(location));
					}
				}
				else if (request[1].equals("replay")) {
					return serve(req, resp, req.getBody());
				}
				else if (md5Regex.matcher(request[1]).matches()) {
					BeatmapFile f;
					SongFolder s;
					String args = "";
					
					if (request.length > 2) {
						args = request[2];
					}
					
					if ((f = api.beatmapTable.get(request[1])) != null) {
						return serve(req, resp, f, args);
					}
					else if ((s = api.songTable.get(request[1])) != null) {
						return serve(req, resp, s, args);
					}
				}
			}
		}
		
		Headers h = resp.getHeaders();
		h.add("Access-Control-Allow-Origin", "*");
		resp.send(404, "Not Found");
		return 0;
	}
	
	public int serve(Request req, Response resp, String resource) throws IOException {
		Headers h = resp.getHeaders();
		
		String contentType = "text/plain";
		int extIndex = resource.lastIndexOf(".");
		if (extIndex >= 0) {
			String ext = resource.substring(extIndex).toLowerCase();
			if (contentTypes.containsKey(ext)) {
				contentType = contentTypes.get(ext);
			}
		}
		
		InputStream fileStream = BeatmapAPI.class.getResourceAsStream(resource);
		byte[] filedata;
		try {
			filedata = IOUtils.toByteArray(fileStream);
		}
		catch (IOException ex) {
			ex.printStackTrace();
			filedata = new byte[] { 0 };
		}
		fileStream.close();
		
		h.add("Content-Type", contentType);
		h.add("Content-Length", String.valueOf(filedata.length));
		resp.sendHeaders(200);
		resp.sendBody(new ByteArrayInputStream(filedata), filedata.length, null);
		
		return 0;
	}
	
	public int serve(Request req, Response resp, BeatmapFile f, String args) throws IOException {
		Headers h = resp.getHeaders();
		h.add("Access-Control-Allow-Origin", "*");
		
		if (args.length() == 1) {
			if (args.charAt(0) == 'R') {
				h.add("Content-Type", "text/plain; charset=utf-8");
				h.add("Content-Length", String.valueOf(f.file.length()));
				resp.sendHeaders(200);
				resp.sendBody(new FileInputStream(f.file), f.file.length(), null);
				
				return 0;
			}
		}
		else if (args.length() > 1) {
			if (args.charAt(0) == 'R' && args.charAt(1) == '/') {
				return serve(req, resp, f.song, args);
			}
		}
		
		h.add("Content-Type", "application/json");
		h.add("Content-Length", String.valueOf(f.data.length()));
		resp.sendHeaders(200);
		resp.sendBody(new FileInputStream(f.data), f.data.length(), null);
		
		return 0;
	}
	
	public int serve(Request req, Response resp, SongFolder s, String args) throws IOException {
		Headers h = resp.getHeaders();
		h.add("Access-Control-Allow-Origin", "*");
		
		s.parseFolder();
		
		if (args.length() == 1) {
			if (args.charAt(0) == 'R') {
				StringBuilder output = new StringBuilder();
				for (String file : s.fileList) {
					output.append(file + "\n");
				}
				
				h.add("Content-Type", "text/plain; charset=utf-8");
				h.add("Content-Length", String.valueOf(output.length()));
				resp.send(200, output.toString());
				
				return 0;
			}
		}
		else if (args.length() > 1) {
			if (args.charAt(0) == 'R' && args.charAt(1) == '/') {
				File target = new File(s.file, args.substring(2));
				
				String contentType = "application/octet-stream";
				int extIndex = args.lastIndexOf(".");
				if (extIndex >= 0) {
					String ext = args.substring(extIndex).toLowerCase();
					
					if (contentTypes.containsKey(ext)) {
						contentType = contentTypes.get(ext);
					}
				}
				
				if (contentType.startsWith("audio") || contentType.startsWith("video")) {
					h.add("Accept-Ranges", "bytes");
				}
				
				if (isParent(s.file, target)) {
					h.add("Content-Type", contentType);
					h.add("Content-Length", String.valueOf(target.length()));
					resp.sendHeaders(200);
					resp.sendBody(new FileInputStream(target), target.length(), null);
					
					return 0;
				}
			}
		}
		
		StringBuilder output = new StringBuilder("[");
		boolean first = true;
		for (String file : s.fileList) {
			if (first) first = false;
			else output.append(",");
			output.append("\"" + file + "\"");
		}
		output.append("]");
		
		h.add("Content-Type", "application/json");
		h.add("Content-Length", String.valueOf(output.length()));
		resp.send(200, output.toString());
		
		return 0;
	}
	
	/**
	 * Proxy an external file no larger than 1 MB.
	 * 
	 * @param location
	 */
	public int serve(Request req, Response resp, URL location) throws IOException {
		Headers h = resp.getHeaders();
		h.add("Access-Control-Allow-Origin", "*");
		
		String locString = location.toString();
		
		try {
			String contentType = "text/plain; charset=utf-8";
			int extIndex = locString.lastIndexOf(".");
			if (extIndex >= 0) {
				String ext = locString.substring(extIndex).toLowerCase();
				if (contentTypes.containsKey(ext)) {
					contentType = contentTypes.get(ext);
				}
			}
			
			// localhost:9678/replay/aHR0cDovLzEyNy4wLjAuMTo5Njc4
			
			StringBuilder replayFile = new StringBuilder();
			
			byte[] buffer = new byte[1024];
			int read, total = 0;
			InputStream uin = location.openStream();
			
			// Write hex
			while ((read = uin.read(buffer)) != -1) {
				total += read;
				replayFile.append(hexify(buffer, read));
				if (total > 1024 * 1024) {
					uin.close();
					
					return 404;
				}
			}
			uin.close();
			
			h.add("Content-Type", contentType);
			h.add("Content-Length", String.valueOf(replayFile.length()));
			resp.send(200, replayFile.toString());
			
			return 0;
		}
		catch (IOException e1) {
			e1.printStackTrace();
			return 404;
		}
	}
	
	final protected static char[]	hexabet	= "0123456789ABCDEF".toCharArray();
	
	/**
	 * Convert byte array to hex string
	 */
	private String hexify(byte[] buffer, int length) {
		char[] hex = new char[length * 2];
		for (int i = 0; i < length; i++) {
			hex[2 * i] = hexabet[(buffer[i] >>> 4) & 0xF];
			hex[2 * i + 1] = hexabet[(buffer[i]) & 0xF];
		}
		return new String(hex);
	}
	
	/**
	 * Serves an uploaded replay file. Not implemented.
	 */
	public int serve(Request req, Response resp, InputStream body) throws IOException {
		return 404;
	}
	
	// http://stackoverflow.com/questions/16186285/secure-way-to-create-relative-java-io-file
	public boolean isParent(File parent, File file) {
		File f;
		try {
			parent = parent.getCanonicalFile();
			
			f = file.getCanonicalFile();
		}
		catch (IOException e) {
			return false;
		}
		
		while (f != null) {
			if (parent.equals(f)) {
				return true;
			}
			f = f.getParentFile();
		}
		
		return false;
	}
}
