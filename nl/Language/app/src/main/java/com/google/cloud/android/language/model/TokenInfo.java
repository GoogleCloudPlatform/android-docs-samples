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

import android.os.Parcel;
import android.os.Parcelable;


/**
 * A {@link Parcelable} variant of {@link Token}.
 */
public class TokenInfo implements Parcelable {

    public static final Creator<TokenInfo> CREATOR = new Creator<TokenInfo>() {
        @Override
        public TokenInfo createFromParcel(Parcel in) {
            return new TokenInfo(in);
        }

        @Override
        public TokenInfo[] newArray(int size) {
            return new TokenInfo[size];
        }
    };

    /**
     * The token text.
     */
    public final String text;

    /**
     * The beginning offset of the content in the original document.
     */
    public final int beginOffset;

    /**
     * The token lemma (dictionary form).
     */
    public final String lemma;

    /**
     * The parts of speech tag for this token.
     */
    public final String partOfSpeech;

    /**
     * The head of this token in the dependency tree.
     */
    public final int headTokenIndex;

    /**
     * The parse label for the token.
     */
    public final String label;

    public TokenInfo(Token token) {
        final TextSpan textSpan = token.getText();
        if (textSpan == null) {
            text = null;
            beginOffset = -1;
        } else {
            text = textSpan.getContent();
            beginOffset = textSpan.getBeginOffset();
        }
        lemma = token.getLemma();
        final PartOfSpeech pos = token.getPartOfSpeech();
        if (pos != null) {
            this.partOfSpeech = pos.getTag();
        } else {
            this.partOfSpeech = null;
        }
        final DependencyEdge dependencyEdge = token.getDependencyEdge();
        if (dependencyEdge != null) {
            headTokenIndex = dependencyEdge.getHeadTokenIndex();
            label = dependencyEdge.getLabel();
        } else {
            headTokenIndex = -1;
            label = null;
        }
    }

    protected TokenInfo(Parcel in) {
        text = in.readString();
        beginOffset = in.readInt();
        lemma = in.readString();
        partOfSpeech = in.readString();
        headTokenIndex = in.readInt();
        label = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(text);
        out.writeInt(beginOffset);
        out.writeString(lemma);
        out.writeString(partOfSpeech);
        out.writeInt(headTokenIndex);
        out.writeString(label);
    }

}
