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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class UtteranceTest {

    @Test
    public void instantiate() {
        final Utterance utterance = new Utterance(Utterance.INCOMING, "Hello");
        assertThat(utterance.direction, is(Utterance.INCOMING));
        assertThat(utterance.text, is("Hello"));
    }

    @Test
    public void parcel() {
        final Utterance original = new Utterance(Utterance.INCOMING, "Hello");
        final Parcel parcel = Parcel.obtain();
        try {
            parcel.writeParcelable(original, 0);
            parcel.setDataPosition(0);
            final Utterance restored = parcel.readParcelable(getClass().getClassLoader());
            assertThat(restored.direction, is(Utterance.INCOMING));
            assertThat(restored.text, is("Hello"));
        } finally {
            parcel.recycle();
        }
    }

}
