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
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class MessageDialogFragmentTest extends UiTest {

    private MessageDialogFragment.Listener mListener;
    private MessageDialogFragment mMessageDialogFragment;

    @Before
    public void setUp() {
        mListener = mock(MessageDialogFragment.Listener.class);
        activityRule.getActivity().setMessageDialogFragmentListener(mListener);
        mMessageDialogFragment = MessageDialogFragment.newInstance("MessageDialogFragmentTest");
    }

    @Test
    @MediumTest
    public void showAndDismiss() {
        mMessageDialogFragment.show(activityRule.getActivity().getSupportFragmentManager(), "a");
        onView(withText("MessageDialogFragmentTest")).check(matches(isDisplayed()));
        onView(withText(android.R.string.ok)).perform(click());
        verify(mListener).onMessageDialogDismissed();
    }

}
