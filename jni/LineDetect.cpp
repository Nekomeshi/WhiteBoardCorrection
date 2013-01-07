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

#include "LineDetect.h"
#define _USE_MATH_DEFINES
#include <math.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <vector>
#include "lsd.h"
	
using namespace std;
//#define DEBUG

#define LOG_TAG "LineDetect"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)


int g_ImgWidth;
int g_ImgHeight;
int g_MaxLineNum;
cv::Mat *g_GrayImg = NULL;
int g_LSDScale;

//#define USE_HOUGH
#define USE_LSD

double *g_LSDImg = NULL;
vector<cv::Vec4i> g_HoughLines;

int doHough(int *lines, cv::Mat *pRgb)
{
 	cv::Canny(*g_GrayImg, *g_GrayImg, 80, 100);
 	g_HoughLines.clear();
 	cv::HoughLinesP(*g_GrayImg, g_HoughLines, 1, M_PI/180.0, (int) 50, 100.0, 20.0);  
 	int lineNum = g_HoughLines.size();
 	lineNum = lineNum < g_MaxLineNum ? lineNum:g_MaxLineNum;
 	for(int i = 0;i < lineNum;i++){
 		cv::Vec4i line = g_HoughLines.at(i);
 		int i5 = i*5;
 		for(int j = 0;j < 4;j++){
 	 		lines[i5+j] = line[j];
 		}
 		lines[i5+4] = 1;
#ifdef DEBUG
    	if(NULL != pRgb){
	    	cv::Point pt1 = cv::Point(lines[i5+0], lines[i5+1]);
    		cv::Point pt2 = cv::Point(lines[i5+2], lines[i5+3]);
	    	cv::line(*pRgb, pt1, pt2, cv::Scalar(0,0,255), 3, CV_AA);
	    }
#endif
  	}
  	return lineNum;
}

int doLSD(int *lines, cv::Mat *pRgb)
{
	uchar *pGray = g_GrayImg->ptr<uchar>(0);
	const int h = g_ImgHeight/g_LSDScale;
	const int w = g_ImgWidth/g_LSDScale;
	int size = 0;
	for(int y = 0;y < g_ImgHeight;y += g_LSDScale){
		int yy = y*g_ImgWidth;
		for(int x = 0;x < g_ImgWidth;x += g_LSDScale){
			g_LSDImg[size] = (double)pGray[yy + x];
			size++;
		}
	}

	int lineNum;
	double *lsdLines = LineSegmentDetection(&lineNum,
								g_LSDImg, w, h,
								0.8, 0.6, 2.0,
								22.5, 0.0, 0.7,
								1024,
								NULL, NULL, NULL);

    lineNum = lineNum < g_MaxLineNum ? lineNum:g_MaxLineNum;
	for(int i = 0;i < lineNum;i++){
		int i5 = (i << 2) + i; 	//i*5
		int i7 = i5 + (i << 1);	//i*7
		for(int j = 0;j < 4;j++){
			lines[i5+j] = (int)((lsdLines[i7+j] + 0.5)*(double)g_LSDScale + 0.5);
		}
		lines[i5+4] = (int)(lsdLines[i7+4] + 0.5);
#ifdef DEBUG
		if(NULL != pRgb){
	    	cv::Point pt1 = cv::Point(lines[i5+0], lines[i5+1]);
	   		cv::Point pt2 = cv::Point(lines[i5+2], lines[i5+3]);
	    	cv::line(*pRgb, pt1, pt2, cv::Scalar(0,128,128), lines[i5+4]*2, CV_AA);
		}
#endif
	}

	return lineNum;
}


/*
 * Class:     com_nekomeshi312_whiteboardcorrection_DetectLines
 * Method:    detectLines
 * Signature: (JZJ[I)I
 */
JNIEXPORT jint JNICALL Java_com_nekomeshi312_whiteboardcorrection_DetectLines_detectLines
								(JNIEnv *jenv, jobject jobj, jlong inputImg, jboolean isYUV, jlong outputImg, jintArray lines)
{
    cv::Mat* pInputImg = (cv::Mat*)inputImg;
    cv::Mat* pOutputImg = (cv::Mat*)outputImg;
    jint *l = (int *)jenv->GetIntArrayElements(lines, 0);
    if(NULL != outputImg){
    	if(isYUV){
    	    cv::cvtColor(*pInputImg, *pOutputImg, cv::COLOR_YUV420sp2RGBA);
    	}
    	else{
    	    cv::cvtColor(*pInputImg, *pOutputImg, cv::COLOR_RGB2RGBA);
    	}
 		cv::cvtColor(*pOutputImg, *g_GrayImg, cv::COLOR_RGBA2GRAY);
    }
    else{
    	if(isYUV){
    		cv::cvtColor(*pInputImg, *g_GrayImg, cv::COLOR_YUV420sp2GRAY);
    	}
    	else{
    		cv::cvtColor(*pInputImg, *g_GrayImg, cv::COLOR_RGB2GRAY);
    	}
	}
#ifdef USE_HOUGH
	int lineNum = doHough(l, pOutputImg);
#endif
#ifdef USE_LSD
	int lineNum = doLSD(l, pOutputImg);
#endif

	
#ifdef DEBUG
	LOGD("Detected Line Num = %d", lineNum);
#endif
	jenv->ReleaseIntArrayElements(lines, l, 0);
	return lineNum;
}



/*
 * Class:     com_nekomeshi312_whiteboardcorrection_DetectLines
 * Method:    initLines
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_com_nekomeshi312_whiteboardcorrection_DetectLines_initLines
				(JNIEnv *jenv, jobject jobj, jint width, jint height, jint scale, jint maxLineNum)
{
	if(NULL != g_GrayImg){
		Java_com_nekomeshi312_whiteboardcorrection_DetectLines_cleanupLines(jenv, jobj);
	}
	LOGD("LineDetector INIT");
	g_ImgWidth = width;
	g_ImgHeight = height;
	g_LSDScale = scale;
	g_MaxLineNum = maxLineNum;
	g_GrayImg = new cv::Mat(g_ImgHeight, g_ImgWidth, CV_8UC1);
#ifdef USE_LSD
	const int h = g_ImgHeight/g_LSDScale;
	const int w = g_ImgWidth/g_LSDScale;
	g_LSDImg = new double[h*w];
#endif
}

/*
 * Class:     com_nekomeshi312_whiteboardcorrection_DetectLines
 * Method:    cleanupLines
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_nekomeshi312_whiteboardcorrection_DetectLines_cleanupLines
								  (JNIEnv *, jobject)
{
	LOGD("LineDetector Clean UP");
	if(NULL != g_GrayImg){
		g_GrayImg->release();
		delete g_GrayImg;
		g_GrayImg = NULL;
	}
#ifdef USE_LSD
	if(NULL != g_LSDImg){
		delete [] g_LSDImg;
		g_LSDImg = NULL;
	}
#endif
}
