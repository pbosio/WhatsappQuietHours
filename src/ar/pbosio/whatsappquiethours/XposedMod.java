package ar.pbosio.whatsappquiethours;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {
	
	private static final Helper mSettingsHelp = new Helper();
	
	private static final String QUIETHOURS_OPTION_TITLE = "Quiet hours";
	private static final int QUIETHOURS_OPTION_ID = -1;
	
	private static final String NOTIFICATION_TAG = "let_me_in";
	
	private static final int CYCLE_NOT = 1;
	private static final int CYCLE_URI = 2;
	
	private static Uri mNotificationUri = null;
	private static Notification mNotificationNotif = null;
	private static int mNotificationId = 0;
	private static int mNofiticationCycle = 0;
	
	private void addCycleUri(Uri uri)
	{
		mNofiticationCycle |= CYCLE_URI;
		mNotificationUri = uri;
	}
	
	private void addCycleNotification(Notification not, int Id)
	{
		mNofiticationCycle |= CYCLE_NOT;
		mNotificationNotif = not;
		mNotificationId = Id;
	}
	
	private boolean isCycleComplete()
	{
		boolean ret = (mNofiticationCycle == (CYCLE_URI | CYCLE_NOT));
		if (ret)
			mNofiticationCycle = 0;
		return ret;
	}
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		
		if (lpparam.packageName.equals("com.whatsapp"))
		{				
			hookAllMethods(MediaPlayer.class, "setDataSource", new XC_MethodHook()
			{
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {					
					try {
						if (param.args.length == 2) {
							if (param.args[1] instanceof Uri) {
								Logger.log("START MediaPlayer setDataSource "+ param.args.length);
								addCycleUri((Uri) param.args[1]);
							}
						}
					} catch (Exception e) {
						Logger.log("MediaPlayer setDataSource error",e);
					}
				}
			});
			
			hookAllMethods(MediaPlayer.class, "start", new XC_MethodHook()
			{
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {						
						Logger.log("START MediaPlayer start ");
						
						if (isCycleComplete()){
							
							if (mSettingsHelp.isCustom() || mSettingsHelp.isForced())
							{
								if (mSettingsHelp.shouldMuteNotification())
								{
									Logger.log("CUSTOM mute sound");
									MediaPlayer mp = (MediaPlayer)param.thisObject;
									mp.seekTo(mp.getDuration());							
									mp.setVolume(0, 0);									
								}						
							}
							else
							{
								Logger.log("PUSH notification");
								MediaPlayer mp = (MediaPlayer)param.thisObject;
								mp.seekTo(mp.getDuration());							
								mp.setVolume(0, 0);
								
								mNotificationNotif.sound = mNotificationUri;
								Context con = AndroidAppHelper.currentApplication().getApplicationContext();
								NotificationManager notman = (NotificationManager)con.getSystemService(Context.NOTIFICATION_SERVICE);
								notman.notify(NOTIFICATION_TAG, mNotificationId, mNotificationNotif);
							}
							
						}
						
					} catch (Exception e) {
						Logger.log("MediaPlayer start error",e);
					}
				}
			});		
			
			hookAllMethods(NotificationManager.class, "notify", new XC_MethodHook()
			{
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {
						if (param.args.length == 3)
						{
							if (param.args[0] != null &&((String)param.args[0]).equals(NOTIFICATION_TAG))
							{
								param.args[0] = null;
								return;
							}
							
							Logger.log("START NotificationManager notify "+param.args.length);
							
							int id = (Integer)param.args[1];
							Notification not = (Notification)param.args[2];
							addCycleNotification(not,id);
							
							if (mSettingsHelp.isCustom() || mSettingsHelp.isForced())
							{
								if (mSettingsHelp.shouldDisableNotLED())
								{
									Logger.log("CUSTOM disable led");
									not.ledOffMS = 0;
									not.ledOnMS = 0;
									not.flags &= ~Notification.FLAG_SHOW_LIGHTS;	
								}
								
								if (mSettingsHelp.shouldDisableVibrations())
								{
									Logger.log("CUSTOM disable vibration");
									not.vibrate = null;
								}							
							}
							else
							{
								param.setResult(null);
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
	private void printArgs(Object args[])
	{
		Logger.log("Args count: "+args.length);
		int i = 0;
		for(Object arg : args)
		{
			printArg(arg,i);
			i++;
		}
	}
	
	private void printArg(Object arg,int id)
	{
		if (arg != null)
		{
			Logger.log("Arg "+id+" is "+arg.getClass().getName()+": "+arg.toString());
		}
		else
		{
			Logger.log("Arg "+id+" is null");
		}
	}
}