package com.github.therealcheebs.maintenancerecords.ui

import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.github.therealcheebs.maintenancerecords.R

object ToolbarHelper {
    fun setupToolbar(
        activity: AppCompatActivity,
        toolbar: MaterialToolbar,
        title: String,
        showBackButton: Boolean,
        onMenuItemClick: (MenuItem) -> Boolean
    ) {
        toolbar.title = title
        activity.setSupportActionBar(toolbar)
        if (showBackButton) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
            toolbar.setNavigationOnClickListener { activity.finish() }
        } else {
            toolbar.navigationIcon = null
        }
        toolbar.setOnMenuItemClickListener(onMenuItemClick)
    }
}
