package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import android.util.Log;

/**
 * Created by ulrikkowalk on 28.02.17.
 */

public class Answer {

    private String LOG_STRING = "Answer";
    String Text;
    int Id, Group;
    private String mUUID;
    private boolean isDefault = false;
    private boolean isDebug = false;
    private boolean isExclusive = false;

    public Answer(String sAnswer, int nAnswerId, int nGroup) {
        Text = sAnswer;
        Text = Text.replaceAll("&lt;", "<");
        Text = Text.replaceAll("&gt;", ">");
        Id = nAnswerId;
        Group = nGroup;

        if (isDebug) {
            Log.i(LOG_STRING, "Answer added - Text: " + Text + ", Id: " + Id + ", Group: " + Group);
        }
    }

    public Answer(String sAnswer, int nAnswerId, int nGroup, boolean bDefault, boolean bExclusive) {
        Text = sAnswer;
        Text = Text.replaceAll("&lt;", "<");
        Text = Text.replaceAll("&gt;", ">");
        Id = nAnswerId;
        Group = nGroup;
        isDefault = bDefault;
        isExclusive = bExclusive;

        if (isDebug) {
            Log.i(LOG_STRING, "Answer added - Text: " + Text + ", Id: " +
                    Id + ", Group: " + nGroup + ", Default: " + isDefault +
                    ", Exclusive: "+isExclusive);
        }
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isExclusive() { return isExclusive; }

    public void setUUID(String uuid) { mUUID = uuid; }
}
