/**
    Copyright (C) 2018 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.forrestguice.suntimeswidget.calculator;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;

import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.AUTHORITY;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_CONFIG_ALTITUDE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_CONFIG_APPTHEME;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_CONFIG_APPWIDGETID;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_CONFIG_LATITUDE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_CONFIG_LOCALE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_CONFIG_LONGITUDE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_CONFIG_TIMEZONE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_MOON_RISE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_MOON_SET;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_ACTUAL_RISE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_ACTUAL_SET;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_ASTRO_RISE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_ASTRO_SET;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_BLUE4_RISE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_BLUE4_SET;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_BLUE8_RISE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_BLUE8_SET;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_CIVIL_RISE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_CIVIL_SET;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_GOLDEN_EVENING;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_GOLDEN_MORNING;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_NAUTICAL_RISE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_NAUTICAL_SET;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SUN_NOON;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.QUERY_CONFIG;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.QUERY_CONFIG_PROJECTION;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.QUERY_MOON;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.QUERY_MOONPHASE;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_MOON_FIRST;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_MOON_FULL;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_MOON_NEW;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_MOON_THIRD;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.QUERY_MOONPHASE_PROJECTION;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.QUERY_MOON_PROJECTION;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.QUERY_SEASONS;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SEASON_AUTUMN;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SEASON_SUMMER;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SEASON_VERNAL;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SEASON_WINTER;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.COLUMN_SEASON_YEAR;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.QUERY_SEASONS_PROJECTION;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.QUERY_SUN;
import static com.forrestguice.suntimeswidget.calculator.CalculatorProviderContract.QUERY_SUN_PROJECTION;

/**
 * CalculatorProvider
 * @see CalculatorProviderContract
 */
public class CalculatorProvider extends ContentProvider
{
    private static final int URIMATCH_CONFIG = 0;

    private static final int URIMATCH_SUN = 10;
    private static final int URIMATCH_SUN_FOR_DATE = 20;
    private static final int URIMATCH_SUN_FOR_RANGE = 30;

    private static final int URIMATCH_MOON = 40;
    private static final int URIMATCH_MOON_FOR_DATE = 50;
    private static final int URIMATCH_MOON_FOR_RANGE = 60;

    private static final int URIMATCH_MOONPHASE = 70;
    private static final int URIMATCH_MOONPHASE_FOR_DATE = 80;
    private static final int URIMATCH_MOONPHASE_FOR_RANGE = 90;

    private static final int URIMATCH_SEASONS = 100;
    private static final int URIMATCH_SEASONS_FOR_YEAR = 110;
    private static final int URIMATCH_SEASONS_FOR_RANGE = 120;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static
    {
        uriMatcher.addURI(AUTHORITY, QUERY_CONFIG, URIMATCH_CONFIG);

        uriMatcher.addURI(AUTHORITY, QUERY_SUN, URIMATCH_SUN);
        uriMatcher.addURI(AUTHORITY, QUERY_SUN + "/#", URIMATCH_SUN_FOR_DATE);
        uriMatcher.addURI(AUTHORITY, QUERY_SUN + "/*", URIMATCH_SUN_FOR_RANGE);

        uriMatcher.addURI(AUTHORITY, QUERY_MOON, URIMATCH_MOON);
        uriMatcher.addURI(AUTHORITY, QUERY_MOON + "/#", URIMATCH_MOON_FOR_DATE);
        uriMatcher.addURI(AUTHORITY, QUERY_MOON + "/*", URIMATCH_MOON_FOR_RANGE);

        uriMatcher.addURI(AUTHORITY, QUERY_MOONPHASE, URIMATCH_MOONPHASE);
        uriMatcher.addURI(AUTHORITY, QUERY_MOONPHASE + "/#", URIMATCH_MOONPHASE_FOR_DATE);
        uriMatcher.addURI(AUTHORITY, QUERY_MOONPHASE + "/*", URIMATCH_MOONPHASE_FOR_RANGE);

        uriMatcher.addURI(AUTHORITY, QUERY_SEASONS, URIMATCH_SEASONS);
        uriMatcher.addURI(AUTHORITY, QUERY_SEASONS + "/#", URIMATCH_SEASONS_FOR_YEAR);
        uriMatcher.addURI(AUTHORITY, QUERY_SEASONS + "/*", URIMATCH_SEASONS_FOR_RANGE);
    }

