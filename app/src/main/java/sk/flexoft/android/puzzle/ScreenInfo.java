package sk.flexoft.android.puzzle;

import sk.flexoft.android.puzzle.util.AndroidExtensions;
import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * @author Vladimir Iszer
 * Encapsulates information related to a screen layout. 
 */
public class ScreenInfo {
	
	/** The width of the screen	 */
	int screenWidth;
	
	/** The height of the screen	 */
	int screenHeight;
	
	/** Rectangle the bitmap has to fit in	 */
	Rect targetRect;
	
	/** Individual small Bitmaps composing together the original bitmap */
	Bitmap[][] bmpParts;
	
	/** Individual small rectangles composing together the target rectangle */
	Rect[][] targetParts;
	
	/** Amount of parts along one square side the bitmap and screen has to be split to. */
	int parts;
	
	/**
	 * The constructor.
	 * @param bitmap - The original bitmap to be split
	 * @param scrWidth The screen width
	 * @param scrHeight The screen height
	 * @param parts Amount of parts along one square side the bitmap and screen has to be split to. 
	 * @throws IllegalArgumentException
	 */
	public ScreenInfo(Bitmap bitmap, int scrWidth, int scrHeight, int parts) throws IllegalArgumentException 
	{
		if (bitmap == null)
		{
			throw new IllegalArgumentException("bitmap");
		}
		
		if (parts < 1)
		{
			throw new IllegalArgumentException("parts"); 
		}
		
		this.parts = parts;
		screenWidth = scrWidth;
		screenHeight = scrHeight;
		
		int bmpWidth, bmpHeight;
		bmpWidth = bitmap.getWidth();
		bmpHeight = bitmap.getHeight();
		
		if (screenWidth == bmpWidth && screenHeight == bmpHeight)
		{
			targetRect = new Rect(0, 0, bmpWidth, bmpHeight);
		}
		else if (screenWidth >= bmpWidth && screenHeight >= bmpHeight)
		{
			Enlarge(bmpWidth, bmpHeight);
		}
		else
		{
			ScaleDown(bmpWidth, bmpHeight);
		}
		
		if (targetRect.bottom > scrHeight - 1)
		{
			targetRect.bottom = scrHeight - 1;
		}
		
		if (targetRect.right > scrWidth - 1)
		{
			targetRect.right = scrWidth - 1;
		}
		
		InitParts(bitmap);
	}
	
	/**
	 * Gets the rectangle index from point.
	 *
	 * @param x the x
	 * @param y the y
	 * @return The rectangle index from point if the point is inside; otherwise -1
	 */
	public int getRectIdxFromPoint(int x, int y) {

		
		if (targetRect.contains(x, y))
		{
			for(int i = 0; i < parts; i++)
			{
				for(int j = 0; j < parts; j++)
				{
					if (targetParts[i][j].contains(x, y))
					{
						return PuzzleActivity.indexes2Int(i, j, parts);
					}
				}
			}
		}
		
		return -1;
	}

	/**
	 * Initializes raster bitmaps and targets.
	 *
	 * @param bitmap The source bitmap.
	 */
	private void InitParts(Bitmap bitmap) {
		assert bitmap != null;
		
		bmpParts = new Bitmap[parts][parts];
		targetParts = new Rect[parts][parts];
		
		int bmpHorStep = bitmap.getWidth() / parts;
		int bmpVertStep = bitmap.getHeight() / parts;
		
		int partWidth = targetRect.width() / parts;
		int partHeight = targetRect.height() / parts;
		
		for (int i = 0; i < parts; i++)
		{
			for(int j = 0; j < parts; j++)
			{
				bmpParts[i][j] = AndroidExtensions.copyBitmapPart(
						bitmap, 
						new Rect(i * bmpHorStep, j * bmpVertStep, ((i + 1) * bmpHorStep) -1, ((j + 1) * bmpVertStep) -1),
						new Rect(0, 0, partWidth, partHeight));
				
				targetParts[i][j] = new Rect(
						targetRect.left + i * partWidth, 
						targetRect.top + j * partHeight, 
						targetRect.left + (i + 1) * partWidth - 1,
						targetRect.top + (j + 1) * partHeight - 1);
			}
		}
	}

	/**
	 * Scales down a bitmap to fit the current screen.
	 * @param bmpWidth
	 * @param bmpHeight
	 */
	private void ScaleDown(int bmpWidth, int bmpHeight) {
		float widthRatio = (float)bmpWidth /(float)screenWidth;
		float ratio = (float)bmpHeight / (float)screenHeight;
		
		if (ratio < widthRatio)
		{
			ratio = widthRatio;
		}
		
		targetRect = new Rect(0, 0, (int)(bmpWidth / ratio), (int)(bmpHeight / ratio));
		
		if (ratio == widthRatio)
		{
			targetRect.offset(0,  (screenHeight - targetRect.height())/2);
		}
		else
		{
			targetRect.offset((screenWidth - targetRect.width())/2, 0);
		}
	}

	/**
	 * Enlarge bitmap which is smaller than screen to fit the screen
	 *
	 * @param bmpWidth the bitmap width
	 * @param bmpHeight the bitmap height
	 */
	private void Enlarge(int bmpWidth, int bmpHeight) {
		float widthRatio = (float)screenWidth / (float)bmpWidth;
		float ratio = (float)screenHeight / (float)bmpHeight;
		
		if (ratio > widthRatio)
		{
			ratio = widthRatio;
		}
		
		targetRect = new Rect(0, 0, (int)(bmpWidth * ratio), (int)(bmpHeight * ratio));
		
		if (ratio == widthRatio)
		{
			targetRect.offset(0,  (screenHeight - targetRect.height())/2);
		}
		else
		{
			targetRect.offset((screenWidth - targetRect.width())/2, 0);
		}
	}
}
