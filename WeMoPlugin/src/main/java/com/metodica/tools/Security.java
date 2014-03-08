package com.metodica.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;

public class Security {
	
	private String publicKey = "d3m0p4ssw0rd";
	private SecretKey key = null;
	private int keyLength = 256;
	IvParameterSpec iv = new IvParameterSpec(new byte[16]);
	private byte[] salt = new byte[]
    {
        0x07, 0x08, 0x00, 0x09, 0x06, 0x07, 0x09, 0x08, 0x08, 0x07,0x05, 0x06, 0x04, 0x05, 0x07, 0x06
    };
	
	PublicKey RSAPublic = null;
	PrivateKey RSAPrivate = null;
	
	public void setPublicKey(String pass) {
		publicKey = pass;
	}
	
	public void setKeyLength(int len) {
		keyLength = len;
	}
	
	public void setSalt(byte[] newSalt) {
		salt = newSalt;
	}
	
	public synchronized static String toBase64(byte[] bytes) {
    	return (Base64.encodeToString(bytes, Base64.DEFAULT));
    }
    
    public synchronized static byte[] fromBase64(String base64Text) throws IOException {
    	return (Base64.decode(base64Text, Base64.DEFAULT));
    }
    
	public String getPublicKey() {
		return publicKey;
	}
	
	////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	////					 HASH MD5 ZONE						\\\\
	
