package com.nekomeshi312.whiteboardcorrection;

import java.io.File;
import java.util.ArrayList;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateBeamUrisCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.type.Note;
import com.nekomeshi312.whiteboardcorrection.MyUtils.ProgressDialogFrag;

public class WhiteBoardResultFragment extends SherlockFragment{

	public interface WhiteBoardResultCallback{
		public void onWhiteBoardResultCompleted();
		public void onWhiteBoardResultCanceled();
	}
	private static final String LOG_TAG = "WhiteBoardResultFragment";

	private Activity mParentActivity;
	private static final String TAG_FRAGMENT = "tag";
	private static final String TAG_WARP_NAME = "warpname";
	
	private int mScreenWidth;
	private int mScreenHeight;
	private Bitmap mPicBitmap = null;

	private ImageView mWarpedImageView = null;
	private RelativeLayout mResultViewBase = null;
	
	private String mWarpName = null;
	private boolean mArgsOK = true;
	private String mMyTag = null;

    private static final int BEAM_MESSAGE_SENT = 1;
    private static final int MAIL_MESSAGE_SENT = 2;
    private EvernoteCtrl mEvernoteCtrl;

	public static WhiteBoardResultFragment newInstance(String tag, 
														String warpName) {
		WhiteBoardResultFragment frag = new WhiteBoardResultFragment();
		Bundle args = new Bundle();
		args.putString(TAG_WARP_NAME, warpName);
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
		if(!(activity instanceof WhiteBoardResultCallback)){
			Log.e(LOG_TAG, "Parent is not instance of WhiteBoardResultCallback");
			mParentActivity = null;
			return;
		}
		mParentActivity = activity;
		mEvernoteCtrl = new EvernoteCtrl(activity);
	}

	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragment#onDetach()
	 */
	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		super.onDetach();
		mParentActivity = null;
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_result, container, false);
        final Bundle args = getArguments();
        mArgsOK = true;
        if(args == null){
        	mArgsOK = false;
        }
        else{
    		mMyTag = args.getString(TAG_FRAGMENT);
            mWarpName = args.getString(TAG_WARP_NAME);
            
            if(MyDebug.DEBUG){
            	Log.d(LOG_TAG, "WarpName = " + mWarpName);
            }
        }
        ActionBar actionBar =  ((SherlockFragmentActivity) mParentActivity).getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		
		
		
		mResultViewBase = (RelativeLayout)root.findViewById(R.id.area_result_view_base);
		mWarpedImageView = (ImageView)root.findViewById(R.id.warped_image);
		final View viewBase = (View)mParentActivity.findViewById(R.id.camera_view_base);
		mScreenWidth = viewBase.getWidth();
		mScreenHeight = viewBase.getHeight();

		LoadJpegFAsyncTask loadJpeg = new LoadJpegFAsyncTask();
		loadJpeg.execute();
		
		Button okButton = (Button)root.findViewById(R.id.button_result_ok);
		okButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(mParentActivity == null){
					Log.w(LOG_TAG, "ParentActivity == null");
					return;
				}
				WhiteBoardResultCallback cb = (WhiteBoardResultCallback)mParentActivity;
				cb.onWhiteBoardResultCompleted();
			}
		});
		Button cancelButton = (Button)root.findViewById(R.id.button_result_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(mParentActivity == null){
					Log.w(LOG_TAG, "ParentActivity == null");
					return;
				}
				WhiteBoardResultCallback cb = (WhiteBoardResultCallback)mParentActivity;
				cb.onWhiteBoardResultCanceled();
			}
		});
		return root;
	}
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BEAM_MESSAGE_SENT:
                Toast.makeText(mParentActivity, "Message sent!", Toast.LENGTH_LONG).show();
                break;
            }
        }
    };

	
	private class LoadJpegFAsyncTask extends AsyncTask<Void, Void, Integer>{
		private static final int CANT_READ_FILE_ERROR = -1;
		private static final int LOAD_OK = 0;
		
		private ProgressDialog mLoadingDialog;
		private int mWidth;
		private int mHeight;
		

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
			if(result == CANT_READ_FILE_ERROR){
			}
			else{
				FrameLayout.LayoutParams prm = (FrameLayout.LayoutParams) mResultViewBase.getLayoutParams();
				prm.width = mWidth;
				prm.height = mHeight;
				prm.gravity=Gravity.CENTER;
				mResultViewBase.setLayoutParams(prm);
				mWarpedImageView.setImageBitmap(mPicBitmap);
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
			String msg = mParentActivity.getString(R.string.wait_a_minute);
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
		}
		
		@Override
		protected Integer doInBackground(Void... arg0) {
			// TODO Auto-generated method stub
			//jpegロード
			mPicBitmap = MyUtils.loadJpeg(mWarpName, mScreenWidth, mScreenHeight);
			if(mPicBitmap == null){
				return CANT_READ_FILE_ERROR;
			}
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

			mWidth = w;
			mHeight = h;
			if(MyDebug.DEBUG) Log.d(LOG_TAG, "screen size  = " + w + ":" + h);
			//リサイズ
			Imgproc.resize(pic, pic, new Size(w, h));
			mPicBitmap.recycle();
			mPicBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(pic, mPicBitmap);
			//jpeg表示
			return LOAD_OK;
		}
	}


	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragment#onCreateOptionsMenu(com.actionbarsherlock.view.Menu, com.actionbarsherlock.view.MenuInflater)
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO Auto-generated method stub
    	inflater.inflate(R.menu.fragment_result, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}


	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragment#onPrepareOptionsMenu(com.actionbarsherlock.view.Menu)
	 */
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuItem beam = menu.findItem(R.id.menu_beam);
		MenuItem beamSetup = menu.findItem(R.id.menu_beam_setup);
		//beamが使えない端末ではメニューを出さない
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN){
			beamSetup.setVisible(false);
			beam.setVisible(false);
		}
		else if (NfcAdapter.getDefaultAdapter(mParentActivity) == null) {
			beamSetup.setVisible(false);
			beam.setVisible(false);
        } 
		super.onPrepareOptionsMenu(menu);
	}


	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragment#onOptionsItemSelected(com.actionbarsherlock.view.MenuItem)
	 */
	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
        SherlockFragmentActivity activity = (SherlockFragmentActivity)mParentActivity;
        switch (item.getItemId()) {
        	case R.id.menu_beam://API level 16未満は　onPrepareOptionMenuでこのメニューが表示されなくなっている
        		if(!NfcAdapter.getDefaultAdapter(mParentActivity).isEnabled()){//NFCがoff
        			Toast.makeText(mParentActivity, R.string.nfc_switch_off, Toast.LENGTH_SHORT).show();
            		Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            		startActivity(intent);
            		return true;
        		}
        		else if(!NfcAdapter.getDefaultAdapter(mParentActivity).isNdefPushEnabled()){//Beamがoff これはAPI level 16以上
        			Toast.makeText(mParentActivity, R.string.beam_switch_off, Toast.LENGTH_SHORT).show();
            		Intent intent = new Intent(Settings.ACTION_NFCSHARING_SETTINGS);
            		startActivity(intent);
            		return true;
        		}
                BeamDialogFragment dlg = BeamDialogFragment.newInstance(mMyTag, mWarpName);
                dlg.show(activity.getSupportFragmentManager(), "dialog");
                return true;
        	case R.id.menu_beam_setup:
        		Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
        		startActivity(intent);
        		return true;
        	case R.id.menu_evernote:
        		if(!mEvernoteCtrl.isLoggedIn()){
        			Toast.makeText(mParentActivity, R.string.evernote_not_loggedin, Toast.LENGTH_SHORT).show();
            		Intent i = new Intent(mParentActivity, EvernoteSettingActivity.class);
            		startActivity(i);
            		return true;
        		}
        		File f = new File(mWarpName);
        		final String path = f.getParent();
        		final String name = f.getName();
        		if(path == null || name == null){
           			Log.e(LOG_TAG, "wrap name is invalid");
           			Toast.makeText(mParentActivity, R.string.error_msg_warp_file_not_found, Toast.LENGTH_SHORT).show();
           			return true;
        		}
        		final ProgressDialogFrag progressDlg = ProgressDialogFrag.newInstance(mParentActivity.getString(R.string.wait_a_minute),  
        																				mParentActivity.getString(R.string.evernote_note_saving_in_progress));
        		progressDlg.show(activity.getSupportFragmentManager(), "progressDialog");
        		ArrayList<String> tags = EvernoteSettingActivity.getTags(mParentActivity);
        		String notebook = EvernoteSettingActivity.getNotebook(mParentActivity);
        		mEvernoteCtrl.saveImage(path,
        								name,
        								null,
        								"image/jpeg",
        								tags,
        								notebook,
        								new OnClientCallback<Note>(){
        									@Override
											public void onSuccess(Note data) {
												// TODO Auto-generated method stub
												Toast.makeText(mParentActivity, R.string.evernote_note_saving_complete, Toast.LENGTH_SHORT).show();
												progressDlg.dismiss();
											}

											@Override
											public void onException(
													Exception exception) {
												// TODO Auto-generated method stub
									            Log.e(LOG_TAG, "Error saving note", exception);
									            Toast.makeText(mParentActivity, R.string.error_msg_evernote_saving_note, Toast.LENGTH_LONG).show();
												progressDlg.dismiss();
											}
        							});

        		return true;
        	case R.id.menu_evernote_setup:
        		Intent i = new Intent(mParentActivity, EvernoteSettingActivity.class);
        		startActivity(i);
        		return true;
        	case R.id.menu_mail:
				final Uri uri = Uri.fromFile(new File(mWarpName));
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");
				emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, 
    										getString(R.string.e_mail_subject));
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, 
    										getString(R.string.e_mail_body));
				startActivityForResult(Intent.createChooser(emailIntent, 
    						getString(R.string.e_mail_chooser)), MAIL_MESSAGE_SENT);					
        				
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }
	}
	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode){
			case MAIL_MESSAGE_SENT:
				Toast.makeText(mParentActivity, R.string.e_mail_completed, Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
		}
	}
	
		 
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public static class BeamDialogFragment extends SherlockDialogFragment {
		private static final String KEY_TAG = "tag";
		private static final String KEY_WARP_NAME = "warp_name";
    	private NfcAdapter mNfcAdapter = null;
    	private Activity mParentActivity;
    	private String mWarpName;
    	private Handler mHandler;
    	private boolean mIsBeamComplete = false;
    	
    	public static BeamDialogFragment newInstance(String tag, String warpName) {
    		BeamDialogFragment frag = new BeamDialogFragment();
            Bundle args = new Bundle();
            args.putString(KEY_TAG, tag);
            args.putString(KEY_WARP_NAME, warpName);
            frag.setArguments(args);
            return frag;
        }
		
		public Dialog onCreateDialog(Bundle savedInstanceState) {
        	final String tag = getArguments().getString(KEY_TAG);
        	mWarpName = getArguments().getString(KEY_WARP_NAME);
			mParentActivity = (WhiteBoardCorrectionActivity)getSherlockActivity();

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.beam_message);
			builder.setTitle(R.string.beam_title);
			builder.setNegativeButton(R.string.beam_cancel,  new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
				}
			});
			beamSetup();
			Dialog dlg = builder.create();
			mHandler = new Handler();
			return dlg;
		}
    	/* (non-Javadoc)
		 * @see android.support.v4.app.DialogFragment#onDismiss(android.content.DialogInterface)
		 */
		@Override
		public void onDismiss(DialogInterface dialog) {
			// TODO Auto-generated method stub
			super.onDismiss(dialog);
	        beamStop();
	        if(mIsBeamComplete){
	        	Toast.makeText(mParentActivity, R.string.beam_complete_message, Toast.LENGTH_SHORT).show();
	        }
		}
		
		private void beamSetup(){
	       	if(MyDebug.DEBUG) Log.d(LOG_TAG, "NFC OK");
	       	mNfcAdapter = NfcAdapter.getDefaultAdapter(mParentActivity);
	       	mNfcAdapter.setOnNdefPushCompleteCallback(new OnNdefPushCompleteCallback(){
				@Override
				public void onNdefPushComplete(NfcEvent event) {
					// TODO Auto-generated method stub
			        mHandler.post(new Runnable(){
						@Override
						public void run() {
							// TODO Auto-generated method stub
							mIsBeamComplete = true;
					        dismiss();
						}
			        });
				}
	       	},  mParentActivity);
	        mNfcAdapter.setNdefPushMessageCallback(null, mParentActivity);
	        mNfcAdapter.setBeamPushUrisCallback(new CreateBeamUrisCallback(){

	        	@Override
				public Uri[] createBeamUris(NfcEvent event) {
					// TODO Auto-generated method stub
					File warp = new File(mWarpName);
					Uri[] uri = new Uri[] { Uri.fromFile(warp)};
					return uri;
				};
	        }, mParentActivity);
		}

		private void beamStop(){
	        mNfcAdapter.setBeamPushUrisCallback(null, mParentActivity);
	       	mNfcAdapter.setOnNdefPushCompleteCallback(null, mParentActivity);
	       	mNfcAdapter = null;
		}
		
	}
}
