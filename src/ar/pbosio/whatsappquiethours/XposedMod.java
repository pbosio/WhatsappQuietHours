package ar.pbosio.whatsappquiethours;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getAdditionalStaticField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalStaticField;

import java.util.Locale;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {
	
	private static final String QUIETHOURS_OPTION_TITLE = "Quiet hours";
	private static final int QUIETHOURS_OPTION_ID = -1;
	private static final String MUTE_OPTION_TITLE = "Mute all";
	private static final String MUTE_OPTION_TITLE_C = "Cancel mute";
	private static final int MUTE_OPTION_ID = -2;
	private static final int NEW_GROUP_OPTION_ID = 2131755032;
	
	Helper getHelper()
	{
		Helper helper = (Helper)getAdditionalStaticField(Helper.class, "Helper");
		if (helper == null)
		{
			Logger.log(">>> ERROR! helper is null");
		}
		return helper;	
	}
	
	NotiManager getNotiManager()
	{
		NotiManager notiManager = (NotiManager)getAdditionalStaticField(NotiManager.class, "NotiManager");
		if (notiManager == null)
		{
			Logger.log(">>> ERROR! notiManager is null");
		}
		return notiManager;
	}
	
	void instanceAuxClasses()
	{
		setAdditionalStaticField(Helper.class, "Helper", new Helper());
	}
	
	void instanceKKAuxClasses()
	{
		setAdditionalStaticField(NotiManager.class, "NotiManager", new NotiManager());
	}
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		
		if (lpparam.packageName.equals("com.whatsapp"))
		{	
			instanceAuxClasses();	
			
			/* HOOKS FOR KITKAT */
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP)
			{
				instanceKKAuxClasses();
				
				hookAllMethods(MediaPlayer.class, "setDataSource", new XC_MethodHook()
				{
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {					
						try {
							if (param.args.length == 2) {
								if (param.args[1] instanceof Uri) {
									Logger.log("START MediaPlayer setDataSource "+ param.args.length);
									
									getHelper().reloadPreferences();
									
									if(!getHelper().isInOutSound(param.args[1]) && !getHelper().isCallSound(param.args[1]))
									{
										if (!getHelper().shouldMuteNotification())
										{
											Logger.log("add sound "+((Uri)param.args[1]).toString());
											getNotiManager().addSound(param.args[1]);
										}
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
							
							if (getHelper().muteSound)
							{
								Logger.log("sound muted");
								getHelper().muteSound = false;
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
								
								if (getNotiManager().isValidTag(param.args[0]))
								{
									param.args[0] = getNotiManager().fixNotificationTag(param.args[0]);
									Logger.log("notification push");
									return;
								}							
								
								Notification not = (Notification)param.args[2];
								getHelper().reloadPreferences();
								
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
									not.defaults &= ~Notification.DEFAULT_VIBRATE;
									not.vibrate = null;
								}	
								
								NotificationManager manager = (NotificationManager)param.thisObject;

								getNotiManager().notify(param.args,manager);
								Logger.log("notification stopped");
								param.setResult(null);

							}
						}
						catch(Exception e)
						{
							Logger.log("NotificationManager error",e);
						}
					}
				});
			}
				
			/* HOOKS FOR LOLLIPOP */
			else
			{
				hookAllMethods(NotificationManager.class, "notify", new XC_MethodHook()
				{
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						try {
							if (param.args.length == 3)
							{												
								Notification not = (Notification)param.args[2];
								
								Logger.log("notification");
								
								getHelper().reloadPreferences();
								
								if (getHelper().shouldDisableNotLED())
								{
									Logger.log("disable led");
									not.ledOffMS = 0;
									not.ledOnMS = 0;
									not.flags &= ~Notification.FLAG_SHOW_LIGHTS;	
								}
								
								if (getHelper().shouldDisableVibrations())
								{
									Logger.log("disable vibration");
									not.defaults &= ~Notification.DEFAULT_VIBRATE;
									not.vibrate = null;
								}
								
								if (getHelper().shouldMuteNotification())
								{
									Logger.log("disable sound");
									not.sound = null;
								}							
							}
						}
						catch(Exception e)
						{
							Logger.log("NotificationManager error",e);
						}
					}
				});
			}
			
			/* HOOKS FOR ALL SDK */
			findAndHookMethod("android.support.v4.app.FragmentActivity", lpparam.classLoader, "onPrepareOptionsPanel",android.view.View.class,android.view.Menu.class,new XC_MethodHook(){
				@Override
				protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
					try {			
						if (param.args[1] != null){
							
							android.view.Menu menu = (android.view.Menu)param.args[1];
							
							android.view.MenuItem newgroupMenuItem = menu.findItem(NEW_GROUP_OPTION_ID);
							
							if (newgroupMenuItem != null && newgroupMenuItem.isVisible()){
							
								android.view.MenuItem quietMenuItem = menu.findItem(QUIETHOURS_OPTION_ID);
								android.view.MenuItem muteMenuItem = menu.findItem(MUTE_OPTION_ID);
								
								if (quietMenuItem == null)
								{
									quietMenuItem = menu.add(0,QUIETHOURS_OPTION_ID,0,translate(QUIETHOURS_OPTION_TITLE));
								}
								if (muteMenuItem == null)
								{
									muteMenuItem = menu.add(0,MUTE_OPTION_ID,0,get_mute_title());
								}
								else{
									muteMenuItem.setTitle(get_mute_title());
								}
							}
						}
					}
					catch(Exception e)
					{
						Logger.log("android.support.v4.app.FragmentActivity onPrepareOptionsPanel error",e);
					}
				}
			});
			
			findAndHookMethod("android.support.v4.app.FragmentActivity", lpparam.classLoader, "onMenuItemSelected",int.class,android.view.MenuItem.class,new XC_MethodHook(){
				@Override
				protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
					try {			
						android.view.MenuItem item = (android.view.MenuItem)param.args[1];
						on_item_pressed(item.getItemId(),param);
					}
					catch(Exception e)
					{
						Logger.log("android.support.v4.app.FragmentActivity onMenuItemSelected error",e);
					}
				}
			});
		}
	}
	
	void on_item_pressed(int itemId,de.robv.android.xposed.XC_MethodHook.MethodHookParam param)
	{
		if(itemId == QUIETHOURS_OPTION_ID)
		{
			Activity act = (Activity) param.thisObject;
			Intent intent = new Intent(Intent.ACTION_RUN);
			intent.setComponent(new ComponentName(Constant.PACKAGE_NAME, Constant.PACKAGE_NAME+".MainActivity"));
			act.startActivity(intent);
			param.setResult(true);
		}
		else if(itemId == MUTE_OPTION_ID)
		{
			Activity act = (Activity) param.thisObject;
			Intent intent = new Intent(Intent.ACTION_RUN);
			intent.setComponent(new ComponentName(Constant.PACKAGE_NAME, Constant.PACKAGE_NAME+".MuteActivity"));
			getHelper().reloadPreferences();
			intent.putExtra("cancel_mute", getHelper().isForced());
			act.startActivity(intent);
			param.setResult(true);
		}			
	}
	
	String get_mute_title()
	{
		getHelper().reloadPreferences();
		if (getHelper().isForced())
			return translate(MUTE_OPTION_TITLE_C);
		return translate(MUTE_OPTION_TITLE);
	}
	
	String translate(String s)
	{
		if (s.equals(MUTE_OPTION_TITLE))
		{
			if (Locale.getDefault().getLanguage().equalsIgnoreCase("es"))
				return "Silenciar todo";
			if (Locale.getDefault().getLanguage().equalsIgnoreCase("de"))
				return "Alle Stumm stellen";
		}
		else if (s.equals(MUTE_OPTION_TITLE_C))
		{
			if (Locale.getDefault().getLanguage().equalsIgnoreCase("es"))
				return "Cancelar silenciado";
			if (Locale.getDefault().getLanguage().equalsIgnoreCase("de"))
				return "Beende Stummphase";
		}
		else if (s.equals(QUIETHOURS_OPTION_TITLE))
		{
			if (Locale.getDefault().getLanguage().equalsIgnoreCase("de"))
				return "Ruhige Stunden";
		}
		return s;
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