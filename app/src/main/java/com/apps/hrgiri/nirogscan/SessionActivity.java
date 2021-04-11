package com.apps.hrgiri.nirogscan;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
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

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.apps.hrgiri.nirogscan.Constants.BATTERY_CHARACTERISTIC_UUID;
import static com.apps.hrgiri.nirogscan.Constants.FNP_CHARACTERISTIC_UUID;
import static com.apps.hrgiri.nirogscan.Constants.READ_CHARACTERISTIC_UUID;
import static com.apps.hrgiri.nirogscan.Constants.SERVICE_UUID;

public class SessionActivity extends AppCompatActivity implements BtControllerCallback {

    // ----------------------------------- CONSTANTS --------------------------------------------------//
    public static final long PING_TIMEOUT = 20000;
    public static final long READ_TIMEOUT = 35000;
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

    private static final String WELCOME_UTTERANCE_ID = "TTS#1";
    private static final String FINGER_PLACE_UTTERANCE_ID = "TTS#2";
    private static final String READING_UTTERANCE_ID = "TTS#3";
    private static final String COMPLETION_UTTERANCE_ID = "TTS#4";
    // ---------------------------------------------------------------------------------------------//

    // --------------------------------------------------------------------- VARIABLES ----------------------------------------------------------------------------//
    // ----------------------------------- UI --------------------------------------------------//
    Button thermoButton, oxyButton, configButton;
    TextView messageBox, nameView, infoDisplayView, tempValue, hrValue, oxyValue, tempPrecView, hrPrecView, oxyPrecView, batteryTextView;
    EditText etSSID, etPass;
    ScrollView mScrollView;
    ImageView tempImg, hrImg, oxyImg, batteryView;
    LinearLayout ll5;
    VideoView mVideoView;

    AnyChartView tempChartView, heartChartView, oxyChartView;
    LinearGauge linearGauge;
    CircularGauge circularGaugeHR, circularGaugeOxy;

    ColorStateList defaultColors;
    // ---------------------------------------------------------------------------------------------//

    // ----------------------------------- FINCTIONALITY --------------------------------------------------//
    private BtController btController;

    private String logContents = "";
    private String employeeName;
    private String employeePhone;

    private JSONObject employeeDetails;

    private TextToSpeech textToSpeech;
    private boolean fingerNotPlaced = true;

    private boolean stopVideo = false;

    private boolean isReadingCompleted = false;
    // ---------------------------------------------------------------------------------------------//

    // ---------------------------------------------------------------------------------------------------------------------------------------------------------//

    // ----------------------------------- OVERRIDES --------------------------------------------------//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        logContents += "Started session on " + Calendar.getInstance().getTime().toString() + "\n";

        // Get the singleton BtController object and associate it with this activity
        btController = BtController.getInstance();
        btController.setCallback(this);

        // Get the Intent that carries the employee info
        Intent intent = getIntent();
        try {
            employeeDetails = new JSONObject(intent.getStringExtra(Scanner.EXTRA_MESSAGE));
            Log.d("INTENT",employeeDetails.toString());
            employeeName = employeeDetails.getString(NAME_STRING);
            employeePhone = employeeDetails.getString(PHONE_STRING);
        } catch (JSONException e) {
            e.printStackTrace();
        }

//        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
//        Network activeWifiNetwork = null;
//        for (Network network : connectivityManager.getAllNetworks()) {
//            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
//            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
//                if(networkInfo.isConnected()) activeWifiNetwork = network;
//                break;
//            }
//        }
//        connectivityManager.bindProcessToNetwork(activeWifiNetwork);


        // For displaying charts (currently hidden)
        setTempVis();
        setHeartRateVis();
        setOxyVis();

        // Set up UI
        messageBox = findViewById(R.id.message_box);
        thermoButton = findViewById(R.id.thermo_button);
        oxyButton = findViewById(R.id.oxy_button);
        configButton = findViewById(R.id.configure_button);
        etSSID = findViewById(R.id.et_ssid);
        etPass = findViewById(R.id.et_pass);
        mScrollView = findViewById(R.id.log_display_scrollview);
        nameView = findViewById(R.id.name_view);
        infoDisplayView = findViewById(R.id.tv_info_display);
        batteryTextView = findViewById(R.id.tv_battery);
        batteryView = findViewById(R.id.iv_battery);

