package com.example.userauthentication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DietaryPreferencesFragment extends Fragment {

    private CheckBox cbVegan, cbVegetarian, cbGlutenFree, cbKeto, cbMediterranean;
    private Button btnNext;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_dietary_preferences, container, false);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind views
        cbVegan = view.findViewById(R.id.cb_vegan);
        cbVegetarian = view.findViewById(R.id.cb_vegetarian);
        cbGlutenFree = view.findViewById(R.id.cb_gluten_free);
        cbKeto = view.findViewById(R.id.cb_keto);
        cbMediterranean = view.findViewById(R.id.cb_mediterranean);
        btnNext = view.findViewById(R.id.btn_next_dietary_preferences);

        // Handle button click
        btnNext.setOnClickListener(v -> saveDietaryPreferences());

        return view;
    }

    private void saveDietaryPreferences() {
        // Create a list to store selected dietary preferences
        List<String> preferences = new ArrayList<>();

        // Check if each checkbox is selected and add corresponding value to the list
        if (cbVegan.isChecked()) {
            preferences.add("Vegan");
        }
        if (cbVegetarian.isChecked()) {
            preferences.add("Vegetarian");
        }
        if (cbGlutenFree.isChecked()) {
            preferences.add("Gluten-Free");
        }
        if (cbKeto.isChecked()) {
            preferences.add("Keto");
        }
        if (cbMediterranean.isChecked()) {
            preferences.add("Mediterranean");
        }

        // If no preferences are selected, show a toast message
        if (preferences.isEmpty()) {
            Toast.makeText(getActivity(), "Please select at least one dietary preference.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare data to save in Firestore
        // Create a map to store preferences
        Map<String, Object> dietaryPreferencesData = new HashMap<>();
        dietaryPreferencesData.put("preference", preferences);

        // Save the dietary preferences data in Firestore
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("dietary_preferences")
                .document("details")
                .set(dietaryPreferencesData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Dietary preferences saved!", Toast.LENGTH_SHORT).show();

                    // Navigate to the next fragment or activity
                    IntermittentFastingFragment intermittentFastingFragment = new IntermittentFastingFragment();
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, intermittentFastingFragment)
                            .addToBackStack(null) // Optionally add to backstack to navigate back
                            .commit();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to save dietary preferences: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
