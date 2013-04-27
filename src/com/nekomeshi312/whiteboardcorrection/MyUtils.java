package com.nekomeshi312.whiteboardcorrection;

import java.io.File;

import com.actionbarsherlock.app.SherlockDialogFragment;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

public class MyUtils {
	private static final String LOG_TAG = "MyUtils";
	
	/**
	 * 指定されたターゲット解像度に近くなるように画像を読み込む
	 * @param fn 読み込むファイル
	 * @param targetWidth ターゲット幅
	 * @param targetHeight　ターゲット高さ
	 * @return　成功時は読み込まれた画像のBitmap 失敗時はnull
	 */
	public static Bitmap loadJpeg(String fn, int targetWidth, int targetHeight){
		//画像のサイズを先読み
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(fn, opt);
		int ww = Math.max((int) ((double)opt.outWidth/(double)targetWidth + 0.5), 1);
		int hh = Math.max((int) ((double)opt.outHeight/(double)targetHeight + 0.5), 1);
		//縮小サイズを計算
		opt.inSampleSize = Math.max(ww, hh);
		//読み直し
		opt.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(fn, opt);
	}
	
	/**
	 * Intentで渡された画像の情報を取得する
	 * @param context context
	 * @param intent 情報を取得するintent
	 * @param pathInfo 画像のPath情報 pathInfo[0]:パス　 pathInfo[1]:ファイル名
	 * @param size 画像サイズ size[0]:width, size[1]:height
	 * @return true:成功　false:失敗
	 */
	public static boolean getImageInfoFromIntent(Context context, 
												Intent intent, 
												String [] pathInfo, 
												int [] size){
		Uri uri = intent.getData();
		if(uri == null){
    		Log.w(LOG_TAG, "Uri == null");
    		return false;
		}
		else{
			//uriからファイル名に変換
    		Cursor c = context.getContentResolver().query(uri, null, null, null, null);
    		c.moveToFirst();
    		String filename = null;
    		try{
    			filename = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
    		}
    		catch(IllegalArgumentException  e){
    			e.printStackTrace();
    			return false;
    		}
			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filename, opt);//画像の解像度を取得
    		File f = new File(filename);
    		pathInfo[0] = f.getParent();
    		pathInfo[1] = f.getName();
    		size[0] = opt.outWidth;
    		size[1] = opt.outHeight;
    		return true;
		}
	}
	
	public static class ProgressDialogFrag extends SherlockDialogFragment {
		private static final String KEY_TITLE = "title";
		private static final String KEY_MSG = "message";
    	private Activity mParentActivity;

		public static ProgressDialogFrag newInstance(String title, String msg) {
			ProgressDialogFrag frag = new ProgressDialogFrag();
		    Bundle args = new Bundle();
		    args.putString(KEY_TITLE, title);
		    args.putString(KEY_MSG, msg);
		    frag.setArguments(args);
		    return frag;
		}
		 
		@Override
		public void onCreate(Bundle savedInstanceState) {
			// TODO 自動生成されたメソッド・スタブ
		    super.onCreate(savedInstanceState);
		}
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
		    // TODO 自動生成されたメソッド・スタブ
		    //return super.onCreateDialog(savedInstanceState);
		 
		    final String title = getArguments().getString(KEY_TITLE);
		    final String msg = getArguments().getString(KEY_MSG);

			mParentActivity = (WhiteBoardCorrectionActivity)getSherlockActivity();
		    ProgressDialog proDialog = new ProgressDialog(mParentActivity);
		    proDialog.setCancelable(false);
		    proDialog.setTitle(title);
		    proDialog.setMessage(msg);
		    proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		    proDialog.show();
		    return proDialog;
		}
	}

}