        tempValue = findViewById(R.id.temp_val);
        hrValue = findViewById(R.id.hr_val);
        oxyValue = findViewById(R.id.oxy_val);
        tempImg = findViewById(R.id.imageView1);
        hrImg = findViewById(R.id.imageView2);
        oxyImg = findViewById(R.id.imageView3);
        tempPrecView = findViewById(R.id.temp_prec_val);
        hrPrecView = findViewById(R.id.hr_prec_val);
        oxyPrecView = findViewById(R.id.oxy_prec_val);

        ll5 = findViewById(R.id.LL5);       // Linear Layout that encloses the result views

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
            }
        });

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.ENGLISH);
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String s) {

                        }

                        @Override
                        public void onDone(String s) {
                            switch (s){
                                case FINGER_PLACE_UTTERANCE_ID:
                                    if(!isReadingCompleted && fingerNotPlaced)
                                        textToSpeech.speak("Please place your finger on the sensor",TextToSpeech.QUEUE_ADD,null,FINGER_PLACE_UTTERANCE_ID);
                            }
                        }

                        @Override
                        public void onError(String s) {

                        }
                    });
                    // Start the reading process
                    requestReading();
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

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (mVideoView != null)
                if(mVideoView.isPlaying())
                    mVideoView.resume();
        }
    }

    @Override
    public void onBtDisconnected() {
        btController.startBleScan();
    }

    @Override
    public void onGattReady(){
        byte[] writeValue = {1};
        btController.btGatt.getService(UUID.fromString(SERVICE_UUID)).getCharacteristic(UUID.fromString(READ_CHARACTERISTIC_UUID)).setValue(writeValue);
        btController.btGatt.writeCharacteristic(
                btController.btGatt.getService(UUID.fromString(SERVICE_UUID))
                        .getCharacteristic(UUID.fromString(READ_CHARACTERISTIC_UUID)));
    }
    @Override
    public void onCharacteristicReadSuccess(BluetoothGattCharacteristic characteristic){
    }

    @Override
    public void onCharacteristicWriteSuccess(BluetoothGattCharacteristic characteristic) {
    }

    @Override
    public void onCharacteristicNotification(BluetoothGattCharacteristic characteristic) {
        try {
            String message = "";
            String data = characteristic.getStringValue(0);
            Log.i("BluetoothGattCallback", "Read characteristic " + characteristic.getUuid() + ":\t" + data);

            switch (characteristic.getUuid().toString()) {
                case BATTERY_CHARACTERISTIC_UUID:
                    Log.i("BluetoothReceive", "Read characteristic " + characteristic.getUuid() + ":\t" + byteToHex(characteristic.getValue()));
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
                case FNP_CHARACTERISTIC_UUID:
                    Log.i("BluetoothReceive", "Read characteristic " + characteristic.getUuid() + ":\t" + byteToHex(characteristic.getValue()));
                    if(characteristic.getValue()[0] == 1){
                        fingerNotPlaced = false;
                        textToSpeech.speak("Reading. Please Wait.",TextToSpeech.QUEUE_FLUSH,null,READING_UTTERANCE_ID);
                        displayOnScreen("Reading. Please Wait.");
                        Log.d("VIDEO","Here");
                        releasePlayer();
                        initializePlayer(Constants.SCANNING_FILENAME);
                    }
                    else{
                        fingerNotPlaced = true;
                        textToSpeech.speak("Please place your finger on the sensor",TextToSpeech.QUEUE_FLUSH,null,FINGER_PLACE_UTTERANCE_ID);
                        displayOnScreen("Please place your finger on the sensor!",getColor(Constants.TEXT_COLOR_RED));
                        releasePlayer();
                        initializePlayer(Constants.FINGER_PLACING_FILENAME);
                    }
                    break;
                case READ_CHARACTERISTIC_UUID:
                    String[] recv_messages = data.split(",", 0);        // The received string is a csv
                    Log.d("LENGTH OF SPLIT", recv_messages.length + "");
                    if(recv_messages.length == 5) {
                        isReadingCompleted = true;
                        releasePlayer();
                        double temperature = Float.parseFloat(recv_messages[4]);
                        double heart_rate = Float.parseFloat(recv_messages[0]);
                        double spo = Float.parseFloat(recv_messages[2]);
                        double oxy_prec = Float.parseFloat(recv_messages[3]), hr_prec = Float.parseFloat(recv_messages[1]);

                        boolean bestReading = true;
                        message = "Your heart rate is " + heart_rate + " bpm";
                        if (hr_prec < 99.0) {
                            message += " with " + hr_prec + "% precision";
                            bestReading = false;
                        }
                        message += " and oxygen saturation is " + spo + "%";
                        if (oxy_prec < 98.7) {
                            message += " with " + oxy_prec + "% precision";
                            bestReading = false;
                        }

                        message += "\nYour temperature is " + temperature;
                        displayOnLogScreen(message);

                        if (bestReading)
                            displayOnScreen("Success!", getColor(Constants.TEXT_COLOR_GREEN));
                        else
                            displayOnScreen("A yellow tick indicates the reading might not be accurate.\nTrying again is recommended.");

                        final double oxy_accuracy = oxy_prec, hr_precision = hr_prec, hr = heart_rate, spa = spo, temp = temperature;

                        // Trim floats for displaying
                        DecimalFormat df = new DecimalFormat("0.00");
                        df.setMaximumFractionDigits(2);
                        final String oxy_prec1 = df.format(oxy_prec), hr_prec1 = df.format(hr_prec), heart_rate1 = df.format(heart_rate), spo1 = df.format(spo), temperature1 = df.format(temperature);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mVideoView.setVisibility(View.GONE);
                                ll5.setVisibility(View.VISIBLE);

                                if (hr_precision < 99.0) {
                                    hrImg.setImageResource(R.drawable.yellow_tick);
                                    hrPrecView.setText("Confidence:\n" + hr_prec1 + "%");
                                } else {
                                    hrImg.setImageResource(R.drawable.green_tick);
                                    hrPrecView.setText("");
                                }
                                if (oxy_accuracy < 98.7) {
                                    oxyImg.setImageResource(R.drawable.yellow_tick);
                                    oxyPrecView.setText("Confidence:\n" + oxy_prec1 + "%");
                                } else {
                                    oxyImg.setImageResource(R.drawable.green_tick);
                                    oxyPrecView.setText("");
                                }
                                if (hr <= 40) {
                                    hrImg.setImageResource(R.drawable.yellow_tick);
                                    hrPrecView.setText("Confidence:\n" + 10 + "%");
                                }
                                if (spa <= 90) {
                                    oxyImg.setImageResource(R.drawable.yellow_tick);
                                    oxyPrecView.setText("Confidence:\n" + 10 + "%");
                                }
                                //                                            hrImg.setImageResource(R.drawable.green_tick);
                                //                                            oxyImg.setImageResource((R.drawable.green_tick));
                                hrValue.setText("" + heart_rate1 + " bpm");
                                hrImg.setVisibility(View.VISIBLE);
                                oxyValue.setText("" + spo1 + "%");
                                oxyImg.setVisibility(View.VISIBLE);

                                tempValue.setText("" + temperature1 + "\u2103");
                                tempImg.setImageResource(R.drawable.green_tick);
                                tempImg.setVisibility(View.VISIBLE);
                            }
                        });

                        employeeDetails.put(SPO_STRING, spo1);
                        employeeDetails.put(HR_STRING, heart_rate1);
                        employeeDetails.put(TEMPERATURE_STRING, temperature1);

                        // For displaying charts (currently hidden)
                        APIlib.getInstance().setActiveAnyChartView(heartChartView);
                        circularGaugeHR.data(new SingleValueDataSet(new Double[]{heart_rate}));
                        circularGaugeHR.label(1)
                                .text("<span style=\"font-size: 15\">" + heart_rate + "</span>");

                        APIlib.getInstance().setActiveAnyChartView(oxyChartView);
                        circularGaugeOxy.data(new SingleValueDataSet(new Double[]{spo}));
                        circularGaugeOxy.label(1)
                                .text("<span style=\"font-size: 15\">" + spo + "</span>");
                        APIlib.getInstance().setActiveAnyChartView(tempChartView);
                        linearGauge.data(new SingleValueDataSet(new Double[]{temperature}));

                        textToSpeech.speak("Completed! Thank you for your time.", TextToSpeech.QUEUE_FLUSH, null, COMPLETION_UTTERANCE_ID);
                        displayOnScreen("Done!", getColor(Constants.TEXT_COLOR_GREEN));

                        // Save in File
                        Context context = getApplicationContext();
                        String filename = Constants.logFilename;
                        File file = new File(context.getFilesDir(), filename);

                        logContents += "\n\n\n";
                        try (FileOutputStream fos = new FileOutputStream(file, true)) {
                            fos.write(logContents.getBytes());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Log saved!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                            displayOnLogScreen(e.toString());
                        }

                        if (employeeDetails.has(TEMPERATURE_STRING) && employeeDetails.has(SPO_STRING) && employeeDetails.has(HR_STRING)) {
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

                        // Upload on to the web and return after upload complete
                        // Instantiate the RequestQueue.
                        RequestQueue queue = Volley.newRequestQueue(this);
                        String url = "https://things.ubidots.com/api/v1.6/devices/esp?token=BBFF-qfdAOBNngUyWFyYXAnWK3yvyD6oO4i";
                        JSONObject jsonBody = new JSONObject();
                        jsonBody.put("temperature", temperature);
                        jsonBody.put("heartrate", heart_rate);
                        jsonBody.put("oxygen_level", spo);
                        final String requestBody = jsonBody.toString();
                        // Request a string response from the provided URL.
                        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        Log.i("HttpResponse", "Response is: "+ response);
                                        Toast.makeText(getApplicationContext(),"Uploaded to the web!",Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.i("HttpResponse", "That didn't work!");
                                error.printStackTrace();
                                Toast.makeText(getApplicationContext(),"Failed to upload to the web. Cached.",Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }){
                            @Override
                            public String getBodyContentType() {
                                return "application/json; charset=utf-8";
                            }

                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                return super.getHeaders();
                            }

                            @Override
                            public byte[] getBody() {
                                try {
                                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                                } catch (UnsupportedEncodingException uee) {
                                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                                    return null;
                                }
                            }

                            @Override
                            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                                String responseString = "";
                                if (response != null) {
                                    responseString = String.valueOf(response.statusCode);
                                    // can get more details such as response.headers
                                }
                                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                            }
                        };

                        // Add the request to the RequestQueue.
                        queue.add(stringRequest);

                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            displayOnLogScreen(e.toString());
                        }

