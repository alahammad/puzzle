/**
 * 
 */
package sk.flexoft.android.puzzle.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.Gravity;
import android.widget.Toast;

/**
 * @author Vladimir Iszer
 * The class implements helper methods extending Android related functionality. 
 *
 */
public class AndroidExtensions {

	/**
	 * Defines possible log types.
	 */
	public enum LogType
	{
		Debug,
		Warning,
		Info,
		Error,
		Verbose
	}
	
	/**
	 * Logs formatted message.
	 *
	 * @param type type
	 * @param tag The tag
	 * @param format The format
	 * @param args The objects to be interpreted by the format.
	 */
	public static void Log(LogType type, String tag, String format, Object...args){
		String message = String.format(format, args);
		
		switch (type)
		{
			case Warning: android.util.Log.w(tag, message);break;
			case Info: android.util.Log.i(tag, message);break;
			case Error: android.util.Log.e(tag, message);break;
			case Verbose: android.util.Log.v(tag, message);break;
			default: android.util.Log.d(tag, message);break;
		}
	}
	
	/**
	 * Copy bitmap part into another by making it fit to target size.
	 *
	 * @param source the source
	 * @param sourceRect the source rectangle
	 * @param targetRect the target rectangle
	 * @return the bitmap
	 * @throws NullPointerException the null pointer exception
	 */
	public static Bitmap copyBitmapPart(Bitmap source, Rect sourceRect, Rect targetRect) throws IllegalArgumentException
	{
		if (source == null)
		{
			throw new IllegalArgumentException("source");
		}
		
		if (sourceRect == null)
		{
			throw new IllegalArgumentException("sourceRect");
		}
		
		if (targetRect == null)
		{
			throw new IllegalArgumentException("targetRect");
		}
		
		Bitmap result = Bitmap.createBitmap(targetRect.width(), targetRect.height(), source.getConfig());
		Canvas canvas = new Canvas(result);
		canvas.drawBitmap(source, sourceRect, targetRect, null);
		
		return result;
	}
	
	/**
	 * Shows Toast with provided message centered in the middle of the context.
	 * @param context The message context.
	 * @param messageId The message identifier.
	 * @param lng If true a long Toast is shown; otherwise short one is shown.
	 */
	public static void showCenteredToast(Context context, int messageId, boolean lng)
	{
		Toast toast = Toast.makeText(context, messageId, lng ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
}
