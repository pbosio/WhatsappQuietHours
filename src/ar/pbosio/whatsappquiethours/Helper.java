package ar.pbosio.whatsappquiethours;

import android.app.AndroidAppHelper;
import android.content.ContentResolver;
import android.provider.Settings;
import de.robv.android.xposed.XSharedPreferences;

class Helper {
	private static final String KEY_QUIET_HOURS_START = "quiet_hours_start";
	private static final String KEY_QUIET_HOURS_END = "quiet_hours_end";
	
	XSharedPreferences prefs = null;
	
	Helper()
	{
		prefs = new XSharedPreferences(Constant.PACKAGE_NAME);
	}
	
	public int getQuietHourStart()
	{
		return getTimeRange(KEY_QUIET_HOURS_START);
	}
	
	public int getQuietHourEnd()
	{
		return getTimeRange(KEY_QUIET_HOURS_END);
	}
	
	public boolean isCustom()
	{
		prefs.reload();
		boolean ret = prefs.getBoolean("pref_custom_quiet_hours", false);
		Logger.log("pref_custom_quiet_hours: " + ret);
		return ret;
	}
	
	public boolean shouldMuteNotification()
	{
		if (isCustom())
		{
			boolean ret = prefs.getBoolean("pref_mute_notifications", false);
			Logger.log("pref_mute_notifications: " + ret);			
			return ret;
		}
		return true;
	}
	
	public boolean shouldDisableVibrations()
	{
		if (isCustom())
		{
			boolean ret = prefs.getBoolean("pref_disable_vibrations", false);
			Logger.log("pref_disable_vibrations: " + ret);			
			return ret;
		}
		return false;		
	}
	
	public boolean shouldDisableNotLED()
	{
		if (isCustom())
		{
			boolean ret = prefs.getBoolean("pref_disable_notification_light", false);
			Logger.log("pref_disable_notification_light: " + ret);			
			return ret;
		}
		return false;		
	}
	
	private int getTimeRange(String key)
	{
		if (isCustom()){
			int retValue = -1;
			try{
				String[] range = prefs.getString("pref_timerange", "-1;-1").split(";");
				int index = 0;
				if (key == KEY_QUIET_HOURS_END)
				{
					index = 1;
				}
				retValue = Integer.parseInt(range[index]);
			}
			catch(Exception e)
			{
				Logger.log("com.pbosio.whatsappquiethoursfix timeRange error ",e);
			}
			return retValue;
		}
		else{
			ContentResolver resolver = AndroidAppHelper.currentApplication().getContentResolver();
			return Settings.System.getInt(resolver, key,-1);
		}
	}
}