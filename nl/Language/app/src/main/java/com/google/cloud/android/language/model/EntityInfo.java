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

import com.google.api.services.language.v1.model.Entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;

import java.util.Map;


/**
 * A {@link Parcelable} variant of {@link Entity}.
 */
public class EntityInfo implements Parcelable {

    public static final Creator<EntityInfo> CREATOR = new Creator<EntityInfo>() {
        @Override
        public EntityInfo createFromParcel(Parcel in) {
            return new EntityInfo(in);
        }

        @Override
        public EntityInfo[] newArray(int size) {
            return new EntityInfo[size];
        }
    };

    @VisibleForTesting
    static final String KEY_WIKIPEDIA_URL = "wikipedia_url";

    /**
     * The representative name for the entity.
     */
    public final String name;

    /**
     * The entity type.
     */
    public final String type;

    /**
     * The salience score associated with the entity in the [0, 1.0] range.
     */
    public final float salience;

    /**
     * The Wikipedia URL.
     */
    public final String wikipediaUrl;

    public EntityInfo(Entity entity) {
        name = entity.getName();
        type = entity.getType();
        salience = entity.getSalience();
        final Map<String, String> metadata = entity.getMetadata();
        if (metadata != null && metadata.containsKey(KEY_WIKIPEDIA_URL)) {
            wikipediaUrl = metadata.get(KEY_WIKIPEDIA_URL);
        } else {
            wikipediaUrl = null;
        }
    }

    protected EntityInfo(Parcel in) {
        name = in.readString();
        type = in.readString();
        salience = in.readFloat();
        wikipediaUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);
        out.writeString(type);
        out.writeFloat(salience);
        out.writeString(wikipediaUrl);
    }

}
