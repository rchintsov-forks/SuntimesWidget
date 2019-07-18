/**
    Copyright (C) 2018-2019 Forrest Guice
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

package com.forrestguice.suntimeswidget.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.calculator.core.SuntimesCalculator;
import com.forrestguice.suntimeswidget.calculator.SuntimesRiseSetDataset;
import com.forrestguice.suntimeswidget.calculator.core.Location;

import java.util.Calendar;

/**
 * WorldMapTask
 */
public class WorldMapTask extends AsyncTask<Object, Bitmap, Bitmap>
{
    private WorldMapProjection projection = new WorldMapEquirectangular();
    private WorldMapOptions options = new WorldMapOptions();

    public WorldMapTask()
    {
    }

    /**
     * @param params 0: SuntimesRiseSetDataset,
     *               1: Integer (width),
     *               2: Integer (height),
     *               3: Drawable (map)
     * @return a bitmap, or null params are invalid
     */
    @Override
    protected Bitmap doInBackground(Object... params)
    {
        int w, h;
        SuntimesRiseSetDataset data;
        try {
            data = (SuntimesRiseSetDataset)params[0];
            w = (Integer)params[1];
            h = (Integer)params[2];
            if (params.length > 3) {
                options = (WorldMapOptions) params[3];
            }
            if (params.length > 4) {
                projection = (WorldMapProjection) params[4];
            }

        } catch (ClassCastException e) {
            Log.w("WorldMapTask", "Invalid params; using [null, 0, 0]");
            return null;
        }
        return makeBitmap(data, w, h, options);
    }

    public Bitmap makeBitmap(SuntimesRiseSetDataset data, int w, int h, WorldMapOptions options)
    {
        return projection.makeBitmap(data, w, h, options);
    }

    @Override
    protected void onPreExecute()
    {
        if (listener != null) {
            listener.onStarted();
        }
    }

    @Override
    protected void onProgressUpdate( Bitmap... frames )
    {
        if (listener != null)
        {
            for (int i=0; i<frames.length; i++) {
                listener.onFrame(frames[i], options.offsetMinutes);
            }
        }
    }

    @Override
    protected void onPostExecute( Bitmap lastFrame )
    {
        if (isCancelled()) {
            lastFrame = null;
        }
        if (listener != null) {
            listener.onFinished(lastFrame);
        }
    }

    /////////////////////////////////////////////

    private WorldMapTaskListener listener = null;
    public void setListener( WorldMapTaskListener listener ) {
        this.listener = listener;
    }

    /**
     * WorldMapOptions
     */
    public static class WorldMapOptions
    {
        public boolean modified = false;

        public Drawable map = null;                  // BitmapDrawable
        public Drawable map_night = null;            // BitmapDrawable
        public int backgroundColor = Color.BLUE;
        public int foregroundColor = Color.TRANSPARENT;
        public boolean hasTransparentBaseMap = true;

        public boolean showGrid = false;
        public int gridXColor = Color.LTGRAY;
        public int gridYColor = Color.WHITE;

        public boolean showMajorLatitudes = false;
        public int[] latitudeColors = { Color.DKGRAY, Color.WHITE, Color.DKGRAY };    // equator, tropics, polar circle
        float[][] latitudeLinePatterns = new float[][] {{ 0, 0 }, {5, 10}, {10, 5}};    // {dash-on, dash-off} .. for equator, tropics, and polar circle .. dash-on 0 for a solid line
        public float latitudeLineScale = 0.5f;

        public boolean showSunPosition = true;
        public int sunFillColor = Color.YELLOW;
        public int sunStrokeColor = Color.BLACK;
        public int sunScale = 48;                     // 48; default 48 suns fit within the width of the image (which is 24 hr wide meaning the sun has diameter of a half-hour)
        public int sunStrokeScale = 3;                // 3; default 3 strokes fit within the radius of the sun (i.e. the stroke is 1/3 the width)

        public boolean showSunShadow = true;
        public int sunShadowColor = Color.BLACK;

        public boolean showMoonPosition = true;
        public int moonFillColor = Color.WHITE;
        public int moonStrokeColor = Color.BLACK;
        public int moonScale = 72;                    // 72; default moonscale is 3/4 the size of the sun (48)
        public int moonStrokeScale = 3;               // 3; default 3 strokes fit within the radius of the moon

