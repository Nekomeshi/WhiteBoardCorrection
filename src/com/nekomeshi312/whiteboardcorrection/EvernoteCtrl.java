package com.nekomeshi312.whiteboardcorrection;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.EvernoteUtil;
import com.evernote.client.android.InvalidAuthenticationException;
import com.evernote.client.android.OnClientCallback;
import com.evernote.client.conn.mobile.FileData;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.Resource;
import com.evernote.edam.type.ResourceAttributes;
import com.evernote.thrift.transport.TTransportException;


public class EvernoteCtrl {
	private static final String LOG_TAG = "EvernoteCtrl";
	/**
	* ************************************************************************
	* You MUST change the following values to run this sample application.    *
	* *************************************************************************
	*/

	// Your Evernote API key. See http://dev.evernote.com/documentation/cloud/
	// Please obfuscate your code to help keep these values secret.
	private static final String CONSUMER_KEY = "xxxxxxxxxxxxxxxx";
	private static final String CONSUMER_SECRET = "xxxxxxxxxxxxxxxx";

	// Initial development is done on Evernote's testing service, the sandbox.
	// Change to HOST_PRODUCTION to use the Evernote production service
	// once your code is complete, or HOST_CHINA to use the Yinxiang Biji
	// (Evernote China) production service.
	private static final EvernoteSession.EvernoteService EVERNOTE_SERVICE = EvernoteSession.EvernoteService.SANDBOX;	  
	private EvernoteSession mEvernoteSession;
	  
	private Context mContext;

	EvernoteCtrl(Context context){
		mContext = context;
		mEvernoteSession = EvernoteSession.getInstance(mContext, CONSUMER_KEY, CONSUMER_SECRET, EVERNOTE_SERVICE);
	}
	
	/**
	 * loginを実行する　すでにloginしているときは何もしない
	 */
	public void login() {
		if(!mEvernoteSession.isLoggedIn()){
			  mEvernoteSession.authenticate(mContext);
		}
	}

	/**
	 * ログアウトを実行する
	 */
	public void logout() {
		try {
			mEvernoteSession.logOut(mContext);
		}
		catch (InvalidAuthenticationException e) {
			Log.w(LOG_TAG, "Tried to call logout with not logged in", e);
		}
	}
	public boolean isLoggedIn(){
		return mEvernoteSession.isLoggedIn();
	}
	/**
	 * デフォルトのNotebookを取得する
	 * @param callback 取得できた/失敗した時に呼び出されるコールバック
	 * @return true:アクセス成功　false:失敗
	 */
	public boolean getDefaultNotebook(OnClientCallback<Notebook> callback){
		try {
			mEvernoteSession.getClientFactory().createNoteStoreClient().getDefaultNotebook(callback);
		}
		catch (TTransportException exception) {
			Log.e(LOG_TAG, "Error creating notestore", exception);
			Toast.makeText(mContext, R.string.error_msg_evernote_receiving_default_note, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}


	/**
	 * 登録されているNotebookの一覧を取得する
	 * @param callback 取得が完了/失敗した場合に呼ばれるコールバック
	 * @return true:アクセス開始　false:アクセス失敗
	 */
	public boolean getNotebookList(OnClientCallback<List<Notebook>> callback) {

		try {
			mEvernoteSession.getClientFactory().createNoteStoreClient().listNotebooks(callback);
		}
		catch (TTransportException exception) {
			Log.e(LOG_TAG, "Error creating notestore", exception);
			Toast.makeText(mContext, R.string.error_msg_evernote_listing_notebooks, Toast.LENGTH_LONG).show();
			return false;
		}
		
		return true;
	}


	/**
	 * 指定したファイルをnoteに保存する
	 * @param filePath ファイルのパス。最後にセパレータ"/"は入れない
	 * @param fileName ファイル名
	 * @param title noteのタイトル。nullの場合はファイル名がtitleとして設定される
	 * @param mimeType 送信するファイルのmimetype jpegの場合は"image/jpeg"
	 * @param tags セットするタグ。 セットしない場合はnull
	 * @param notebookGuid NotebookのGUID。デフォルトのNotebookを使う場合はnullを指定
	 * @param callback 送受信の完了・失敗時に呼ばれるコールバック
	 * @return true:成功　false:失敗　loginしていない、ファイルが読めないなど
	 */
	public boolean saveImage(String filePath, 
							String fileName, 
							String title,
							String mimeType,
							List<String>tags,
							String notebookGuid,
							OnClientCallback<Note> callback) {
		if (!mEvernoteSession.isLoggedIn()) return false;

		final String fn = filePath + "/" + fileName;
		try {
			// Hash the data in the image file. The hash is used to reference the
			// file in the ENML note content.
			InputStream in = new BufferedInputStream(new FileInputStream(fn));
			FileData data = new FileData(EvernoteUtil.hash(in), new File(fn));
			in.close();

			// Create a new Resource
			Resource resource = new Resource();
			resource.setData(data);
			resource.setMime(mimeType);
			ResourceAttributes attributes = new ResourceAttributes();
			attributes.setFileName(fileName);
			resource.setAttributes(attributes);

			// Create a new Note
			Note note = new Note();
			note.setTitle(title != null ? title:fileName);

			note.addToResources(resource);

			// Set the note's ENML content. Learn about ENML at
			// http://dev.evernote.com/documentation/cloud/chapters/ENML.php
			String content =
						EvernoteUtil.NOTE_PREFIX +
						"<p>This note was uploaded from WhiteBoardCorrection. It contains an image.</p>" +
						EvernoteUtil.createEnMediaTag(resource) +
						EvernoteUtil.NOTE_SUFFIX;

			note.setContent(content);
			if(notebookGuid != null && notebookGuid.length()>0) note.setNotebookGuid(notebookGuid);
			if(tags != null && tags.size() > 0) note.setTagNames(tags);
			// Create the note on the server. The returned Note object
			// will contain server-generated attributes such as the note's
			// unique ID (GUID), the Resource's GUID, and the creation and update time.
			mEvernoteSession.getClientFactory().createNoteStoreClient().createNote(note,  callback);
		} 
		catch (Exception ex) {
			Log.e(LOG_TAG, "Error creating notestore", ex);
			Toast.makeText(mContext, R.string.error_msg_evernote_creating_notestore, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}	
}