//                        finish();
                    }
                    else {
                        Log.e("Received","Invalid format");
                    }
                    break;
            }
        }
        catch (JSONException e){
            displayOnLogScreen(e.toString());
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------------------------------//

    // ----------------------------------- METHODS -------------------------------------------------//
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
        textToSpeech.speak("Please place your finger on the sensor",TextToSpeech.QUEUE_ADD,null,FINGER_PLACE_UTTERANCE_ID);
        displayOnScreen("Session Started!",getColor(android.R.color.holo_green_dark));
        displayOnLogScreen("Connected\n" +
                "Guest Name: " + employeeName); //+

        initializePlayer(Constants.FINGER_PLACING_FILENAME);

        // A write starts the reading process and when the process is done, the on
        byte[] writeValue = {1};
        btController.btGatt.getService(UUID.fromString(SERVICE_UUID)).getCharacteristic(UUID.fromString(READ_CHARACTERISTIC_UUID)).setValue(writeValue);
        btController.btGatt.writeCharacteristic(
                btController.btGatt.getService(UUID.fromString(SERVICE_UUID))
                        .getCharacteristic(UUID.fromString(READ_CHARACTERISTIC_UUID)));
    }

    private String byteToHex(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }
    // ---------------------------------------------------------------------------------------------//
}
