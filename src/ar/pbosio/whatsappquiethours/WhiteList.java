package ar.pbosio.whatsappquiethours;

import java.util.Hashtable;

import android.content.SharedPreferences;
import de.robv.android.xposed.XSharedPreferences;

class WhiteList 
{
	public static final String WHITE_LIST_PREF_KEY = "white_list_contacts";
	public static final String WHITE_LIST_CONTACT_DIVIDER = "#";
	
	private Hashtable<String, String> m_WhiteListedContacts = null;
	private String m_strSharedValue = "";
	
	public String getSharedValue()
	{
		return m_strSharedValue;
	}
	
	public WhiteList(XSharedPreferences prefs) 
	{
		m_WhiteListedContacts = new Hashtable<String, String>();
		reloadWhiteList(prefs);
	}
	
	void reloadWhiteList(XSharedPreferences prefs)
	{
		String pref_data = prefs.getString(WHITE_LIST_PREF_KEY, "");
		
		if (!pref_data.equals(m_strSharedValue))
		{
			m_WhiteListedContacts.clear();
			m_strSharedValue = pref_data;
			
			String [] ids = pref_data.split(WHITE_LIST_CONTACT_DIVIDER);
			
			for (String id:ids)
			{
				String name = DBHelper.Instance().getContactNameFromId(id);
				m_WhiteListedContacts.put(id, name);
			}
			
			Logger.log("Whitelist: Reloaded ("+m_WhiteListedContacts.size()+" contacts)");
		}
	}
	
	static String [] getWhitelistedContactsFromPrefences(SharedPreferences prefs)
	{
		String pref_data = prefs.getString(WHITE_LIST_PREF_KEY, "");
		return pref_data.split(WHITE_LIST_CONTACT_DIVIDER);
	}
	
	boolean addContact(String id,String name)
	{
		if (m_WhiteListedContacts.containsKey(id))
			return false;
		
		m_WhiteListedContacts.put(id, name);
		m_strSharedValue += id+WHITE_LIST_CONTACT_DIVIDER;
		
		Logger.log("Whitelist: added contact "+id+" "+name);
		
		return true;
	}
	
	boolean removeContact(String id)
	{
		if (!m_WhiteListedContacts.containsKey(id))
			return false;
		
		String name = m_WhiteListedContacts.remove(id);
		m_strSharedValue = m_strSharedValue.replaceAll(id+WHITE_LIST_CONTACT_DIVIDER, "");
		
		Logger.log("Whitelist: removed contact "+id+" "+name);
		
		return true;
	}
	
	boolean isContactIDWhitelisted(String id)
	{
		return m_WhiteListedContacts.containsKey(id);
	}
};