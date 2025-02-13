package com.example.userauthentication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProgramsFragment extends Fragment {
    private static final String TAG = "ProgramsFragment";

    private RadioGroup rgProgramPace;
    private Button btnSaveProgram;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_programs, container, false);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind views
        rgProgramPace = view.findViewById(R.id.rg_program_pace);
        btnSaveProgram = view.findViewById(R.id.btn_save_program);

        // Handle button click
        btnSaveProgram.setOnClickListener(v -> saveProgramData());

        Log.d(TAG, "onCreateView: Fragment view created successfully.");
        return view;
    }

    private void saveProgramData() {
        Log.d(TAG, "saveProgramData: Starting to save program data...");

        // Get selected radio button
        int selectedId = rgProgramPace.getCheckedRadioButtonId();
        String chosenPace;
        if (selectedId == R.id.rb_steady) {
            chosenPace = "steady";
        } else if (selectedId == R.id.rb_relaxed) {
            chosenPace = "relaxed";
        } else if (selectedId == R.id.rb_vigorous) {
            chosenPace = "vigorous";
        } else {
            Toast.makeText(getActivity(), "Please select a program pace.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "saveProgramData: No pace selected.");
            return;
        }
        Log.d(TAG, "saveProgramData: Selected pace is " + chosenPace);

        // Get user ID
        String userId;
        try {
            userId = auth.getCurrentUser().getUid();
        } catch (NullPointerException e) {
            Log.e(TAG, "saveProgramData: Current user is null. User not logged in.", e);
            Toast.makeText(getActivity(), "User not logged in. Please log in and try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "saveProgramData: User ID is " + userId);

        // Fetch program details for the chosen pace
        db.collection("users").document(userId).collection("programs").document("details")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "saveProgramData: Program details document exists.");
                        // Retrieve field values dynamically
                        Object caloriesObj = documentSnapshot.get(chosenPace + "Calories");
                        Object durationObj = documentSnapshot.get(chosenPace + "Duration");
                        String chosenCalories = caloriesObj != null ? caloriesObj.toString() : null;
                        String chosenDuration = durationObj != null ? durationObj.toString() : null;
                        Log.d(TAG, "saveProgramData: Chosen calories: " + chosenCalories + ", Chosen duration: " + chosenDuration);

                        if (chosenCalories == null || chosenDuration == null) {
                            Toast.makeText(getActivity(), "Program details are incomplete.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "saveProgramData: Missing program details. Calories or Duration is null.");
                            return;
                        }

                        // Prepare data to save in Firestore
                        Map<String, Object> programData = new HashMap<>();
                        programData.put("chosenPace", chosenPace);
                        programData.put("chosenDailyCalories", chosenCalories);
                        programData.put("chosenDuration", chosenDuration);
                        programData.put("waterIntake", 0);

                        // Save data to Firestore
                        db.collection("dashboard").document(userId)
                                .collection("profile_details").document("details")
                                .set(programData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "saveProgramData: Program data saved successfully.");
                                    Toast.makeText(getActivity(), "Program data saved!", Toast.LENGTH_SHORT).show();

                                    // Update profileComplete to true
                                    db.collection("users").document(userId)
                                            .update("profileComplete", true)
                                            .addOnSuccessListener(unused -> {
                                                Log.d(TAG, "saveProgramData: Profile marked as complete.");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "saveProgramData: Failed to update profileComplete.", e);
                                            });

                                    // Navigate to DashboardActivity
                                    Intent intent = new Intent(getActivity(), DashboardActivity.class);
                                    startActivity(intent);
                                    Log.d(TAG, "saveProgramData: Navigating to DashboardActivity.");
                                    getActivity().finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "saveProgramData: Failed to save program data.", e);
                                    Toast.makeText(getActivity(), "Failed to save program data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.w(TAG, "saveProgramData: Program details document does not exist.");
                        Toast.makeText(getActivity(), "Program details not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveProgramData: Failed to fetch program details.", e);
                    Toast.makeText(getActivity(), "Failed to fetch program details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
