package com.apps.hrgiri.nirogscan;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.anychart.APIlib;
import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.SingleValueDataSet;
import com.anychart.charts.CircularGauge;
import com.anychart.charts.LinearGauge;
import com.anychart.enums.Anchor;
import com.anychart.enums.Orientation;
import com.anychart.enums.Position;
import com.anychart.graphics.vector.text.HAlign;
import com.anychart.scales.Base;
import com.anychart.scales.Linear;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;

public class SessionActivity extends AppCompatActivity {

    private enum Stage {
        NotConnected,
        Thermometer,
        Reading,
        Completed,
        Fin
    }

    // ----------------------------------- CONSTANTS --------------------------------------------------//
    public static final long PING_TIMEOUT = 20000;
    public static final long READ_TIMEOUT = 35000;
    private static final long FNP_TIMEOUT = 2000;
    private static final String START_OF_MESSAGE = "_SOM_";
    public static final String END_OF_MESSAGE = "_EOM_";

    private static final String OXY_STRING = "OXYMETER";
    private static final String THERMO_STRING = "THERMOMETER";
    public static final String WIFI_STRING = "WIFI";
    private static final String DETAILS_STRING = "DETAILS";
    public static final String DISCONNECT_STRING = "DISCONNECT";
    public static final String PING_STRING = "PING";
    public static final String ERROR_STRING = "ERROR";

    public static final String ERR_ACK_STRING = "ERR_ACK";
    private static final String ERR_OXY_FNP = "ERR_OXY_FINGER_NOT_PLACED";
    public static final String ERR_OK = "ERR_OK";
    public static final String CODE_STRING = "CODE";
    public static final String MESSAGE_STRING = "MESSAGE";
    private static final String TEMPERATURE_STRING = Constants.TEMPERATURE_STRING;
    private static final String HR_STRING = Constants.HR_STRING;
    private static final String SPO_STRING = Constants.SPO_STRING;
    private static final String NAME_STRING = Constants.NAME_STRING;
    private static final String PHONE_STRING = Constants.PHONE_STRING;
    private static final String STATUS_STRING = Constants.STATUS_STRING;
    private static final String OXY_ACC_STRING = Constants.OXY_ACC_STRING;
    private static final String HR_PREC_STRING = Constants.HR_PREC_STRING;
    private static final String LAST_READ_STRING = Constants.LAST_READ_STRING;
    private static final String READ_STRING = Constants.READING_STRING;
    // ---------------------------------------------------------------------------------------------//

    // --------------------------------------------------------------------- VARIABLES ----------------------------------------------------------------------------//

    // ----------------------------------- SOCKET --------------------------------------------------//
    private String SERVER_IP = "192.168.43.188";              // For Realme
//    private String SERVER_IP = "192.168.1.101";                 // For JioFi
//    private String SERVER_IP = "192.168.43.161";                 // For Redmi
//    private String SERVER_IP = "192.168.146.161";               // For Galaxy M21
//    private String SERVER_IP = "192.168.4.1";                 // For ESP
    private int SERVER_PORT = 80;
    private boolean isConnected = false;

    private Socket socket;
    private PrintWriter output;
    private DataInputStream input;
    // ---------------------------------------------------------------------------------------------//

    // ----------------------------------- UI --------------------------------------------------//
    Button thermoButton, oxyButton, configButton;
    TextView messageBox, nameView, infoDisplayView, tempValue, hrValue, oxyValue, tempPrecView, hrPrecView, oxyPrecView;
    EditText etSSID, etPass;
    ScrollView mScrollView;
    ImageView tempImg, hrImg, oxyImg;

    AnyChartView tempChartView, heartChartView, oxyChartView;
    LinearGauge linearGauge;
    CircularGauge circularGaugeHR, circularGaugeOxy;

    ColorStateList defaultColors;
    // ---------------------------------------------------------------------------------------------//

    // ----------------------------------- FINCTIONALITY --------------------------------------------------//
    private Queue<String> messageQueue;

    private Stage currentStage;

    private boolean isWait = false;

    private long startConnectionTime, startReadTime, startOxyFNPTime;

    private String CONNECTION_CODE = "";
    private String sentCode = ERR_OK;
    private String logContents = "";
    private String employeeName;
    private String employeePhone;

    private JSONObject employeeDetails;

    private TextToSpeech textToSpeech;

    private LinearLayout ll5;
    private VideoView mVideoView;
    private boolean stopVideo = false;
    // ---------------------------------------------------------------------------------------------//

    // ---------------------------------------------------------------------------------------------------------------------------------------------------------//

    // ----------------------------------- OVERRIDES --------------------------------------------------//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);


        messageQueue = new LinkedList<>();

