/**
    Copyright (C) 2014 Forrest Guice
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

import android.content.Context;
import android.util.Log;

import com.forrestguice.suntimeswidget.settings.SuntimesWidgetSettings;
import com.forrestguice.suntimeswidget.calculator.sunrisesunset_java.SunriseSunsetSuntimesCalculator;

/**
 * A factory class that creates instances of SuntimesCalculator. The specific implementation returned
 * by the factory's createCalculator method is controlled by the SuntimesCalculatorDescriptor arg
 * passed to the constructor.

 * The SuntimesCalculatorDescriptor specifies a (fully qualified) class string that is instantiated
 * using reflection when the createCalculator method is called. The descriptor identifies the
 * calculator using name(), the class to instantiate using getReference(), and the value to display
 * in the UI using getDisplayString().
 *
 * SuntimesCalculatorDescriptor keeps a static list of installed calculators that SuntimesCalculatorFactory
 * must initialize - initialization is performed using the SuntimesCalculatorFactory.initCalculators() method.
 * Using the SuntimesCalculatorDescriptor.values() and SuntimesCalculatorDescriptor.valueOf() methods will
 * trigger lazy initialization by the factory class.
 *
 * This factory knows about the following implementations:
 *
 *   * sunrisesunsetlib
 *     :: com.forrestguice.suntimeswidget.calculator.sunrisesunset_java.SunriseSunsetSuntimesCalculator.class
 *
 */
public class SuntimesCalculatorFactory
{
    protected static boolean initialized = false;
    public static void initCalculators()
    {
        if (!initialized)
        {
            SuntimesCalculatorDescriptor.addValue(SunriseSunsetSuntimesCalculator.getDescriptor());

            initialized = true;
        }
    }

    private SuntimesCalculatorDescriptor current;
    private Context context;

    /**
     * Create a SuntimesCalculatorFactory object.
     * @param context the Android context used by this factory
     * @param calculatorSetting a SuntimesCalculatorDescriptor that specifies the implementation this factory creates
     */
    public SuntimesCalculatorFactory(Context context, SuntimesCalculatorDescriptor calculatorSetting)
    {
        this.context = context;
        this.current = calculatorSetting;

        if (!initialized)
        {
            SuntimesCalculatorFactory.initCalculators();
        }
    }

    /**
     * Create a calculator for a given location and timezone using the calculator descriptor that was
     * passed to the factory when it was created.
     * @param location a SuntimesWidgetSettings.Location specifying latitude and longitude
     * @param timezone a timezone string
     * @return a calculator object that implements SuntimesCalculator
     */
    public SuntimesCalculator createCalculator(SuntimesWidgetSettings.Location location, String timezone)
    {
        SuntimesCalculator calculator;
        try {
            Log.d("createCalculator", "trying .oO( " + current.getReference() + " )");
            Class calculatorClass = Class.forName(current.getReference());

            calculator = (SuntimesCalculator)calculatorClass.newInstance();
            Log.d("createCalculator", "using .oO( " + calculator.name() + " ): " + timezone);

        } catch (Exception e1) {
            calculator = new SunriseSunsetSuntimesCalculator();
            Log.e("createCalculator", "fail! .oO( " + current.name() + "), so using the default: " + calculator.name() +": " + timezone);
        }

        calculator.init(location, timezone);
        return calculator;
    }
}
