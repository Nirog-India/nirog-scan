package com.apps.nirogindia.nirogscan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String FIREBASE_TAG = "Firebase";
    private static final String CONFIG_TAG = "Config";
    private static final String WIFI_TAG = "WIFI_P2P";
    private static final String BLE_TAG = "BluetoothLE";

    private static final String SENSOR_SERVICE_UUID = "0000aa-0000-1000-8000-00805f9b34fb";
    private static final String READ_CHARACTERISTIC_UUID = "0000aa01-0000-1000-8000-00805f9b34fb";
    private static final String FNP_CHARACTERISTIC_UUID = "0000aa02-0000-1000-8000-00805f9b34fb";
    private static final String BATTERY_CHARACTERISTIC_UUID = "0000aa03-0000-1000-8000-00805f9b34fb";

    private static final String CONFIGURE_SERVICE_UUID = "000000bb-0000-1000-8000-00805f9b34fb";
    private static final String CONFIGURE_WRITE_CHARACTERISTIC_UUID = "0000bb01-0000-1000-8000-00805f9b34fb";
    private static final String CONFIGURE_NOTIFY_CHARACTERISTIC_UUID = "0000bb02-0000-1000-8000-00805f9b34fb";

    private static final String WELCOME_UTTERANCE_ID = "TTS#1";
    private static final String FINGER_PLACE_UTTERANCE_ID = "TTS#2";
    private static final String READING_UTTERANCE_ID = "TTS#3";
    private static final String COMPLETION_UTTERANCE_ID = "TTS#4";

    private static final String FIELD_USER_ID = "USER_ID";
    private static final String FIELD_DEVICE_ID = "DEVICE_ID";
    private static final String FIELD_FCM_TOKEN = "FCM_TOKEN";
    private static final String FIELD_DISPLAY_NAME = "DISPLAY_NAME";
    private static final String FIELD_WIFI_SSID = "WIFI_SSID";
    private static final String FIELD_WIFI_PASSWORD = "WIFI_PASSWORD";

    private static final String CONFIG_FILENAME = "deviceConfig.txt";
    private static final String FINGER_PLACING_FILENAME = "finger_placing";
    private static final String SCANNING_FILENAME = "scanning";

    private static final long READ_TIMEOUT = 35000;

    private static final int TEXT_COLOR_RED = android.R.color.holo_red_light;
    private static final int TEXT_COLOR_GREEN = android.R.color.holo_green_dark;
    private static final int TEXT_COLOR_BLACK = android.R.color.black;

    private static final int PERMISSIONS_ACCESS_FINE_LOCATION = 3;
    private static final int PERMISSIONS_ACCESS_WIFI_STATE = 4;
    private static final int PERMISSIONS_CHANGE_WIFI_STATE = 5;

    private static final int FIRESTORE_READINGS_LIMIT = 500;
    private static final String BATTERY_TAG = "Battery";

    private String userID;
    private String deviceID;
    private String FCMtoken;
    private String displayName;
    private String wifiSSID;
    private String wifiPassword;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private ScanCallback scanCallback;
    private BluetoothGattServer server;
    private BluetoothDevice myNirogScanDevice;
    private BluetoothDevice esp;
    private BluetoothGatt btGatt;

    private boolean isBleScanning = false;
    private boolean isDeviceFound = false;

    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;

    private FirebaseFirestore firestore;
    private ListenerRegistration dataRegistration;
    private ListenerRegistration deviceRegistration;
    private Map<String,Object> deviceData;

//    private BroadcastReceiver wifiP2PReceiver;
//    private WifiP2pManager wifiP2pManager;
//    private WifiP2pManager.Channel channel;
//    private IntentFilter intentFilter;
//    private WifiP2pManager.PeerListListener peerListListener;

    private ConstraintLayout mainLayout;
    private ConstraintLayout videoLayout;
    private VideoView mVideoView;
    private ImageView ivBattery;
    private TextView tvInfo;
    private TextView tvBattery;
    private TextView tvTemperature;
    private TextView tvSpo;
    private TextView tvHeartRate;
    private TextView tvStatus;
    private CardView statusCard;

    private boolean isReadingCompleted = false;
    private boolean fingerNotPlaced = true;
    private boolean stopVideo = false;

    private Timer timer = new Timer();
    private TextToSpeech textToSpeech;
    private DocumentSnapshot dataDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        displayName = "Nirog Scan";

        mainLayout = findViewById(R.id.main_layout);
        videoLayout = findViewById(R.id.main_layout2);
        mVideoView = findViewById(R.id.videoView);
        ivBattery = findViewById(R.id.iv_battery);
        tvBattery = findViewById(R.id.tv_battery);
        tvInfo = findViewById(R.id.tv_info);
        tvTemperature = findViewById(R.id.tv_temp_val);
        tvSpo = findViewById(R.id.tv_spo_val);
        tvHeartRate = findViewById(R.id.tv_hr_val);
        tvStatus = findViewById(R.id.tv_status);
        statusCard = findViewById(R.id.status_card);

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if(!stopVideo)
                    mVideoView.start();
            }
        });
        textToSpeech = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.ENGLISH);
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String s) {

                        }

                        @Override
                        public void onDone(String s) {
                            switch (s) {
                                case FINGER_PLACE_UTTERANCE_ID:
                                    if (!isReadingCompleted && fingerNotPlaced)
                                        textToSpeech.speak("Please place your finger on the sensor", TextToSpeech.QUEUE_ADD, null, FINGER_PLACE_UTTERANCE_ID);
                            }
                        }

                        @Override
                        public void onError(String s) {

                        }
                    });
                }
            }
        });

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
//        bluetoothAdapter.setName(displayName);
        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
        checkPermissions();

        readConfigData();
