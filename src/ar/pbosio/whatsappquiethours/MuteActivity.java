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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

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
        		Logger.log("MuteActivity: error mute intent extra == NULL");
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
					((CheckBox)view).setChecked(prefs.getBoolean(view.getTag().toString(), true));
					
					if (view.getId() == R.id.mute_check_whitelist && !Helper.CAN_USE_WHITELIST)
					{
						view.setVisibility(View.GONE);
					}
				}
			}
			
			SeekBar seekbar = (SeekBar) findViewById(R.id.seekBar1);
			final TextView hoursTextView = (TextView)findViewById(R.id.hours);
			final TextView hoursLabel = (TextView)findViewById(R.id.hour_text);
			
			seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
					hoursTextView.setText(String.valueOf(progress+1));
					hoursLabel.setText(R.string.mute_pref_hours);
					if (progress == 0){
						hoursLabel.setText(R.string.mute_pref_hour);
					}
				}
			});
			
			int mute_hours = prefs.getInt("mute_all_hours", 1);
			seekbar.setProgress(mute_hours-1);
			hoursTextView.setText(String.valueOf(mute_hours));
			hoursLabel.setText(R.string.mute_pref_hours);
			if (mute_hours == 1){
				hoursLabel.setText(R.string.mute_pref_hour);
			}			
			
		} catch (Exception e) {
			Logger.log("MuteActivity: error setting mute preferences",e);
		}  
    }
	
	@SuppressLint({ "WorldReadableFiles", "CommitPrefEdits" })
	@SuppressWarnings("deprecation")
	void getPreferences()
	{
		prefs = getSharedPreferences(Constants.SHARED_PREFS,MODE_MULTI_PROCESS | MODE_WORLD_READABLE);
		editor = prefs.edit();
	}
    
    public void onOkPressed(View v)
    {
    	try {
			SeekBar seekbar = (SeekBar) findViewById(R.id.seekBar1);
			int hours = seekbar.getProgress()+1;
			setEndTime(hours);
			editor.putInt("mute_all_hours", hours);
			
			LinearLayout layout = (LinearLayout) findViewById(R.id.mute_layout);
			
			for (int i = 0; i < layout.getChildCount(); i++) {
				View view = layout.getChildAt(i);
				if (view instanceof CheckBox) {
					boolean isChecked = ((CheckBox) view).isChecked();
					editor.putBoolean(view.getTag().toString(), isChecked);
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
	void setEndTime(int hours)
    {
    	Calendar c = Calendar.getInstance();
    	
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