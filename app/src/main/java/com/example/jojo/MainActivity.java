package com.example.jojo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView = findViewById(R.id.my_image);
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.anim);
        imageView.startAnimation(fadeInAnimation);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Start SecondActivity after the delay
                Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                startActivity(intent);
                finish(); // Close MainActivity
            }
        }, 2000);

    }
}