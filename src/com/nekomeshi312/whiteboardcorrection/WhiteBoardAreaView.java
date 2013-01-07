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

import org.opencv.core.Point;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
public class WhiteBoardAreaView extends View {
	private static final String LOG_TAG = "WhiteBoardAreaView";
	private static final int LINE_WIDTH = 5;//検出されたWBエリアを示すラインの幅
	private static final int INVALID_AREA_LINE_WIDTH = 1;//WB禁止エリアを示すひしがた領域の線の幅
	private Paint mLinePaint = new Paint();	//WBエリアを示すpaint
	private Path mLinePath = new Path();//WBエリアを示すpath
	
	private Paint mInvalidAreaPaint = new Paint();//WB禁止エリアを示す領域のpaint
	private Paint mInvalidAreaLine = new Paint();//WB禁止エリアを示す線のpaint
	private Path mInvalidAreaPath = null;//WB禁止エリアを示すpath
		
	private boolean mIsDraggable = false;//WBエリアの頂点をドラッグ可能かどうか
	private ArrayList<Point> mCorners = new ArrayList<Point>();//WBエリアの頂点座標
	private int mDraggingPoint = -1;//現在dragging中の頂点の番号　負の値の場合はdragしていない
	private static final double POINT_SHIFT_SIZE = 300.0;//ドラッグ中の点を実際の指の位置からシフトさせる量。指の真下だと見えないためシフトさせる
	private static final int DRAG_POINT_CIRCLE_D = 10;
	private final int mDragPointColors[] = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};//drag可能時の、頂点を示す○の色
	private ArrayList<Paint> mDraggableAreaPaint = new ArrayList<Paint>();
	private ArrayList<Paint> mDraggingAreaPaint = new ArrayList<Paint>();
	private static final int TOUCH_DETECT_SIZE = 900;//(dp)　指の位置が実頂点からどれだけずれていてもタッチしたとみなすかのサイズ
	
	private double mDensity;//デイスプレイのdensity
	
	public WhiteBoardAreaView(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}
	public WhiteBoardAreaView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		// TODO Auto-generated constructor stub
	}
	public WhiteBoardAreaView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub

		for(int i = 0;i < 4;i++){
			Paint p = new Paint();
			p.setStyle(Paint.Style.STROKE);
			p.setColor(mDragPointColors[i]);
			p.setStrokeWidth(LINE_WIDTH);
			mDraggableAreaPaint.add(p);
			
			p = new Paint();
			p.setStyle(Paint.Style.FILL);
			p.setColor(mDragPointColors[i]);
			p.setStrokeWidth(LINE_WIDTH);
			mDraggingAreaPaint.add(p);
		}
		
		mLinePaint.setStyle(Paint.Style.STROKE);
		mLinePaint.setColor(0xFF00FF00);
		mLinePaint.setStrokeWidth(LINE_WIDTH);
		
		mInvalidAreaPaint.setStyle(Paint.Style.FILL);
		mInvalidAreaPaint.setStrokeWidth(0);
		mInvalidAreaPaint.setColor(0x80000000);
		mInvalidAreaLine.setStyle(Paint.Style.STROKE);
		mInvalidAreaLine.setStrokeWidth(INVALID_AREA_LINE_WIDTH);
		mInvalidAreaLine.setColor(0xFFFFFFFF);
		DisplayMetrics metrics = new DisplayMetrics();  
		((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics); 
		mDensity = metrics.density;
	}
	
	private int mStartX = 0;//ドラッグ開始時の座標。dragキャンセル時に戻すために使用
	private int mStartY = 0;
	private double mPointShiftVexX = 0.0;//drag中の指の実位置から点をずらすベクトル。画面中心から外にずらす
	private double mPointShiftVexY = 0.0;
	/* (non-Javadoc)
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if(!mIsDraggable){
			return super.onTouchEvent(event);
		}
		if(mCorners.size() < 4){
			return super.onTouchEvent(event);
		}
		final int x = (int) event.getX();
		final int y = (int) event.getY();

		switch(event.getAction()){
			case MotionEvent.ACTION_DOWN:
				int pos = -1;
				int minAve = Integer.MAX_VALUE;
				for(int i = 0;i < mCorners.size();i++){
					Point p = mCorners.get(i);
					final int thres = (int)((double)TOUCH_DETECT_SIZE/mDensity + 0.5);
					final int dx = (int)Math.abs(p.x - x);
					final int dy = (int)Math.abs(p.y - y);
					if(dx > thres || dy > thres)continue;
					final int ave = dx + dy;
					if(ave < minAve){
						pos = i;
						minAve = ave;
					}
				}
				if(pos < 0) break;
				mStartX = x;
				mStartY = y;
				mDraggingPoint = pos;
				
				final int centerX = this.getWidth() >> 1;
				final int centerY = this.getHeight() >> 1;
				mPointShiftVexX = (double)(x - centerX);
				mPointShiftVexY = (double)(y - centerY);
				double size = Math.sqrt(mPointShiftVexX*mPointShiftVexX + mPointShiftVexY*mPointShiftVexY);
				mPointShiftVexX *= (POINT_SHIFT_SIZE/mDensity/size);
				mPointShiftVexY *= (POINT_SHIFT_SIZE/mDensity/size);
				
			case MotionEvent.ACTION_MOVE:
				mCorners.get(mDraggingPoint).x = x + mPointShiftVexX;
				mCorners.get(mDraggingPoint).y = y + mPointShiftVexY;
				setWhiteBoardCorners();
				return true;
			case MotionEvent.ACTION_CANCEL:
				mCorners.get(mDraggingPoint).x = mStartX;
				mCorners.get(mDraggingPoint).y = mStartY;
			case MotionEvent.ACTION_UP:
				mDraggingPoint = -1;
				setWhiteBoardCorners();
				return true;
			default:
				break;
			
		}
		return super.onTouchEvent(event);
		
	}


	public void setDraggable(boolean isDraggable){
		mIsDraggable = isDraggable;
	}
	
	public ArrayList<Point> getWhiteBoardCorners(){
		return mCorners;
	}
	public void setWhiteBoardCorners(ArrayList<Point> corners){
		mCorners = (ArrayList<Point>) corners.clone();
		setWhiteBoardCorners();
	}
	private void setWhiteBoardCorners(){
		mLinePath.reset();
		if(mCorners.size() <= 1){
		}
		else{
			for(int i = 0;i < mCorners.size();i++){
				Point p = mCorners.get(i);
				if(i == 0){
					mLinePath.moveTo((float)p.x, (float)p.y);
				}
				else{
					mLinePath.lineTo((float)p.x, (float)p.y);
				}
			}
			mLinePath.close();
		}
		invalidate();
	}
	public void setInvalidWhiteBoardArea(ArrayList<Point> invalidArea){
		if(mInvalidAreaPath == null) mInvalidAreaPath = new Path();
		mInvalidAreaPath.reset();
		for(int i = 0;i < invalidArea.size();i++){
			Point p = invalidArea.get(i);
			if(i == 0){
				mInvalidAreaPath.moveTo((float)p.x, (float)p.y);
			}
			else{
				mInvalidAreaPath.lineTo((float)p.x, (float)p.y);
			}
		}
		mInvalidAreaPath.close();
	}

	/* (non-Javadoc)
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		canvas.drawPath(mLinePath, mLinePaint);
		if(mIsDraggable){
			for(int i = 0;i < mCorners.size();i++){
				Point p = mCorners.get(i);
				if(i == mDraggingPoint){
					canvas.drawCircle((float)p.x, 
										(float)p.y, 
										(float)DRAG_POINT_CIRCLE_D, 
										mDraggingAreaPaint.get(i%4));
				}
				else{
					canvas.drawCircle((float)p.x, 
										(float)p.y, 
										(float)DRAG_POINT_CIRCLE_D, 
										mDraggableAreaPaint.get(i%4));
				}
			}
		}

		if(mInvalidAreaPath != null){
			canvas.drawPath(mInvalidAreaPath, mInvalidAreaPaint);
			canvas.drawPath(mInvalidAreaPath, mInvalidAreaLine);
		}
		super.onDraw(canvas);
	}

}
