package com.google.cloud.examples.dialogflow.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.cloud.examples.dialogflow.AppController;
import com.google.cloud.examples.dialogflow.R;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class WelcomeActivity extends AppCompatActivity {

    private Button btnNext;
    private CheckBox chkTTS;
    private CheckBox chkSentiment;
    private CheckBox chkKnowledge;
    private FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        getSupportActionBar().setTitle("Dialogflow Sample");

        btnNext = findViewById(R.id.btnNext);
        chkTTS = findViewById(R.id.chkTTS);
        chkSentiment = findViewById(R.id.chkSentiment);
        chkKnowledge = findViewById(R.id.chkKnowledge);

        getFirebaseInstanceId();

        signInAnonymously();
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

    private void setupButtons() {
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(WelcomeActivity.this, ChatActivity.class);

                // According to the selection of checkbox send the type of feature
                intent.putExtra("tts", chkTTS.isChecked());
                intent.putExtra("knowledge", chkKnowledge.isChecked());
                intent.putExtra("sentiment", chkSentiment.isChecked());

                startActivity(intent);
            }
        });
    }

}
