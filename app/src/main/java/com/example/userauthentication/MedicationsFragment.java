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

public class MedicationsFragment extends Fragment {

    private RadioGroup rgMedications;
    private Button btnNext;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_medications, container, false);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind views
        rgMedications = view.findViewById(R.id.rg_medications);
        btnNext = view.findViewById(R.id.btn_next_medications);

        // Handle button click
        btnNext.setOnClickListener(v -> saveMedicationsData());

        return view;
    }

    private void saveMedicationsData() {
        // Get selected radio button
        int selectedId = rgMedications.getCheckedRadioButtonId();
        boolean appetiteReducing = false;
        boolean dietaryFatReduction = false;
        boolean interested = false;

        // Check which radio button is selected and assign values
        if (selectedId == R.id.rb_medications_appetite_reducing) {
            appetiteReducing = true;
        } else if (selectedId == R.id.rb_medications_dietary_fat_reduction) {
            dietaryFatReduction = true;
        } else if (selectedId == R.id.rb_medications_no_interested) {
            interested = true;
        }
        // If no selection, default values remain as false

        // Prepare data to save in Firestore
        Map<String, Object> medicationsData = new HashMap<>();
        medicationsData.put("appetite_reducing", appetiteReducing);
        medicationsData.put("dietary_fat_absorption_reduction", dietaryFatReduction);
        medicationsData.put("interested", interested);

        // Save data to Firestore
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("medications")
                .document("details")
                .set(medicationsData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Medications data saved!", Toast.LENGTH_SHORT).show();

                    // Navigate to the next fragment or activity
                    // Assuming the next step is another fragment/activity
                    ActivityLevelFragment activityLevelFragment = new ActivityLevelFragment();
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, activityLevelFragment)
                            .addToBackStack(null) // Optionally add to backstack to navigate back
                            .commit();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to save medications data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