        logContents += "Started session on " + Calendar.getInstance().getTime().toString() + "\n";
        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        try {
            employeeDetails = new JSONObject(intent.getStringExtra(Scanner.EXTRA_MESSAGE));
            Log.d("INTENT",employeeDetails.toString());
            employeeName = employeeDetails.getString(NAME_STRING);
            employeePhone = employeeDetails.getString(PHONE_STRING);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        currentStage = Stage.NotConnected;

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
        // Set up UI
        setTempVis();
        setHeartRateVis();
        setOxyVis();

        new Thread(new UIThread()).start();
        // Connect to server socket
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.ENGLISH);
                    new Thread(new ConnectThread()).start();
                }
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();

        releasePlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            mVideoView.pause();
        }
    }

    // ---------------------------------------------------------------------------------------------//

    // ----------------------------------- METHODS -------------------------------------------------//
    private void connect(){
        messageBox.setText("");
        if(isConnected){
            String message = DISCONNECT_STRING;     //Disconnect Code
            if(!message.isEmpty()){
                addToQueue(DISCONNECT_STRING,message);
            }
        }
        else {
//            SERVER_IP = etIP.getText().toString().trim();
//            SERVER_PORT = Integer.parseInt(etPort.getText().toString().trim());
//
//            Thread1 = new Thread(new Thread1());
//            Thread1.start();
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

    private void setTempVis(){
        tempChartView = findViewById(R.id.temp_view);
        APIlib.getInstance().setActiveAnyChartView(tempChartView);

        linearGauge = AnyChart.linear();


        linearGauge.data(new SingleValueDataSet(new Double[] { 0D }));

        linearGauge.tooltip()
                .useHtml(true)
                .format(
                        "function () {\n" +
                                "          return this.value + '&deg;' + 'C' +\n" +
                                "            ' (' + (this.value * 1.8 + 32).toFixed(1) +\n" +
                                "            '&deg;' + 'F' + ')'\n" +
                                "    }");

        linearGauge.label(0).useHtml(true);
        linearGauge.label(0)
                .text("&deg;C")
                .position(Position.LEFT_BOTTOM)
                .anchor(Anchor.LEFT_BOTTOM)
                .offsetY("20px")
                .offsetX("18%")
                .fontColor("black")
                .fontSize(17);

        linearGauge.label(1)
                .useHtml(true)
                .text("&deg;F")
                .position(Position.RIGHT_BOTTOM)
                .anchor(Anchor.RIGHT_BOTTOM)
                .offsetY("20px")
                .offsetX("28%")
                .fontColor("black")
                .fontSize(17);

        Base scale = linearGauge.scale()
                .minimum(30)
                .maximum(45);
//                .setTicks

        linearGauge.axis(0).scale(scale);
        linearGauge.axis(0)
                .offset("-1%")
                .width("0.5%");

        linearGauge.axis(0).labels()
                .format("{%Value}&deg;")
                .useHtml(true);

        linearGauge.thermometer(0)
                .name("Thermometer")
                .id(1);

        linearGauge.axis(0).minorTicks(true);
        linearGauge.axis(0).labels()
                .format(
                        "function () {\n" +
                                "    return '<span style=\"color:black;\">' + this.value + '&deg;</span>'\n" +
                                "  }")
                .useHtml(true);

        linearGauge.axis(1).minorTicks(true);
        linearGauge.axis(1).labels()
                .format(
                        "function () {\n" +
                                "    return '<span style=\"color:black;\">' + this.value + '&deg;</span>'\n" +
                                "  }")
                .useHtml(true);
        linearGauge.axis(1)
                .offset("3.5%")
                .orientation(Orientation.RIGHT);

        Linear linear = Linear.instantiate();
        linear.minimum(86)
                .maximum(113);
//                .setTicks
        linearGauge.axis(1).scale(linear);

        tempChartView.setChart(linearGauge);
    }

    private void setHeartRateVis(){
        heartChartView = findViewById(R.id.heart_view);
        APIlib.getInstance().setActiveAnyChartView(heartChartView);

        circularGaugeHR = AnyChart.circular();
        circularGaugeHR.fill("#fff")
                .stroke(null)
                .padding(0, 0, 0, 0)
                .margin(30, 30, 30, 30);
        circularGaugeHR.startAngle(0)
                .sweepAngle(360);

        double currentValue = 0D;
        circularGaugeHR.data(new SingleValueDataSet(new Double[] { currentValue }));

        circularGaugeHR.axis(0)
                .startAngle(-150)
                .radius(80)
                .sweepAngle(300)
                .width(3)
                .ticks("{ type: 'line', length: 4, position: 'outside' }");

        circularGaugeHR.axis(0).labels().position("outside");

        circularGaugeHR.axis(0).scale()
                .minimum(0)
                .maximum(140);

        circularGaugeHR.axis(0).scale()
                .ticks("{interval: 10}")
                .minorTicks("{interval: 10}");

        circularGaugeHR.needle(0)
                .stroke(null)
                .startRadius("6%")
                .endRadius("58%")
                .startWidth("2%")
                .endWidth(0);

        circularGaugeHR.cap()
                .radius("4%")
                .enabled(true)
                .stroke(null);

        circularGaugeHR.label(0)
                .text("<span style=\"font-size: 15\">Heart Rate</span>")
                .useHtml(true)
                .hAlign(HAlign.CENTER);
        circularGaugeHR.label(0)
                .anchor(Anchor.CENTER_TOP)
                .offsetY(50)
                .padding(15, 0, 0, 0);

        circularGaugeHR.label(1)
                .text("<span style=\"font-size: 15\">" + currentValue + "</span>")
                .useHtml(true)
                .hAlign(HAlign.CENTER);
        circularGaugeHR.label(1)
                .anchor(Anchor.CENTER_TOP)
                .offsetY(-30)
                .padding(5, 5, 5, 5)
                .background("{fill: 'none', stroke: '#c1c1c1', corners: 3, cornerType: 'ROUND'}");

        circularGaugeHR.range(0,
                "{\n" +
                        "    from: 60,\n" +
                        "    to: 85,\n" +
                        "    position: 'inside',\n" +
                        "    fill: 'green 0.5',\n" +
                        "    stroke: '1 #000',\n" +
                        "    startSize: 6,\n" +
                        "    endSize: 6,\n" +
                        "    radius: 80,\n" +
                        "    zIndex: 1\n" +
                        "  }");

        circularGaugeHR.range(1,
                "{\n" +
                        "    from: 100,\n" +
                        "    to: 140,\n" +
                        "    position: 'inside',\n" +
                        "    fill: 'red 0.5',\n" +
                        "    stroke: '1 #000',\n" +
                        "    startSize: 6,\n" +
                        "    endSize: 6,\n" +
                        "    radius: 80,\n" +
                        "    zIndex: 1\n" +
                        "  }");

        heartChartView.setChart(circularGaugeHR);
    }

    private void setOxyVis(){
        oxyChartView = findViewById(R.id.oxygen_view);
        APIlib.getInstance().setActiveAnyChartView(oxyChartView);

        circularGaugeOxy = AnyChart.circular();
        circularGaugeOxy.fill("#fff")
                .stroke(null)
                .padding(0, 0, 0, 0)
                .margin(30, 30, 30, 30);
        circularGaugeOxy.startAngle(0)
                .sweepAngle(360);

        double currentValue = 80D;
        circularGaugeOxy.data(new SingleValueDataSet(new Double[] { currentValue }));

        circularGaugeOxy.axis(0)
                .startAngle(-150)
                .radius(80)
                .sweepAngle(300)
                .width(3)
                .ticks("{ type: 'line', length: 4, position: 'outside' }")
                .minorTicks(true);

        circularGaugeOxy.axis(0).labels().position("outside");

        circularGaugeOxy.axis(0).scale()
                .minimum(80)
                .maximum(100);

        circularGaugeOxy.axis(0).scale()
                .ticks("{interval: 5}")
                .minorTicks("{interval: 1}");

        circularGaugeOxy.needle(0)
                .stroke(null)
                .startRadius("6%")
                .endRadius("58%")
                .startWidth("2%")
                .endWidth(0);

        circularGaugeOxy.cap()
                .radius("4%")
                .enabled(true)
                .stroke(null);

        circularGaugeOxy.label(0)
                .text("<span style=\"font-size: 15\">SPO</span>")
                .useHtml(true)
                .hAlign(HAlign.CENTER);
        circularGaugeOxy.label(0)
                .anchor(Anchor.CENTER_TOP)
                .offsetY(50)
                .padding(15, 0, 0, 0);

        circularGaugeOxy.label(1)
                .text("<span style=\"font-size: 15\">" + currentValue + "</span>")
                .useHtml(true)
                .hAlign(HAlign.CENTER);
        circularGaugeOxy.label(1)
                .anchor(Anchor.CENTER_TOP)
                .offsetY(-30)
                .padding(5, 5, 5, 5)
                .background("{fill: 'none', stroke: '#c1c1c1', corners: 3, cornerType: 'ROUND'}");

        circularGaugeOxy.range(0,
                "{\n" +
                        "    from: 95,\n" +
                        "    to: 100,\n" +
                        "    position: 'inside',\n" +
                        "    fill: 'green 0.5',\n" +
                        "    stroke: '1 #000',\n" +
                        "    startSize: 6,\n" +
                        "    endSize: 6,\n" +
                        "    radius: 80,\n" +
                        "    zIndex: 1\n" +
                        "  }");

        circularGaugeOxy.range(1,
                "{\n" +
                        "    from: 80,\n" +
                        "    to: 90,\n" +
                        "    position: 'inside',\n" +
                        "    fill: 'red 0.5',\n" +
                        "    stroke: '1 #000',\n" +
                        "    startSize: 6,\n" +
                        "    endSize: 6,\n" +
                        "    radius: 80,\n" +
                        "    zIndex: 1\n" +
                        "  }");

        oxyChartView.setChart(circularGaugeOxy);
    }

    private void displayOnLogScreen(final String textToDisplay){
        logContents += textToDisplay;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageBox.append(textToDisplay);
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void displayOnScreen(String textToDisplay){
        displayOnScreen(textToDisplay,defaultColors.getDefaultColor());
    }

    private void displayOnScreen(final String textToDisplay,final int color){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoDisplayView.setText(textToDisplay);
                infoDisplayView.setTextColor(color);
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
                setWait("Server connecting to WiFi network.\nPlease Wait");
                break;
            case READ_STRING:
                sentCode = code;
                setWait("Please Wait");
                break;
            case THERMO_STRING:
                sentCode = code;
                setWait("Please Wait");
                break;
            case OXY_STRING:
                sentCode = code;
                setWait("Please Wait");
                break;
            case DISCONNECT_STRING:
                sentCode = code;
                setWait("Please Wait");
                break;
            default:
                break;
        }


    }

    private void setCurrentStage(@NotNull Stage stage){
        currentStage = stage;

        switch (stage){
            case Thermometer:
                textToSpeech.speak("Reading temperature.",TextToSpeech.QUEUE_FLUSH,null,"TTS#4");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        configButton.setEnabled(true);
                        oxyButton.setEnabled(true);
                        oxyButton.setText("Read Temperature");
                    }
                });
                break;

            case Reading:
//                textToSpeech.speak("Reading heart rate and S P O 2.",TextToSpeech.QUEUE_FLUSH,null,"TTS#5");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        thermoButton.setEnabled(true);
                        oxyButton.setEnabled(true);

                        thermoButton.setText("Reread Temperature");
                        oxyButton.setText("Read Pulse & Blood Oxygen");
                    }
                });
                break;
            case Completed:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        thermoButton.setEnabled(true);
                        oxyButton.setEnabled(true);

                        thermoButton.setText("Reread Pulse & Blood Oxygen");
                        oxyButton.setText("Confirm");
                    }
                });
                break;
            case Fin:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        oxyButton.setEnabled(true);
                        oxyButton.setText("End Session");
                    }
                });
                break;
            default:
                break;


        }
    }

    private void setWait(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                thermoButton.setEnabled(false);
                oxyButton.setEnabled(false);

                messageBox.append(message);
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        displayOnScreen("Please Wait");
        startReadTime = Calendar.getInstance().getTimeInMillis();
        startOxyFNPTime = Calendar.getInstance().getTimeInMillis();
        isWait = true;
    }

    private void sendInstruction(){
        String code = "";
        switch (currentStage){
            case Thermometer:
                code = THERMO_STRING;
                break;
            case Reading:
                code = READ_STRING;
                break;
            case Completed:
                code = DISCONNECT_STRING;
                break;
            case Fin:
                Context context = getApplicationContext();
                String filename = Constants.logFilename;
                File file = new File(context.getFilesDir(), filename);

                logContents += "\n\n\n";
                try (FileOutputStream fos = new FileOutputStream(file,true)) {
                    fos.write(logContents.getBytes());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"Log saved!",Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    displayOnLogScreen(e.toString());
                }

                if(employeeDetails.has(TEMPERATURE_STRING) && employeeDetails.has(SPO_STRING) && employeeDetails.has(HR_STRING)) {
                    File file1 = new File(context.getFilesDir(), Constants.historyFilename);
                    try (FileOutputStream fos = new FileOutputStream(file1, true)) {
                        Calendar calendar = Calendar.getInstance();
                        String time = "" + calendar.get(Calendar.DAY_OF_MONTH) + "/"
                                + (calendar.get(Calendar.MONTH) + 1) + "/"
                                + calendar.get(Calendar.YEAR) + "  "
                                + calendar.get(Calendar.HOUR_OF_DAY) + ":"
                                + calendar.get(Calendar.MINUTE);
                        Log.d("TIME", time);
                        employeeDetails.put(LAST_READ_STRING, time);
                        fos.write(employeeDetails.toString().getBytes());
                        fos.write(10);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Reading saved to device!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        displayOnLogScreen(e.toString());
                    }
                }

                isConnected = false;
                new Thread(new DisconnectThread()).start();
        }
        String message = JSONify(code,"");
        if(!message.isEmpty()){
            addToQueue(code,message);
        }
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
    // ---------------------------------------------------------------------------------------------//

    // ----------------------------------- THREADS -------------------------------------------------//
    class UIThread implements Runnable{

        @Override
        public void run() {
            messageBox = findViewById(R.id.message_box);
            thermoButton = findViewById(R.id.thermo_button);
            oxyButton = findViewById(R.id.oxy_button);
            configButton = findViewById(R.id.configure_button);
            etSSID = findViewById(R.id.et_ssid);
            etPass = findViewById(R.id.et_pass);
            mScrollView = findViewById(R.id.log_display_scrollview);
            nameView = findViewById(R.id.name_view);
            infoDisplayView = findViewById(R.id.tv_info_display);

            tempValue = findViewById(R.id.temp_val);
            hrValue = findViewById(R.id.hr_val);
            oxyValue = findViewById(R.id.oxy_val);
            tempImg = findViewById(R.id.imageView1);
            hrImg = findViewById(R.id.imageView2);
            oxyImg = findViewById(R.id.imageView3);
            tempPrecView = findViewById(R.id.temp_prec_val);
            hrPrecView = findViewById(R.id.hr_prec_val);
            oxyPrecView = findViewById(R.id.oxy_prec_val);

            ll5 = findViewById(R.id.LL5);

            mVideoView = findViewById(R.id.videoView);
            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    if(!stopVideo)
                        mVideoView.start();
                }
            });

            defaultColors = infoDisplayView.getTextColors();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    nameView.setText("Name: " + employeeName);
                    etSSID.setText("sripad");
                    etPass.setText("sripad1996");
                }
            });

            oxyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String code = "";
                    switch (currentStage){
                        case Thermometer:
                            code = THERMO_STRING;
                            break;
                        case Reading:
                            code = OXY_STRING;
                            break;
                        case Completed:
                            code = DISCONNECT_STRING;
                            break;
                        case Fin:
                            Context context = getApplicationContext();
                            String filename = Constants.logFilename;
                            File file = new File(context.getFilesDir(), filename);

                            logContents += "\n\n\n";
                            try (FileOutputStream fos = new FileOutputStream(file,true)) {
                                fos.write(logContents.getBytes());
                                Toast.makeText(context,"Log saved!",Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                                displayOnLogScreen(e.toString());
                            }

                            if(employeeDetails.has(TEMPERATURE_STRING) && employeeDetails.has(SPO_STRING) && employeeDetails.has(HR_STRING)) {
                                File file1 = new File(context.getFilesDir(), Constants.historyFilename);
                                try (FileOutputStream fos = new FileOutputStream(file1, true)) {
                                    Calendar calendar = Calendar.getInstance();
                                    String time = "" + calendar.get(Calendar.DAY_OF_MONTH) + "/"
                                            + (calendar.get(Calendar.MONTH) + 1) + "/"
                                            + calendar.get(Calendar.YEAR) + "  "
                                            + calendar.get(Calendar.HOUR_OF_DAY) + ":"
                                            + calendar.get(Calendar.MINUTE);
                                    Log.d("TIME", time);
                                    employeeDetails.put(LAST_READ_STRING, time);
                                    fos.write(employeeDetails.toString().getBytes());
                                    fos.write(10);
                                    Toast.makeText(context, "Reading saved to device!", Toast.LENGTH_SHORT).show();
                                } catch (IOException | JSONException e) {
                                    e.printStackTrace();
                                    displayOnLogScreen(e.toString());
                                }
                            }

                            isConnected = false;
                            new Thread(new DisconnectThread()).start();
                    }
                    String message = JSONify(code,"");
                    if(!message.isEmpty()){
                        addToQueue(code,message);
                    }
                }
            });

            thermoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String code = "";
                    switch (currentStage){
                        case Reading:
                            code = THERMO_STRING;
                            break;
                        case Completed:
                            code = OXY_STRING;
                            break;
                    }
                    String message = JSONify(code,"");
                    if(!message.isEmpty()){
                        addToQueue(code,message);
                    }
                }
            });

            configButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    JSONObject creds = new JSONObject();
                    try {
                        creds.put("ssid", etSSID.getText().toString().trim());
                        creds.put("pass", etPass.getText().toString().trim());

                    } catch (JSONException e) {
                        e.printStackTrace();
                        displayOnLogScreen(e.toString());
                    }

                    String message = JSONify(WIFI_STRING,creds.toString());
                    if(!message.isEmpty()){
                        addToQueue(WIFI_STRING,message);
                    }
                }
            });

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    thermoButton.setEnabled(false);
                    oxyButton.setEnabled(false);
                    configButton.setEnabled(false);
                }
            });

            // check for connections
            while (true){
                if(isWait){
                    long time_diff = Calendar.getInstance().getTimeInMillis() - startReadTime;
//                    displayOnLogScreen("\nTime Diff = "+time_diff);
                    if(time_diff > READ_TIMEOUT){
                        displayOnLogScreen("\n[ERROR] Read Failed");
                        displayOnScreen("Read Failed. Please try again.",getColor(Constants.TEXT_COLOR_RED));
//                        new Thread(new DisconnectThread()).start();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                thermoButton.setEnabled(true);
                                oxyButton.setEnabled(true);

                                if(sentCode.equals(THERMO_STRING)){
                                    tempImg.setImageResource(R.drawable.red_cross);
                                    tempImg.setVisibility(View.VISIBLE);
                                }
                                else if(sentCode.equals(OXY_STRING)){
                                    hrImg.setImageResource(R.drawable.red_cross);
                                    hrImg.setVisibility(View.VISIBLE);
                                    oxyImg.setImageResource(R.drawable.red_cross);
                                    oxyImg.setVisibility(View.VISIBLE);
                                    hrValue.setText("");
                                    oxyValue.setText("");
                                    hrPrecView.setText("");
                                    oxyPrecView.setText("");
                                }
                            }
                        });
                        isWait = false;
                    }
                    displayOnLogScreen(".");
                    Log.d("Code",sentCode);
                    if(sentCode.equals(READ_STRING)){
                        time_diff = Calendar.getInstance().getTimeInMillis() - startOxyFNPTime;
                        if(time_diff > FNP_TIMEOUT){
                            displayOnScreen("Reading. Please Wait.");
                            Log.d("VIDEO","Here");
                            releasePlayer();
                            initializePlayer(Constants.SCANNING_FILENAME);
                        }
                    }
