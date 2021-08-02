package com.uiza.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.layoutContainer, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val verPref = findPreference<Preference>("version_key")
            if (verPref != null) {
                verPref.setDefaultValue(BuildConfig.VERSION_CODE.toString())
                verPref.summary = String.format(
                    Locale.getDefault(),
                    "%s - %d",
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                )
            }
        }
    }
}
