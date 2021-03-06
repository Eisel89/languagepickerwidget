/*
 *  This file is part of Language Picker Widget.
 *
 *  Language Picker Widget is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Language Picker Widget is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Language Picker Widget.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gnvo.langpicker;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

public class LangPicker extends AppWidgetProvider  {
	protected static final String LOG_TAG = "LangPicker";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d(LOG_TAG, "onUpdate");

		super.onUpdate(context, appWidgetManager, appWidgetIds);
		for (int appWidgetId : appWidgetIds){
			prepareAppWidget(context, appWidgetManager, appWidgetId);
		}
	}

	static void prepareAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
		Log.d(LOG_TAG, "prepareAppWidget");
		//TODO, create only one LangPickerDataAdapter object for insert(in the configure) and fetch
		Loc loc = new Loc(new LangPickerDataAdapter().fetchWidgetLanguage(context, appWidgetId));
		if (loc.getLocale() == null)
			return; // Widget created, but not configured yet.
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

		Intent intent = new Intent(context, LocaleChangerReceiver.class);
		intent.putExtra("locale", loc.getIetfLanguageTag());
		// We need to make the this intent unique:
		intent.setData((Uri.parse("custom://langpicker/locale/" + loc.getIetfLanguageTag())));

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.label, pendingIntent);
		
		if (loc.isLocaleCurrentLocale())
			views.setTextColor(R.id.label, R.color.gray);

		views.setTextViewText(R.id.label, loc.twoLinesLanguageCountry());
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}


	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();

		if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			Bundle extras = intent.getExtras();
			final int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] { appWidgetId });
			}
		} else {
			super.onReceive(context, intent);
		}

	} 

	public static class LocaleChangerReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//TODO validate extras
			Bundle extras = intent.getExtras();
			String ietfLanguageTag = extras.getString("locale");

			try {
				IActivityManager am = ActivityManagerNative.getDefault();
				Configuration config = am.getConfiguration();
				
				Loc newLoc = new Loc(ietfLanguageTag);
				config.locale = newLoc.getLocale();

				config.userSetLocale = true;
				am.updateConfiguration(config);

			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}   
}
