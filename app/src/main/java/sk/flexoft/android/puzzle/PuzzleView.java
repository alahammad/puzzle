/**
 * 
 */
package sk.flexoft.android.puzzle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.TreeMap;

import sk.flexoft.android.puzzle.util.AndroidExtensions;
import sk.flexoft.android.puzzle.util.AndroidExtensions.LogType;

/**
 * @author Vladimir Iszer
 * The Class PuzzleView encapsulates the surface drawing.
 */
public class PuzzleView extends SurfaceView implements Runnable, SurfaceHolder.Callback, View.OnTouchListener {

	/** The Constant TAG. */
	private static final String TAG = "PuzzleView";
	
	/** Multiplier used for hashing rectangle size. */
	private static final int RECT_HASH_MULTIPLIER = 10000;
	
	/** The bitmap. */
	Bitmap bitmap;
	
	/** The surface holder. */
	SurfaceHolder holder;

	/** The paint defining raster color, thickness, etc. */
	Paint rasterPaint;
	
	/** The paint defining raster border color, thickness, etc. */
	Paint rasterBorderPaint;
	
	/** The paint used for interaction related painting. */
	Paint touchPaint;
	
	/** Flag indicating if application is actively running. */
	boolean isRunning = false;
	
	/** The rendering thread. */
	Thread t;
	
	/** Currently active rectangle. */
	Rect activeRect = null;
	
	/** The active rectangle hash. */
	int activeRectHash = 0;
	
	/** The map of views to view related info to avoid recalculating bitmap info when view has changed. */
	TreeMap<Integer, ScreenInfo> viewMap = new TreeMap<Integer, ScreenInfo>();

	/** The main application activity. */
	PuzzleActivity puzzleActivity;
	
	/** The identifier of a gesture starting pointer. */
	private int gesturePointerId = -1; 
	
	/** The gesture rectangle indexes. */
	private int gestureRectIdx = -1;
	
	/**
	 * Constructor of the PuzzleView class. 
	 * @param context The activity creating this instance.
	 * @bitmap the bitmap to be used for story board
	 */
	public PuzzleView(Context context, Bitmap bitmap) {
		super(context);
		
		if (context == null)
		{
			throw new IllegalArgumentException("context");
		}
		
		if (bitmap == null)
		{
			throw new IllegalArgumentException("bitmap");
		}
		
		this.bitmap = bitmap;
		puzzleActivity = (PuzzleActivity)context;
		
		rasterPaint = new Paint();
		rasterPaint.setColor(Color.LTGRAY);
		rasterPaint.setAlpha(128);
		rasterPaint.setStrokeWidth(1);
		
		touchPaint = new Paint();
		touchPaint.setColor(Color.LTGRAY);
		touchPaint.setAlpha(64);
		touchPaint.setStrokeWidth(1);
		
		rasterBorderPaint = new Paint();
		rasterBorderPaint.setColor(Color.LTGRAY);
		rasterBorderPaint.setStrokeWidth(1);
		rasterBorderPaint.setStyle(Paint.Style.STROKE);
		
		holder = getHolder();
		holder.addCallback(this);
		
		setOnTouchListener(this);
		
		resume();
	}

	/**
	 * Performs needed operations on application pause.
	 */
	public void pause()
	{
		isRunning = false;
		
		if (t == null)
		{
			return;
		}
		
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Log.d(TAG, "Paused");
		t = null;
	}
	
