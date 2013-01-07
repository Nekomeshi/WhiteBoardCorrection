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

import com.nekomeshi312.cameraandparameters.CameraAndParameters;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

public class CameraSettingActivity extends PreferenceActivity {
	private static final String LOG_TAG = "CameraSettingActivity";
	private CameraAndParameters mCameraSetting;
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if(MyDebug.DEBUG)Log.i(LOG_TAG, "onPause");
		Log.i(LOG_TAG, "onPause : camera open = " + mCameraSetting.isCameraOpen());
		super.onPause();
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		if(MyDebug.DEBUG)Log.i(LOG_TAG, "onResume");
		super.onResume();
	}
	@Override
    public void onCreate(Bundle savedInstanceState) {
		if(MyDebug.DEBUG)Log.i(LOG_TAG, "onCreate");
		super.onCreate(savedInstanceState);
		mCameraSetting = CameraAndParameters.newInstance(this);
		Log.i(LOG_TAG, "onCreate : camera open = " + mCameraSetting.isCameraOpen());

        setPreferenceScreen(createPreferenceHierarchy());
    }
	private void setNoInfoPref(PreferenceCategory parent){
        // Toggle preference
        Preference pref = new Preference(this);
        pref.setTitle(getString(R.string.setting_no_info_pref_title));
        parent.addPreference(pref);
		
	}
    private PreferenceScreen createPreferenceHierarchy() {
        // Root
    	PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

    	//画像設定
    	int count = 0;
    	
    	//画質設定
    	count = 0;
    	PreferenceCategory prefCatQuality = new PreferenceCategory(this);
    	prefCatQuality.setTitle(getString(R.string.setting_category_title_quality));
    	root.addPreference(prefCatQuality);
    	if(null != mCameraSetting.mPictureSize && 
    			true == mCameraSetting.mPictureSize.setPreference(prefCatQuality, this)) count++;
    	if(null != mCameraSetting.mJpegQuality &&
    			true == mCameraSetting.mJpegQuality.setPreference(prefCatQuality, this)) count++;
    	if(0 == count){
    		setNoInfoPref(prefCatQuality);
    	}
        
    	//撮影設定
    	count = 0;
    	PreferenceCategory prefCatImaging= new PreferenceCategory(this);
    	prefCatImaging.setTitle(getString(R.string.setting_category_title_imaging));
    	root.addPreference(prefCatImaging);
    	if(null != mCameraSetting.mZoom &&
    			true == mCameraSetting.mZoom.setPreference(prefCatImaging, this)) count++;
    	if(null != mCameraSetting.mExposurecompensation &&
    			true == mCameraSetting.mExposurecompensation.setPreference(prefCatImaging, this)) count++;
    	if(null != mCameraSetting.mFlashMode &&
    			true == mCameraSetting.mFlashMode.setPreference(prefCatImaging, this)) count++;
    	if(null != mCameraSetting.mWhiteBalance &&
    			true == mCameraSetting.mWhiteBalance.setPreference(prefCatImaging, this)) count++;
    	if(true == mCameraSetting.mVideoStabilization.setPreference(prefCatImaging, this)) count++;
    	if(0 == count){
    		setNoInfoPref(prefCatImaging);
    	}
    	return root;
    }

	public static int getResolutionWidth(Context context){
		SharedPreferences sharedPreferences = 
			PreferenceManager.getDefaultSharedPreferences(context);   
		int resolution = sharedPreferences.getInt(context.getString(R.string.pref_resolution_key), 1024);
		return resolution;
	}
	public static void setResolutionWidth(Context context, int resolution){
		SharedPreferences sharedPreferences = 
			PreferenceManager.getDefaultSharedPreferences(context);   
	    Editor ed = PreferenceManager.getDefaultSharedPreferences(context).edit();
    	ed.putInt(context.getString(R.string.pref_resolution_key), resolution);
		ed.commit();
	}
	
	public static int getAspectWidth(Context context){
		SharedPreferences sharedPreferences = 
			PreferenceManager.getDefaultSharedPreferences(context);   
		int width = sharedPreferences.getInt(context.getString(R.string.pref_aspect_width_key), 16);
		return width;
	}
	public static void setAspectWidth(Context context, int width){
	    Editor ed = PreferenceManager.getDefaultSharedPreferences(context).edit();
    	ed.putInt(context.getString(R.string.pref_aspect_width_key), width);
		ed.commit();
	}
	public static int getAspectHeight(Context context){
		SharedPreferences sharedPreferences = 
			PreferenceManager.getDefaultSharedPreferences(context);   
		int height = sharedPreferences.getInt(context.getString(R.string.pref_aspect_height_key), 9);
		return height;
	}
	public static void setAspectHeight(Context context, int height){
	    Editor ed = PreferenceManager.getDefaultSharedPreferences(context).edit();
    	ed.putInt(context.getString(R.string.pref_aspect_height_key), height);
		ed.commit();
	}
}
