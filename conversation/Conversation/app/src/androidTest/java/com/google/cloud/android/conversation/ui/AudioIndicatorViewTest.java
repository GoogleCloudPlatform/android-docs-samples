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
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import com.google.cloud.android.conversation.R;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class AudioIndicatorViewTest extends UiTest {

    private AudioIndicatorView mAudioIndicatorView;

    @Before
    public void setUp() throws Throwable {
        mAudioIndicatorView = activityRule.getActivity().findViewById(R.id.indicator);
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAudioIndicatorView.setVisibility(View.VISIBLE);
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test
    @MediumTest
    public void hearingVoice() throws Throwable {
        assertThat(mAudioIndicatorView.isHearingVoice(), is(false));
        onView(withId(R.id.indicator)).perform(setHearingVoice(true));
        assertThat(mAudioIndicatorView.isHearingVoice(), is(true));
        onView(withId(R.id.indicator)).perform(setHearingVoice(false));
        assertThat(mAudioIndicatorView.isHearingVoice(), is(false));
    }

    private ViewAction setHearingVoice(final boolean hearingVoice) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(AudioIndicatorView.class);
            }

            @Override
            public String getDescription() {
                return "setHearingVoice";
            }

            @Override
            public void perform(UiController uiController, View view) {
                final AudioIndicatorView indicator = (AudioIndicatorView) view;
                indicator.setHearingVoice(hearingVoice);
            }
        };
    }

}
