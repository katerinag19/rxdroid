/**
 * Copyright (C) 2011, 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 * This file is part of RxDroid.
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

package at.caspase.rxdroid;

import java.util.WeakHashMap;

import android.app.Activity;


public class Application extends android.app.Application
{
	private static WeakHashMap<Activity, Boolean> sActivityVisibility =
			new WeakHashMap<Activity, Boolean>();

	@Override
	public void onCreate()
	{
		GlobalContext.set(getApplicationContext());
		Settings.init();
		AutoIntakeCreator.registerSelf();

		super.onCreate();
	}

	public static void setIsVisible(Activity activity, boolean isVisible) {
		sActivityVisibility.put(activity, isVisible);
	}

	public static boolean isUiVisible()
	{
		for(Activity activity : sActivityVisibility.keySet())
		{
			if(sActivityVisibility.get(activity))
				return true;
		}

		return false;
	}
}
