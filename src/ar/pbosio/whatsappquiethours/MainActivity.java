package ar.pbosio.whatsappquiethours;

import java.io.File;

import ar.pbosio.whatsappquiethours.R;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null)
            getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
        
        try{
        	ActionBar b = getActionBar();
        	b.setHomeButtonEnabled(true);
        }catch(Exception e){
        	Logger.log("couldn't get ActionBar",e);
        }
    }
    
    @Override 
    public boolean onOptionsItemSelected (MenuItem item) { 
    	if(item.getItemId() == android.R.id.home) {
    		onBackPressed();
    	}
		return true;
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
            
            if (pref_hide_launcher != null)
            {
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
            
            if (!Helper.CAN_USE_WHITELIST)
            {
            	PreferenceCategory category = (PreferenceCategory)findPreference("pref_category_whitelist");
            	category.removeAll();
            	category.setTitle("");
            }
            else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP){
            	Preference whitelistMethod = this.findPreference("pref_whitelist_method");
            	if (whitelistMethod != null){
            		whitelistMethod.setEnabled(false);
            	}
            }
            
            
            final Preference pref_delete_whitelist = findPreference("pref_delete_whitelist");
            if (pref_delete_whitelist != null)
            {
            	pref_delete_whitelist.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            		@SuppressLint("WorldReadableFiles")
            		@Override
            		public boolean onPreferenceClick(Preference preference) {
            			AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
            			alertDialog.setMessage(getString(R.string.pref_delete_whitelist_data));

            			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.mute_pref_ok), new DialogInterface.OnClickListener() {
            				public void onClick(DialogInterface dialog, int which) {

            					SharedPreferences.Editor pref_editor = activity.getSharedPreferences(
            							Constants.SHARED_PREFS,MODE_MULTI_PROCESS | MODE_WORLD_READABLE).edit();
            					pref_editor.putString(WhiteList.WHITE_LIST_PREF_KEY, "");
            					pref_editor.apply();

            					File file = new File(Environment.getExternalStorageDirectory()+Helper.STORAGE_FOLDER,
            							Helper.STORAGE_CONTACTS_FILENAME);

            					if (file.delete())
            						Toast.makeText(activity.getApplicationContext(), getString(R.string.pref_delete_whitelist_data_toast),
            								Toast.LENGTH_SHORT).show();
            					else
            						Toast.makeText(activity.getApplicationContext(), getString(R.string.pref_delete_whitelist_data_toast_error),
            								Toast.LENGTH_SHORT).show();
            				}
            			});

            			alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.mute_pref_cancel), new DialogInterface.OnClickListener() {
            				public void onClick(DialogInterface dialog, int which) {
            					dialog.cancel();
            				}
            			});

            			alertDialog.show();
            			return true;
            		}
            	});
            }
        }
    }
}