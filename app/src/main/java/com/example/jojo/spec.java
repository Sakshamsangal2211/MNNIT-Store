package com.example.jojo;

import android.graphics.Matrix;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class spec extends AppCompatActivity {

    private FirebaseFirestore db;
    private String productId;
    private String sellerId;
    private FirebaseAuth mAuth;
    private LinearLayout linearLayoutImages;
    private Button buttonBid;
    private FirebaseUser currentUser;
    private String existingBidId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spec);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get references to views
        linearLayoutImages = findViewById(R.id.linearLayoutImages);
        TextView textViewProductName = findViewById(R.id.textViewProductNameDetail);
        TextView textViewProductCategory = findViewById(R.id.textViewProductCategoryDetail);
        TextView textViewProductPrice = findViewById(R.id.textViewProductPriceDetail);
        buttonBid = findViewById(R.id.buttonBid);

        // Get data from Intent
        String productName = getIntent().getStringExtra("productName");
        String productCategory = getIntent().getStringExtra("category");
        String productPrice = getIntent().getStringExtra("price");
        String productOldPrice = getIntent().getStringExtra("actualPrice");
        productId = getIntent().getStringExtra("productId");
        sellerId = getIntent().getStringExtra("sellerId");
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Set data to views
        textViewProductName.setText(productName);
        textViewProductCategory.setText(productCategory);
        textViewProductPrice.setText("Price: Rs" + productPrice + " (was Rs" + productOldPrice + ")");

        // Load product images
        loadProductImages();

        // Check if a bid already exists for this product and user
        checkExistingBid();

        // Set click listener for the bid button
        buttonBid.setOnClickListener(v -> showBidDialog());
    }

    private void loadProductImages() {
        db.collection("products")
                .document(productId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> imageUrls = (List<String>) document.get("imageUrls");
                            if (imageUrls != null && !imageUrls.isEmpty()) {
                                displayImages(imageUrls);
                            } else {
                                Toast.makeText(spec.this, "No images found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(spec.this, "Product not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(spec.this, "Failed to load images", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayImages(List<String> imageUrls) {
        for (String imageUrl : imageUrls) {
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(1000, LinearLayout.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(0, 0, 12, 0); // Margin between images
            Glide.with(this).load(imageUrl).into(imageView);
            linearLayoutImages.addView(imageView);
        }
    }

    private void checkExistingBid() {
        db.collection("products")
                .document(productId)
                .collection("bids")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
//                            String status = document.getString("status");
//                            if ("pending".equals(status)) {
                                existingBidId = document.getId();
                                buttonBid.setText("Edit Bid");
                                break;
//                            }
                        }
                    }
                });
    }

    private void showBidDialog() {
        // Inflate the dialog view
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bid, null);
        EditText editTextBidValue = dialogView.findViewById(R.id.editTextBidValue);

        // Create a BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(dialogView);

        // Set click listener for the submit button
        Button buttonSubmitBid = dialogView.findViewById(R.id.buttonSubmitBid);
        buttonSubmitBid.setOnClickListener(v -> {
            String bidValue = editTextBidValue.getText().toString().trim();
            if (!TextUtils.isEmpty(bidValue)) {
                if (existingBidId != null) {
                    updateBid(Double.parseDouble(bidValue));
                } else {
                    submitBid(Double.parseDouble(bidValue));
                }
                bottomSheetDialog.dismiss();
            } else {
                Toast.makeText(spec.this, "Please enter a bid value", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheetDialog.show();
    }

    private void submitBid(double bidValue) {
        // Create a map to store bid details
        Map<String, Object> bid = new HashMap<>();
        bid.put("bidValue", bidValue);
        bid.put("sellerId", sellerId);
        bid.put("userId", currentUser.getUid());
        bid.put("productId", productId);
        bid.put("status", "pending");

        // Add bid to the product's bids array
        db.collection("products")
                .document(productId)
                .collection("bids")
                .add(bid)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(spec.this, "Bid submitted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(spec.this, "Failed to submit bid", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateBid(double bidValue) {
        db.collection("products")
                .document(productId)
                .collection("bids")
                .document(existingBidId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        if ("pending".equals(status)) {
                            // Update the existing bid
                            db.collection("products")
                                    .document(productId)
                                    .collection("bids")
                                    .document(existingBidId)
                                    .update("bidValue", bidValue)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(spec.this, "Bid updated successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(spec.this, "Failed to update bid", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(spec.this, "Bid cannot be edited now", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
