package com.coraducci.ahcoso.utils;

import android.content.Context;
import android.content.Intent;

public class SendBroadcastTo {

    private Context CONTEXT;
    private Intent intent;

    public SendBroadcastTo(Context CONTEXT, String receiver) {
        this.CONTEXT = CONTEXT;
        intent = new Intent(receiver);
    }

    public void addExtra(BroadcastHelper broadcastHelper){
        switch(broadcastHelper.type){
            case(Types.STRING):
                intent.putExtra(broadcastHelper.extra, broadcastHelper.valueString);
                break;
            case(Types.BOOLEAN):
                intent.putExtra(broadcastHelper.extra, broadcastHelper.valueBoolean);
                break;
            case(Types.INT):
                intent.putExtra(broadcastHelper.extra, broadcastHelper.valueInteger);
                break;
            case(Types.FLOAT):
                intent.putExtra(broadcastHelper.extra, broadcastHelper.valueFloat);
                break;
            case(Types.DOUBLE):
                intent.putExtra(broadcastHelper.extra, broadcastHelper.valueDouble);
                break;
            case(Types.BYTE_ARRAY):
                intent.putExtra(broadcastHelper.extra, broadcastHelper.valueByteArray);
                break;
        }
    }
    public void addClass(Class<?> serviceClass){
        intent.setClass(CONTEXT, serviceClass);
    }

    public void addAction(String action){
        intent.setAction(action);
    }

    public void send(){
        CONTEXT.sendBroadcast(intent);
    }
}
