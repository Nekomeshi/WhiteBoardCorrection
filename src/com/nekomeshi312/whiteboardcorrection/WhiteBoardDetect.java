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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.util.Log;


public class WhiteBoardDetect {
	private static final String LOG_TAG = "WhiteBoardDetect";
	static final int ANGLE_DIV_NUM = 30;
	static final double DIV_ANGLE = Math.PI/(double)ANGLE_DIV_NUM;

	private int mViewWidth;
	private int mViewHeight;
	private int mCenterX;
	private int mCenterY;
	ArrayList<LineInfo> mLineInfo = new ArrayList<LineInfo>();
	
	public WhiteBoardDetect(int width, int height){
		mViewWidth = width;
		mViewHeight = height;
		mCenterX = (width >> 1);
		mCenterY = (height >> 1);
	}
	
	/**
	 * 線分の角度で２つのクラスに分ける。しきい値は判別分析法で。結果は各線分のLineInfoクラスのmLineInfo.mLocationFlgに書き込まれる
	 * @param lines　画像から検出された元線分データ
	 * @return true:成功　false:失敗
	 */
	private boolean divByAngle(int lines[], int lineNum, Mat img){
		double angleHist[] = new double[ANGLE_DIV_NUM];
		for(int i = 0;i < ANGLE_DIV_NUM;i++) angleHist[i] = 0.0;
		
		for (int i = 0; i < lineNum; i++){
			int i5 = i*5;
			LineInfo li = new LineInfo();
			li.setLineInfo(	(double)lines[i5+0], 
							(double)lines[i5+1], 
							(double)lines[i5+2], 
							(double)lines[i5+3], 
							(double)lines[i5+4]);
			if(!li.mIsOK) continue;//点は排除
			final int anglePos = (int)(li.mAngle/DIV_ANGLE);
			angleHist[anglePos] += li.mLength;
			mLineInfo.add(li);
		}

		//角度について判別分析法でしきい値を決める。0～PI 三値
		//判別分析方については　http://imagingsolution.blog107.fc2.com/blog-entry-113.html
		if(MyDebug.DEBUG){
			for(int i = 0;i < ANGLE_DIV_NUM;i++){
				Log.d(LOG_TAG, "hist(" + i + ") = " + angleHist[i]);
			}
		}
		int thres1 = 0;
		int thres2 = 0;
		double integral[] = new double[ANGLE_DIV_NUM];
		double average[] = new double[ANGLE_DIV_NUM];
		integral[0] = angleHist[0];
		average[0] = 0.0;
		for(int i = 1;i < ANGLE_DIV_NUM;i++){
			integral[i] = integral[i - 1] + angleHist[i];
			average[i] = average[i - 1] + angleHist[i]*(double)i;
		}
		final double allNum = integral[ANGLE_DIV_NUM - 1];
		final double allAvg = average[ANGLE_DIV_NUM - 1];
		double maxVal = -1;
		for(int t1 = 0;t1 < ANGLE_DIV_NUM;t1++){
			for(int t2 = t1+1;t2 < ANGLE_DIV_NUM;t2++){
			    // 指定閾値より小さい値の領域
				final double n1 = integral[t1];  // 画素数
				final double n2 = integral[t2] - integral[t1];  // 画素数
				final double n3 = allNum - integral[t2];  // 画素数
				final double m1 = n1 == 0 ? 0.0:average[t1]/n1;
				final double m2 = n2 == 0 ? 0.0:(average[t2] - average[t1])/n2;
				final double m3 = n3 == 0 ? 0.0:(allAvg - average[t2])/n3;
				final double subm1m2 = m1 - m2;
				final double subm2m3 = m2 - m3;
				final double subm3m1 = m3 - m1;
				final double val = n1*n2*subm1m2*subm1m2 + n2*n3*subm2m3*subm2m3 + n3*n1*subm3m1*subm3m1;
				if(val > maxVal){
					maxVal = val;
					thres1 = t1;
					thres2 = t2;
				}
			}
		}
		int angle0Count = 0;
		int angle1Count = 0;

		//各クラスごとに線分を描画(debug)
		Scalar color[] = new Scalar[2];
		color[0] = new Scalar(0xff, 0x00, 0x80);
		color[1] = new Scalar(0x00, 0xff, 0x80);

		
		for(LineInfo li:mLineInfo){
			final int pos = (int)(li.mAngle/DIV_ANGLE);
			if(pos > thres1 && pos <= thres2){
				li.mLocationFlg =  LineInfo.ANGLE0;
				angle0Count++;
			}
			else{
				li.mLocationFlg =  LineInfo.ANGLE1;
				angle1Count++;
			}

/*			if(img != null){
				if(li.mLocationFlg == LineInfo.ANGLE0){
				    Core.line(img, li.mStart, li.mEnd, color[0], 5);
				}
				else{
				    Core.line(img, li.mStart, li.mEnd, color[1], 5);
				}
			}
*/	
		}
		if(angle0Count == 0 || angle1Count == 0) return false;
		return true;
	}
	/**
	 * 分けられた各角度に対して、位置で２つのクラスに分ける。しきい値は判別分析法で。結果は各線分のLineInfoクラスのmLineInfo.mLocationFlgに書き込まれる
	 * @param img　結果を書き込む画像。nullなら書き込まない。
	 * @return true:成功　false:失敗
	 */
	private boolean divByIntersept(Mat img){
		ArrayList<LineInfo> []lineArray = new ArrayList[2];
		for(int i = 0;i < 2;i++) lineArray[i] = new ArrayList<LineInfo>();
		double [] minSectionX = {Double.MAX_VALUE, Double.MAX_VALUE};
		double [] maxSectionX = {-Double.MAX_VALUE, -Double.MAX_VALUE};
		int [] sectionNumX = {0, 0};
		int [] sectionSteepNumX = {0, 0};
		double [] minSectionY = {Double.MAX_VALUE, Double.MAX_VALUE};
		double [] maxSectionY = {-Double.MAX_VALUE, -Double.MAX_VALUE};
		int [] sectionNumY = {0, 0};
		int [] sectionSteepNumY = {0, 0};
	
		//傾きで２つのクラスに線分を分けつつ切片の最大値・最小値を計算・・・切片を使って、位置で傾きのクラスごとに２つのクラスに分けるため
		for(LineInfo li:mLineInfo){
			final int angleNo = li.mLocationFlg == LineInfo.ANGLE0 ? 0:1;

			if(li.mSectionX != Double.MAX_VALUE){//X切片が計算されている場合
				if(li.mSectionX > maxSectionX[angleNo]) maxSectionX[angleNo] = li.mSectionX;
				if(li.mSectionX < minSectionX[angleNo]) minSectionX[angleNo] = li.mSectionX;
				sectionNumX[angleNo]++;
			}
			if(li.mSectionY != Double.MAX_VALUE){//Y切片が計算されている場合
				if(li.mSectionY > maxSectionY[angleNo]) maxSectionY[angleNo] = li.mSectionY;
				if(li.mSectionY < minSectionY[angleNo]) minSectionY[angleNo] = li.mSectionY;
				sectionNumY[angleNo]++;
			}
			if(li.mIsSteep){//傾きが急な場合はX切片が有利
				sectionSteepNumX[angleNo]++;
			}
			else{//傾きがゆるやかな場合はY切片が有利
				sectionSteepNumY[angleNo]++;
			}
			lineArray[angleNo].add(li);
		}
		
		boolean []useSection = new boolean[2];
		
		if(lineArray[0].size() == 0 || lineArray[1].size() == 0) return false;//縦線/横線少なくともどちらかが存在しない。	

		final int SECTION_DIV_NUM = 30;
		double sectionHist[] = new double[SECTION_DIV_NUM];
		//傾きのクラスごとに
		for(int angleNo = 0;angleNo < 2;angleNo++){
			//切片でヒストグラムを作る
			for(int i = 0;i < SECTION_DIV_NUM;i++) sectionHist[i] = 0.0;
			double minSection;
			double maxSection;
			boolean useX;
			//ヒストグラムの最大・最小を計算
			if(sectionNumX[angleNo] > sectionNumY[angleNo]){//X切片のほうが数が多い場合X切片を使う
				minSection = minSectionX[angleNo];
				maxSection = maxSectionX[angleNo];
				useX = true;
			}
			else if(sectionNumX[angleNo] < sectionNumY[angleNo]){//Y切片のほうが多い場合はY切片を使う
				minSection = minSectionY[angleNo];
				maxSection = maxSectionY[angleNo];
				useX = false;
			}
			else{//X切片、Y切片数が同じ場合
				if(sectionSteepNumX[angleNo] > sectionSteepNumY[angleNo]){//傾きが急なほうが多い場合はX切片を使う
					minSection = minSectionX[angleNo];
					maxSection = maxSectionX[angleNo];
					useX = true;
				}
				else{//そうでない場合はY切片を使う
					minSection = minSectionY[angleNo];
					maxSection = maxSectionY[angleNo];
					useX = false;
				}
			}
			useSection[angleNo] = useX;
			//切片ヒストグラム１ステップの間隔を計算
			final double sectionRange = maxSection - minSection;
			//ヒストグラム作成
			for(LineInfo li: lineArray[angleNo]){
				double section = useX ? li.mSectionX:li.mSectionY;
				if(section == Double.MAX_VALUE) continue;
				int hisPos = (int)((section - minSection)/sectionRange * (double)SECTION_DIV_NUM);
				if(hisPos == SECTION_DIV_NUM) hisPos--;//最大値の時は当てはまる
				sectionHist[hisPos]+= li.mLength;
			}
			if(MyDebug.DEBUG){
				for(int i = 0;i < SECTION_DIV_NUM;i++){
					Log.d(LOG_TAG, "hist(" + angleNo + ")[" + i + "] =" + sectionHist[i] );
				}
			}
			//判別分析法
			double integral[] = new double[SECTION_DIV_NUM];
			double average[] = new double[SECTION_DIV_NUM];
			integral[0] = sectionHist[0];
			average[0] = 0.0;
			for(int i = 1;i < SECTION_DIV_NUM;i++){
				integral[i] = integral[i-1] + sectionHist[i];
				average[i] = average[i-1] + sectionHist[i]*(double)i;
			}
			final double allNum = integral[SECTION_DIV_NUM-1];
			final double allAvg = average[SECTION_DIV_NUM-1];
			 // 各階調の値を閾値としたときの分散の計算
			double maxVal = 0;
			int thres = 0;
			for(int i = 0;i < SECTION_DIV_NUM;i++){
				final double n1 = integral[i];
				final double n2 = allNum - integral[i];
				final double m1 = n1 == 0 ? 0.0:average[i]/n1;
				final double m2 = n2 == 0 ? 0.0:(allAvg - average[i])/n2;
				final double subm2m1 = m2 - m1;
				final double val = n1*n2*subm2m1*subm2m1;
		        if (val > maxVal) {
		          maxVal = val;
		          thres = i;
		        }
			}
			if(MyDebug.DEBUG){
				Log.d(LOG_TAG, "thres = " + thres);
			}
			for(LineInfo li: lineArray[angleNo]){
				double section = useX ? li.mSectionX:li.mSectionY;
				int hisPos = (int)((section - minSection)/sectionRange * (double)SECTION_DIV_NUM);
				
				li.mLocationFlg |= (hisPos > thres) ? LineInfo.LOCAT0:LineInfo.LOCAT1;
			}
		}

		return true;
	}
	/**
	 * 傾き・位置で 2x2=4クラスに分けた各クラスから、代表線分を選択する。
	 * @param lineEq 選択された線分の直線方程式(ax+by=1) 配列は[angle][section]。
	 * @param img　結果を書き込む画像。nullなら書き込まない。
	 * @return true:成功　false:失敗
	 */
	private boolean selectLines(StraightLineEquation lineEq[][], Mat img){
		//
		//各クラスごとにarrayを作る
		ArrayList<LineInfo> [][]classifiedLines = new ArrayList[2][2];
		LineInfo[][] designateLine = new LineInfo[2][2];
		
		for(int i = 0;i < 2;i++){
			for(int j = 0;j < 2;j++){
				classifiedLines[i][j] = new ArrayList<LineInfo>();
				designateLine[i][j] = null;
			}
		}
		for(LineInfo li:mLineInfo){
			final int agl = (li.mLocationFlg & LineInfo.ANGLE0) == LineInfo.ANGLE0 ? 0:1;
			final int sec = (li.mLocationFlg & LineInfo.LOCAT0) == LineInfo.LOCAT0 ? 0:1;
			classifiedLines[agl][sec].add(li);
		}
        final float centerX = (float)(mViewWidth >> 1);
        final float centerY = (float)(mViewHeight >> 1);

        //4クラスとも線分が存在したら(普通あるはずだが一応
		if(classifiedLines[0][0].size() == 0 || classifiedLines[0][1].size() == 0 ||
				classifiedLines[1][0].size() == 0 || classifiedLines[1][1].size() == 0) return false;
		
		for(int ang = 0;ang < 2;ang++){//傾きごとに
			for(int sec = 0;sec < 2;sec++){//切片ごとに
				//直線の式 ax+by=1で aとbについて二次元ヒストグラムを作成し、最も大きいa, bを代表の直線として採用する。
				final int HIST_DIV_NUM = 50;
				final int OUTLIER_LOOPNUM = 2;
				//a, bとも、2σの線分だけ対象にする
				final double SIGMA_MUL = 1.0;
				double aveA = 0.0;
				double aveB = 0.0;
				double stdevA = 0.0;
				double stdevB = 0.0;
				double aMax = 0.0;
				double aMin = 0.0;
				double bMax = 0.0;
				double bMin = 0.0;
			
				for(int i = 0;i < OUTLIER_LOOPNUM;i++){
					aveA = 0.0;
					aveB = 0.0;
					stdevA = 0.0;
					stdevB = 0.0;
					double aveL = 0.0;
					for(LineInfo li:classifiedLines[ang][sec]){
						aveA += li.mLineEq.a * li.mLength * li.mWidth;
						aveB += li.mLineEq.b * li.mLength * li.mWidth;
						aveL += li.mLength * li.mWidth;
					}
					aveA /= aveL;
					aveB /= aveL;
					for(LineInfo li:classifiedLines[ang][sec]){
						final double aa = li.mLineEq.a - aveA;
						final double bb = li.mLineEq.b - aveB;
						stdevA += aa*aa;
						stdevB += bb*bb;
					}
					stdevA = Math.sqrt(stdevA / classifiedLines[ang][sec].size())*SIGMA_MUL;
					stdevB = Math.sqrt(stdevB / classifiedLines[ang][sec].size())*SIGMA_MUL;
					aMax = aveA + stdevA;
					aMin = aveA - stdevA;
					bMax = aveB + stdevB;
					bMin = aveB - stdevB;
					if(i < OUTLIER_LOOPNUM-1){
						ArrayList<LineInfo> tmp = new ArrayList<LineInfo>();
						for(LineInfo li:classifiedLines[ang][sec]){
							if(li.mLineEq.a > aMax) continue;
							if(li.mLineEq.a < aMin) continue;
							if(li.mLineEq.b > bMax) continue;
							if(li.mLineEq.b < bMin) continue;
							tmp.add(li);
						}
						classifiedLines[ang][sec] = tmp;
					}
				}
				if(classifiedLines[ang][sec].size() == 0) return false;
				
				final double aDiv = (aMax - aMin)/(double)HIST_DIV_NUM;
				final double bDiv = (bMax - bMin)/(double)HIST_DIV_NUM;

				LineList hist[][] = new LineList[HIST_DIV_NUM][HIST_DIV_NUM];
				for(int i = 0;i < HIST_DIV_NUM;i++){
					for(int j = 0;j < HIST_DIV_NUM;j++){
						hist[i][j] = new LineList();
					}
				}
				int linenum = 0;
				for(LineInfo li:classifiedLines[ang][sec]){
					if(li.mLineEq.a > aMax) continue;
					if(li.mLineEq.a < aMin) continue;
					if(li.mLineEq.b > bMax) continue;
					if(li.mLineEq.b < bMin) continue;
					int aPos = (int) ((li.mLineEq.a - aMin)/aDiv);
					if(aPos == HIST_DIV_NUM) aPos--;
					int bPos = (int)((li.mLineEq.b - bMin)/bDiv);
					if(bPos == HIST_DIV_NUM) bPos--;
					hist[aPos][bPos].pushLine(li);
					linenum++;
				}
				if(linenum == 0) return false;
				int maxAPos = 0;
				int maxBPos = 0;
				double maxLen = 0.0;
//				Log.e(LOG_TAG, "ang = " + ang + "  sec = " + sec);
				for(int a = 0;a < HIST_DIV_NUM;a++){
					for(int b = 0;b < HIST_DIV_NUM;b++){
						if(hist[a][b].getLineListNum() == 0){
							continue;
						}
						double len  = 0.0;
						for(LineInfo li:hist[a][b].mLineList){
							len += li.mLength;
						}
//						Log.d(LOG_TAG, "a:b = " + a*aDiv + aMin + ":" + b*bDiv + bMin + "length sum = " + len + "amax/min = " + aMax + ":" + aMin + " bmax/min = " + bMax + ":" + bMin);
						if(maxLen < len){
							maxAPos = a;
							maxBPos = b;
							maxLen = len;
						}
					}
				}
				if(linenum == 1){
					lineEq[ang][sec].a = hist[maxAPos][maxBPos].mLineList.get(0).mLineEq.a;
					lineEq[ang][sec].b = hist[maxAPos][maxBPos].mLineList.get(0).mLineEq.b;
				}
				else{
					lineEq[ang][sec].a = ((double)maxAPos + 0.5)*aDiv + aMin;
					lineEq[ang][sec].b = ((double)maxBPos + 0.5)*bDiv + bMin;
				}
//				Log.d(LOG_TAG, "max = "+ maxAPos + ":" + maxBPos + "div = " + aDiv + ":" + bDiv + "min = " + aMin + ":" + bMin + "ab = " + lineEq[ang][sec].a + ":" + lineEq[ang][sec].b);

				if(img != null){
					final double aa = lineEq[ang][sec].a;
					final double bb = lineEq[ang][sec].b;
					Point pt1 = new Point();
					Point pt2 = new Point();
					if(Math.abs(bb) > Math.abs(aa)){
						pt1.x = 0.0;
					    pt1.y = (1.0 - aa*(float)(-centerX))/bb + (float)centerY;
					    pt2.x = (float)mViewWidth;
					    pt2.y = (1.0 - aa*(float)(centerX))/bb + (float)centerY;
					}
					else{
					    pt1.x = (1.0 - bb*(float)(-centerY))/aa + (float)centerX;
						pt1.y= 0.0;
					    pt2.x = (1.0 - bb*(float)(centerY))/aa + (float)centerX;
					    pt2.y = (float)mViewHeight;
					}
					if(Math.abs(bb) > 0.001 && Math.abs(aa/bb) > 0.3 && Math.abs(aa/bb) < 2){
						if(MyDebug.DEBUG){
							Log.d(LOG_TAG, "ang = " + ang + " sec = " + sec + " max a/b = " + maxAPos + ":" + maxBPos);
						}
						
//					    Core.line(img, pt1, pt2, new Scalar(0xff, 0x00, 0x00), 5);
					}
					else{
	//				    Core.line(img, pt1, pt2, new Scalar(0xff, 0xff, 0xff), 5);
					}

					//各クラスごとに線分を描画(debug)
					Scalar color[] = new Scalar[4];
					color[0] = new Scalar(0xff, 0x00, 0x00);
					color[1] = new Scalar(0x00, 0xff, 0x00);
					color[2] = new Scalar(0x00, 0x00, 0xff);
					color[3] = new Scalar(0xff, 0x00, 0xff);
					
					for(LineInfo li:mLineInfo){
					//for(LineInfo li:hist[maxAPos][maxBPos].mLineList){
						int c = 0;
						if(li.mLocationFlg == (LineInfo.ANGLE0|LineInfo.LOCAT0)){
							c = 0;
						}
						else if(li.mLocationFlg == (LineInfo.ANGLE0|LineInfo.LOCAT1)){
							c = 1;
						}
						if(li.mLocationFlg == (LineInfo.ANGLE1|LineInfo.LOCAT0)){
							c = 2;
						}
						else if(li.mLocationFlg == (LineInfo.ANGLE1|LineInfo.LOCAT1)){
							c = 3;
						}
					    Core.line(img, li.mStart, li.mEnd, color[c], 1);
					    Core.circle(img, li.mStart, 10, color[0]);
					    Core.circle(img, li.mEnd, 10, color[1]);

					}
				}
			}
		}
		return true;
	}
	
