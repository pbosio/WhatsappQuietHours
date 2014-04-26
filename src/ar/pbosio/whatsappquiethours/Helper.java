package ar.pbosio.whatsappquiethours;

import android.text.format.Time;
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
		return ret;
	}
	
	public boolean shouldMuteNotification()
	{
		prefs.reload();
		
		if (isCustom())
		{
			boolean ret = false;
			if (isQuietHour())
			{
				ret = prefs.getBoolean("pref_mute_notifications", false);
			}
			return ret;
		}
		return false;
	}
	
	public boolean shouldDisableVibrations()
	{
		prefs.reload();
		
		if (isCustom())
		{
			boolean ret = false;
			if (isQuietHour())
			{
				ret = prefs.getBoolean("pref_disable_vibrations", false);
			}
			return ret;
		}
		return false;		
	}
	
	public boolean shouldDisableNotLED()
	{
		prefs.reload();
		
		if (isCustom())
		{
			boolean ret = false;
			if (isQuietHour())
			{
				ret = prefs.getBoolean("pref_disable_notification_light", false);
			}
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
			return -1;
		}
	}
	
	private boolean isQuietHour()
	{
		int quietHoursStart = getQuietHourStart();
		int quietHoursEnd = getQuietHourEnd();
		
		Time t = new Time();
		t.setToNow();
		long now = ((t.hour * 60) + t.minute);
		
		Logger.log("current time values-> quietHoursStart: "+quietHoursStart+" quietHoursEnd: "+quietHoursEnd+" now: "+now);
		if (quietHoursStart == -1 || quietHoursEnd == -1)
			return false;
		
		if (quietHoursEnd <= quietHoursStart)
		{
			if (now >= quietHoursStart && now <= 1440)
				return true;
			if (now >= 0 && now <= quietHoursEnd)
				return true;
		}
		else
		{
			if (now >= quietHoursStart && now <= quietHoursEnd)
				return true;
		}
		
		return false;
	}
}