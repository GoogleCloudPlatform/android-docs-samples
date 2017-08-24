/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.android.conversation.ui;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.cloud.android.conversation.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class InputHelperTest extends UiTest {

    private EditText mEditText;
    private ImageButton mToggle;
    private AudioIndicatorView mAudioIndicatorView;

    private InputHelper.Callback mCallback;
    private InputHelper mInputHelper;

    @Before
    public void setUp() throws Throwable {
        final UiTestActivity activity = activityRule.getActivity();
        mEditText = activity.findViewById(R.id.input);
        mToggle = activity.findViewById(R.id.toggle);
        mAudioIndicatorView = activity.findViewById(R.id.indicator);
        mCallback = mock(InputHelper.Callback.class);
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInputHelper = new InputHelper(mEditText, mToggle, mAudioIndicatorView, mCallback);
                mInputHelper.setEnabled(true);
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @After
    public void tearDown() {
        mInputHelper.release();
    }

    @Test
    @MediumTest
    public void initialState() {
        assertNotNull(mInputHelper);
        assertThat(mToggle.getContentDescription().toString(), is("Voice"));
        assertThat(mEditText.isEnabled(), is(true));
        assertThat(mToggle.isEnabled(), is(true));
        assertThat(mToggle.getVisibility(), is(View.VISIBLE));
        assertThat(mAudioIndicatorView.getVisibility(), is(View.INVISIBLE));
    }

    @Test
    @MediumTest
    public void setEnabled() throws Throwable {
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInputHelper.setEnabled(false);
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertThat(mEditText.isEnabled(), is(false));
        assertThat(mToggle.isEnabled(), is(false));
    }

    @Test
    @MediumTest
    public void type() {
        onView(withId(R.id.input)).perform(replaceText("Hello"));
        assertThat(mToggle.getContentDescription().toString(), is("Send"));
        onView(withId(R.id.toggle)).perform(click());
        verify(mCallback).onText(eq("Hello"));
        assertThat(mEditText.getText().toString(), is(""));
        assertThat(mToggle.getContentDescription().toString(), is("Voice"));
    }

    @Test
    @MediumTest
    public void audio() {
        when(mCallback.ensureRecordAudioPermission()).thenReturn(true);
        onView(withId(R.id.toggle)).perform(click());
        assertThat(mToggle.getContentDescription().toString(), is("Keyboard"));
        assertThat(mAudioIndicatorView.getVisibility(), is(View.VISIBLE));
        onView(withId(R.id.toggle)).perform(click());
        assertThat(mToggle.getContentDescription().toString(), is("Voice"));
    }

}
