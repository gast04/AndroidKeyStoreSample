package com.sample.demo_keystore;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import io.castle.android.Castle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {

    private PinHandling pin_handler;

    private PinWebHandling pin_web;

    private Button btnFirstPin;
    private ImageView gifView;
    private boolean gif_enabled = false;
    private ProgressBar progressBar; // default visibility is false

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(constants.FUNCTAG, "onCreate");

        // Place the below in your Application class onCreate method
        Castle.configure(getApplication(), BuildConfig.PUBLISHABLE_KEY);

        // NOTE: localhost will be the devices or emulators localhost
        pin_web = new PinWebHandling("http://10.13.37.73:9988/");

        // Fetch token synchronously using ExecutorService
        Future<String> tokenFuture =
                executor.submit(
                        () -> {
                            try {
                                return pin_web.fetchToken();
                            } catch (Exception e) {
                                Log.e(constants.LOGTAG, "Token fetching failed", e);
                                throw new RuntimeException("Token fetching failed", e);
                            }
                        });

        try {
            // This will block until the token is fetched
            String token = tokenFuture.get();
            Log.i(constants.LOGTAG, "Token received: " + token);

            Castle.userJwt(token);

            String currentTime =
                    new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            Castle.screen("Onboarding - Verify documents");
            Castle.custom("AppStartup", Map.of("time", currentTime));

            Log.i(constants.LOGTAG, "Token setup completed");
        } catch (Exception e) {
            Log.e(constants.LOGTAG, "Token initialization failed", e);
            // Handle the error - you might want to show an error dialog or finish the activity
            TextView txtSuccess = findViewById(R.id.txtSuccess);
            if (txtSuccess != null) {
                txtSuccess.setText("Token fetching failed");
            }
            disableAllButtons(true);
        }
        Log.i(constants.LOGTAG, "Token collected");

        // create new pinhandler instance
        pin_handler = new PinHandling(MainActivity.this.getFilesDir());
        if (!pin_handler.checkIfPinExists()) {
            // first login, we have to generate a login PIN
            setContentView(R.layout.pingeneration);

            btnFirstPin = findViewById(R.id.btnFirstPinGeneration);
            EditText txtPin = findViewById(R.id.txtPIN);
            if (txtPin == null) {
                // not found, should never happen
            }

            txtPin.addTextChangedListener(
                    new TextWatcher() {
                        @Override
                        public void beforeTextChanged(
                                CharSequence charSequence, int i, int i1, int i2) {}

                        @Override
                        public void onTextChanged(
                                CharSequence charSequence, int i, int i1, int i2) {}

                        @Override
                        public void afterTextChanged(Editable editable) {
                            if (editable.length() == 4) {
                                btnFirstPin.setEnabled(true);
                            } else {
                                btnFirstPin.setEnabled(false);
                            }
                        }
                    });
        } else {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown the executor service to prevent memory leaks
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    // **********************************************************************************************
    // Application Code
    // **********************************************************************************************

    String inputpin = "";
    int pin_round = 0;

    private void resetAllRbtn() {
        Log.v(constants.FUNCTAG, "resetAllRbtn");

        pin_round = 0;
        inputpin = "";

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
        Log.v(constants.FUNCTAG, "rbtnSetting");

        RadioButton rbtn = findViewById(R.id.rbtn1);
        rbtn.setChecked(pin_round >= 1);

        rbtn = findViewById(R.id.rbtn2);
        rbtn.setChecked(pin_round >= 2);

        rbtn = findViewById(R.id.rbtn3);
        rbtn.setChecked(pin_round >= 3);

        rbtn = findViewById(R.id.rbtn4);
        rbtn.setChecked(pin_round == 4);
    }

    private void disableAllButtons(boolean disable) {
        Log.v(constants.FUNCTAG, "disableAllButtons: " + disable);

        int[] buttonIds = {
            R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6,
            R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn_null, R.id.btn_ok, R.id.btn_del
        };

        for (int buttonId : buttonIds) {
            Button button = findViewById(buttonId);
            if (button != null) {
                button.setEnabled(!disable);
            }
        }
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

        if (view.getId() == R.id.btn_del) {
            pin_round--;
            if (pin_round < 0) {
                pin_round = 0;
                inputpin = "";
            }

            if (!inputpin.isEmpty()) {
                inputpin = inputpin.substring(0, inputpin.length() - 1);
            }
        } else if (view.getId() == R.id.btn_ok) {
            // do nothing
        } else {
            pin_round++;
            inputpin += DIGIT_MAP.get(view.getId());
        }

        rbtnSetting();

        TextView txtSuccess = findViewById(R.id.txtSuccess);
        if (pin_round == 4) {
            // do checking of PIN
            boolean local_pin_ok = pin_handler.verifyPIN(inputpin);

            // Show spinner while checking with backend
            // NOTE: both have to match, we do not immediately return on keystore match
            txtSuccess.setText("Verifying...");

            // NOTE: do not use future here, to not freeze UI and let the progressbar spin
            // the buttons are disabled, so nothing can happen
            if (progressBar == null) {
                progressBar = findViewById(R.id.progressBar);
            }
            progressBar.setVisibility(View.VISIBLE);

            // Disable all buttons during verification
            disableAllButtons(true);

            // pin also needs to be approved by backend
            pin_web.verifyPIN(
                    inputpin,
                    Castle.createRequestToken(),
                    new PinWebHandling.PinVerificationCallback() {
                        @Override
                        public void onSuccess(boolean web_pin_ok) {
                            runOnUiThread(
                                    () -> {
                                        progressBar.setVisibility(View.GONE);
                                        disableAllButtons(false);

                                        if (local_pin_ok && web_pin_ok) {
                                            setContentView(R.layout.mainapp);
                                            gifView = findViewById(R.id.imageViewGif);
                                            Glide.with(MainActivity.this)
                                                    .load(
                                                            "https://media.giphy.com/media/FbPsiH5HTH1Di/giphy.gif")
                                                    .into(gifView);
                                            gif_enabled = true;
                                        } else if (!web_pin_ok) {
                                            txtSuccess.setText("Network Validation Failed");
                                            resetAllRbtn();
                                        } else {
                                            txtSuccess.setText("Validation Failed");
                                            resetAllRbtn();
                                        }
                                    });
                        }

                        @Override
                        public void onError(Throwable error) {
                            runOnUiThread(
                                    () -> {
                                        progressBar.setVisibility(View.GONE);
                                        disableAllButtons(false);
                                        txtSuccess.setText("Network Error");
                                        resetAllRbtn();
                                    });
                        }
                    });
        } else {
            txtSuccess.setText("");
        }
    }

    public void firstLoginDone(View view) {
        Log.v(constants.FUNCTAG, "firstLoginDone");

        EditText txtPin = findViewById(R.id.txtPIN);
        if (pin_handler.storeNewPin(txtPin.getText().toString())) {
            // switch to MainView
            setContentView(R.layout.activity_main);
        }
    }

    // **********************************************************************************************
    // After Login Code
    // **********************************************************************************************

    TextView txtValue;

    int value = 0;

    public void IncVal(View view) {
        Log.v(constants.FUNCTAG, "IncVal");
        value++;
        txtValue = findViewById(R.id.txtValue);
        txtValue.setText("" + value);
    }

    public void DecVal(View view) {
        Log.v(constants.FUNCTAG, "DecVal");

        value--;
        txtValue = findViewById(R.id.txtValue);
        txtValue.setText("" + value);
    }

    public void btnDisableGifOnClick(View view) {
        if (gif_enabled) {
            gifView.setImageDrawable(null);
        } else {
            Glide.with(this)
                    .load("https://media.giphy.com/media/FbPsiH5HTH1Di/giphy.gif")
                    .into(gifView);
        }
        gif_enabled = !gif_enabled;
    }
}
