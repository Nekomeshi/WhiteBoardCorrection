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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class PictureFolder{
	private static final int INITIAL_FOLDER_NUMBER = 100;
	private static final int INITIAL_PICTURE_NUMBER = 1;
	private static final String FOLDER_NUMBER_PREF = "FolderNumber";
	private static final String PICTURE_NUMBER_PREF = "PictureNumber";

	/**
	 * 写真の番号をカウントアップする
	 */
	public static void pictureNumberCountUp(Context context){
    	int  dataFolderNumber = INITIAL_FOLDER_NUMBER;
    	int	 pictureNumber = INITIAL_PICTURE_NUMBER;
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		dataFolderNumber = pref.getInt(FOLDER_NUMBER_PREF, INITIAL_FOLDER_NUMBER);
		pictureNumber = pref.getInt(PICTURE_NUMBER_PREF, INITIAL_PICTURE_NUMBER);

		//撮影に成功したらファイル番号をインクリメント
		if(9999 == pictureNumber){
			pictureNumber = INITIAL_PICTURE_NUMBER;
			if(999 == dataFolderNumber){
				dataFolderNumber = INITIAL_FOLDER_NUMBER;
			}
			else{
				dataFolderNumber++;
			}
		}
		else{
			pictureNumber++;
		}
		//新しいファイル番号とフォルダ番号を保存
	    Editor ed = PreferenceManager.getDefaultSharedPreferences(context).edit();
    	ed.putInt(FOLDER_NUMBER_PREF, dataFolderNumber);
    	ed.putInt(PICTURE_NUMBER_PREF, pictureNumber);
		ed.commit();
	}

	/**
	 * 写真を格納するフォルダのフルパスを作成する SDカードの /DCIM/000ABCDE/   <- 最後の "/"　付き
	 * @param context
	 * @param folderName　 フォルダ名　上記のABCDEを指定する
	 * @return　フルパス。フォルダが存在せず、かつ作成できない場合はnullを返す
	 */
	public static String createPicturePath(Context context, String folderName){
		String dataPath = null;
		File file = Environment.getExternalStorageDirectory();
    	int  dataFolderNumber = INITIAL_FOLDER_NUMBER;
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		dataFolderNumber = pref.getInt(FOLDER_NUMBER_PREF, INITIAL_FOLDER_NUMBER);
		//データフォルダ名作成
    	dataPath = file.getPath() + "/DCIM/" + String.format("%03d", dataFolderNumber) + folderName + "/";
    	//データフォルダがなければ作成する
    	File fl = new File(dataPath);
		if(true != fl.exists() && true != fl.mkdirs()){
			Toast.makeText(context, R.string.error_msg_cant_create_data_folder, Toast.LENGTH_SHORT).show();
			return null;
		}
		return dataPath;
	}
	/**
	 * 写真のjpegファイル名を作成する　DSC_0123.jpg
	 * @param context
	 * @param folderName ファイルを格納するフォル名のベース名。　createPicturePathでpathを生成し、ファイルが保存できない場合はインクリメントする
	 * @return　ファイル名　失敗した場合はnull
	 */
	public static String createPictureName(Context context, String folderName, String picName, String warpName){
    	//データファイル名を追加
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		String name;
		do{
			final int pictureNumber = pref.getInt(PICTURE_NUMBER_PREF, INITIAL_PICTURE_NUMBER);
			String path = createPicturePath(context, folderName);
			if(path == null) return null;
			name = picName + String.format("%04d", pictureNumber) + ".jpg";
			String warp = name.replaceAll(picName, warpName);
			File fl = new File(path + name);
			File flw = new File(path + warp);
			if(!fl.exists() && !flw.exists()) break;
			pictureNumberCountUp(context);
		}while(true);
    	return name;
	}
}
