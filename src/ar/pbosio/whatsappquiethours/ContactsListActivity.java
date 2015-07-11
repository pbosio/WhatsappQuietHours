package ar.pbosio.whatsappquiethours;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactsListActivity extends ListActivity
{
	private SharedPreferences preferences;
	private ContactData [] Contacts = null;
	
	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.contacts_list_view);

		setFinishOnTouchOutside(false);
		
		preferences = getSharedPreferences(Constants.SHARED_PREFS,MODE_MULTI_PROCESS | MODE_WORLD_READABLE);
		
		Contacts = getContacts();
		
		if (Contacts != null)
		{
			ContactsListAdapter adapter = new ContactsListAdapter(this,Contacts);
			setListAdapter(adapter);
		}
	}
	
	@Override
	public void onBackPressed() 
	{
		if (Contacts != null)
		{
			String pref_value = "";
			for (ContactData data:Contacts)
			{
				if (data.whitelisted)
					pref_value += data.id+WhiteList.WHITE_LIST_CONTACT_DIVIDER;
			}
			
			SharedPreferences.Editor pref_editor = preferences.edit();
			pref_editor.putString(WhiteList.WHITE_LIST_PREF_KEY, pref_value);
			pref_editor.apply();
		}
		
		super.onBackPressed();
	}
		
	private String getContactsFileData()
	{
		BufferedReader br = null;
		StringBuilder text = new StringBuilder();

		try {
			File f = new File(Environment.getExternalStorageDirectory()+Helper.STORAGE_FOLDER,Helper.STORAGE_CONTACTS_FILENAME);
			
			br = new BufferedReader(new FileReader(f));
			String line;   
			while ((line = br.readLine()) != null) {
				text.append(line);
			}

			br.close();
			return text.toString();
			
		} catch (FileNotFoundException e) {
			Logger.log("ContactsListActivity: contacts file not found",e);
		} catch (IOException e) {
			Logger.log("ContactsListActivity: error reading contacts file",e);
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				Logger.log("ContactsListActivity: error closing contacts file",e);
			}
		}
		return null;
	}


	private ContactData[] getContacts()
	{
		try {
			String json_string = getContactsFileData();
			
			if (json_string == null)
			{
				return null;
			}
			
			JSONObject json = new JSONObject(json_string);
			
			ContactData[] data = new ContactData[json.length()];
			String[] whitelisted = WhiteList.getWhitelistedContactsFromPrefences(preferences);
			
			int i=0;
			for (int j=0;j<whitelisted.length;j++)
			{
				String id = whitelisted[j];
				if (json.has(id)){
					data[i] = new ContactData(id,json.getString(id));
					data[i].whitelisted = true;
					json.remove(id);
					i++;
				}
			}
			
			Iterator<String> keyItr = json.keys();
			
			while(keyItr.hasNext()) {
			    String id = keyItr.next();
			    if (json.has(id)){
				    data[i] = new ContactData(id,json.getString(id));
				    i++;
			    }
			}
	
			return data;
		
		} catch (JSONException e) {
			Logger.log("ContactsListActivity: error reading contacts from JSON",e);
		}
		
		return null;
	}
	


	class ContactData
	{
		String id = "";
		String name = "";
		boolean whitelisted = false;
		Object thumnail = null;

		public ContactData (String _id, String _name)
		{
			id = _id;
			name = _name;
		}

	};

	public class ContactsListAdapter extends ArrayAdapter<ContactData>{
		private final Activity context;
		private final ContactData[] contacts;

		class ViewHolder {
			public TextView text;
			public TextView id;
			public ImageView image;
			public CheckBox checkbox;
		}

		public ContactsListAdapter(Activity context, ContactData[] data) {
			super(context, R.layout.contacts_list_item, data);
			this.context = context;
			this.contacts = data;
			
			if (data == null)
			{
				Logger.log("ContactsListAdapter@ContactsListActivity: contacts data is null");
			}
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View itemView = convertView;

			if (itemView == null) {
				LayoutInflater inflater = context.getLayoutInflater();
				itemView = inflater.inflate(R.layout.contacts_list_item,null);

				ViewHolder viewHolder = new ViewHolder();
				viewHolder.text = (TextView) itemView.findViewById(R.id.contact_name);
				viewHolder.id = (TextView) itemView.findViewById(R.id.contact_id);
				viewHolder.image = (ImageView) itemView.findViewById(R.id.contact_thumbnail);
				viewHolder.checkbox = (CheckBox)itemView.findViewById(R.id.whitelist_checkbox);
				itemView.setTag(viewHolder);
				
				viewHolder.checkbox.setTag(position);
				viewHolder.checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						int pos = (Integer)buttonView.getTag();
						ContactData c = contacts[pos];
						c.whitelisted = isChecked;
					}
				});
			}

			ViewHolder holder = (ViewHolder) itemView.getTag();
			ContactData c = contacts[position];
			holder.checkbox.setTag(position);
			holder.text.setText(c.name);
			holder.id.setText(c.id.split("@")[0]);
			holder.checkbox.setChecked(c.whitelisted);

			return itemView;
		}
	}
};