    @Override
    public boolean onCreate()
    {
        return true;
    }

    private static SparseArray<SuntimesCalculator> sunSource = new SparseArray<>();    // sun source for appWidgetID (app is 0)
    private static SuntimesCalculator initSunCalculator(Context context, int appWidgetID)
    {
        SuntimesCalculator retValue = sunSource.get(appWidgetID);
        if (retValue == null)         // lazy init
        {
            WidgetSettings.Location config_location = WidgetSettings.loadLocationPref(context, appWidgetID);
            TimeZone timezone = TimeZone.getTimeZone(WidgetSettings.loadTimezonePref(context, appWidgetID));
            SuntimesCalculatorDescriptor sunSourceDesc = WidgetSettings.loadCalculatorModePref(context, appWidgetID);
            SuntimesCalculatorFactory sunSourceFactory = new SuntimesCalculatorFactory(context, sunSourceDesc);
            sunSource.put(appWidgetID, (retValue = sunSourceFactory.createCalculator(config_location, timezone)));
        }
        return retValue;
    }

    private static SparseArray<SuntimesCalculator> moonSource = new SparseArray<>();   // moon source for appWidgetID (app is 0)
    private static SuntimesCalculator initMoonCalculator(Context context, int appWidgetID)
    {
        SuntimesCalculator retValue = moonSource.get(appWidgetID);
        if (retValue == null)    // lazy init
        {
            WidgetSettings.Location config_location = WidgetSettings.loadLocationPref(context, appWidgetID);
            TimeZone timezone = TimeZone.getTimeZone(WidgetSettings.loadTimezonePref(context, appWidgetID));
            SuntimesCalculatorDescriptor moonSourceDesc = WidgetSettings.loadCalculatorModePref(context, 0, "moon");      // always use app calculator (0)
            SuntimesCalculatorFactory moonSourceFactory = new SuntimesCalculatorFactory(context, moonSourceDesc);
            moonSource.put(appWidgetID, (retValue = moonSourceFactory.createCalculator(config_location, timezone)));
        }
        return retValue;
    }

