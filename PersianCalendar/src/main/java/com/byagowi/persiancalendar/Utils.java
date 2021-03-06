package com.byagowi.persiancalendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.azizhuss.arabicreshaper.ArabicShaping;
import com.byagowi.common.Range;
import com.byagowi.persiancalendar.entity.Day;
import com.byagowi.persiancalendar.locale.LocaleUtils;
import com.byagowi.persiancalendar.service.AlarmReceiver;
import com.byagowi.persiancalendar.service.AthanResetReceiver;
import com.github.praytimes.CalculationMethod;
import com.github.praytimes.Clock;
import com.github.praytimes.Coordinate;
import com.github.praytimes.PrayTime;
import com.github.praytimes.PrayTimesCalculator;
import com.github.twaddington.TypefaceSpan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import calendar.AbstractDate;
import calendar.CivilDate;
import calendar.DateConverter;
import calendar.DayOutOfRangeException;
import calendar.IslamicDate;
import calendar.LocaleData;
import calendar.PersianDate;

/**
 * Common utilities that needed for this calendar
 *
 * @author ebraminio
 */
public class Utils {
    private static final String TAG = "Utils";
    private static Utils myInstance;
    private LocaleUtils localeUtils;
    private static boolean athanRepeaterSet = false;

    public static final char PERSIAN_COMMA = '،';
    public static final char[] arabicIndicDigits = {'٠', '١', '٢', '٣', '٤', '٥',
            '٦', '٧', '٨', '٩'};
    public static final String[] firstCharOfDaysOfWeekName = {"ش", "ی", "د", "س",
            "چ", "پ", "ج"};
    private static final char[] arabicDigits = {'0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9'};
    private static final char[] persianDigits = {'۰', '۱', '۲', '۳', '۴', '۵', '۶',
            '۷', '۸', '۹'};
    private String AM_IN_PERSIAN = "ق.ظ";
    private String PM_IN_PERSIAN = "ب.ظ";

    private Typeface typeface;
    private int[] daysIcons = {0,
            R.drawable.day1, R.drawable.day2, R.drawable.day3,
            R.drawable.day4, R.drawable.day5, R.drawable.day6,
            R.drawable.day7, R.drawable.day8, R.drawable.day9,
            R.drawable.day10, R.drawable.day11, R.drawable.day12,
            R.drawable.day13, R.drawable.day14, R.drawable.day15,
            R.drawable.day16, R.drawable.day17, R.drawable.day18,
            R.drawable.day19, R.drawable.day20, R.drawable.day21,
            R.drawable.day22, R.drawable.day23, R.drawable.day24,
            R.drawable.day25, R.drawable.day26, R.drawable.day27,
            R.drawable.day28, R.drawable.day29, R.drawable.day30,
            R.drawable.day31};

    private List<Event> holidays;
    private List<Event> events;
    private PrayTimesCalculator prayTimesCalculator;
    private Map<PrayTime, Clock> prayTimes;

    private Utils() {
    }

    public static Utils getInstance() {
        if (myInstance == null) {
            myInstance = new Utils();
        }
        return myInstance;
    }

    public static String textShaper(String text) {
        return (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) ? ArabicShaping.shape(text) : text;
    }

    public String getString(String key) {
        return localeUtils == null ? "" : textShaper(localeUtils.getString(key));
    }

