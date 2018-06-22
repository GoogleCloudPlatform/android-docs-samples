/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.examples.speechrecognition

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)

    @Rule
    @JvmField
    val mActivityRule: ActivityTestRule<MainActivity> =
            ActivityTestRule(MainActivity::class.java)

    @Test
    fun isReadyForSpeech() {
        onView(allOf(withText(
                getString(R.string.start_talking)), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .check(matches(isDisplayed()))
    }

    private fun getString(id: Int) =
            InstrumentationRegistry.getTargetContext().resources.getString(id)

}
