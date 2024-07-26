package com.example.jojo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class user_profile extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get user ID from Firebase Auth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId = currentUser.getUid();

        // Fetch user details from Firestore
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            // Retrieve user details
                            String username = documentSnapshot.getString("username");
                            String phoneNumber = documentSnapshot.getString("phoneNumber");
                            String collegeYear = documentSnapshot.getString("collegeYear");
                            String branch = documentSnapshot.getString("branch");
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                            // Initialize views
                            ImageView profileImageView = findViewById(R.id.profileImageView);
                            TextView usernameTextView = findViewById(R.id.usernameTextView);
                            TextView phoneNumberTextView = findViewById(R.id.phoneNumberTextView);
                            TextView collegeYearTextView = findViewById(R.id.collegeYearTextView);
                            TextView branchTextView = findViewById(R.id.branchTextView);

                            // Load profile image using Glide
                            Glide.with(user_profile.this)
                                    .load(profileImageUrl)
                                    .into(profileImageView);

                            // Set user details
                            usernameTextView.setText("Username : "+username);
                            phoneNumberTextView.setText("PhoneNumber : "+phoneNumber);
                            collegeYearTextView.setText("CollegeYear : "+collegeYear);
                            branchTextView.setText("Branch : "+branch);
                        } else {
                            Log.d(TAG, "Document does not exist");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error fetching document", e);
                    }
                });
    }

    // Method to handle logout button click
    public void logoutUser(View view) {
        FirebaseAuth.getInstance().signOut();

        // Clear login state from SharedPreferences
        getSharedPreferences("MyPrefsFile", MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", false)
                .apply();

        startActivity(new Intent(user_profile.this, MainActivity2.class));
        finish(); // Close the current activity
    }
}
