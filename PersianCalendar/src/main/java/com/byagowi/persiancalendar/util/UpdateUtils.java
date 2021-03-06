package com.byagowi.persiancalendar.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import com.byagowi.persiancalendar.R;
import com.byagowi.persiancalendar.Utils;
import com.byagowi.persiancalendar.Widget2x2;
import com.byagowi.persiancalendar.view.activity.MainActivity;
import com.byagowi.persiancalendar.Widget1x1;
import com.byagowi.persiancalendar.Widget4x1;
import com.github.praytimes.Clock;
import com.google.android.apps.dashclock.api.ExtensionData;

import java.util.Calendar;
import java.util.Date;

import calendar.CivilDate;
import calendar.DateConverter;
import calendar.PersianDate;

public class UpdateUtils {
    private static final int NOTIFICATION_ID = 1001;
    private static UpdateUtils myInstance;
    private final Utils utils = Utils.getInstance();

    //
    private NotificationManager mNotificationManager;
    private Bitmap largeIcon;
    private ExtensionData mExtensionData;

    private UpdateUtils() {
    }

    public static UpdateUtils getInstance() {
        if (myInstance == null) {
            myInstance = new UpdateUtils();
        }
        return myInstance;
    }

    public void update(Context context) {
        utils.loadLanguageFromSettings(context);
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        char[] digits = utils.preferredDigits(context);
        boolean iranTime = prefs.getBoolean("IranTime", false);
        Calendar calendar = utils.makeCalendarFromDate(new Date(), iranTime);
        CivilDate civil = new CivilDate(calendar);
        PersianDate persian = Utils.getToday();

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent launchAppPendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //
        // Widgets
        //
        //
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        RemoteViews remoteViews1 = new RemoteViews(context.getPackageName(),
                R.layout.widget1x1);
        RemoteViews remoteViews4 = new RemoteViews(context.getPackageName(),
                R.layout.widget4x1);
        RemoteViews remoteViews2 = new RemoteViews(context.getPackageName(),
                R.layout.widget2x2);
        String colorInt = prefs.getString("SelectedWidgetTextColor",
                context.getString(R.string.default_widget_text_color));
        int color = Color.parseColor(colorInt);

        // Widget 1x1
        remoteViews1.setTextColor(R.id.textPlaceholder1_1x1, color);
        remoteViews1.setTextColor(R.id.textPlaceholder2_1x1, color);
        remoteViews1.setTextViewText(R.id.textPlaceholder1_1x1,
                Utils.formatNumber(persian.getDayOfMonth(), digits));
        remoteViews1.setTextViewText(R.id.textPlaceholder2_1x1,
                Utils.textShaper(utils.getMonthName(persian)));
        remoteViews1.setOnClickPendingIntent(R.id.widget_layout1x1,
                launchAppPendingIntent);
        manager.updateAppWidget(new ComponentName(context, Widget1x1.class),
                remoteViews1);

        // Widget 4x1
        remoteViews4.setTextColor(R.id.textPlaceholder1_4x1, color);
        remoteViews4.setTextColor(R.id.textPlaceholder2_4x1, color);
        remoteViews4.setTextColor(R.id.textPlaceholder3_4x1, color);

        String text1;
        String text2;
        String text3 = "";
        String weekDayName = utils.getWeekDayName(civil);
        String persianDate = utils.dateToString(persian, digits);
        String civilDate = utils.dateToString(civil, digits);
        String date = persianDate + Utils.PERSIAN_COMMA + " " + civilDate;

        boolean in24 = prefs.getBoolean("WidgetIn24", true);
        String time = utils.getPersianFormattedClock(calendar, digits, in24);
        boolean enableClock = prefs.getBoolean("WidgetClock", true);

        if (enableClock) {
            text2 = weekDayName + " " + date;
            text1 = time;
            if (iranTime) {
                text3 = "(" + context.getString(R.string.iran_time) + ")";
            }
        } else {
            text1 = weekDayName;
            text2 = date;
        }

        remoteViews4.setTextViewText(R.id.textPlaceholder1_4x1,
                Utils.textShaper(text1));
        remoteViews4.setTextViewText(R.id.textPlaceholder2_4x1,
                Utils.textShaper(text2));
        remoteViews4.setTextViewText(R.id.textPlaceholder3_4x1,
                Utils.textShaper(text3));

        remoteViews4.setOnClickPendingIntent(R.id.widget_layout4x1,
                launchAppPendingIntent);
        manager.updateAppWidget(new ComponentName(context, Widget4x1.class),
                remoteViews4);


        // Widget 2x2
        remoteViews2.setTextColor(R.id.time_2x2, color);
        remoteViews2.setTextColor(R.id.date_2x2, color);
        remoteViews2.setTextColor(R.id.event_2x2, color);
        remoteViews2.setTextColor(R.id.owghat_2x2, color);

        if (enableClock) {
            text2 = weekDayName + " " + persianDate;
            text1 = time;
        } else {
            text1 = weekDayName;
            text2 = persianDate;
        }

        Clock currentClock = new Clock(
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));

        String owghat = utils.getNextOghatTime(context, currentClock, false);
        if (owghat != null) {
            remoteViews2.setTextViewText(R.id.owghat_2x2,
                    Utils.textShaper(owghat));
            remoteViews2.setViewVisibility(R.id.owghat_2x2, View.VISIBLE);
        } else {
            remoteViews2.setViewVisibility(R.id.owghat_2x2, View.GONE);
        }

