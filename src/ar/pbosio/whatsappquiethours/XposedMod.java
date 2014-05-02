package ar.pbosio.whatsappquiethours;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getAdditionalStaticField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalStaticField;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {
	
	private static final String QUIETHOURS_OPTION_TITLE = "Quiet hours";
	private static final int QUIETHOURS_OPTION_ID = -1;
	
	NotiManager getNotiManager()
	{
		NotiManager notiManager = (NotiManager)getAdditionalStaticField(NotiManager.class, "NotiManager");
		if (notiManager == null)
		{
			Logger.log(">>> ERROR! notiManager is null");
		}
		return notiManager;
	}
	
	Helper getHelper()
	{
		Helper helper = (Helper)getAdditionalStaticField(Helper.class, "Helper");
		if (helper == null)
		{
			Logger.log(">>> ERROR! helper is null");
		}
		return helper;	
	}
	
	void instanceAuxClasses()
	{
		setAdditionalStaticField(NotiManager.class, "NotiManager", new NotiManager());
		setAdditionalStaticField(Helper.class, "Helper", new Helper());
	}
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		
		if (lpparam.packageName.equals("com.whatsapp"))
		{	
			instanceAuxClasses();
			
			hookAllMethods(MediaPlayer.class, "setDataSource", new XC_MethodHook()
			{
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {					
					try {
						if (param.args.length == 2) {
							if (param.args[1] instanceof Uri) {
								Logger.log("START MediaPlayer setDataSource "+ param.args.length);
								
								if (!getHelper().isCustom() && !getHelper().isForced() 
										&& !getHelper().isInSendSound(param.args[1])){		
									Logger.log("add sound "+((Uri)param.args[1]).toString());
									getNotiManager().addSound(param.args[1]);
									getHelper().muteSound = true;
								}
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
						Logger.log("START MediaPlayer start "+param.args.length);
						boolean mute = false;
						
						if (getHelper().isCustom() || getHelper().isForced())
						{
							if (getHelper().shouldMuteNotification())
								mute = true;
						}
						else
						{
							mute = getHelper().muteSound;
							getHelper().muteSound = false;
						}
						
						if (mute)
						{
							Logger.log("sound muted");
							MediaPlayer mp = (MediaPlayer)param.thisObject;
							mp.seekTo(mp.getDuration());							
							mp.setVolume(0, 0);
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
							Logger.log("START NotificationManager notify "+param.args.length);
							
							Notification not = (Notification)param.args[2];
							
							if (getHelper().isCustom() || getHelper().isForced())
							{
								if (getHelper().shouldDisableNotLED())
								{
									Logger.log("CUSTOM disable led");
									not.ledOffMS = 0;
									not.ledOnMS = 0;
									not.flags &= ~Notification.FLAG_SHOW_LIGHTS;	
								}
								
								if (getHelper().shouldDisableVibrations())
								{
									Logger.log("CUSTOM disable vibration");
									not.vibrate = null;
								}							
							}
							else
							{
								if (getNotiManager().isValidTag(param.args[0]))
								{
									param.args[0] = getNotiManager().fixNotificationTag(param.args[0]);
									Logger.log("notification push");
									return;
								}
								
								getNotiManager().notify(param.args);
								Logger.log("notification stopped");
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