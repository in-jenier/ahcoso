package com.coraducci.ahcoso.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class Messages {
    public static String NOFITY_SWITCH_TO_SAME = "Switch to SAME notity";
    public static String NOFITY_SWITCH_TO_SEPARATE = "Switch to SEPARATE notity";
    public static class ServiceCamera {
        public static String NEED_DISABLE_CENSUS = "Necessario disattivare censimento";
    }
    public static void setCulture(Context context, Locale locale){
        /*
        Before, set new colture in the current configuration
         */
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        /*
        Reload all strings in the new culture
         */
        //NOFITY_SWITCH_TO_SAME = context.getResources().getString(R.string.notify_swith_to_same);
        //NOFITY_SWITCH_TO_SEPARATE = context.getResources().getString(R.string.notify_swith_to_separate);
    }
}
