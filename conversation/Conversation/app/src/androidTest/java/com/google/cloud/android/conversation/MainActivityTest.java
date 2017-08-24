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

package com.google.cloud.android.conversation;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static com.google.cloud.android.conversation.TestUtils.hasDirection;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.support.test.filters.LargeTest;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import com.google.cloud.android.conversation.api.ConversationService;
import com.google.cloud.android.conversation.api.Utterance;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO);

    private ConversationService.Listener mListener;

    @Before
    public void setUp() {
        final ConversationService service = activityRule.getActivity().mConversationService;
        mListener = mock(ConversationService.Listener.class);
        service.addListener(mListener);
    }

    @After
    public void tearDown() {
        activityRule.getActivity().mConversationService.removeListener(mListener);
    }

    @Test
    @MediumTest
    public void initialState() {
        onView(withId(R.id.text)).check(matches(isDisplayed()));
        onView(withId(R.id.toggle)).check(matches(isDisplayed()));
        onView(withId(R.id.history)).check(matches(isDisplayed()));
    }

    @Test
    @LargeTest
    public void detectIntentByText() {
        verify(mListener, timeout(TestUtils.API_TIMEOUT_MILLIS)).onApiReady();
        onView(withId(R.id.text)).perform(replaceText("MainActivityTest"));
        onView(withId(R.id.toggle)).perform(click());
        verify(mListener)
                .onNewUtterance(eq(new Utterance(Utterance.OUTGOING, "MainActivityTest")));
        onView(withId(R.id.history)).check(matches(hasDescendant(withText("MainActivityTest"))));
        verify(mListener, timeout(TestUtils.API_TIMEOUT_MILLIS))
                .onNewUtterance(argThat(hasDirection(Utterance.INCOMING)));
    }

    @Test
    @LargeTest
    public void switchToVoice() {
        verify(mListener, timeout(TestUtils.API_TIMEOUT_MILLIS)).onApiReady();
        onView(withId(R.id.toggle)).perform(click());
        onView(withId(R.id.indicator)).check(matches(isDisplayed()));
    }

}
