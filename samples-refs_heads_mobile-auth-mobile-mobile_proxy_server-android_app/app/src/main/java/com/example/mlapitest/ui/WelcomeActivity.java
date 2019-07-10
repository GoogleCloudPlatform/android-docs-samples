package com.example.mlapitest.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.mlapitest.AppController;
import com.example.mlapitest.R;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Arrays;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    public static RequestQueue requestQueue;
    private static FirebaseUser firebaseUser;
    private Button btnNext;
    private CheckBox chkTTS;
    private CheckBox chkSentiment;
    private CheckBox chkKnowledge;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        getSupportActionBar().setTitle("Dialog Flow Sample");

        btnNext = findViewById(R.id.btnNext);
        chkTTS = findViewById(R.id.chkTTS);
        chkSentiment = findViewById(R.id.chkSentiment);
        chkKnowledge = findViewById(R.id.chkKnowledge);

        // Instantiate the RequestQueue.
        requestQueue = Volley.newRequestQueue(this);

        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build());


        // Sign in code
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);

        chkKnowledge.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    chkSentiment.setChecked(false);
                    chkTTS.setChecked(false);
                }
            }
        });

        chkSentiment.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    chkKnowledge.setChecked(false);
                    chkTTS.setChecked(false);
                }
            }
        });

        chkTTS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    chkSentiment.setChecked(false);
                    chkKnowledge.setChecked(false);
                }
            }
        });

        getFirebaseInstanceId();
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
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                System.out.println("SUCCESS");
                firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                setupButtons();
            } else {
                System.out.println("FAILURE");
            }
        }
    }

    private void setupButtons() {
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String path = "android.resource://" + getPackageName() + "/" + R.raw.book_a_room;

                Intent intent = new Intent(WelcomeActivity.this, ChatActivity.class);

                // According to the selection of checkbox send the type of feature
                if (chkTTS.isChecked()) {
                    intent.putExtra("type", "tts");
                } else if (chkKnowledge.isChecked()) {
                    intent.putExtra("type", "knowledge");
                } else if (chkSentiment.isChecked()) {
                    intent.putExtra("type", "sentiment");
                }

                if(!intent.hasExtra("type")) {
                    Toast.makeText(WelcomeActivity.this, "Please select a type to continue.", Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(intent);
            }
        });
    }

}
