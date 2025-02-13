package com.example.userauthentication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

public class BasicDetailsFragment extends Fragment {

    private EditText etName, etAge, etHeight, etWeight;
    private RadioGroup rgGender;
    private Button btnNext;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_basic_details, container, false);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind views
        etName = view.findViewById(R.id.et_name);
        etAge = view.findViewById(R.id.et_age);
        etHeight = view.findViewById(R.id.et_height);
        etWeight = view.findViewById(R.id.et_weight);
        rgGender = view.findViewById(R.id.rg_gender);
        btnNext = view.findViewById(R.id.btn_next_basic_details);

        // Handle button click
        btnNext.setOnClickListener(v -> saveBasicDetails());

        return view;
    }

    private void saveBasicDetails() {
        // Retrieve inputs
        String name = etName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        int selectedGenderId = rgGender.getCheckedRadioButtonId();

        // Validate inputs
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(ageStr) || TextUtils.isEmpty(heightStr) || TextUtils.isEmpty(weightStr) || selectedGenderId == -1) {
            Toast.makeText(getActivity(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse numeric inputs
        int age = Integer.parseInt(ageStr);
        int height = Integer.parseInt(heightStr);
        int weight = Integer.parseInt(weightStr);

        // Get selected gender
        RadioButton selectedGenderButton = getView().findViewById(selectedGenderId);
        String gender = selectedGenderButton.getText().toString();

        // Calculate calorie needs using Harris-Benedict Equation (Example Calculation)
        double calorieNeeds;
        if ("Male".equalsIgnoreCase(gender)) {
            calorieNeeds = 10 * weight + 6.25 * height - 5 * age + 5;
        } else {
            calorieNeeds = 10 * weight + 6.25 * height - 5 * age - 161;
        }

        // Prepare data to save
        Map<String, Object> basicDetails = new HashMap<>();
        basicDetails.put("name", name);
        basicDetails.put("age", age);
        basicDetails.put("height", height);
        basicDetails.put("weight", weight);
        basicDetails.put("gender", gender);
        basicDetails.put("calorie_needs", Math.round(calorieNeeds));

        // Save data to Firestore
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("basic_details")
                .document("details")
                .set(basicDetails)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Basic details saved!", Toast.LENGTH_SHORT).show();

                    // Navigate to MedicationsFragment
                    MedicationsFragment medicationsFragment = new MedicationsFragment();
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, medicationsFragment)
                            .addToBackStack(null) // Optionally add to backstack to navigate back
                            .commit();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to save details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
