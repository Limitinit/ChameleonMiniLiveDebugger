/*
This program (The Chameleon Mini Live Debugger) is free software written by
Maxie Dion Schmidt: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

The complete license provided with source distributions of this library is
available at the following link:
https://github.com/maxieds/ChameleonMiniLiveDebugger
*/

package com.maxieds.chameleonminilivedebugger;

import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;

public class ThemesConfiguration {

    private static final String TAG = ThemesConfiguration.class.getSimpleName();

    /**
     * Obtains the color associated with the theme.
     * @param attrID
     * @return
     */
    @ColorInt
    public static int getThemeColorVariant(int attrID) {
        return LiveLoggerActivity.getInstance().getTheme().obtainStyledAttributes(new int[] {attrID}).getColor(0, attrID);
    }

    @ColorInt
    public static int getThemeColorVariant(ChameleonMiniLiveDebuggerActivity activity, int attrID) {
        return activity.getInstance().getTheme().obtainStyledAttributes(new int[] {attrID}).getColor(0, attrID);
    }

    /**
     * Attempts to set themes will a background before the current tab has been loaded will
     * result in a NullPointerException getting issued by the system. We fix this by setting the
     * theme about 1 second after the application's onCreate(...) method is invoked.
     */
    public static String storedAppTheme = "Standard Green";
    public static int appThemeResID = R.style.AppTheme;
    public static Handler setThemeHandler = new Handler();
    public static Runnable setThemeRunner = new Runnable() {
        @Override
        public void run() {
            setLocalTheme(storedAppTheme, true);
        }
    };

    /**
     * Sets the local theme (before the ful UI updating to implement the theme change) based on
     * the passed theme text description.
     * @param themeDesc
     * @ref res/values/style.xml
     */
    public static int setLocalTheme(String themeDesc, boolean applyTheme) {
        int themeID;
        switch(themeDesc) {
            case "Atlanta":
                themeID = R.style.AppThemeAtlanta;
                break;
            case "Black":
                themeID = R.style.AppThemeBlack;
                break;
            case "Chicky":
                themeID = R.style.AppThemeChicky;
                break;
            case "Crash Report":
                themeID = R.style.AppThemeCrashReport;
                break;
            case "Frosty":
                themeID = R.style.AppThemeFrosty;
                break;
            case "Linux Green on Black":
                themeID = R.style.AppThemeLinuxGreenOnBlack;
                break;
            case "Purple":
                themeID = R.style.AppThemePurple;
                break;
            case "RaspberryPI":
                themeID = R.style.AppThemeRaspberryPI;
                break;
            case "Redmond":
                themeID = R.style.AppThemeRedmond;
                break;
            case "Teal":
                themeID = R.style.AppThemeTeal;
                break;
            case "Sunshine":
                themeID = R.style.AppThemeSunshine;
                break;
            case "Standard Green (Default)":
                if(BuildConfig.FLAVOR.equals("paid")) {
                    themeID = R.style.AppThemeGreenPaid;
                }
                else {
                    themeID = R.style.AppThemeGreen;
                }
                break;
            case "Urbana DESFire":
                themeID = R.style.AppThemeUrbanaDesfire;
                break;
            case "Winter":
                themeID = R.style.AppThemeWinter;
                break;
            default:
                return appThemeResID;
        }
        if(applyTheme) {
            Log.w(TAG, themeDesc);
            Log.w(TAG, String.valueOf(themeID));
            LiveLoggerActivity.getInstance().setTheme(themeID);
            appThemeResID = themeID;
        }
        return themeID;
    }

    public static void actionButtonAppSettings(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(LiveLoggerActivity.getInstance(), appThemeResID);
        final View dialogView = LiveLoggerActivity.getInstance().getLayoutInflater().inflate(R.layout.theme_config, null);
        // set the correct current theme as the selected radio button:
        RadioGroup themeRadioGroup = (RadioGroup) dialogView.findViewById(R.id.themeRadioGroup);
        for(int rb = 0; rb < themeRadioGroup.getChildCount(); rb++) {
            RadioButton curThemeBtn = (RadioButton) themeRadioGroup.getChildAt(rb);
            if(curThemeBtn.isEnabled() && curThemeBtn.getText().toString().equals(storedAppTheme)) {
                curThemeBtn.setChecked(true);
                break;
            }
        }
        // finish constructing the theme selection dialog:
        ScrollView themesScroller = new ScrollView(LiveLoggerActivity.getInstance());
        themesScroller.addView(dialogView);
        dialog.setView(themesScroller);
        dialog.setIcon(R.drawable.settingsgears24);
        dialog.setMessage("Set the color profile and toolbar icon for the application.");
        dialog.setTitle( "Application Themes:");
        dialog.setPositiveButton( "Set Theme️", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int whichBtn) {
                int getSelectedOption = ((RadioGroup) dialogView.findViewById(R.id.themeRadioGroup)).getCheckedRadioButtonId();
                String themeID = ((RadioButton) dialogView.findViewById(getSelectedOption)).getText().toString();
                String themeDesc = themeID;
                setLocalTheme(themeDesc, true);
                storedAppTheme = themeDesc;
                // store the theme setting for when the app reopens:
                ThemesConfiguration.storedAppTheme = themeDesc;
                AndroidSettingsStorage.updateValueByKey(AndroidSettingsStorage.THEMEID_PREFERENCE);
                // finally, apply the theme settings by (essentially) restarting the activity UI:
                MainActivityLogUtils.appendNewLog(LogEntryMetadataRecord.createDefaultEventRecord("THEME", "New theme installed: " + themeDesc));
                LiveLoggerActivity.getInstance().recreate();

            }
        });
        dialog.setNegativeButton( "Cancel", null);
        dialog.setInverseBackgroundForced(true);
        final AlertDialog displayDialog = dialog.create();
        displayDialog.show();
    }
}