	/**
	 * Resumes the processing on application restore.
	 */
	public void resume()
	{
		if (t != null)
		{
			pause();
		}
		
		t = new Thread(this);
		isRunning = true;
		t.start();
		
		Log.d(TAG, "Resumed");
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		boolean isFinishedStateDrawn = false;
		while (isRunning)
		{
			// in case the puzzle is finished by last move, draw the new state only once
			if (puzzleActivity.getBoard().isFinished())
			{
				if (!isFinishedStateDrawn)
				{
					Log.d(TAG, "Seeting isFinishedStateDrawn to true.");
					isFinishedStateDrawn = true;
				}
				else
				{
					try 
					{
						Thread.sleep(200);
						continue;
					} 
					catch (InterruptedException e) 
					{
						// thread termination was requested during the sleep
						return;
					}
				}
			}
			else
			{
				isFinishedStateDrawn = false;
			}
			
			Surface s = holder.getSurface(); 
			if (s == null || !s.isValid() || activeRect == null || activeRectHash == 0 || !viewMap.containsKey(activeRectHash))
			{
				continue;
			}
			
			Canvas c = holder.lockCanvas();
			
			try
			{
				ScreenInfo info = viewMap.get(activeRectHash);
				PuzzleBoard board = puzzleActivity.getBoard();
				
				if (info != null && board != null && info.parts == board.getSize())
				{
					drawScreen(c, info, board);	
				}
			}
			finally
			{
				if (c!=null)
				holder.unlockCanvasAndPost(c);
			}
		}
 	}

	
	/* (non-Javadoc)
	 * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
		AndroidExtensions.Log(LogType.Debug, TAG, "surfaceChanged: [%d, %d]", width, height);
		pause();
		activeRect = new Rect(0, 0, width, height);
		int rasterSize = puzzleActivity.getRasterSize();
		activeRectHash = hashRectSize(activeRect, rasterSize);
		if (!viewMap.containsKey(activeRectHash))
		{
			viewMap.put(activeRectHash, new ScreenInfo(bitmap, activeRect.width(), activeRect.height(), rasterSize));
		}
		
		AndroidExtensions.Log(LogType.Debug, TAG, "view contains %d items", viewMap.size());
		resume();
	}

	/* (non-Javadoc)
	 * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
		Log.d(TAG, "surfaceCreated");
		RefreshActiveScreenInfo();
	}

	/* (non-Javadoc)
	 * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

		Log.d(TAG, "surfaceDestroyed");
		
		activeRect = null;
		activeRectHash = 0;
	}
	
	/**
	 * Refreshes active rectangle and screen info
	 */
	public void RefreshActiveScreenInfo()
	{
		pause();
		Rect rect = holder.getSurfaceFrame();
		if (rect != null && rect.width() != 0 && rect.height() != 0)
		{
			AndroidExtensions.Log(LogType.Debug, TAG, "RefreshActiveScreenInfo: [%d, %d]", rect.width(), rect.height());
			
			activeRect = new Rect(rect);
			int rasterSize = puzzleActivity.getRasterSize();
			activeRectHash = hashRectSize(activeRect, rasterSize);
			if (!viewMap.containsKey(activeRectHash))
			{
				viewMap.put(activeRectHash, new ScreenInfo(bitmap, activeRect.width(), activeRect.height(), rasterSize));
			}
			
			AndroidExtensions.Log(LogType.Debug, TAG, "view contains %d items", viewMap.size());
		}
		
		resume();
	}
	
	/**
	 * On touch event handling.
	 *
	 * @param v The view sending the event.
	 * @param m The motion event descriptor.
	 * @return true, if handled successfully 
	 */
	public boolean onTouch(View v, MotionEvent m) {
		// printSamples(m);
		if (!puzzleActivity.getBoard().isFinished())
		{
			switch (m.getActionMasked())
			{
				case MotionEvent.ACTION_DOWN: handleTouchDown(m); 
					break;
				case MotionEvent.ACTION_MOVE: handleTouchMove(m);
					break;
				case MotionEvent.ACTION_CANCEL: handleTouchCancel(m);
					break;
				case MotionEvent.ACTION_UP: 
				case MotionEvent.ACTION_POINTER_UP: handleTouchPointerUp(m);
				break;
			}
		}
		return true;
	}
	
	/**
	 * Hashes rectangle size into integer.
	 *
	 * @param rect the rectangle to be hashed
	 * @param level The level to be used for hashing
	 * @return hashed size of the rectangle in string
	 */
	private int hashRectSize(Rect rect, int level)
	{
		assert rect != null;
		assert level > 0;
		
		return rect.width() * RECT_HASH_MULTIPLIER + rect.height() + level;
	}
	
