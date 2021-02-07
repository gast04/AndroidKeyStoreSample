package com.sample.demo_keystore;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.stream.IntStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

public class PinHandling {

    private static final String PIN_STORAGE_ALIAS = "PinStorage";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private KeyStore keyStore;
    private byte[] encryption;
    private byte[] iv;

    File path = null;
    PinHandling(File path1){
        path = path1;
    }

    //**********************************************************************************************
    // Encryption Code
    //**********************************************************************************************
    private boolean encryptText(final String alias, final String textToEncrypt, boolean randomIV)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, IOException, InvalidAlgorithmParameterException,
            BadPaddingException, IllegalBlockSizeException {
        Log.v("Function", "encryptText");

        // needs to be disabled in order to allow custom IVs, this is not recommended but
        // necessary to get two times the same encrypted text..
        // KeyGenParameterSpec.Builder.setRandomizedEncryptionRequired

        SecretKey skey = null;
        GCMParameterSpec gcmspec;
        if(randomIV) {
            // create random IV
            iv = new byte[12];
            SecureRandom r = new SecureRandom();
            r.nextBytes(iv);
            gcmspec = new GCMParameterSpec(96, iv);

            // create new key
            skey = getSecretKeyEnc(alias);
        }
        else{
            // reuse iv and key
            gcmspec = new GCMParameterSpec(96, iv);
            try {
                skey = getSecretKeyDec(alias);
            }
            catch (Exception ex ){
                System.out.println("Could not reuse key");
                return false;
            }
        }

        // encryption code
        final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, skey, gcmspec);
        iv = cipher.getIV();
        encryption = cipher.doFinal(textToEncrypt.getBytes("UTF-8"));

        return true;
    }


    private SecretKey getSecretKeyEnc(final String alias) throws NoSuchProviderException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException
    {
        final KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        Log.v("Function", "getSecretKeyEnc");

        keyGenerator.init(new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false)
                .build());

        return keyGenerator.generateKey();
    }

    //**********************************************************************************************
    // Decryption Code
    //**********************************************************************************************
    private void initKeyStore() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException {
        Log.v("Function", "initKeyStore");
        keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
    }

    private SecretKey getSecretKeyDec(final String alias) throws NoSuchAlgorithmException,
            UnrecoverableEntryException, KeyStoreException, CertificateException, IOException {
        Log.v("Function", "getSecretKeyDec");
        initKeyStore();

        // debug derivative function
        char[] ch = alias.toCharArray();
        int length = alias.length();

        // assume virtualization from here on
        for(int i = 0; i < length; i ++){
            if( (i%3) == 0)
                ch[i] ^= 0x11;

            if( (i%5) == 0)
                ch[i] ^= 0x22;
        }
        System.out.print(ch);

        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(alias, null)).getSecretKey();
    }

    /*
    private String decryptData(final String alias, final byte[] encryptedData, final byte[] encryptionIv)
            throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException,
            NoSuchPaddingException, InvalidKeyException, IOException,
            BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {

        final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        final GCMParameterSpec spec = new GCMParameterSpec(128, encryptionIv);
        //cipher.init(Cipher.DECRYPT_MODE, getSecretKeyDec(alias), spec);

        return new String(cipher.doFinal(encryptedData), "UTF-8");
    }*/


    //**********************************************************************************************
    // Read/Write Code
    //**********************************************************************************************
    private void writeToFile(byte[] data) {
        Log.v("Function", "writeToFile");
        try{
            File file = new File(path, "store.enc");
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(data);
            stream.close();
        }
        catch (IOException e) {
            System.out.println("Could not write to File");
        }
    }

    private byte[] readFromFile() {
        Log.v("Function", "readFromFile");
        try{
            File file = new File(path, "store.enc");
            byte[] bytes = new byte[(int)file.length()];
            FileInputStream in = new FileInputStream(file);
            in.read(bytes);
            in.close();
            return bytes;
        }
        catch (IOException e) {
            System.out.println("Could not write to File");
        }
        return null;
    }

    //**********************************************************************************************
    // Application Code
    //**********************************************************************************************
    public boolean checkIfPinExists(){
        Log.v("Function", "checkIfPinExists");

        // check if file exists, if yes, we already have a PIN defined
        File file = new File(path, "store.enc");
        if (file.exists())
            return true;
        return false;
    }

    public boolean storeNewPin(String pin){
        Log.v("Function", "storeNewPin");

        // encrypt and store PIN
        try {
            if( !encryptText(PIN_STORAGE_ALIAS, pin, true))
                return false;
        }
        catch(Exception ex) {
            System.out.println("Could not encrypt data");
            return false;
        }

        byte[] data = new byte[iv.length + encryption.length];
        System.arraycopy(iv, 0, data, 0, iv.length);
        System.arraycopy(encryption, 0, data, iv.length, encryption.length);
        writeToFile(data);
        return true;
    }

    public boolean verifyPIN(String input){
        Log.v("Function", "verifyPIN");

        // load file content
        byte[] data = readFromFile();
        if(data == null)
            return false;

        // write to public iv and private enc
        iv = new byte[12];
        byte[] enc_vf = new byte[data.length-12];
        System.arraycopy(data, 0, iv, 0, 12);
        System.arraycopy(data, 12, enc_vf, 0, data.length-12);

        // encrypt PIN
        try {
            encryptText(PIN_STORAGE_ALIAS, input, false);

            if(Arrays.equals(enc_vf, encryption))
                return true;
        }
        catch(Exception ex){
            System.out.println("Could not encrypt data");
            return false;
        }

        return false;
    }

}
