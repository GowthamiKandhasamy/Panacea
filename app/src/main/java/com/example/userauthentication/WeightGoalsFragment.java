package com.example.userauthentication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class WeightGoalsFragment extends Fragment {

    private RadioGroup rgWeightGoals;
    private RadioButton rbLoseWeight, rbMaintainWeight, rbGainWeight;
    private EditText etTargetWeight;
    private Button btnNextWeightGoals;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private View targetWeightSection;

    private static final String TAG = "WeightGoalsFragment"; // Debug tag for logging

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Fragment view created.");

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_weight_goals, container, false);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind views
        rgWeightGoals = view.findViewById(R.id.rg_weight_goals);
        rbLoseWeight = view.findViewById(R.id.rb_lose_weight);
        rbMaintainWeight = view.findViewById(R.id.rb_maintain_weight);
        rbGainWeight = view.findViewById(R.id.rb_gain_weight);
        etTargetWeight = view.findViewById(R.id.et_target_weight);
        btnNextWeightGoals = view.findViewById(R.id.btn_next_weight_goals);
        targetWeightSection = view.findViewById(R.id.et_target_weight);

        // Initially hide the target weight field
        targetWeightSection.setVisibility(View.GONE);
        Log.d(TAG, "Target weight section hidden initially.");

        // Handle weight goal selection
        rgWeightGoals.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG, "onCheckedChanged: Weight goal selected.");
            if (checkedId == R.id.rb_lose_weight || checkedId == R.id.rb_gain_weight) {
                targetWeightSection.setVisibility(View.VISIBLE);
                Log.d(TAG, "Target weight section shown.");
            } else {
                targetWeightSection.setVisibility(View.GONE);
                Log.d(TAG, "Target weight section hidden.");
            }
        });

        // Handle Next button click
        btnNextWeightGoals.setOnClickListener(v -> {
            Log.d(TAG, "Next button clicked.");
            saveWeightGoalData(); // Save data and then navigate to the next fragment
        });

        return view;
    }

    private void saveWeightGoalData() {
        String goal = ""; // Weight goal
        final Double[] targetWeight = {null}; // Wrapper for final or effectively final variable

        // Determine the selected weight goal
        if (rbLoseWeight.isChecked()) {
            goal = "lose";
            Log.d(TAG, "Lose weight goal selected.");
        } else if (rbMaintainWeight.isChecked()) {
            goal = "maintain";
            Log.d(TAG, "Maintain weight goal selected.");
        } else if (rbGainWeight.isChecked()) {
            goal = "gain";
            Log.d(TAG, "Gain weight goal selected.");
        }

        if (goal.equals("lose") || goal.equals("gain")) {
            String targetWeightText = etTargetWeight.getText().toString();
            if (!targetWeightText.isEmpty()) {
                targetWeight[0] = Double.parseDouble(targetWeightText);
                Log.d(TAG, "Target weight entered: " + targetWeight[0]);
            } else {
                Toast.makeText(getActivity(), "Please enter a valid target weight", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Target weight is empty.");
                return;
            }
        }

        String userId = auth.getCurrentUser().getUid();
        Log.d(TAG, "User ID: " + userId);

        // Use final wrappers for effectively final variables
        final String finalGoal = goal;

        db.collection("users").document(userId).collection("basic_details")
                .document("details")
                .get()
                .addOnSuccessListener(basicDetailsSnapshot -> {
                    if (basicDetailsSnapshot.exists()) {
                        Log.d(TAG, "Basic details retrieved successfully.");
                        Double bmr = basicDetailsSnapshot.getDouble("calorie_needs");
                        Log.d(TAG, "BMR retrieved: " + bmr);

                        db.collection("users").document(userId).collection("activity_level")
                                .document("details")
                                .get()
                                .addOnSuccessListener(activityLevelSnapshot -> {
                                    if (activityLevelSnapshot.exists()) {
                                        Double activityFactor = activityLevelSnapshot.getDouble("activity_factor");
                                        Log.d(TAG, "Activity factor retrieved: " + activityFactor);

                                        if (bmr != null && activityFactor != null) {
                                            double dailyCaloricNeeds = bmr * activityFactor;
                                            Log.d(TAG, "Daily caloric needs calculated: " + dailyCaloricNeeds);

                                            // Prepare weight goals data
                                            Map<String, Object> weightGoalData = new HashMap<>();
                                            weightGoalData.put("goal", finalGoal);
                                            weightGoalData.put("target_weight", targetWeight[0]);

                                            if (finalGoal.equals("maintain")) {
                                                Log.d(TAG, "User wants to maintain weight.");
                                                updatePrograms(userId, 0, 0, 0, 0, 0, 0);
                                            } else {
                                                Log.d(TAG, "User wants to " + finalGoal + " weight.");
                                                double weightDiff = Math.abs(targetWeight[0] - basicDetailsSnapshot.getDouble("weight"));
                                                double relaxedCalories = dailyCaloricNeeds - (finalGoal.equals("lose") ? 250 : -250);
                                                double steadyCalories = dailyCaloricNeeds - (finalGoal.equals("lose") ? 500 : -500);
                                                double vigorousCalories = dailyCaloricNeeds - (finalGoal.equals("lose") ? 1000 : -1000);

                                                double relaxedDuration = Math.round(weightDiff / 0.25);
                                                double steadyDuration = Math.round(weightDiff / 0.5);
                                                double vigorousDuration = Math.round(weightDiff / 1.0);

                                                updatePrograms(userId, relaxedCalories, relaxedDuration, steadyCalories, steadyDuration, vigorousCalories, vigorousDuration);
                                            }

                                            saveWeightGoalDataToFirestore(userId, weightGoalData);
                                        } else {
                                            Log.e(TAG, "BMR or activity factor is null.");
                                        }
                                    } else {
                                        Log.e(TAG, "Activity level not found.");
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to retrieve activity level: " + e.getMessage()));
                    } else {
                        Log.e(TAG, "Basic details not found.");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to retrieve basic details: " + e.getMessage()));
    }

    private void saveWeightGoalDataToFirestore(String userId, Map<String, Object> weightGoalData) {
        Log.d(TAG, "Saving weight goal data to Firestore.");

        db.collection("users").document(userId).collection("weight_goals")
                .document("details")
                .set(weightGoalData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Weight goal data saved successfully.");
                    Toast.makeText(getActivity(), "Weight goal data saved!", Toast.LENGTH_SHORT).show();

                    // Navigate to the next fragment (Dietary Preferences)
                    Log.d(TAG, "Navigating to Dietary Preferences Fragment.");

                    // Ensure fragment transaction happens on the main thread
                    requireActivity().runOnUiThread(() -> {
                        DietaryPreferencesFragment dietaryPreferencesFragment = new DietaryPreferencesFragment();
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, dietaryPreferencesFragment)
                                .addToBackStack(null)
                                .commit();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save weight goal data: " + e.getMessage());
                    Toast.makeText(getActivity(), "Failed to save weight goal data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updatePrograms(String userId, double relaxedCalories, double relaxedDuration, double steadyCalories, double steadyDuration, double vigorousCalories, double vigorousDuration) {
        Log.d(TAG, "Updating programs with new caloric and duration values.");

        Map<String, Object> programsData = new HashMap<>();
        programsData.put("relaxedCalories", Math.round(relaxedCalories));
        programsData.put("relaxedDuration", Math.round(relaxedDuration));
        programsData.put("steadyCalories", Math.round(steadyCalories));
        programsData.put("steadyDuration", Math.round(steadyDuration));
        programsData.put("vigorousCalories", Math.round(vigorousCalories));
        programsData.put("vigorousDuration", Math.round(vigorousDuration));

        db.collection("users").document(userId).collection("programs")
                .document("details")
                .set(programsData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Programs updated successfully.");
                    Toast.makeText(getActivity(), "Programs updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update programs: " + e.getMessage());
                    Toast.makeText(getActivity(), "Failed to update programs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
