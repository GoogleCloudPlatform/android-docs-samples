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

import com.google.cloud.android.conversation.api.Utterance;

import org.mockito.ArgumentMatcher;


public class TestUtils {

    /** Timeout limit for API calls. */
    public static final int API_TIMEOUT_MILLIS = 10000;

    private TestUtils() {
    }

    /**
     * Matches an {@link Utterance} with the specified {@code direction}.
     *
     * @param direction The direction. Either {@link Utterance#INCOMING} or {@link
     *                  Utterance#OUTGOING}.
     * @return The matcher.
     */
    public static ArgumentMatcher<Utterance> hasDirection(final int direction) {
        return new ArgumentMatcher<Utterance>() {
            @Override
            public boolean matches(Utterance utterance) {
                return utterance.direction == direction;
            }
        };
    }

}
