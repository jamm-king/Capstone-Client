package com.example.motionsensorapp;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import com.google.gson.Gson;

public class HttpClient {
    // static final String serverUrl = "http://waity.pe.kr:5000";
    static final String serverUrl = "http://192.168.219.105:5000";

    public void sendMotionSensorData(List<MotionSensorData> motionSensorData) {
        try {
            URL url = new URL(serverUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set up the connection
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            Gson gson = new Gson();
            String jsonData = gson.toJson(motionSensorData);

            // Write the motion sensor data (now in JSON format) to the output stream
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonData.getBytes());
            }

            // Get the response from the server (optional)
            int responseCode = connection.getResponseCode();
            System.out.println("Server response code: " + responseCode);

            // Handle the response if needed

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendVideo(String filePath) {
        try {
            URL url = new URL(serverUrl + "/video");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set up the connection
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/octet-stream");

            // Create a DataOutputStream to write the file content to the connection output stream
            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                // Specify the file you want to upload
                File file = new File(filePath);
                FileInputStream fis = new FileInputStream(file);

                // Read the file content into a byte array
                byte[] buffer = new byte[4096];
                int bytesRead;

                // Write the file content to the connection output stream
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }

                // Close the file input stream
                fis.close();
            }

            // Get the response code from the server
            int responseCode = connection.getResponseCode();
            System.out.println("Server Response Code: " + responseCode);

            // Close the connection
            connection.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
