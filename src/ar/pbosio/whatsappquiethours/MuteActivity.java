package ar.pbosio.whatsappquiethours;

import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class MuteActivity extends Activity {
	
	SharedPreferences prefs = null;
	SharedPreferences.Editor editor = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.mute_layout);
        getPreferences();
        
        Bundle b = getIntent().getExtras();
        if (b != null && b.getBoolean("cancel_mute"))
        {
        	editor.remove("mute_pref_end");
        	editor.apply();
        	finish();
        }
        else
        {
        	if (b == null)
        	{
        		Logger.log("Error mute intent extra == NULL");
        	}
        	setPreferences();
        }
    }
    
    void setPreferences()
    {   	
    	try {
			LinearLayout layout = (LinearLayout)findViewById(R.id.mute_layout);
			
			for (int i = 0; i< layout.getChildCount(); i++)
			{    
				View view = layout.getChildAt(i);
				if (view instanceof CheckBox)
				{
					if (prefs.getBoolean(view.getTag().toString(), false))
					{
						((CheckBox)view).setChecked(true);
					}
				}
			}
		} catch (Exception e) {
			Logger.log("Error setting mute preferences",e);
		}  
    }
	
	@SuppressLint({ "WorldReadableFiles", "CommitPrefEdits" })
	@SuppressWarnings("deprecation")
	void getPreferences()
	{
		prefs = getSharedPreferences(Constant.SHARED_PREFS,MODE_MULTI_PROCESS | MODE_WORLD_READABLE);
		editor = prefs.edit();
	}
    
    public void onOkPressed(View v)
    {
    	try {
			RadioGroup radGroup = (RadioGroup) findViewById(R.id.mute_radio_group);
			for (int i = 0; i < radGroup.getChildCount(); i++) {
				if (radGroup.getChildAt(i) instanceof RadioButton)
				{
					RadioButton b = (RadioButton) radGroup.getChildAt(i);
					if (b.isChecked()) {
						setEndTime(b.getTag().toString());
						break;
					}
				}
			}
			LinearLayout layout = (LinearLayout) findViewById(R.id.mute_layout);
			for (int i = 0; i < layout.getChildCount(); i++) {
				View view = layout.getChildAt(i);
				if (view instanceof CheckBox) {
					if (((CheckBox) view).isChecked()) {
						editor.putBoolean(view.getTag().toString(), true);
					}
					else{
						editor.remove(view.getTag().toString());
					}
				}
			}
			editor.apply();
		} catch (Exception e) {
			Logger.log("Error on mute popup",e);
		}
		finish();
    }
    
    public void onCancelPressed(View v)
    {
    	finish();
    }
    
    @SuppressLint("CommitPrefEdits")
	void setEndTime(String tag)
    {
    	Calendar c = Calendar.getInstance();
    	
    	int hours = 0;
    	
    	if(tag.equals("1H"))
    		hours = 1;
    	if(tag.equals("4H"))
    		hours = 4;
    	if(tag.equals("8H"))
    		hours = 8;
    	
    	c.add(Calendar.HOUR_OF_DAY, hours);
    	
    	int s = c.get(Calendar.SECOND);
    	int m = c.get(Calendar.MINUTE);
    	int h = c.get(Calendar.HOUR_OF_DAY);
    	int d = c.get(Calendar.DAY_OF_MONTH);
    	int mth = c.get(Calendar.MONTH);
    	int y = c.get(Calendar.YEAR);
    	
    	Time time = new Time();
    	time.set(s,m,h,d,mth,y);
    	time.normalize(true);
    	
    	editor.putString("mute_pref_end", time.format3339(false));
    }
}