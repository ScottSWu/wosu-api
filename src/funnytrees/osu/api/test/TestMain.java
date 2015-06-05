package funnytrees.osu.api.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import net.freeutils.httpserver.HTTPServer;
import net.freeutils.httpserver.HTTPServer.ContextHandler;
import net.freeutils.httpserver.HTTPServer.Header;
import net.freeutils.httpserver.HTTPServer.Headers;
import net.freeutils.httpserver.HTTPServer.Request;
import net.freeutils.httpserver.HTTPServer.Response;
import net.freeutils.httpserver.HTTPServer.VirtualHost;

public class TestMain {
	public static void main(String args[]) throws FileNotFoundException, IOException {
		HTTPServer server = new HTTPServer(8888);
		VirtualHost host = new VirtualHost(null);
		
		File f = new File("White Album.mp3");
		
		host.setAllowGeneratedIndex(false);
		final byte[] fileData = IOUtils.toByteArray(new FileInputStream(f));
		System.out.println(fileData.length);
		
		host.addContext("/", new ContextHandler() {

			@Override
			public int serve(Request req, Response resp) throws IOException {
				long[] ranges = req.getRange(fileData.length);
				Headers qhs = req.getHeaders();
				for (Header h : qhs) {
					System.out.println("\t" + h.getName() + ": " + h.getValue());
				}
				
				Headers shs = resp.getHeaders();
				shs.add("Accept-Ranges", "bytes");
				shs.add("Access-Control-Allow-Origin", "*");
				shs.add("Content-Type", "audio/mpeg");
				shs.add("Content-Length", String.valueOf(fileData.length));
				resp.sendHeaders(200);
				resp.sendBody(new ByteArrayInputStream(fileData), fileData.length, ranges);
				return 0;
			}
			
		});
		
		server.addVirtualHost(host);
		try {
			server.start();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
