package com.coraducci.ahcoso.utils;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.coraducci.ahcoso.R;

public class CommonNotificationSystem {
    private final Context CONTEXT;

    public int SERVICE_ID;
    private String CHANNEL_ID;
    private String CHANNEL_NAME;
    private String CHANNEL_DESCRIPTION;

    private int NOTIFY_NUMBER = 1;
    public boolean sameChannel = false;

    private int iconIdle;
    private int iconRun;
    public boolean customIcons = false;

    public CommonNotificationSystem(Context context) {
        CONTEXT = context;
    }
    public void setIcons(){
        switch (CHANNEL_ID){
            case(Costants.CHANNEL_ID_CAMERA):
                iconIdle = Costants.STANDARD_NOTIFY_ICON_CAMERA_IDLE;
                iconRun = Costants.STANDARD_NOTIFY_ICON_CAMERA_RUN;
                if(customIcons) {
                    iconIdle = R.drawable.ic_stat_camera_idle;
                    iconRun = R.drawable.ic_stat_camera_run;
                }
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setParams(int notifyNumber, int svcID, String chlID, String chlName, String chlDescription){
        if(sameChannel){
            NOTIFY_NUMBER = 1;
            SERVICE_ID = Costants.NOTIFY_SERVICE_ID;
            CHANNEL_NAME = Costants.NOTIFY_CHANNEL_NAME;
            CHANNEL_DESCRIPTION = Costants.NOTIFY_CHANNEL_DESCRIPTION;

            if(!customIcons) {
                iconIdle = R.drawable.ic_stat_camera_idle;
                iconRun = R.drawable.ic_stat_camera_run;
            }
        }else{
            NOTIFY_NUMBER = notifyNumber;
            SERVICE_ID = svcID;
            CHANNEL_NAME = chlName;
            CHANNEL_DESCRIPTION = chlDescription;
        }
        CHANNEL_ID = chlID;
        setIcons();
    }
    //Android 8
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification Android_O() {
        return SimpleNotify(CHANNEL_NAME, CHANNEL_DESCRIPTION);
    }
    //Android 9
    @RequiresApi(api = Build.VERSION_CODES.P)
    public Notification Android_P() {
        createChannel();
        return SimpleNotify(CHANNEL_NAME, CHANNEL_DESCRIPTION);
    }
    //Android 9
    @RequiresApi(api = Build.VERSION_CODES.P)
    public Notification Android_P_Advanced() {
        createChannel();
        return SimpleNotify(CHANNEL_NAME, CHANNEL_DESCRIPTION);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel createChannel(){
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(CHANNEL_DESCRIPTION);
        channel.setSound(null, null);
        channel.setImportance(NotificationManager.IMPORTANCE_LOW);
        getManager().createNotificationChannel(channel);
        return channel;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel getChannel(){
        return getManager().getNotificationChannel(CHANNEL_ID);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationManager getManager(){
        return (NotificationManager) CONTEXT.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    private Notification SimpleNotify(String title, String description){
        return new NotificationCompat.Builder(CONTEXT, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(description)
                .setNotificationSilent()
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }
    @SuppressLint({"NewApi", "NotificationPermission"})
    public void MakeNotification(String title, String description, Boolean update, Boolean mainNotification, boolean idleTime) {
        int icon = iconIdle;
        if (!idleTime) {
            icon = iconRun;
        }
        Notification notification = new NotificationCompat.Builder(CONTEXT, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(description)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(icon)
                .build();
        if (update) {
            int number = SERVICE_ID;
            if (!mainNotification) {
                number = NOTIFY_NUMBER;
            }
            getManager().notify(number, notification);
        }
    }

    @SuppressLint({"NewApi", "NotificationPermission"})
    public void MakeNotificationWithImage(String title, String contextText, String description, Bitmap image, Boolean update, Boolean mainNotification, boolean idleTime){
        int icon = iconIdle;
        if (!idleTime) {
            icon = iconRun;
        }
        Notification notification = new NotificationCompat.Builder(CONTEXT, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(contextText)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(icon)
                .setLargeIcon(image)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(image)
                        .setSummaryText(description))
                .build();
        if(update){
            int number = SERVICE_ID;
            if(!mainNotification){
                number = NOTIFY_NUMBER;
            }
            getManager().notify(number, notification);
        }
    }

    @SuppressLint({"NewApi", "MissingPermission", "NotificationPermission"})
    public void MakeNotificationWithImage(String title, String contextText, String description, Bitmap image, Boolean update, Boolean mainNotification, boolean idleTime, PendingIntent pendingIntent){
        int icon = iconIdle;
        if (!idleTime) {
            icon = iconRun;
        }
        Notification notification = new NotificationCompat.Builder(CONTEXT, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(contextText)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(icon)
                .setLargeIcon(image)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(image)
                        .setSummaryText(description))
                .setContentIntent(pendingIntent)
                .build();
        if(update){
            int number = SERVICE_ID;
            if(!mainNotification){
                number = NOTIFY_NUMBER;
            }
            getManager().notify(number, notification);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.P)
    public void removeChannel(String chnID){
        if(checkExists()) {
            getManager().deleteNotificationChannel(chnID);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean checkExists(){
        boolean exists = false;
        StatusBarNotification[] statusBarNotificationList = getManager().getActiveNotifications();
        for (StatusBarNotification notification: statusBarNotificationList) {
            if(notification.getId() == SERVICE_ID){
                exists = true;
            }
        }
        return exists;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean checkExists(int nodifyID){
        boolean exists = false;
        StatusBarNotification[] statusBarNotificationList = getManager().getActiveNotifications();
        for (StatusBarNotification notification: statusBarNotificationList) {
            if(notification.getId() == nodifyID){
                exists = true;
            }
        }
        return exists;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void removeNotify(){
        getManager().cancel(SERVICE_ID);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void removeNotify(int number){
        getManager().cancel(number);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void removeAllNotify(){
        getManager().cancelAll();
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void active(){
        StatusBarNotification[] statusBarNotificationList = getManager().getActiveNotifications();
        ToastOrLog.i("CNS", "active... START");
        for (StatusBarNotification notification: statusBarNotificationList) {
            ToastOrLog.i("CNS", "getGroupKey:" + notification.getGroupKey());
            ToastOrLog.i("CNS", "getKey:" + notification.getKey());
            ToastOrLog.i("CNS", "getTag:" + notification.getTag());
            ToastOrLog.i("CNS", "getId:" + notification.getId());
            ToastOrLog.i("CNS", "getPackageName:" + notification.getPackageName());
            ToastOrLog.i("CNS", "getChannelId:"+notification.getNotification().getChannelId());
            ToastOrLog.i("CNS", "group:" + notification.getNotification().getGroup());
            ToastOrLog.i("CNS", "=====================");
        }
        ToastOrLog.i("CNS", "active... END");
    }
}