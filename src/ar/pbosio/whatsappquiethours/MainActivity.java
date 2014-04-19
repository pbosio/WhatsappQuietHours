package ar.pbosio.whatsappquiethours;

import ar.pbosio.whatsappquiethours.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null)
            getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();

    }

    public static class PrefsFragment extends PreferenceFragment {

        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.preferences);
            final Activity activity = getActivity();
            
            final Preference pref_hide_launcher = this.findPreference("pref_hide_launcher");
            pref_hide_launcher.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    PackageManager p = activity.getPackageManager();
                    int state = (Boolean) newValue ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                    final ComponentName alias = new ComponentName(activity, "com.pbosio.whatsappquiethours.MainActivity-Alias");
                    p.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP);
                    return true;
                }
            });            
        }
    }
}