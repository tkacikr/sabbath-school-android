/*
 * Copyright (c) 2021. Adventech <info@adventech.io>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.cryart.sabbathschool.settings

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.cryart.sabbathschool.core.misc.SSConstants
import com.cryart.sabbathschool.core.model.AppConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SSSettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var dailyReminder: DailyReminder

    @Inject
    lateinit var appConfig: AppConfig

    private val viewModel: SettingsViewModel by activityViewModels()

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.ss_settings)

        val aboutPref = findPreference<Preference>(getString(R.string.ss_settings_version_key))
        aboutPref?.summary = appConfig.version

        findPreference<Preference>(getString(R.string.ss_settings_delete_account_key))
            ?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.ss_delete_account_question)
                    .setMessage(R.string.ss_delete_account_warning)
                    .setPositiveButton(R.string.ss_login_anonymously_dialog_positive) { _: DialogInterface?, _: Int ->
                        viewModel.deleteAccount()
                    }
                    .setNegativeButton(R.string.ss_login_anonymously_dialog_negative, null)
                    .create()
                    .show()
                true
            }
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences?, key: String?) {
        when (key) {
            SSConstants.SS_SETTINGS_REMINDER_ENABLED_KEY, SSConstants.SS_SETTINGS_REMINDER_TIME_KEY -> {
                dailyReminder.reSchedule()
            }
        }
    }

    companion object {
        fun newInstance(): SSSettingsFragment = SSSettingsFragment()
    }
}
