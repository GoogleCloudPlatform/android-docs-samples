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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import com.google.cloud.android.conversation.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class BubbleViewTest extends UiTest {

    private BubbleView mBubbleView;

    @Before
    public void setUp() {
        mBubbleView = activityRule.getActivity().findViewById(R.id.bubble);
    }

    @Test
    @MediumTest
    public void direction() throws Throwable {
        assertThat(mBubbleView.getDirection(), is(BubbleView.DIRECTION_INCOMING));
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBubbleView.setDirection(BubbleView.DIRECTION_OUTGOING);
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertThat(mBubbleView.getDirection(), is(BubbleView.DIRECTION_OUTGOING));
    }

}
