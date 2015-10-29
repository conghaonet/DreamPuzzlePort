package org.hao.cache;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {
	// MD5変換
	public static String Md5(String str) {
		if (str != null && !str.equals("")) {
			try {
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
				byte[] md5Byte = md5.digest(str.getBytes("UTF8"));
				StringBuilder sb = new StringBuilder();
                for (byte aMd5Byte : md5Byte) {
                    sb.append(HEX[(aMd5Byte & 0xff) / 16]);
                    sb.append(HEX[(aMd5Byte & 0xff) % 16]);
                }
				str = sb.toString();
			} catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
			} catch (Exception e) {
                e.printStackTrace();
			}
		}
		return str;
	}
	
}
