package com.example.userauthentication;

import android.content.Intent;
import android.os.Bundle;
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

public class ActivityLevelFragment extends Fragment {
    private RadioGroup rgActivityLevel;
    private Button btnNext;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_activity_level, container, false);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind views
        rgActivityLevel = view.findViewById(R.id.rg_activity_level);
        btnNext = view.findViewById(R.id.btn_next_activity_level);

        // Handle button click
        btnNext.setOnClickListener(v -> saveActivityLevelData());

        return view;
    }

    private void saveActivityLevelData() {
        // Get selected radio button
        int selectedId = rgActivityLevel.getCheckedRadioButtonId();
        double activityFactor;

        // Check which radio button is selected and assign values
        if (selectedId == R.id.rb_sedentary) {
            activityFactor = 1.2;
        } else if (selectedId == R.id.rb_lightly_active) {
            activityFactor = 1.375;
        } else if (selectedId == R.id.rb_moderately_active) {
            activityFactor = 1.55;
        } else if (selectedId == R.id.rb_active) {
            activityFactor = 1.725;
        } else if (selectedId == R.id.rb_very_active) {
            activityFactor = 1.9;
        } else {
            // If no option is selected, show a toast message
            Toast.makeText(getActivity(), "Please select an activity level.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare data to save in Firestore
        Map<String, Object> activityLevelData = new HashMap<>();
        activityLevelData.put("activity_factor", activityFactor);

        // Save data to Firestore
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("activity_level")
                .document("details")
                .set(activityLevelData) // Using set() instead of update() to create the document if it doesn't exist
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Activity level data saved!", Toast.LENGTH_SHORT).show();
                    // Navigate to the next fragment or activity
                    WeightGoalsFragment weightGoalsFragment = new WeightGoalsFragment();
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, weightGoalsFragment)
                            .addToBackStack(null) // Optionally add to backstack to navigate back
                            .commit();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to save activity level data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
