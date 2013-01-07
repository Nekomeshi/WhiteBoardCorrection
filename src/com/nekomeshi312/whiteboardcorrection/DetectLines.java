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

import org.opencv.core.Mat;

import android.util.Log;

public class DetectLines {
	private static final String LOG_TAG = "DetectLines";
	public static final int MAX_LINE_NUM = 500;
	boolean mIsInit = false;

	public DetectLines(int width, int height, int scale){
		initLines(width, height, scale, MAX_LINE_NUM);
		mIsInit = true;
	}
	
	public int lineDetect(Mat inputImg, boolean isYUV, Mat outputImg, int lines[]){
		if(!mIsInit) return -1;
	    return detectLines (inputImg.getNativeObjAddr(), 
	    					isYUV, 
	    					outputImg == null ? 0:outputImg.getNativeObjAddr(), 
	    					lines);		
	}
	public void cleanUp(){
		if(!mIsInit) return;
		cleanupLines();
		mIsInit = false;
	}

	static {
		try{
	        System.loadLibrary("LineDetect");
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
			throw e;
		}
    } 
	/**
	 * 
	 * @param inputImg 入力画像のポインタ
	 * @param isYUV　入力画像がYUVの場合はtrue RGBの場合はfalse
	 * @param outputImg RGB画像のポインタ 0以外を与えるとYUV->RGB変換した画像がここに格納される
	 * @param lines 検出された線分の座標　StartX, StartY, EndX, EndY, Width ・・・の順に格納される
	 * @return 検出された線分の本数
	 */
    native private int detectLines (long inputImg, boolean isYUV, long outputImg, int lines[]);
    /**
     * 直線検出の初期化
     * @param width　　画像の幅
     * @param height　画像の高さ
     * @param scale　線分検出をする際に縮小する係数　2->半分 4->1/4に縮小する　線分検出法がHoughの時は無効
     * @param maxLineNum　検出される線分の最大数
     */
    native private void initLines(int width, int height, int scale, int maxLineNum);
    /**
     * 線分検出のクリーンアップ
     */
    native private void cleanupLines();

}
