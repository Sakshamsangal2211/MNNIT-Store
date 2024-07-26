package com.example.jojo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class mymain extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 71;

    private LinearLayout imageContainer;
    private TextView categoryTextView;
    private LinearLayout categorySelectionLayout;
    private RadioGroup radioGroupCategories;
    private EditText priceEditText, actualPriceEditText, contactEditText;
    private Button uploadImageButton, addProductButton;
    private ArrayList<Uri> fileUris = new ArrayList<>();

    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseVisionImageLabeler labeler;

    private FirebaseFirestore db;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mymain);

        imageContainer = findViewById(R.id.imageContainer);
        categoryTextView = findViewById(R.id.categoryTextView);
        categorySelectionLayout = findViewById(R.id.categorySelectionLayout);
        radioGroupCategories = findViewById(R.id.radioGroupCategories);
        priceEditText = findViewById(R.id.priceEditText);
        actualPriceEditText = findViewById(R.id.actualPriceEditText);
        contactEditText = findViewById(R.id.contactEditText);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        addProductButton = findViewById(R.id.addProductButton);
        Button product = findViewById(R.id.bunty);
 storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        db = FirebaseFirestore.getInstance();
        progressDialog = new ProgressDialog(this);
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize FirebaseVisionImageLabeler
        FirebaseVisionOnDeviceImageLabelerOptions options =
                new FirebaseVisionOnDeviceImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.7f) // Adjust confidence threshold as needed
                        .build();
        labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler(options);

        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        addProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addProduct();
            }
        });

        product.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mymain.this, sellerprofile.class));
            }
        });

        // Set onClickListener for categoryTextView to show/hide categorySelectionLayout
        categoryTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (categorySelectionLayout.getVisibility() == View.GONE) {
                    categorySelectionLayout.setVisibility(View.VISIBLE);
                } else {
                    categorySelectionLayout.setVisibility(View.GONE);
                }
            }
        });

        // Set onCheckedChangeListener for radioGroupCategories to update categoryTextView
        radioGroupCategories.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton selectedRadioButton = findViewById(checkedId);
                String selectedCategory = selectedRadioButton.getText().toString();
                categoryTextView.setText(selectedCategory);
                categorySelectionLayout.setVisibility(View.GONE);
            }
        });
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri fileUri = data.getClipData().getItemAt(i).getUri();
                        fileUris.add(fileUri);
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), fileUri);
                            ImageView imageView = new ImageView(this);
                            imageView.setImageBitmap(bitmap);
                            imageContainer.addView(imageView);
                            processImageForLabeling(fileUri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (data.getData() != null) {
                    Uri fileUri = data.getData();
                    fileUris.add(fileUri);
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), fileUri);
                        ImageView imageView = new ImageView(this);
                        imageView.setImageBitmap(bitmap);
                        imageContainer.addView(imageView);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void addProduct() {
        String category = categoryTextView.getText().toString().trim();
        String price = priceEditText.getText().toString().trim();
        String actualPrice = actualPriceEditText.getText().toString().trim();
       String contact = contactEditText.getText().toString().trim();

        if (fileUris.isEmpty()) {
            Toast.makeText(this, "Please upload at least one image.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(category)) {
            categoryTextView.setError("Category is required.");
            return;
        }

        if (TextUtils.isEmpty(price)) {
            priceEditText.setError("Price is required.");
            return;
        }

        if (TextUtils.isEmpty(actualPrice)) {
            actualPriceEditText.setError("Actual price is required.");
            return;
        }
         progressDialog.setMessage("Uploading...");
        progressDialog.show();

        final String randomKey = UUID.randomUUID().toString();
        ArrayList<String> imageUrls = new ArrayList<>();
        for (Uri fileUri : fileUris) {
            StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());
            ref.putFile(fileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    imageUrls.add(uri.toString());
                                    if (imageUrls.size() == fileUris.size()) {
                                        saveProductToFirestore(randomKey, category, price, actualPrice, contact, imageUrls);
                                    }
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(mymain.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void saveProductToFirestore(String randomKey, String category, String price, String actualPrice, String contact, ArrayList<String> imageUrls) {
        Map<String, Object> product = new HashMap<>();
        product.put("category", category);
        product.put("price", price);
        product.put("actualPrice", actualPrice);
        product.put("productName", contact);
        product.put("imageUrls", imageUrls);
        product.put("sellerId", FirebaseAuth.getInstance().getCurrentUser().getUid());

        db.collection("products").document(randomKey)
                .set(product)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        Toast.makeText(mymain.this, "Product added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(mymain.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


        // Process each image for labeling



    private void processImageForLabeling(Uri fileUri) {
        Toast.makeText(mymain.this, "ok", Toast.LENGTH_SHORT).show();
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), fileUri);
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

            labeler.processImage(image)
                    .addOnCompleteListener(new OnCompleteListener<List<FirebaseVisionImageLabel>>() {
                        @Override
                        public void onComplete(@NonNull Task<List<FirebaseVisionImageLabel>> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                List<FirebaseVisionImageLabel> labels = task.getResult();
                                String category = getCategoryFromLabels(labels);
                                contactEditText.setText(category);
                            } else {
                                Toast.makeText(mymain.this, "Failed to label image.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getCategoryFromLabels(List<FirebaseVisionImageLabel> labels) {
        // Example: Extract first label as category (adjust as needed)
        if (!labels.isEmpty()) {
            FirebaseVisionImageLabel label = labels.get(0);
            return label.getText();
        }
        return "Uncategorized";
    }
}
