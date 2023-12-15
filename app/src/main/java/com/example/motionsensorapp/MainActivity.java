package com.example.motionsensorapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button samplerButton = findViewById(R.id.samplerButton);
        samplerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start CameraActivity when the button is clicked
                Intent intent = new Intent(MainActivity.this, SAMplerActivity.class);
                startActivity(intent);
            }
        });
    }
}
