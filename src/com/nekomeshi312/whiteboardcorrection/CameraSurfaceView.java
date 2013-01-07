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

import java.util.List;
import com.nekomeshi312.cameraandparameters.CameraAndParameters;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraSurfaceView extends SurfaceView{
	public interface CameraPreview{
		public Bitmap getCameraBitmap();
	}
	private static final String LOG_TAG = "CameraSurfaceView";

    private CameraPreview mCameraPreview = null;
    
    public CameraSurfaceView(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}
	public CameraSurfaceView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		// TODO Auto-generated constructor stub
	}
	public CameraSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public void setCameraPreview(CameraPreview camPreview){
		mCameraPreview = camPreview;
	}
	/* (non-Javadoc)
	 * @see android.view.SurfaceView#onMeasure(int, int)
	 */


	/* (non-Javadoc)
	 * @see android.view.SurfaceView#dispatchDraw(android.graphics.Canvas)
	 */
	@Override
	protected void dispatchDraw(Canvas canvas) {
		// TODO Auto-generated method stub

		
		Bitmap bmp = null;
		if(mCameraPreview != null){
			bmp = mCameraPreview.getCameraBitmap();
		}
		if(bmp != null){
			canvas.drawBitmap(bmp, 0, 0, null);
		}
		else{
			super.dispatchDraw(canvas);
		}
	}
}