    /**
     * query
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder)
    {
        int appWidgetID = 0;
        Calendar now = Calendar.getInstance();
        Calendar date = Calendar.getInstance();
        Calendar[] range;
        Cursor retValue = null;

        int uriMatch = uriMatcher.match(uri);
        switch (uriMatch)
        {
            case URIMATCH_CONFIG:
                Log.d("CalculatorProvider", "URIMATCH_CONFIG");
                retValue = queryConfig(appWidgetID, uri, projection, selection, selectionArgs, sortOrder);
                break;

            case URIMATCH_SEASONS:
                Log.d("CalculatorProvider", "URIMATCH_SEASONS");
                retValue = querySeasons(appWidgetID, new Calendar[] {now, now}, uri, projection, selection, selectionArgs, sortOrder);
                break;
            case URIMATCH_SEASONS_FOR_YEAR:
                Log.d("CalculatorProvider", "URIMATCH_SEASONS_FOR_YEAR");
                date.set(Calendar.YEAR, (int)ContentUris.parseId(uri));
                retValue = querySeasons(appWidgetID, new Calendar[] { date, date }, uri, projection, selection, selectionArgs, sortOrder);
                break;
            case URIMATCH_SEASONS_FOR_RANGE:
                Log.d("CalculatorProvider", "URIMATCH_SEASONS_FOR_RANGE");
                range = parseYearRange(uri.getLastPathSegment());
                retValue = querySeasons(appWidgetID, range, uri, projection, selection, selectionArgs, sortOrder);
                break;

            case URIMATCH_SUN:
                Log.d("CalculatorProvider", "URIMATCH_SUN");
                retValue = querySun(appWidgetID, new Calendar[] {now, now}, uri, projection, selection, selectionArgs, sortOrder);
                break;
            case URIMATCH_SUN_FOR_DATE:
                Log.d("CalculatorProvider", "URIMATCH_SUN_FOR_DATE");
                retValue = querySun(appWidgetID, new Calendar[] {date, date}, uri, projection, selection, selectionArgs, sortOrder);
                break;
            case URIMATCH_SUN_FOR_RANGE:
                Log.d("CalculatorProvider", "URIMATCH_SUN_FOR_RANGE");
                range = parseDateRange(uri.getLastPathSegment());
                retValue = querySun(appWidgetID, range, uri, projection, selection, selectionArgs, sortOrder);
                break;

            case URIMATCH_MOON:
                Log.d("CalculatorProvider", "URIMATCH_MOON");
                retValue = queryMoon(appWidgetID, new Calendar[] {now, now}, uri, projection, selection, selectionArgs, sortOrder);
                break;
            case URIMATCH_MOON_FOR_DATE:
                Log.d("CalculatorProvider", "URIMATCH_MOON_FOR_DATE");
                retValue = queryMoon(appWidgetID, new Calendar[] {date, date}, uri, projection, selection, selectionArgs, sortOrder);
                break;
            case URIMATCH_MOON_FOR_RANGE:
                Log.d("CalculatorProvider", "URIMATCH_MOON_FOR_RANGE");
                range = parseDateRange(uri.getLastPathSegment());
                retValue = queryMoon(appWidgetID, range, uri, projection, selection, selectionArgs, sortOrder);
                break;

            case URIMATCH_MOONPHASE:
                Log.d("CalculatorProvider", "URIMATCH_MOONPHASE");
                retValue = queryMoonPhase(appWidgetID, new Calendar[] {now, now}, uri, projection, selection, selectionArgs, sortOrder);
                break;
            case URIMATCH_MOONPHASE_FOR_DATE:
                Log.d("CalculatorProvider", "URIMATCH_MOONPHASE_FOR_DATE");
                date.setTimeInMillis(ContentUris.parseId(uri));
                retValue = queryMoonPhase(appWidgetID, new Calendar[] {date, date}, uri, projection, selection, selectionArgs, sortOrder);
                break;
            case URIMATCH_MOONPHASE_FOR_RANGE:
                Log.d("CalculatorProvider", "URIMATCH_MOONPHASE_FOR_RANGE");
                range = parseDateRange(uri.getLastPathSegment());
                retValue = queryMoonPhase(appWidgetID, range, uri, projection, selection, selectionArgs, sortOrder);
                break;

            default:
                Log.e("CalculatorProvider", "Unrecognized URI! " + uri);
                break;
        }
        return retValue;
    }

    protected Calendar[] parseDateRange(String rangeSegment)
    {
        Calendar[] retValue = new Calendar[2];
        String[] rangeString = rangeSegment.split("-");
        if (rangeString.length == 2)
        {
            try {
                retValue[0] = Calendar.getInstance();
                retValue[0].setTimeInMillis(Long.parseLong(rangeString[0]));

                retValue[1] = Calendar.getInstance();
                retValue[1].setTimeInMillis(Long.parseLong(rangeString[1]) + 1000);

            } catch (NumberFormatException e) {
                Log.w("CalculatorProvider", "Invalid range! " + rangeSegment);
                retValue[0] = retValue[1] = Calendar.getInstance();
            }
        } else {
            Log.w("CalculatorProvider", "Invalid range! " + rangeSegment);
            retValue[0] = retValue[1] = Calendar.getInstance();
        }
        Log.d("DEBUG", "startDate: " + retValue[0].getTimeInMillis() + ", endDate: " + retValue[1].getTimeInMillis());
        return retValue;
    }

    protected Calendar[] parseYearRange(String rangeSegment)
    {
        Calendar[] retValue = new Calendar[2];
        String[] rangeString = rangeSegment.split("-");
        if (rangeString.length == 2)
        {
            try {
                retValue[0] = Calendar.getInstance();
                retValue[0].set(Calendar.YEAR, Integer.parseInt(rangeString[0]));

                retValue[1] = Calendar.getInstance();
                retValue[1].set(Calendar.YEAR, Integer.parseInt(rangeString[1]) + 1);

            } catch (NumberFormatException e) {
                Log.w("CalculatorProvider", "Invalid range! " + rangeSegment);
                retValue[0] = retValue[1] = Calendar.getInstance();
            }
        } else {
            Log.w("CalculatorProvider", "Invalid range! " + rangeSegment);
            retValue[0] = retValue[1] = Calendar.getInstance();
        }
        Log.d("DEBUG", "startDate: " + retValue[0].get(Calendar.YEAR) + ", endDate: " + retValue[1].get(Calendar.YEAR));
        return retValue;
    }

    /**
     * queryConfig
     */
    private Cursor queryConfig(int appWidgetID, @NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder)
    {
        Context context = getContext();
        String[] columns = (projection != null ? projection : QUERY_CONFIG_PROJECTION);
        MatrixCursor retValue = new MatrixCursor(columns);

        if (context != null)
        {
            initSunCalculator(getContext(), appWidgetID);
            SuntimesCalculator calculator = sunSource.get(appWidgetID);
            if (calculator != null)
            {
                WidgetSettings.Location location = null;
                Object[] row = new Object[columns.length];
                for (int i=0; i<columns.length; i++)
                {
                    switch (columns[i])
                    {
                        case COLUMN_CONFIG_LOCALE:
                            AppSettings.LocaleMode localeMode = AppSettings.loadLocaleModePref(context);
                            row[i] = ((localeMode == AppSettings.LocaleMode.SYSTEM_LOCALE) ? null : AppSettings.loadLocalePref(context));
                            break;

                        case COLUMN_CONFIG_APPTHEME:
                            row[i] = AppSettings.loadThemePref(context);
                            break;

                        case COLUMN_CONFIG_LATITUDE:
                            if (location == null) {
                                location = WidgetSettings.loadLocationPref(context, appWidgetID);
                            }
                            row[i] = location.getLatitude();
                            break;

                        case COLUMN_CONFIG_LONGITUDE:
                            if (location == null) {
                                location = WidgetSettings.loadLocationPref(context, appWidgetID);
                            }
                            row[i] = location.getLongitude();
                            break;

                        case COLUMN_CONFIG_ALTITUDE:
                            if (location == null) {
                                location = WidgetSettings.loadLocationPref(context, appWidgetID);
                            }
                            row[i] = location.getAltitude();
                            break;

                        case COLUMN_CONFIG_TIMEZONE:
                            row[i] = WidgetSettings.loadTimezonePref(context, appWidgetID);
                            break;

                        case COLUMN_CONFIG_APPWIDGETID:
                            row[i] = appWidgetID;
                            break;

                        default:
                            row[i] = null;
                            break;
                    }
                }
                retValue.addRow(row);

            } else Log.e("queryConfig", "sunSource " + appWidgetID + " is null!");
        } else Log.e("queryConfig", "context is null!");
        return retValue;
    }