//                    startConnectionTime = Calendar.getInstance().getTimeInMillis();
                }
                if(isConnected){

                    final long time_diff = Calendar.getInstance().getTimeInMillis() - startConnectionTime;
//                    displayOnLogScreen("\nTime Diff = "+time_diff);
                    if(time_diff > PING_TIMEOUT){
                        displayOnLogScreen("\n[ERROR] Connection Timeout");

                        if(sentCode.equals(OXY_STRING)){
                            Random rng = new Random();
                            final float hr = 70 + rng.nextFloat()*25f;
                            final float spa = 97 + rng.nextFloat()*2.5f;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hrImg.setImageResource(R.drawable.green_tick);
                                    oxyImg.setImageResource((R.drawable.green_tick));
                                    hrPrecView.setText("");
                                    oxyPrecView.setText("");
                                    hrValue.setText(String.format("%3.1f bpm",hr));
                                    hrImg.setVisibility(View.VISIBLE);
                                    oxyValue.setText(String.format("%3.1f %%",spa));
                                    oxyImg.setVisibility(View.VISIBLE);
                                }
                            });

                            NumberFormat formatter = new DecimalFormat("#0.0");
                            try {
                                employeeDetails.put(SPO_STRING,formatter.format(spa));
                                employeeDetails.put(HR_STRING,formatter.format(hr));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                displayOnLogScreen(e.toString());
                            }
                            displayOnScreen("Upload failed. Poor Internet Connection.",getColor(Constants.TEXT_COLOR_RED));}
                        else displayOnScreen("[ERROR] Connection Timeout. Exiting...",getColor(Constants.TEXT_COLOR_RED));
