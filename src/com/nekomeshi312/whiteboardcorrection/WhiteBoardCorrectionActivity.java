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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import com.nekomeshi312.cameraandparameters.CameraAndParameters;
import com.nekomeshi312.checksdlib.SDCardAccess;

import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class WhiteBoardCorrectionActivity extends SherlockFragmentActivity
					implements Camera.PictureCallback,
								CameraViewFragment.LineDetectCallback,
								WhiteBoardCheckFragment.WhiteBoardCheckCallback{


	private static final String LOG_TAG = "WhiteBoardCorrectionActivity";
	
	private static CameraAndParameters mCameraSetting;
	public static CameraAndParameters getCameraSetting(){
		return mCameraSetting;
	}

	private static final String FRAG_CAMERA_VIEW_TAG = "mFragCameraView";
	private CameraViewFragment mFragCameraView = null;
	private final String FRAG_WB_CHECK_TAG = "mBoardCheckFragment";
	private WhiteBoardCheckFragment mBoardCheckFragment = null;
	
    private PowerManager.WakeLock mWakeLock = null;

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);
    	//action bar をオーバレイ表示させる。フルsクリーン表示してstatus barを非表示
    	final android.view.Window wnd = getWindow();
        wnd.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        wnd.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.activity_white_board_correction);
        mCameraSetting = CameraAndParameters.newInstance(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        //actionbarの背景を設定（半透明のグラデーション)
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_background));
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        
        FragmentManager fm = getSupportFragmentManager();
        mFragCameraView = (CameraViewFragment) fm.findFragmentByTag(FRAG_CAMERA_VIEW_TAG);
        mBoardCheckFragment =(WhiteBoardCheckFragment) fm.findFragmentByTag(FRAG_WB_CHECK_TAG);
    }
    
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
        mWakeLock.acquire();
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mWakeLock.release();	
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
		//viewのサイズが確定してからフラグメントを追加する
		if(hasFocus){
			if(mFragCameraView == null || mFragCameraView.isDetached()){
				mFragCameraView = new CameraViewFragment();
				FragmentManager fm = getSupportFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				ft.add(R.id.camera_view_base, mFragCameraView, FRAG_CAMERA_VIEW_TAG);
				ft.commit();
				OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
			}
		}
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getSupportMenuInflater().inflate(R.menu.activity_camera_function_check, menu);
        return true;
    }
	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragmentActivity#onOptionsItemSelected(com.actionbarsherlock.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if(MyDebug.DEBUG) Log.i(LOG_TAG, item.toString());
		switch(item.getItemId()){
			case android.R.id.home:
				if(mBoardCheckFragment != null && mBoardCheckFragment.isAdded()){
			    	FragmentManager fm = getSupportFragmentManager();
			    	fm.popBackStack();
				}
				return true;

			case R.id.menu_about:
				ShowAboutDlgTask task = new ShowAboutDlgTask();
				task.execute();
				return true;
			default:
				break;
		}
		return false;
	}
    
	private static final int SHUTTER_STATUS_IDLE = 0;
	private static final int SHUTTER_STATUS_FOCUS = 1;
	private static final int SHUTTER_STATUS_TAKEPIC = 2;
	private static final int SHUTTER_STATUS_TAKEN = 3;
	private int mShutterStatus = SHUTTER_STATUS_IDLE;
	private static final int FOCUS_STATUS_IDLE = 0;
	private static final int FOCUS_STATUS_FOCUSING = 1;
	private static final int FOCUS_STATUS_FOCUS_OK = 2;
	private int mFocusStatus = FOCUS_STATUS_IDLE;

	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
	    switch (keyCode) {
	    	case KeyEvent.KEYCODE_FOCUS:
	    		if(mShutterStatus < SHUTTER_STATUS_FOCUS){
	    			mShutterStatus = SHUTTER_STATUS_FOCUS;
	    			startAutoFocus();
	    		}
	    		return true;
	    	case KeyEvent.KEYCODE_CAMERA:
	    		if(mShutterStatus < SHUTTER_STATUS_TAKEPIC){
		    		if(mFocusStatus == FOCUS_STATUS_FOCUS_OK){
			    		mShutterStatus = SHUTTER_STATUS_TAKEPIC;
			    		takePic(mCameraSetting.getCamera());
		    		}
		    		else if (mFocusStatus == FOCUS_STATUS_FOCUSING){
			    		mShutterStatus = SHUTTER_STATUS_TAKEPIC;
		    		}
	    		}
	    		return true;
	    	default:
	    		return super.onKeyDown(keyCode, event);
	    }
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onKeyUp(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
	    switch (keyCode) {
	    	case KeyEvent.KEYCODE_FOCUS:
	    		if(mShutterStatus < SHUTTER_STATUS_TAKEN){
	    			mShutterStatus = SHUTTER_STATUS_IDLE;
	    			mFocusStatus = FOCUS_STATUS_IDLE;
	    		}
	    		return true;
	    	case KeyEvent.KEYCODE_CAMERA:
    		default:
    			return super.onKeyUp(keyCode, event);
	    }
	}
	
	@Override
	public void onShutterReleased() {
		// TODO Auto-generated method stub
		mShutterStatus = SHUTTER_STATUS_TAKEPIC;
		startAutoFocus();
		return;
	}
	
	private void startAutoFocus(){
		if(!mCameraSetting.isCameraOpen()){
			Log.w(LOG_TAG, "Camera is not open");
			return ;
		}
		//オートフォーカス中はなにもしない。
		if(mFocusStatus != FOCUS_STATUS_IDLE){
			Log.w(LOG_TAG, "Autofocus is Running");
			return ;
		}
		mFocusStatus  = FOCUS_STATUS_FOCUSING;
		Camera camera = mCameraSetting.getCamera();

		try{
			camera.autoFocus(new Camera.AutoFocusCallback() {//カメラにAFがある場合
				@Override
				public void onAutoFocus(boolean success, Camera c) {
					// TODO Auto-generated method stub
					Log.i(LOG_TAG, "Autofocus = " + success);
					if(success){
						mFocusStatus = FOCUS_STATUS_FOCUS_OK;
						takePic(c);
					}
					else{
						Toast.makeText(WhiteBoardCorrectionActivity.this, R.string.error_msg_cant_focus, Toast.LENGTH_SHORT).show();
						mFocusStatus = FOCUS_STATUS_IDLE;
					}
				}
			});
		}
		catch(Exception e){//ない場合はいきなりシャッタを切る
			e.printStackTrace();
			try{				
				mFocusStatus = FOCUS_STATUS_FOCUS_OK;
				takePic(camera);
			}
			catch(Exception e1){
				e1.printStackTrace();
				mFocusStatus = FOCUS_STATUS_IDLE;
			}
		}
	}
	private void takePic(Camera c) throws RuntimeException{
		if(mShutterStatus == SHUTTER_STATUS_TAKEPIC &&
				mFocusStatus == FOCUS_STATUS_FOCUS_OK){
			mShutterStatus = SHUTTER_STATUS_TAKEN;
			c.setPreviewCallback(null);
			c.takePicture(null, null, null, this);
		}
	}
	
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
        	if(status != LoaderCallbackInterface.SUCCESS){
        		switch(status){//とりあえずエラーメッセージの表示など
        			case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
        				Log.w(LOG_TAG, "LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION");
        				break;
        			case LoaderCallbackInterface.INIT_FAILED:
        				Log.w(LOG_TAG, "LoaderCallbackInterface.INIT_FAILD");
        				break;
        			case LoaderCallbackInterface.INSTALL_CANCELED:
        				Log.w(LOG_TAG, "LoaderCallbackInterface.CANCELED");
        				break;
        			case LoaderCallbackInterface.MARKET_ERROR:
        				Log.w(LOG_TAG, "LoaderCallbackInterface.MARKET_ERROR");
        				break;
        			default:
        				Log.w(LOG_TAG, "OpenCV Loading unknown error");
        				break;
        		}
        		//OpenCVManagerのインストールを実施した場合は、インストール完了してアクティビティがここに戻ってきた時にSUCESSが返される。
                super.onManagerConnected(status);//どうも勝手にactivityを閉じてくれるらしい。
                return;
        	}
           	Log.i(LOG_TAG, "OpenCV loaded successfully");
           	mFragCameraView.startPreview();
        }
    };

	private class ShowAboutDlgTask extends AsyncTask<Void, Void, Void>{
		private String mLicenseMessage;
		private ProgressDialog mWaitDialog;

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			mWaitDialog = new ProgressDialog(WhiteBoardCorrectionActivity.this);
			String msg = WhiteBoardCorrectionActivity.this.getString(R.string.about_dialog_wait);
			mWaitDialog.setMessage(msg);
		    // 円スタイル（くるくる回るタイプ）に設定します
		    mWaitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		    mWaitDialog.setCancelable(false);
		    // プログレスダイアログを表示
		    mWaitDialog.show();
			super.onPreExecute();
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onCancelled()
		 */
		@Override
		protected void onCancelled() {
			// TODO Auto-generated method stub
			try{
				if(mWaitDialog != null) mWaitDialog.dismiss();
			}
			catch(IllegalArgumentException e){//親のactivityが閉じてしまっている場合のエラーを無視する
				e.printStackTrace();
			}
			mWaitDialog = null;
			super.onCancelled();
		}
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			DialogFragment dlgFragment = AboutDlgFragment.newInstance(mLicenseMessage);
			dlgFragment.show(getSupportFragmentManager(), "dialog");
			try{
				if(mWaitDialog != null) mWaitDialog.dismiss();
			}
			catch(IllegalArgumentException e){//親のactivityが閉じてしまっている場合のエラーを無視する
				e.printStackTrace();
			}
			mWaitDialog = null;

			super.onPostExecute(result);
		}
		

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			mLicenseMessage = loadAGPLLicenseFile();
	        if(mLicenseMessage == null) mLicenseMessage = "error";
			return null;
		}
	}
	
    public static class AboutDlgFragment extends SherlockDialogFragment {

    	public static AboutDlgFragment newInstance(String licenseMessage) {
        	AboutDlgFragment frag = new AboutDlgFragment();
            Bundle args = new Bundle();
            args.putString("license", licenseMessage);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String license = getArguments().getString("license");
            LayoutInflater inflater = getActivity().getLayoutInflater();  
            View view = inflater.inflate(R.layout.dialog_about, null, false);  
            TextView licenseMessage = (TextView)view.findViewById(R.id.license_message);
           	licenseMessage.setText(license);

           	String verInfo = null;
           	String appName = null;
            try {
            	final PackageManager pm = getSherlockActivity().getPackageManager();
            	final String pn = getSherlockActivity().getPackageName();
    			PackageInfo pi = pm.getPackageInfo(pn, PackageManager.GET_ACTIVITIES);
    			ApplicationInfo ai = pi.applicationInfo;
    			verInfo = pi.versionName;
    			appName = (String) pm.getApplicationLabel(ai);
    		} catch (NameNotFoundException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
            if(verInfo == null) verInfo = "unknown";
            if(appName == null) appName = "unknown";
            String dlURL = getSherlockActivity().getString(R.string.about_download_url);
            String aboutMsg = String.format(getSherlockActivity().getString(R.string.about_copyright), appName, verInfo, dlURL);
           	TextView copyRightInfo = (TextView)view.findViewById(R.id.app_copyright_info);
           	copyRightInfo.setText(aboutMsg);
           	
            // 独自のタブ用レイアウト生成
            View tabView;
            TextView label;
            
            //tabの表示内容設定
            TabHost host = (TabHost) view.findViewById(R.id.about_tab_host);
            if(host == null){
            }
            else{
                LayoutInflater linf = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                host.setup();
                
                TabSpec tabAbout = host.newTabSpec("tab_about_msg");
                tabView = (View)linf.inflate(R.layout.tab_view, null);
                label = (TextView) tabView.findViewById(R.id.tab_label);
                label.setText(R.string.about_tab_app);
                tabAbout.setIndicator(tabView);
                tabAbout.setContent(R.id.tab_about_msg);
                host.addTab(tabAbout);

                TabSpec tabLicense = host.newTabSpec("tab_agpl_license");
                tabView = (View)linf.inflate(R.layout.tab_view, null);
                label = (TextView) tabView.findViewById(R.id.tab_label);
                label.setText(R.string.about_tab_license);
                tabLicense.setIndicator(tabView);
                tabLicense.setContent(R.id.tab_agpl_license);
                host.addTab(tabLicense);
            }
           	
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.menu_about)
                    .setPositiveButton("OK", null);
            builder.setView(view);
            return builder.create();
        }
    }
    /**
     * AGPLライセンス文言をassetsから読み込む
     * @return　文言　読み込み失敗時はnullを返す
     */
    private String loadAGPLLicenseFile(){
    	String result = null;
    	try {
   		  	InputStream fileInputStream = getAssets().open("txt" + "/" + "agpl-3.0.txt");
   		  	BufferedReader in = new BufferedReader(new InputStreamReader(fileInputStream, "UTF-8"));
   		  	String readString;
   		  	while ((readString= in.readLine()) != null) {
   		  		readString += "\n";
   		  		if(result == null){
   		  			result = readString;
   		  		}
   		  		else{
   		  			result += readString;
   		  		}
   		  	}
    	}
   	  	catch (FileNotFoundException e) {
   	  		e.printStackTrace();
   	  		result = null;
   	  	}
    	catch (IOException e) {
    		e.printStackTrace();
   	  		result = null;
   	  	}
		return result;
    }

    
	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		Log.i(LOG_TAG, "onPictureTaken");

		String path = null;
		String name = null;
		if(null != data){
			if(MyDebug.DEBUG) Log.d(LOG_TAG, "Captured:size = " + data.length);
			int sdErrorID = SDCardAccess.checkSDCard(this);
			if(0 != sdErrorID){//SDカードが刺さっていないとき
				Toast.makeText(this, 
			    		sdErrorID, 
			            Toast.LENGTH_LONG).show();
			}
			else{
				String fn = null;
				final String folderBase = getString(R.string.picture_folder_base_name);
				final String filenameBase = getString(R.string.picture_base_name);
				name = PictureFolder.createPictureName(this, folderBase, filenameBase);
				path = PictureFolder.createPicturePath(this, folderBase);
				if(name != null && path != null){
					fn = path + name;
					if(MyDebug.DEBUG) Log.d(LOG_TAG, "Picture file name = " + fn);
					
					FileOutputStream fileOutputStream = null;
					try {
						fileOutputStream = new FileOutputStream(fn);
						fileOutputStream.write(data);
					}
					catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						name = null;
					}
					finally{
						if(null != fileOutputStream){
							try {
								fileOutputStream.flush();
								fileOutputStream.close();
							} 
							catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								name = null;
							}
						}
					}
				}
			}
		}
		mFocusStatus = FOCUS_STATUS_IDLE;
		mShutterStatus = SHUTTER_STATUS_IDLE;
		
		if(null == name){
			mFragCameraView.startPreview();//下のpictureTakenで、撮影結果を表示するactivityを表示するため、その時にカメラが造り直されるのでここでstartPreviewする必要なし
			return;
		}
		mFragCameraView.stopPreview();//バッファメモリを解放するためとりあえず実行しておく。なくてもいいかも。
		//fragmentをチェックfragmentに切り替える
		final int width = camera.getParameters().getPictureSize().width;
		final int height = camera.getParameters().getPictureSize().height;
		final int prevWidth = camera.getParameters().getPreviewSize().width;
		final int prevHeight = camera.getParameters().getPreviewSize().height;

		
		//保存したjpge画像をギャラリーに登録
		//MediaScannerConnection.scanFileを使ったほうが簡単だけど、DISPLAY_NAMEとかWIDTHとか全端末で
		//ほんとにちゃんと入れてくれるか心配なので、あえてこっちを使う
		ContentResolver cr = getContentResolver();
		ContentValues values = new ContentValues();  
		values.put(Images.Media.TITLE, name);  
		values.put(Images.Media.DISPLAY_NAME, name);  //<- ギャラリーから画像を読み出すときはこのファイル名を使う
		values.put(Images.Media.MIME_TYPE, "image/jpeg");  
		values.put(Images.Media.DATA, path+name);  
		values.put(Images.Media.WIDTH, width);
		values.put(Images.Media.HEIGHT, height);
		cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);  	
		
		transitToBoardCheckFragment(path, name,
									width, height,
									prevWidth, prevHeight,
									mDetectedPoints);
	}

	@Override
	public void onJpegFileSelected(String name, int width, int height) {
		// TODO Auto-generated method stub
		final String folderBase = getString(R.string.picture_folder_base_name);
		String path = PictureFolder.createPicturePath(this, folderBase);
		transitToBoardCheckFragment(path, name,
									width, height,
									0, 0,
									null);
	}    

	private void transitToBoardCheckFragment(String path, String name,
											int width, int height,
											int prevWidth, int prevHeight,
											ArrayList<Point>detectedPoints){

		if(mBoardCheckFragment != null && mBoardCheckFragment.isAdded()) return;
		mBoardCheckFragment = WhiteBoardCheckFragment.newInstance(FRAG_WB_CHECK_TAG, 
																path, 
																name, 
																width, 
																height,
																prevWidth, 
																prevHeight, 
																detectedPoints);
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.camera_view_base, mBoardCheckFragment, FRAG_WB_CHECK_TAG);
		// Fragmentの変化時のアニメーションを指定
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.addToBackStack(null);
		ft.commit();
	}
	private ArrayList<Point> mDetectedPoints = null;
	@Override
	public void onLineDetected(ArrayList<Point> points) {
		// TODO Auto-generated method stub
		mDetectedPoints = points;
	}

	@Override
	public void onWhiteBoardCorrectionCompleted() {
		// TODO Auto-generated method stub
		FragmentManager fm = getSupportFragmentManager();
    	fm.popBackStack();	
	}
}
