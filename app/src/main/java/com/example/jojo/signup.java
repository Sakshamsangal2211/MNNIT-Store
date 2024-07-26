package com.example.jojo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.HashMap;
import java.util.Map;

public class signup extends AppCompatActivity {
 int sot=0;
    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText usernameEditText, emailEditText, passwordEditText, collegeYearEditText, hostelNameEditText, branchEditText, phoneNumberEditText;
    private Button signupButton;
    private TextView loginTextView;
    private CircleImageView profileImageView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri profileImageUri;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        collegeYearEditText = findViewById(R.id.collegeYearEditText);
        hostelNameEditText = findViewById(R.id.hostelNameEditText);
        branchEditText = findViewById(R.id.branchEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText); // New phone number field
        signupButton = findViewById(R.id.signupButton);
        loginTextView = findViewById(R.id.loginTextView);
        profileImageView = findViewById(R.id.profileImageView);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");
        progressDialog.setCancelable(false);

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createUser();
            }
        });

        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(signup.this, MainActivity2.class));
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            profileImageUri = data.getData();
            sot=1;
            profileImageView.setImageURI(profileImageUri);
        }
    }

    private void createUser() {
        String username = usernameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String collegeYear = collegeYearEditText.getText().toString();
        String hostelName = hostelNameEditText.getText().toString();
        String branch = branchEditText.getText().toString();
        String phoneNumber = phoneNumberEditText.getText().toString(); // New phone number field

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required.");
            return;
        }
         if (TextUtils.isEmpty(phoneNumber)) {
            passwordEditText.setError("phoneNumber is required.");
            return;
        }
          if (TextUtils.isEmpty(branch)) {
            passwordEditText.setError("branch is required.");
            return;
        }
           if (TextUtils.isEmpty(collegeYear)) {
            passwordEditText.setError("collegeyear is required.");
            return;
        }
            if (TextUtils.isEmpty(hostelName)) {
            passwordEditText.setError("hostelName is required.");
            return;
        }
             if (TextUtils.isEmpty(username)) {
            passwordEditText.setError("username is required.");
            return;
        }
              if (sot==0) {
            passwordEditText.setError("profileImage is required.");
            return;
        }

        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String userId = mAuth.getCurrentUser().getUid();
                            saveUserProfile(userId, username, email, collegeYear, hostelName, branch, phoneNumber);
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(signup.this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserProfile(String userId, String username, String email, String collegeYear, String hostelName, String branch, String phoneNumber) {
        if (profileImageUri != null) {
            StorageReference fileReference = storage.getReference("profile_images").child(userId + ".jpg");
            fileReference.putFile(profileImageUri)
                    .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                fileReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            String downloadUrl = task.getResult().toString();
                                            saveUserData(userId, username, email, collegeYear, hostelName, branch, phoneNumber, downloadUrl);
                                        } else {
                                            progressDialog.dismiss();
                                            Toast.makeText(signup.this, "Failed to get profile image URL", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(signup.this, "Profile image upload failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            saveUserData(userId, username, email, collegeYear, hostelName, branch, phoneNumber, null);
        }
    }

    private void saveUserData(String userId, String username, String email, String collegeYear, String hostelName, String branch, String phoneNumber, String profileImageUrl) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("collegeYear", collegeYear);
        user.put("hostelName", hostelName);
        user.put("branch", branch);
        user.put("phoneNumber", phoneNumber); // New phone number field
        if (profileImageUrl != null) {
            user.put("profileImageUrl", profileImageUrl);
        }

        db.collection("users").document(userId).set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(signup.this, "User created successfully.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(signup.this, MainActivity2.class));
                            finish();
                        } else {
                            Toast.makeText(signup.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