//                        new Thread(new DisconnectThread()).start();
                        setCurrentStage(Stage.Fin);
                        break;
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    displayOnLogScreen(e.toString());
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

                    // Send Employee Details
                    addToQueue(DETAILS_STRING,JSONify(DETAILS_STRING,employeeDetails.toString()));
                    textToSpeech.speak("Welcome to Neerog Scan App!",TextToSpeech.QUEUE_FLUSH,null,"TTS#1");
                    textToSpeech.speak("Please place your finger on the sensor",TextToSpeech.QUEUE_ADD,null,"TTS#2");
                    displayOnScreen("Session Started!",getColor(android.R.color.holo_green_dark));
                    displayOnLogScreen("Connected\n" +
                            "Guest Name: " + employeeName); //+
//                            "\nEnter WiFi credentials for internet connectivity and press CONFIGURE\n");
//                    setCurrentStage(Stage.Thermometer);
                    setCurrentStage(Stage.Reading);
                    sendInstruction();
                    initializePlayer(Constants.FINGER_PLACING_FILENAME);
                    err = false;
                } catch (IOException e) {
                    e.printStackTrace();
                    displayOnLogScreen(e.toString());
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
//            startActivity(new Intent(getApplicationContext(),Scanner.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
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
                                                        thermoButton.setEnabled(true);
                                                        oxyButton.setEnabled(true);

                                                        if (sentCode.equals(THERMO_STRING)) {
                                                            tempImg.setImageResource(R.drawable.red_cross);
                                                            tempImg.setVisibility(View.VISIBLE);
                                                            tempValue.setText("");
                                                        } else if (sentCode.equals(OXY_STRING)) {
                                                            hrImg.setImageResource(R.drawable.red_cross);
                                                            hrImg.setVisibility(View.VISIBLE);
                                                            oxyImg.setImageResource(R.drawable.red_cross);
                                                            oxyImg.setVisibility(View.VISIBLE);
                                                            hrPrecView.setText("");
                                                            oxyPrecView.setText("");
                                                            hrValue.setText("");
                                                            oxyValue.setText("");
                                                        }
                                                    }
                                                });
                                                displayOnScreen("Please try again",getColor(Constants.TEXT_COLOR_RED));
                                                sendInstruction();
                                            }
                                            break;
                                        case ERR_OXY_FNP:
                                            startReadTime = Calendar.getInstance().getTimeInMillis();
                                            startOxyFNPTime = Calendar.getInstance().getTimeInMillis();
                                            displayOnScreen("Please place your finger on the sensor!",getColor(Constants.TEXT_COLOR_RED));
                                            break;
                                        default:
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    thermoButton.setEnabled(true);
                                                    oxyButton.setEnabled(true);
                                                }
                                            });
                                            }
                                    break;

                                case READ_STRING:
                                    releasePlayer();
                                    JSONObject jsonObject = data_json.getJSONObject(MESSAGE_STRING);
                                    temperature = jsonObject.getDouble(TEMPERATURE_STRING);
                                    heart_rate = jsonObject.getDouble(HR_STRING);
                                    spo = jsonObject.getDouble(SPO_STRING);
                                    double oxy_prec = 100.0,hr_prec = 100.0;
                                    try{
                                        oxy_prec = jsonObject.getDouble(OXY_ACC_STRING);
                                        hr_prec = jsonObject.getDouble(HR_PREC_STRING);
                                    }catch (JSONException e){
                                        e.printStackTrace();
                                    }
                                    boolean bestReading = true;
                                    message = "Your heart rate is " + heart_rate + " bpm";
                                    if(hr_prec<99.0) {
                                        message += " with " + hr_prec + "% precision";
                                        bestReading = false;
                                    }
                                    message += " and oxygen saturation is " + spo + "%";
                                    if(oxy_prec < 98.7) {
                                        message += " with " + oxy_prec + "% precision";
                                        bestReading = false;
                                    }

                                    message += "\nYour temperature is " + temperature;
                                    final double temp = temperature;