	private float mPointCenterX = 0.0f;
	private float mPointCenterY = 0.0f;
	
	private class PointComparator implements Comparator<Object>{

		@Override
		public int compare(Object lhs, Object rhs) {
			// TODO Auto-generated method stub
			Point p1 = (Point) lhs;
			Point p2 = (Point) rhs;
			final double angle1 = Math.atan2(p1.y - mPointCenterY, p1.x - mPointCenterX);
			final double angle2 = Math.atan2(p2.y - mPointCenterY, p2.x - mPointCenterX);
			if(angle1 > angle2){
				return 1;
			}
			else if(angle1 < angle2){
				return -1;
			}
			else{
				return 0;
			}
		}
	}
	/**
	 * 選択された４直線の交点を求める。交点は時計回りにソートされる。
	 * @param lineEq 選択された線分の直線方程式(ax+by=1) 配列は[angle][section]。
	 * @param points 交点座標のArrayList
	 * @param img 結果を書き込む画像。nullなら書き込まない
	 * @return true:成功 false:失敗
	 */
	private boolean calcSquare(StraightLineEquation lineEq[][], 
								ArrayList<Point> points,
								Mat img){
		//2直線の交点を求める
		Mat mat = new Mat(2, 2, CvType.CV_32F);
		mPointCenterX = 0.0f;
		mPointCenterY = 0.0f;
		int counter = 0;
		for(int ang0sec = 0;ang0sec < 2;ang0sec++){
			mat.put(0,  0, lineEq[0][ang0sec].a);
			mat.put(0,  1, lineEq[0][ang0sec].b);
			for(int ang1sec = 0;ang1sec < 2;ang1sec++){
				mat.put(1,  0, lineEq[1][ang1sec].a);
				mat.put(1,  1, lineEq[1][ang1sec].b);
				Mat matAns;
				try{
					matAns = mat.inv();
					if(matAns == null) return false;
				}
				catch(Exception e){//逆行列がなかった。２直線は平行。ないはず。
					e.printStackTrace();
					return false;
				}
				float x = (float) (matAns.get(0, 0)[0] + matAns.get(0, 1)[0] + mCenterX);
				float y = (float) (matAns.get(1, 0)[0] + matAns.get(1, 1)[0] + mCenterY);
				Point p = new Point(x, y);
				points.add(p);
				mPointCenterX += x;
				mPointCenterY += y;
				counter++;
			}
		}
		mPointCenterX /= (float)counter;
		mPointCenterY /= (float)counter;
		//時計回りになるようにソート
		Collections.sort(points, new PointComparator());
		if(img != null){
			Scalar color[] = new Scalar[4];
			color[0] = new Scalar(0xff, 0x00, 0x00);
			color[1] = new Scalar(0x00, 0xff, 0x00);
			color[2] = new Scalar(0x00, 0x00, 0xff);
			color[3] = new Scalar(0xff, 0x00, 0xff);

			for(int i = 0;i < 4;i++){
			    Core.circle(img, points.get(i), 30, color[i], 5);
			}
		}

		return true;
	}
	/**
	 * 与えられた線分からホワイトボードの四角形を計算する
	 * @param lines　　線分のリスト　StartX, StartY, EndX, EndY, Width ・・・で格納されている
	 * @param lineNum　線分の数
	 * @param points　計算されたホワイトボードの4隅の座標。時計回りにソートされる。
	 * @param img　画像データ。非nullが与えられたときは途中処理の線分情報が画像に追加される。nullが与えられたときは何もしない
	 * @return true:ホワイトボード検出成功　false:失敗
	 */
	public boolean detectWhiteBoard(int lines[], int lineNum, ArrayList<Point> points, Mat img){
		
		if( lineNum == 0) return false;
		mLineInfo.clear();
		
		long oldSec;
		long newSec;
		if(MyDebug.DEBUG){
			oldSec = System.currentTimeMillis();
		}
		//線分を傾きで２つのクラスに分ける
		if(!divByAngle(lines, lineNum, img)) return false;
		if(MyDebug.DEBUG){
			newSec = System.currentTimeMillis();
			Log.d(LOG_TAG, "divByAngle" + (newSec - oldSec));
			oldSec = newSec;
		}
		//各クラスについて、位置で２つのクラスに分ける(2x2=4クラス)
		if(!divByIntersept(img)) return false;
		if(MyDebug.DEBUG){
			newSec = System.currentTimeMillis();
			Log.d(LOG_TAG, "divByIntersept" + (newSec - oldSec));
			oldSec = newSec;
		}
	
		//各クラスの代表直線を選択する
		StraightLineEquation [][]lineEq = new StraightLineEquation[2][2];
		for(int i = 0;i < 2;i++){
			for(int j = 0;j < 2;j++){
				lineEq[i][j] = new StraightLineEquation();
			}
		}
		if(!selectLines(lineEq, img)) return false;
		
		if(MyDebug.DEBUG){
			newSec = System.currentTimeMillis();
			Log.d(LOG_TAG, "selectLines" + (newSec - oldSec));
			oldSec = newSec;
		}
		
		//代表直線の交点を求める(交点は時計回り).
		if(!calcSquare(lineEq, points, img)) return false;
		if(MyDebug.DEBUG){
			newSec = System.currentTimeMillis();
			Log.d(LOG_TAG, "calcSquare" + (newSec - oldSec));
			oldSec = newSec;
		}

		return true;
	}
	
