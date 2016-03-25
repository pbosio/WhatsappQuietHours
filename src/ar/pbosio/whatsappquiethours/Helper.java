package ar.pbosio.whatsappquiethours;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.text.format.Time;
import de.robv.android.xposed.XSharedPreferences;

public class Helper {
	
	private static final String KEY_QUIET_HOURS_START = "quiet_hours_start";
	private static final String KEY_QUIET_HOURS_END = "quiet_hours_end";
	
	public static final String STORAGE_FOLDER = "/.whatsappquiethours";
	public static final String STORAGE_CONTACTS_FILENAME = "contacts.json";
	
	public static final String EXTRA_FORCE_SHOW_LIGHTS = "android.forceShowLights";
	
	//public static final boolean CAN_USE_WHITELIST = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
	public static final boolean CAN_USE_WHITELIST = true;
	
	private XSharedPreferences prefs = null;
	private XSharedPreferences WApref = null;
	private WhiteList m_WhiteList = null;
	private DBHelper m_dbHelper = null;
	
	public boolean muteSound = false;
	private boolean wasWhiteListed = false;
	
	@SuppressLint("SdCardPath")
	public Helper()
	{
		prefs = new XSharedPreferences(Constants.PACKAGE_NAME);
		WApref = new XSharedPreferences("com.whatsapp");
		
		if (CAN_USE_WHITELIST)
		{
			m_WhiteList = new WhiteList(prefs);
			m_dbHelper = DBHelper.Instance();
			
			saveContactsJSON();
		}
		
		Logger.log("Helper: new instance");
	}
	
	public void reloadPreferences(){
		prefs.reload();
	}
	
	public void reloadPreferences(Boolean reloadWhiteList)
	{
		reloadPreferences();
		
		if(CAN_USE_WHITELIST && reloadWhiteList)
			m_WhiteList.reloadWhiteList(prefs);
	}
	
