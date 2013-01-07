/*----------------------------------------------------------------------------

  WhiteBoardCorrection 

  This code is part of the following publication and was subject
  to peer review:

    "WhiteBoardCorrection" by Nekomeshi

  Copyright (c) Nekomeshi <Nekomeshi312@gmail.com>

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program. If not, see <http://www.gnu.org/licenses/>.

  ----------------------------------------------------------------------------*/
package com.nekomeshi312.whiteboardcorrection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.nekomeshi312.uitools.NumberPicker2;

public class WhiteBoardCheckFragment extends SherlockFragment{
	public interface WhiteBoardCheckCallback{
		public void onWhiteBoardCorrectionCompleted();
	}

	private static final String TAG_FRAGMENT = "tag";
	private static final String TAG_FILE_PATH = "filepath";
	private static final String TAG_FILE_NAME = "filename";
	private static final String TAG_WIDTH = "width";
	private static final String TAG_HEIGHT = "height";
	private static final String TAG_PREV_WIDTH = "prevWidth";
	private static final String TAG_PREV_HEIGHT = "prevHeight";
	private static final String TAG_WB_POS = "whiteboardPos";

	private static final String LOG_TAG = "WhiteBoardCheckFragment";
	private Activity mParentActivity = null;
	private int mPicWidth = -1;
	private int mPicHeight = -1;
	private int mPrevWidth = -1;
	private int mPrevHeight = -1;
	private String mFilePath = null;
	private String mFileName = null;
	private double [] mPreviewPoints = null;
	private boolean mArgsOK = true;
	private boolean mIsPictureOK = false;//Whiteboardの補正がかけられる状態にあればtrue

	private ImageView mCapturedImageView = null;
	private WhiteBoardAreaView mWBCorrectionView = null;
	private FrameLayout mWBCheckViewBase = null;
	private Bitmap mPicBitmap = null;

	
	private Menu mMenu = null;
	private int mOutputWidth = 640;
	private int mOutputAspectWidth = 4;
	private int mOutputAspectHeight = 2;

	private String mMyTag = null;
	
