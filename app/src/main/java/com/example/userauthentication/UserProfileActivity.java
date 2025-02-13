package com.example.userauthentication;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        // Check if the user is logged in
        if (currentUser == null) {
            // If not logged in, redirect to LoginActivity
            Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Check profileComplete flag in Firestore
        String userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Boolean profileComplete = snapshot.getBoolean("profileComplete");
                if (profileComplete != null && profileComplete) {
                    // If profile is complete, redirect to DashboardActivity
                    startActivity(new Intent(UserProfileActivity.this, DashboardActivity.class));
                    finish();
                } else {
                    // If profile is not complete, start the profile setup flow
                    if (savedInstanceState == null) {
                        showFragment(new BasicDetailsFragment());
                    }
                }
            } else {
                // If no user document exists, start the profile setup flow
                if (savedInstanceState == null) {
                    showFragment(new BasicDetailsFragment());
                }
            }
        });
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