//        startBleServer();

//        uploadTestData();
//        uploadData(10.0,20.0,30.0);
    }
    /* register the broadcast receiver with the intent values to be matched */

    @Override
    protected void onStart() {
        super.onStart();
//        startBleScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(dataRegistration != null)
            dataRegistration.remove();
        if(deviceRegistration != null)
            deviceRegistration.remove();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_CHANGE_WIFI_STATE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "All permissions granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "permission not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            case PERMISSIONS_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startBleServer();
                } else {
                    requestLocationPermission();
                }
                break;
        }
    }

    private void checkPermissions() {
        while (true) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_ACCESS_FINE_LOCATION);
            } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MainActivity.this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, PERMISSIONS_ACCESS_WIFI_STATE
                );
            } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CHANGE_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MainActivity.this, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, PERMISSIONS_CHANGE_WIFI_STATE
                );
            } else
                break;
        }
    }

    private void uploadData(Float temperature, Float hr, Float spo) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Map<String,Object> previousReadings = (Map<String, Object>) dataDocument.get("previous_readings");
        if(previousReadings == null){
            previousReadings = new HashMap<>();
        }

        Map<String, Object> reading = new HashMap<>();
        reading.put("temperature", temperature);
        reading.put("heartrate", hr);
        reading.put("oxygen", spo);
        reading.put("uuid",deviceID);

        Log.d(FIREBASE_TAG,"Readings in current document = " + previousReadings.size());
        if(previousReadings.size() >= FIRESTORE_READINGS_LIMIT){
            // Create a new document
            previousReadings.clear();
            previousReadings.put("" + Calendar.getInstance().getTimeInMillis(),reading);

            Map<String,Object> newDocument = new HashMap<>();
            newDocument.put("created","" + Calendar.getInstance().getTimeInMillis());
            newDocument.put("uuid",deviceID);
            newDocument.put("previous_readings",previousReadings);

            firestore.collection("users").document(userID)
                    .collection("Readings")
                    .add(newDocument)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.w(FIREBASE_TAG, "Added a document with id: " + documentReference.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(FIREBASE_TAG, "Error adding document", e);
                        }
                    });

        }
        else {
            previousReadings.put("" + Calendar.getInstance().getTimeInMillis(), reading);
            // Add a new document with a generated ID
            firestore.collection("users").document(userID)
                    .collection("Readings")
                    .document(dataDocument.getId())
                    .update("previous_readings", previousReadings)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.w(FIREBASE_TAG, "Uploaded data!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@androidx.annotation.NonNull Exception e) {
                            Log.w(FIREBASE_TAG, "Error updating document", e);
                        }
                    });
        }
    }

    private void uploadTestData() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Map<String,Object> previousReadings = new HashMap<>();

        for(int i=0;i<1000;i++) {
            Random random = new Random();
            float temperature = 93 + random.nextFloat()*15;
            float hr = 40 + random.nextFloat()*100;
            float spo = 93 + random.nextFloat()*7;
            Map<String, Object> reading = new HashMap<>();
            reading.put("temperature", temperature);
            reading.put("heartrate", hr);
            reading.put("oxygen", spo);
            reading.put("uuid", "oKH3JylBOtMhMLMj3gPYmJSoBdJ3-01");
            previousReadings.put("" + Calendar.getInstance().getTimeInMillis(),reading);
        }


        // Add a new document with a generated ID
        firestore.document("/users/oKH3JylBOtMhMLMj3gPYmJSoBdJ3/Readings/jtGjfAQsplIjmzIgXJIp")
                .update("previous_readings",previousReadings)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.w(FIREBASE_TAG, "Uploaded data!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception e) {
                        Log.w(FIREBASE_TAG, "Error adding document", e);
                    }
                });
    }

    private void fetchLatestDataDocument(){
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        dataRegistration = firestore.collection("users").document(userID)
                .collection("Readings")
                .whereEqualTo("uuid",deviceID)
                .orderBy("created", Query.Direction.DESCENDING).limit(1)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(FIREBASE_TAG, "Listen failed.", e);
                            return;
                        }

                        if (snapshot != null) {
                            Log.d(FIREBASE_TAG, "Current data: " + snapshot.getMetadata());
                            for (DocumentSnapshot document : snapshot.getDocuments()) {
                                Log.d(FIREBASE_TAG, document.getId());
                                dataDocument = document;
                            }
                            if(bluetoothAdapter.isEnabled()){
                                if(btGatt == null)
                                    startBleScan();
                            }
                            else {
                                //TODO: Enable Bluetooth
                            }
                        } else {
                            Log.d(FIREBASE_TAG, "Current data: null");
                        }
                    }
                });
    }


    private void uploadDeviceDetails() {
        Map<String, Object> deviceDetails = new HashMap<>();
        deviceDetails.put(FIELD_DEVICE_ID, deviceID);
        deviceDetails.put(FIELD_FCM_TOKEN, FCMtoken);
        deviceDetails.put(FIELD_DISPLAY_NAME, displayName);
        deviceDetails.put(FIELD_WIFI_SSID, wifiSSID);
        deviceDetails.put(FIELD_WIFI_PASSWORD, wifiPassword);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(userID)
                .collection("devices").document(deviceID)
                .set(deviceDetails)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void mVoid) {
                        Log.d(FIREBASE_TAG, "DocumentSnapshot written successfully");
                        saveConfigData();
                        notifyClient("COMPLETED");
                        printOnScreen("Established connection to Database!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception e) {
                        Log.w(FIREBASE_TAG, "Error adding document", e);
                        notifyClient("UPLOAD_FAIL");
                    }
                });
    }

    private void fetchDeviceDetails(){
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        deviceRegistration = firestore.collection("users").document(userID)
                .collection("devices").document(deviceID)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(FIREBASE_TAG, "Listen failed.", e);
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            Log.d(FIREBASE_TAG, "Current data: " + snapshot.getData());
                            deviceData = snapshot.getData();
                            //TODO: use the data
                            fetchLatestDataDocument();
                        } else {
                            Log.d(FIREBASE_TAG, "Current data: null");
                        }
                    }
                });

    }

    private void saveConfigData() {
        File file1 = new File(getApplicationContext().getFilesDir(), CONFIG_FILENAME);
        try (FileOutputStream fos = new FileOutputStream(file1, false)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(FIELD_DEVICE_ID, deviceID);
            jsonObject.put(FIELD_USER_ID, userID);
            jsonObject.put(FIELD_FCM_TOKEN, FCMtoken);
            jsonObject.put(FIELD_DISPLAY_NAME, displayName);
            jsonObject.put(FIELD_WIFI_SSID,wifiSSID);
            jsonObject.put(FIELD_WIFI_PASSWORD,wifiPassword);

            fos.write(jsonObject.toString().getBytes());
            fos.write(10);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Config saved!", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void readConfigData() {
        Context context = getApplicationContext();
        FileInputStream fis;
        try {
            fis = context.openFileInput(CONFIG_FILENAME);
            InputStreamReader inputStreamReader =
                    new InputStreamReader(fis, StandardCharsets.UTF_8);

            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            if (line != null) {
                JSONObject details = new JSONObject(line);
                Log.d(CONFIG_TAG, details.toString());
                userID = details.getString(FIELD_USER_ID);
                deviceID = details.getString(FIELD_DEVICE_ID);
                FCMtoken = details.getString(FIELD_FCM_TOKEN);
                displayName = details.getString(FIELD_DISPLAY_NAME);
                wifiSSID = details.getString(FIELD_WIFI_SSID);
                wifiPassword = details.getString(FIELD_WIFI_PASSWORD);
                //TODO: check if the details are valid
                //connect to wifi
                if(wifiManager.isWifiEnabled()){
                    Log.d(CONFIG_TAG,"WiFi is Enabled!");
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    if(networkInfo!=null && networkInfo.isConnected()){
                        Log.d(CONFIG_TAG,"WiFi is Connected!");
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        Log.d(CONFIG_TAG,"WiFi is Connected to " + wifiInfo.getSSID() + " but wifiSSID is " + wifiSSID);
                        if(wifiInfo.getSSID().equals("\"" + wifiSSID + "\"")){
                            Log.d(CONFIG_TAG,"WiFi is Connected to " + wifiSSID);
                            printOnScreen("Connected to WiFi");
                            fetchDeviceDetails();
                        }
                    }
                }
            }
        } catch (FileNotFoundException e){
            Log.e(CONFIG_TAG,"Config file does not exist!");
            printOnScreen("Looks like you haven't configured the device.\nPlease configure from My NirogScan app!");
            startBleServer();
        }
        catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void startBleServer() {
        if (!isLocationPermissionGranted()) {
            requestLocationPermission();
        } else {
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setConnectable(true)
                    .build();

            AdvertiseData advertiseData = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .setIncludeTxPowerLevel(true)
                    .build();

            AdvertiseData scanResponseData = new AdvertiseData.Builder()
                    .addServiceUuid(ParcelUuid.fromString(CONFIGURE_SERVICE_UUID))
                    .setIncludeTxPowerLevel(true)
                    .build();
            BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback);

        }
    }

    private boolean isLocationPermissionGranted() {
        return (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    private void requestLocationPermission() {
        if (isLocationPermissionGranted()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setMessage("Starting from Android M (6.0), the system requires apps to be granted " +
                "location access in order to scan for BLE devices.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ActivityCompat.requestPermissions(
                                MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_ACCESS_FINE_LOCATION);
                    }
                });
        //Creating dialog box
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void startBleScan(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted()) {
                requestLocationPermission();
            } else {
                isDeviceFound = false;
                isBleScanning = false;
                ScanFilter nameFilter = new ScanFilter.Builder()
                        .setDeviceName("BOOTH_GATT_SERVER")
                        .build();
                List<ScanFilter> filters = new ArrayList<>();
                filters.add(nameFilter);
                ScanSettings scanSettings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();

                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
                scanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        if(!isDeviceFound) {
                            isDeviceFound = true;
                            Log.i(BLE_TAG, "Found BLE device! Name: " + result.getDevice().getName() + ", address: " + result.getDevice().getAddress());
                            if (isBleScanning) {
                                isBleScanning = false;
                                stopBleScan();
                            }
                            esp = result.getDevice();
                            Log.w(BLE_TAG, "Connecting to " + esp.getAddress());
                            esp.connectGatt(getApplicationContext(), true, gattCallback,BluetoothDevice.TRANSPORT_LE);
                        }
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        super.onScanFailed(errorCode);
                        Log.e("ScanCallback","Error Code: " + errorCode);
                    }
                };
                bleScanner.startScan(filters, scanSettings, scanCallback);
                isBleScanning = true;
            }
        }
    }

    public void stopBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bleScanner.stopScan(scanCallback);
        }
        isBleScanning = false;
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String deviceAddress = gatt.getDevice().getAddress();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w(BLE_TAG, "Successfully connected to " + deviceAddress);
                    btGatt = gatt;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            btGatt.discoverServices();
                        }
                    });
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w(BLE_TAG, "Successfully disconnected from " + deviceAddress);
                    gatt.close();
                    printOnScreen("Not connected to sensing device\nIf this state persists, please restart NirogScan");
                }
            } else {
                Log.w(BLE_TAG, "Error " + status + " encountered for " + deviceAddress + "! Disconnecting...");
                isDeviceFound = false;
                gatt.close();
                startBleScan();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.w(BLE_TAG, "Discovered " + gatt.getServices().size() + " services for " + esp.getAddress());
            gatt.requestMtu(500);
            //            printGattTable(gatt);// See implementation just above this section
            // Consider connection setup as complete here
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.e("BluetoothGattCallback", "Status : " + status);
            printGattTable(gatt);// See implementation just above this section
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    Log.i("BluetoothGattCallback", "Read characteristic " + characteristic.getUuid() + ":\t" + characteristic.getStringValue(0));
                    //TODO: Implement Read Logic
                    break;
                case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                    Log.e("BluetoothGattCallback", "Read not permitted for " + characteristic.getUuid() + "!");
                    break;
                default:
                    Log.e("BluetoothGattCallback", "Characteristic read failed for " + characteristic.getUuid() + ", error: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    Log.i("BluetoothGattCallback", "Write characteristic " + characteristic.getUuid() + ":\t" + characteristic.getStringValue(0));
                    //TODO: Implement Write Logic
                    BluetoothGattCharacteristic btChar;
                    switch (characteristic.getUuid().toString()){
                        case BATTERY_CHARACTERISTIC_UUID:
                            btChar = gatt.getService(UUID.fromString(SENSOR_SERVICE_UUID)).getCharacteristic(UUID.fromString(FNP_CHARACTERISTIC_UUID));
                            gatt.setCharacteristicNotification(btChar,true);
                            byte[] writeValue = {1};
                            btChar.setValue(writeValue);
                            gatt.writeCharacteristic(btChar);
                            break;
                        case  FNP_CHARACTERISTIC_UUID:
                            btChar = gatt.getService(UUID.fromString(SENSOR_SERVICE_UUID)).getCharacteristic(UUID.fromString(READ_CHARACTERISTIC_UUID));
                            gatt.setCharacteristicNotification(btChar,true);
                            // Start the reading process
                            requestReading();
                            printOnScreen("Connected to " + btGatt.getDevice().getName());
                            break;
                    }
                    break;
                case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                    Log.e("BluetoothGattCallback", "Write not permitted for " + characteristic.getUuid() + "!");
                    break;
                default:
                    Log.e("BluetoothGattCallback", "Characteristic read failed for " + characteristic.getUuid() + ", error: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i("BluetoothGattCallback", "Notify characteristic " + characteristic.getUuid() + ":\t" + characteristic.getStringValue(0));
            String message = "";
            String data = characteristic.getStringValue(0);
            Log.i("BluetoothGattCallback", "Read characteristic " + characteristic.getUuid() + ":\t" + data);

            switch (characteristic.getUuid().toString()) {
                case BATTERY_CHARACTERISTIC_UUID:
                    Log.i("BluetoothReceive", "Read characteristic " + characteristic.getUuid() + ":\t" + byteToHex(characteristic.getValue()));
                    // ESP battery
                    final int batteryVal = characteristic.getValue()[0];

                    // Android battery
                    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    Intent batteryStatus = registerReceiver(null, ifilter);
                    // Are we charging / charged?
                    int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    final boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL;

                    // How are we charging?
                    int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    final boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
                    final boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                    final float batteryPct = level * 100 / (float)scale;

//                    Log.d(BATTERY_TAG,"Battery level: " + level + ", Pecentage: " + batteryPct + ", Charging? " + isCharging + ", USB Charge? " + usbCharge + ", AC charge: " + acCharge);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int currentBattery = batteryVal < batteryPct ? batteryVal : (int) batteryPct;
                            tvBattery.setText(currentBattery + "%");
                            if(currentBattery < 25)
                                ivBattery.setImageResource(R.drawable.battery_one_bar);
                            else if(currentBattery < 50)
                                ivBattery.setImageResource(R.drawable.battery_two_bars);
                            else if(currentBattery < 75)
                                ivBattery.setImageResource(R.drawable.battery_three_bars);
                            else
                                ivBattery.setImageResource(R.drawable.battery_full);
                        }
                    });
                    break;
                case FNP_CHARACTERISTIC_UUID:
                    Log.i("BluetoothReceive", "Read characteristic " + characteristic.getUuid() + ":\t" + byteToHex(characteristic.getValue()));
                    if(characteristic.getValue()[0] == 1){
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                isReadingCompleted = true;
                                textToSpeech.stop();
                                printOnScreen("Please place your finger on the sensor!");
                                releasePlayer();
                                initializePlayer(FINGER_PLACING_FILENAME);
                            }
                        },READ_TIMEOUT);

                        fingerNotPlaced = false;
                        textToSpeech.speak("Reading. Please Wait.", TextToSpeech.QUEUE_FLUSH,null,READING_UTTERANCE_ID);
                        printOnScreen("Reading. Please Wait.");
                        releasePlayer();
                        initializePlayer(SCANNING_FILENAME);
                    }
                    else{
                        fingerNotPlaced = true;
                        if(!isReadingCompleted) {
                            textToSpeech.speak("Please place your finger on the sensor", TextToSpeech.QUEUE_FLUSH, null, FINGER_PLACE_UTTERANCE_ID);
                            printOnScreen("Please place your finger on the sensor!");
                            releasePlayer();
                            initializePlayer(FINGER_PLACING_FILENAME);
                        }
                        else {
                            textToSpeech.stop();
                            printOnScreen("Please place your finger on the sensor!");
                            releasePlayer();
                            initializePlayer(FINGER_PLACING_FILENAME);
                        }
                    }
                    break;
                case READ_CHARACTERISTIC_UUID:
                    String[] recv_messages = data.split(",", 0);        // The received string is a csv
                    Log.d("LENGTH OF SPLIT", recv_messages.length + "");
                    if(recv_messages.length == 5) {
                        timer.cancel();
                        isReadingCompleted = true;
                        releasePlayer();
                        float temperature = Float.parseFloat(recv_messages[4]);
                        float heart_rate = Float.parseFloat(recv_messages[0]);
                        float spo = Float.parseFloat(recv_messages[2]);
                        float oxy_prec = Float.parseFloat(recv_messages[3]), hr_prec = Float.parseFloat(recv_messages[1]);

                        temperature = (temperature * 9f/5) + 32;


                        final boolean isHealthy = !(temperature > 100) && !(temperature < 95) && !(spo < 95);
//                        if(temperature > 100 || temperature < 95 || spo < 95){
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    tvStatus.setText("You might be ill!");
//                                    statusCard.setCardBackgroundColor(Color.RED);
//                                }
//                            });
//                        }
//                        else {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    tvStatus.setText("You are safe!");
//                                    statusCard.setCardBackgroundColor(getResources().getColor(R.color.theme_green));
//                                }
//                            });
//                        }


                        // Trim floats for displaying
                        DecimalFormat df = new DecimalFormat("0.0");
                        df.setMaximumFractionDigits(1);
                        final String heart_rate1 = df.format(heart_rate), spo1 = df.format(spo), temperature1 = df.format(temperature);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                videoLayout.setVisibility(View.GONE);
                                mainLayout.setVisibility(View.VISIBLE);

                                tvHeartRate.setText(heart_rate1);
                                tvSpo.setText(spo1);
                                tvTemperature.setText(temperature1);

                                if(isHealthy){
                                    tvStatus.setText("You are safe!");
                                    statusCard.setCardBackgroundColor(getResources().getColor(R.color.theme_green));
                                }
                                else {
                                    tvStatus.setText("You might be ill!");
                                    statusCard.setCardBackgroundColor(Color.RED);
                                }
                            }
                        });


                        textToSpeech.speak("Completed! Thank you for your time.", TextToSpeech.QUEUE_FLUSH, null, COMPLETION_UTTERANCE_ID);
                        printOnScreen("Done!");

                        uploadData(temperature,heart_rate,spo);
                        if(!isHealthy){
                            // TODO:Send Notification
                        }
                        // Upload on to the web and return after upload complete
