package ru.mirea.rulev.httpurlconnection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import ru.mirea.rulev.httpurlconnection.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkinfo = null;
                if (connectivityManager != null) {
                    networkinfo = connectivityManager.getActiveNetworkInfo();
                }
                if (networkinfo != null && networkinfo.isConnected()) {

                    new DownloadPageTask().execute("https://ipinfo.io/json"); // запуск нового потока

                } else {
                    Toast.makeText(getBaseContext(), "Нет интернета", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private class DownloadPageTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            binding.button.setText("Loading...");
        }
        @Override
        protected String doInBackground(String... urls) {
            try {
                JSONObject responseJson = new JSONObject(downloadIpInfo(urls[0]));
                String[] coord = responseJson.getString("loc").split(",");
                return downloadIpInfo(String.format("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current_weather=true", coord[0], coord[1]));

            } catch (IOException e) {
                e.printStackTrace();
                return "error";
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        protected void onPostExecute(String result) {
            JSONObject jsonObject = null;
            String temp;
            String wind;
            try {
                jsonObject = new JSONObject(result);
                JSONObject weather = new JSONObject(String.valueOf(jsonObject.getJSONObject("current_weather")));
                temp = weather.getString("temperature");
                wind = weather.getString("windspeed");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            binding.temp.setText(String.format("temperature -  %s", temp));
            binding.wind.setText(String.format("windspeed -  %s", wind));

            binding.button.setText("connect");
            super.onPostExecute(result);
        }
    }
    private String downloadIpInfo(String address) throws IOException {
        InputStream inputStream = null;
        String data = "";
        try {
            URL url = new URL(address);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(100000);
            connection.setConnectTimeout(100000);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 200 OK
                inputStream = connection.getInputStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int read = 0;
                while ((read = inputStream.read()) != -1) {
                    bos.write(read); }
                bos.close();
                data = bos.toString();
            } else {
                data = connection.getResponseMessage()+". Error Code: " + responseCode;
            }
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return data;
    }
}