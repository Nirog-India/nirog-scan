package com.apps.hrgiri.nirogscan;

import android.annotation.TargetApi;
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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.zxing.Result;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Scanner extends AppCompatActivity {

    CodeScanner codeScanner;
    CodeScannerView codeScannerView;

    public static final String EXTRA_MESSAGE = "com.apps.hrgiri.nirogscan.EMPLOYEE_DETAILS";

    private Context context = Scanner.this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        codeScannerView = findViewById(R.id.code_scanner_view);
        codeScanner = new CodeScanner(this,codeScannerView);

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
        codeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        codeScanner.releaseResources();
        super.onPause();
    }
/*
    private void connectToWifi(String networkSSID,String networkPass){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            connectToWifiNew(networkSSID, networkPass);
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

            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
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

            //remember id
//        int netId = wifiManager.addNetwork(conf);
//        Log.d("CONNECT","Disconnecting from previous WiFi");
//        wifiManager.disconnect();
//        wifiManager.enableNetwork(netId, true);
//        Log.d("CONNECT","Reconnecting");
//        wifiManager.reconnect();
        }
    }

    @TargetApi(29)
    private void connectToWifiNew(String networkSSID, String networkPass){
        NetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                        .setSsid(networkSSID)
                        .setWpa2Passphrase(networkPass)
                        .build();

        final NetworkRequest request =
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(specifier)
                        .build();
        final ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(Network network) {
                Toast.makeText(getApplicationContext(),"Connected!",Toast.LENGTH_LONG).show();

                Intent intent = new Intent(Scanner.this, SessionActivity.class);
                JSONObject fakeEmployee = new JSONObject();
                try {
                    fakeEmployee.put("NAME","Jane Doe");
                    fakeEmployee.put("PHONE","999999");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                intent.putExtra(EXTRA_MESSAGE, fakeEmployee.toString());
                startActivity(intent);
            }
        };
        connectivityManager.requestNetwork(request, networkCallback);

    }
    */
}