	/**
	 * Draws current board state visual representation to the canvas. 
	 * @param canvas The canvas to draw into.
	 * @param scrInfo Visual content related to current screen layout. 
	 * @param board The game board providing current state of the game.
	 */
	private void drawScreen(Canvas canvas, ScreenInfo scrInfo, PuzzleBoard board)
	{
		assert canvas != null;
		assert scrInfo != null;
		assert board != null;
		
		canvas.drawColor(Color.BLACK);

		// canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()) , scrInfo.targetRect, null);
		for (int i = 0; i < scrInfo.parts; i++)
		{
			for(int j = 0; j < scrInfo.parts; j++)
			{
				int bmpCompositeIndex = board.getPuzzleIndexAt(i, j);
				assert bmpCompositeIndex >= 0 || bmpCompositeIndex < Math.pow(scrInfo.parts, 2) || bmpCompositeIndex == PuzzleBoard.EMPTY_FIELD_IDX;
				
				// empty field is left black
				if (bmpCompositeIndex != PuzzleBoard.EMPTY_FIELD_IDX)
				{
					Bitmap bmpPart = scrInfo.bmpParts[bmpCompositeIndex / scrInfo.parts][bmpCompositeIndex % scrInfo.parts];
					Rect targetPart = scrInfo.targetParts[i][j]; 
					canvas.drawBitmap(bmpPart, targetPart.left, targetPart.top, null);
				}
			}
		}
		
		int horStep = scrInfo.targetRect.width() / scrInfo.parts;
		int verStep = scrInfo.targetRect.height() / scrInfo.parts;
		for (int i = 1; i < scrInfo.parts; i++)
		{
			int vertPos = scrInfo.targetRect.top + i * verStep;
			canvas.drawLine(scrInfo.targetRect.left, vertPos , scrInfo.targetRect.right, vertPos, rasterPaint);
			
			int horPos = scrInfo.targetRect.left + i * horStep;
			canvas.drawLine(horPos, scrInfo.targetRect.top , horPos, scrInfo.targetRect.bottom, rasterPaint);
		}
		
		canvas.drawRect(scrInfo.targetRect, rasterBorderPaint);
		
		if (gestureRectIdx != -1)
		{
			int[] indexes = PuzzleActivity.int2Indexes(gestureRectIdx, scrInfo.parts);
			if (board.isFieldExchangeable(indexes[0], indexes[1]))
			{
				touchPaint.setColor(Color.LTGRAY);
				touchPaint.setAlpha(64);
			}
			else
			{
				touchPaint.setColor(Color.RED);
				touchPaint.setAlpha(96);
			}
			
			canvas.drawRect(scrInfo.targetParts[indexes[0]][indexes[1]], touchPaint);
		}
		
		// make the surface look inactive after game was finished
		if (board.isFinished())
		{
			AndroidExtensions.Log(LogType.Debug, TAG,"Paiting finished state on rect [%d, %d].", scrInfo.targetRect.width(), scrInfo.targetRect.height());
			touchPaint.setColor(Color.LTGRAY);
			touchPaint.setAlpha(64);
			canvas.drawRect(scrInfo.targetRect, touchPaint);
		}
	}
	

	/**
	 * Handls user interaction - user touch up.
	 * @param m Provides details of the touch event.
	 */
	private void handleTouchPointerUp(MotionEvent m) {
		int i = m.getActionIndex();
		
		AndroidExtensions.Log(LogType.Debug, TAG, "Pointer %d [id %d] up -> finishing gesture. gesturePointerId is %d", i, m.getPointerId(i), gesturePointerId);
		
		if (m.getPointerId(i) == gesturePointerId)
		{
			AndroidExtensions.Log(LogType.Debug, TAG, "Pointer %d up -> finishing gesture.", gesturePointerId);
			
			if (activeRect != null)
			{
				int key = activeRectHash;
				if (viewMap.containsKey(key))
				{
					ScreenInfo info = viewMap.get(key);
					assert info != null;
					gestureRectIdx = info.getRectIdxFromPoint((int)m.getX(), (int)m.getY());
					
					
					if (puzzleActivity.getBoard().isFieldExchangeable(gestureRectIdx))
					{
						puzzleActivity.getBoard().exchange(gestureRectIdx);
					}
				}				
			}
			
			gesturePointerId = gestureRectIdx = -1;
		}
	}
	
	/**
	 * Handles cancellation of a gesture.
	 * @param m Details about the cancellation.
	 */
	private void handleTouchCancel(MotionEvent m) {
		gesturePointerId = gestureRectIdx = -1;
	}

	/**
	 * Handles screen touch move. 
	 * @param m Details about the move event.
	 */
	private void handleTouchMove(MotionEvent m) {
		for(int i = 0; i < m.getPointerCount(); i++)
		{
			if (m.getPointerId(i) == gesturePointerId && activeRect != null)
			{
				int key = activeRectHash;
				if (viewMap.containsKey(key))
				{
					ScreenInfo info = viewMap.get(key);
					assert info != null;
					gestureRectIdx = info.getRectIdxFromPoint((int)m.getX(), (int)m.getY());
					break;
				}
			}
		}
		
	}

	/**
	 * Initiates a gesture handling after touch down event.
	 * @param m Details about the touch event.
	 */
	private void handleTouchDown(MotionEvent m) {
		assert m.getPointerCount() == 0;
		gesturePointerId = m.getPointerId(m.getActionIndex());
		if (activeRect != null)
		{
			int key = activeRectHash;
			if (viewMap.containsKey(key))
			{
				AndroidExtensions.Log(LogType.Debug, TAG, "Pointer %d down -> starting gesture.", gesturePointerId);
				ScreenInfo info = viewMap.get(key);
				assert info != null;
				gestureRectIdx = info.getRectIdxFromPoint((int)m.getX(), (int)m.getY());
			}
		}
	}
}