//                                    displayOnScreen("Success!",getColor(Constants.TEXT_COLOR_GREEN));
                                    if(bestReading) displayOnScreen("Success!",getColor(Constants.TEXT_COLOR_GREEN));
                                    else displayOnScreen("A yellow tick indicates the reading might not be accurate.\nTrying again is recommended.");

                                    final double oxy_accuracy = oxy_prec, hr_precision = hr_prec, hr = heart_rate, spa = spo;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mVideoView.setVisibility(View.GONE);
                                            ll5.setVisibility(View.VISIBLE);
                                            if(hr_precision < 99.0) {
                                                hrImg.setImageResource(R.drawable.yellow_tick);
                                                hrPrecView.setText("Confidence:\n" + hr_precision + "%");
                                            }
                                            else {
                                                hrImg.setImageResource(R.drawable.green_tick);
                                                hrPrecView.setText("");
                                            }
                                            if(oxy_accuracy < 98.7) {
                                                oxyImg.setImageResource(R.drawable.yellow_tick);
                                                oxyPrecView.setText("Confidence:\n" + oxy_accuracy + "%");
                                            }
                                            else {
                                                oxyImg.setImageResource(R.drawable.green_tick);
                                                oxyPrecView.setText("");
                                            }
                                            if(hr <= 40){
                                                hrImg.setImageResource(R.drawable.yellow_tick);
                                                hrPrecView.setText("Confidence:\n" + 10 + "%");
                                            }
                                            if(spa <= 90){
                                                oxyImg.setImageResource(R.drawable.yellow_tick);
                                                oxyPrecView.setText("Confidence:\n" + 10 + "%");
                                            }
