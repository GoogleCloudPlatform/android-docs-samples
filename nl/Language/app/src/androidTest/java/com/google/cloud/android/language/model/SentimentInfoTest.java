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

package com.google.cloud.android.language.model;

import com.google.api.services.language.v1.model.Sentiment;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@RunWith(AndroidJUnit4.class)
public class SentimentInfoTest {

    public static final float SCORE = -1.f;
    public static final float MAGNITUDE = 0.1f;

    @Test
    public void newFromSentiment() {
        final SentimentInfo sentiment = new SentimentInfo(createSentiment());
        assertThat(sentiment.score, is(SCORE));
        assertThat(sentiment.magnitude, is(MAGNITUDE));
    }

    @Test
    public void parcel() {
        final SentimentInfo original = new SentimentInfo(createSentiment());
        final Parcel parcel = Parcel.obtain();
        try {
            parcel.writeParcelable(original, 0);
            parcel.setDataPosition(0);
            final SentimentInfo restored = parcel.readParcelable(getClass().getClassLoader());
            assertThat(restored.score, is(original.score));
            assertThat(restored.magnitude, is(original.magnitude));
        } finally {
            parcel.recycle();
        }
    }

    private Sentiment createSentiment() {
        return new Sentiment()
                .setScore(SCORE)
                .setMagnitude(MAGNITUDE);
    }

}