//                        uploadData(temperature,heart_rate,spo);

                        // Instantiate the RequestQueue.
//                        RequestQueue queue = Volley.newRequestQueue(this);
//                        String url = "https://things.ubidots.com/api/v1.6/devices/esp?token=BBFF-qfdAOBNngUyWFyYXAnWK3yvyD6oO4i";
//                        JSONObject jsonBody = new JSONObject();
//                        jsonBody.put("temperature", temperature);
//                        jsonBody.put("heartrate", heart_rate);
//                        jsonBody.put("oxygen_level", spo);
//                        final String requestBody = jsonBody.toString();
//                        // Request a string response from the provided URL.
//                        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
//                                new Response.Listener<String>() {
//                                    @Override
//                                    public void onResponse(String response) {
//                                        Log.i("HttpResponse", "Response is: "+ response);
//                                        Toast.makeText(getApplicationContext(),"Uploaded to the web!",Toast.LENGTH_SHORT).show();
////                                        finish();
//                                    }
//                                }, new Response.ErrorListener() {
//                            @Override
//                            public void onErrorResponse(VolleyError error) {
//                                Log.i("HttpResponse", "That didn't work!");
//                                error.printStackTrace();
//                                Toast.makeText(getApplicationContext(),"Failed to upload to the web. Cached.",Toast.LENGTH_SHORT).show();
////                                finish();
//                            }
//                        }){
//                            @Override
//                            public String getBodyContentType() {
//                                return "application/json; charset=utf-8";
//                            }
//
//                            @Override
//                            public Map<String, String> getHeaders() throws AuthFailureError {
//                                return super.getHeaders();
//                            }
//
//                            @Override
//                            public byte[] getBody() {
//                                try {
//                                    return requestBody == null ? null : requestBody.getBytes("utf-8");
//                                } catch (UnsupportedEncodingException uee) {
//                                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
//                                    return null;
//                                }
//                            }
//
//                            @Override
//                            protected Response<String> parseNetworkResponse(NetworkResponse response) {
//                                String responseString = "";
//                                if (response != null) {
//                                    responseString = String.valueOf(response.statusCode);
//                                    // can get more details such as response.headers
//                                }
//                                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
//                            }
//                        };
//
//                        // Add the request to the RequestQueue.
//                        queue.add(stringRequest);

                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