        public boolean showMoonLight = true;
        public int moonLightColor = Color.LTGRAY;

        public boolean translateToLocation = false;

        public double[][] locations = null;  // a list of locations {{lat, lon}, {lat, lon}, ...} or null
        public int locationFillColor = Color.MAGENTA;
        public int locationScale = 192;

        public int offsetMinutes = 0;    // minutes offset from "now" (default 0)
    }

    /**
     * WorldMapProjection
     */
    public static abstract class WorldMapProjection
    {
        public abstract int[] toBitmapCoords(int w, int h, double lat, double lon);

        /**
         * algorithm described at https://gis.stackexchange.com/questions/17184/method-to-shade-or-overlay-a-raster-map-to-reflect-time-of-day-and-ambient-light
         */
        public abstract Bitmap makeBitmap(SuntimesRiseSetDataset data, int w, int h, WorldMapTask.WorldMapOptions options);
        public abstract double[] initMatrix();            // creates flattened multi-dimensional array; [lon][lat][v(3)]
        public abstract int[] matrixSize();               // [width(lon), height(lat)]
        protected abstract int k(int x, int y, int z);    // returns index into flattened array

        protected Calendar mapTime(SuntimesRiseSetDataset data, WorldMapTask.WorldMapOptions options)
        {
            Calendar mapTime = data.nowThen(data.calendar());    // the current time (maybe on some other day)
            mapTime.add(Calendar.MINUTE, options.offsetMinutes);    // offset by some minutes
            return mapTime;
        }

        /**
         * Implemented using algorithm found at
         * http://129.79.46.40/~foxd/cdrom/musings/formulas/formulas.htm (Useful Formulas for Amateur SETI)
         * "Hour Angle(HA) and Declination(DE) given the Altitude(AL) and Azimuth(AZ) of a star and the observers Latitude(LA) and Longitude(LO)"
         *
         * 1. Convert Azimuth(AZ) and Altitude(AL) to decimal degrees.
         * 2. Compute sin(DE)=(sin(AL)*sin(LA))+(cos(AL)*cos(LA)*cos(AZ)).
         * 3. Take the inverse sine of sin(DE) to get the declination.
         * 4. Compute cos(HA)=(sin(AL)-(sin(LA)*sin(DE)))/(cos(LA)*cos(DE)).
         * 5. Take the inverse cosine of cos(HA).
         * 6. Take the sine of AZ. If it is positive then HA=360-HA.
         *
         * @param location latitude and longitude
         * @param pos azimuth and altitude
         * @return { greenwich hour angle, declination }
         */
        protected double[] gha(Location location, @NonNull SuntimesCalculator.Position pos)
        {
            double radLat = Math.toRadians(location.getLatitudeAsDouble());
            double sinLat = Math.sin(radLat);
            double cosLat = Math.cos(radLat);

            double radAlt = Math.toRadians(pos.elevation);
            double sinAlt = Math.sin(radAlt);
            double cosAlt = Math.cos(radAlt);

            double radAz = Math.toRadians(pos.azimuth);
            double sinAz = Math.sin(radAz);
            double cosAz = Math.cos(radAz);

            double sinDec = (sinAlt * sinLat) + (cosAlt * cosLat * cosAz);
            double dec = Math.asin(sinDec);  // radians

            double cosHourAngle = (sinAlt - (sinLat * sinDec)) / (cosLat * Math.cos(dec));
            double hourAngle = Math.toDegrees(Math.acos(cosHourAngle));  // local hour angle (degrees)
            if (Math.toDegrees(sinAz) > 0)
                hourAngle = 360 - hourAngle;

            hourAngle = (hourAngle - location.getLongitudeAsDouble()) % 360; // greenwich hour angle (degrees)
            //Log.d(WorldMapView.LOGTAG, "hourAngle is " + hourAngle + ", dec is " + Math.toDegrees(dec) + " (" + pos.declination + ")");
            return new double[] { hourAngle, Math.toDegrees(dec) };
        }

        protected double[] unitVector( double lat, double lon )
        {
            double radLon = Math.toRadians(lon);
            double radLat = Math.toRadians(lat);
            double cosLat = Math.cos(radLat);
                                                       // spherical coordinates to unit vector
            double[] retValue = new double[3];            // v[3] = { (cos(lon)cos(lat), sin(lon)cos(lat), sin(lat)) }
            retValue[0] = Math.cos(radLon) * cosLat;
            retValue[1] = Math.sin(radLon) * cosLat;
            retValue[2] = Math.sin(radLat);
            return retValue;
        }

