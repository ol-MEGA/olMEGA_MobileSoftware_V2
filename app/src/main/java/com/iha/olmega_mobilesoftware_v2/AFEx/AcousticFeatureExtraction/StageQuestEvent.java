package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import android.content.Intent;
import android.util.Log;

import java.util.HashMap;

/**
 * Example stage that sends an event broadcast to trigger questionnaire
 */

public class StageQuestEvent extends Stage {

    final static String LOG = "StageQuestEvent";

    int blocks = 0;

    public StageQuestEvent(HashMap parameter) {
        super(parameter);
    }

    @Override
    protected void process(float[][] buffer) {
        // define stage with a blocksize fs
        if (blocks == 5) {
            Log.d(LOG, "GO!!!!!!!!!!!!!");
            Intent  intent = new Intent("QuestionnaireEvent");
            intent.setPackage(context.getPackageName());
            intent.putExtra("Value", true);
            context.sendBroadcast(intent);
            blocks = 0;
        }
        blocks++;
    }


}
