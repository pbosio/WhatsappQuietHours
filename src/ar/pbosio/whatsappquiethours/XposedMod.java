package ar.pbosio.whatsappquiethours;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaPlayer;
import android.text.format.Time;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {
	
	private static final Helper mSettingsHelp = new Helper();
	private static final String QUIETHOURS_OPTION_TITLE = "Quiet hours";
	private static final int QUIETHOURS_OPTION_ID = -1;
	
	private boolean isQuietHour()
	{
		int quietHoursStart = mSettingsHelp.getQuietHourStart();
		int quietHoursEnd = mSettingsHelp.getQuietHourEnd();
		
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
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		
		if (lpparam.packageName.equals("com.whatsapp"))
		{
			hookAllMethods(MediaPlayer.class, "start", new XC_MethodHook()
			{
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {			
						if (isQuietHour())
						{
							Logger.log("I'M IN QUIET HOURS");
							if (mSettingsHelp.shouldMuteNotification())
							{
								MediaPlayer mp = (MediaPlayer)param.thisObject;
								mp.seekTo(mp.getDuration());							
								mp.setVolume(0, 0);
							}
						}
						else
						{
							Logger.log("I'M NOT IN QUIET HOURS");
						}
						
					} catch (Exception e) {
						Logger.log("MediaPlayer error",e);
					}
				}
			});		
			
			hookAllMethods(NotificationManager.class, "notify", new XC_MethodHook()
			{
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {
						if (mSettingsHelp.shouldDisableNotLED() || mSettingsHelp.shouldDisableVibrations())
						{
							if (isQuietHour())
							{
								for(Object arg : param.args) {
									if(arg instanceof Notification) {
										Notification notif = (Notification) arg;
										
										if (mSettingsHelp.shouldDisableNotLED())
										{
											Logger.log("TRYING TO STOP NOTIFICATION LIGHT");
											notif.ledOffMS = 0;
											notif.ledOnMS = 0;
											notif.flags &= ~Notification.FLAG_SHOW_LIGHTS;
										}
										
										if (mSettingsHelp.shouldDisableVibrations())
										{
											Logger.log("TRYING TO STOP VIBRATION");
											notif.vibrate = null;
										}
									}
								}
							}
						}
					}
					catch(Exception e)
					{
						Logger.log("NotificationManager error",e);
					}
				}
			});
			
			findAndHookMethod("com.whatsapp.Conversations", lpparam.classLoader, "onCreateOptionsMenu","com.actionbarsherlock.view.Menu",new XC_MethodHook(){
				@Override
				protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
					try {
						XposedHelpers.callMethod(param.args[0], "add",0,QUIETHOURS_OPTION_ID,0,QUIETHOURS_OPTION_TITLE);
					}
					catch(Exception e)
					{
						Logger.log("Conversations onCreateOptionsMenu error",e);
					}
				}
			});
			
			findAndHookMethod("com.whatsapp.Conversations", lpparam.classLoader, "onOptionsItemSelected","com.actionbarsherlock.view.MenuItem",new XC_MethodHook(){
				@Override
				protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
					try {
						int id = (Integer)XposedHelpers.callMethod(param.args[0], "getItemId");
						if(id == QUIETHOURS_OPTION_ID)
						{
							Activity act = (Activity) param.thisObject;
							Intent intent = new Intent(Intent.ACTION_RUN);
							intent.setComponent(new ComponentName(Constant.PACKAGE_NAME, Constant.PACKAGE_NAME+".MainActivity"));
							act.startActivity(intent);
							param.setResult(true);
						}
					}
					catch(Exception e)
					{
						Logger.log("Conversations onOptionsItemSelected error",e);
					}
				}
			});
		}
	}
	  
	@SuppressWarnings("unused")
	private void printArg(Object arg)
	{
		if (arg instanceof String)
			Logger.log("Arg is string: "+((String)arg));
		if (arg instanceof Integer)
			Logger.log("Arg is int: "+((Integer)arg));
		if (arg instanceof CharSequence)
			Logger.log("Arg is charsequence: "+((CharSequence)arg));		
	}
}