package ar.pbosio.whatsappquiethours;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getAdditionalStaticField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalStaticField;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AndroidAppHelper;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.XModuleResources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources  {
	
	private static final int QUIETHOURS_OPTION_ID = -1;
	private static final int MUTE_OPTION_ID = -2;
	
	private static String MODULE_PATH = null;
	private static XModuleResources ModRes = null;
	
	private static int newGroupOptionID = -1;
	private static int contactsRefreshOptionID = -1;
	
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
	public void initZygote(StartupParam startupParam) throws Throwable {
		MODULE_PATH = startupParam.modulePath;
	}
	
	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (resparam.packageName.equals(Constants.WA_PACKAGE_NAME))
		{
			ModRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
		}
	}
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		
		if (lpparam.packageName.equals(Constants.WA_PACKAGE_NAME))
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
									
									if(!getHelper().isInOutSound(param.args[1]) 
									&& !getHelper().isCallSound(param.args[1])
									&& !getHelper().wasSenderWhitelisted())
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
								
								boolean fixStream = getHelper().processNotification(not,null,null);
								
								if (fixStream){	
									NotificationManager manager = (NotificationManager)param.thisObject;
	
									getNotiManager().notify(param.args,manager);
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
			}
				
			/* HOOKS FOR LOLLIPOP */
			else
			{
				hookAllMethods(NotificationManager.class, "notify", new XC_MethodHook()
				{
					@SuppressLint({ "NewApi", "InlinedApi" })
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						try {
							if (param.args.length == 3)
							{							
								if (param.args[0] != null && ((String)param.args[0]).equals(Helper.EXTRA_FORCE_SHOW_LIGHTS))
								{
									return;
								}
									
								Notification not = (Notification)param.args[2];
								
								boolean bypassZenMode = !getHelper().processNotification(not,
										AndroidAppHelper.currentApplication().getApplicationContext(),
										param.thisObject);
								
								if (bypassZenMode)
									not.category = Notification.CATEGORY_ALARM;
												
							}
						}
						catch(Exception e)
						{
							Logger.log("NotificationManager error",e);
						}
					}
				});
				
				hookAllMethods(NotificationManager.class, "cancel", new XC_MethodHook() {
					
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (param.args.length == 2 && param.args[0] != null 
								&& Helper.EXTRA_FORCE_SHOW_LIGHTS.equals((String)param.args[0]))
						{
							return;
						}
						
						getHelper().cancelLedNotification(param.thisObject);
					}
				});
			}
			
			/* HOOKS FOR ALL SDK */		
			findAndHookMethod("com.whatsapp.HomeActivity", lpparam.classLoader, "onPrepareOptionsMenu",
					android.view.Menu.class,new XC_MethodHook(){
				@Override
				protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
					try {			
						if (param.args[0] != null){
							
							android.view.Menu menu = (android.view.Menu)param.args[0];
							
							if (newGroupOptionID ==-1 || contactsRefreshOptionID == -1){
								
								android.content.Context context = AndroidAppHelper.currentApplication().getApplicationContext();
								android.content.res.Resources res  = context.getResources();
								
								String newgroup = res.getString(res.getIdentifier("menuitem_groupchat", "string", Constants.WA_PACKAGE_NAME));
								String refresh = res.getString(res.getIdentifier("menuitem_refresh", "string", Constants.WA_PACKAGE_NAME));
								
								for (int i=0;i<menu.size();i++){
									if (menu.getItem(i).getTitle() == newgroup){
										newGroupOptionID = menu.getItem(i).getItemId();
									}
									else if (menu.getItem(i).getTitle() == refresh){
										contactsRefreshOptionID = menu.getItem(i).getItemId();
									}
								}
							}
							
							android.view.MenuItem newgroupMenuItem = menu.findItem(newGroupOptionID);
							
							if (newgroupMenuItem != null && newgroupMenuItem.isVisible()){
							
								android.view.MenuItem quietMenuItem = menu.findItem(QUIETHOURS_OPTION_ID);
								android.view.MenuItem muteMenuItem = menu.findItem(MUTE_OPTION_ID);
								
								if (quietMenuItem == null)
								{
									quietMenuItem = menu.add(0,QUIETHOURS_OPTION_ID,0,ModRes.getString(R.string.quitehourse_option_title));
								}
								if (muteMenuItem == null)
								{
									muteMenuItem = menu.add(0,MUTE_OPTION_ID,0,get_mute_title());
								}
								else{
									muteMenuItem.setTitle(get_mute_title());
								}
							}
							
							Logger.log("options menu prepared");
						}
					}
					catch(Exception e)
					{
						Logger.log("com.whatsapp.HomeActivity onPrepareOptionsMenu error",e);
					}
				}
			});
			
			findAndHookMethod("com.whatsapp.HomeActivity", lpparam.classLoader, "onOptionsItemSelected",
					android.view.MenuItem.class,new XC_MethodHook(){
				@Override
				protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
					try {			
						android.view.MenuItem item = (android.view.MenuItem)param.args[0];
						on_item_pressed(item.getItemId(),param);
					}
					catch(Exception e)
					{
						Logger.log("com.whatsapp.HomeActivity onMenuItemSelected error",e);
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
			intent.setComponent(new ComponentName(Constants.PACKAGE_NAME, Constants.MAIN_ACTIVITY));
			act.startActivity(intent);
			param.setResult(true);
		}
		else if(itemId == MUTE_OPTION_ID)
		{
			Activity act = (Activity) param.thisObject;
			Intent intent = new Intent(Intent.ACTION_RUN);
			intent.setComponent(new ComponentName(Constants.PACKAGE_NAME, Constants.MUTE_ACTIVITY));
			getHelper().reloadPreferences();
			intent.putExtra("cancel_mute", getHelper().isForced());
			act.startActivity(intent);
			param.setResult(true);
		}			
		else if (itemId == contactsRefreshOptionID)
		{
			Logger.log("refreshing contacts");
			getHelper().saveContactsJSON();
		}
		
		Logger.log("menu item pressed "+itemId);
	}
	
	String get_mute_title()
	{
		getHelper().reloadPreferences();
		if (getHelper().isForced())
			return ModRes.getString(R.string.mute_option_title_c);
		return ModRes.getString(R.string.mute_option_title);
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
	
	@SuppressWarnings("unused")
	private void print_children(Object o,String str)
	{
		Logger.log(str+o.getClass().getName()+" -> "+o.toString());
		if (o instanceof android.view.ViewGroup){
			android.view.ViewGroup ss = (android.view.ViewGroup)o;
			for (int i=0; i<ss.getChildCount();i++)
			{
				print_children(ss.getChildAt(i),str+"/"+i);
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void testNotification(final android.content.Context context)
	{
		android.os.Handler handler = new android.os.Handler();
		handler.postDelayed(new Runnable() {
			
			@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
			@Override
			public void run() {
				try {
					Logger.log("test notification....");
					final Notification.Builder builder = new Notification.Builder(context);
					builder.setContentTitle("TestContact");
					builder.setContentText("random message");
					builder.setDefaults(Notification.DEFAULT_ALL);
					builder.setAutoCancel(false);

					NotificationManager nm = (NotificationManager) context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
					
					Notification n = builder.build();
					n.flags = Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_ONLY_ALERT_ONCE;
					nm.notify(null,3,n);
				} catch (Exception e) {
					Logger.log("Error en la notificacion de testeo",e);
				}
				
			}
		}, 5000);
	}
}