package com.example.firsttestapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private PinHandling pin_handler;
    private Button btnFirstPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create new pinhandler instance
        pin_handler = new PinHandling(MainActivity.this.getFilesDir());
        if( !pin_handler.checkIfPinExists() ){
            // first login, we have to generate a login PIN
            setContentView(R.layout.pingeneration);

            btnFirstPin =  findViewById(R.id.btnFirstPinGeneration);
            EditText txtPin =  findViewById(R.id.txtPIN);
            if (txtPin == null){
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
                if (inputpin.length() > 0) {
                    inputpin = inputpin.substring(0, inputpin.length() - 1);
                }
                break;
        }
        if(pin_round < 0) {
            pin_round = 0;
            inputpin = "";
        }

        rbtnSetting();

        TextView txtSuccess = findViewById(R.id.txtSuccess);
        if(pin_round == 4){
            // do checking of PIN
            if(pin_handler.verifyPIN(inputpin)){
                txtSuccess.setText("Correct Password: " + inputpin);
                // TODO: open secret layout
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

        EditText txtPin = findViewById(R.id.txtPIN);
        if (pin_handler.storeNewPin(txtPin.getText().toString())) {
            // switch to MainView
            setContentView(R.layout.activity_main);
        }

    }

}
