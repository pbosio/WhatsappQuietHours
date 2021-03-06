package ar.pbosio.whatsappquiethours;

import java.util.ArrayList;
import android.app.Notification;
import android.app.NotificationManager;
import android.net.Uri;
import android.os.Handler;

public class NotiManager
{
	private static final String NOTIFICATION_TAG = "QH;";
	private static final int NOTIFICATION_DELAY = 2000;
	
	private Handler mHandler = null;
	private ArrayList<NotificationData> mNotifications = new ArrayList<NotificationData>();
	private Uri mSavedSound = null;
	
	public NotiManager() 
	{
		Logger.log("NotiManager: new instance");
	}
	
	private Handler getHandler()
	{
		if (mHandler == null)
		{
			mHandler = new Handler();
		}
		return mHandler;
	}

	public void notify(Object [] args,NotificationManager manager)
	{
		notify((String)args[0], (Integer)args[1], (Notification)args[2],manager);
	}
	
	public boolean isValidTag(Object tag)
	{
		if (tag == null)
			return false;
		return (((String)tag).contains(NOTIFICATION_TAG));
	}
	
	public Object fixNotificationTag(Object tag)
	{
		try {							
			if (isValidTag(tag)) {
				if (tag.equals(NOTIFICATION_TAG)) {
					return null;
				} else {
					String ret = (String)tag;
					ret = ret.replace(NOTIFICATION_TAG, "");
					return ret;
				}
			}
		} catch (Exception e) {
			Logger.log("NotiManager: fixing tag error",e);
		}
		return tag;
	}
	
	void notify(String tag, int id, Notification noti, NotificationManager manager)
	{
		try {
			NotificationData n = new NotificationData();
			n.mManager = manager;
			n.mNotification = noti;
			n.mTag = NOTIFICATION_TAG + (tag == null ? "" : tag);
			n.mId = id;
			mNotifications.add(n);
			getHandler().postDelayed(n, NOTIFICATION_DELAY);
			
			if (mSavedSound != null)
			{
				n.mNotification.sound = mSavedSound;
				mSavedSound = null;
			}
			
		} catch (Exception e) {
			Logger.log("NotiManager: notify error",e);
		}
	}
	
	public void addSound(Object sound)
	{
		try {
			for (int i = 0; i < mNotifications.size(); i++) {
				NotificationData data = mNotifications.get(i);
				if (data.mNotification.sound == null) {
					data.mNotification.sound = (Uri)sound;
					return;
				}
			}
			if (mNotifications.size()== 0)
			{
				mSavedSound = ((Uri)sound);
			}
		} catch (Exception e) {
			Logger.log("NotiManager: addSound error",e);
		}
	}
	
	class NotificationData implements Runnable
	{
		NotificationManager mManager;
		Notification mNotification;
		String mTag;
		int mId;
		
		@Override
		public void run() {
			try {
				if (mNotifications != null) {
					try {
						mNotifications.remove(this);
					} catch(Exception e) {
						Logger.log("NotiManager: Runnable mNotifications error",e);		
					}
				}
				else {
					Logger.log("NotiManager: Runnable mNotifications == NULL");
				}
				getNotificationManager().notify(mTag, mId, mNotification);
			} catch (Exception e) {
				Logger.log("NotiManager: Runnable run error",e);
			}
		}
		
		private NotificationManager getNotificationManager()
		{
			if (mManager == null)
			{
				Logger.log("NotiManager: Runnable mManager == NULL");
			}
			return mManager;
		}
	}
}