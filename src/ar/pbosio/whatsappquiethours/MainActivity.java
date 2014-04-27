package ar.pbosio.whatsappquiethours;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;


public class MainActivity extends PreferenceActivity {
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
	}

	@SuppressWarnings("deprecation")
	private void setupSimplePreferencesScreen() {
		addPreferencesFromResource(R.xml.preferences);
		
		final Activity activity = this;
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

	@Override
	public boolean onIsMultiPane() {
		return false;
	}
}