	/**
	 * 		//ax+by=1の係数
	 * @author masaki
	 *
	 */
	private class StraightLineEquation{
		private double a = 0.0;
		private double b = 0.0;		
	}
	
	public ArrayList<Point> getUnAcceptableLineArea(){
		ArrayList<Point> points = new ArrayList<Point>();
		int qx = mViewWidth >> 2;
		int hx = mViewWidth >> 1;
		int q3x = 3*qx;
		int qy = mViewHeight >> 2;
		int hy = mViewHeight >> 1;
		int q3y = 3*qy;		
		Point p = new Point(qx, hy);
		points.add(p);
		p = new Point(hx, qy);
		points.add(p);
		p = new Point(q3x, hy);
		points.add(p);
		p = new Point(hx, q3y);
		points.add(p);
		return points;
	}
	
	
	private class LineInfo implements Cloneable{
		private static final int ANGLE0 = 0x01;
		private static final int ANGLE1 = 0x02;
		private static final int LOCAT0 = 0x04;
		private static final int LOCAT1 = 0x08;
		private Point mStart = new Point();
		private Point mEnd = new Point();
		private double mWidth = 0.0;//線幅

		private StraightLineEquation mLineEq = new StraightLineEquation();
		private boolean mIsOK = false;
		private double mLength = 0.0;				//線分の長さ
		private double mAngle = 0.0;				//線分の傾き 0～PI
		private double mSectionX = Double.MAX_VALUE;//X軸との切片　X軸と平行に近い場合はMAX_VALUEのまま
		private double mSectionY = Double.MAX_VALUE;//Y軸との切片 	Y軸と平行に近い場合はMAX_VALUEのまま
		private int mLocationFlg;					//角度と切片とで2x2にクラス分けした時のクラスを表すフラグ
		private boolean mIsSteep;					//45度より急の場合はtrue


