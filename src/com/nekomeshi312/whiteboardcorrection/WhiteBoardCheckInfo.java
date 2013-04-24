package com.nekomeshi312.whiteboardcorrection;

import java.util.ArrayList;

import org.opencv.core.Point;

public class WhiteBoardCheckInfo {
	public int mPicWidth;
	public int mPicHeight;
	public int mPrevWidth;
	public int mPrevHeight;
	public String mFilePath;
	public String mFileName;
	public ArrayList<Point> mDetectedPoints;
}
