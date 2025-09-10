package com.github.therealcheebs.maintenancerecords.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.github.therealcheebs.maintenancerecords.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun testRedirectsToKeyManagerIfNoKey() {
        Intents.init()
        // Simulate no key present (you may need to mock NostrClient)
        // Check that KeyManagerActivity is launched
        onView(withId(R.id.toolbar)).check(matches(withText("Maintenance Records - Unknown Key")))
        Intents.intended(hasComponent(NostrKeyManagerActivity::class.java.name))
        Intents.release()
    }

    @Test
    fun testImportPromptWhenNoRecords() {
        // Simulate no records for current key (you may need to mock repository)
        // Check that import prompt or button is visible
        onView(withId(R.id.emptyStateText)).check(matches(withText("No records found for Unknown Key")))
        onView(withId(R.id.btnCreateRecord)).check(matches(withText("Create Record")))
    }
}
