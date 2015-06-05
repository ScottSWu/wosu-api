package funnytrees.osu.api.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.zip.CRC32;

public class HashUtils {
	
	final protected static char[] hex = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	
	public static String md5(File f) {
		try {
			FileInputStream fin = new FileInputStream(f);
			byte[] data = new byte[fin.available()];
			fin.read(data);
			fin.close();
			return md5(data);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			return "";
		}
		catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static String md5(String s) {
		try {
			return md5(s.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static String md5(byte[] bs) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hashb = md.digest(bs);
			
			char[] hashc = new char[hashb.length*2];
			int b;
			for (int i=0; i<hashb.length; i++) {
				b = hashb[i] & 0xFF;
				hashc[i*2] = hex[b>>>4];
				hashc[i*2+1] = hex[b&0xF];
			}
			
			return new String(hashc);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static String CRC32(String s) {
		CRC32 crc = new CRC32();
		crc.update(s.getBytes());
		StringBuilder hash = new StringBuilder(Long.toHexString(crc.getValue()));
		while (hash.length()<8) {
			hash.insert(0, '0');
		}
		return hash.toString();
	}
}
