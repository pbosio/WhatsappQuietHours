package ar.pbosio.whatsappquiethours;

import java.util.Hashtable;

import android.content.SharedPreferences;
import de.robv.android.xposed.XSharedPreferences;

class WhiteList 
{
	private final String GROUP_DIVIDER = " @ ";
	private final String DISCARD_TITLE = "WhatsApp";
	
	public static final String WHITE_LIST_PREF_KEY = "white_list_contacts";
	public static final String WHITE_LIST_CONTACT_DIVIDER = "#";
	
	private Hashtable<String, String> m_WhiteListedContacts = null;
	private String m_strSharedValue = "";
	
	public String getSharedValue()
	{
		return m_strSharedValue;
	}
	
	private String getGroupName(String title)
	{
		if (title.equals(DISCARD_TITLE))
		{
			return null;
		}
		
		String [] split =  title.split(GROUP_DIVIDER);
		
		if (split.length <= 1)
			return null;
		
		return split[split.length-1];
	}
	
	private String getContactName(String title, String groupName)
	{
		if (title.equals(DISCARD_TITLE))
		{
			return null;
		}
		
		if (groupName != null)
		{
			return title.substring(0,title.indexOf(GROUP_DIVIDER+groupName)-1);
			
		}
		return title;
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
	
	boolean isContactWhiteListed(String title)
	{
		if (title == null)
			return false;
					
		String group = getGroupName(title);
		String contact = getContactName(title, group);
		
		if (contact == null)
			return false;
		
		if (group != null)
			return false; //no whitelist for contact msg in groups, maybe add preference for that later?
		
		Logger.log("Contact Group |"+group+"|");
		Logger.log("Contact Name |"+contact+"|");
		
		return isContactNameWhiteListed(contact);
	}
	
	boolean isContactNameWhiteListed(String contactName)
	{
		try {
			java.util.Set<String> keys = m_WhiteListedContacts.keySet();
			for (String key: keys)
			{
				if (m_WhiteListedContacts.get(key).equals(contactName))
				{
					Logger.log(contactName+" WhiteListed");
					return true;
				}
			}
		} catch (Exception e) {
			Logger.log("Whitelist: isContactNameWhiteListed error",e);
		}
		Logger.log(contactName+" Not WhiteListed");
		return false;
	}
};