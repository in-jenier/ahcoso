package com.coraducci.ahcoso.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class Now {

    private String TIME_FORMAT = "HH:mm";
    private Calendar calendar;
    public Now(){
        Instance();
    }
    public void Instance(){
        calendar = Calendar.getInstance(Locale.getDefault());
    }
    public String Moment() {
        Instance();
        return getValueFormatted(true, getHours()) + ":" + getValueFormatted(true, getMinutes()) + ":" + getValueFormatted(true, getSeconds());
    }
    public String MomentWithDate() {
        Instance();
        return getValueFormatted(true, getYear()) + ":" + getValueFormatted(true, getMonth()) + ":" + getValueFormatted(true, getDay()) + "-" + Moment();
    }
    public String Clock() {
        Instance();
        return new SimpleDateFormat(TIME_FORMAT).format(calendar.getTime());
    }
    public int getHours(){
        //Instance();
        return calendar.get(Calendar.HOUR_OF_DAY);
    }
    public int getMinutes(){
        //Instance();
        return calendar.get(Calendar.MINUTE);
    }
    public int getSeconds(){
        //Instance();
        return calendar.get(Calendar.SECOND);
    }
    public int getDay(){
        //Instance();
        return calendar.get(Calendar.DAY_OF_MONTH);
    }
    public int getMonth(){
        //Instance();
        return calendar.get(Calendar.MONTH) + 1;
    }
    public int getYear(){
        //Instance();
        return calendar.get(Calendar.YEAR);
    }
    public String getValueFormatted(Boolean fillZero, int value){
        String x = String.valueOf(value);
        if(x.length() == 1 && fillZero) {
            x = "0" + x;
        }
        return x;
    }
}