	public void forceSound(int defaults,Uri uri, Object context)
	{
		if (context == null)
		{
			Logger.log("Helper: forceSound context == null");
			return;
		}
		
		MediaPlayer player = new MediaPlayer();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			if ((defaults & Notification.DEFAULT_SOUND) != 0)
			{
				player.setDataSource((Context)context, 
						android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION));
			}
			else if(uri != null)
			{
				player.setDataSource((Context)context, uri);
			}
		} catch (IllegalArgumentException e) {
			Logger.log("Helper: forceSoundPlay setDataSource illegal argument",e);
		} catch (SecurityException e) {
			Logger.log("Helper: forceSoundPlay setDataSource security exception",e);
		} catch (IllegalStateException e) {
			Logger.log("Helper: forceSoundPlay setDataSource illegal state",e);
		} catch (IOException e) {
			Logger.log("Helper: forceSoundPlay setDataSource exception",e);
		}
		try {
			player.prepare();
		} catch (IllegalStateException e) {
			Logger.log("Helper: forceSoundPlay prepare illegal state",e);
		} catch (IOException e) {
			Logger.log("Helper: forceSoundPlay prepare exception",e);
		}
		player.start();
	}
	
	public void forceVibration(int defaults, long[]pattern,Object context)
	{
		if (context == null)
		{
			Logger.log("Helper: forceVibration context == null");
			return;
		}
		
		Vibrator v = (Vibrator) ((Context)context).getSystemService(Context.VIBRATOR_SERVICE);
		
		if ((defaults & Notification.DEFAULT_VIBRATE) != 0 || pattern == null)
		{
			v.vibrate(new long[] {0, 350, 250, 350},-1);
		}
		else
		{
			v.vibrate(pattern, -1);
		}
	}
	
	/*this will probably only work on CM based ROMS*/
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public void forceLed(int defaults,int flags, int ledOffMS, int ledOnMS, int ledARGB, Object context, Object notificationman)
	{
		if (context == null)
		{
			Logger.log("Helper: forceLed context == null");
			return;
		}
		
		if ((flags & Notification.FLAG_SHOW_LIGHTS) == 0)
		{
			return;
		}
		
		try {
			
			android.app.NotificationManager notman = (android.app.NotificationManager)notificationman;
			
			final android.os.Bundle b = new android.os.Bundle();
			b.putBoolean(EXTRA_FORCE_SHOW_LIGHTS, true);

			final Notification.Builder builder = new Notification.Builder((Context)context);

			if ((defaults & Notification.DEFAULT_LIGHTS) != 0)
			{
				builder.setDefaults(Notification.DEFAULT_LIGHTS);
			}
			else
			{
				builder.setLights(ledARGB, ledOnMS, ledOffMS);
			}
			
			builder.setExtras(b);
			notman.notify(EXTRA_FORCE_SHOW_LIGHTS,1, builder.build());
		} catch (Exception e) {
			Logger.log("Helper: forceLed error",e);
		}
	}
	
	public void cancelLedNotification(Object notificationman)
	{
		if (!prefs.getBoolean("pref_enable_whitelist", false))
		{
			return;
		}
		if (prefs.getString("pref_whitelist_method", "zen_mode").equals("zen_mode"))
		{
			return;
		}
		
		try {
			android.app.NotificationManager nm = (android.app.NotificationManager)notificationman;
			nm.cancel(EXTRA_FORCE_SHOW_LIGHTS, 1);
		} catch (Exception e) {
			Logger.log("Helper: cancelLedNotification error",e);
		}
	}
	
	public boolean isInOutSound(Object sound)
	{
		String path = ((Uri)sound).toString();
		return path.contains("android.resource://com.whatsapp/");
	}
	
	public boolean isCallSound(Object sound)
	{
		WApref.reload();
		Uri s = Uri.parse(WApref.getString("call_ringtone",""));
		Uri s2 = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);
		String strSoundUri = ((Uri)sound).toString();
		return  (s.toString().equals(strSoundUri)) || (s2.toString().equals(strSoundUri));
	}
	
	public int getQuietHourStart()
	{
		return getTimeRange(KEY_QUIET_HOURS_START);
	}
	
	public int getQuietHourEnd()
	{
		return getTimeRange(KEY_QUIET_HOURS_END);
	}
	
	public boolean isSenderWhitelisted()
	{
		if (!CAN_USE_WHITELIST)
		{
			return false;
		}
		
		try {
			boolean active = prefs.getBoolean("pref_enable_whitelist", false);
			String senderID = m_dbHelper.getLastMessageSenderID();
			return active && m_WhiteList.isContactIDWhitelisted(senderID);
		} catch (Exception e) {
			Logger.log("Helper: isWhiteListed error:",e);
		}
		return false;		
	}
	
	public void saveContactsJSON()
	{
		if (!CAN_USE_WHITELIST)
		{
			return;
		}
		
		if (!prefs.getBoolean("pref_enable_whitelist",false))
		{
			return;
		}
		
		FileOutputStream file = null;
	    try {
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state)) {
				
				JSONObject json = m_dbHelper.getAllContactsJSON(true);
				
				File f = new File(Environment.getExternalStorageDirectory()+STORAGE_FOLDER,STORAGE_CONTACTS_FILENAME);
				File parent = f.getParentFile(); 
				if (parent.mkdirs())
				{
					Logger.log("Helper: path created "+parent.getAbsolutePath());
				}
				
				file = new FileOutputStream(f, false);

				file.write(json.toString().getBytes());
				file.flush();

				Logger.log("Helper: saving contacts files...");
			}
		} 
	    catch (FileNotFoundException e) {
			Logger.log("Helper: file (contacts) not found",e);
		} 
	    catch (IOException e) {
			Logger.log("Helper: error saving contacts file",e);
		}
	    finally{
			if (file != null)
				try {
					file.close();
				} catch (IOException e) {
					Logger.log("Helper: error trying to close contacts file",e);
				}
		}

	}
	
	public boolean shouldRespectWhitelist()
	{
		return prefs.getBoolean("mute_pref_respect_whitelist", true);
	}
	
	public boolean isCustom()
	{
		boolean ret = prefs.getBoolean("pref_custom_quiet_hours", false);
		return ret;
	}
	
	public boolean isForced()
	{
		String data = prefs.getString("mute_pref_end", "-1");
		
		if (data.equals("-1"))
			return false;
		
		Time now = new Time();
		Time end = new Time();
		
		now.setToNow();
		boolean r = end.parse3339(data);
		
		if (!r){
			Logger.log("Helper: error parsing mute end time");
			return false;
		}
		
		return end.after(now);
	}
	
	public boolean shouldMuteNotification()
	{
		return shouldMuteNotification(isForced());
	}
	
	public boolean shouldMuteNotification(boolean forced)
	{
		if (forced)
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
		return shouldDisableVibrations(isForced());
	}
	
	public boolean shouldDisableVibrations(boolean forced)
	{	
		if (forced)
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
		return shouldDisableNotLED(isForced());
	}
	
	public boolean shouldDisableNotLED(boolean forced)
	{		
		if (forced)
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
				Logger.log("Helper: timeRange error ",e);
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
	
	
	public boolean processNotification(Notification not, Object context, Object notManager){
		Logger.log("processNotification");
		wasWhiteListed = false;
		
		boolean isForced = false;
		boolean isSenderWhitelisted = false;
		boolean forcedRespectWhitelist = true;
		boolean useZenMode = false;
		
		boolean force = false;

		if (CAN_USE_WHITELIST){
			reloadPreferences(true);
			isSenderWhitelisted = isSenderWhitelisted();
			forcedRespectWhitelist = shouldRespectWhitelist();
		}
		else{
			reloadPreferences();
		}
		
		isForced = isForced();
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
		{
			useZenMode = prefs.getString("pref_whitelist_method", "zen_mode").equals("zen_mode");
			if (useZenMode && isSenderWhitelisted && (!isForced || (isForced && forcedRespectWhitelist))) return false;
			
			force = !useZenMode && isSenderWhitelisted && (!isForced || (isForced && forcedRespectWhitelist));
		}
		else if (CAN_USE_WHITELIST){
			if(isSenderWhitelisted && (!isForced || (isForced && forcedRespectWhitelist))){
				wasWhiteListed = true;
				return false;
			}
		}

		
		if ((isSenderWhitelisted && !useZenMode) || shouldDisableNotLED(isForced))
		{
			if (force){
				Logger.log("contact whitelisted, forcing notification led");
				forceLed(not.defaults, not.flags, not.ledOffMS, not.ledOnMS, not.ledARGB,context, notManager);
			}
			
			Logger.log("disable led");
			not.ledOffMS = 0;
			not.ledOnMS = 0;
			not.flags &= ~Notification.FLAG_SHOW_LIGHTS;
		}
		
		if ((isSenderWhitelisted && !useZenMode) || shouldDisableVibrations(isForced))
		{
			if (force){
				Logger.log("contact whitelisted, forcing notification vibration");
				forceVibration(not.defaults, not.vibrate,context);
			}
			
			Logger.log("disable vibration");
			not.defaults &= ~Notification.DEFAULT_VIBRATE;
			not.vibrate = null;
		}
		
		if ((isSenderWhitelisted && !useZenMode) || shouldMuteNotification(isForced))
		{
			if (force){
				Logger.log("contact whitelisted, forcing notification sound");
				forceSound(not.defaults,not.sound,context);
			}
			
			Logger.log("disable sound");
			not.defaults &= ~Notification.DEFAULT_SOUND;
			not.sound = null;
		}
		return true;
	}
	
	public boolean wasSenderWhitelisted(){
		return wasWhiteListed;
	}
}