//                        finish();
                        // TODO: start process again
                        requestReading();
                    }
                    else {
                        Log.e("Received","Invalid format");
                    }
                    break;
            }
            //            btGatt.readCharacteristic(characteristic);
        }


        private void printGattTable(BluetoothGatt gatt) {
            if (gatt.getServices().isEmpty()) {
                Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?");
                return;
            }

            for (BluetoothGattService service : gatt.getServices()) {
                String characteristicsTable = "|--";
                StringBuilder stringBuilder = new StringBuilder();
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()){
                    stringBuilder.append(characteristic.getUuid());
                    stringBuilder.append("\t" + characteristic.getStringValue(0));
                    stringBuilder.append("\n\t|--");
                    for (String property : getPropetiesString(characteristic)){
                        stringBuilder.append(property);
                        stringBuilder.append("\n\t|--");
                    }
                    stringBuilder.append("\n|--");
                }
                characteristicsTable += stringBuilder;
                Log.i("printGattTable", "\nService " + service.getUuid() + "\nCharacteristics:\n" + characteristicsTable);

//                Intent intent = new Intent(MainActivity.this, Main2Activity.class);
//                intent.putExtra(EXTRA_MESSAGE, gatt.getDevice().getAddress());
//                startActivity(intent);
//                gatt.readCharacteristic(
//                        gatt.getService(UUID.fromString("0000aa-0000-1000-8000-00805f9b34fb"))
//                        .getCharacteristic(UUID.fromString("0000aa01-0000-1000-8000-00805f9b34fb")));
                BluetoothGattCharacteristic characteristic = btGatt.getService(UUID.fromString(SENSOR_SERVICE_UUID)).getCharacteristic(UUID.fromString(BATTERY_CHARACTERISTIC_UUID));
                btGatt.setCharacteristicNotification(characteristic,true);
                byte[] writeValue = {1};
                characteristic.setValue(writeValue);
                btGatt.writeCharacteristic(characteristic);
//                callback.onGattReady();
            }
        }

        private boolean isReadable(BluetoothGattCharacteristic characteristic){
            return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0;
        }

        private boolean isWritable(BluetoothGattCharacteristic characteristic){
            return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
        }

        private boolean isNotifiable(BluetoothGattCharacteristic characteristic){
            return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
        }

        private List<String> getPropetiesString(BluetoothGattCharacteristic characteristic){
            List<String> properties = new ArrayList<>();
            if(isReadable(characteristic)) properties.add("Read");
            if(isWritable(characteristic)) properties.add("Write");
            if(isNotifiable(characteristic)) properties.add("Notify");
            if( (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_BROADCAST) != 0 ) properties.add("Broadcast");
            if( (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) != 0 ) properties.add("Extended Props");
            if( (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 ) properties.add("Indicate");
            if( (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0 ) properties.add("Signed Write");
            if( (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0 ) properties.add("Write No Response");

            return properties;
        }

    };

    BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.d(BLE_TAG, "Service added = " + service.getUuid());
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            String deviceAddress = device.getAddress();
            Log.d(BLE_TAG,deviceAddress);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w(BLE_TAG, deviceAddress + " connected to you!");
                    myNirogScanDevice = device;
                    printOnScreen("Nirog Scan is being configured...");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w(BLE_TAG, "Successfully disconnected from " + deviceAddress);
                    server.close();
                    fetchDeviceDetails();
                    printOnScreen("Nirog Scan configured!");
                }
            } else {
                Log.w(BLE_TAG, "Error " + status + " encountered for " + deviceAddress + "! Disconnecting...");
            }

        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            Log.i(BLE_TAG,"MTU changed to " + mtu);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            String message = new String(value,StandardCharsets.UTF_8);
            Log.d(BLE_TAG,"Received: " + message);
            if(message.equals("RETRY")){
                uploadDeviceDetails();
            }
            else {
                String[] data = message.split(",");
                userID = data[0];
                deviceID = data[1];
                FCMtoken = data[2];
                displayName = data[3];
                wifiSSID = data[4];
                wifiPassword = data[5];
                server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, "Success!".getBytes(StandardCharsets.UTF_8));
                connectToWifi(wifiSSID, wifiPassword);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.d(BLE_TAG,"Notification sent!");
        }
    };

    private void printOnScreen(String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvInfo.setText(s);
            }
        });
    }

    AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(BLE_TAG, "BLE advertisement added successfully");

            server = bluetoothManager.openGattServer(getApplicationContext(), bluetoothGattServerCallback);

            BluetoothGattService service = new BluetoothGattService(UUID.fromString(CONFIGURE_SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);

            BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(CONFIGURE_WRITE_CHARACTERISTIC_UUID),
                    BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);


            writeCharacteristic.addDescriptor(new BluetoothGattDescriptor(UUID.fromString("00bb0101-0000-1000-8000-00805f9b34fb"), BluetoothGattCharacteristic.PERMISSION_WRITE));

            service.addCharacteristic(writeCharacteristic);

            BluetoothGattCharacteristic notifyCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(CONFIGURE_NOTIFY_CHARACTERISTIC_UUID),
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY , BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);


            notifyCharacteristic.addDescriptor(new BluetoothGattDescriptor(UUID.fromString("00bb0201-0000-1000-8000-00805f9b34fb"), BluetoothGattCharacteristic.PERMISSION_WRITE));

            service.addCharacteristic(notifyCharacteristic);

            server.addService(service);
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(BLE_TAG, "Failed to add BLE advertisement, reason: " + errorCode);
        }
    };


    private void connectToWifi(final String networkSSID, String networkPass){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            connectToWifiNew(networkSSID,networkPass);
        }
        else {
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + networkSSID + "\"";
            conf.status = WifiConfiguration.Status.DISABLED;
            conf.priority = 40;
            conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

            conf.preSharedKey = "\"" + networkPass + "\"";


            wifiManager.addNetwork(conf);
            Log.d("CONNECT", "Configuration added");
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                    Log.d("CONNECT", "Disconnecting from previous WiFi");
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    Log.d("CONNECT", "Reconnecting to " + i.networkId);
                    wifiManager.reconnect();

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                    intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                    intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
                    registerReceiver(broadcastReceiver, intentFilter);

                    break;
                }
            }

