package com.example.jojo;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class user_records extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout linearLayoutBids;
    private String userId;
    private FirebaseAuth mAuth;
    private String sellerId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_records);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get references to views
        linearLayoutBids = findViewById(R.id.productContainer);

        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Get data from Intent
        userId = currentUser.getUid();

        // Load bids and products
        loadBidsAndProducts();
    }

    private void loadBidsAndProducts() {
        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot productDocument : queryDocumentSnapshots) {
                        String productId = productDocument.getId();
                        DocumentReference productRef = db.collection("products").document(productId);
                        productRef.collection("bids")
                                .whereEqualTo("userId", userId)
                                .get()
                                .addOnSuccessListener(bidSnapshots -> {
                                    for (QueryDocumentSnapshot bidDocument : bidSnapshots) {
                                        // Load product details
                                        loadProductDetails(productDocument, bidDocument);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // Handle failure
                                    Toast.makeText(user_records.this, "Failed to load bids", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    Toast.makeText(user_records.this, "Failed to load products", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProductDetails(QueryDocumentSnapshot productDocument, QueryDocumentSnapshot bidDocument) {
        if (productDocument.exists()) {
            // Inflate product item layout
            View productView = LayoutInflater.from(user_records.this)
                    .inflate(R.layout.product_iti, linearLayoutBids, false);

            // Bind views
            ImageView imageViewProduct = productView.findViewById(R.id.imageViewProduct);
            TextView textViewProductName = productView.findViewById(R.id.textViewProductName);
            TextView textViewProductPrice = productView.findViewById(R.id.textViewProductPrice);
            TextView textViewProductDescription = productView.findViewById(R.id.textViewProductDescription);
            TextView textViewPendingStatus = productView.findViewById(R.id.textViewPendingStatus);
            TextView textViewAdditionalInfo = productView.findViewById(R.id.textViewAdditionalInfo);
            LinearLayout li = productView.findViewById(R.id.li);

            // Set data
            String productName = productDocument.getString("category");
            String productPrice = productDocument.getString("price");
            String productDescription = productDocument.getString("actualPrice");
            List<String> imageUrls = (List<String>) productDocument.get("imageUrls"); // Assuming you have an imageUrls field

            textViewProductName.setText("Product: " + productName);
            textViewProductPrice.setText("Price: Rs" + productPrice);
            textViewProductDescription.setText("Actual Price: " + productDescription);

            // Check the bid status
            String bidStatus = bidDocument.getString("status");

            if ("Accepted".equals(bidStatus)) {
                textViewPendingStatus.setText("Accepted");
                textViewPendingStatus.setBackgroundColor(Color.GREEN);
                textViewPendingStatus.setTypeface(null, Typeface.BOLD);

                // Add phone number TextView dynamically
                sellerId = bidDocument.getString("sellerId");
                if (sellerId != null) {
                    DocumentReference sellerRef = db.collection("users").document(sellerId);
                    sellerRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String phoneNumber = documentSnapshot.getString("phoneNumber");
                            if (phoneNumber != null) {
                                TextView textViewPhoneNumber = new TextView(user_records.this);
                                textViewPhoneNumber.setText("Seller Phone: " + phoneNumber);
                                textViewPhoneNumber.setTextColor(Color.RED);
                                textViewPhoneNumber.setTextSize(14);
                                li.addView(textViewPhoneNumber);
                            }
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(user_records.this, "Failed to load seller phone number", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            // Set bid value in the additional TextView
            Double bidValue = bidDocument.getDouble("bidValue");
            textViewAdditionalInfo.setText("Bid Value: Rs" + bidValue);

            // Load product image using Glide
            if (imageUrls != null && !imageUrls.isEmpty()) {
                Glide.with(user_records.this)
                        .load(imageUrls.get(0))
                        .into(imageViewProduct);
            } else {
                imageViewProduct.setImageResource(R.drawable.jo); // Set a default image if no image URL is available
            }
             productView.setOnClickListener(v -> {
                Intent intent = new Intent(user_records.this, spec.class);
                intent.putExtra("category", productName);
                intent.putExtra("productName", productDocument.getString("name"));
                intent.putExtra("sellerId", sellerId);
                intent.putExtra("productId", productDocument.getId());
                intent.putExtra("price", productPrice);
                intent.putExtra("actualPrice", productDescription);
                startActivity(intent);
            });

            // Handle options menu click
            TextView imageViewOptions = productView.findViewById(R.id.imageViewOptions);
            imageViewOptions.setOnClickListener(v -> {
                showOptionsDialog(productDocument.getId()); // Pass the product document ID to identify the product
            });

            // Add product view to linear layout
            linearLayoutBids.addView(productView);
        }
    }

    // Method to show options dialog
    public void showOptionsDialog(String productId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete");
        builder.setMessage("Are you sure you want to delete this bid?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Perform delete operation here
            deleteBid(productId);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Do nothing, simply dismiss the dialog
            dialog.dismiss();
        });

        builder.show();
    }

    // Method to delete bid
    private void deleteBid(String productId) {
          db.collection("products")
                .document(productId)
                .collection("bids")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Delete the bid document
                            db.collection("products")
                                    .document(productId)
                                    .collection("bids")
                                    .document(document.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(user_records.this, "Bid deleted successfully", Toast.LENGTH_SHORT).show();
                                        // Optionally, update UI or perform any other operations after deletion
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(user_records.this, "Failed to delete bid: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(user_records.this, "Error getting documents: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
        // Implement deletion logic here based on the productId
        // Example: Delete bid from Firestore or perform necessary updates
        Toast.makeText(this, "Deleting bid for product: " + productId, Toast.LENGTH_SHORT).show();
    }
}