    /**
     * querySun
     */
    private Cursor querySun(int appWidgetID, Calendar[] range, @NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder)
    {
        String[] columns = (projection != null ? projection : QUERY_SUN_PROJECTION);
        MatrixCursor retValue = new MatrixCursor(columns);

        SuntimesCalculator calculator = initSunCalculator(getContext(), appWidgetID);
        if (calculator != null)
        {
            Calendar day = Calendar.getInstance();
            day.setTimeInMillis(range[0].getTimeInMillis());
            do {
                Calendar[] morningBlueHour = null, eveningBlueHour = null;
                Object[] row = new Object[columns.length];
                for (int i=0; i<columns.length; i++)
                {
                    switch (columns[i])
                    {
                        case COLUMN_SUN_ACTUAL_RISE:
                            row[i] = calculator.getOfficialSunriseCalendarForDate(day);
                            break;
                        case COLUMN_SUN_ACTUAL_SET:
                            row[i] = calculator.getOfficialSunsetCalendarForDate(day);
                            break;

                        case COLUMN_SUN_CIVIL_RISE:
                            row[i] = calculator.getCivilSunriseCalendarForDate(day);
                            break;
                        case COLUMN_SUN_CIVIL_SET:
                            row[i] = calculator.getCivilSunsetCalendarForDate(day);
                            break;

                        case COLUMN_SUN_NAUTICAL_RISE:
                            row[i] = calculator.getNauticalSunriseCalendarForDate(day);
                            break;
                        case COLUMN_SUN_NAUTICAL_SET:
                            row[i] = calculator.getNauticalSunsetCalendarForDate(day);
                            break;

                        case COLUMN_SUN_ASTRO_RISE:
                            row[i] = calculator.getAstronomicalSunriseCalendarForDate(day);
                            break;
                        case COLUMN_SUN_ASTRO_SET:
                            row[i] = calculator.getAstronomicalSunsetCalendarForDate(day);
                            break;

                        case COLUMN_SUN_NOON:
                            row[i] = calculator.getSolarNoonCalendarForDate(day);
                            break;

                        case COLUMN_SUN_GOLDEN_EVENING:
                            row[i] = calculator.getEveningGoldenHourForDate(day);
                            break;
                        case COLUMN_SUN_GOLDEN_MORNING:
                            row[i] = calculator.getMorningGoldenHourForDate(day);
                            break;

                        case COLUMN_SUN_BLUE8_RISE:
                            if (morningBlueHour == null) {
                                morningBlueHour = calculator.getMorningBlueHourForDate(day);
                            }
                            row[i] = morningBlueHour[0];
                            break;
                        case COLUMN_SUN_BLUE4_RISE:
                            if (morningBlueHour == null) {
                                morningBlueHour = calculator.getMorningBlueHourForDate(day);
                            }
                            row[i] = morningBlueHour[1];
                            break;

                        case COLUMN_SUN_BLUE4_SET:
                            if (eveningBlueHour == null) {
                                eveningBlueHour = calculator.getEveningBlueHourForDate(day);
                            }
                            row[i] = eveningBlueHour[0];
                            break;
                        case COLUMN_SUN_BLUE8_SET:
                            if (eveningBlueHour == null) {
                                eveningBlueHour = calculator.getEveningBlueHourForDate(day);
                            }
                            row[i] = eveningBlueHour[1];
                            break;

                        default:
                            row[i] = null;
                            break;
                    }
                }
                retValue.addRow(row);
                day.add(Calendar.DAY_OF_YEAR, 1);
            } while (day.before(range[1]));

        } else Log.d("DEBUG", "sunSource is null!");
        return retValue;
    }