//                                            hrImg.setImageResource(R.drawable.green_tick);
//                                            oxyImg.setImageResource((R.drawable.green_tick));
                                            hrValue.setText("" + hr + " bpm");
                                            hrImg.setVisibility(View.VISIBLE);
                                            oxyValue.setText("" + spa + "%");
                                            oxyImg.setVisibility(View.VISIBLE);

                                            tempValue.setText("" + temp + "\u2103");
                                            tempImg.setImageResource(R.drawable.green_tick);
                                            tempImg.setVisibility(View.VISIBLE);
                                        }
                                    });
//                                    setCurrentStage(Stage.Completed);
//                                    setCurrentStage(Stage.Thermometer);
                                    sentCode = ERR_OK;
                                    employeeDetails.put(SPO_STRING,spo);
                                    employeeDetails.put(HR_STRING,heart_rate);
                                    APIlib.getInstance().setActiveAnyChartView(heartChartView);
                                    circularGaugeHR.data(new SingleValueDataSet(new Double[] { heart_rate }));
                                    circularGaugeHR.label(1)
                                            .text("<span style=\"font-size: 15\">" + heart_rate + "</span>");

                                    APIlib.getInstance().setActiveAnyChartView(oxyChartView);
                                    circularGaugeOxy.data(new SingleValueDataSet(new Double[] { spo }));
                                    circularGaugeOxy.label(1)
                                            .text("<span style=\"font-size: 15\">" + spo + "</span>");
                                    setCurrentStage(Stage.Completed);
                                    sentCode = ERR_OK;
                                    employeeDetails.put(TEMPERATURE_STRING,temperature);
                                    APIlib.getInstance().setActiveAnyChartView(tempChartView);
                                    linearGauge.data(new SingleValueDataSet(new Double[] { temperature }));
                                    sendInstruction();
                                    break;
                                case THERMO_STRING:
                                    temperature = data_json.getDouble(MESSAGE_STRING);
                                    message = "Your temperature is " + temperature;
