/**
 * RxDroid - A Medication Reminder
 * Copyright 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package at.jclehner.rxdroid;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.Intake;


public class RxDroid extends Application
{
	private static final String TAG = RxDroid.class.getSimpleName();

	private static WeakHashMap<Activity, Boolean> sActivityVisibility =
			new WeakHashMap<Activity, Boolean>();

	private static long sUnlockedTime = 0;
	private static volatile WeakReference<Context> sContextRef;
	private static volatile Handler sHandler;

	@Override
	public void onCreate()
	{
		setContext(getApplicationContext());

		Settings.init();
		AutoIntakeCreator.registerSelf();
		Database.registerEventListener(sNotificationUpdater);

		final Configuration config = getResources().getConfiguration();
		final int screenSize = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

		if(screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL)
		{
			if(Settings.getBoolean(Settings.Keys.ENABLE_LANDSCAPE, Settings.Defaults.ENABLE_LANDSCAPE))
			{
				Log.i(TAG, "Small screen detected - disabling landscape mode");
				Settings.putBoolean(Settings.Keys.ENABLE_LANDSCAPE, false);
			}
		}

		super.onCreate();
	}

	public static void setContext(Context context)
	{
		sContextRef = new WeakReference<Context>(context);
		sHandler = new Handler(context.getMainLooper());
	}

	/**
	 * Calls {@link GlobalContext#get(boolean)} with <code>allowNullContext=false</code>.
	 */
	public static Context getContext() {
		return getContext(false);
	}

	public static Context getContext(boolean allowNullContext)
	{
		final Context c = sContextRef.get();
		if(c == null && !allowNullContext)
			throw new IllegalStateException("Context is null");
		return c;
	}

	public static void toastShort(int textResId) {
		toast(textResId, Toast.LENGTH_SHORT);
	}

	public static void toastLong(int textResId) {
		toast(textResId, Toast.LENGTH_LONG);
	}

	public static void runInMainThread(Runnable r)
	{
		if(true)
		{
			sHandler.post(r);
		}
		else
		{
			r.run();
		}
	}

	public static LocalBroadcastManager getLocalBroadcastManager() {
		return LocalBroadcastManager.getInstance(getContext());
	}

	public static void doStartActivity(Intent intent) {
		getContext().startActivity(intent);
	}

	public static void doSendBroadcast(Intent intent) {
		getContext().sendBroadcast(intent);
	}

	public static String getQuantityString(int id, int quantity, Object... formatArgs)
	{
		final Object[] newArgs = new Object[formatArgs.length + 1];
		newArgs[0] = quantity;
		System.arraycopy(formatArgs, 0, newArgs, 1, formatArgs.length);
		return getContext().getResources().getQuantityString(id, quantity, newArgs);
	}

	public static String getQuantityString(int id, int quantity) {
		return getContext().getResources().getQuantityString(id, quantity, quantity);
	}

	public static boolean isLocked()
	{
		final int timeoutSeconds = Settings.getInt(Settings.Keys.LOCKSCREEN_TIMEOUT, 0);
		if(timeoutSeconds == 0)
			return sUnlockedTime == 0;

		final long now = System.currentTimeMillis();
		if(sUnlockedTime > now)
			return true;

		final long diffSeconds = (now - sUnlockedTime) / 1000;
		return diffSeconds >= timeoutSeconds;
	}

	public static void unlock() {
		sUnlockedTime = System.currentTimeMillis();
	}

	public static void setIsVisible(Activity activity, boolean isVisible) {
		sActivityVisibility.put(activity, isVisible);
	}

	public static boolean isUiVisible()
	{
//		for(Activity activity : sActivityVisibility.keySet())
//		{
//			if(sActivityVisibility.get(activity))
//				return true;
//		}

		return false;
	}

	private static void toast(final int textResId, final int duration)
	{
		runInMainThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getContext(), textResId, duration).show();
			}
		});
	}

	private static final Database.OnChangeListener sNotificationUpdater = new Database.OnChangeListener() {

		@Override
		public void onEntryUpdated(Entry entry, int flags) {
			NotificationReceiver.rescheduleAlarmsAndUpdateNotification(entry instanceof Intake);
		}

		@Override
		public void onEntryDeleted(Entry entry, int flags) {
			NotificationReceiver.rescheduleAlarmsAndUpdateNotification(false);
		}

		@Override
		public void onEntryCreated(Entry entry, int flags) {
			NotificationReceiver.rescheduleAlarmsAndUpdateNotification(entry instanceof Intake);
		}
	};
}
