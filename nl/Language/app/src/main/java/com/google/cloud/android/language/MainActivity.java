/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.android.language;

import com.google.cloud.android.language.model.EntityInfo;
import com.google.cloud.android.language.model.SentimentInfo;
import com.google.cloud.android.language.model.TokenInfo;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements ApiFragment.Callback {

    private static final int API_ENTITIES = 0;
    private static final int API_SENTIMENT = 1;
    private static final int API_SYNTAX = 2;

    private static final String FRAGMENT_API = "api";

    private static final int LOADER_ACCESS_TOKEN = 1;

    private static final String STATE_SHOWING_RESULTS = "showing_results";

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                // The icon button is clicked; start analyzing the input.
                case R.id.analyze:
                    startAnalyze();
                    break;
            }
        }
    };

    private final TextView.OnEditorActionListener mOnEditorActionListener
            = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // Enter pressed; Start analyzing the input.
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                startAnalyze();
                return true;
            }
            return false;
        }
    };

    private View mIntroduction;

    private View mResults;

    private View mProgress;

    private EditText mInput;

    private ViewPager mViewPager;

    private ResultPagerAdapter mAdapter;

    /**
     * Whether the result view is animating to hide.
     */
    private boolean mHidingResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIntroduction = findViewById(R.id.introduction);
        mResults = findViewById(R.id.results);
        mProgress = findViewById(R.id.progress);

        // Set up the input EditText so that it accepts multiple lines
        mInput = (EditText) findViewById(R.id.input);
        mInput.setHorizontallyScrolling(false);
        mInput.setMaxLines(Integer.MAX_VALUE);

        // Set up the view pager
        final FragmentManager fm = getSupportFragmentManager();
        mAdapter = new ResultPagerAdapter(fm, this);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        final Resources resources = getResources();
        mViewPager.setPageMargin(resources.getDimensionPixelSize(R.dimen.page_margin));
        mViewPager.setPageMarginDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.page_margin, getTheme()));
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mAdapter);
        TabLayout tab = (TabLayout) findViewById(R.id.tab);
        tab.setupWithViewPager(mViewPager);

        if (savedInstanceState == null) {
            // The app has just launched; handle share intent if it is necessary
            handleShareIntent();
        } else {
            // Configuration changes; restore UI states
            boolean results = savedInstanceState.getBoolean(STATE_SHOWING_RESULTS);
            if (results) {
                mIntroduction.setVisibility(View.GONE);
                mResults.setVisibility(View.VISIBLE);
                mProgress.setVisibility(View.INVISIBLE);
            } else {
                mResults.setVisibility(View.INVISIBLE);
            }
        }

        // Bind event listeners
        mInput.setOnEditorActionListener(mOnEditorActionListener);
        findViewById(R.id.analyze).setOnClickListener(mOnClickListener);

        // Prepare the API
        if (getApiFragment() == null) {
            fm.beginTransaction().add(new ApiFragment(), FRAGMENT_API).commit();
        }
        prepareApi();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_SHOWING_RESULTS, mResults.getVisibility() == View.VISIBLE);
    }

    private void handleShareIntent() {
        final Intent intent = getIntent();
        if (TextUtils.equals(intent.getAction(), Intent.ACTION_SEND)
                && TextUtils.equals(intent.getType(), "text/plain")) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                mInput.setText(text);
            }
        }
    }

    private ApiFragment getApiFragment() {
        return (ApiFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_API);
    }

    private void prepareApi() {
        // Initiate token refresh
        getSupportLoaderManager().initLoader(LOADER_ACCESS_TOKEN, null,
                new LoaderManager.LoaderCallbacks<String>() {
                    @Override
                    public Loader<String> onCreateLoader(int id, Bundle args) {
                        return new AccessTokenLoader(MainActivity.this);
                    }

                    @Override
                    public void onLoadFinished(Loader<String> loader, String token) {
                        getApiFragment().setAccessToken(token);
                    }

                    @Override
                    public void onLoaderReset(Loader<String> loader) {
                    }
                });
    }

    private void startAnalyze() {
        // Hide the software keyboard if it is up
        mInput.clearFocus();
        InputMethodManager ime = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        ime.hideSoftInputFromWindow(mInput.getWindowToken(), 0);

        // Show progress
        showProgress();

        // Call the API
        final String text = mInput.getText().toString();
        getApiFragment().analyzeEntities(text);
        getApiFragment().analyzeSentiment(text);
        getApiFragment().analyzeSyntax(text);
    }

    private void showProgress() {
        mIntroduction.setVisibility(View.GONE);
        if (mResults.getVisibility() == View.VISIBLE) {
            mHidingResult = true;
            ViewCompat.animate(mResults)
                    .alpha(0.f)
                    .setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(View view) {
                            mHidingResult = false;
                            view.setVisibility(View.INVISIBLE);
                        }
                    });
        }
        if (mProgress.getVisibility() == View.INVISIBLE) {
            mProgress.setVisibility(View.VISIBLE);
            ViewCompat.setAlpha(mProgress, 0.f);
            ViewCompat.animate(mProgress)
                    .alpha(1.f)
                    .setListener(null)
                    .start();
        }
    }

    private void showResults() {
        mIntroduction.setVisibility(View.GONE);
        if (mProgress.getVisibility() == View.VISIBLE) {
            ViewCompat.animate(mProgress)
                    .alpha(0.f)
                    .setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(View view) {
                            view.setVisibility(View.INVISIBLE);
                        }
                    });
        }
        if (mHidingResult) {
            ViewCompat.animate(mResults).cancel();
        }
        if (mResults.getVisibility() == View.INVISIBLE) {
            mResults.setVisibility(View.VISIBLE);
            ViewCompat.setAlpha(mResults, 0.01f);
            ViewCompat.animate(mResults)
                    .alpha(1.f)
                    .setListener(null)
                    .start();
        }
    }

    @Override
    public void onEntitiesReady(EntityInfo[] entities) {
        if (mViewPager.getCurrentItem() == API_ENTITIES) {
            showResults();
        }
        mAdapter.setEntities(entities);
    }

    @Override
    public void onSentimentReady(SentimentInfo sentiment) {
        if (mViewPager.getCurrentItem() == API_SENTIMENT) {
            showResults();
        }
        mAdapter.setSentiment(sentiment);
    }

    @Override
    public void onSyntaxReady(TokenInfo[] tokens) {
        if (mViewPager.getCurrentItem() == API_SYNTAX) {
            showResults();
        }
        mAdapter.setTokens(tokens);
    }

    /**
     * Provides content of the {@link ViewPager}.
     */
    public static class ResultPagerAdapter extends FragmentPagerAdapter {

        private final String[] mApiNames;

        private final Fragment[] mFragments = new Fragment[3];

        public ResultPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            mApiNames = context.getResources().getStringArray(R.array.api_names);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            mFragments[position] = fragment;
            return fragment;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case API_ENTITIES:
                    return EntitiesFragment.newInstance();
                case API_SENTIMENT:
                    return SentimentFragment.newInstance();
                case API_SYNTAX:
                    return SyntaxFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mApiNames[position];
        }

        public void setEntities(EntityInfo[] entities) {
            final EntitiesFragment fragment = (EntitiesFragment) mFragments[API_ENTITIES];
            if (fragment != null) {
                fragment.setEntities(entities);
            }
        }

        public void setSentiment(SentimentInfo sentiment) {
            final SentimentFragment fragment = (SentimentFragment) mFragments[API_SENTIMENT];
            if (fragment != null) {
                fragment.setSentiment(sentiment);
            }
        }

        public void setTokens(TokenInfo[] tokens) {
            final SyntaxFragment fragment = (SyntaxFragment) mFragments[API_SYNTAX];
            if (fragment != null) {
                fragment.setTokens(tokens);
            }
        }

    }

}
