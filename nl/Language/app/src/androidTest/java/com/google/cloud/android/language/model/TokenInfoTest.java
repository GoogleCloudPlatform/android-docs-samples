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

import com.google.api.services.language.v1.model.DependencyEdge;
import com.google.api.services.language.v1.model.PartOfSpeech;
import com.google.api.services.language.v1.model.TextSpan;
import com.google.api.services.language.v1.model.Token;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@RunWith(AndroidJUnit4.class)
public class TokenInfoTest {

    private static final String TEXT = "text";
    private static final int BEGIN_OFFSET = 1;
    private static final String LEMMA = "lemma";
    private static final String PART_OF_SPEECH = "part_of_speech";
    private static final int HEAD_TOKEN_INDEX = 2;
    private static final String LABEL = "label";

    @Test
    public void newFromToken() {
        final TokenInfo token = new TokenInfo(createToken());
        assertThat(token.text, is(equalTo(TEXT)));
        assertThat(token.beginOffset, is(BEGIN_OFFSET));
        assertThat(token.lemma, is(equalTo(LEMMA)));
        assertThat(token.partOfSpeech, is(equalTo(PART_OF_SPEECH)));
        assertThat(token.headTokenIndex, is(HEAD_TOKEN_INDEX));
        assertThat(token.label, is(equalTo(LABEL)));
    }

    @Test
    public void parcel() {
        final TokenInfo original = new TokenInfo(createToken());
        final Parcel parcel = Parcel.obtain();
        try {
            parcel.writeParcelable(original, 0);
            parcel.setDataPosition(0);
            final TokenInfo restored = parcel.readParcelable(getClass().getClassLoader());
            assertThat(restored.text, is(equalTo(original.text)));
            assertThat(restored.beginOffset, is(original.beginOffset));
            assertThat(restored.lemma, is(equalTo(original.lemma)));
            assertThat(restored.partOfSpeech, is(equalTo(original.partOfSpeech)));
            assertThat(restored.headTokenIndex, is(original.headTokenIndex));
            assertThat(restored.label, is(equalTo(original.label)));
        } finally {
            parcel.recycle();
        }
    }

    private Token createToken() {
        return new Token()
                .setText(new TextSpan()
                        .setContent(TEXT)
                        .setBeginOffset(BEGIN_OFFSET))
                .setLemma(LEMMA)
                .setPartOfSpeech(new PartOfSpeech()
                        .setTag(PART_OF_SPEECH))
                .setDependencyEdge(new DependencyEdge()
                        .setHeadTokenIndex(HEAD_TOKEN_INDEX)
                        .setLabel(LABEL));
    }

}
