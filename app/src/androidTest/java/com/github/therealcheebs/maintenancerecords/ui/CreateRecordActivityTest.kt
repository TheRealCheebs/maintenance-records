package com.github.therealcheebs.maintenancerecords.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.assertion.ViewAssertions.matches
import com.github.therealcheebs.maintenancerecords.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateRecordActivityTest {
    @get:Rule
    val activityRule = ActivityTestRule(CreateRecordActivity::class.java)

    @Test
    fun testToolbarTitleIsDisplayed() {
        onView(withId(R.id.toolbar)).check(matches(withText("Create Record")))
    }

    @Test
    fun testSaveButtonDisabledWithEmptyFields() {
        // Try clicking save with empty fields
        onView(withId(R.id.btnSave)).perform(click())
        // You can add checks for error messages or Toasts here
    }

    @Test
    fun testEnterRecordAndSave() {
        onView(withId(R.id.editItemId)).perform(typeText("TestItem"))
        onView(withId(R.id.editDescription)).perform(typeText("Test Description"))
        onView(withId(R.id.editTechnician)).perform(typeText("Test Tech"))
        onView(withId(R.id.btnSave)).perform(click())
        // Add assertions for success Toast or navigation
    }
}