	public static WhiteBoardCheckFragment newInstance(String tag, 
														String path, 
														String name, 
														int width, 
														int height, 
														int prevWidth, 
														int prevHeight, 
														ArrayList<Point> detectedPoints) {
		WhiteBoardCheckFragment frag = new WhiteBoardCheckFragment();
        Bundle args = new Bundle();
        args.putString(TAG_FRAGMENT, tag);
        args.putString(TAG_FILE_PATH, path);
		args.putString(TAG_FILE_NAME, name);
		args.putInt(TAG_WIDTH, width);
		args.putInt(TAG_HEIGHT, height);
		args.putInt(TAG_PREV_WIDTH, prevWidth);
		args.putInt(TAG_PREV_HEIGHT, prevHeight);
		if(detectedPoints != null){
			double [] points = new double[8];
			for(int i = 0;i < 4;i++){
				points[i*2 + 0] = detectedPoints.get(i).x;
				points[i*2 + 1] = detectedPoints.get(i).y;
			}
			args.putDoubleArray(TAG_WB_POS, points);
		}
        frag.setArguments(args);
        return frag;
    }

	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragment#onAttach(android.app.Activity)
	 */
	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
		if(!(activity instanceof WhiteBoardCheckCallback)){
			Log.e(LOG_TAG, "Parent is not instance of WhiteBoardCheckCallback");
			mParentActivity = null;
			return;
		}
		mParentActivity = activity;
	}

	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragment#onDetach()
	 */
	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		super.onDetach();
		mParentActivity = null;
		if(MyDebug.DEBUG)Log.i(LOG_TAG, "onDetach");

	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		mOutputWidth = CameraSettingActivity.getResolutionWidth(mParentActivity);
		mOutputAspectWidth = CameraSettingActivity.getAspectWidth(mParentActivity);
		mOutputAspectHeight = CameraSettingActivity.getAspectHeight(mParentActivity);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		if(MyDebug.DEBUG)Log.i(LOG_TAG, "onCreateView");
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_white_board_check, container, false);
        final Bundle args = getArguments();
        mArgsOK = true;
        if(args == null){
        	mArgsOK = false;
        }
        else{
    		mMyTag = args.getString(TAG_FRAGMENT);
            mFilePath = args.getString(TAG_FILE_PATH);
            mFileName = args.getString(TAG_FILE_NAME);
            mPicWidth = args.getInt(TAG_WIDTH, -1);
            mPicHeight = args.getInt(TAG_HEIGHT, -1);
            mPrevWidth = args.getInt(TAG_PREV_WIDTH, -1);
            mPrevHeight = args.getInt(TAG_PREV_HEIGHT, -1);
            mPreviewPoints = args.getDoubleArray(TAG_WB_POS);
            
            if(mFilePath == null){
            	Log.w(LOG_TAG, "Invalid picture path");
            	mArgsOK = false;
            }
            if(mFileName == null){
            	Log.w(LOG_TAG, "Invalid picture name");
            	mArgsOK = false;
            }
            if(mPicWidth < 0){
            	Log.w(LOG_TAG, "Invalid picture width(" + mPicWidth + ")");
            	mArgsOK = false;
            }
            if(mPicHeight < 0){
            	Log.w(LOG_TAG, "Invalid picture height(" + mPicHeight + ")");
            	mArgsOK = false;
            }
            if(mPrevWidth < 0){
            	Log.w(LOG_TAG, "Invalid preview width(" + mPrevWidth + ")");
            	mArgsOK = false;
            }
            if(mPrevHeight < 0){
            	Log.w(LOG_TAG, "Invalid preview height(" + mPrevHeight + ")");
            	mArgsOK = false;
            }
            
            if(MyDebug.DEBUG){
            	Log.d(LOG_TAG, "FilePath = " + mFilePath);
            	Log.d(LOG_TAG, "FileName = " + mFileName);
            	Log.d(LOG_TAG, "PicWidth = " + mPicWidth);
            	Log.d(LOG_TAG, "PicHeight = " + mPicHeight);
            	Log.d(LOG_TAG, "PreviewWidth = " + mPrevWidth);
            	Log.d(LOG_TAG, "PreviewHeight = " + mPrevHeight);
            	if(mPreviewPoints == null){
            		Log.d(LOG_TAG, "mPreviewPoints = null");
            	}
            	else{
                	for(int i = 0;i < 4;i++){
                		Log.d(LOG_TAG, "PrevPoints(" + i + ") = " + mPreviewPoints[i*2] + ":" + mPreviewPoints[i*2+1]);
                	}
            	}
            }
        }
        ActionBar actionBar =  ((SherlockFragmentActivity) mParentActivity).getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		mCapturedImageView = (ImageView) root.findViewById(R.id.captured_image);
		mWBCorrectionView = (WhiteBoardAreaView) root.findViewById(R.id.whiteboard_area_correction_view);
		mWBCorrectionView.setDraggable(true);
		mWBCheckViewBase = (FrameLayout)root.findViewById(R.id.area_check_view_base);
		mWBCheckViewBase.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener(){

			@Override
			public void onGlobalLayout() {//親のviewのサイズが確定するのを待って読み出しを開始する
				// TODO Auto-generated method stub
				if(mPicBitmap == null){
					//jpegファイルをロードしWBエリアを再度計算し直す
					LoadJpegFileAsyncTask loadJpegTask = new LoadJpegFileAsyncTask ();
					loadJpegTask.execute();
				}
			}
			
		});
        return root;
	}
	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onDestroyView()
	 */
	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onResume()
	 */
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(!mArgsOK){
			Toast.makeText(mParentActivity, R.string.error_msg_saved_file_info, Toast.LENGTH_SHORT).show();
			return;
		}
	}

	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragment#onCreateOptionsMenu(com.actionbarsherlock.view.Menu, com.actionbarsherlock.view.MenuInflater)
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO Auto-generated method stub
    	inflater.inflate(R.menu.fragment_whiteboard_check, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragment#onPrepareOptionsMenu(com.actionbarsherlock.view.Menu)
	 */
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		mMenu = menu;
		setResolutionToOptionMenu();
		setAspectToOptionMenu();
		setCalcableToOptionMenu();
		super.onPrepareOptionsMenu(menu);
	}
	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragment#onOptionsItemSelected(com.actionbarsherlock.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		 switch (item.getItemId()) {
		 	case R.id.menu_resolution_select:
				setResolutionToOptionMenu();
		 		Toast.makeText(mParentActivity, R.string.menu_resolution_explanation, Toast.LENGTH_SHORT).show();
		 		return true;
		 	case R.id.menu_whiteboard_correct_ok:
		 		WarpImageAsyncTask task = new WarpImageAsyncTask();
		 		task.execute();
		 		return true;
		 	case R.id.menu_resolution_1920:
		 		mOutputWidth = 1920;
		 		CameraSettingActivity.setResolutionWidth(mParentActivity, mOutputWidth);
		 		return true;
		 	case R.id.menu_resolution_1280:
		 		mOutputWidth = 1280;
		 		CameraSettingActivity.setResolutionWidth(mParentActivity, mOutputWidth);
		 		return true;
		 	case R.id.menu_resolution_1024:
		 		mOutputWidth = 1024;
		 		CameraSettingActivity.setResolutionWidth(mParentActivity, mOutputWidth);
		 		return true;
		 	case R.id.menu_resolution_640:
		 		mOutputWidth = 640;
		 		CameraSettingActivity.setResolutionWidth(mParentActivity, mOutputWidth);
		 		return true;
		 	case R.id.menu_aspect_select:
				setAspectToOptionMenu();
		 		Toast.makeText(mParentActivity, R.string.menu_aspect_explanation, Toast.LENGTH_SHORT).show();
		 		return true;
		 	case R.id.menu_aspect_4_3:
		 		setAspect(4, 3);
		 		return true;
		 	case R.id.menu_aspect_3_2:
		 		setAspect(3, 2);
		 		return true;
		 	case R.id.menu_aspect_16_10:
		 		setAspect(16, 10);
		 		return true;
		 	case R.id.menu_aspect_16_9:
		 		setAspect(16, 9);
		 		return true;
		 	case R.id.menu_aspect_2_1:
		 		setAspect(2, 1);
		 		return true;
		 	case R.id.menu_aspect_other:
				final ChoseAspectDlgFragment dlgFragment = ChoseAspectDlgFragment.newInstance(mMyTag, mOutputAspectWidth, mOutputAspectHeight);
				dlgFragment.show(getSherlockActivity().getSupportFragmentManager(), "dialog");
		 		return true;

		    default:
		        break;	
		 }
		 return super.onOptionsItemSelected(item);
	}

	/**
	 * WBエリアの計算に失敗しているときは「確認」メニューを無効化する
	 */
	private void setCalcableToOptionMenu(){
		try{//menuボタンがある端末ではmenuが開いていないことがある（？）のでtry/catchでエラーにならないようにする
	    	MenuItem item = mMenu.findItem(R.id.menu_whiteboard_correct_ok);
			item.setEnabled(mIsPictureOK);
		}
		catch(Exception e){
			if(MyDebug.DEBUG) e.printStackTrace();
			return;
		}
	}
	/**
	 * aspect比選択メニューで現在選択されているaspect比にチェックを入れる
	 */
	private void setAspectToOptionMenu(){
		try{//menuボタンがある端末ではmenuが開いていないことがある（？）のでtry/catchでエラーにならないようにする
	    	MenuItem item = mMenu.findItem(R.id.menu_aspect_select);
	    	SubMenu subMenu = item.getSubMenu();
	    	String currentTitle =String.format(mParentActivity.getString(R.string.menu_aspect_title_format),
	    						mOutputAspectWidth, mOutputAspectHeight);
	    	for(int i = 0;i < subMenu.size();i++){
	    		MenuItem subItem = subMenu.getItem(i);
	    		if(currentTitle.equals(subItem.getTitle())){
	    			subItem.setChecked(true);
	    			return;
	    		}
	    	}
	    	//どのアスペクト比にも合わなかったときは「その他」
			MenuItem subItem = subMenu.getItem(subMenu.size()-1);
			String title = String.format(mParentActivity.getString(R.string.menu_aspect_other_format), 
											mOutputAspectWidth, mOutputAspectHeight);
			subItem.setTitle(title);
			subItem.setChecked(true);
			return;
		}
		catch(Exception e){
			if(MyDebug.DEBUG)e.printStackTrace();
		}
		
	}
	/**
	 * 出力解像度選択メニューで現在選択されている解像度wにチェックを入れる
	 */
	private void setResolutionToOptionMenu(){
		try{//menuボタンがある端末ではmenuが開いていないことがある（？）のでtry/catchでエラーにならないようにする
	    	MenuItem item = mMenu.findItem(R.id.menu_resolution_select);
	    	SubMenu subMenu = item.getSubMenu();
	    	String currentTitle =String.format(mParentActivity.getString(R.string.menu_resolution_title_format),
	    						mOutputWidth);
	    	for(int i = 0;i < subMenu.size();i++){
	    		MenuItem subItem = subMenu.getItem(i);
	    		if(currentTitle.equals(subItem.getTitle())){
	    			subItem.setChecked(true);
	    			break;
	    		}
	    	}
		}
		catch(Exception e){
			if(MyDebug.DEBUG) e.printStackTrace();
			return;
		}
	}
	/**
	 * 指定されたターゲット解像度に近くなるように画像を読み込む
	 * @param fn 読み込むファイル
	 * @param targetWidth ターゲット幅
	 * @param targetHeight　ターゲット高さ
	 * @return　成功時は読み込まれた画像のBitmap 失敗時はnull
	 */
	private static Bitmap loadJpeg(String fn, int targetWidth, int targetHeight){
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
	 * 選択されたaspect比をプレファレンスに保存する
	 * @param width　aspect比の横
	 * @param height　aspect比の縦
	 */
	private void setAspect(int width, int height){
 		mOutputAspectWidth = width;
 		mOutputAspectHeight = height;
 		CameraSettingActivity.setAspectWidth(mParentActivity, mOutputAspectWidth);
 		CameraSettingActivity.setAspectHeight(mParentActivity, mOutputAspectHeight);

	}
	/**
	 * その他のaspectが選択された時のaspect比をマニュアル入力するためのダイアログフラグメント
	 * @author masaki
	 *
	 */
    public static class ChoseAspectDlgFragment extends SherlockDialogFragment {
    	private static final int MAX_ASPECT_VAL = 100;
    	private static final String KEY_WIDTH = "width";
    	private static final String KEY_HEIGHT = "height";
    	private static final String KEY_TAG = "tag";
    	/**
    	 * fragmentを作成するメソッド
    	 * @param tag このfragmentを生成する親fragmentのタグ　この親のメソッドをこのクラスから実行するときに使用する
    	 * @param width　defaultの幅
    	 * @param height　defaultの高さ
    	 * @return
    	 */
    	public static ChoseAspectDlgFragment newInstance(String tag, int width, int height) {
    		ChoseAspectDlgFragment frag = new ChoseAspectDlgFragment();
            Bundle args = new Bundle();
            args.putString(KEY_TAG, tag);
            args.putInt(KEY_WIDTH, width);
            args.putInt(KEY_HEIGHT, height);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
        	int width = getArguments().getInt(KEY_WIDTH);
        	int height = getArguments().getInt(KEY_HEIGHT);
        	final String tag = getArguments().getString(KEY_TAG);

            LayoutInflater inflater = getActivity().getLayoutInflater();  
            View view = inflater.inflate(R.layout.fragment_aspect_input, null, false);  
            
            final NumberPicker2 pickWidth = (NumberPicker2) view.findViewById(R.id.aspect_select_width);
            final NumberPicker2 pickHeight = (NumberPicker2) view.findViewById(R.id.aspect_select_height);
            if(width < 1) width = 1;
            if(width > MAX_ASPECT_VAL) width = MAX_ASPECT_VAL;
            if(height < 1) height = 1;
            if(height > MAX_ASPECT_VAL) height = MAX_ASPECT_VAL;
            pickWidth.setRange(1, MAX_ASPECT_VAL);
            pickWidth.setCurrent(width);
            pickHeight.setRange(1, MAX_ASPECT_VAL);
            pickHeight.setCurrent(height);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.aspect_dlg_title)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener(){

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							final int w = pickWidth.getCurrent();
							final int h = pickHeight.getCurrent();
							//クラスがstaticなので、activityからtagでfragmentを見つけてfragment内のメソッドを読み出す（遠回り・・他にいい方法ない？）
							WhiteBoardCorrectionActivity activity = (WhiteBoardCorrectionActivity)getSherlockActivity();
							FragmentManager fm = activity.getSupportFragmentManager();
							WhiteBoardCheckFragment parentFragment = 
										(WhiteBoardCheckFragment) fm.findFragmentByTag(tag);
							
							parentFragment.setAspect(w, h);
						}
                    })
            		.setNegativeButton("Cancel", null);
            builder.setView(view);
            return builder.create();
        }
    }
    
    /**
     * warpされた結果を確認するダイアログフラグメント
     * @author masaki
     *
     */
    public static class WarpResultDlgFragment extends SherlockDialogFragment {
		private static final String KEY_TAG = "tag";
    	private static final String KEY_AFTER_FILENAME = "afterfn";
    	private static final String KEY_BEFORE_FILENAME = "beforefn";
    	private static final int IMG_SIZE = 200;//dip

    	private String mBeforeFn = null;
    	private String mAfterFn = null;
    	/**
    	 * fragmentを作成するメソッド
    	 * @param tag　親フラグメントのtag このフラグメントから親フラグメントのメソッドを実行する際に使用する
    	 * @param beforeFn　warp前の画像のファイル名
    	 * @param afterFn warp後の画像のファイル名
    	 * @return
    	 */
    	public static WarpResultDlgFragment newInstance(String tag, 
    													String beforeFn,
    													String afterFn) {
    		WarpResultDlgFragment frag = new WarpResultDlgFragment();
            Bundle args = new Bundle();
            args.putString(KEY_TAG, tag);
            args.putString(KEY_BEFORE_FILENAME, beforeFn);
            args.putString(KEY_AFTER_FILENAME, afterFn);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
        	mBeforeFn = getArguments().getString(KEY_BEFORE_FILENAME);
        	mAfterFn = getArguments().getString(KEY_AFTER_FILENAME);
        	final String tag = getArguments().getString(KEY_TAG);
        	//ダイアログに表示する画像のサイズをpx->dipに変換
        	final float scale = getActivity().getResources().getDisplayMetrics().density;
        	final int imgsize = (int) ((double)IMG_SIZE*scale + 0.5);
            LayoutInflater inflater = getActivity().getLayoutInflater();  
            View view = inflater.inflate(R.layout.fragment_warp_result, null, false);  
            ImageView iv = (ImageView)view.findViewById(R.id.image_warp_before);
            iv.setImageBitmap(loadJpeg(mBeforeFn, imgsize, imgsize));
            iv = (ImageView)view.findViewById(R.id.image_warp_after);
            iv.setImageBitmap(loadJpeg(mAfterFn, imgsize, imgsize));

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
            	.setIcon(R.drawable.ic_launcher)
            	.setTitle(R.string.warp_check_dlg_title)
            	.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
				
            		@Override
            		public void onClick(DialogInterface dialog, int which) {
            			// TODO Auto-generated method stub
            			//warp前の画像ファイルを削除
            			File f = new File(mBeforeFn);
            			f.delete();
            			//保存したjpge画像をギャラリーに登録
            			ContentResolver cr = getSherlockActivity().getContentResolver();
            			ContentValues values = new ContentValues();  
            			f = new File(mAfterFn);
            			String name = f.getName();
            			values.put(Images.Media.TITLE, name);  
            			values.put(Images.Media.DISPLAY_NAME, name); 
            			values.put(Images.Media.MIME_TYPE, "image/jpeg");  
            			values.put(Images.Media.DATA, mAfterFn);  
            			cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);  	
            			//保存したファイル名をtoastで表示
            			Toast.makeText(getSherlockActivity(), 
									getSherlockActivity().getString(R.string.save_as_following_name) + mAfterFn, 
									Toast.LENGTH_SHORT).show();
            			//親fragmentのメソッドを呼び出し、処理が完了したことを知らせる
            			WhiteBoardCorrectionActivity activity = (WhiteBoardCorrectionActivity)getSherlockActivity();
            			FragmentManager fm = activity.getSupportFragmentManager();
            			WhiteBoardCheckFragment parentFragment = 
								(WhiteBoardCheckFragment) fm.findFragmentByTag(tag);
            			final WhiteBoardCorrectionActivity parentActivity = 
								(WhiteBoardCorrectionActivity) parentFragment.mParentActivity;
            			parentActivity.onWhiteBoardCorrectionCompleted();
            		}
            	})
            	.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
            		@Override
            		public void onClick(DialogInterface dialog, int which) {
            			// TODO Auto-generated method stub
            			deleteAllImgFile();
            		}
            	})
            	.setCancelable(false);

            builder.setView(view);
            return builder.create();
        }
        /**
         * warp前、warp後両画像のファイルを削除する
         */
        private void deleteAllImgFile(){
			File f = new File(mBeforeFn);
			f.delete();
			f = new File(mAfterFn);
			f.delete();
        	
        }
		/* (non-Javadoc)
		 * @see android.support.v4.app.DialogFragment#onCancel(android.content.DialogInterface)
		 */
		@Override
		public void onCancel(DialogInterface dialog) {
			// TODO Auto-generated method stub
			super.onCancel(dialog);
			deleteAllImgFile();
		}

    	/* (non-Javadoc)
		 * @see android.support.v4.app.DialogFragment#onDismiss(android.content.DialogInterface)
		 */
		@Override
		public void onDismiss(DialogInterface dialog) {
			// TODO Auto-generated method stub
			super.onDismiss(dialog);
		}
    }
    /**
     * warpを非同期実行するためのAsyncTaskクラス
     * @author masaki
     *
     */
    private class WarpImageAsyncTask extends AsyncTask<Void, Void, Integer>{
    	private static final int RESULT_STATUS_OK = 0;
    	private static final int RESULT_STATUS_FILE_LOAD_ERROR = 1;
    	private static final int RESULT_STATUS_FILE_SAVE_ERROR = 2;
    	private static final int RESULT_STATUS_OUT_OF_MEMORY = 3;
    	
		private ProgressDialog mLoadingDialog;
		private Mat mWarpImg;
		private ArrayList<Point>mCornerPointUnwarp = new ArrayList<Point>();
		private Size mTargetSize = new Size();
		private String mClippedFileName = null;
		private String mWarpedFileName = null;

		private void deleteFiles(){
			if(mWarpedFileName != null){
				File f = new File(mWarpedFileName);
				f.delete();
			}
			if(mClippedFileName != null){
				File f = new File(mClippedFileName);
				f.delete();
			}
		}
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onCancelled()
		 */
		@Override
		protected void onCancelled() {
			// TODO Auto-generated method stub
			super.onCancelled();
			try{
				if(mLoadingDialog != null) mLoadingDialog.dismiss();
			}
			catch(IllegalArgumentException e){//親のactivityが閉じてしまっている場合のエラーを無視する
				e.printStackTrace();
			}
			mLoadingDialog = null;
			if(mWarpImg != null){
				mWarpImg.release();
				mWarpImg = null;
			}
			deleteFiles();
		}


		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Integer result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			try{
				if(mLoadingDialog != null) mLoadingDialog.dismiss();
			}
			catch(IllegalArgumentException e){//親のactivityが閉じてしまっている場合のエラーを無視する
				e.printStackTrace();
			}
			mLoadingDialog = null;
			if(mWarpImg != null){
				mWarpImg.release();
				mWarpImg = null;
			}
			switch(result){
				case RESULT_STATUS_FILE_LOAD_ERROR:
					Toast.makeText(mParentActivity, R.string.error_msg_cant_load_image_file, Toast.LENGTH_SHORT).show();
					deleteFiles();
					break;
				case RESULT_STATUS_FILE_SAVE_ERROR:
					Toast.makeText(mParentActivity, R.string.error_msg_cant_save_image_file, Toast.LENGTH_SHORT).show();
					deleteFiles();
					break;
				case RESULT_STATUS_OUT_OF_MEMORY:
					Toast.makeText(mParentActivity, R.string.error_msg_warp_out_of_memory, Toast.LENGTH_SHORT).show();
					deleteFiles();
					break;
				case RESULT_STATUS_OK://warp成功。warp結果ダイアログを表示する
					final  WarpResultDlgFragment dlgFragment =  
								WarpResultDlgFragment.newInstance(mMyTag, mClippedFileName, mWarpedFileName);
					dlgFragment.show(getSherlockActivity().getSupportFragmentManager(), "dialog");
					break;
			}
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			mLoadingDialog = new ProgressDialog(mParentActivity);
			String msg = mParentActivity.getString(R.string.about_dialog_wait);
			mLoadingDialog.setMessage(msg);
		    // 円スタイル（くるくる回るタイプ）に設定します
		    mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		    mLoadingDialog.setCancelable(false);
		    // プログレスダイアログを表示
		    mLoadingDialog.show();

		}

		/**
		 * jpegファイルをWBエリアのみ、出力サイズとほぼ同じ大きさになるように読み出し、その大きさに合うようにWBエリアの大きさをリサイズする
		 * @return true:成功　false:失敗
		 * @throws OutOfMemoryError　メモリ不足時
		 */
		private boolean loadJpegAndCalcTargetAreaSize() throws OutOfMemoryError {
			try{
				ArrayList<Point> corners = mWBCorrectionView.getWhiteBoardCorners();
				
				//ターゲットエリアを囲む矩形領域
				int rectLeft = Integer.MAX_VALUE;
				int rectRight = Integer.MIN_VALUE;
				int rectTop = Integer.MAX_VALUE;
				int rectBottom = Integer.MIN_VALUE;
				for(Point p:corners){
					if(p.x < rectLeft) rectLeft = (int)(p.x + 0.5);
					if(p.y < rectTop) rectTop = (int)(p.y + 0.5);
					if(p.x > rectRight) rectRight = (int)(p.x + 0.5);
					if(p.y > rectBottom) rectBottom = (int)(p.y + 0.5);
				}
				//現在読まれている画像サイズ
				final int imgWidth = mPicBitmap.getWidth();
				final int imgHeight = mPicBitmap.getHeight();
				//オリジナルの画像サイズを取得
				BitmapFactory.Options opt = new BitmapFactory.Options();
				opt.inJustDecodeBounds = true;
				final String fn = mFilePath + mFileName;
				BitmapFactory.decodeFile(fn, opt);
				double ratioW = (double)opt.outWidth/(double)imgWidth;//現在の読み込みデータの縮小比　-> オリジナル/現在読み込みサイズ
				double ratioH = (double)opt.outHeight/(double)imgHeight;
				if(MyDebug.DEBUG) Log.d(LOG_TAG, "ratio = " + ratioW + "/" + ratioH);
				
				//オリジナルの画像サイズに換算した時のターゲットエリアの矩形領域
				Rect orgRct = new Rect();
				orgRct.left = (int) (rectLeft*ratioW + 0.5);
				orgRct.right = (int) (rectRight*ratioW + 0.5);
				orgRct.top = (int) (rectTop*ratioH + 0.5);
				orgRct.bottom = (int) (rectBottom*ratioH + 0.5);
				
				//設定されている出力横幅とaspect比から、出力画像の解像度を計算
				mTargetSize.width = mOutputWidth;
				mTargetSize.height = (int)((double)mOutputWidth * (double)mOutputAspectHeight/(double)mOutputAspectWidth + 0.5);
				if(MyDebug.DEBUG) Log.d(LOG_TAG, "TargetSize  = " + mTargetSize.width + "/" + mTargetSize.height);
				//オリジナルの画像ファイルから読み出すときのリサイズ率を計算
				final int wStep = Math.max((int) ((double)(orgRct.right - orgRct.left)/(double)mTargetSize.width + 0.5), 1);
				final int hStep = Math.max((int) ((double)(orgRct.bottom - orgRct.top)/(double)mTargetSize.height + 0.5), 1);
				final int step = Math.min(wStep, hStep);
				//読み出された画像の予定サイズ
				final int w = (int) ((double)(orgRct.right - orgRct.left)/(double)step + 0.5);
				final int h = (int) ((double)(orgRct.bottom - orgRct.top)/(double)step + 0.5);
				if(MyDebug.DEBUG){
					Log.d(LOG_TAG, "Step  = " + wStep + ":" + hStep + " Result Size = " + w + "/" + h);
				}
				//縮小サイズを計算
				opt.inSampleSize = step;
				//読み直し
				opt.inJustDecodeBounds = false;
				Bitmap bmp = null;
				BitmapRegionDecoder decoder;
				try {
					//jpegの部分読み出し
					if(orgRct.left < 0) orgRct.left = 0;
					if(orgRct.right >= opt.outWidth)orgRct.right = opt.outWidth-1;
					if(orgRct.top < 0) orgRct.top = 0;
					if(orgRct.bottom >= opt.outHeight) orgRct.bottom = opt.outHeight-1;
					decoder = BitmapRegionDecoder.newInstance(fn, false);
					bmp = decoder.decodeRegion(orgRct, opt);
					//領域の４頂点を、読みだした画像サイズに合うように位置を移動
					for(Point p:corners){
						Point np = new Point();
						np.x = (double)(p.x*ratioW - orgRct.left)/(double)step;
						np.y = (double)(p.y*ratioH - orgRct.top)/(double)step;
						mCornerPointUnwarp.add(np);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(bmp == null) return false;
				mWarpImg = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC4);
			    Utils.bitmapToMat(bmp, mWarpImg);
			    bmp.recycle();
			    //読み出し倍率が1/2^nでない場合は予定の大きさとずれることがあるため再度リサイズする
			    Imgproc.resize(mWarpImg, mWarpImg, new Size(w, h));
			    if(MyDebug.DEBUG){
			    	Log.d(LOG_TAG, "imgsize = " + mWarpImg.cols() + ":" + mWarpImg.rows());
			    	final int sz = mCornerPointUnwarp.size();
			    	for(int i = 0;i < sz;i++){
				    	Core.line(mWarpImg, mCornerPointUnwarp.get(i), mCornerPointUnwarp.get((i+1)%sz), new Scalar(0xff, 0x00, 0x00), 5);
				    	Log.d(LOG_TAG, "point = " + mCornerPointUnwarp.get(i).x + ":" + mCornerPointUnwarp.get(i).y);
			    	}
			    }
			}
			catch(OutOfMemoryError e){
				e.printStackTrace();
				throw e;
			}
			return (true);
		}
		/**
		 * bitmapをファイルに保存する
		 * @param mBitmap 保存するbitmap
		 * @param name 保存するファイル名（パス無し）
		 * @return　保存されたファイルのフルパス
		 */
		private String saveBitmapToSd(Bitmap mBitmap, String name) {

			try {
				// 保存処理開始
				FileOutputStream fos = null;
				String fn = mFilePath + name;
				fos = new FileOutputStream(new File(fn));
				// jpegで保存
				mBitmap.compress(CompressFormat.JPEG, 100, fos);
				// 保存処理終了
				fos.close();
				return fn;
			} catch (Exception e) {
				Log.e("Error", "" + e.toString());
			}
			return null;
		}

		@Override
		protected Integer doInBackground(Void... params) {
			// TODO Auto-generated method stub
			for(int i = 0;i < 2;i++){
				try{
					if(loadJpegAndCalcTargetAreaSize() == false) return RESULT_STATUS_FILE_LOAD_ERROR;
					Bitmap bmp = Bitmap.createBitmap(mWarpImg.cols(), 
							mWarpImg.rows(), 
								Bitmap.Config.ARGB_8888);
					//warp前の画像を保存
					Utils.matToBitmap(mWarpImg, bmp);
					String fn = saveBitmapToSd(bmp, "tmp.jpg");
					bmp.recycle();
					if(fn == null) return RESULT_STATUS_FILE_SAVE_ERROR;
					mClippedFileName = fn;
					break;
				}
				catch(OutOfMemoryError e1){
					if(i == 1) return RESULT_STATUS_OUT_OF_MEMORY;
					e1.printStackTrace();
					System.gc();			//out of memoryの場合はGCを走らせてメモリを開けたあと再度トライする
				}
			}

			//warpするhomography行列を作成
			//変換前の座標
			MatOfPoint2f source = new MatOfPoint2f(	mCornerPointUnwarp.get(0), 
														mCornerPointUnwarp.get(1), 
															mCornerPointUnwarp.get(2), 
																mCornerPointUnwarp.get(3));
			//変換後の座標
			Point p0 = new Point();
			p0.x = 0;p0.y = 0;
			Point p1 = new Point();
			p1.x = mTargetSize.width;p1.y = 0;
			Point p2 = new Point();
			p2.x = mTargetSize.width;p2.y = mTargetSize.height;
			Point p3 = new Point();
			p3.x = 0;p3.y = mTargetSize.height;
			MatOfPoint2f target = new MatOfPoint2f(p0, p1, p2, p3);
			//homography行列生成
			Mat m =  Calib3d.findHomography(source, target);
			//warp実行
			Imgproc.warpPerspective(mWarpImg, mWarpImg, m, mTargetSize, Imgproc.INTER_CUBIC);
			m.release();
			//warp後の画像をファイルに保存
			Bitmap bmp = Bitmap.createBitmap(mWarpImg.cols(), 
										mWarpImg.rows(), 
											Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(mWarpImg, bmp);
			//warp後のファイル名を作成
			String warpName = mFileName.replaceAll(mParentActivity.getString(R.string.picture_base_name), 
													mParentActivity.getString(R.string.picture_warped_name));
			String fn = saveBitmapToSd(bmp, warpName);
			if(fn == null) return RESULT_STATUS_FILE_SAVE_ERROR;
			mWarpedFileName = fn;
			bmp.recycle();
			return RESULT_STATUS_OK;
		}
    }
	
	private class LoadJpegFileAsyncTask extends AsyncTask<Void, Integer, ArrayList<Point>>{
		private static final int PROGRESS_STEP_JPEGSIZE_DECIDED = 0;
		private static final int PROGRESS_STEP_LOAD_JPEG = 1;
		private static final int PROGRESS_STEP_DETECT_LINE = 2;
		private static final int PROGRESS_STEP_DETECT_WB = 3;
		private ProgressDialog mLoadingDialog;
		private int mScreenWidth;
		private int mScreenHeight;

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onCancelled()
		 */
		@Override
		protected void onCancelled() {
			// TODO Auto-generated method stub
			super.onCancelled();
			try{
				if(mLoadingDialog != null) mLoadingDialog.dismiss();
			}
			catch(IllegalArgumentException e){//親のactivityが閉じてしまっている場合のエラーを無視する
				e.printStackTrace();
			}

			mLoadingDialog = null;
			Toast.makeText(mParentActivity, R.string.white_board_check_canceled, Toast.LENGTH_SHORT).show();
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(ArrayList<Point> result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			try{
				if(mLoadingDialog != null) mLoadingDialog.dismiss();
			}
			catch(IllegalArgumentException e){//親のactivityが閉じてしまっている場合のエラーを無視する
				e.printStackTrace();
			}
			mLoadingDialog = null;
			if(result == null){
				Toast.makeText(mParentActivity, R.string.error_msg_cant_detect_wb, Toast.LENGTH_SHORT).show();
			}
			else{
				mWBCorrectionView.setWhiteBoardCorners(result);
				mIsPictureOK = true;
			}
			setCalcableToOptionMenu();
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			mLoadingDialog = new ProgressDialog(mParentActivity);
			String msg = mParentActivity.getString(R.string.about_dialog_wait);
			mLoadingDialog.setMessage(msg);
		    // 円スタイル（くるくる回るタイプ）に設定します
		    mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		    mLoadingDialog.setCancelable(false);
		    // プログレスダイアログを表示
		    mLoadingDialog.show();

			final View viewBase = (View)mParentActivity.findViewById(R.id.camera_view_base);
			mScreenWidth = viewBase.getWidth();
			mScreenHeight = viewBase.getHeight();
			if(MyDebug.DEBUG)Log.d(LOG_TAG, "viewBaseSize = " + mScreenWidth + ":" + mScreenHeight);
			mIsPictureOK = false;
		}
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
		 */
		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);

			switch(values[0]){
				case PROGRESS_STEP_JPEGSIZE_DECIDED:
					//jpegサイズが決まったので、親のviewのサイズをそれに合わせる
					final int w = values[1];
					final int h = values[2];
					FrameLayout.LayoutParams prm = (FrameLayout.LayoutParams) mWBCheckViewBase.getLayoutParams();
					prm.width = w;
					prm.height = h;
					prm.gravity=Gravity.CENTER;
					mWBCheckViewBase.setLayoutParams(prm);
					break;
				case PROGRESS_STEP_LOAD_JPEG:
					//ImageViewに設定
					mCapturedImageView.setImageBitmap(mPicBitmap);
					break;
				case PROGRESS_STEP_DETECT_LINE:
					break;
				case PROGRESS_STEP_DETECT_WB:
					break;
					
			}
		}
		
		@Override
		protected ArrayList<Point> doInBackground(Void... arg0) {
			// TODO Auto-generated method stub
			//jpegロード
			final String fn = mFilePath + mFileName;
			mPicBitmap = loadJpeg(fn, mScreenWidth, mScreenHeight);
			if(mPicBitmap == null) return null;
			//ロード時のサンプリングの関係でjpegサイズがスクリーンサイズに一致しないので再度リサイズする
			int w = mPicBitmap.getWidth();
			int h = mPicBitmap.getHeight();
		    Mat pic = new Mat(w, h, CvType.CV_8UC4);
		    Utils.bitmapToMat(mPicBitmap, pic);
			final double aspectScreen = (double)mScreenWidth/(double)mScreenHeight;
			final double aspectPic = (double)w/(double)h;
			if(aspectPic >= aspectScreen){//Picのほうが横長->幅を合わせる
				final double resize = (double)mScreenWidth/(double)w;
				h = (int)((double)h*resize + 0.5);
				w = mScreenWidth;
			}
			else{
				final double resize = (double)mScreenHeight/(double)h;
				w = (int)((double)w*resize + 0.5);
				h = mScreenHeight;
			}
			//画像サイズが4の倍数になるように
			w = ((w >> 2) + 1) << 2;
			h = ((h >> 2) + 1) << 2;
			publishProgress(PROGRESS_STEP_JPEGSIZE_DECIDED, w, h);

			if(MyDebug.DEBUG) Log.d(LOG_TAG, "screen size  = " + w + ":" + h);
			//リサイズ
			Imgproc.resize(pic, pic, new Size(w, h));
			mPicBitmap.recycle();
			mPicBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(pic, mPicBitmap);
			//jpeg表示
			publishProgress(PROGRESS_STEP_LOAD_JPEG);
			//線分検出
			DetectLines lineDetector = new DetectLines(w, h, 4);
		    int lines[] = new int[DetectLines.MAX_LINE_NUM*5];// x5は 始点のｘ，ｙ　終点のx, y、太さの５つ
		    int lineNum = lineDetector.lineDetect(pic, false,  null, lines);
		    lineDetector.cleanUp();
		    pic.release();
		    pic = null;
		    //線分検出完了表示
		    publishProgress(PROGRESS_STEP_DETECT_LINE);
			//WBエリア計算
			boolean wbResult = true;
			ArrayList<Point> points = null;
			if(lineNum < 4){
				wbResult = false;
			}
			else{//４本以上線分が見つかったときはWBエリアを計算
				WhiteBoardDetect wbDetect = new WhiteBoardDetect(w, h);
				points = new ArrayList<Point>();
				wbResult = wbDetect.detectWhiteBoard(lines, lineNum, points, null);
				publishProgress(PROGRESS_STEP_DETECT_WB);

			}
			if(!wbResult){//WBエリアが見つからなかった時

				if(mPreviewPoints == null){//previewでもWBエリアが見つかっていないときは諦める
					points = null;
				}
				else{//PreviewでのWBエリアを最終画像でのエリアに変換する
					points = new ArrayList<Point>();
					for(int i = 0;i < 4;i++){
						Point p = new Point();
						p.x = (double)mPreviewPoints[i*2 + 0]*(double)w/(double)mPrevWidth;
						p.y = (double)mPreviewPoints[i*2 + 1]*(double)h/(double)mPrevHeight;
						points.add(p);
					}
				}
			}
			
			if(MyDebug.DEBUG && points != null){
				for(int i = 0;i < 4;i++) Log.d(LOG_TAG, "corners(" + i + ")" + points.get(i).x + ":" + points.get(i).y);
			}
			//WBエリアが見つかっていないときはnullが返る
			return points;
		}
	}
}
