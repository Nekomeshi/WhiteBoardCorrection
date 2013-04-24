package com.nekomeshi312.whiteboardcorrection;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.type.Notebook;

public class EvernoteSettingActivity extends SherlockPreferenceActivity  {
	private static final String LOG_TAG = "EvernoteSettingActivity";
	private EvernoteCtrl mEvernoteCtrl ;
	//login switch
	private org.jraf.android.backport.switchwidget.SwitchPreference mSwitchPrefLogin = null;
	//notebook
	private List<Notebook> mNoteBooks = null;
	private static final int NOTEBOOK_RESULT_NOT_YET = -1;
	private static final int NOTEBOOK_RESULT_OK = 0;
	private static final int NOTEBOOK_RESULT_RECEIVING = 1;
	private static final int NOTEBOOK_RESULT_ERROR = 2;
	private int mNotebookListResult = NOTEBOOK_RESULT_NOT_YET;
	private Notebook mDefaultNotebook = null;
	private int mDefaultNotebookResult = NOTEBOOK_RESULT_NOT_YET;
	private ListPreference mListPrefNotebook = null;
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
    	setTheme(R.style.Theme_Sherlock);
		super.onCreate(arg0);
		addPreferencesFromResource(R.xml.evernote_setting);
		mEvernoteCtrl = new EvernoteCtrl(this);
		mNotebookListResult = NOTEBOOK_RESULT_NOT_YET;
		mDefaultNotebookResult = NOTEBOOK_RESULT_NOT_YET;
		setupPreferences();
		updateAuthUi();
		
