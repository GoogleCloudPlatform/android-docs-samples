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

package com.google.cloud.android.conversation.api;

import static com.google.cloud.android.conversation.TestUtils.hasDirection;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.google.cloud.android.conversation.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;


@RunWith(AndroidJUnit4.class)
public class ConversationServiceTest {

    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    private ConversationService mService;
    private ConversationService.Listener mListener;

    @Before
    public void setUp() throws TimeoutException {
        mService = ConversationService.from(serviceRule.bindService(
                new Intent(InstrumentationRegistry.getTargetContext(), ConversationService.class)));
        mListener = mock(ConversationService.Listener.class);
        mService.addListener(mListener);
    }

    @After
    public void tearDown() {
        mService.removeListener(mListener);
        serviceRule.unbindService();
    }

    @Test
    @LargeTest
    public void detectIntentByText() {
        if (!mService.isApiReady()) {
            verify(mListener, timeout(TestUtils.API_TIMEOUT_MILLIS)).onApiReady();
        }
        mService.detectIntentByText("detectIntentByText");
        verify(mListener)
                .onNewUtterance(eq(new Utterance(Utterance.OUTGOING, "detectIntentByText")));
        verify(mListener, timeout(TestUtils.API_TIMEOUT_MILLIS))
                .onNewUtterance(argThat(hasDirection(Utterance.INCOMING)));
    }


}
