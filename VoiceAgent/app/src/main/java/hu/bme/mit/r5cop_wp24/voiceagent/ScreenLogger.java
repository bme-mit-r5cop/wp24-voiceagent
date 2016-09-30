package hu.bme.mit.r5cop_wp24.voiceagent;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by steve on 9/30/16.
 */
public class ScreenLogger {

    public ScreenLogger(TextView textView, Activity activity) {

        ScreenLogger.tv = textView;
        ScreenLogger.act = activity;
        sb = new StringBuilder();
    }

    static TextView tv;
    static Activity act;
    static StringBuilder sb;


    public static void e(String tag, String s) {
        log(tag, s);
        Log.e(tag, s);
    }
    public static void i(String tag, String s) {
        log(tag, s);
        Log.i(tag, s);
    }
    public static void d(String tag, String s) {
        log(tag, s);
        Log.d(tag, s);
    }
    public static void v(String tag, String s) {
        log(tag, s);
        Log.v(tag, s);
    }

    private static void log(final String tag, final String s) {
        if (tv != null) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (tv != null) {
                        sb.append(s);
                        sb.append("\n***\n");
                        tv.setText(sb.toString());
                        final int scrollAmount = tv.getLayout().getLineTop(tv.getLineCount()) - tv.getHeight();
                        if (scrollAmount > 0)
                            tv.scrollTo(0, scrollAmount);
                        else
                            tv.scrollTo(0, 0);
                    }
                }
            });
        }
    }

    public static void destroy() {
        tv = null;
        act = null;
        sb = null;
    }

}
