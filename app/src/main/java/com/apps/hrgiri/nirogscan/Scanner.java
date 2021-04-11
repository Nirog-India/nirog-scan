package com.apps.hrgiri.nirogscan;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.zxing.Result;

import static com.apps.hrgiri.nirogscan.Constants.BATTERY_CHARACTERISTIC_UUID;

public class Scanner extends AppCompatActivity implements BtControllerCallback {

    public static final String EXTRA_MESSAGE = "com.apps.hrgiri.nirogscan.EMPLOYEE_DETAILS";

    private BtController btController;
    private CodeScanner codeScanner;

    private CodeScannerView codeScannerView;

    private ImageView batteryView;
    private TextView batteryTextView;

    private Context context = Scanner.this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        batteryView = findViewById(R.id.iv_battery);
        batteryTextView = findViewById(R.id.tv_battery);

        codeScannerView = findViewById(R.id.code_scanner_view);
        codeScanner = new CodeScanner(this,codeScannerView);
//        codeScanner.setCamera(CodeScanner.CAMERA_FRONT);

        codeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(Result result) {
                // Process the result
                // Will be a JSON in the form of {"ssid":value,"pass":value}
                if(result.getText().startsWith("WIFI")){
                    String mainString = result.getText().substring(5);
                    String creds[] = mainString.split(";");
                    String ssid = "";
                    String pass = "";

                    for (String cred:creds) {
                        char code = cred.charAt(0);
                        switch (code){
                            case 'S':
                                ssid = cred.substring(2);
                                break;
                            case 'P':
                                pass = cred.substring(2);
                                break;
                            case 'T':
                                //Security (Either "WEP", "WPA" or ""
                                break;
                            case 'H':
                                //Hidden (Either true or false)
                                break;
                        }
                    }
                    Log.d("DECODE","SSID: " + ssid + ", Password: " + pass);
//                    connectToWifi(ssid,pass);

                }
                else if(result.getText().startsWith("EMPLOYEE")){
                    String message = result.getText().substring(9);

                    Intent intent = new Intent(Scanner.this, SessionActivity.class);
                    intent.putExtra(EXTRA_MESSAGE, message);
//                    intent.putExtra(MainActivity.EXTRA_OBJECT, getIntent().getSerializableExtra(MainActivity.EXTRA_OBJECT));
                    startActivity(intent);
                }
                else {
                    Toast.makeText(getApplicationContext(),"Invalid QR Code!",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        btController = BtController.getInstance(); //(BtController) intent.getSerializableExtra(MainActivity.EXTRA_OBJECT);
        btController.setCallback(this);
        codeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        codeScanner.releaseResources();
        super.onPause();
    }

    @Override
    protected void onRestart() {
        btController = BtController.getInstance(); //(BtController) intent.getSerializableExtra(MainActivity.EXTRA_OBJECT);
        btController.setCallback(this);
        super.onRestart();
        codeScanner.startPreview();
    }

    @Override
    public void onBtDisconnected() {
        btController.startBleScan();
    }

    @Override
    public void onGattReady(){
    }
    @Override
    public void onCharacteristicReadSuccess(BluetoothGattCharacteristic characteristic){
    }

    @Override
    public void onCharacteristicWriteSuccess(BluetoothGattCharacteristic characteristic) {
    }

    @Override
    public void onCharacteristicNotification(BluetoothGattCharacteristic characteristic) {
        String message = "";
        String data = characteristic.getStringValue(0);
        Log.i("BluetoothGattCallback", "Read characteristic " + characteristic.getUuid() + ":\t" + data);

        switch (characteristic.getUuid().toString()) {
            case BATTERY_CHARACTERISTIC_UUID:
                final int batteryVal = characteristic.getValue()[0];
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        batteryTextView.setText(batteryVal + "%");
                        if(batteryVal < 25)
                            batteryView.setImageResource(R.drawable.battery_one_bar);
                        else if(batteryVal < 50)
                            batteryView.setImageResource(R.drawable.battery_two_bars);
                        else if(batteryVal < 75)
                            batteryView.setImageResource(R.drawable.battery_three_bars);
                        else
                            batteryView.setImageResource(R.drawable.battery_full);
                    }
                });
                break;
        }

    }
}
