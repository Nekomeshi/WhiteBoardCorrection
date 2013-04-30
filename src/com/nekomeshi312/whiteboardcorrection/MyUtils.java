package com.nekomeshi312.whiteboardcorrection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

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
	
	public static String getImageInfoFromIntent(Context context, 
												Intent intent, 
												int [] size){
		Uri uri = intent.getData();
		if(uri == null){
    		Log.w(LOG_TAG, "Uri == null");
    		return null;
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
    			return null;
    		}
			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filename, opt);//画像の解像度を取得
    		size[0] = opt.outWidth;
    		size[1] = opt.outHeight;
    		return filename;
		}
	}
	
	public static boolean copyFile(String src, String dst){
		FileChannel destChannel = null;
		FileChannel srcChannel = null;
		boolean result = false;
		try{
		    srcChannel = new FileInputStream(src).getChannel();
		    destChannel = new FileOutputStream(dst).getChannel();
	        srcChannel.transferTo(0, srcChannel.size(), destChannel);
	        result = true;
		} catch(FileNotFoundException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if(srcChannel != null)srcChannel.close();
				if(destChannel != null) destChannel.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		        result = false;
			}
		}
		return result;

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

			mParentActivity = (Activity)getSherlockActivity();
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
