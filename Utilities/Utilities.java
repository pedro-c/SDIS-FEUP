package Utilities;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.sql.Timestamp;

public class Utilities {

    private static final String transformation = "AES";

    /**
     * Returns a hexadecimal encoded SHA-256 hash for the input String.
     *
     * @param data
     * @return string with Hash
     */
    public static BigInteger createHash(String data) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes("UTF-8"));
            return new BigInteger(hash).abs();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Use javax.xml.bind.DatatypeConverter class in JDK to convert byte array
     * to a hexadecimal string. Note that this generates hexadecimal in upper case.
     *
     * @param hash
     * @return string with hash in hexadecimal
     */
    public static String bytesToHex(byte[] hash) {
        return DatatypeConverter.printHexBinary(hash);
    }

    public static int get32bitHashValue(BigInteger bigInteger) {
        return Integer.remainderUnsigned(Math.abs(bigInteger.intValue()), 128);
    }

    public static long getTimestamp() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return timestamp.getTime();
    }

    /**
     * Generates Chat id with creation date and user_id : CREATION_DATE + USER_CREATOR_ID
     *
     * @return BigInteger chat Id
     */
    public static BigInteger generateChatId(String email) {
        return createHash(String.valueOf(getTimestamp()) + email);
    }


    public static byte[] encrypt(byte[] inpBytes, PublicKey key) throws Exception
    {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(inpBytes);
    }

    public static byte[] decrypt(byte[] inpBytes, PrivateKey key) throws Exception
    {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(inpBytes);
    }


    public static KeyPair generateUserKeys(String password) throws NoSuchProviderException, NoSuchAlgorithmException {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

        SecureRandom random = new SecureRandom(password.getBytes());
        keyGen.initialize(1024, random);

        return keyGen.generateKeyPair();
    }


}
