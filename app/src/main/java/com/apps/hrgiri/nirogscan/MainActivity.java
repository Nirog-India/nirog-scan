package com.apps.hrgiri.nirogscan;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ----------------------------------- WIFI --------------------------------------------------//
    private ListView wifiList;
    private WifiManager wifiManager;
    private final int MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1;
    private BroadcastReceiver wifiScanReceiver;

//    private String espSSID = "Realme C1";
//    private String espPassword = "12345677";

//    private String espSSID = "JioFi2_053A3F";
//    private String espPassword = "erxyd3y86s";

    private String espSSID = "Covid";
    private String espPassword = "password";

    private Context context = MainActivity.this;

    private Button startSeshButton, connectToWiFiButton, viewLogButton, clearLogButton,historyButton, configureWifiButton;
    private TextView infoMessageBox;
    // ---------------------------------------------------------------------------------------------//

    public String filename = Constants.logFilename;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startSeshButton = findViewById(R.id.scan_qr_button);
        connectToWiFiButton = findViewById(R.id.scan_wifi_button);
        viewLogButton = findViewById(R.id.view_log_button);
        clearLogButton = findViewById(R.id.clear_log_button);
        historyButton = findViewById(R.id.history_button);
        infoMessageBox = findViewById(R.id.info_message_box);
        configureWifiButton = findViewById(R.id.configure_wifi_button);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
//                Toast.makeText(MainActivity.this,"Connected to " + wifiManager.getConnectionInfo().getSSID() + "\nStrCmp result = " + wifiManager.getConnectionInfo().getSSID().equals("\""+espSSID+"\""),Toast.LENGTH_LONG).show();
                if(wifiManager.getConnectionInfo().getSSID().equals("\""+espSSID+"\"")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            infoMessageBox.setText("Connected to " + wifiManager.getConnectionInfo().getSSID());
                            startSeshButton.setEnabled(true);
                            configureWifiButton.setEnabled(true);
                        }
                    });
                }
                else{
                    boolean success = intent.getBooleanExtra(
                            WifiManager.EXTRA_RESULTS_UPDATED, false);
                    String action = intent.getAction();
                    if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                        List<ScanResult> wifiDeviceList = wifiManager.getScanResults();
//                    ArrayList<String> deviceList = new ArrayList<>();
                        boolean espAvailable = false;
                        for (ScanResult scanResult : wifiDeviceList) {
                            if (scanResult.SSID.equals(espSSID)) {
                                Log.d("WIFI Name",scanResult.SSID);
//                                Toast.makeText(MainActivity.this,scanResult.capabilities,Toast.LENGTH_LONG).show();
                                connectToWifi(scanResult.SSID, espPassword);
                                espAvailable = true;
                                break;
                            }
                        }
                        if(!espAvailable){
                            infoMessageBox.setText(espSSID + " not found");
                            startSeshButton.setEnabled(false);
                            connectToWiFiButton.setEnabled(true);
                            configureWifiButton.setEnabled(false);
                        }
                    }
                }
            }
        };

        startSeshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(getApplicationContext(),Scanner.class));
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 0);
                }
            }
        });

        connectToWiFiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getWifi();
            }
        });

        configureWifiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),ConfigureWifiActivity.class));
            }
        });
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileInputStream fis = null;
                try {
                    fis = context.openFileInput(Constants.historyFilename);
                    startActivity(new Intent(context,ReadHistoryActivity.class));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(context,"No readings yet!",Toast.LENGTH_LONG).show();
                }
            }
        });

        viewLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileInputStream fis = null;
                try {
                    fis = context.openFileInput(filename);
                    startActivity(new Intent(context,ViewLogActivity.class));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(context,"Log file does not exist yet!",Toast.LENGTH_LONG).show();
                }
            }
        });

        clearLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final File file = new File(context.getFilesDir(), filename);
                if(file.exists()) {
                    // Show a dialog box
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Are you sure you want to clear log?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    file.delete();
                                    Toast.makeText(context,"Log cleared!",Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //  Action for 'NO' Button
                                    dialog.cancel();//
                                }
                            });
                    //Creating dialog box
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                else Toast.makeText(context,"Log file does not exist!",Toast.LENGTH_LONG).show();
            }
        });

        connectToWiFiButton.setEnabled(false);
        startSeshButton.setEnabled(false);

        while (true) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
            } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MainActivity.this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION
                );
            } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CHANGE_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MainActivity.this, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION
                );
            }
            else
                break;
        }
        if(!wifiManager.getConnectionInfo().getSSID().equals("\"" + espSSID + "\"")) {
            getWifi();
            infoMessageBox.setText("Not connected to " + espSSID);
            startSeshButton.setEnabled(false);
            configureWifiButton.setEnabled(false);
        }
        else{
            infoMessageBox.setText("Connected to " + wifiManager.getConnectionInfo().getSSID());
            startSeshButton.setEnabled(true);
            connectToWiFiButton.setEnabled(false);
            configureWifiButton.setEnabled(true);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
//        receiverWifi = new WiFiReceiver(wifiManager, wifiList);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);
        if(!wifiManager.getConnectionInfo().getSSID().equals("\"" + espSSID + "\"")) {
            getWifi();
            infoMessageBox.setText("Not connected to " + espSSID);
            startSeshButton.setEnabled(false);
            configureWifiButton.setEnabled(false);
        }
        else{
            infoMessageBox.setText("Connected to " + wifiManager.getConnectionInfo().getSSID());
            startSeshButton.setEnabled(true);
            connectToWiFiButton.setEnabled(false);
            configureWifiButton.setEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiScanReceiver);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "permission granted", Toast.LENGTH_SHORT).show();
                    wifiManager.startScan();
                } else {
                    Toast.makeText(MainActivity.this, "permission not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
        }
    }

    // ---------------------------------------------------------------------------------------- Methods -------------------------------------------------------------------------------------------//

    private void getWifi() {

//        if (!wifiManager.getConnectionInfo().getSSID().equals(espSSID)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            Toast.makeText(MainActivity.this, "version >= marshmallow", Toast.LENGTH_SHORT).show();
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "location turned off", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE
                    }, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
                } else {
                    Toast.makeText(MainActivity.this, "location turned on", Toast.LENGTH_SHORT).show();
                    wifiManager.startScan();
                }
            } else {
                Toast.makeText(MainActivity.this, "scanning", Toast.LENGTH_SHORT).show();
                wifiManager.startScan();
            }
//        }
    }

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
                    Log.d("CONNECT", "Reconnecting");
                    wifiManager.reconnect();

                    break;
                }
            }

//            unregisterReceiver(wifiScanReceiver);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    infoMessageBox.setText("Connected to " + networkSSID);
                    startSeshButton.setEnabled(true);
                }
            });

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
        builder.setMessage("Please select " + espSSID + " in the next page and press back!")
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
                        connectToWiFiButton.setEnabled(true);
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        infoMessageBox.setText("Connected to " + networkSSID);
                        startSeshButton.setEnabled(true);
                        connectToWiFiButton.setEnabled(false);
                    }
                });

            }
        };
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
}
