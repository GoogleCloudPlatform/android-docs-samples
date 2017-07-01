package com.example.migration.endpoints.endpointsframeworksmigrationexample;


import android.os.SystemClock;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void mainActivityTest() {
        ViewInteraction nameInput = onView(allOf(withId(R.id.person_name_input), isDisplayed()));
        nameInput.perform(replaceText("Human 1"));

        ViewInteraction sendButton = onView(
                allOf(withId(R.id.send_bn), withText("Send"), isDisplayed()));
        sendButton.perform(click());

        // Sleep for a 1 second to receive a response and update R.id.response_view
        SystemClock.sleep(1000);

        onView(withId(R.id.response_view)).check(matches(withText("Hi, Human 1!")));
    }
}
