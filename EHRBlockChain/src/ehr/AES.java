package ehr;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class AES implements Serializable {

    //Generate Symmetric Key
    public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(n);
        SecretKey key = keyGenerator.generateKey();
        return key;
    }

    //Generate Initialization Vector
    public static byte[] generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static String encrypt(String algorithm, String input, SecretKey key, IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String decrypt(String algorithm, String cipherText, SecretKey key, IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        String outputString = new String(plainText, "UTF-8");
        return new String(plainText);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException,
            BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException, UnsupportedEncodingException {

            //Sender Side
            String input = "TestPlainText";
            SecretKey key = AES.generateKey(128);
            //IvParameterSpec ivParameterSpec = AES.generateIv();
            byte[] iv = AES.generateIv();
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            String ivAsString = Base64.getEncoder().encodeToString(iv);
            String algorithm = "AES/CBC/PKCS5Padding";
            String cipherText = AES.encrypt(algorithm, input, key, ivParameterSpec);

            SecretKey wrongKey = AES.generateKey(128);
            //Receiver Side
            byte[] ivDecoded = Base64.getDecoder().decode(ivAsString);
            IvParameterSpec ivReceiverSide = new IvParameterSpec(ivDecoded);

           String plainText = "";
            try {
                plainText = AES.decrypt(algorithm, cipherText, wrongKey, ivReceiverSide);
            }
            catch (BadPaddingException e){
                System.out.println(cipherText);
            }


            if (plainText.equals(input)){
                System.out.println("NICE!!");
            }

            //Assertions.assertEquals(input, plainText);

    }

}
