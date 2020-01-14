package com.assignment.callrecording.activities;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.view.MenuItem;

import com.github.axet.androidlibrary.activities.AppCompatSettingsThemeActivity;
import com.github.axet.androidlibrary.preferences.NameFormatPreferenceCompat;
import com.github.axet.androidlibrary.preferences.OptimizationPreferenceCompat;
import com.github.axet.androidlibrary.preferences.SeekBarPreference;
import com.github.axet.androidlibrary.preferences.StoragePathPreferenceCompat;
import com.github.axet.androidlibrary.widgets.Toast;
import com.github.axet.audiolibrary.app.Sound;
import com.github.axet.audiolibrary.widgets.RecordingVolumePreference;
import com.assignment.callrecording.R;
import com.assignment.callrecording.app.CallApplication;
import com.assignment.callrecording.app.Storage;
import com.assignment.callrecording.services.RecordingService;
import com.assignment.callrecording.widgets.MixerPathsPreferenceCompat;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatSettingsThemeActivity implements PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {

    public static final int RESULT_FILE = 1;

    public static final String[] CONTACTS = new String[]{
            Manifest.permission.READ_CONTACTS,
    };

    GeneralPreferenceFragment f;
    Storage storage;

    @SuppressWarnings("unchecked")
    public static <T> T[] removeElement(Class<T> c, T[] aa, int i) {
        List<T> ll = Arrays.asList(aa);
        ll = new ArrayList<>(ll);
        ll.remove(i);
        return ll.toArray((T[]) Array.newInstance(c, ll.size()));
    }

    @Override
    public int getAppTheme() {
        return CallApplication.getTheme(this, R.style.RecThemeLight, R.style.RecThemeDark);
    }

    @Override
    public String getAppThemeKey() {
        return CallApplication.PREFERENCE_THEME;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(getAppTheme());
        super.onCreate(savedInstanceState);
        storage = new Storage(this);

        setupActionBar();

        f = new GeneralPreferenceFragment();
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
//            actionBar.setBackgroundDrawable(new ColorDrawable(CallApplication.getActionbarColor(this)));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    @TargetApi(11)
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName) || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);
        if (key.equals(CallApplication.PREFERENCE_STORAGE)) {
            storage.migrateLocalStorageDialog(this);
        }
        if (key.equals(CallApplication.PREFERENCE_SOURCE)) {
            String source = sharedPreferences.getString(CallApplication.PREFERENCE_SOURCE, "-1");
            if (source.equals(Integer.toString(MediaRecorder.AudioSource.UNPROCESSED))) {
                if (!Sound.isUnprocessedSupported(this))
                    Toast.Error(this, "Raw is not supported");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
        MainActivity.startActivity(this);
    }

    @Override
    public boolean onPreferenceDisplayDialog(PreferenceFragmentCompat caller, Preference pref) {
        if (pref instanceof NameFormatPreferenceCompat) {
            NameFormatPreferenceCompat.show(caller, pref.getKey());
            return true;
        }
        if (pref instanceof SeekBarPreference) {
            RecordingVolumePreference.show(caller, pref.getKey());
            return true;
        }
        return false;
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat {
        public GeneralPreferenceFragment() {
        }

        void initPrefs(PreferenceManager manager) {
            final Context context = manager.getContext();

            ListPreference format = (ListPreference) manager.findPreference(CallApplication.PREFERENCE_FORMAT);
            if (!Storage.permitted(context, CONTACTS)) {
                CharSequence[] ee = format.getEntries();
                CharSequence[] vv = format.getEntryValues();
            }
            bindPreferenceSummaryToValue(format);

            final ListPreference enc = (ListPreference) manager.findPreference(CallApplication.PREFERENCE_ENCODING);
            String v = enc.getValue();
            CharSequence[] ee = Storage.getEncodingTexts(context);
            CharSequence[] vv = Storage.getEncodingValues(context);
            if (ee.length > 1) {
                enc.setEntries(ee);
                enc.setEntryValues(vv);

                int i = enc.findIndexOfValue(v);
                if (i == -1) {
                    enc.setValueIndex(0);
                } else {
                    enc.setValueIndex(i);
                }

                bindPreferenceSummaryToValue(enc);
            } else {
                enc.setVisible(false);
            }

            OptimizationPreferenceCompat optimization = (OptimizationPreferenceCompat) manager.findPreference(CallApplication.PREFERENCE_OPTIMIZATION);
            optimization.enable(RecordingService.class);

            bindPreferenceSummaryToValue(manager.findPreference(CallApplication.PREFERENCE_RATE));
            bindPreferenceSummaryToValue(manager.findPreference(CallApplication.PREFERENCE_THEME));
            bindPreferenceSummaryToValue(manager.findPreference(CallApplication.PREFERENCE_CHANNELS));
            bindPreferenceSummaryToValue(manager.findPreference(CallApplication.PREFERENCE_DELETE));
            bindPreferenceSummaryToValue(manager.findPreference(CallApplication.PREFERENCE_SOURCE));

            final PreferenceCategory filters = (PreferenceCategory) manager.findPreference("filters");
            Preference vol = manager.findPreference(CallApplication.PREFERENCE_VOLUME);
            String encoder = enc.getValue();
            onResumeVol(filters, encoder);
            enc.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    onResumeVol(filters, (String) newValue);
                    return sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, newValue);
                }
            });
            bindPreferenceSummaryToValue(vol);

            StoragePathPreferenceCompat s = (StoragePathPreferenceCompat) manager.findPreference(CallApplication.PREFERENCE_STORAGE);
            s.setStorage(new Storage(getContext()));
            s.setPermissionsDialog(this, Storage.PERMISSIONS_RW, RESULT_FILE);
            if (Build.VERSION.SDK_INT >= 21)
                s.setStorageAccessFramework(this, RESULT_FILE);
        }

        void onResumeVol(PreferenceCategory vol, String encoder) {
            boolean b;
            if (Storage.isMediaRecorder(encoder))
                b = false;
            else
                b = true;
            for (int i = 0; i < vol.getPreferenceCount(); i++) {
                vol.getPreference(i).setVisible(b);
            }
            vol.setVisible(b);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setHasOptionsMenu(true);
            addPreferencesFromResource(R.xml.pref_general);
            initPrefs(getPreferenceManager());
        }

        @Override
        public void onResume() {
            super.onResume();
            OptimizationPreferenceCompat optimization = (OptimizationPreferenceCompat) findPreference(CallApplication.PREFERENCE_OPTIMIZATION);
            optimization.onResume();
            MixerPathsPreferenceCompat mix = (MixerPathsPreferenceCompat) findPreference(CallApplication.PREFERENCE_MIXERPATHS);
            mix.onResume();
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            StoragePathPreferenceCompat s = (StoragePathPreferenceCompat) findPreference(CallApplication.PREFERENCE_STORAGE);
            switch (requestCode) {
                case RESULT_FILE:
                    s.onRequestPermissionsResult(permissions, grantResults);
                    break;
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            StoragePathPreferenceCompat s = (StoragePathPreferenceCompat) findPreference(CallApplication.PREFERENCE_STORAGE);
            switch (requestCode) {
                case RESULT_FILE:
                    s.onActivityResult(resultCode, data);
                    break;
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

}
