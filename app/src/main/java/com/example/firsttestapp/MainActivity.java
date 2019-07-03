package com.example.firsttestapp;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends AppCompatActivity {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //**********************************************************************************************
    // KeyStore Code
    //**********************************************************************************************

    private static final String PIN_STORAGE_ALIAS = "PinStorage";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private KeyStore keyStore;

    private byte[] encryption;
    private byte[] iv;

    // ENCRYPTION CODE

    private SecretKey getSecretKeyEnc(final String alias) throws NoSuchProviderException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException
    {
        final KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

        keyGenerator.init(new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());

        return keyGenerator.generateKey();
    }

    byte[] encryptText(final String alias, final String textToEncrypt)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, IOException, InvalidAlgorithmParameterException,
            BadPaddingException, IllegalBlockSizeException {

        final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKeyEnc(alias));

        iv = cipher.getIV();

        return (encryption = cipher.doFinal(textToEncrypt.getBytes("UTF-8")));
    }

    // DECRYPTION CODE

    private void initKeyStore() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException {
        keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
    }

    private SecretKey getSecretKeyDec(final String alias) throws NoSuchAlgorithmException,
            UnrecoverableEntryException, KeyStoreException {
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(alias, null)).getSecretKey();
    }

    String decryptData(final String alias, final byte[] encryptedData, final byte[] encryptionIv)
            throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException,
            NoSuchPaddingException, InvalidKeyException, IOException,
            BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {

        final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        final GCMParameterSpec spec = new GCMParameterSpec(128, encryptionIv);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKeyDec(alias), spec);

        return new String(cipher.doFinal(encryptedData), "UTF-8");
    }


    //**********************************************************************************************
    // Read/Write Code
    //**********************************************************************************************

    private void writeToFile(byte[] data) {
        try{
            File path = MainActivity.this.getFilesDir();
            System.out.println(path.toString());
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
        try{
            File path = MainActivity.this.getFilesDir();
            System.out.println(path.toString());
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

    String inputpin = "";
    int pin_round = 0;

    private void resetAllRbtn(){

        RadioButton rbtn = findViewById(R.id.rbtn1);
        rbtn.setChecked(false);

        rbtn = findViewById(R.id.rbtn2);
        rbtn.setChecked(false);

        rbtn = findViewById(R.id.rbtn3);
        rbtn.setChecked(false);

        rbtn = findViewById(R.id.rbtn4);
        rbtn.setChecked(false);
    }

    private void rbtnSetting() {

        RadioButton rbtn = findViewById(R.id.rbtn1);
        if (pin_round >= 1)
            rbtn.setChecked(true);
        else
            rbtn.setChecked(false);

        rbtn = findViewById(R.id.rbtn2);
        if (pin_round >= 2)
            rbtn.setChecked(true);
        else
            rbtn.setChecked(false);

        rbtn = findViewById(R.id.rbtn3);
        if (pin_round >= 3)
            rbtn.setChecked(true);
        else
            rbtn.setChecked(false);

        rbtn = findViewById(R.id.rbtn4);
        if (pin_round == 4)
            rbtn.setChecked(true);
        else
            rbtn.setChecked(false);
    }

    public void sendMessage(View view) {

        switch (view.getId()) {
            case R.id.btn1:
                inputpin += "1";
                pin_round++;
                break;
            case R.id.btn2:
                inputpin += "2";
                pin_round++;
                break;
            case R.id.btn3:
                inputpin += "3";
                pin_round++;
                break;
            case R.id.btn4:
                inputpin += "4";
                pin_round++;
                break;
            case R.id.btn5:
                inputpin += "5";
                pin_round++;
                break;
            case R.id.btn6:
                inputpin += "6";
                pin_round++;
                break;
            case R.id.btn7:
                inputpin += "7";
                pin_round++;
                break;
            case R.id.btn8:
                inputpin += "8";
                pin_round++;
                break;
            case R.id.btn9:
                inputpin += "9";
                pin_round++;
                break;
            case R.id.btn_null:
                inputpin += "0";
                pin_round++;
                break;
            case R.id.btn_del:
                pin_round--;
                break;
        }
        if(pin_round < 0)
            pin_round = 0;

        rbtnSetting();

        TextView txtSuccess = findViewById(R.id.txtSuccess);
        if(pin_round == 4){
            // do checking of PIN


            // if false pin
            txtSuccess.setText("Wrong Password: " + inputpin);
            resetAllRbtn();
            pin_round = 0;
            inputpin = "";
        }
        else {
            txtSuccess.setText("");
        }
    }

    public void encryptMessage(View view) {

        try {
            TextView txtContent = findViewById(R.id.txtContent);
            encryptText(PIN_STORAGE_ALIAS, txtContent.getText().toString());

            TextView txtviewIV = findViewById(R.id.txtIV);
            txtContent.setText(bytesToHex(encryption));
            txtviewIV.setText(bytesToHex(iv));
        }
        catch(Exception ex){
            System.out.println("Could not encrypt data");
        }
    }

    public void decryptMessage(View view) {

        try {
            TextView txtContent = findViewById(R.id.txtContent);
            TextView txtIV = findViewById(R.id.txtIV);

            String enc_data = txtContent.getText().toString();
            String enc_iv = txtIV.getText().toString();

            initKeyStore();
            String msg = decryptData(PIN_STORAGE_ALIAS, hexStringToByteArray(enc_data), hexStringToByteArray(enc_iv));

            txtContent.setText(msg);
            txtIV.setText("");
        }
        catch(Exception ex) {
            System.out.println("Could not decrypt data");
        }
    }

    public void saveMessage(View view) {

        if(iv == null)
            return;

        System.out.println("IV length: " + iv.length);

        byte[] data = new byte[iv.length + encryption.length];
        System.arraycopy(iv, 0, data, 0, iv.length);
        System.arraycopy(encryption, 0, data, iv.length, encryption.length);
        writeToFile(data);
    }

    public void loadMessage(View view) {

        byte[] data = readFromFile();
        if(data == null)
            return;

        iv = new byte[12];
        encryption = new byte[data.length-12];

        System.arraycopy(data, 0, iv, 0, 12);
        System.arraycopy(data, 12, encryption, 0, data.length-12);

        TextView txtviewContent = findViewById(R.id.txtContent);
        TextView txtviewIV = findViewById(R.id.txtIV);
        txtviewContent.setText(bytesToHex(encryption));
        txtviewIV.setText(bytesToHex(iv));

    }
}
