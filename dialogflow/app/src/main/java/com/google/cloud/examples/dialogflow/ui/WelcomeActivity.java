package com.google.cloud.examples.dialogflow.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.cloud.examples.dialogflow.AppController;
import com.google.cloud.examples.dialogflow.R;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Arrays;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    public static RequestQueue requestQueue;
    private static FirebaseAuth firebaseAuth;
    private Button btnNext;
    private Button btnSignInGoogle;
    private Button btnSignInAnonymous;
    private CheckBox chkTTS;
    private CheckBox chkSentiment;
    private CheckBox chkKnowledge;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        getSupportActionBar().setTitle("Dialogflow Sample");

        btnNext = findViewById(R.id.btnNext);
        btnSignInAnonymous = findViewById(R.id.btnSignInAnonymous);
        btnSignInGoogle = findViewById(R.id.btnSignInGoogle);
        chkTTS = findViewById(R.id.chkTTS);
        chkSentiment = findViewById(R.id.chkSentiment);
        chkKnowledge = findViewById(R.id.chkKnowledge);

        // Instantiate the RequestQueue.
        requestQueue = Volley.newRequestQueue(this);

        final List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build());


        btnSignInAnonymous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInAnonymously();
            }
        });

        btnSignInGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sign in code
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN);
            }
        });

        getFirebaseInstanceId();
    }

    private void signInAnonymously() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(WelcomeActivity.this, "Sign In was successful",
                                    Toast.LENGTH_SHORT).show();
                            setupButtons();
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(WelcomeActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });

    }

    private void getFirebaseInstanceId() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String deviceToken = instanceIdResult.getToken();
                AppController.firebaseInstanceId = deviceToken;
                Log.i("fcmId", deviceToken);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                System.out.println("SUCCESS");
                setupButtons();
            } else {
                System.out.println("FAILURE");
            }
        }
    }

    private void setupButtons() {
        btnSignInGoogle.setVisibility(View.GONE);
        btnSignInAnonymous.setVisibility(View.GONE);
        btnNext.setVisibility(View.VISIBLE);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(WelcomeActivity.this, ChatActivity.class);

                // According to the selection of checkbox send the type of feature
                if (chkTTS.isChecked()) {
                    intent.putExtra("tts", true);
                }
                if (chkKnowledge.isChecked()) {
                    intent.putExtra("knowledge", true);
                }
                if (chkSentiment.isChecked()) {
                    intent.putExtra("sentiment", true);
                }

                if(!chkTTS.isChecked() && !chkKnowledge.isChecked() && !chkTTS.isChecked()) {
                    Toast.makeText(WelcomeActivity.this, "Please select atleast one type to continue.", Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(intent);
            }
        });
    }

}
