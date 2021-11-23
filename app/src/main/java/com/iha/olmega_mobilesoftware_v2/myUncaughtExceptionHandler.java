package com.iha.olmega_mobilesoftware_v2;

import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.Core.LogIHAB;

import java.io.PrintWriter;
import java.io.StringWriter;

public class myUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Context myContext;
    private final Class<?> myActivityClass;
    private String TAG = this.getClass().getSimpleName();

    public myUncaughtExceptionHandler(Context context, Class<?> c) {
        myContext = context;
        myActivityClass = c;
    }

    public void uncaughtException(Thread thread, Throwable exception) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        Log.e(TAG, sw.toString());
        LogIHAB.log("<begin stacktrace>\n" + sw.toString() + "\n<end stacktrace>");
        Intent intent = new Intent(myContext, myActivityClass);
        //you can use this String to know what caused the exception and in which Activity
        intent.putExtra("uncaughtException", "Exception is: " + sw.toString());
        intent.putExtra("stacktrace", sw.toString());
        myContext.startActivity(intent);
        //for restarting the Activity
        Process.killProcess(Process.myPid());
        System.exit(0);
    }
}
