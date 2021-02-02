package com.apps.hrgiri.nirogscan;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ViewLogActivity extends AppCompatActivity {

    TextView textView;
    ScrollView logScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_log);

        textView = findViewById(R.id.log_view);
        logScroll = findViewById(R.id.log_scroll);

        Context context = getApplicationContext();
        String filename = Constants.logFilename;
        FileInputStream fis = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            fis = context.openFileInput(filename);
            InputStreamReader inputStreamReader =
                new InputStreamReader(fis, StandardCharsets.UTF_8);

            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line).append('\n');
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            textView.setText(e.toString());
        } catch (IOException e) {
            // Error occurred when opening raw file for reading.
            e.printStackTrace();
            textView.setText(e.toString());
        } finally {
            final String contents = stringBuilder.toString();
            textView.setText(contents);
            logScroll.fullScroll(ScrollView.FOCUS_DOWN);
        }
    }
}
