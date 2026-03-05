package com.punchh.server.utilities;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
 



public class RailsHmac {
 
    public static String hmacSha256(String pepper, String apiKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec =
                    new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
 
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
 
            StringBuilder hex = new StringBuilder();
            for (byte b : hmacBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
 
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    

    //This is a sample code to test the HMAC generation
    public static void main(String[] args) {
    	String apiKey = "QKQq7yA4BgJ9eSmYs6Vo";
    	String pepper = "efa6954a2772a260e275433bd50b2b1d3b9263a26275b1776786c4559009ff02056b89e6d569f1d4a644fcb92db259de3e578ee8a49376942244daf406e2077b";
    	 
    	String encryptedKey = RailsHmac.hmacSha256(pepper, apiKey);
    	System.out.println("Encrypted Key: " + encryptedKey);
	}

 



}
