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

import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@RunWith(AndroidJUnit4.class)
public class EntityInfoTest {

    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final float SALIENCE = 0.5f;
    private static final String URL = "https://en.wikipedia.org/wiki/Android_(operating_system)";

    @Test
    public void newFromEntity() {
        final EntityInfo entity = new EntityInfo(createEntity());
        assertThat(entity.name, is(equalTo(NAME)));
        assertThat(entity.type, is(equalTo(TYPE)));
        assertThat(entity.salience, is(SALIENCE));
        assertThat(entity.wikipediaUrl, is(equalTo(URL)));
    }

    @Test
    public void parcel() {
        final EntityInfo original = new EntityInfo(createEntity());
        final Parcel parcel = Parcel.obtain();
        try {
            parcel.writeParcelable(original, 0);
            parcel.setDataPosition(0);
            final EntityInfo restored = parcel.readParcelable(getClass().getClassLoader());
            assertThat(restored.name, is(equalTo(original.name)));
            assertThat(restored.type, is(equalTo(original.type)));
            assertThat(restored.salience, is(original.salience));
            assertThat(restored.wikipediaUrl, is(equalTo(original.wikipediaUrl)));
        } finally {
            parcel.recycle();
        }
    }

    @NonNull
    private Entity createEntity() {
        final HashMap<String, String> metadata = new HashMap<>();
        metadata.put(EntityInfo.KEY_WIKIPEDIA_URL, URL);
        return new Entity()
                .setName(NAME)
                .setType(TYPE)
                .setSalience(SALIENCE)
                .setMetadata(metadata);
    }

}