    /**
     * queryMoon
     */
    private Cursor queryMoon(int appWidgetID, Calendar[] range, @NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder)
    {
        String[] columns = (projection != null ? projection : QUERY_MOON_PROJECTION);
        MatrixCursor retValue = new MatrixCursor(columns);

        SuntimesCalculator calculator = initMoonCalculator(getContext(), appWidgetID);
        if (calculator != null)
        {
            Calendar day = Calendar.getInstance();
            day.setTimeInMillis(range[0].getTimeInMillis());
            do {
                SuntimesCalculator.MoonTimes moontimes = null;
                Object[] row = new Object[columns.length];
                for (int i=0; i<columns.length; i++)
                {
                    switch (columns[i])
                    {
                        case COLUMN_MOON_RISE:
                            moontimes = (moontimes == null ? calculator.getMoonTimesForDate(day) : moontimes);
                            row[i] = moontimes.riseTime;
                            break;
                        case COLUMN_MOON_SET:
                            moontimes = (moontimes == null ? calculator.getMoonTimesForDate(day) : moontimes);
                            row[i] = moontimes.setTime;
                            break;

                        default:
                            row[i] = null;
                            break;
                    }
                }
                retValue.addRow(row);
                day.add(Calendar.DAY_OF_YEAR, 1);
            } while (day.before(range[1]));

        } else Log.d("DEBUG", "moonSource is null!");
        return retValue;
    }

