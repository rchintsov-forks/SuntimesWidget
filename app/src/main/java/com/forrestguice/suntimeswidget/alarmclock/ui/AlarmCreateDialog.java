/**
    Copyright (C) 2020 Forrest Guice
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
package com.forrestguice.suntimeswidget.alarmclock.ui;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.forrestguice.suntimeswidget.AlarmDialog;
import com.forrestguice.suntimeswidget.LocationConfigDialog;
import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.alarmclock.AlarmClockItem;
import com.forrestguice.suntimeswidget.alarmclock.AlarmNotifications;
import com.forrestguice.suntimeswidget.alarmclock.AlarmSettings;
import com.forrestguice.suntimeswidget.calculator.SuntimesEquinoxSolsticeDataset;
import com.forrestguice.suntimeswidget.calculator.SuntimesMoonData;
import com.forrestguice.suntimeswidget.calculator.SuntimesRiseSetDataset;
import com.forrestguice.suntimeswidget.calculator.core.Location;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.SolarEvents;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;

@SuppressWarnings("Convert2Diamond")
public class AlarmCreateDialog extends BottomSheetDialogFragment
{
    public static final String EXTRA_MODE = "mode";             // "by event", "by time", ..
    public static final String EXTRA_ALARMTYPE = "alarmtype";
    public static final String EXTRA_HOUR = "hour";
    public static final String EXTRA_MINUTE = "minute";
    public static final String EXTRA_DATE = "date";
    public static final String EXTRA_OFFSET = "offset";
    public static final String EXTRA_TIMEZONE = "timezone";
    public static final String EXTRA_LOCATION = "location";
    public static final String EXTRA_EVENT = "event";

    public static final int DEF_MODE = 1;
    public static final int DEF_HOUR = 6;
    public static final int DEF_MINUTE = 30;
    public static final SolarEvents DEF_EVENT = SolarEvents.SUNRISE;
    public static final AlarmClockItem.AlarmType DEF_ALARMTYPE = AlarmClockItem.AlarmType.ALARM;

    public static final String EXTRA_PREVIEW_OFFSET = "previewOffset";
    public static final String EXTRA_BUTTON_ALARMLIST = "showAlarmListButton";

    public static final String DIALOG_LOCATION = "locationDialog";

    public static final String PREFS_ALARMCREATE = "com.forrestguice.suntimeswidget.alarmcreate";

    protected TabLayout tabs;
    protected TextView text_title, text_offset, text_date, text_note;
    protected TextSwitcher text_time;
    protected ImageView icon_offset;
    protected Spinner spin_type;
    protected ImageButton btn_alarms;
    protected SuntimesUtils utils = new SuntimesUtils();

    public AlarmCreateDialog() {
        super();

        Bundle args = new Bundle();
        args.putInt(EXTRA_MODE, DEF_MODE);
        args.putBoolean(EXTRA_PREVIEW_OFFSET, false);
        args.putBoolean(EXTRA_BUTTON_ALARMLIST, false);

        args.putInt(EXTRA_HOUR, DEF_HOUR);
        args.putInt(EXTRA_MINUTE, DEF_MINUTE);
        args.putLong(EXTRA_OFFSET, 0);
        args.putString(EXTRA_TIMEZONE, null);
        args.putSerializable(EXTRA_EVENT, DEF_EVENT);
        args.putSerializable(EXTRA_ALARMTYPE, DEF_ALARMTYPE);

        setArguments(args);
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedState)
    {
        Bundle args = getArguments();
        if (getLocation() == null) {
            args.putParcelable(EXTRA_LOCATION, WidgetSettings.loadLocationPref(getActivity(), 0));
        }

        Context context = getActivity();
        SolarEvents.initDisplayStrings(context);
        AlarmClockItem.AlarmType.initDisplayStrings(context);
        AlarmClockItem.AlarmTimeZone.initDisplayStrings(context);

        //setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppTheme);
        super.onCreate(savedState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedState) {
        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(getActivity(), AppSettings.loadTheme(getContext()));
        View dialogContent = inflater.cloneInContext(contextWrapper).inflate(R.layout.layout_dialog_alarmcreate, parent, false);

        initViews(getContext(), dialogContent);
        if (savedState != null) {
            loadSettings(savedState);
        }

        TabLayout.Tab tab = tabs.getTabAt(getArguments().getInt(EXTRA_MODE, 0));
        if (tab != null) {
            tab.select();
        }
        showFragmentForMode(tab != null ? tab.getPosition() : 0);

        return dialogContent;
    }

    private void showFragmentForMode(int mode)
    {
        switch (mode)
        {
            case 1:
                showByTimeFragment();
                break;

            case 0:
            default:
                showByEventFragment();
                break;
        }
    }

    protected void showByEventFragment() {
        FragmentManager fragments = getChildFragmentManager();
        FragmentTransaction transaction = fragments.beginTransaction();

        AlarmDialog fragment = new AlarmDialog();
        fragment.setDialogShowFrame(false);
        fragment.setDialogShowDesc(false);
        fragment.setType(getAlarmType());
        initEventDialog(getActivity(), fragment, getLocation());
        fragment.setDialogListener(new AlarmDialog.DialogListener() {
            @Override
            public void onChanged(AlarmDialog dialog) {
                getArguments().putSerializable(EXTRA_EVENT, dialog.getChoice());
                getArguments().putParcelable(EXTRA_LOCATION, dialog.getLocation());
                updateViews(getActivity());
            }

            @Override
            public void onAccepted(AlarmDialog dialog) {}

            @Override
            public void onCanceled(AlarmDialog dialog) {}

            @Override
            public void onLocationClick(AlarmDialog dialog) {
                showLocationDialog(getActivity());
            }
        });
        fragment.setChoice(getEvent());

        transaction.replace(R.id.fragmentContainer1, fragment, "AlarmDialog");
        transaction.commit();
    }

    protected void showLocationDialog(Context context)
    {
        final LocationConfigDialog dialog = new LocationConfigDialog();
        dialog.setHideTitle(true);
        dialog.setHideMode(true);
        dialog.setLocation(context, getLocation());
        dialog.setDialogListener(onLocationChanged);
        dialog.show(getChildFragmentManager(), DIALOG_LOCATION);
    }
    private LocationConfigDialog.LocationConfigDialogListener onLocationChanged = new LocationConfigDialog.LocationConfigDialogListener()
    {
        @Override
        public boolean saveSettings(Context context, WidgetSettings.LocationMode locationMode, Location location)
        {
            FragmentManager fragments = getChildFragmentManager();
            LocationConfigDialog dialog = (LocationConfigDialog) fragments.findFragmentByTag(DIALOG_LOCATION);
            if (dialog != null)
            {
                setEvent(getEvent(), location);
                updateViews(getActivity());
                return true;
            }
            return false;
        }
    };

    protected void showByTimeFragment()
    {
        FragmentManager fragments = getChildFragmentManager();
        FragmentTransaction transaction = fragments.beginTransaction();

        AlarmTimeDialog fragment = new AlarmTimeDialog();
        fragment.setTime(getHour(), getMinute());
        fragment.setTimeZone(getTimeZone());
        fragment.setLocation(getLocation());
        fragment.set24Hour(SuntimesUtils.is24());
        fragment.setDialogListener(new AlarmTimeDialog.DialogListener()
        {
            @Override
            public void onChanged(AlarmTimeDialog dialog)
            {
                getArguments().putInt(EXTRA_HOUR, dialog.getHour());
                getArguments().putInt(EXTRA_MINUTE, dialog.getMinute());
                getArguments().putString(EXTRA_TIMEZONE, dialog.getTimeZone());
                updateViews(getActivity());
            }

            @Override
            public void onLocationClick(AlarmTimeDialog dialog) {
                showLocationDialog(getActivity());
            }

            @Override
            public void onAccepted(AlarmTimeDialog dialog) {
                onChanged(dialog);
            }

            @Override
            public void onCanceled(AlarmTimeDialog dialog) {}
        });

        transaction.replace(R.id.fragmentContainer1, fragment, "AlarmTimeDialog");
        transaction.commit();
    }

    private void initViews(final Context context, View dialogContent)
    {
        text_title = (TextView) dialogContent.findViewById(R.id.dialog_title);
        text_time = (TextSwitcher) dialogContent.findViewById(R.id.text_datetime);
        text_offset = (TextView) dialogContent.findViewById(R.id.text_datetime_offset);
        icon_offset = (ImageView) dialogContent.findViewById(R.id.icon_datetime_offset);
        text_date = (TextView) dialogContent.findViewById(R.id.text_date);
        text_note = (TextView) dialogContent.findViewById(R.id.text_note);

        spin_type = (Spinner) dialogContent.findViewById(R.id.type_spin);
        spin_type.setAdapter(new AlarmTypeAdapter(getContext(), R.layout.layout_listitem_alarmtype));

        tabs = (TabLayout) dialogContent.findViewById(R.id.tabLayout);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                getArguments().putInt(EXTRA_MODE, tab.getPosition());
                showFragmentForMode(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        Button btn_cancel = (Button) dialogContent.findViewById(R.id.dialog_button_cancel);
        if (btn_cancel != null) {
            btn_cancel.setOnClickListener(onDialogCancelClick);
        }

        ImageButton btn_accept = (ImageButton) dialogContent.findViewById(R.id.dialog_button_accept);
        if (btn_accept != null) {
            btn_accept.setOnClickListener(onDialogAcceptClick);
        }

        Button btn_neutral = (Button) dialogContent.findViewById(R.id.dialog_button_neutral);
        if (btn_neutral != null) {
            btn_neutral.setOnClickListener(onDialogNeutralClick);
        }

        View layout_time = dialogContent.findViewById(R.id.layout_datetime);
        if (layout_time != null) {
            layout_time.setOnClickListener(onDialogBottomBarClick);
        }

        btn_alarms = (ImageButton) dialogContent.findViewById(R.id.dialog_button_alarms);
        if (btn_alarms != null) {
            btn_alarms.setOnClickListener(onDialogNeutralClick);
        }
    }

    private AdapterView.OnItemSelectedListener onTypeSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.d("DEBUG", "onItemSelected: " + position);
            setAlarmType((AlarmClockItem.AlarmType) parent.getItemAtPosition(position));
            updateViews(getActivity());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    };

    private void updateViews(Context context)
    {
        detachListeners();

        if (btn_alarms != null) {
            btn_alarms.setVisibility(showAlarmListButton() ? View.VISIBLE : View.GONE);
        }

        AlarmClockItem.AlarmType alarmType = getAlarmType();
        AlarmClockItem item = createAlarm(AlarmCreateDialog.this, alarmType);
        item.offset = getOffset();
        boolean isSchedulable = AlarmNotifications.updateAlarmTime(context, item);

        if (text_title != null) {
            text_title.setText(context.getString(alarmType == AlarmClockItem.AlarmType.NOTIFICATION ? R.string.configAction_addNotification : R.string.configAction_addAlarm));
        }
        if (spin_type != null) {
            spin_type.setSelection(alarmType.ordinal(), false);
        }

        if (text_offset != null) {
            text_offset.setText(isSchedulable ? AlarmEditViewHolder.displayOffset(context, item) : "");
        }
        if (text_time != null) {
            text_time.setText(isSchedulable ? AlarmEditViewHolder.displayAlarmTime(context, item, previewOffset()) : "");
        }
        if (text_date != null)
        {
            text_date.setText(isSchedulable ? AlarmEditViewHolder.displayAlarmDate(context, item, previewOffset()) : "");
            text_date.setVisibility(isSchedulable && AlarmEditViewHolder.showAlarmDate(context, item) ? View.VISIBLE : View.GONE);
        }
        if (text_note != null) {    // TODO: periodic update
            text_note.setText(AlarmEditViewHolder.displayAlarmNote(context, item, isSchedulable));
        }

        attachListeners();
    }

    protected void attachListeners()
    {
        if (spin_type != null) {
            spin_type.setOnItemSelectedListener(onTypeSelected);
        }
    }

    protected void detachListeners()
    {
        if (spin_type != null) {
            spin_type.setOnItemSelectedListener(null);
        }
    }

    public static class AlarmTypeAdapter extends ArrayAdapter<AlarmClockItem.AlarmType>
    {
        protected int layout;

        public AlarmTypeAdapter(@NonNull Context context, int resource)
        {
            super(context, resource);
            layout = resource;

            if (Build.VERSION.SDK_INT >= 11) {
                addAll(AlarmClockItem.AlarmType.values());
            } else {
                for (AlarmClockItem.AlarmType type : AlarmClockItem.AlarmType.values()) {
                    add(type);
                }
            }
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            return createView(position, convertView, parent);
        }
        @NonNull @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            return createView(position, convertView, parent);
        }

        @SuppressLint("ResourceType")
        private View createView(int position, View convertView, ViewGroup parent)
        {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                view = inflater.inflate(layout, parent, false);
            }

            int[] iconAttr = { R.attr.icActionAlarm, R.attr.icActionNotification };
            TypedArray typedArray = getContext().obtainStyledAttributes(iconAttr);
            int res_iconAlarm = typedArray.getResourceId(0, R.drawable.ic_action_alarms);
            int res_iconNotification = typedArray.getResourceId(1, R.drawable.ic_action_notification);
            typedArray.recycle();

            ImageView icon = (ImageView) view.findViewById(android.R.id.icon1);
            TextView text = (TextView) view.findViewById(android.R.id.text1);
            AlarmClockItem.AlarmType alarmType = getItem(position);
            if (alarmType != null)
            {
                icon.setImageDrawable(null);
                icon.setBackgroundResource(alarmType == AlarmClockItem.AlarmType.NOTIFICATION ? res_iconNotification : res_iconAlarm);
                text.setText(alarmType.getDisplayString());
            } else {
                icon.setImageDrawable(null);
                icon.setBackgroundResource(0);
                text.setText("");
            }

            return view;
        }
    }

    @SuppressWarnings({"deprecation","RestrictedApi"})
    @NonNull @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(onDialogShow);
        return dialog;
    }

    @Override
    public void onSaveInstanceState( Bundle outState )
    {
        saveSettings(outState);
        super.onSaveInstanceState(outState);
    }

    protected void loadSettings(Bundle bundle) {}

    public void loadSettings(Context context)
    {
        loadSettings(context.getSharedPreferences(PREFS_ALARMCREATE, 0));
    }
    public void loadSettings(SharedPreferences prefs)
    {
        Bundle args = getArguments();
        args.putInt(EXTRA_MODE, prefs.getInt(EXTRA_MODE, getDialogMode()));
        args.putInt(EXTRA_HOUR, prefs.getInt(EXTRA_HOUR, getHour()));
        args.putInt(EXTRA_MINUTE, prefs.getInt(EXTRA_MINUTE, getMinute()));
        args.putString(EXTRA_TIMEZONE, prefs.getString(EXTRA_TIMEZONE, getTimeZone()));
        args.putSerializable(EXTRA_EVENT, SolarEvents.valueOf(prefs.getString(EXTRA_EVENT, SolarEvents.SUNRISE.name())));;
        args.putSerializable(EXTRA_ALARMTYPE, AlarmClockItem.AlarmType.valueOf(prefs.getString(EXTRA_ALARMTYPE, AlarmClockItem.AlarmType.ALARM.name())));

        if (isAdded())
        {
            FragmentManager fragments = getChildFragmentManager();
            AlarmDialog fragment0 = (AlarmDialog) fragments.findFragmentByTag("AlarmDialog");
            if (fragment0 != null) {
                initEventDialog(getActivity(), fragment0, getLocation());
                fragment0.setChoice(getEvent());
                fragment0.setType(getAlarmType());
            }

            AlarmTimeDialog fragment1 = (AlarmTimeDialog) fragments.findFragmentByTag("AlarmTimeDialog");
            if (fragment1 != null) {
                fragment1.setLocation(getLocation());
                fragment1.setTime(getHour(), getMinute());
                fragment1.setTimeZone(getTimeZone());
                fragment1.updateViews(getActivity());
            }

            updateViews(getActivity());
        }
    }

    protected void saveSettings(Bundle bundle) {}

    public void saveSettings(@Nullable Context context) {
        if (context != null) {
            saveSettings(context.getSharedPreferences(PREFS_ALARMCREATE, 0));
        }
    }
    public void saveSettings(SharedPreferences prefs)
    {
        SharedPreferences.Editor out = prefs.edit();
        out.putInt(EXTRA_MODE, getDialogMode());
        out.putInt(EXTRA_HOUR, getHour());
        out.putInt(EXTRA_MINUTE, getMinute());
        out.putString(EXTRA_TIMEZONE, getTimeZone());
        out.putString(EXTRA_EVENT, getEvent().name());
        out.putString(EXTRA_ALARMTYPE, getAlarmType().name());
        out.apply();
    }

    private DialogInterface.OnClickListener onAccepted = null;
    public void setOnAcceptedListener( DialogInterface.OnClickListener listener ) {
        onAccepted = listener;
    }

    private DialogInterface.OnClickListener onCanceled = null;
    public void setOnCanceledListener( DialogInterface.OnClickListener listener ) {
        onCanceled = listener;
    }

    private DialogInterface.OnClickListener onNeutral = null;
    public void setOnNeutralListener( DialogInterface.OnClickListener listener) {
        onNeutral = listener;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        FragmentManager fragments = getChildFragmentManager();
        LocationConfigDialog locationDialog = (LocationConfigDialog) fragments.findFragmentByTag(DIALOG_LOCATION);
        if (locationDialog != null) {
            locationDialog.setDialogListener(onLocationChanged);
        }
    }

    private DialogInterface.OnShowListener onDialogShow = new DialogInterface.OnShowListener()
    {
        @Override
        public void onShow(DialogInterface dialog)
        {
            BottomSheetDialog bottomSheet = (BottomSheetDialog) dialog;
            FrameLayout layout = (FrameLayout) bottomSheet.findViewById(android.support.design.R.id.design_bottom_sheet);  // for AndroidX, resource is renamed to com.google.android.material.R.id.design_bottom_sheet
            if (layout != null)
            {
                final BottomSheetBehavior behavior = BottomSheetBehavior.from(layout);
                behavior.setPeekHeight((int)getResources().getDimension(R.dimen.alarmcreate_bottomsheet_peek));
                layout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }, AUTO_EXPAND_DELAY);
            }
        }
    };
    public static final int AUTO_EXPAND_DELAY = 500;

    private View.OnClickListener onDialogNeutralClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (onNeutral != null) {
                onNeutral.onClick(getDialog(), 0);
            }
        }
    };

    private View.OnClickListener onDialogCancelClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (getShowsDialog())
            {
                getDialog().cancel();

            } else if (onCanceled != null) {
                onCanceled.onClick(getDialog(), 0);
            }
        }
    };

    @Override
    public void onCancel(DialogInterface dialog)
    {
        if (onCanceled != null) {
            onCanceled.onClick(getDialog(), 0);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog)
    {
        super.onDismiss(dialog);
        saveSettings(getActivity());
    }

    private View.OnClickListener onDialogAcceptClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (onAccepted != null) {
                onAccepted.onClick(getDialog(), 0);
            }
            if (getShowsDialog()) {
                dismiss();
            }
        }
    };

    private View.OnClickListener onDialogBottomBarClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setPreviewOffset(!previewOffset());
            animatePreviewOffset(AlarmCreateDialog.this, previewOffset());
        }
    };

    protected void animatePreviewOffset(final AlarmCreateDialog dialog, final boolean enable)
    {
        if (dialog == null || dialog.getActivity() == null || !isAdded()) {
            return;
        }

        AlarmClockItem item = createAlarm(dialog, getAlarmType());
        item.offset = getOffset();
        boolean isSchedulable = AlarmNotifications.updateAlarmTime(getActivity(), item);

        if (text_time != null) {
            text_time.setText(isSchedulable ? AlarmEditViewHolder.displayAlarmTime(getActivity(), item, enable) : "");
        }
        if (text_date != null) {
            text_date.setText(isSchedulable ? AlarmEditViewHolder.displayAlarmDate(getActivity(), item, enable): "");
        }

        if (Build.VERSION.SDK_INT >= 14)
        {
            if (!enable && text_offset != null) {
                text_offset.setAlpha(0.0f);
                text_offset.setVisibility(View.VISIBLE);
            }
            if (icon_offset != null) {
                icon_offset.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
            }

            if (text_offset != null)
            {
                text_offset.animate().translationY((enable ? 2 * text_offset.getHeight() : 0))
                        .alpha(enable ? 0.0f : 1.0f).setListener(new Animator.AnimatorListener() {
                    public void onAnimationCancel(Animator animation) {}
                    public void onAnimationRepeat(Animator animation) {}
                    public void onAnimationStart(Animator animation) {}
                    public void onAnimationEnd(Animator animation) {
                        onAnimatePreviewOffsetEnd(dialog, enable);
                    }
                });
            }

        } else {
            onAnimatePreviewOffsetEnd(dialog, enable);
        }
    }
    public static final int PREVIEW_OFFSET_DURATION_MILLIS = 1500;

    protected void onAnimatePreviewOffsetEnd(final AlarmCreateDialog dialog, boolean enable)
    {
        text_offset.setVisibility(enable ? View.INVISIBLE : View.VISIBLE);
        if (enable)
        {
            text_offset.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setPreviewOffset(false);
                    animatePreviewOffset(dialog,false);
                }
            }, PREVIEW_OFFSET_DURATION_MILLIS);
        }
    }

    public boolean previewOffset() {
        return getArguments().getBoolean(EXTRA_PREVIEW_OFFSET, false);
    }
    public void setPreviewOffset(boolean value) {
        getArguments().putBoolean(EXTRA_PREVIEW_OFFSET, value);
    }

    public boolean showAlarmListButton() {
        return getArguments().getBoolean(EXTRA_BUTTON_ALARMLIST, false);
    }
    public void setShowAlarmListButton(boolean value) {
        getArguments().putBoolean(EXTRA_BUTTON_ALARMLIST, value);
    }

    public int getMode() {
        return tabs.getSelectedTabPosition();
    }

    public SolarEvents getEvent()
    {
        SolarEvents event = (SolarEvents) getArguments().getSerializable(EXTRA_EVENT);
        return (event != null ? event : DEF_EVENT);
    }
    public Location getLocation()
    {
        Location location = getArguments().getParcelable(EXTRA_LOCATION);
        return (location != null ? location : WidgetSettings.loadLocationPref(getActivity(), 0));
    }
    public void setEvent( SolarEvents event, Location location )
    {
        Bundle args = getArguments();
        args.putSerializable(EXTRA_EVENT, event);
        args.putParcelable(EXTRA_LOCATION, location);

        if (isAdded())
        {
            FragmentManager fragments = getChildFragmentManager();
            AlarmDialog fragment0 = (AlarmDialog) fragments.findFragmentByTag("AlarmDialog");
            if (fragment0 != null) {
                initEventDialog(getActivity(), fragment0, location);
                fragment0.setChoice(event);
            }

            AlarmTimeDialog fragment1 = (AlarmTimeDialog) fragments.findFragmentByTag("AlarmTimeDialog");
            if (fragment1 != null) {
                fragment1.setLocation(location);
                fragment1.updateViews(getActivity());
            }
        }
    }

    public int getHour() {
        return getArguments().getInt(EXTRA_HOUR, DEF_HOUR);
    }
    public int getMinute() {
        return getArguments().getInt(EXTRA_MINUTE, DEF_MINUTE);
    }
    public long getDate() {
        return getArguments().getLong(EXTRA_DATE, System.currentTimeMillis());
    }
    public String getTimeZone() {
        return getArguments().getString(EXTRA_TIMEZONE);
    }
    public void setAlarmTime( int hour, int minute, String timezone )
    {
        Bundle args = getArguments();
        args.putInt(EXTRA_HOUR, hour);
        args.putInt(EXTRA_MINUTE, minute);
        args.putString(EXTRA_TIMEZONE, timezone);

        if (isAdded())
        {
            FragmentManager fragments = getChildFragmentManager();
            AlarmTimeDialog fragment1 = (AlarmTimeDialog) fragments.findFragmentByTag("AlarmTimeDialog");
            if (fragment1 != null) {
                fragment1.setTime(hour, minute);
                fragment1.setTimeZone(timezone);
                fragment1.updateViews(getActivity());
            }

            updateViews(getActivity());
        }
    }

    public void setDialogMode(int mode) {
        getArguments().putInt(EXTRA_MODE, mode);
    }
    public int getDialogMode() {
        return getArguments().getInt(EXTRA_MODE, DEF_MODE);
    }

    public void setAlarmType(AlarmClockItem.AlarmType value)
    {
        getArguments().putSerializable(EXTRA_ALARMTYPE, value);
        if (isAdded())
        {
            FragmentManager fragments = getChildFragmentManager();
            AlarmDialog fragment = (AlarmDialog) fragments.findFragmentByTag("AlarmDialog");
            if (fragment != null) {
                fragment.setType(getAlarmType());
            }
        }
    }
    public AlarmClockItem.AlarmType getAlarmType() {
        return (AlarmClockItem.AlarmType) getArguments().getSerializable(EXTRA_ALARMTYPE);
    }

    public void setOffset(long offset)
    {
        getArguments().putLong(EXTRA_OFFSET, offset);
        if (isAdded()) {
            updateViews(getActivity());
        }
    }
    public long getOffset() {
        return getArguments().getLong(EXTRA_OFFSET, 0);
    }

    private void initEventDialog(Context context, AlarmDialog dialog, Location forLocation)
    {
        SuntimesRiseSetDataset sunData = new SuntimesRiseSetDataset(context, 0);
        SuntimesMoonData moonData = new SuntimesMoonData(context, 0);
        SuntimesEquinoxSolsticeDataset equinoxData = new SuntimesEquinoxSolsticeDataset(context, 0);

        if (forLocation != null) {
            sunData.setLocation(forLocation);
            moonData.setLocation(forLocation);
            equinoxData.setLocation(forLocation);
        }

        sunData.calculateData();
        moonData.calculate();
        equinoxData.calculateData();
        dialog.setData(context, sunData, moonData, equinoxData);
    }

    public static AlarmClockItem createAlarm(@NonNull AlarmCreateDialog dialog, AlarmClockItem.AlarmType type)
    {
        int hour;
        int minute;
        SolarEvents event;
        String timezone;

        if (dialog.getMode() == 0)
        {
            hour = -1;
            minute = -1;
            timezone = null;
            event = dialog.getEvent();

        } else {
            hour = dialog.getHour();
            minute = dialog.getMinute();
            timezone = dialog.getTimeZone();
            event = null;
        }

        return AlarmListDialog.createAlarm(dialog.getActivity(), type, "", event, dialog.getLocation(), hour, minute, timezone, AlarmSettings.loadPrefVibrateDefault(dialog.getActivity()), AlarmSettings.getDefaultRingtoneUri(dialog.getActivity(), type), AlarmRepeatDialog.PREF_DEF_ALARM_REPEATDAYS);
    }

    public static void updateAlarmItem(AlarmCreateDialog dialog, AlarmClockItem item)
    {
        item.type = dialog.getAlarmType();
        item.location = dialog.getLocation();

        if (dialog.getMode() == 0)
        {
            item.hour = -1;
            item.minute = -1;
            item.timezone = null;
            item.event = dialog.getEvent();

        } else {
            item.hour = dialog.getHour();
            item.minute = dialog.getMinute();
            item.timezone = dialog.getTimeZone();
            item.event = null;
        }
    }

}
