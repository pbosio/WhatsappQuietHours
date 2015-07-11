package ar.pbosio.whatsappquiethours;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

class DBHelper
{
	private static final String CONTACTS_TABLE_NAME = "wa_contacts";
	private static final String NAMES_COLUMN = "display_name";
	private static final String IDS_COLUMN = "jid";
	private static final String IS_WA_USER_COLUMN = "is_whatsapp_user";
	
	@SuppressLint("SdCardPath")
	static final String WA_DB_PATH = "/data/data/com.whatsapp/databases/wa.db";
	
	private static DBHelper instance = null;
	
	private SQLiteDatabase m_WAContactsDB = null;
	
	private DBHelper()
	{
		try {
			m_WAContactsDB = SQLiteDatabase.openDatabase(WA_DB_PATH, null, SQLiteDatabase.OPEN_READONLY);
			
			Logger.log("DBHelper: new instance");
			if (m_WAContactsDB == null)
			{
				Logger.log("DBHelper: WA Contacts DB == NULL");
			}
		} catch (Exception e) {
			Logger.log("DBHelper: error opening WA Contacts DB",e);
		}
	}
	
	public String getContactNameFromId(String id)
	{
		if (m_WAContactsDB == null)
		{
			return null;
		}
		
		String []select = {NAMES_COLUMN};
		String where = IDS_COLUMN+" ==\""+id+"\"";
		
		Cursor cursor = m_WAContactsDB.query(CONTACTS_TABLE_NAME, select, where, null, null, null, null);
		cursor.moveToFirst();
		return cursor.getString(cursor.getColumnIndexOrThrow(NAMES_COLUMN));
		
	}
	
	public JSONObject getAllContactsJSON(boolean ignoreGroups)
	{
		try {
			if (m_WAContactsDB == null)
			{
				return null;
			}

			JSONObject ret = new JSONObject();
			
			String []select = {NAMES_COLUMN,IDS_COLUMN};
			String where = IS_WA_USER_COLUMN+" == 1 AND "+NAMES_COLUMN+" != \"\"";
			String order = NAMES_COLUMN + " ASC";
			
			Cursor cursor = m_WAContactsDB.query(CONTACTS_TABLE_NAME, select, where, null, null, null, order);
			cursor.moveToFirst();
			
			do{
				String id = cursor.getString(cursor.getColumnIndexOrThrow(IDS_COLUMN));
				String name = cursor.getString(cursor.getColumnIndexOrThrow(NAMES_COLUMN));
				
				if (!ignoreGroups || (ignoreGroups && !id.contains("@g.us"))){
					ret.put(id, name);
				}
				
			}while(cursor.moveToNext());

			return ret;
			
		} catch (IllegalArgumentException e) {
			Logger.log("DBHelper: error reading SQL DB",e);
		} catch (JSONException e) {
			Logger.log("DBHelper: error generating contacts JSON",e);
		}	
		return null;
	}
	
	
	static DBHelper Instance()
	{
		if (instance == null)
		{
			instance = new DBHelper();
		}
		return instance;
	}
};