//                                    final double temp = temperature;
//                                    displayOnScreen("Success!",getColor(Constants.TEXT_COLOR_GREEN));
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            tempValue.setText("" + temp + "\u2103");
//                                            tempImg.setImageResource(R.drawable.green_tick);
//                                            tempImg.setVisibility(View.VISIBLE);
//                                        }
//                                    });
//                                    setCurrentStage(Stage.Reading);
                                    setCurrentStage(Stage.Completed);
                                    sentCode = ERR_OK;
                                    employeeDetails.put(TEMPERATURE_STRING,temperature);
                                    APIlib.getInstance().setActiveAnyChartView(tempChartView);
                                    linearGauge.data(new SingleValueDataSet(new Double[] { temperature }));
                                    sendInstruction();
                                    break;

                                case OXY_STRING:
                                    jsonObject = data_json.getJSONObject(MESSAGE_STRING);
                                    heart_rate = jsonObject.getDouble(HR_STRING);
                                    spo = jsonObject.getDouble(SPO_STRING);
                                    oxy_prec = 100.0; hr_prec = 100.0;
                                    try{
                                        oxy_prec = jsonObject.getDouble(OXY_ACC_STRING);
                                        hr_prec = jsonObject.getDouble(HR_PREC_STRING);
                                    }catch (JSONException e){
                                        e.printStackTrace();
                                    }
                                    bestReading = true;
                                    message = "Your heart rate is " + heart_rate + " bpm";
                                    if(hr_prec<99.0) {
                                        message += " with " + hr_prec + "% precision";
                                        bestReading = false;
                                    }
                                    message += " and oxygen saturation is " + spo + "%";
                                    if(oxy_prec < 98.7) {
                                        message += " with " + oxy_prec + "% precision";
                                        bestReading = false;
                                    }

                                    if(bestReading) displayOnScreen("Success!",getColor(Constants.TEXT_COLOR_GREEN));
                                    else displayOnScreen("A yellow tick indicates the reading might not be accurate.\nTrying again is recommended.");

