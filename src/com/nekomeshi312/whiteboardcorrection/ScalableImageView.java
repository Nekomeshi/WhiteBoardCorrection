package com.nekomeshi312.whiteboardcorrection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

public class ScalableImageView extends ImageView {
	private static final String LOG_TAG = "ScalableImageView";
	
	private Matrix mMatrix = new Matrix();
	public ScalableImageView(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}
	public ScalableImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		// TODO Auto-generated constructor stub
	}
	public ScalableImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see android.widget.ImageView#setImageBitmap(android.graphics.Bitmap)
	 */
	@Override
	public void setImageBitmap(Bitmap bm) {
		// TODO Auto-generated method stub
		super.setImageBitmap(bm);

		setScaleType(ScaleType.MATRIX);
		final float ratioW = (float)bm.getWidth()/(float)getWidth();
		final float ratioH = (float)bm.getHeight()/(float)getHeight();
		float scale;
		if(ratioW > ratioH){//画面に対し画像が横長
			scale = 1.0f/ratioW;
		}
		else{
			scale = 1.0f/ratioH;			
		}
		final float transX = ((float)getWidth() - (float)bm.getWidth()*scale)/2.0f;
		final float transY = ((float)getHeight() - (float)bm.getHeight()*scale)/2.0f;

		mMatrix.setScale(scale, scale);
		mMatrix.postTranslate(transX, transY);
		setImageMatrix(mMatrix);
		invalidate(); 
	}
	
	private int mLastFingerNum = 0;
	/* (non-Javadoc)
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if(getScaleType()!= ScaleType.MATRIX){//まだ結果画像が設定されていない
			return super.onTouchEvent(event);
		}
		final int pointCount = event.getPointerCount();
		final boolean isInit = mLastFingerNum != pointCount || event.getAction() != MotionEvent.ACTION_MOVE;
		boolean result = false;
		switch(pointCount){
			case 0:	result = super.onTouchEvent(event);
					break;
			case 1:imageMove(event.getX(), event.getY(), isInit);
					result = true;
					break;
			case 2:imageZoom(event.getX(0), event.getY(0), 
							event.getX(1), event.getY(1), isInit);
					result = true;
					break;
			default:result = super.onTouchEvent(event);
					break;
		}
		mLastFingerNum = pointCount;
		return result;
	}
	
	private float mLastXPos;
	private float mLastYPos;
	private void imageMove(float xpos, float ypos, boolean init){
		if(!init){
			mMatrix.postTranslate(xpos - mLastXPos, ypos - mLastYPos);
			setImageMatrix(mMatrix);
			invalidate(); 
		}
		mLastXPos = xpos;
		mLastYPos = ypos;
	}
	private float mLastSize;
	private void imageZoom(float xpos0, float ypos0, 
							float xpos1, float ypos1,
							boolean init){
		final float s = (float) Math.sqrt( (xpos0 - xpos1)*(xpos0 - xpos1) + (ypos0 - ypos1)*(ypos0 - ypos1));
		float cx = (xpos0 + xpos1)/2.0f;
		float cy = (ypos0 + ypos1)/2.0f;
		if(!init){
			mMatrix.postTranslate(-cx, -cy);
			mMatrix.postScale(s/mLastSize, s/mLastSize);
			mMatrix.postTranslate(cx, cy);
			setImageMatrix(mMatrix);
			invalidate(); 
		}
		mLastSize = s;
		imageMove(cx, cy, init);
	}
}
