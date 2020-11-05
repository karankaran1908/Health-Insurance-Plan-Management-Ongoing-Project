package com.csye7255.project.controller;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@RestController
public class TokenController {
    private static String finalKey = "0123456789abcdef";

    @RequestMapping(value="/token",method = RequestMethod.GET)
    public ResponseEntity getToken(@RequestHeader HttpHeaders headers) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, JSONException {

        String initVector = "RandomInitVector";
        //create token(Sample token)
        JSONObject object = new JSONObject();
        object.put("organization", "example.com");
        object.put("user", "user");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 60);
        Date date =  calendar.getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        object.put("ttl", df.format(date));


        //Partial token created
        String token = object.toString();
        System.out.println("Token values is " + token);
        System.out.println("TTL is : " + object.get("ttl"));


        IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
        SecretKeySpec skeySpec = new SecretKeySpec(finalKey.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        //encrypting token
        byte[] encrypted = cipher.doFinal(token.getBytes());

        // encoded token (Base64 encoding)
        String finalToken = org.apache.tomcat.util.codec.binary.Base64.encodeBase64String(encrypted);
        return new ResponseEntity(finalToken, HttpStatus.CREATED);
    }
}
