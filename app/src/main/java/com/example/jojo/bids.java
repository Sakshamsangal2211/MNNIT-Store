package com.example.jojo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;

public class bids extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout linearLayoutBids;
    private String productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bids);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get references to views
        linearLayoutBids = findViewById(R.id.linearLayoutBids);

        // Get productId from Intent or other sources
        productId = getIntent().getStringExtra("productId");

        // Load bids
        loadBids();
    }

    private void loadBids() {
        db.collection("products")
                .document(productId)
                .collection("bids")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Inflate bid item layout
                        View bidView = LayoutInflater.from(bids.this)
                                .inflate(R.layout.bid_item, linearLayoutBids, false);

                        // Bind views
                        TextView textViewBidValue = bidView.findViewById(R.id.textViewBidValue);
                        TextView textViewBidderId = bidView.findViewById(R.id.textViewBidderId);
                        Button viewDetailsButton = bidView.findViewById(R.id.viewDetailsButton);
                        TextView textViewStatus = bidView.findViewById(R.id.additionalTextView);
                        TextView textViewContactNumber = bidView.findViewById(R.id.textViewContactNumber); // Add this line

                        // Set data
                        Map<String, Object> bidData = document.getData();
                        if (bidData != null && bidData.get("bidValue") != null) {
                            double bidValue = (double) bidData.get("bidValue");
                            String bidderId = (String) bidData.get("userId");
                            String bidStatus = (String) bidData.get("status");

                            // Set bid value and bidder ID
                            textViewBidValue.setText("Bid: " + bidValue);
//                            textViewBidderId.setText("Bidder Name: " + bidderId);

                            // Set bid status
                            if ("Accepted".equals(bidStatus)) {
                                textViewStatus.setText("Accepted");
                                textViewStatus.setTextColor(Color.GREEN);
                                textViewStatus.setTypeface(null, Typeface.BOLD);
                                // Fetch and set contact number
                                db.collection("users").document(bidderId).get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                String contactNumber = documentSnapshot.getString("contact");
                                                textViewContactNumber.setText("Ph " + contactNumber);
                                            }
                                        });
                            } else {
                                textViewContactNumber.setText(""); // Make it blank if not accepted
                            }

                            // Fetch and set profile image
                            db.collection("users").document(bidderId).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                                            ImageView profileImageView = bidView.findViewById(R.id.profileImageView);
                                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                                Glide.with(bids.this)
                                                        .load(profileImageUrl)
                                                        .into(profileImageView);
                                            }
                                        }
                                    });

                            // Handle View Details button click
                            viewDetailsButton.setOnClickListener(v -> showUserDetailsDialog(bidderId, document.getId()));
                        }

                        // Add bid view to linear layout
                        linearLayoutBids.addView(bidView);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    Toast.makeText(bids.this, "Failed to load bids", Toast.LENGTH_SHORT).show();
                });
    }

    private void showUserDetailsDialog(String userId, String bidId) {
        // Create a custom dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_user_details);

        // Bind views
        CircleImageView dialogProfileImageView = dialog.findViewById(R.id.dialogProfileImageView);
        TextView dialogUsername = dialog.findViewById(R.id.dialogUsername);
        TextView dialogPhoneNumber = dialog.findViewById(R.id.dialogPhoneNumber);
        TextView dialogCollegeYear = dialog.findViewById(R.id.dialogCollegeYear);
        TextView dialogBranch = dialog.findViewById(R.id.dialogBranch);
        Button dialogAcceptButton = dialog.findViewById(R.id.dialogAcceptButton);

        // Fetch user details from Firestore
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String phoneNumber = documentSnapshot.getString("phoneNumber");
                        String collegeYear = documentSnapshot.getString("collegeYear");
                        String branch = documentSnapshot.getString("branch");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        // Set user details to dialog views
                        dialogUsername.setText("Username: " + username);
                        dialogPhoneNumber.setText("Phone: " + phoneNumber);
                        dialogCollegeYear.setText("College Year: " + collegeYear);
                        dialogBranch.setText("Branch: " + branch);

                        // Load profile image using Glide
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(bids.this)
                                    .load(profileImageUrl)
                                    .into(dialogProfileImageView);
                        }
                    } else {
                        Toast.makeText(bids.this, "User details not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(bids.this, "Failed to load user details", Toast.LENGTH_SHORT).show();
                });

        // Handle Accept button click
        dialogAcceptButton.setOnClickListener(v -> {
            // Show confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Accept Offer")
                    .setMessage("Are you sure you want to accept this offer?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        updateBidStatus(bidId);
                        dialog.dismiss();
                    })
                    .setNegativeButton("No", (dialogInterface, i) -> {
                        // Just dismiss the dialog
                        dialogInterface.dismiss();
                    })
                    .show();
        });

        // Show the dialog
        dialog.show();
    }

    private void updateBidStatus(String bidId) {
        // Update the bid status in Firestore
        db.collection("products").document(productId).collection("bids")
                .document(bidId)
                .update("status", "Accepted")
                .addOnSuccessListener(aVoid -> {
                    // Refresh the bids list
                    linearLayoutBids.removeAllViews();
                    loadBids();
                    Toast.makeText(bids.this, "Bid accepted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(bids.this, "Failed to accept bid", Toast.LENGTH_SHORT).show();
                });
    }
}
