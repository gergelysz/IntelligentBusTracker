package com.example.intelligentbustracker.activity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.intelligentbustracker.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
internal class MainActivityTest {

    @get:Rule
    val activityRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
    }

    @Test
    fun testViewItems() {
        /** Test Button Views */
        onView(withId(R.id.load_map_button)).check(matches(isDisplayed()))
        onView(withId(R.id.settings_button)).check(matches(isDisplayed()))
        onView(withId(R.id.schedules_button)).check(matches(isDisplayed()))

        /** Test Image View */
        onView(withId(R.id.bus_logo_main_imageview)).check(matches(isDisplayed()))

        /** Test Text Views */
        onView(withId(R.id.bus_logo_main_textView))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.bustracker)))
        onView(withId(R.id.bus_logo_main_description_textView))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.keep_track_of_your_public_transportation)))
    }

    @After
    fun tearDown() {
    }
}