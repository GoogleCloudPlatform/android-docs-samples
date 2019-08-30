package com.example.translate.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.translate.AppController;
import com.example.translate.R;
import com.example.translate.adapter.LanguageAdapter;
import com.example.translate.adapter.VoiceAdapter;
import com.example.translate.utils.ApiRequest;
import com.example.translate.utils.AuthUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.cloud.texttospeech.v1beta1.Voice;
import com.google.cloud.translate.Language;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.iid.InstanceIdResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {


    private Spinner spTranslateFrom;
    private Spinner spTranslateTo;
    private Spinner spTtsType;
    private Spinner spVoiceType;
    private Button btnProceed;
    private AlertDialog alert;
    private String languageListCode = "";
    private ApiRequest apiRequest;

    private ArrayList<Language> sourceLanguageList = new ArrayList<>();
    private ArrayList<Language> targetLanguageList = new ArrayList<>();
    private ArrayList<Voice> voicesList = new ArrayList<>();
    private ArrayList<Voice> filteredVoicesList = new ArrayList<>();
    private ArrayList<String> ttsTypes = new ArrayList<>();
    private LanguageAdapter sourceLanguageListArrayAdapter;
    private LanguageAdapter targetLanguageListArrayAdapter;
    private VoiceAdapter voicesListArrayAdapter;
    private ArrayAdapter<String> ttsListArrayAdapter;

    /**
     * Broadcast receiver to hide the progress dialog when token is received
     */
    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            alert.dismiss();
            fetchSupportedLanguages(languageListCode);
        }
    };

    private void fetchSupportedLanguages(String sourceLanguageCode) {
        if (AuthUtils.expiryTime != null && !AuthUtils.token.equals("") && AuthUtils.expiryTime.getTime() > System.currentTimeMillis()) {
            new LanguageList(AuthUtils.token, AuthUtils.expiryTime, sourceLanguageCode).execute();
        } else {
            // get new token if expired or not received
            languageListCode = sourceLanguageCode;
            getNewToken();
        }
    }

    private void fetchVoiceslist(String targetLanguageCode) {
        if (AuthUtils.expiryTime != null && !AuthUtils.token.equals("") && AuthUtils.expiryTime.getTime() > System.currentTimeMillis()) {
            new VoicesList(AuthUtils.token, AuthUtils.expiryTime, targetLanguageCode).execute();
        } else {
            // get new token if expired or not received
            languageListCode = targetLanguageCode;
            getNewToken();
        }
    }

    private void getNewToken() {
        showProgressDialog();
        if (AuthUtils.checkSignIn()) {
            AuthUtils.callFirebaseFunction();
        }
    }

    /**
     * function to show the progress dialog
     */
    private void showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Fetching auth token...");
        builder.setCancelable(false);

        alert = builder.create();
        alert.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (AppController.PROJECT_ID.equals("GCP_PROJECT_ID")) {
            Toast.makeText(this, "Please update the GCP_PROJECT_ID in strings.xml", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        checkPermissions();

        apiRequest = new ApiRequest();

        initViews();
        initListeners();


        AuthUtils.signInAnonymously(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    AuthUtils.getFirebaseInstanceId(new OnSuccessListener<InstanceIdResult>() {
                        @Override
                        public void onSuccess(InstanceIdResult instanceIdResult) {
                            String deviceToken = instanceIdResult.getToken();
                            AuthUtils.firebaseInstanceId = deviceToken;
                            Log.i("fcmId", deviceToken);
                            fetchSupportedLanguages("");
                        }
                    });

                    Toast.makeText(SettingsActivity.this, "Sign In was successful",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(SettingsActivity.this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        loadTtsTypes();

    }

    private void loadTtsTypes() {
        ttsTypes.add("WaveNet");
        ttsTypes.add("Standard");
        ttsListArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, ttsTypes);
        spTtsType.setAdapter(ttsListArrayAdapter);

        filterVoiceTypes();
    }

    private void loadVoices() {
        voicesListArrayAdapter = new VoiceAdapter(SettingsActivity.this, android.R.layout.simple_list_item_1, filteredVoicesList);
        spVoiceType.setAdapter(voicesListArrayAdapter);
    }

    private void initViews() {
        spTranslateFrom = findViewById(R.id.spTranslateFrom);
        spTranslateTo = findViewById(R.id.spTranslateTo);
        spTtsType = findViewById(R.id.spTtsType);
        spVoiceType = findViewById(R.id.spVoiceType);
        btnProceed = findViewById(R.id.btnProceed);
    }

    private void initListeners() {
        spTranslateFrom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fetchSupportedLanguages(sourceLanguageList.get(position).getCode());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spTranslateTo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fetchVoiceslist(targetLanguageList.get(position).getCode());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spTtsType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                filterVoiceTypes();

                loadVoices();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        btnProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                intent.putExtra("sourceLanguageCode", sourceLanguageList.get(spTranslateFrom.getSelectedItemPosition()).getCode());
                intent.putExtra("targetLanguageCode", targetLanguageList.get(spTranslateTo.getSelectedItemPosition()).getCode());
                if (filteredVoicesList.size() > 0) {
                    intent.putExtra("ssmlGenderValue", filteredVoicesList.get(spVoiceType.getSelectedItemPosition()).getSsmlGenderValue());
                }
                startActivity(intent);
            }
        });
    }

    private void filterVoiceTypes() {
        filteredVoicesList = new ArrayList<>();

        for (int i = 0; i < voicesList.size(); i++) {
            if (voicesList.get(i).getName().toLowerCase().contains(ttsTypes.get(spTtsType.getSelectedItemPosition()).toLowerCase())) {
                filteredVoicesList.add(voicesList.get(i));
            }
        }
    }

    public void checkPermissions() {

        ArrayList<String> arrPerm = new ArrayList<>();
        arrPerm.add(Manifest.permission.INTERNET);
        arrPerm.add(Manifest.permission.RECORD_AUDIO);
        arrPerm.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        arrPerm.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (!arrPerm.isEmpty()) {
            String[] permissions = new String[arrPerm.size()];
            permissions = arrPerm.toArray(permissions);
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(AppController.TOKEN_RECEIVED);
        registerReceiver(br, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (br != null) {
            try {
                unregisterReceiver(br);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private class LanguageList extends AsyncTask<Void, Void, List<Language>> {

        private String sourceLanguageCode;
        private String token;
        private Date expiryTime;

        public LanguageList(String token, Date expiryTime, String sourceLanguageCode) {
            this.token = token;
            this.expiryTime = expiryTime;
            this.sourceLanguageCode = sourceLanguageCode;
        }

        @Override
        protected List<Language> doInBackground(Void... voids) {
            return apiRequest.getSupportedLanguages(token, expiryTime, sourceLanguageCode);
        }

        @Override
        protected void onPostExecute(List<Language> response) {
            super.onPostExecute(response);
            if (TextUtils.isEmpty(sourceLanguageCode)) {
                sourceLanguageList.addAll(response);
                sourceLanguageListArrayAdapter = new LanguageAdapter(SettingsActivity.this, android.R.layout.simple_list_item_1, sourceLanguageList);
                spTranslateFrom.setAdapter(sourceLanguageListArrayAdapter);
            } else {
                targetLanguageList.addAll(response);
                targetLanguageListArrayAdapter = new LanguageAdapter(SettingsActivity.this, android.R.layout.simple_list_item_1, targetLanguageList);
                spTranslateTo.setAdapter(targetLanguageListArrayAdapter);
            }
        }
    }

    private class VoicesList extends AsyncTask<Void, Void, List<Voice>> {

        private String sourceLanguageCode;
        private String token;
        private Date expiryTime;

        public VoicesList(String token, Date expiryTime, String sourceLanguageCode) {
            this.token = token;
            this.expiryTime = expiryTime;
            this.sourceLanguageCode = sourceLanguageCode;
        }

        @Override
        protected List<Voice> doInBackground(Void... voids) {
            return apiRequest.listVoices(token, expiryTime, sourceLanguageCode);
        }

        @Override
        protected void onPostExecute(List<Voice> response) {
            super.onPostExecute(response);
            if (response != null) {
                voicesList.addAll(response);
                filteredVoicesList.addAll(response);
                loadVoices();
            }
        }
    }
}
