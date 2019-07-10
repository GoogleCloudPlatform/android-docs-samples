package com.example.mlapitest.ui;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.mlapitest.AppController;
import com.example.mlapitest.R;
import com.example.mlapitest.utils.ApiRequest;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Firebase Login
    private static final int RC_SIGN_IN = 123;

    // User
    private static FirebaseUser firebaseUser;
    private ApiRequest apiRequest;


    public static TextView resultsTextView;
    public static RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiate the RequestQueue.
        requestQueue = Volley.newRequestQueue(this);
        resultsTextView = findViewById(R.id.result_text);

        findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Choose authentication providers
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.GoogleBuilder().build());

                // Create and launch sign-in intent
                startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);
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
                // Successfully signed in
                firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                apiRequest = new ApiRequest();
                setupButtons();


                // ...
            } else {
                System.out.println("FAILURE");

                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }

    private void setupButtons() {
        findViewById(R.id.make_request).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    AssetManager am = getAssets();
                    InputStream inputStream = am.open("book_a_room.wav");
                    File file = createFileFromInputStream(inputStream);
                    if(AppController.exipryTime!=null && !AppController.token.equals("") && AppController.exipryTime.getTime()>System.currentTimeMillis()) {
                        apiRequest.callAudioAPI(v.getContext(), AppController.token, AppController.exipryTime,  file.getPath());
                    } else {
                        getNewToken();
                    }
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        findViewById(R.id.clear_output).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultsTextView.setText(null);
            }
        });
    }

    private void getNewToken() {
        AppController.callFirebaseFunction();
    }

    private File createFileFromInputStream(InputStream inputStream) {

        try{
            File f = new File(Environment.getExternalStorageDirectory().getPath() + "/book_a_room.wav");
            OutputStream outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length = 0;

            while((length=inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }

            outputStream.close();
            inputStream.close();

            return f;
        }catch (IOException e) {
            //Logging exception
        }

        return null;
    }
}
