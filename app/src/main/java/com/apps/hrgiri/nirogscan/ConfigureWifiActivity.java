package com.apps.hrgiri.nirogscan;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.anychart.APIlib;
import com.anychart.chart.common.dataentry.SingleValueDataSet;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

import static com.apps.hrgiri.nirogscan.SessionActivity.CODE_STRING;
import static com.apps.hrgiri.nirogscan.SessionActivity.DISCONNECT_STRING;
import static com.apps.hrgiri.nirogscan.SessionActivity.END_OF_MESSAGE;
import static com.apps.hrgiri.nirogscan.SessionActivity.ERROR_STRING;
import static com.apps.hrgiri.nirogscan.SessionActivity.ERR_ACK_STRING;
import static com.apps.hrgiri.nirogscan.SessionActivity.ERR_OK;
import static com.apps.hrgiri.nirogscan.SessionActivity.MESSAGE_STRING;
import static com.apps.hrgiri.nirogscan.SessionActivity.PING_STRING;
import static com.apps.hrgiri.nirogscan.SessionActivity.PING_TIMEOUT;
import static com.apps.hrgiri.nirogscan.SessionActivity.READ_TIMEOUT;
import static com.apps.hrgiri.nirogscan.SessionActivity.WIFI_STRING;

public class ConfigureWifiActivity extends AppCompatActivity {

    enum Stage{
        NOT_CONNECTED,
        WIFI,
        COMPLETED,
        FIN
    }
    // ----------------------------------- SOCKET --------------------------------------------------//
//    private String SERVER_IP = "192.168.43.188";              // For Realme
//    private String SERVER_IP = "192.168.1.101";                 // For JioFi
    private String SERVER_IP = "192.168.4.1";                 // For ESP
    private int SERVER_PORT = 80;
    private boolean isConnected = false;

    private Socket socket;
    private PrintWriter output;
    private DataInputStream input;
    // ---------------------------------------------------------------------------------------------//

    // ----------------------------------- FINCTIONALITY --------------------------------------------------//
    private Queue<String> messageQueue;

    private Stage currentStage;

    private boolean isWait = false;

    private long startConnectionTime, startReadTime;

    private String CONNECTION_CODE = "";
    private String sentCode = ERR_OK;
    private String logContents = "";
    // ---------------------------------------------------------------------------------------------//

