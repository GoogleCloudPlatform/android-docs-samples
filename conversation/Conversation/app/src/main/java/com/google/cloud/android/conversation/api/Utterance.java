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

import android.os.Parcel;
import android.os.Parcelable;

public class Utterance implements Parcelable {

    public static final int INCOMING = 1;
    public static final int OUTGOING = 2;

    public final int direction;
    public final String text;

    public Utterance(int direction, String text) {
        this.direction = direction;
        this.text = text;
    }

    protected Utterance(Parcel in) {
        direction = in.readInt();
        text = in.readString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Utterance utterance = (Utterance) o;

        //noinspection SimplifiableIfStatement
        if (direction != utterance.direction) return false;
        return text != null ? text.equals(utterance.text) : utterance.text == null;

    }

    @Override
    public int hashCode() {
        int result = direction;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Utterance{" +
                "direction=" + direction +
                ", text='" + text + '\'' +
                '}';
    }

    public static final Creator<Utterance> CREATOR = new Creator<Utterance>() {
        @Override
        public Utterance createFromParcel(Parcel in) {
            return new Utterance(in);
        }

        @Override
        public Utterance[] newArray(int size) {
            return new Utterance[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(direction);
        parcel.writeString(text);
    }

}
