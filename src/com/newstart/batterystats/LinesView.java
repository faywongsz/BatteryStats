package com.newstart.batterystats;

import com.newstart.batterystats.MainActivity.StatData;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

public class LinesView extends View {

	private StatData[] mData;
	private int mMaxMinutes = 0;
	private int mWidth, mHeight, mFontWidth, mFontHeight;
	private Point mOriginPoint = new Point();
	private Paint mBlackPaint, mRedPaint;
	private float mDesity;
	private boolean mHasDrawn = false;
	
	public LinesView(Context context) {
		this(context, null);
	}

	public LinesView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public LinesView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		mDesity = getResources().getDisplayMetrics().density;
		mBlackPaint = new Paint();
		mBlackPaint.setStyle(Paint.Style.STROKE);
		mBlackPaint.setAntiAlias(true);
		mBlackPaint.setColor(Color.parseColor("#000000"));
		mBlackPaint.setTextSize(12);
		
		mRedPaint = new Paint();
		mRedPaint.setStyle(Paint.Style.STROKE);
		mRedPaint.setAntiAlias(true);
		mRedPaint.setColor(Color.parseColor("#FF0000"));
		mRedPaint.setTextSize(12);
		
		mFontWidth = (int) mBlackPaint.measureText("1000");
		mFontHeight = (int) (mBlackPaint.getFontMetrics().bottom - mBlackPaint.getFontMetrics().top);
	}
	

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		mWidth = MeasureSpec.getSize(widthMeasureSpec);
		mHeight = MeasureSpec.getSize(heightMeasureSpec);
		
		mOriginPoint.x = mFontWidth;
		mOriginPoint.y = (int) (mHeight - mFontHeight - mDesity);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		mRedPaint.setTextSize(12);
		
		int xLength = mWidth - mFontWidth * 2;
		int yLength = mHeight - mFontHeight * 2;
		
		//x axis
		canvas.drawLine(mOriginPoint.x, mOriginPoint.y, mOriginPoint.x + xLength, mOriginPoint.y, mBlackPaint);
		for(int i = 0; i <= 20; i++) {
			if(i % 4 == 0) {
				String minuteValue = String.valueOf(i * mMaxMinutes / 20);
				canvas.drawText(minuteValue, mOriginPoint.x + i * xLength / 20 - mRedPaint.measureText(minuteValue) / 2, mOriginPoint.y + mDesity + mFontHeight, mRedPaint);
			}
			canvas.drawLine(mOriginPoint.x + i * xLength / 20, mOriginPoint.y - 2 * mDesity, mOriginPoint.x + i * xLength / 20, mOriginPoint.y + 2 * mDesity, (i % 4 == 0) ? mRedPaint : mBlackPaint);
		}
		
		//y axis
		canvas.drawLine(mOriginPoint.x, mOriginPoint.y, mOriginPoint.x, mOriginPoint.y - yLength, mBlackPaint);
		for(int i = 0; i <= 20; i++) {
			canvas.drawText(String.valueOf(i * 5), mDesity, mOriginPoint.y - i * yLength / 20, mBlackPaint);
			canvas.drawLine(mOriginPoint.x - 2 * mDesity, mOriginPoint.y - i * yLength / 20, mOriginPoint.x + 2 * mDesity, mOriginPoint.y - i * yLength / 20, mBlackPaint);
		}
		
		//lines
		for(int i = 0; mData != null && i < mData.length - 1; i++) {
			if(mData[i].time > 0) {
				for(int j = i + 1; j < mData.length; j++) {
					if(mData[j].time >= 0) {
						canvas.drawLine(mOriginPoint.x + mData[i].getTimeMinute() * xLength / mMaxMinutes, mOriginPoint.y - mData[i].precent * yLength / 100, 
								mOriginPoint.x + mData[j].getTimeMinute() * xLength / mMaxMinutes, mOriginPoint.y - mData[j].precent * yLength / 100, mRedPaint);
						android.util.Log.d("BatteryStats", "i =" + i + ",j=" + j + "(" + (mOriginPoint.x + (mData[i].time / 60) * xLength / mMaxMinutes) + ", " + (mOriginPoint.y - mData[i].precent * yLength / 100) +  ")->" + "(" + (mOriginPoint.x + (mData[j].time / 60) * xLength / mMaxMinutes) + ", " + (mOriginPoint.y - mData[j].precent * yLength / 100) +  ")");
						break;
					}
				}
			}
		}
		
		//points
		for(int i = 0; mData != null && i <= 20; i = i + 2) {
			for(int j = mData.length - 1; j >= 0; j--) {
				if(mData[j].getTimeMinute() >= i * mMaxMinutes / 20) {
					canvas.drawText(String.valueOf((mData[j].getTimeMinute() == i * mMaxMinutes / 20) ? j : (j + 1)), 
							mOriginPoint.x + i * xLength / 20, Math.max(mOriginPoint.y - j * yLength / 100 - mFontHeight / 2, mFontHeight), mRedPaint);
					break;
				}
			}
		}
		
		//tips
		mRedPaint.setTextSize(16);
		String tips = "x=Total Time";
		final float padding = mRedPaint.measureText(tips) + 10 * mDesity;
		canvas.drawText(tips, mOriginPoint.x + xLength - padding, mOriginPoint.y - yLength, mRedPaint);
		tips = "y=Percent";
		canvas.drawText(tips, mOriginPoint.x + xLength - padding, mOriginPoint.y - yLength + mFontHeight, mRedPaint);
	}
	
	public void setData(StatData[] data) {
		mData = data;
		int max = 0;
		for(int i = 0; mData != null && i < mData.length; i++) {
			max = mData[i].time > max ? mData[i].time : max;
		}
		mMaxMinutes = (int) (max / 60.f);
		if(mMaxMinutes <= 100) {
			mMaxMinutes = 100;
		}
		
		invalidate();
	}
	
	public boolean hasDrawn() {
		return mHasDrawn;
	}
	
	public void setHasDrawn(boolean hasDrawn) {
		mHasDrawn = hasDrawn;
	}
}
