package ar.pbosio.whatsappquiethours;

import android.text.format.Time;
import android.util.Log;

class Logger
{
	static final String TAG = "ar.pbosio.whatsappquiethours";
	
	private static Logger instance = null;
	
	private Time time = null;
	//private List<String> logs;
	
	public static void log(String arg0) {
		Time t = getInstance().time;
		t.setToNow();
		String msg = t.format("[%H:%M:%S]: ") + arg0;
		//getInstance().logMsg(msg);
		Log.d(TAG,msg);
	}
	
	public static void log(String arg0, Exception e) {
		log(arg0+": "+e.toString()+"->"+e.getStackTrace());
	}

	/*private void logMsg(String msg)
	{
		logs.add(logs.size(),msg);
	}*/
	
	private Logger() {
		time = new Time();
		time.setToNow();
		//logs.clear();
	}
	
	private static Logger getInstance()
	{
		if (instance == null)
		{
			instance = new Logger();
		}
		return instance;
	}
}