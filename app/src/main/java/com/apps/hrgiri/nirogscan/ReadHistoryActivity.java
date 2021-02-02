package com.apps.hrgiri.nirogscan;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.ScrollView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class ReadHistoryActivity extends AppCompatActivity {

    ListView patientListView;
    ArrayList<HashMap> list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_history);

        patientListView = findViewById(R.id.listview_patient);

        Context context = getApplicationContext();
        String filename = Constants.historyFilename;
        FileInputStream fis = null;
        try {
            fis = context.openFileInput(filename);
            InputStreamReader inputStreamReader =
                    new InputStreamReader(fis, StandardCharsets.UTF_8);

            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            list = new ArrayList<HashMap>();
            while (line != null) {
                Log.d("LIST",line);
                JSONObject details = new JSONObject(line);
                HashMap map = new HashMap();
                map.put(Constants.FIRST_COLUMN,details.getString(Constants.NAME_STRING));
                map.put(Constants.SECOND_COLUMN,"Last Read:\n" + details.getString(Constants.LAST_READ_STRING));
                map.put(Constants.THIRD_COLUMN,"Temperature: " + details.getString(Constants.TEMPERATURE_STRING)  + "\u2103 \n"
                                            + "Heart Rate: " + details.getString(Constants.HR_STRING) + "bpm\n"
                                            + "SPO: " + details.getString(Constants.SPO_STRING) + "%");
                list.add(map);

                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException | JSONException e) {
            // Error occurred when opening raw file for reading.
            e.printStackTrace();
        } finally {
            ArrayList<HashMap> invertedList = new ArrayList<HashMap>();
            Log.d("LIST","Size = " + list.size());
            for(int i=list.size()-1; i >= 0; i--){
                invertedList.add(list.get(i));
                Log.d("LIST","Element added at " + i);
            }
            CustomAdapter adapter = new CustomAdapter(invertedList,this);
            patientListView.setAdapter(adapter);
        }
    }
}