	////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	public synchronized static byte[] hash(String text) throws Exception {
		MessageDigest md = MessageDigest.getInstance("md5"); 
		return md.digest(text.getBytes("utf-8"));
	}
	
	public synchronized static String hashString(String text) throws Exception {
		MessageDigest md = MessageDigest.getInstance("md5"); 
		return toBase64(md.digest(text.getBytes("utf-8")));
	}
	
    public synchronized static String md5(String s) {
    	String ret = s;
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();
            
            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i < messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            ret = hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return ret;
    }
	
	
	
	////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	////						AES ZONE						\\\\
	
	////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    
    public String AESGenerateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
    	SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(publicKey.toCharArray(), salt, 1024, keyLength);
        SecretKey tmp = factory.generateSecret(spec);
        key = new SecretKeySpec(tmp.getEncoded(), "AES");
        return toBase64(key.getEncoded());
    }
    
    public void AESLoadKey(String codedKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
    	key = null;
    	key = new SecretKeySpec(fromBase64(codedKey), "AES");
    }
    
    public void loadPlainAESKey(String keys) {
    	key = new SecretKeySpec(keys.getBytes(), "AES");
    }
    
    public byte[] AESencrypt(String message) throws Exception {

        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        byte[] plainTextBytes = message.getBytes("utf-8");
        byte[] cipherText = cipher.doFinal(plainTextBytes);
        
        return cipherText;
    }
    
    public String AESdecrypt(byte[] message) throws Exception {
        
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        Cipher decipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        decipher.init(Cipher.DECRYPT_MODE, key, iv);

        byte[] plainText = decipher.doFinal(message);

        return new String(plainText, "UTF-8");
    }
	
	
	////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	////					TRIPLE DES ZONE						\\\\
	
	////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
    public void TripleDESGenerateKey() throws Exception {
//    	byte[] digestOfPassword = hash(publicKey);
//        
//        byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
//        for (int j = 0, k = 16; j < 8;) {
//                keyBytes[k++] = keyBytes[j++];
//        }
//
//        key = new SecretKeySpec(keyBytes, "DESede");
    }
		
    public byte[] TripleDESencrypt(String message) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        byte[] plainTextBytes = message.getBytes("utf-8");
        byte[] cipherText = cipher.doFinal(plainTextBytes);
        
        return cipherText;
    }
    
    public String TripleDESdecrypt(byte[] message) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        Cipher decipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        decipher.init(Cipher.DECRYPT_MODE, key, iv);

        byte[] plainText = decipher.doFinal(message);

        return new String(plainText, "UTF-8");
    }
    
    
	////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	////						RSA ZONE						\\\\
	
	////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    
    public void RSAGenerateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
    	KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    	kpg.initialize(keyLength);
    	KeyPair kp = kpg.genKeyPair();
    	RSAPublic = kp.getPublic();
    	RSAPrivate = kp.getPrivate();
    }
    
    public byte[] RSAencryptLong (String message) throws Exception {
	    byte[] cipherTextBytes;
	    byte[] clearTextBytes = message.getBytes("utf-8");
	    ByteArrayOutputStream cipherTextBaos = new ByteArrayOutputStream();
	
	    ByteArrayOutputStream baos;
	    CipherOutputStream cos;
	
	    Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
	    rsaCipher.init(Cipher.ENCRYPT_MODE, RSAPublic);
	
	    int clearTextLength = clearTextBytes.length;
	    int blockSize = rsaCipher.getBlockSize();

	    for (int bytesRead = 0; bytesRead < clearTextLength; bytesRead = bytesRead + blockSize) {
		    if (bytesRead + blockSize > clearTextLength) {
			    baos = new ByteArrayOutputStream();
			    cos = new CipherOutputStream(baos, rsaCipher);
	
			    cos.write(clearTextBytes, bytesRead, clearTextLength - bytesRead);
			    cos.close();
		
			    cipherTextBaos.write(baos.toByteArray());
		    } else {
		    	baos = new ByteArrayOutputStream();
		    	cos = new CipherOutputStream(baos, rsaCipher);
	
		    	cos.write(clearTextBytes, bytesRead, blockSize);
		    	cos.close();
		
		    	cipherTextBaos.write(baos.toByteArray());
		    }
	    }
	    cipherTextBaos.close();
	    cipherTextBytes = cipherTextBaos.toByteArray();
	
	    return cipherTextBytes;
    }
    
    public byte[] RSAencrypt(String message) throws Exception {

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, RSAPublic);

        byte[] plainTextBytes = message.getBytes("utf-8");
        byte[] cipherText = cipher.doFinal(plainTextBytes);
        
        return cipherText;
    }
    
    public String RSAdecryptLong(byte[] cipherTextBytes) throws Exception {
	    byte[] clearTextBytes;
	    ByteArrayOutputStream cipherTextBaos = new ByteArrayOutputStream();
		ByteArrayOutputStream baos;
		CipherOutputStream cos;

		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		rsaCipher.init(Cipher.DECRYPT_MODE, RSAPrivate);

		int cipherTextLength = cipherTextBytes.length;
		int blockSize = rsaCipher.getBlockSize();

		for (int bytesRead = 0; bytesRead < cipherTextLength; bytesRead = bytesRead + blockSize) {
			if (bytesRead + blockSize > cipherTextLength) {
				baos = new ByteArrayOutputStream();
				cos = new CipherOutputStream(baos, rsaCipher);

				cos.write(cipherTextBytes, bytesRead, cipherTextLength - bytesRead);
				cos.close();
				cipherTextBaos.write(baos.toByteArray());
			} else {
				baos = new ByteArrayOutputStream();
				cos = new CipherOutputStream(baos, rsaCipher);
				
				
				cos.write(cipherTextBytes, bytesRead, 
				blockSize);
				cos.close();

				cipherTextBaos.write(baos.toByteArray());
			}
		}
		cipherTextBaos.close();
		clearTextBytes = cipherTextBaos.toByteArray();

		return new String(clearTextBytes, "UTF-8");
    }
    
    public String RSAdecrypt(byte[] message) throws Exception {
        
        Cipher decipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decipher.init(Cipher.DECRYPT_MODE, RSAPrivate);

        byte[] plainText = decipher.doFinal(message);

        return new String(plainText, "UTF-8");
    }
    
    public byte[] RSAsign(String message) throws Exception {

    	Signature cipher = Signature.getInstance("SHA1withRSA");
        cipher.initSign(RSAPrivate);
        
        byte[] plainTextBytes = message.getBytes("utf-8");
        cipher.update(plainTextBytes);
        
        return cipher.sign();
    }
    
    public boolean RSAverify(byte[] sign, String message) throws Exception {
        
    	Signature decipher = Signature.getInstance("SHA1withRSA");
        decipher.initVerify(RSAPublic);

        decipher.update(message.getBytes());

        return decipher.verify(sign);
    }
    
    public void RSALoadPrivateKey (byte[] RSALoadKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
    	RSAPrivate = null;
    	KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    	KeySpec pKeySpec = new PKCS8EncodedKeySpec(RSALoadKey);
		RSAPrivate = keyFactory.generatePrivate(pKeySpec);
    }
    
    public void RSALoadPublicKey (byte[] RSALoadKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
    	RSAPublic = null;
    	KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    	KeySpec pKeySpec = new X509EncodedKeySpec(RSALoadKey);
		RSAPublic = keyFactory.generatePublic(pKeySpec);
    }
    
    public void RSALoadPrivateKey (String RSALoadKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
    	RSAPrivate = null;
    	KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    	KeySpec pKeySpec = new PKCS8EncodedKeySpec(fromBase64(RSALoadKey));
		RSAPrivate = keyFactory.generatePrivate(pKeySpec);
    }
    
    public void RSALoadPublicKey (String RSALoadKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
    	RSAPublic = null;
    	KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    	KeySpec pKeySpec = new X509EncodedKeySpec(fromBase64(RSALoadKey));
		RSAPublic = keyFactory.generatePublic(pKeySpec);
    }
    
    public void RSALoadPrivateKeyAsPublicKey (String RSALoadKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
    	RSAPublic = null;
    	KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    	KeySpec pKeySpec = new PKCS8EncodedKeySpec(fromBase64(RSALoadKey));
		RSAPublic = keyFactory.generatePublic(pKeySpec);
    }
}