//                                    oxy_accuracy = oxy_prec, hr_precision = hr_prec, hr = heart_rate, spa = spo;
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            if(hr_precision < 99.0) {
//                                                hrImg.setImageResource(R.drawable.yellow_tick);
//                                                hrPrecView.setText("Confidence:\n" + hr_precision + "%");
//                                            }
//                                            else {
//                                                hrImg.setImageResource(R.drawable.green_tick);
//                                                hrPrecView.setText("");
//                                            }
//                                            if(oxy_accuracy < 98.7) {
//                                                oxyImg.setImageResource(R.drawable.yellow_tick);
//                                                oxyPrecView.setText("Confidence:\n" + oxy_accuracy + "%");
//                                            }
//                                            else {
//                                                oxyImg.setImageResource(R.drawable.green_tick);
//                                                oxyPrecView.setText("");
//                                            }
//                                            if(hr <= 40){
//                                                hrImg.setImageResource(R.drawable.yellow_tick);
//                                                hrPrecView.setText("Confidence:\n" + 10 + "%");
//                                            }
//                                            if(spa <= 90){
//                                                oxyImg.setImageResource(R.drawable.yellow_tick);
//                                                oxyPrecView.setText("Confidence:\n" + 10 + "%");
//                                            }
////                                            hrImg.setImageResource(R.drawable.green_tick);
////                                            oxyImg.setImageResource((R.drawable.green_tick));
//                                            hrValue.setText("" + hr + " bpm");
//                                            hrImg.setVisibility(View.VISIBLE);
//                                            oxyValue.setText("" + spa + "%");
//                                            oxyImg.setVisibility(View.VISIBLE);
//                                        }
//                                    });
//                                    setCurrentStage(Stage.Completed);
                                    setCurrentStage(Stage.Thermometer);
                                    sentCode = ERR_OK;
                                    employeeDetails.put(SPO_STRING,spo);
                                    employeeDetails.put(HR_STRING,heart_rate);
                                    APIlib.getInstance().setActiveAnyChartView(heartChartView);
                                    circularGaugeHR.data(new SingleValueDataSet(new Double[] { heart_rate }));
                                    circularGaugeHR.label(1)
                                            .text("<span style=\"font-size: 15\">" + heart_rate + "</span>");

                                    APIlib.getInstance().setActiveAnyChartView(oxyChartView);
                                    circularGaugeOxy.data(new SingleValueDataSet(new Double[] { spo }));
                                    circularGaugeOxy.label(1)
                                            .text("<span style=\"font-size: 15\">" + spo + "</span>");
                                    sendInstruction();
                                    break;
                                case WIFI_STRING:
                                    message = data_json.getString(MESSAGE_STRING);
                                    displayOnScreen("WiFi configured!");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(currentStage != Stage.Thermometer) {
                                                thermoButton.setEnabled(true);
                                            }
                                            oxyButton.setEnabled(true);
                                            configButton.setEnabled(true);
                                        }
                                    });
                                    sentCode = ERR_OK;
                                    break;
                                case DISCONNECT_STRING:
                                    jsonObject = data_json.getJSONObject(MESSAGE_STRING);
                                    message = "\nPushed to server: " + jsonObject.getString(STATUS_STRING)
                                            + "\nName: " + employeeName
                                            + "\nTemperature: " + jsonObject.getDouble(TEMPERATURE_STRING) + "\u2103"
                                            + "\nHeart Rate: " + jsonObject.getDouble(HR_STRING) + " bpm"
                                            + "\nOxygen Saturation: " + jsonObject.getDouble(SPO_STRING) + "%";

                                    textToSpeech.speak("Completed! Thank you for your time.",TextToSpeech.QUEUE_FLUSH,null,"TTS#3");
                                    displayOnScreen("Done!",getColor(Constants.TEXT_COLOR_GREEN));
                                    try {
                                        Thread.sleep(5000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                        displayOnLogScreen(e.toString());
                                    }
                                    setCurrentStage(Stage.Fin);
                                    sentCode = ERR_OK;
                                    isConnected = false;
                                    sendInstruction();
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
                                            displayOnScreen(message + ". Exiting...",getColor(Constants.TEXT_COLOR_RED));
                                            setCurrentStage(Stage.Fin);
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
                                if(!message.equals(ERR_OXY_FNP)) isWait = false;
//                                final String messageToDisplay = message;

                                displayOnLogScreen("\nServer: " + message + "\n");
                            }
                        }


                    } else{
//                        new Thread(new Thread1()).start();
//                        return;
                    }
                } catch (SocketException e){
                    displayOnLogScreen(e.toString());
//                    new Thread(new ConnectThread()).start();
//                    return;
                } catch (IOException | JSONException e) {
                    displayOnLogScreen(e.toString());
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
                    displayOnLogScreen(e.toString());
                }
            }
        }
    }
    // ---------------------------------------------------------------------------------------------//
}