//            unregisterReceiver(wifiScanReceiver);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    tvInfo.setText("Connected to " + networkSSID);
//                }
//            });

        }
    }

    @TargetApi(29)
    private void connectToWifiNew(final String networkSSID, String networkPass){
        final WifiNetworkSuggestion suggestion1 = new WifiNetworkSuggestion.Builder()
                .setSsid(networkSSID)
                .setWpa2Passphrase(networkPass)
                .setIsAppInteractionRequired(true)
                .build();

        final List<WifiNetworkSuggestion> suggestionsList =
                new ArrayList<WifiNetworkSuggestion>() {
                    {add(suggestion1);}
                };
//        final WifiManager wifiManager1 = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.removeNetworkSuggestions(suggestionsList);
        int status = wifiManager.addNetworkSuggestions(suggestionsList);
        if(status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS){
            Toast.makeText(getApplicationContext(),"Added network suggestion",Toast.LENGTH_SHORT).show();
        }
        // We added suggestions!
        Log.d("Suggestions Status","" + status);
        Toast.makeText(MainActivity.this,"Status="+ status,Toast.LENGTH_SHORT).show();

        // Show a dialog box
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please select " + networkSSID + " in the next page and press back!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS),0);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //  Action for 'NO' Button
                        dialog.cancel();
