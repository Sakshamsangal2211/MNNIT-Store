package com.example.jojo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Map;

public class sellerprofile extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout linearLayoutProducts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sellerprofile);

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get reference to the linear layout
        linearLayoutProducts = findViewById(R.id.linearLayoutProducts);

        // Load seller products
        loadSellerProducts();
    }

    private void loadSellerProducts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Handle user not logged in
            return;
        }

        String sellerId = currentUser.getUid();

        db.collection("products")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        // Get product details
                        String productId = document.getId();
                        String productName = document.getString("productName");
                        String productCategory = document.getString("category");
                        String productPrice = document.getString("price");
                        String productOldPrice = document.getString("actualPrice");
                        List<String> imageUrls = (List<String>) document.get("imageUrls");

                        // Inflate product item layout
                        View productView = LayoutInflater.from(sellerprofile.this)
                                .inflate(R.layout.product_it, linearLayoutProducts, false);

                        // Bind views
                        ImageView imageViewProduct = productView.findViewById(R.id.imageViewProduct);
                        TextView textViewProductName = productView.findViewById(R.id.textViewProductName);
                        TextView textViewProductCategory = productView.findViewById(R.id.textViewProductCategory);
                        TextView textViewProductPrice = productView.findViewById(R.id.textViewProductPrice);
                        TextView textViewHighestBid = productView.findViewById(R.id.textViewHighestBid);
                        Button buttonViewAll = productView.findViewById(R.id.buttonViewAll);
                        Button buttonMoreOptions = productView.findViewById(R.id.buttonMoreOptions);

                        // Set data
                        textViewProductName.setText(productName);
                        textViewProductCategory.setText(productCategory);
                        textViewProductPrice.setText("Price: Rs" + productPrice + " (was Rs" + productOldPrice + ")");

                        // Load the first image from the imageUrls list
                        if (imageUrls != null && !imageUrls.isEmpty()) {
                            String firstImageUrl = imageUrls.get(0);
                            Glide.with(this).load(firstImageUrl).into(imageViewProduct);
                        }

                        // Load bids and find the highest bid
                        db.collection("products")
                                .document(productId)
                                .collection("bids")
                                .get()
                                .addOnSuccessListener(bidSnapshots -> {
                                    double highestBid = 0;
                                    for (DocumentSnapshot bidDocument : bidSnapshots) {
                                        Map<String, Object> bid = bidDocument.getData();
                                        if (bid != null && bid.get("bidValue") != null) {
                                            double bidValue = (double) bid.get("bidValue");
                                            if (bidValue > highestBid) {
                                                highestBid = bidValue;
                                            }
                                        }
                                    }
                                    textViewHighestBid.setText("Highest Bid: Rs" + highestBid);
                                });

                        buttonViewAll.setOnClickListener(v -> {
                            Intent intent = new Intent(sellerprofile.this, bids.class);
                            intent.putExtra("productId", productId);
                            startActivity(intent);
                        });

                        buttonMoreOptions.setOnClickListener(v -> {
                            showDeleteConfirmationDialog(productId);
                        });

                        // Add the product view to the container
                        linearLayoutProducts.addView(productView);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    Toast.makeText(sellerprofile.this, "Failed to load products", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmationDialog(String productId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteProduct(productId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProduct(String productId) {
        db.collection("products")
                .document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(sellerprofile.this, "Product deleted", Toast.LENGTH_SHORT).show();
                    recreate(); // Reload the activity to refresh the product list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(sellerprofile.this, "Failed to delete product", Toast.LENGTH_SHORT).show();
                });
    }
}