        protected void drawMap(Canvas c, int w, int h, WorldMapTask.WorldMapOptions options)
        {
            if (options.map != null)
            {
                if (options.foregroundColor != Color.TRANSPARENT)
                {
                    Bitmap b = ((BitmapDrawable)options.map).getBitmap();
                    Rect src = new Rect(0,0,b.getWidth()-1, b.getHeight()-1);
                    Rect dst = new Rect(0,0,w-1, h-1);

                    Paint paintForeground = new Paint();
                    paintForeground.setColorFilter(new LightingColorFilter(options.foregroundColor, 0));
                    c.drawBitmap(b, src, dst, paintForeground);

                } else {
                    options.map.setBounds(0, 0, w, h);
                    options.map.draw(c);
                }
            }
        }

        protected double sunRadius(Canvas c, WorldMapTask.WorldMapOptions options)
        {
            double sunDiameter = (int)Math.ceil(c.getWidth() / (double)options.sunScale);
            return (int)Math.ceil(sunDiameter / 2d);
        }

        protected int sunStroke(Canvas c, WorldMapTask.WorldMapOptions options)
        {
            return (int)Math.ceil(sunRadius(c, options) / (double)options.sunStrokeScale);
        }

        protected void drawSun(Canvas c, int x, int y, Paint p, WorldMapTask.WorldMapOptions options)
        {
            if (p == null) {
                p = new Paint(Paint.ANTI_ALIAS_FLAG);
            }

            int sunRadius = (int)sunRadius(c, options);
            int sunStroke = (int)Math.ceil(sunRadius / (double)options.sunStrokeScale);

            p.setStyle(Paint.Style.FILL);
            p.setColor(options.sunFillColor);
            c.drawCircle(x, y, sunRadius, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(sunStroke);
            p.setColor(options.sunStrokeColor);
            c.drawCircle(x, y, sunRadius, p);
        }

        protected void drawMoon(Canvas c, int x, int y, Paint p, WorldMapTask.WorldMapOptions options)
        {
            if (p == null) {
                p = new Paint(Paint.ANTI_ALIAS_FLAG);
            }

            double moonDiameter = Math.ceil(c.getWidth() / (double)options.moonScale);
            int moonRadius = (int)Math.ceil(moonDiameter / 2d);
            int moonStroke = (int)Math.ceil(moonRadius / (double)options.moonStrokeScale);

            p.setStyle(Paint.Style.FILL);
            p.setColor(options.moonFillColor);
            c.drawCircle(x, y, moonRadius, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(moonStroke);
            p.setColor(options.moonStrokeColor);
            c.drawCircle(x, y, moonRadius, p);
        }

        public void drawMajorLatitudes(Canvas c, int w, int h, Paint p, WorldMapTask.WorldMapOptions options) { /* EMPTY */ }
        public void drawLocations(Canvas c, int w, int h, Paint p, WorldMapTask.WorldMapOptions options)
        {
            if (p == null) {
                p = new Paint(Paint.ANTI_ALIAS_FLAG);
            }

            if (options.locations != null && options.locations.length > 0)
            {
                for (int i=0; i<options.locations.length; i++)
                {
                    int[] point = toBitmapCoords(w, h, options.locations[i][0], options.locations[i][1]);
                    drawLocation(c, point[0], point[1], p, options);
                    Log.d("DEBUG", "drawLocations: " + options.locations[i][0] + ", " + options.locations[i][1]);
                }
            }
        }

        protected void drawLocation(Canvas c, int x, int y, Paint p, WorldMapTask.WorldMapOptions options)
        {
            double pointDiameter = (int)Math.ceil(c.getWidth() / (double)options.locationScale);
            int pointRadius = (int)Math.ceil(pointDiameter / 2d);

            p.setStyle(Paint.Style.FILL);
            p.setColor(options.locationFillColor);
            c.drawCircle(x, y, pointRadius, p);
        }

    }

    /**
     * WorldMapTaskListener
     */
    @SuppressWarnings("EmptyMethod")
    public static abstract class WorldMapTaskListener
    {
        public void onStarted() {}
        public void onFrame( Bitmap frame, int offsetMinutes ) {}
        public void onFinished( Bitmap result ) {}
    }

}