		if(mEvernoteCtrl.isLoggedIn()){//loginしていたらnotebookのリストを取得する
			startReceivingDefaultNotebook();
			startReceiveNotebookList();
		}
	}
	
	private void setPrefNotebookSummary(){

		mListPrefNotebook.setEnabled(false);
		if(!mEvernoteCtrl.isLoggedIn()){
			mListPrefNotebook.setSummary(R.string.evernote_not_loggedin);
			return;
		}

		switch(mNotebookListResult){
			case NOTEBOOK_RESULT_NOT_YET:
				mListPrefNotebook.setSummary(R.string.evernote_not_loggedin);
				break;
			case NOTEBOOK_RESULT_RECEIVING:
				mListPrefNotebook.setSummary(R.string.evernote_receiving_notebook_list);
				break;
			case NOTEBOOK_RESULT_ERROR:
				mListPrefNotebook.setSummary(R.string.evernote_error_receiving_notebook_list);
				break;
			case NOTEBOOK_RESULT_OK:
				switch(mDefaultNotebookResult){
					case NOTEBOOK_RESULT_NOT_YET:
						mListPrefNotebook.setSummary(R.string.evernote_not_loggedin);
						break;
					case NOTEBOOK_RESULT_RECEIVING:
						mListPrefNotebook.setSummary(R.string.evernote_receiving_default_notebook);
						break;
					case NOTEBOOK_RESULT_ERROR:
						mListPrefNotebook.setSummary(R.string.evernote_error_receiving_default_notebook);
						break;
					case NOTEBOOK_RESULT_OK:
						mListPrefNotebook.setSummary(R.string.evernote_chose_notebook);
						String defaultGUID = mDefaultNotebook.getGuid();
						SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this); 
						for(int i = 0;i < mNoteBooks.size();i++){
							if(mNoteBooks.get(i).getGuid().equals(defaultGUID)){
								mListPrefNotebook.setDefaultValue(mListPrefNotebook.getEntryValues()[i]);//なぜかうまく動かない
								break;
							}
						}
						mListPrefNotebook.setEnabled(true);
						break;
				}
				break;
		}
	}

	private void setupPreferences(){
		mSwitchPrefLogin = (org.jraf.android.backport.switchwidget.SwitchPreference)findPreference(getString(R.string.pref_evernote_login_key));
		mSwitchPrefLogin.setOnPreferenceChangeListener(new org.jraf.android.backport.switchwidget.SwitchPreference.OnPreferenceChangeListener(){
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				// TODO Auto-generated method stub
				Boolean isChecked = (Boolean)newValue;
				if(isChecked){
					mEvernoteCtrl.login();
				}
				else{
					mEvernoteCtrl.logout();
				}
				return true;
			}
			
		});
		mListPrefNotebook = (ListPreference)findPreference(getString(R.string.pref_evernote_notebook_list_key));
		setPrefNotebookSummary();
	}
	
	private void startReceivingDefaultNotebook(){
		mDefaultNotebookResult = NOTEBOOK_RESULT_NOT_YET;
		if(!mEvernoteCtrl.isLoggedIn()) return;
		boolean result = mEvernoteCtrl.getDefaultNotebook(new OnClientCallback<Notebook>(){

			@Override
			public void onSuccess(Notebook data) {
				// TODO Auto-generated method stub
				mDefaultNotebook = data;
				mDefaultNotebookResult = NOTEBOOK_RESULT_OK;
				setPrefNotebookSummary();
			}

			@Override
			public void onException(Exception e) {
				// TODO Auto-generated method stub
				mDefaultNotebookResult = NOTEBOOK_RESULT_ERROR;
				setPrefNotebookSummary();
				e.printStackTrace();
			}
			
		});
		if(!result) return;
		mDefaultNotebookResult = NOTEBOOK_RESULT_RECEIVING;
		setPrefNotebookSummary();
		
	}
	private void startReceiveNotebookList(){
		mNotebookListResult = NOTEBOOK_RESULT_NOT_YET;
		if(!mEvernoteCtrl.isLoggedIn()) return;
		boolean result = mEvernoteCtrl.getNotebookList(new OnClientCallback<List<Notebook>>(){

			@Override
			public void onSuccess(List<Notebook> data) {
				// TODO Auto-generated method stub
				mNoteBooks = data;
				CharSequence [] name = new CharSequence[data.size()];
				CharSequence [] guid = new CharSequence[data.size()];
				for(int i = 0;i < data.size();i++){
					Notebook n = data.get(i);
					name[i] = n.getName();
					guid[i] = n.getGuid();
				}
				mListPrefNotebook.setEntries(name);
				mListPrefNotebook.setEntryValues(guid);
				mNotebookListResult = NOTEBOOK_RESULT_OK;
				setPrefNotebookSummary();
			}

			@Override
			public void onException(Exception e) {
				// TODO Auto-generated method stub
				e.printStackTrace();
				mNotebookListResult = NOTEBOOK_RESULT_ERROR;
				setPrefNotebookSummary();
			}
		});
		if(!result) return;
		mNotebookListResult = NOTEBOOK_RESULT_RECEIVING;
		setPrefNotebookSummary();
	}

	private void updateAuthUi() {
		if(mSwitchPrefLogin.isChecked() != mEvernoteCtrl.isLoggedIn()){
			mSwitchPrefLogin.setChecked(mEvernoteCtrl.isLoggedIn());
		}
	}


	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(MyDebug.DEBUG) Log.d(LOG_TAG, "request code = " + requestCode + "oauth result = " + resultCode);
	    switch (requestCode) {
	      	//Update UI when oauth activity returns result
	    	case EvernoteSession.REQUEST_CODE_OAUTH:
	    		if(MyDebug.DEBUG) Log.d(LOG_TAG, "oauth result = " + (resultCode == Activity.RESULT_OK));
	    		updateAuthUi();
	    		if(mEvernoteCtrl.isLoggedIn()){
	    			startReceivingDefaultNotebook();
	    			startReceiveNotebookList();//login完了したらnotebookのリストを取得する
	    		}
	    		break;
	    	default:break;
	    }
	}
	
	
	public static String getNotebook(Context context){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.pref_evernote_notebook_list_key), "");
	}
	
	public static ArrayList<String>getTags(Context context){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		String val = pref.getString(context.getString(R.string.pref_evernote_tag_key), "");
		ArrayList<String> result = new ArrayList<String>();
		if(val == null || val.length() == 0) return result;
		val = val.replaceAll(" ", "");//半角・全角スペース, TABを取り除く
		val = val.replaceAll("　", "");
		val = val.replaceAll("	", "");
		String [] split = val.split(";");
		for(String s:split){
			if(s.length() == 0) continue;
			result.add(s);
		}
		return 	result;
	}

}
