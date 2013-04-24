package com.nekomeshi312.whiteboardcorrection;

import com.actionbarsherlock.app.SherlockDialogFragment;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

public class MyUtils {
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
