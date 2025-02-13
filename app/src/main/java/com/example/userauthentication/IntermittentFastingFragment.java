package com.example.userauthentication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class IntermittentFastingFragment extends Fragment {
    private static final String TAG = "IntermittentFastingFragment";  // Add logging tag
    private RadioGroup rgFasting, rgFastingWindow;
    private RadioButton rbFastingYes, rbFastingNo, rb12_12, rb14_10, rb16_8, rb18_6;
    private Button btnGeneratePlan;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private View fastingWindowSection;
    private View fastingWindowTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_intermittent_fasting, container, false);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind views
        rgFasting = view.findViewById(R.id.rg_fasting);
        rgFastingWindow = view.findViewById(R.id.rg_fasting_window);
        rbFastingYes = view.findViewById(R.id.rb_fasting_yes);
        rbFastingNo = view.findViewById(R.id.rb_fasting_no);
        rb12_12 = view.findViewById(R.id.rb_12_12);
        rb14_10 = view.findViewById(R.id.rb_14_10);
        rb16_8 = view.findViewById(R.id.rb_16_8);
        rb18_6 = view.findViewById(R.id.rb_18_6);
        btnGeneratePlan = view.findViewById(R.id.btn_generate_plan);
        fastingWindowSection = view.findViewById(R.id.rg_fasting_window);
        fastingWindowTitle = view.findViewById(R.id.tv_fasting_window_title);

        // Initially hide the fasting window options
        fastingWindowSection.setVisibility(View.GONE);
        fastingWindowTitle.setVisibility(View.GONE);

        // Handle fasting option selection
        rgFasting.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG, "Fasting selected: " + checkedId);  // Log the fasting selection
            if (checkedId == R.id.rb_fasting_yes) {
                fastingWindowSection.setVisibility(View.VISIBLE);
                fastingWindowTitle.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.rb_fasting_no) {
                fastingWindowSection.setVisibility(View.GONE);
                fastingWindowTitle.setVisibility(View.GONE);
            }
        });

        // Handle Generate Plan button click
        btnGeneratePlan.setOnClickListener(v -> {
            Log.d(TAG, "Generate Plan clicked.");
            saveFastingData();
        });

        return view;
    }

    private void saveFastingData() {
        // Get the fasting option (Yes or No)
        boolean isFasting = rbFastingYes.isChecked();
        Log.d(TAG, "Is fasting: " + isFasting);

        // Determine the fasting window if Yes is selected
        String fastingWindow = "";
        if (isFasting) {
            if (rb12_12.isChecked()) {
                fastingWindow = "12/12";
            } else if (rb14_10.isChecked()) {
                fastingWindow = "14/10";
            } else if (rb16_8.isChecked()) {
                fastingWindow = "16/8";
            } else if (rb18_6.isChecked()) {
                fastingWindow = "18/6";
            }
            Log.d(TAG, "Fasting window: " + fastingWindow);
        }

        // Prepare data to save in Firestore
        Map<String, Object> fastingData = new HashMap<>();
        fastingData.put("isFasting", isFasting);
        fastingData.put("fastingWindow", fastingWindow);

        // Check if the user is authenticated
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(getActivity(), "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save data to Firestore
        Log.d(TAG, "Saving data to Firestore for userId: " + userId);
        db.collection("users").document(userId).collection("intermittent_fasting")
                .document("details")
                .set(fastingData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Data saved successfully.");
                    Toast.makeText(getActivity(), "Intermittent fasting data saved!", Toast.LENGTH_SHORT).show();

                    // Navigate to the next fragment (ProgramsFragment)
                    ProgramsFragment programsFragment = new ProgramsFragment();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, programsFragment)
                            .addToBackStack(null) // Optionally add to backstack to navigate back
                            .commit();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save intermittent fasting data: " + e.getMessage());
                    Toast.makeText(getActivity(), "Failed to save intermittent fasting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