    private Button configButton;
    private TextView infoDisplay;
    private EditText etSSID, etPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_wifi);

        configButton = findViewById(R.id.button_wifi_config);
        infoDisplay = findViewById(R.id.tv_wifi_info);
        etSSID = findViewById(R.id.et_wifi_ssid);
        etPass = findViewById(R.id.et_wifi_password);

        messageQueue = new LinkedList<>();

        etSSID.setText("sripad");
        etPass.setText("sripad1996");

        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageContent = "";
                String code = "";
                switch (currentStage){
                    case WIFI:
                        JSONObject creds = new JSONObject();
                        try {
                            creds.put("ssid", etSSID.getText().toString().trim());
                            creds.put("pass", etPass.getText().toString().trim());
                            messageContent = creds.toString();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        code = WIFI_STRING;
                        break;
                    case COMPLETED:
                        code = DISCONNECT_STRING;
                        break;
                    case FIN:
                        new Thread(new DisconnectThread()).start();

                }

                String message = JSONify(code,messageContent);
                if(!message.isEmpty()){
                    addToQueue(code,message);
                }
            }
        });

        setCurrentStage(Stage.NOT_CONNECTED);

        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeWifiNetwork = null;
        for (Network network : connectivityManager.getAllNetworks()) {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                if(networkInfo.isConnected()) activeWifiNetwork = network;
                break;
            }
        }
        connectivityManager.bindProcessToNetwork(activeWifiNetwork);

        new Thread(new UIThread()).start();
        // Connect to server socket
        new Thread(new ConnectThread()).start();
    }


    private void displayOnScreen(final String textToDisplay){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoDisplay.setText(textToDisplay);
            }
        });
    }

    private void addToQueue(@NotNull String code, String message){
        message += END_OF_MESSAGE;

        messageQueue.add(message);

        switch (code){
            case WIFI_STRING:
                sentCode = code;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        configButton.setEnabled(false);
                    }
                });
                displayOnScreen("Please Wait");
                startReadTime = Calendar.getInstance().getTimeInMillis();
                isWait = true;
                break;
            case DISCONNECT_STRING:
                sentCode = code;
                displayOnScreen("Please Wait");
                startReadTime = Calendar.getInstance().getTimeInMillis();
                isWait = true;
                break;
        }


    }

    private void setCurrentStage(@NotNull Stage stage){
        currentStage = stage;

        switch (stage){
            case NOT_CONNECTED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        configButton.setEnabled(false);
                    }
                });
                break;
            case WIFI:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        configButton.setEnabled(true);
                        configButton.setText("Configure");
                    }
                });
                break;
            case COMPLETED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        configButton.setEnabled(true);
                        configButton.setText("Save Configuration");
                    }
                });
                break;
            case FIN:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        configButton.setEnabled(true);
                        configButton.setText("Back");
                    }
                });
                break;

        }
    }

    private String JSONify(String code, String message){
        JSONObject json = new JSONObject();
        try {
            json.put(CODE_STRING,code);
            json.put(MESSAGE_STRING,message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }


    class UIThread implements Runnable{

        @Override
        public void run() {

            // check for connections
            while (true){
                if(isWait){
                    final long time_diff = Calendar.getInstance().getTimeInMillis() - startReadTime;
//                    displayOnLogScreen("\nTime Diff = "+time_diff);
                    if(time_diff > READ_TIMEOUT){
                        displayOnScreen("Failed. Please try again.");
//                        new Thread(new DisconnectThread()).start();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                configButton.setEnabled(true);
                            }
                        });
                        isWait = false;
                    }
                }
                if(isConnected){
                    final long time_diff = Calendar.getInstance().getTimeInMillis() - startConnectionTime;
//                    displayOnLogScreen("\nTime Diff = "+time_diff);
                    if(time_diff > PING_TIMEOUT){
                        displayOnScreen("[ERROR] Connection Timeout. Exiting...");
//                        new Thread(new DisconnectThread()).start();
                        setCurrentStage(Stage.FIN);
                        break;
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class ConnectThread implements Runnable{
        @Override
        public void run() {
            boolean err = true;
            while (err) {
                try {
                    socket = new Socket(SERVER_IP, SERVER_PORT);

                    output = new PrintWriter(socket.getOutputStream());
                    //input = new BufferedReader(new InputStreamReader(socket.getInputStream()),256);
                    input = new DataInputStream(socket.getInputStream());

                    isConnected = true;
                    startConnectionTime = Calendar.getInstance().getTimeInMillis();
                    new Thread(new ReceiveThread()).start();
                    new Thread(new SendThread()).start();
                    setCurrentStage(Stage.WIFI);
                    displayOnScreen("Please provide WiFi credentials for the kit to connect to internet!");
                    err = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class DisconnectThread implements Runnable{
        @Override
        public void run() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
////                    btnConnect.setText("Connect");
//                    messageBox.setText("Not connected to any server");
//                }
//            });
            startActivity(new Intent(getApplicationContext(),MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        }
    }

    class ReceiveThread implements Runnable{
        @Override
        public void run() {
            startConnectionTime = Calendar.getInstance().getTimeInMillis();
            while (isConnected){
                try{
                    final byte[] data = new byte[256];
//                    final String message = input.read();
                    final int len = input.read(data);

                    if(data != null){
                        String data_string = "";
                        String message = "";

                        double temperature;
                        double heart_rate;
                        double spo;

                        data_string = new String(data, "UTF-8");

                        Log.d("RECEIVED_STRING",data_string);
//                        displayOnLogScreen("Recv: " + data_string);
                        String[] recv_messages = data_string.split(END_OF_MESSAGE,0);
                        Log.d("LENGTH OF SPLIT",recv_messages.length + "");
                        for(String recv_msg:recv_messages){
                            Log.d("SPLIT MESSAGE",recv_msg);

                        }
                        for (String recv_msg:recv_messages) {
                            JSONObject data_json = new JSONObject();
                            Log.d("RECEIVED MESSAGE",recv_msg);
//                            displayOnLogScreen("Split: "+recv_msg);

                            data_json = new JSONObject(recv_msg);
                            Log.d("Received", "" + data_json.toString());

                            String message_code = data_json.getString(CODE_STRING);
                            Log.d("CODE",message_code);

                            switch (message_code){
                                case ERROR_STRING:
                                    message = data_json.getString(MESSAGE_STRING);
                                    switch (message){
                                        case ERR_ACK_STRING:
                                            if(!sentCode.equals(ERR_OK)) {
                                                isWait = false;
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        configButton.setEnabled(true);
                                                    }
                                                });
                                                displayOnScreen("Please try again");
                                            }
                                            break;
                                    }
                                    break;


                                case WIFI_STRING:
                                    message = data_json.getString(MESSAGE_STRING);
                                    displayOnScreen("WiFi configured!");
                                    setCurrentStage(Stage.COMPLETED);
                                    sentCode = ERR_OK;
                                    break;
                                case DISCONNECT_STRING:
                                    message = data_json.getString(MESSAGE_STRING);
                                    displayOnScreen("Done!\nPress \"End Session\" to exit.");
                                    setCurrentStage(Stage.FIN);
                                    sentCode = ERR_OK;
                                    isConnected = false;
                                    break;
                                case PING_STRING:
//                                message = data_json.toString();
                                    if(CONNECTION_CODE.equals(""))
                                        CONNECTION_CODE = data_json.getString(MESSAGE_STRING);
                                    else
                                    {
                                        if(!data_json.getString(MESSAGE_STRING).equals(CONNECTION_CODE)){
//                                        new Thread(new DisconnectThread()).start();
                                            message = "[ERROR] Wrong Connection";
                                            displayOnScreen(message + ". Exiting...");
                                            setCurrentStage(Stage.FIN);
                                        }
                                    }
                                    startConnectionTime = Calendar.getInstance().getTimeInMillis();
                                    String message_value = JSONify(PING_STRING,CONNECTION_CODE);
                                    addToQueue(PING_STRING,message_value);
                                    break;
                                default:
                                    break;

                            }
//                        message += data_string;
                            if(!message.equals("")) {
                                Log.d("STAGE", currentStage.toString());
                                isWait = false;
                            }
                        }


                    } else{
//                        new Thread(new Thread1()).start();
//                        return;
                    }
                } catch (SocketException | UnsupportedEncodingException e){
//                    new Thread(new ConnectThread()).start();
//                    return;
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class SendThread implements Runnable{

        @Override
        public void run() {

            while (isConnected){
                String message = "";
                while(!messageQueue.isEmpty()){
                    message += messageQueue.remove();
                }
                if(!message.equals("")) {
                    message += "\n";
                    output.write(message);
                    output.flush();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
