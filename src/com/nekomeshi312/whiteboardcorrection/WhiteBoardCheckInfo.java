package com.nekomeshi312.whiteboardcorrection;

import java.util.ArrayList;

import org.opencv.core.Point;

public class WhiteBoardCheckInfo {
	public int mPicWidth = 0;
	public int mPicHeight = 0;
	public int mPrevWidth = 0;
	public int mPrevHeight = 0;
	public boolean mIsCaptured = true;
	public String mFilePath = null;
	public String mFileName = null;
	public ArrayList<Point> mDetectedPoints = null;
	
	public WhiteBoardCheckInfo(){
		resetInfo();
	}
	public void resetInfo(){
		mPicWidth = 0;
		mPicHeight = 0;
		mPrevWidth = 0;
		mPrevHeight = 0;
		mIsCaptured = true;
		mFilePath = null;
		mFileName = null;
		mDetectedPoints = null;
	}
}