    public String programVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            Log.e(context.getPackageName(),
                    "Name not found on PersianCalendarUtils.programVersion");
        }
        return "";
    }

    private void initTypeface(Context context) {
        if (typeface == null) {
            typeface = Typeface.createFromAsset(context.getAssets(),
                    "fonts/NotoNaskhArabic-Regular.ttf");
        }
    }

    public void prepareTextView(Context context, TextView textView) {
        initTypeface(context);
        textView.setTypeface(typeface);
        // textView.setLineSpacing(0f, 0.8f);
    }

    public void prepareShapeTextView(Context context, TextView textView) {
        prepareTextView(context, textView);
        textView.setText(textShaper(textView.getText().toString()));
    }

    public void setActivityTitleAndSubtitle(Activity activity, String title, String subtitle) {
        initTypeface(activity);

        //noinspection ConstantConditions
        ActionBar supportActionBar = ((AppCompatActivity) activity).getSupportActionBar();

        SpannableString titleSpan = new SpannableString(textShaper(title));
        titleSpan.setSpan(new TypefaceSpan(typeface), 0, titleSpan.length(), 0);
        titleSpan.setSpan(new RelativeSizeSpan(0.8f), 0, titleSpan.length(), 0);
        supportActionBar.setTitle(titleSpan);

        SpannableString subtitleSpan = new SpannableString(textShaper(subtitle));
        subtitleSpan.setSpan(new TypefaceSpan(typeface), 0, subtitleSpan.length(), 0);
        subtitleSpan.setSpan(new RelativeSizeSpan(0.8f), 0, subtitleSpan.length(), 0);
        supportActionBar.setSubtitle(subtitleSpan);
    }

    public CalculationMethod getCalculationMethod(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        return CalculationMethod.valueOf(prefs.getString("PrayTimeMethod",
                "Jafari")); // Seems Iran is using Jafari method
    }

    public static int getIslamicOffset(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        return Integer.parseInt(prefs.getString("islamicOffset", "0"));
    }

    public Coordinate getCoordinate(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        String location = prefs.getString("Location", "CUSTOM");
        if (!location.equals("CUSTOM")) {
            City city = getCityByKey(location, context);
            return new Coordinate(city.latitude, city.longitude);
        }

        try {
            Coordinate coord = new Coordinate(Double.parseDouble(prefs
                    .getString("Latitude", "0")), Double.parseDouble(prefs
                    .getString("Longitude", "0")), Double.parseDouble(prefs
                    .getString("Altitude", "0")));

            // If latitude or longitude is zero probably preference not set yet
            if (coord.getLatitude() == 0 && coord.getLongitude() == 0) {
                return null;
            }

            return coord;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public char[] preferredDigits(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("PersianDigits", true) ? persianDigits : arabicDigits;
    }

    public static boolean isPersianDigitSelected(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("PersianDigits", true);
    }

    public boolean clockIn24(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return prefs.getBoolean("WidgetIn24", true);
    }

    public static PersianDate getToday() {
        CivilDate civilDate = new CivilDate();
        return DateConverter.civilToPersian(civilDate);
    }

    public List<Day> getDays(Context context, int offset) {
        List<Day> days = new ArrayList<>();
        PersianDate persianDate = getToday();
        int month = persianDate.getMonth() - offset;
        month -= 1;
        int year = persianDate.getYear();

        year = year + (month / 12);
        month = month % 12;
        if (month < 0) {
            year -= 1;
            month += 12;
        }
        month += 1;
        persianDate.setMonth(month);
        persianDate.setYear(year);
        persianDate.setDayOfMonth(1);

        char[] digits = preferredDigits(context);

        int dayOfWeek = DateConverter.persianToCivil(persianDate)
                .getDayOfWeek() % 7;

        try {
            PersianDate today = getToday();
            for (int i = 1; i <= 31; i++) {
                persianDate.setDayOfMonth(i);

                Day day = new Day();
                day.setNum(Utils.formatNumber(i, digits));
                day.setDayOfWeek(dayOfWeek);

                String holidayTitle = getHolidayTitle(persianDate);
                if (holidayTitle != null || dayOfWeek == 6) {
                    day.setHoliday(true);
                }

                String eventTitle = getEventTitle(persianDate);
                if (!TextUtils.isEmpty(eventTitle) || holidayTitle != null ) {
                    day.setEvent(true);
                }

                day.setPersianDate(persianDate.clone());

                if (persianDate.equals(today)) {
                    day.setToday(true);
                }

                days.add(day);
                dayOfWeek++;
                if (dayOfWeek == 7) {
                    dayOfWeek = 0;
                }
            }
        } catch (DayOutOfRangeException e) {
            // okay, it was expected
        }

        return days;
    }

    public Calendar makeCalendarFromDate(Date date, boolean iranTime) {
        Calendar calendar = Calendar.getInstance();
        if (iranTime) {
            calendar.setTimeZone(TimeZone.getTimeZone("Asia/Tehran"));
        }
        calendar.setTime(date);
        return calendar;
    }

    public String clockToString(Clock clock, char[] digits) {
        return clockToString(clock.getHour(), clock.getMinute(), digits);
    }

    public String clockToString(int hour, int minute, char[] digits) {
        return formatNumber(
                String.format(Locale.ENGLISH, "%d:%02d", hour, minute), digits);
    }

    public String getNextOghatTime(Context context, Clock clock, boolean changeDate) {
        Coordinate coordinate = getCoordinate(context);

        if (coordinate != null) {
            char[] digits = preferredDigits(context);
            boolean clockIn24 = clockIn24(context);

            if (prayTimesCalculator == null) {
                prayTimesCalculator = new PrayTimesCalculator(getCalculationMethod(context));
                changeDate = true;
            }

            if (changeDate) {
                CivilDate civilDate = DateConverter.persianToCivil(Utils.getToday());
                Calendar calendar = Calendar.getInstance();
                calendar.set(civilDate.getYear(), civilDate.getMonth() - 1, civilDate.getDayOfMonth());
                Date date = calendar.getTime();
                prayTimes = prayTimesCalculator.calculate(date, coordinate);
            }

            if (prayTimes.get(PrayTime.IMSAK).getInt() > clock.getInt()) {
                return context.getString(R.string.azan1) + ": " + getPersianFormattedClock(prayTimes.get(PrayTime.IMSAK), digits, clockIn24);

            } else if (prayTimes.get(PrayTime.SUNRISE).getInt() > clock.getInt()) {
                return context.getString(R.string.aftab1) + ": " + getPersianFormattedClock(prayTimes.get(PrayTime.SUNRISE), digits, clockIn24);

            } else if (prayTimes.get(PrayTime.DHUHR).getInt() > clock.getInt()) {
                return context.getString(R.string.azan2) + ": " + getPersianFormattedClock(prayTimes.get(PrayTime.DHUHR), digits, clockIn24);

            } else if (prayTimes.get(PrayTime.ASR).getInt() > clock.getInt()) {
                return context.getString(R.string.azan3) + ": " + getPersianFormattedClock(prayTimes.get(PrayTime.ASR), digits, clockIn24);

            } else if (prayTimes.get(PrayTime.SUNSET).getInt() > clock.getInt()) {
                return context.getString(R.string.aftab2) + ": " + getPersianFormattedClock(prayTimes.get(PrayTime.SUNSET), digits, clockIn24);

            } else if (prayTimes.get(PrayTime.MAGHRIB).getInt() > clock.getInt()) {
                return context.getString(R.string.azan4) + ": " + getPersianFormattedClock(prayTimes.get(PrayTime.MAGHRIB), digits, clockIn24);

            } else if (prayTimes.get(PrayTime.ISHA).getInt() > clock.getInt()) {
                return context.getString(R.string.azan5) + ": " + getPersianFormattedClock(prayTimes.get(PrayTime.ISHA), digits, clockIn24);

            } else if (prayTimes.get(PrayTime.MIDNIGHT).getInt() > clock.getInt()) {
                return context.getString(R.string.aftab3) + ": " + getPersianFormattedClock(prayTimes.get(PrayTime.MIDNIGHT), digits, clockIn24);

            } else {
                return context.getString(R.string.azan1) + ": " + getPersianFormattedClock(prayTimes.get(PrayTime.IMSAK), digits, clockIn24); //this is today & not tomorrow
            }

        } else return null;
    }

    public String getPersianFormattedClock(Clock clock, char[] digits, boolean in24) {
        String timeText = null;

        int hour = clock.getHour();
        if (!in24) {
            if (hour >= 12) {
                timeText = PM_IN_PERSIAN;
                hour -= 12;
            } else {
                timeText = AM_IN_PERSIAN;
            }
        }

        String result = clockToString(hour, clock.getMinute(), digits);
        if (!in24) {
            result = result + " " + timeText;
        }
        return result;
    }

    public String getPersianFormattedClock(Calendar calendar, char[] digits, boolean in24) {
        String timeText = null;

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (!in24) {
            if (calendar.get(Calendar.HOUR_OF_DAY) >= 12) {
                timeText = PM_IN_PERSIAN;
                hour -= 12;
            } else {
                timeText = AM_IN_PERSIAN;
            }
        }

        String result = clockToString(hour, calendar.get(Calendar.MINUTE),
                digits);
        if (!in24) {
            result = result + " " + timeText;
        }
        return result;
    }

    public static String formatNumber(int number, char[] digits) {
        return formatNumber(Integer.toString(number), digits);
    }

    public static String formatNumber(String number, char[] digits) {
        if (digits == arabicDigits)
            return number;

        StringBuilder sb = new StringBuilder();
        for (char i : number.toCharArray()) {
            if (Character.isDigit(i)) {
                sb.append(digits[Integer.parseInt(i + "")]);
            } else {
                sb.append(i);
            }
        }
        return sb.toString();
    }

    public String dateToString(AbstractDate date, char[] digits) {
        return formatNumber(date.getDayOfMonth(), digits) + ' '
                + getMonthName(date) + ' '
                + formatNumber(date.getYear(), digits);
    }

    public String dayTitleSummary(PersianDate persianDate, char[] digits) {
        return getWeekDayName(persianDate) + PERSIAN_COMMA + " "
                + dateToString(persianDate, digits);
    }

    public String getMonthYearTitle(PersianDate persianDate, char[] digits) {
        return textShaper(getMonthName(persianDate) + ' '
                + formatNumber(persianDate.getYear(), digits));
    }

    public String getMonthName(AbstractDate date) {
        String monthName = "";
        // zero based
        int month = date.getMonth() - 1;

        if (date.getClass().equals(PersianDate.class)) {
            LocaleData.PersianMonthNames monthNameCode = LocaleData.PersianMonthNames.values()[month];
            monthName = getString(String.valueOf(monthNameCode));
        } else if (date.getClass().equals(CivilDate.class)) {
            LocaleData.CivilMonthNames monthNameCode = LocaleData.CivilMonthNames.values()[month];
            monthName = getString(String.valueOf(monthNameCode));
        } else if (date.getClass().equals(IslamicDate.class)) {
            LocaleData.IslamicMonthNames monthNameCode = LocaleData.IslamicMonthNames.values()[month];
            monthName = getString(String.valueOf(monthNameCode));
        }

        return monthName;
    }

    public List<String> getMonthNameList(AbstractDate date) {
        AbstractDate dateClone = date.clone();
        List<String> monthNameList = new ArrayList<>();
        for (int month : new Range(1, 12)) {
            dateClone.setMonth(month);
            monthNameList.add(textShaper(getMonthName(dateClone)));
        }
        return monthNameList;
    }

    public String getWeekDayName(AbstractDate date) {
        CivilDate civilDate;
        if (date.getClass().equals(PersianDate.class)) {
            civilDate = DateConverter.persianToCivil((PersianDate) date);
        } else if (date.getClass().equals(IslamicDate.class)) {
            civilDate = DateConverter.islamicToCivil((IslamicDate) date);
        } else {
            civilDate = (CivilDate) date;
        }

        // zero based
        int dayOfWeek = civilDate.getDayOfWeek() - 1;
        LocaleData.WeekDayNames weekDayNameCode = LocaleData.WeekDayNames.values()[dayOfWeek];

        return getString(weekDayNameCode.toString());
    }

    public void quickToast(String message, Context context) {
        Toast.makeText(context, textShaper(message), Toast.LENGTH_SHORT).show();
    }

    public int getDayIconResource(int day) {
        try {
            return daysIcons[day];
        } catch (IndexOutOfBoundsException e) {
            Log.e("com.byagowi.calendar", "No such field is available");
            return 0;
        }
    }

    static String convertStreamToString(InputStream is) {
        // http://stackoverflow.com/a/5445161
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static class City {
        public final String key;
        public final String en;
        public final String fa;
        public final String countryCode;
        public final String countryEn;
        public final String countryFa;
        public final double latitude;
        public final double longitude;
        public City(String key, String en, String fa, String countryCode, String countryEn,
                    String countryFa, double latitude, double longitude) {
            this.key = key;
            this.en = en;
            this.fa = fa;
            this.countryCode = countryCode;
            this.countryEn = countryEn;
            this.countryFa = countryFa;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static String persianStringToArabic(String text) {
        return text
            .replaceAll("ی", "ي")
            .replaceAll("ک", "ك")
            .replaceAll("گ", "كی")
            .replaceAll("ژ", "زی")
            .replaceAll("چ", "جی")
            .replaceAll("پ", "بی");
    }

    static private City[] cities;

    static private void loadCities(InputStream is, Context context) {
        ArrayList<City> result = new ArrayList<>();
        try {
            JSONObject countries = new JSONObject(convertStreamToString(is));

            Iterator<String> countryIterator = countries.keys();
            while (countryIterator.hasNext()) {
                String countryCode = countryIterator.next();
                JSONObject country = countries.getJSONObject(countryCode);

                String countryEn = country.getString("en");
                String countryFa = country.getString("fa");

                JSONObject cities = country.getJSONObject("cities");

                Iterator<String> citiesIterator = cities.keys();
                while (citiesIterator.hasNext()) {
                    String key = citiesIterator.next();
                    JSONObject city = cities.getJSONObject(key);

                    String en = city.getString("en");
                    String fa = city.getString("fa");

                    double lat = city.getDouble("latitude");
                    double lon = city.getDouble("longitude");

                    result.add(new City(key, en, fa, countryCode, countryEn, countryFa, lat, lon));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String locale = prefs.getString("AppLanguage", "fa");

        City[] cities = result.toArray(new City[result.size()]);

        // Sort first by country code then city
        Arrays.sort(cities, new Comparator<City>() {
            @Override
            public int compare(City l, City r) {
                if (l.key.equals("CUSTOM")) { return -1; }
                if (r.key.equals("CUSTOM")) { return 1; }
                int compare = r.countryCode.compareTo(l.countryCode);
                return compare != 0
                        ? compare
                        : (locale.equals("en")
                            ? l.en.compareTo(r.en)
                            : persianStringToArabic(l.fa).compareTo(persianStringToArabic(r.fa)));
            }
        });

        Utils.cities = cities;
    }

    public static City[] getAllCities(Context context) {
        if (cities == null) {
            loadCities(context.getResources().openRawResource(R.raw.cities), context);
        }

        return cities;
    }

    public static String cachedCityKey = "";
    public static City cachedCity;

    public static City getCityByKey(String key, Context context) {
        if (cachedCity != null && key.equals(cachedCityKey)) {
            return cachedCity;
        }

        if (cities == null) {
            loadCities(context.getResources().openRawResource(R.raw.cities), context);
        }

        for (City city : cities)
            if (city.key.equals(key)) {
                cachedCity = city;
                cachedCityKey = key;
                return city;
            }

        return null;
    }

    private ArrayList<Event> readEventsFromJSON(InputStream is) {
        ArrayList<Event> result = new ArrayList<>();
        try {
            JSONArray days = new JSONObject(convertStreamToString(is)).getJSONArray("events");

            int length = days.length();
            for (int i = 0; i < length; ++i) {
                JSONObject event = days.getJSONObject(i);

                int year = event.getInt("year");
                int month = event.getInt("month");
                int day = event.getInt("day");
                String title = event.getString("title");

                result.add(new Event(new PersianDate(year, month, day), title));
            }

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        return result;
    }

    public void loadHolidays(InputStream is) {
        holidays = readEventsFromJSON(is);
    }

    public void loadEvents(InputStream is) {
        events = readEventsFromJSON(is);
    }

    public String getHolidayTitle(PersianDate day) {
        for (Event holiday : holidays) {
            if (holiday.getDate().equals(day)) {
                return holiday.getTitle();
            }
        }
        return null;
    }

    public String getEventTitle(PersianDate day) {
        String eventsTitle = "";
        boolean first = true;
        for (Event event : events) {
            if (event.getDate().equals(day)) {

                if (first) {
                    first = false;
                } else {
                    eventsTitle = eventsTitle + "\n";
                }

                // trim XML whitespaces and newlines
                eventsTitle = eventsTitle + event.getTitle().replaceAll("\n", "").trim();

            }
        }
        return  eventsTitle;
    }

    public void setAthanRepeater(Context context) {
        Log.d(TAG, "athan repeater set: " + athanRepeaterSet);
        // load them so the prefs are read for today's alarms
        loadAlarms(context);

        if (!athanRepeaterSet) {

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Calendar repeatTime = Calendar.getInstance();
            repeatTime.set(Calendar.HOUR_OF_DAY, 0);
            repeatTime.set(Calendar.MINUTE, 0);
            Intent intent = new Intent(context, AthanResetReceiver.class);
            PendingIntent repeatIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.setInexactRepeating(AlarmManager.RTC, repeatTime.getTimeInMillis(), (24 * 60 * 60 * 1000), repeatIntent);

            athanRepeaterSet = true;
        }
    }

    public void loadAlarms(Context context) {
        Log.d(TAG, "reading and loading all alarms from prefs");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String prefString = prefs.getString("AthanAlarm", "");
        CalculationMethod calculationMethod = getCalculationMethod(context);
        Coordinate coordinate = getCoordinate(context);
        if (calculationMethod != null && coordinate != null && !TextUtils.isEmpty(prefString)) {
            PrayTimesCalculator calculator = new PrayTimesCalculator(calculationMethod);
            Map<PrayTime, Clock> prayTimes = calculator.calculate(new Date(), coordinate);

            String[] alarmTimesNames = TextUtils.split(prefString, ",");
            for (String prayerName : alarmTimesNames) {
                Clock alarmTime = prayTimes.get(PrayTime.valueOf(prayerName));

                if (alarmTime != null) {
                    setAlarm(context, PrayTime.valueOf(prayerName), alarmTime);
                }
            }
        }
    }

    public void setAlarm(Context context, PrayTime prayTime, Clock clock) {
        Calendar triggerTime = Calendar.getInstance();
        triggerTime.set(Calendar.HOUR_OF_DAY, clock.getHour());
        triggerTime.set(Calendar.MINUTE, clock.getMinute());
        setAlarm(context, prayTime, triggerTime.getTimeInMillis());
    }

    public void setAlarm(Context context, PrayTime prayTime, long timeInMillis) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String valAthanGap = prefs.getString("AthanGap", "0");
        long athanGap = TextUtils.isEmpty(valAthanGap) ? 0 : Long.parseLong(valAthanGap);

        Calendar triggerTime = Calendar.getInstance();
        triggerTime.setTimeInMillis(timeInMillis - TimeUnit.SECONDS.toMillis(athanGap));
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // don't set an alarm in the past
        if (!triggerTime.before(Calendar.getInstance())) {
            Log.d(TAG, "setting alarm for: " + triggerTime.getTime());

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra(AlarmReceiver.KEY_EXTRA_PRAYER_KEY, prayTime.name());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime.getTimeInMillis(), pendingIntent);
            }
        }
    }

    public Uri getAthanUri(Context context) {
        String defaultSoundUri = "android.resource://" + context.getPackageName() + "/" + R.raw.abdulbasit;
        return Uri.parse(defaultSoundUri);
    }

    public void changeAppLanguage(String localeCode, Context context) {
        Locale locale = TextUtils.isEmpty(localeCode) ? Locale.getDefault() : new Locale(localeCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        Resources resources = context.getApplicationContext().getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    public void changeCalendarLanguage(String localeCode, Context context) {
        if (localeUtils == null) {
            localeUtils = LocaleUtils.getInstance(context, localeCode);
        }

        localeUtils.changeLocale(localeCode);
    }

    public String loadLanguageFromSettings(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // set app language
        String locale = prefs.getString("AppLanguage", "fa");
        changeAppLanguage(locale.replaceAll("-(IR|AF)", ""), context);
        changeCalendarLanguage(locale, context);
        return locale;
    }
}
