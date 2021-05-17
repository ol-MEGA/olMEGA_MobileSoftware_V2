package com.iha.olmega_mobilesoftware_v2.Questionnaire.Core;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatActivity;

import com.iha.olmega_mobilesoftware_v2.R;

/**
 * Created by ulrikkowalk on 01.03.17.
 */

public class Units extends AppCompatActivity {

    private static String LOG = "Units";
    private static int SCREEN_SIZE_HEIGHT;
    private static int SCREEN_SIZE_WIDTH;
    private Resources mResources;
    private DisplayMetrics mMetrics;
    private Context mContext;

    public Units(Context context){
        mContext = context;
        mResources = context.getResources();
        mMetrics = mResources.getDisplayMetrics();
        SCREEN_SIZE_WIDTH = mMetrics.widthPixels;
        SCREEN_SIZE_HEIGHT = mMetrics.heightPixels;
    }

    public static int getScreenHeight() {
        return SCREEN_SIZE_HEIGHT;
    }

    public static int getScreenWidth() { return SCREEN_SIZE_WIDTH; }

    public int getUsableSliderHeight(boolean isImmersive) {

        if (isImmersive) {
            return getScreenHeight() -
                    (int) mContext.getResources().getDimension(R.dimen.toolBarHeightWithPadding) -
                    (int) mContext.getResources().getDimension(R.dimen.progressBarHeight) -
                    (int) mContext.getResources().getDimension(R.dimen.questionTextHeight);
        } else {
            return getScreenHeight() -
                    getStatusBarHeight() -
                    (int) mContext.getResources().getDimension(R.dimen.toolBarHeightWithPadding) -
                    (int) mContext.getResources().getDimension(R.dimen.progressBarHeight) -
                    (int) mContext.getResources().getDimension(R.dimen.questionTextHeight) -
                    (int) mContext.getResources().getDimension(R.dimen.preferencesButtonHeight) -
                    (int) mContext.getResources().getDimension(R.dimen.actionBarHeight);
        }
    }

    public int getStatusBarHeight() {
        return (int) (24 * mMetrics.density);
    }

    public int convertDpToPixels(float dp){
        return (int) (dp * ((float) mMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public int convertPixelsToDp(float px){
        return (int) (px / ((float) mMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public int convertPixelsToSp(int px) {
        return (int) (px / mMetrics.scaledDensity);
    }

    public int convertSpToPixels(int sp) {
        return (int) (sp * mMetrics.scaledDensity);
    }

}