		/**
		 * 画面中央縦横半分の菱形領域を通る直線はNGとして判断する
		 * @return
		 */
		private boolean checkLineArea(){
			double xp = mLineEq.a == 0.0 ? Double.MAX_VALUE: 1.0/mLineEq.a;
			double yp = mLineEq.b == 0.0 ? Double.MAX_VALUE: 1.0/mLineEq.b;
			xp = Math.abs(xp);
			yp = Math.abs(yp);
			if(xp > mViewWidth/4.0 && yp > mViewHeight/4.0){
				return true;
			}
			else{
				return false;
			}
		}
		private void setLineInfo(double stX, double stY, double edX, double edY, double width){
			mIsOK = false;
			if(stX < 0.0 || stX >= mViewWidth || stY < 0.0 || stY >= mViewHeight) return;
			mWidth = width;
			mStart.x = stX;
			mStart.y = stY;
			mEnd.x = edX;
			mEnd.y = edY;
			final double vecX = mStart.x  - mEnd.x;
			final double vecY = mStart.y  - mEnd.y;
			final double absVecX = Math.abs(vecX);
			final double absVecY = Math.abs(vecY);
			if(absVecX < 1.0 && absVecY < 1.0){
				return;
			}
	        final float centerX = (float)mCenterX;
	        final float centerY = (float)mCenterY;
			
			mLength = Math.sqrt(vecX*vecX + vecY*vecY); 
			mAngle = Math.atan2(vecY, vecX);
			while(mAngle >= Math.PI) mAngle -= Math.PI;
			while(mAngle < 0.0) mAngle += Math.PI;
			if(absVecX > absVecY/10.0){//傾きが急過ぎない場合はY切片を計算しておく
				mSectionY = mEnd.y - mEnd.x*vecY/vecX;
			}
			else{
				mSectionY = Double.MAX_VALUE;
			}
			if(absVecX < absVecY*10.0){//傾きが緩過ぎない場合はX切片を計算しておく
				mSectionX = mEnd.x - mEnd.y*vecX/vecY;
			}
			else{
				mSectionX = Double.MAX_VALUE;
			}
			if(mSectionX == Double.MAX_VALUE && mSectionY == Double.MAX_VALUE) return;
			
			
			//傾きが45度より急かどうか
			mIsSteep = absVecX < absVecY;
			//線分の直線式を求める
			Mat mat = new Mat(2, 2, CvType.CV_32F);
			mat.put(0,  0, mStart.x - centerX);
			mat.put(0,  1, mStart.y - centerY);
			mat.put(1,  0, mEnd.x - centerX);
			mat.put(1,  1, mEnd.y - centerY);
			try{
				mat = mat.inv();
				if(mat == null) return;
			}
			catch(Exception e){//逆行列がなかった。中心を通る
				e.printStackTrace();
				return;
			}
			mLineEq.a = (float) (mat.get(0, 0)[0] + mat.get(0, 1)[0]);
			mLineEq.b = (float) (mat.get(1, 0)[0] + mat.get(1, 1)[0]);

			//画面中央縦横半分の菱形領域を通る直線はNGとして判断する
			if(!checkLineArea()) return;

			mIsOK = true;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#clone()
		 */
		@Override
		protected Object clone(){
			// TODO Auto-generated method stub
			LineInfo li;
			try {
				li = (LineInfo)super.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			li.mLineEq.a = mLineEq.a;
			li.mLineEq.b = mLineEq.b;
			li.mStart.x = mStart.x;
			li.mStart.y = mStart.y;
			li.mEnd.x = mEnd.x;
			li.mEnd.y = mEnd.y;
			

			return li;
		}

	}
	
	private class LineList{
		private ArrayList<LineInfo> mLineList=null;
		private int getLineListNum(){
			if(mLineList == null) return 0;
			return mLineList.size();
		}
		private boolean mSortByX = true;
		/**
		 * mSortByX==trueの場合はxについて、そうでない場合はyについて、mStart < mEndとなるように始点・終点を入れ替える
		 * @param li
		 */
		private void correctDirection(LineInfo li){
			boolean reverse = false;			
			if(mSortByX){
				if(li.mStart.x > li.mEnd.x){
					reverse = true;
				}
			}
			else{
				if(li.mStart.y > li.mEnd.y){
					reverse = true;
				}
			}
			if(reverse){
				Point tmp = li.mStart;
				li.mStart = li.mEnd;
				li.mEnd = tmp;
			}
		}
		/**
		 * Point1とPoint2の大小を比較する比較する座標はmSortByXで決まる
		 * @param p1
		 * @param p2
		 * @return 1:p1>p2 -1:p1<p2 0:p1==p2
		 */
		private int compare(Point p1, Point p2){
			double v1, v2;
			if(mSortByX){
				v1 = p1.x;
				v2 = p2.x;
			}
			else{
				v1 = p1.y;
				v2 = p2.y;
			}
			if(v1 > v2){
				return 1;
			}
			else if(v1 < v2){
				return -1;
			}
			else{
				return 0;
			}
		}
		/**
		 * 線分を追加する。追加する線分が他の線分と重なっている場合は、重なっている線分に連結し、その中に含まれる線分は削除する
		 * @param lineInfo
		 */
		private void pushLine(LineInfo lineInfo){
			LineInfo li = (LineInfo)lineInfo.clone();
			if(mLineList == null){
				mLineList = new ArrayList<LineInfo>();
				if(!li.mIsSteep){
					mSortByX = true;
				}
				else{
					mSortByX = false;
				}
				correctDirection(li);
				mLineList.add(li);
				return;
			}
			correctDirection(li);
			//stIncluded == trueで終わったときは、その時のstLine番の線分に始点が含まれる
			//stLine == mLineList.size()で終わったときは　線分に含まれず、一番後ろになる
			//stLine < mLineList.size()で終わったときは始点は stLine-1番とstLine番の間にある
			int stLine;
			boolean stIncluded = false;
			final int lineListSize = mLineList.size();
			for(stLine = 0;stLine < lineListSize;stLine++){
				final int compST = compare(li.mStart, mLineList.get(stLine).mStart);
				if(compST < 0) break;//始点は線分に含まれていない
				final int compED = compare(li.mStart, mLineList.get(stLine).mEnd);
				if(compED <= 0){//liの始点がどれかの線分に含まれる
					stIncluded = true;
					break;
				}
			}
			if(stLine == lineListSize){//新しい線分は一番最後なので追加して抜ける
				mLineList.add(li);
				return;
			}
			//edInclude == trueで終わったときはその時のedLine番の線分に終点が含まれる
			//edLine < 0で終わったときは、線分に含まれず、一番前になる
			//edLine >= 0 で終わったときは、終点は edLine番とedLIne+1番との間にある
			int edLine;
			boolean edIncluded = false;
			for(edLine = lineListSize-1;edLine >= 0 ;edLine--){
				final int compED = compare(li.mEnd, mLineList.get(edLine).mEnd);
				if(compED > 0) break;//終点は線分に含まれていない
				final int compST = compare(li.mEnd, mLineList.get(edLine).mStart);
				if(compST >= 0){//liの終点がどれかの線分に含まれる
					edIncluded = true;
					break;
				}
			}
			if(edLine < 0){//新しい線分は一番最初なので追加して抜ける
				mLineList.add(0, li);
				return;
			}
			if(stIncluded && edIncluded){//liの始点・終点とも線分に含まれる場合
				if(stLine == edLine){
					//等しい場合は新しい線分はこれまでの線分に含まれているため何もしない
				}
				else{
					//stLine番とedLine番の線分をつなぐ
					mLineList.get(stLine).setLineInfo(mLineList.get(stLine).mStart.x, 
														mLineList.get(stLine).mStart.y, 
														mLineList.get(edLine).mEnd.x,
														mLineList.get(edLine).mEnd.y, 
														mLineList.get(edLine).mWidth);//widthの割合を考える必要あり
					for(int i = stLine+1;i <= edLine;i++){
						mLineList.remove(stLine+1);
					}
				}
				return;
			}
			else if(stIncluded){//liの始点だけ線分に含まれる場合
				//stLine番の線分の終点をli.mEndまで伸ばす
				mLineList.get(stLine).setLineInfo(mLineList.get(stLine).mStart.x, 
													mLineList.get(stLine).mStart.y, 
													li.mEnd.x,
													li.mEnd.y, 
													mLineList.get(stLine).mWidth);//widthの割合を考える必要あり
				for(int i = stLine+1;i <= edLine;i++){
					mLineList.remove(stLine+1);
				}
				return;
			}
			else if(edIncluded){//liの終点だけ線分に含まれる場合
				//edLine番の線分の始点をli.mStartまで伸ばす
				mLineList.get(edLine).setLineInfo(li.mStart.x, 
												li.mStart.y, 
												mLineList.get(edLine).mEnd.x,
												mLineList.get(edLine).mEnd.y,
												mLineList.get(edLine).mWidth);//widthの割合を考える必要あり
				for(int i = stLine;i < edLine;i++){
					mLineList.remove(stLine);
				}
			}
			else{//始点・終点とも線分に含まれない場合
				for(int i = stLine;i <= edLine;i++){
					mLineList.remove(stLine);
				}
				mLineList.add(stLine, li);
			}
		}	
	}
}
