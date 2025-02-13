package com.example.userauthentication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView caloriesValue, remainingCaloriesValue, paceValue, durationValue;
    private EditText foodIntakeInput;
    private Button foodIntakePlusButton;
    private SeekBar waterIntakeSlider;
    private Button waterIntakePlusButton;
    private int waterIntakePercentage; // To store water intake as a percentage
    private int chosenDailyCalories; // Daily calorie requirement
    private int remainingCalories; // Remaining calories after food intake

    private static final String TAG = "DashboardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get TextView references
        caloriesValue = findViewById(R.id.caloriesValue);
        remainingCaloriesValue = findViewById(R.id.remainingCaloriesValue);
        paceValue = findViewById(R.id.paceValue);
        durationValue = findViewById(R.id.durationValue);

        // Get food intake UI references
        foodIntakeInput = findViewById(R.id.foodIntakeInput);
        foodIntakePlusButton = findViewById(R.id.foodIntakePlusButton);

        // Get water intake UI references
        waterIntakeSlider = findViewById(R.id.waterIntakeSlider);
        waterIntakePlusButton = findViewById(R.id.waterIntakePlusButton);

        // Load data from Firestore
        loadDashboardData();

        // Set up food intake functionality
        setupFoodIntake();

        // Set up water intake functionality
        setupWaterIntake();

        // Set up Logout button
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(view -> logoutUser());

        // Set up New Day button functionality
        Button newDayButton = findViewById(R.id.newDayButton);
        newDayButton.setOnClickListener(view -> resetValuesForNewDay());
    }

    private void loadDashboardData() {
        // Get current user ID
        String userId = auth.getCurrentUser().getUid();

        // Reference to the Firestore document
        db.collection("dashboard")
                .document(userId)
                .collection("profile_details")
                .document("details")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve fields from Firestore
                        String chosenCaloriesStr = documentSnapshot.getString("chosenDailyCalories");
                        String chosenPace = documentSnapshot.getString("chosenPace");
                        String chosenDuration = documentSnapshot.getString("chosenDuration");
                        Long savedWaterIntake = documentSnapshot.getLong("waterIntake");

                        // Parse chosenDailyCalories
                        chosenDailyCalories = chosenCaloriesStr != null ? Integer.parseInt(chosenCaloriesStr.replace(" kcal", "")) : 0;
                        remainingCalories = chosenDailyCalories;

                        // Set data to TextViews
                        caloriesValue.setText(chosenCaloriesStr != null ? chosenCaloriesStr : "0 kcal");
                        remainingCaloriesValue.setText(String.format("%d kcal", remainingCalories));
                        paceValue.setText(chosenPace != null ? chosenPace : "0 km/h");
                        durationValue.setText(chosenDuration != null ? chosenDuration : "0 min");

                        // Set water intake slider progress
                        waterIntakePercentage = savedWaterIntake != null ? savedWaterIntake.intValue() : 0;
                        waterIntakeSlider.setProgress(waterIntakePercentage);
                    } else {
                        Toast.makeText(DashboardActivity.this, "No data found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DashboardActivity.this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupFoodIntake() {
        foodIntakePlusButton.setOnClickListener(view -> {
            String foodIntakeStr = foodIntakeInput.getText().toString();
            if (!foodIntakeStr.isEmpty()) {
                int foodIntake = Integer.parseInt(foodIntakeStr);
                // Update remaining calories
                remainingCalories -= foodIntake;

                // Display remaining calories (negative values included)
                remainingCaloriesValue.setText(String.format("%d kcal", remainingCalories));

                // Save the updated remaining calories to Firestore
                saveRemainingCaloriesToFirestore();
            } else {
                Toast.makeText(DashboardActivity.this, "Please enter the calories consumed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveRemainingCaloriesToFirestore() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("dashboard")
                .document(userId)
                .collection("profile_details")
                .document("details")
                .update("remainingCalories", remainingCalories)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Remaining calories updated in Firestore"))
                .addOnFailureListener(e -> {
                    Toast.makeText(DashboardActivity.this, "Failed to save remaining calories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupWaterIntake() {
        // Update water intake on "+" button click
        waterIntakePlusButton.setOnClickListener(view -> {
            if (waterIntakePercentage < 100) {
                waterIntakePercentage += 12.5;
                if (waterIntakePercentage > 100) {
                    waterIntakePercentage = 100; // Cap at 100%
                }
                waterIntakeSlider.setProgress(waterIntakePercentage);
                saveWaterIntakeToFirestore();
            } else {
                Toast.makeText(DashboardActivity.this, "Water intake is already at 100%", Toast.LENGTH_SHORT).show();
            }
        });

        // Sync slider changes to Firestore
        waterIntakeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) { // Only save when the user manually changes it
                    waterIntakePercentage = progress;
                    saveWaterIntakeToFirestore();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not used
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not used
            }
        });
    }

    private void saveWaterIntakeToFirestore() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("dashboard")
                .document(userId)
                .collection("profile_details")
                .document("details")
                .update("waterIntake", waterIntakePercentage)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Water intake updated in Firestore"))
                .addOnFailureListener(e -> {
                    Toast.makeText(DashboardActivity.this, "Failed to save water intake: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void resetValuesForNewDay() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("dashboard")
                .document(userId)
                .collection("profile_details")
                .document("details")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Reset remainingCalories, chosenDuration, and waterIntake
                        db.collection("dashboard")
                                .document(userId)
                                .collection("profile_details")
                                .document("details")
                                .update(
                                        "remainingCalories", chosenDailyCalories, // Decrement by 1
                                        "waterIntake", 0
                                )
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Values reset for new day");
                                    // Refresh the UI
                                    loadDashboardData();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(DashboardActivity.this, "Failed to reset values: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DashboardActivity.this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logoutUser() {
        auth.signOut(); // Sign out the user
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class); // Redirect to LoginActivity
        startActivity(intent);
        finish(); // Close the DashboardActivity
    }
}
