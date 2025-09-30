package com.sample.demo_keystore;

import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// import com.sample.demo_keystore.R;

public class MainActivity extends AppCompatActivity {

    private PinHandling pin_handler;
    private Button btnFirstPin;
    private ImageView gifView;
    private boolean gif_enabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("Function", "onCreate");

        // create new pinhandler instance
        pin_handler = new PinHandling(MainActivity.this.getFilesDir());
        if( !pin_handler.checkIfPinExists() ){
            // first login, we have to generate a login PIN
            setContentView(R.layout.pingeneration);

            btnFirstPin = findViewById(R.id.btnFirstPinGeneration);
            EditText txtPin = findViewById(R.id.txtPIN);
            if (txtPin == null) {
                // not found, should never happen
            }

            txtPin.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    if ( editable.length() == 4) {
                        btnFirstPin.setEnabled(true);
                    }
                    else {
                        btnFirstPin.setEnabled(false);
                    }
                }
            });
        }
        else {
            // pin already created
            setContentView(R.layout.activity_main);
        }
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
    // Application Code
    //**********************************************************************************************

    String inputpin = "";
    int pin_round = 0;

    private void resetAllRbtn() {
        Log.v("Function", "resetAllRbtn");

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
        Log.v("Function", "rbtnSetting");

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

        // extra debug code
        Random random = new Random();

        int a = random.nextInt(200);
        int b = random.nextInt(200);
    }

    private static final Map<Integer, String> DIGIT_MAP = new HashMap<>();
    static {
        DIGIT_MAP.put(R.id.btn1, "1");
        DIGIT_MAP.put(R.id.btn2, "2");
        DIGIT_MAP.put(R.id.btn3, "3");
        DIGIT_MAP.put(R.id.btn4, "4");
        DIGIT_MAP.put(R.id.btn5, "5");
        DIGIT_MAP.put(R.id.btn6, "6");
        DIGIT_MAP.put(R.id.btn7, "7");
        DIGIT_MAP.put(R.id.btn8, "8");
        DIGIT_MAP.put(R.id.btn9, "9");
        DIGIT_MAP.put(R.id.btn_null, "0");
    }

    public void sendMessage(View view) {
        Log.v("Function", "sendMessage");


        if (view.getId() == R.id.btn_del) {
            pin_round--;
            if(pin_round < 0) {
                pin_round = 0;
                inputpin = "";
            }

            if (!inputpin.isEmpty()) {
                inputpin = inputpin.substring(0, inputpin.length() - 1);
            }
        }
        else {
            pin_round++;
            inputpin += DIGIT_MAP.get(view.getId());
        }

        rbtnSetting();

        TextView txtSuccess = findViewById(R.id.txtSuccess);
        if(pin_round == 4){
            // do checking of PIN
            if(pin_handler.verifyPIN(inputpin)){
                txtSuccess.setText("Correct Password: " + inputpin);
                setContentView(R.layout.mainapp);

                // load gif on startup
                gifView = findViewById(R.id.imageViewGif);
                Glide.with(this).load("https://media.giphy.com/media/FbPsiH5HTH1Di/giphy.gif").into(gifView);
                gif_enabled = true;

                return; // end function after layout switch
            }
            else {
                txtSuccess.setText("Wrong Password: " + inputpin);
            }
            resetAllRbtn();
            pin_round = 0;
            inputpin = "";
        }
        else {
            txtSuccess.setText("");
        }
    }

    public void firstLoginDone(View view) {
        Log.v("Function", "firstLoginDone");

        EditText txtPin = findViewById(R.id.txtPIN);
        if (pin_handler.storeNewPin(txtPin.getText().toString())) {
            // switch to MainView
            setContentView(R.layout.activity_main);
        }
    }

    //**********************************************************************************************
    // After Login Code
    //**********************************************************************************************

    TextView txtValue;

    //SecureInt32 secint32 = new SecureInt32(0);
    //int secint32 = 0;

    int value = 0;

    public void IncVal(View view) {
        Log.v("Function", "IncVal");
        //secint32.add(1);
        value ++;
        txtValue = findViewById(R.id.txtValue);
        //txtValue.setText("" + secint32.getValue());
        txtValue.setText("" + value);
    }

    public void DecVal(View view) {
        Log.v("Function", "DecVal");

        //secint32.sub(1);
        value --;
        txtValue = findViewById(R.id.txtValue);
        //txtValue.setText("" + secint32.getValue());
        txtValue.setText("" + value);
    }

    public void btnDisableGifOnClick(View view) {
        if (gif_enabled) {
            gifView.setImageDrawable(null);
        }
        else {
            Glide.with(this).load("https://media.giphy.com/media/FbPsiH5HTH1Di/giphy.gif").into(gifView);
        }
        gif_enabled = !gif_enabled;
    }
}
