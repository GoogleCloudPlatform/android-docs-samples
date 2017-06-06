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

import android.os.Parcel;
import android.os.Parcelable;


/**
 * A {@link Parcelable} variant of {@link Sentiment}.
 */
public class SentimentInfo implements Parcelable {

    public static final Creator<SentimentInfo> CREATOR = new Creator<SentimentInfo>() {
        @Override
        public SentimentInfo createFromParcel(Parcel in) {
            return new SentimentInfo(in);
        }

        @Override
        public SentimentInfo[] newArray(int size) {
            return new SentimentInfo[size];
        }
    };

    /**
     * Score of the sentiment in the [-1.0, 1.0] range.
     */
    public final float score;

    /**
     * The absolute magnitude of sentiment in the [0, +inf) range.
     */
    public final float magnitude;

    public SentimentInfo(Sentiment sentiment) {
        score = sentiment.getScore();
        magnitude = sentiment.getMagnitude();
    }

    protected SentimentInfo(Parcel in) {
        score = in.readFloat();
        magnitude = in.readFloat();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(score);
        out.writeFloat(magnitude);
    }

}