//                        unregisterReceiver(wifiScanReceiver);

                    }
                });
        //Creating dialog box
        AlertDialog alert = builder.create();
        alert.show();

        final NetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(networkSSID)
                .setWpa2Passphrase(networkPass)
                .build();


        final NetworkRequest request =
                new NetworkRequest.Builder()
//                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(specifier)
                        .build();
        final ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(Network network) {
                Toast.makeText(getApplicationContext(),"Connected!",Toast.LENGTH_LONG).show();
//                unregisterReceiver(wifiScanReceiver);

            }
        };
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        private boolean isConnecting = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info != null && info.isConnected()) {
                    // Do your work.

                    // e.g. To check the Network Name or other info:
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    Log.d(BLE_TAG, "Connected to " + wifiInfo.getSSID());
                    Toast.makeText(getApplicationContext(), "Connected to " + wifiInfo.getSSID() + "!", Toast.LENGTH_LONG).show();
                    notifyClient("CONNECTED");
                    uploadDeviceDetails();
                    unregisterReceiver(broadcastReceiver);
                }
            }
            else if(action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)){
                SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                Log.i(WIFI_TAG,state.toString());
                switch (state){
                    case FOUR_WAY_HANDSHAKE:
                        isConnecting = true;
                        break;
                    case DISCONNECTED:
                        if(isConnecting){
                            isConnecting = false;
                            Log.d(BLE_TAG,"Authentication Failed");
//                            Toast.makeText(getApplicationContext(),"Connected to " + network.toString() + "!",Toast.LENGTH_LONG).show();
                            notifyClient("AUTH_FAIL");
                            unregisterReceiver(broadcastReceiver);
                        }
                        break;
                    case COMPLETED:
                        if(isConnecting)
                            isConnecting = false;
                }
            }
        }
    };

    private void notifyClient(String message) {
        server.getService(UUID.fromString(CONFIGURE_SERVICE_UUID))
                .getCharacteristic(UUID.fromString(CONFIGURE_NOTIFY_CHARACTERISTIC_UUID))
                .setValue(message);
        server.notifyCharacteristicChanged(
                myNirogScanDevice,
                server.getService(UUID.fromString(CONFIGURE_SERVICE_UUID))
                        .getCharacteristic(UUID.fromString(CONFIGURE_NOTIFY_CHARACTERISTIC_UUID)),
                true);
    }

    private void initializePlayer(final String mediaName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Uri videoUri = getMedia(mediaName);
                mVideoView.setVideoURI(videoUri);
                mVideoView.start();
                stopVideo = false;
            }
        });
    }

    private void releasePlayer() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.stopPlayback();
                stopVideo = true;
            }
        });
    }

    private Uri getMedia(String mediaName) {
        return Uri.parse("android.resource://" + getPackageName() +
                "/raw/" + mediaName);
    }

    private void requestReading(){
        textToSpeech.speak("Welcome to Neerog Scan App!",TextToSpeech.QUEUE_FLUSH,null,WELCOME_UTTERANCE_ID);
        //textToSpeech.speak("Please place your finger on the sensor",TextToSpeech.QUEUE_ADD,null,FINGER_PLACE_UTTERANCE_ID);
        printOnScreen("Device Ready to take readings!");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainLayout.setVisibility(View.GONE);
                videoLayout.setVisibility(View.VISIBLE);
            }
        });
        initializePlayer(FINGER_PLACING_FILENAME);

        // A write starts the reading process and when the process is done, the on
        byte[] writeValue = {1};
        btGatt.getService(UUID.fromString(SENSOR_SERVICE_UUID)).getCharacteristic(UUID.fromString(READ_CHARACTERISTIC_UUID)).setValue(writeValue);
        btGatt.writeCharacteristic(
                btGatt.getService(UUID.fromString(SENSOR_SERVICE_UUID))
                        .getCharacteristic(UUID.fromString(READ_CHARACTERISTIC_UUID)));
    }

    private String byteToHex(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }
}