        remoteViews2.setTextViewText(R.id.time_2x2,
                Utils.textShaper(text1));
        remoteViews2.setTextViewText(R.id.date_2x2,
                Utils.textShaper(text2));

        remoteViews2.setOnClickPendingIntent(R.id.widget_layout2x2,
                launchAppPendingIntent);
        manager.updateAppWidget(new ComponentName(context, Widget2x2.class),
                remoteViews2);

        //
        // Permanent Notification Bar and DashClock Data Extension Update
        //
        //
        String status = utils.getMonthName(persian);

        String title = utils.getWeekDayName(civil) + " "
                + utils.dateToString(persian, digits);

        String body = utils.dateToString(civil, digits)
                + Utils.PERSIAN_COMMA
                + " "
                + utils.dateToString(
                DateConverter.civilToIslamic(
                        civil, Utils.getIslamicOffset(context)),
                digits);

        int icon = utils.getDayIconResource(persian.getDayOfMonth());

        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
        }
        if (prefs.getBoolean("NotifyDate", true)) {
            if (largeIcon == null)
                largeIcon = BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.launcher_icon);

            mNotificationManager.notify(
                    NOTIFICATION_ID,
                    new NotificationCompat
                            .Builder(context)
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setOngoing(true)
                            .setLargeIcon(largeIcon)
                            .setSmallIcon(icon)
                            .setWhen(0)
                            .setContentIntent(launchAppPendingIntent)
                            .setContentText(Utils.textShaper(body))
                            .setContentTitle(Utils.textShaper(title))
                            .build());
        } else {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }

        mExtensionData = new ExtensionData().visible(true).icon(icon)
                .status(Utils.textShaper(status))
                .expandedTitle(Utils.textShaper(title))
                .expandedBody(Utils.textShaper(body)).clickIntent(intent);
    }


    public void updateDate(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        utils.loadHolidays(context.getResources().openRawResource(R.raw.holidays));
        utils.loadEvents(context.getResources().openRawResource(R.raw.events));

        RemoteViews remoteViews2 = new RemoteViews(context.getPackageName(),
                R.layout.widget2x2);

        String colorInt = prefs.getString("WidgetTextColor",
                context.getString(R.string.default_widget_text_color));
        int color = Color.parseColor(colorInt);

        boolean enableClock = prefs.getBoolean("WidgetClock", true);

        remoteViews2.setTextColor(R.id.time_2x2, color);
        remoteViews2.setTextColor(R.id.date_2x2, color);
        remoteViews2.setTextColor(R.id.event_2x2, color);
        remoteViews2.setTextColor(R.id.owghat_2x2, color);

        String text1;
        String text2;

        boolean iranTime = prefs.getBoolean("IranTime", false);
        Calendar calendar = utils.makeCalendarFromDate(new Date(), iranTime);
        CivilDate civil = new CivilDate(calendar);
        PersianDate persian = Utils.getToday();
        char[] digits = utils.preferredDigits(context);
        String persianDate = utils.dateToString(persian, digits);
        boolean in24 = prefs.getBoolean("WidgetIn24", true);
        String time = utils.getPersianFormattedClock(calendar, digits, in24);

        String weekDayName = utils.getWeekDayName(civil);

        if (enableClock) {
            text2 = weekDayName + " " + persianDate;
            text1 = time;
        } else {
            text1 = weekDayName;
            text2 = persianDate;
        }

        Clock currentClock = new Clock(
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));

        String owghat = utils.getNextOghatTime(context, currentClock, true);
        if (owghat != null) {
            remoteViews2.setTextViewText(R.id.owghat_2x2,
                    Utils.textShaper(owghat));
            remoteViews2.setViewVisibility(R.id.owghat_2x2, View.VISIBLE);
        } else {
            remoteViews2.setViewVisibility(R.id.owghat_2x2, View.GONE);
        }

        String holidays = utils.getHolidayTitle(persian);

        if (!TextUtils.isEmpty(holidays)) {
            remoteViews2.setTextViewText(R.id.holiday_2x2,
                    Utils.textShaper(holidays));
            remoteViews2.setViewVisibility(R.id.holiday_2x2, View.VISIBLE);
        } else {
            remoteViews2.setViewVisibility(R.id.holiday_2x2, View.GONE);
        }

        String events = utils.getEventTitle(persian);

        if (!TextUtils.isEmpty(events)) {
            remoteViews2.setTextViewText(R.id.event_2x2,
                    Utils.textShaper(events));
            remoteViews2.setViewVisibility(R.id.event_2x2, View.VISIBLE);
        } else {
            remoteViews2.setViewVisibility(R.id.event_2x2, View.GONE);
        }

        remoteViews2.setTextViewText(R.id.time_2x2,
                Utils.textShaper(text1));
        remoteViews2.setTextViewText(R.id.date_2x2,
                Utils.textShaper(text2));


        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent launchAppPendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        remoteViews2.setOnClickPendingIntent(R.id.widget_layout2x2,
                launchAppPendingIntent);

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(new ComponentName(context, Widget2x2.class),
                remoteViews2);


    }

    public ExtensionData getExtensionData() {
        return mExtensionData;
    }

}
