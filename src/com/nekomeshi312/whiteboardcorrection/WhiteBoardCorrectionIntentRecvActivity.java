package com.nekomeshi312.whiteboardcorrection;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

/**
 * Intentで呼ばれた時に呼び出されるActivity。Intent呼び出しをon/offできるように、親とは別のActivityを作っておく
 * @author masaki
 *
 */
public class WhiteBoardCorrectionIntentRecvActivity 
							extends WhiteBoardCorrectionActivity {

	public static void intentReceiveEnable(Context context, boolean enable){
		PackageManager pm = context.getPackageManager();
		
		if(enable){
			// 一覧に表示にする
			// 設定した瞬間にアプリケーションを終了したくないのでDONT_KILL_APPを指定
			pm.setComponentEnabledSetting(new ComponentName(context, WhiteBoardCorrectionIntentRecvActivity.class),
					PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
			if(MyDebug.DEBUG) Toast.makeText(context, "ComponentSetting ON", Toast.LENGTH_SHORT).show();
		}else{
			// 一覧に表示しない
			pm.setComponentEnabledSetting(new ComponentName(context, WhiteBoardCorrectionIntentRecvActivity.class),
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
			if(MyDebug.DEBUG) Toast.makeText(context, "ComponentSetting OFF", Toast.LENGTH_SHORT).show();
		}
	}
}
