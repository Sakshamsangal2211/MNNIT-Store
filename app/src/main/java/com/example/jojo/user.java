package com.example.jojo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class user extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout productContainer;
    private FirebaseAuth mAuth;
    private List<QueryDocumentSnapshot> productList;
    private List<QueryDocumentSnapshot> originalProductList; // To store the original unsorted list
    private SearchView searchView;
    private RadioGroup radioGroupCategories;

    private Button bt, ju, sortByPriceButton; // Added sortByPriceButton
    private ImageView bt2;
    private FirebaseStorage storage;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();

        bt = findViewById(R.id.button);
        ju = findViewById(R.id.recordsButton);
        bt2 = findViewById(R.id.profileImageView);
        productContainer = findViewById(R.id.productContainer);
        searchView = findViewById(R.id.searchView);
        radioGroupCategories = findViewById(R.id.radioGroupCategories);
        progressBar = findViewById(R.id.progressBar);
        sortByPriceButton = findViewById(R.id.sortByPriceButton); // Initialize sortByPriceButton

        bt.setOnClickListener(v -> startActivity(new Intent(user.this, mymain.class)));
        ju.setOnClickListener(v -> startActivity(new Intent(user.this, user_records.class)));
        bt2.setOnClickListener(v -> startActivity(new Intent(user.this, user_profile.class)));

        sortByPriceButton.setOnClickListener(v -> sortProductsByPrice()); // Set click listener for sortByPriceButton

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            loadUserProfile(userId);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterProducts(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterProducts(newText);
                return true;
            }
        });

        // Load products from Firestore
        loadProducts();
        // Load categories from Firestore
        loadCategories();
    }

    private void loadUserProfile(String userId) {
        ImageView profileImageView = findViewById(R.id.profileImageView);
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(user.this).load(profileImageUrl).into(profileImageView);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(user.this, "Failed to load user details", Toast.LENGTH_SHORT).show());
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    productList = new ArrayList<>();
                    originalProductList = new ArrayList<>(); // Initialize originalProductList
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        productList.add(document);
                        originalProductList.add(document); // Populate originalProductList
                    }
                    displayProducts(productList); // Display initial products
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(user.this, "Failed to load products", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayProducts(List<QueryDocumentSnapshot> products) {
        productContainer.removeAllViews();
        for (QueryDocumentSnapshot document : products) {
            View productView = LayoutInflater.from(user.this).inflate(R.layout.product_item, productContainer, false);

            ImageView imageViewProduct = productView.findViewById(R.id.imageViewProduct);
            TextView textViewProductName = productView.findViewById(R.id.textViewProductName);
            TextView textViewProductCategory = productView.findViewById(R.id.textViewProductCategory);
            TextView textViewProductPrice = productView.findViewById(R.id.textViewProductPrice);

            String productName = document.getString("productName");
            String productCategory = document.getString("category");
            String productPrice = document.getString("price");
            String productOldPrice = document.getString("actualPrice");
            List<String> imageUrls = (List<String>) document.get("imageUrls");
            String productId = document.getId();
            String sellerId = document.getString("sellerId");

            textViewProductName.setText(productName);
            textViewProductCategory.setText(productCategory);
            textViewProductPrice.setText("Price: Rs" + productPrice + " (was Rs" + productOldPrice + ")");

            // Load and display the first image from imageUrls if available
            if (imageUrls != null && !imageUrls.isEmpty()) {
                loadImage(imageUrls.get(0), imageViewProduct);
            }

            productView.setOnClickListener(v -> {
                Intent intent = new Intent(user.this, spec.class);
                intent.putExtra("productName", productName);
                intent.putExtra("category", productCategory);
                intent.putExtra("price", productPrice);
                intent.putExtra("actualPrice", productOldPrice);
                intent.putExtra("productId", productId);
                intent.putExtra("sellerId", sellerId);
                startActivity(intent);
            });

            productContainer.addView(productView);
        }
    }

    private void loadImage(String imageUrl, ImageView imageView) {
        Glide.with(this)
                .load(imageUrl)
                .into(imageView);
    }

    private void filterProducts(String query) {
        List<QueryDocumentSnapshot> filteredList = new ArrayList<>();
        for (QueryDocumentSnapshot document : productList) {
            String productName = document.getString("productName");
            if (!TextUtils.isEmpty(productName) && productName.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(document);
            }
        }
        displayProducts(filteredList);
    }

    private void loadCategories() {
        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> categories = new HashSet<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String category = document.getString("category");
                        if (category != null && !category.isEmpty()) {
                            categories.add(category);
                        }
                    }
                    populateCategoryRadioGroup(categories);
                })
                .addOnFailureListener(e -> Toast.makeText(user.this, "Failed to load categories", Toast.LENGTH_SHORT).show());
    }

    private void populateCategoryRadioGroup(Set<String> categories) {
        RadioButton allRadioButton = new RadioButton(this);
        allRadioButton.setText("All");
        allRadioButton.setOnClickListener(v -> displayProducts(productList));
        radioGroupCategories.addView(allRadioButton);

        for (String category : categories) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(category);
            radioButton.setOnClickListener(v -> filterProductsByCategory(category));
            radioGroupCategories.addView(radioButton);
        }

        // Set "All" as the default selected RadioButton
        if (radioGroupCategories.getChildCount() > 0) {
            ((RadioButton) radioGroupCategories.getChildAt(0)).setChecked(true);
        }
    }

    private void filterProductsByCategory(String category) {
        List<QueryDocumentSnapshot> filteredList = new ArrayList<>();
        for (QueryDocumentSnapshot document : productList) {
            String productCategory = document.getString("category");
            if (!TextUtils.isEmpty(productCategory) && productCategory.equalsIgnoreCase(category)) {
                filteredList.add(document);
            }
        }
        displayProducts(filteredList);
    }

    private void sortProductsByPrice() {
        Collections.sort(productList, new Comparator<QueryDocumentSnapshot>() {
            @Override
            public int compare(QueryDocumentSnapshot o1, QueryDocumentSnapshot o2) {
                String price1 = o1.getString("price");
                String price2 = o2.getString("price");
                if (price1 != null && price2 != null) {
                    Double p1 = Double.parseDouble(price1);
                    Double p2 = Double.parseDouble(price2);
                    return p1.compareTo(p2);
                }
                return 0;
            }
        });

        displayProducts(productList);
    }
}
