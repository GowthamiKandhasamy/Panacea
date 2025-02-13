package com.example.userauthentication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SignupActivity extends AppCompatActivity {
    private EditText emailField, passwordField;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        Button signupButton = findViewById(R.id.loginButton);
        TextView loginRedirect = findViewById(R.id.signupText);

        signupButton.setOnClickListener(view -> signupUser());
        loginRedirect.setOnClickListener(view -> {
            // Redirect to LoginActivity
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void signupUser() {
        String email = emailField.getText().toString();
        String password = passwordField.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();

                        // Create the user document with the "profileComplete" field set to false initially
                        HashMap<String, Object> userProfile = new HashMap<>();
                        userProfile.put("profileComplete", false); // Mark profile as incomplete

                        db.collection("users").document(userId)
                                .set(userProfile) // Create the user document with the profileComplete field
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(SignupActivity.this, "User profile created!", Toast.LENGTH_SHORT).show();

                                    // Redirect to the UserProfileActivity to complete the profile
                                    Intent intent = new Intent(SignupActivity.this, UserProfileActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignupActivity.this, "Failed to create user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
