package ar.pbosio.whatsappquiethours;

import android.net.Uri;
import android.text.format.Time;
import de.robv.android.xposed.XSharedPreferences;

class Helper {
	private static final String KEY_QUIET_HOURS_START = "quiet_hours_start";
	private static final String KEY_QUIET_HOURS_END = "quiet_hours_end";
	
	XSharedPreferences prefs = null;
	XSharedPreferences WApref = null;
	
	public boolean muteSound = false;
	
	Helper()
	{
		prefs = new XSharedPreferences(Constant.PACKAGE_NAME);
		WApref = new XSharedPreferences("com.whatsapp");
		Logger.log("Helper: new instance");
	}
	
	public Uri getWhatsAppNotUri()
	{
		WApref.reload();
		return Uri.parse(WApref.getString("notify_ringtone",""));	
	}
	
	public Uri getWhatsAppGroupUri()
	{
		WApref.reload();
		return Uri.parse(WApref.getString("group_notify_tone",""));	
	}
	
	public Uri getWhatsAppBroadcastUri()
	{
		WApref.reload();
		return Uri.parse(WApref.getString("broadcast_notify_tone",""));	
	}
	
	public boolean isNotificationSound(Uri sound)
	{
		return (sound.toString().equals(getWhatsAppNotUri().toString()) || 
				sound.toString().equals(getWhatsAppGroupUri().toString()) || 
				sound.toString().equals(getWhatsAppBroadcastUri().toString()));
	}
	
	public boolean isInOutSound(Object sound)
	{
		String path = ((Uri)sound).toString();
		//return (path.equals(getIncomingPath())||path.equals(getSendMessagePath()));
		return path.contains("android.resource://com.whatsapp/");
	}
	
	public String getIncomingPath()
	{
		return "android.resource://com.whatsapp/2131099649";
	}
	
	public String getSendMessagePath()
	{
		return "android.resource://com.whatsapp/2131099650";
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
	
	public boolean isForced()
	{
		prefs.reload();
		String data = prefs.getString("mute_pref_end", "-1");
		
		if (data.equals("-1"))
			return false;
		
		Time now = new Time();
		Time end = new Time();
		
		now.setToNow();
		boolean r = end.parse3339(data);
		
		if (!r){
			Logger.log("Error parsing mute end time");
			return false;
		}
		
		return end.after(now);
	}
	
	public boolean shouldMuteNotification()
	{
		prefs.reload();
		
		if (isForced())
		{
			return prefs.getBoolean("mute_pref_mute_notifications", false);
		}
		else if (isCustom())
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
		
		if (isForced())
		{
			return prefs.getBoolean("mute_pref_disable_vibrations", false);
		}		
		else if (isCustom())
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
		
		if (isForced())
		{
			return prefs.getBoolean("mute_pref_disable_notification_light", false);
		}		
		else if (isCustom())
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