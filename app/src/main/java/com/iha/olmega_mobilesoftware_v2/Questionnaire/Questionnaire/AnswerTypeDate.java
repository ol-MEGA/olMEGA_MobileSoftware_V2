package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by ul1021 on 17.05.2017.
 */

public class AnswerTypeDate extends AppCompatActivity {

    private final int mQuestionId;
    private final Questionnaire mQuestionnaire;
    private final Context mContext;
    private final SimpleDateFormat DATE_FORMAT;
    private String LOG_STRING = "AnswerTypeDate";
    private String mString = "";

    public AnswerTypeDate(Context context, Questionnaire questionnaire, int questionId) {

        mContext = context;
        mQuestionnaire = questionnaire;
        mQuestionId = questionId;
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);
    }

    public boolean addAnswer(String sAnswer) {
        switch (sAnswer) {
            case "$utcnow":
                mString = generateTimeNowUTC();
                break;
            case "$now":
                mString = generateTimeNow();
                break;
        }
        mQuestionnaire.addTextToEvaluationLst(mQuestionId, mString);
        return true;
    }

    private String generateTimeNow() {
        Calendar dateTime = Calendar.getInstance(TimeZone.getTimeZone("GMT+1"));
        return DATE_FORMAT.format(dateTime.getTime());
    }

    private String generateTimeNowUTC() {
        Calendar dateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        return DATE_FORMAT.format(dateTime.getTime());
    }
}