    /**
     * queryMoonPhase
     */
    private Cursor queryMoonPhase(int appWidgetID, Calendar[] range, @NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder)
    {
        String[] columns = (projection != null ? projection : QUERY_MOONPHASE_PROJECTION);
        MatrixCursor retValue = new MatrixCursor(columns);

        SuntimesCalculator calculator = initMoonCalculator(getContext(), appWidgetID);
        if (calculator != null)
        {
            ArrayList<Calendar> events = new ArrayList<>();
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(range[0].getTimeInMillis());
            do {
                events.clear();
                Object[] row = new Object[columns.length];
                for (int i=0; i<columns.length; i++)
                {
                    Calendar event;
                    switch (columns[i])
                    {
                        case COLUMN_MOON_NEW:
                            events.add(event = calculator.getMoonPhaseNextDate(SuntimesCalculator.MoonPhase.NEW , date));
                            row[i] = event.getTimeInMillis();
                            break;

                        case COLUMN_MOON_FIRST:
                            events.add(event = calculator.getMoonPhaseNextDate(SuntimesCalculator.MoonPhase.FIRST_QUARTER, date));
                            row[i] = event.getTimeInMillis();
                            break;

                        case COLUMN_MOON_FULL:
                            events.add(event = calculator.getMoonPhaseNextDate(SuntimesCalculator.MoonPhase.FULL, date));
                            row[i] = event.getTimeInMillis();
                            break;

                        case COLUMN_MOON_THIRD:
                            events.add(event = calculator.getMoonPhaseNextDate(SuntimesCalculator.MoonPhase.THIRD_QUARTER, date));
                            row[i] = event.getTimeInMillis();
                            break;

                        default:
                            row[i] = null; break;
                    }
                }
                retValue.addRow(row);

                Collections.sort(events);
                Calendar latest = (events.size() > 1) ? events.get(events.size()-1)
                                : (events.size() > 0) ? events.get(0) : null;

                date.setTimeInMillis(latest != null ? latest.getTimeInMillis() + 1000
                                                    : range[1].getTimeInMillis() + 1000);
            } while (date.before(range[1]));

        } else Log.d("DEBUG", "moonSource is null!");
        return retValue;
    }

    /**
     * querySeasons
     */
    private Cursor querySeasons(int appWidgetID, Calendar[] range, @NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder)
    {
        String[] columns = (projection != null ? projection : QUERY_SEASONS_PROJECTION);
        MatrixCursor retValue = new MatrixCursor(columns);

        SuntimesCalculator calculator = initSunCalculator(getContext(), appWidgetID);
        if (calculator != null)
        {
            Calendar year = Calendar.getInstance();
            year.setTimeInMillis(range[0].getTimeInMillis());
            do {
                Object[] row = new Object[columns.length];
                for (int i=0; i<columns.length; i++)
                {
                    switch (columns[i])
                    {
                        case COLUMN_SEASON_YEAR:
                            row[i] = year.get(Calendar.YEAR);
                            break;

                        case COLUMN_SEASON_VERNAL:
                            row[i] = calculator.getVernalEquinoxForYear(year).getTimeInMillis();
                            break;

                        case COLUMN_SEASON_SUMMER:
                            row[i] = calculator.getSummerSolsticeForYear(year).getTimeInMillis();
                            break;

                        case COLUMN_SEASON_AUTUMN:
                            row[i] = calculator.getAutumnalEquinoxForYear(year).getTimeInMillis();
                            break;

                        case COLUMN_SEASON_WINTER:
                            row[i] = calculator.getWinterSolsticeForYear(year).getTimeInMillis();
                            break;

                        default:
                            row[i] = null; break;
                    }
                }
                retValue.addRow(row);
                year.set(Calendar.YEAR, year.get(Calendar.YEAR) + 1);
            } while (year.before(range[1]));

        } else Log.d("DEBUG", "sunSource is null!");
        return retValue;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri)
    {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values)
    {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs)
    {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs)
    {
        return 0;
    }
}
