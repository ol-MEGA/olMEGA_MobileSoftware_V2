package com.iha.olmega_mobilesoftware_v2.Questionnaire.Core;

import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.iha.olmega_mobilesoftware_v2.Core.LogIHAB;
import com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire.Question;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by ulrikkowalk on 14.03.17.
 */

public class MetaData extends AppCompatActivity {

    private static String LOG = "MetaData";

    private String DEVICE_Id, START_DATE, START_DATE_UTC, END_DATE,
            END_DATE_UTC, KEY_HEAD, KEY_FOOT, KEY_TAG_CLOSE, KEY_VALUE_OPEN, KEY_VALUE_CLOSE,
            KEY_SURVEY_URI, KEY_RECORD_OPEN, KEY_RECORD_CLOSE, KEY_DATA, KEY_VERSION,
            KEY_QUESTID, FILE_NAME, KEY_MOTIVATION, KEY_NEW_LINE, KEY_SHORT_CLOSE;

    private SimpleDateFormat DATE_FORMAT, DATE_FORMAT_FILENAME;

    private int mTimeQuery = 0;
    private int mTimeQueryUTC = 0;

    private ArrayList<Question> mQuestionList;

    private Context mContext;

    private EvaluationList mEvaluationList;

    public MetaData(Context context, String head, String foot, String surveyUri, String motivation,
                    String version) {
        mContext = context;
        KEY_HEAD = head;
        KEY_FOOT = foot;
        KEY_SURVEY_URI = surveyUri;
        KEY_MOTIVATION = motivation;
        KEY_VERSION = version;
        mQuestionList = new ArrayList<>();
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);
        DATE_FORMAT_FILENAME = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.ROOT);

        KEY_RECORD_OPEN = "<record";
        KEY_RECORD_CLOSE = "</record>";
        KEY_TAG_CLOSE = ">";
        KEY_VALUE_OPEN = "<value ";
        KEY_VALUE_CLOSE = "</value>";
        KEY_NEW_LINE = "\n";
        KEY_SHORT_CLOSE = "/>";
    }

    public boolean initialise() {
        // Obtain Device Id
        DEVICE_Id = generateDeviceId();
        // Obtain current Time Stamp at the Beginning of Questionnaire
        START_DATE = generateTimeNow();
        // Obtain current UTC Time Stamp at the Beginning of Questionnaire
        START_DATE_UTC = generateTimeNowUTC();

        KEY_QUESTID = generateQuestId();
        FILE_NAME = generateFileName();
        return true;
    }

    public boolean finalise(EvaluationList evaluationList) {
        LogIHAB.log("Questionnaire: Results written");
        mEvaluationList = evaluationList;
        // Obtain current Time Stamp at the End of Questionnaire
        END_DATE = generateTimeNow();
        // Obtain current UTC Time Stamp at the End of Questionnaire
        END_DATE_UTC = generateTimeNowUTC();
        collectData();
        return true;
    }

    // List of questions according to questionnaire - needed to account for unanswered questions
    public void addQuestion(Question question) {
        mQuestionList.add(question);
    }

    private void collectData() {

        int questionId = -255;

        /* Information about Questionnaire */
        KEY_DATA = KEY_HEAD;
        KEY_DATA += KEY_NEW_LINE;
        KEY_DATA += KEY_MOTIVATION;
        KEY_DATA += KEY_NEW_LINE;
        KEY_DATA += KEY_RECORD_OPEN;
        KEY_DATA += " uri=\"";
        KEY_DATA += KEY_SURVEY_URI.substring(0, KEY_SURVEY_URI.length() - 4);        // loose ".xml"
        KEY_DATA += "/";
        KEY_DATA += FILE_NAME;
        KEY_DATA += "\"";
        KEY_DATA += KEY_NEW_LINE;
        KEY_DATA += " survey_uri=\"";
        KEY_DATA += KEY_SURVEY_URI;
        KEY_DATA += "\"";
        KEY_DATA += KEY_TAG_CLOSE;
        KEY_DATA += KEY_NEW_LINE;

        /* Device ID */
        KEY_DATA += KEY_VALUE_OPEN;
        KEY_DATA += "device_id=\"";
        KEY_DATA += DEVICE_Id;
        KEY_DATA += "\"";
        KEY_DATA += KEY_SHORT_CLOSE;
        KEY_DATA += KEY_NEW_LINE;

        /* Start Date */
        KEY_DATA += KEY_VALUE_OPEN;
        KEY_DATA += "start_date=\"";
        KEY_DATA += START_DATE;
        KEY_DATA += "\"";
        KEY_DATA += KEY_SHORT_CLOSE;
        KEY_DATA += KEY_NEW_LINE;

        /* Start Date UTC */
        KEY_DATA += KEY_VALUE_OPEN;
        KEY_DATA += "start_date_UTC=\"";
        KEY_DATA += START_DATE_UTC;
        KEY_DATA += "\"";
        KEY_DATA += KEY_SHORT_CLOSE;
        KEY_DATA += KEY_NEW_LINE;

        /* App version */
        KEY_DATA += KEY_VALUE_OPEN;
        KEY_DATA += "app_version=\"";
        KEY_DATA += KEY_VERSION;
        KEY_DATA += "\"";
        KEY_DATA += KEY_SHORT_CLOSE;
        KEY_DATA += KEY_NEW_LINE;

        /* Questionnaire Results */
        for (int iQuestion = 0; iQuestion < mQuestionList.size(); iQuestion++) {

            questionId = mQuestionList.get(iQuestion).getQuestionId();
            if (questionId != 99999) {

                KEY_DATA += KEY_VALUE_OPEN;
                KEY_DATA += "question_id=\"";
                KEY_DATA += questionId;
                KEY_DATA += "\"";

                String ANSWER_DATA = "";

                switch (mEvaluationList.getAnswerTypeFromQuestionId(questionId)) {
                    case "none":
                        ANSWER_DATA += "/>";
                        break;
                    case "text":
                        ANSWER_DATA += " option_ids=\"";
                        ANSWER_DATA += mEvaluationList.getTextFromQuestionId(questionId);
                        ANSWER_DATA += "\"/>";
                        break;
                    case "id":
                        ArrayList<String> listOfIds =
                                mEvaluationList.getCheckedAnswerIdsFromQuestionId(questionId);
                        Log.e(LOG,"id: "+questionId+" num: "+listOfIds.size());
                        ANSWER_DATA += " option_ids=\"";
                        ANSWER_DATA += listOfIds.get(0);
                        if (listOfIds.size() > 1) {
                            for (int iId = 1; iId < listOfIds.size(); iId++) {
                                ANSWER_DATA += ";";
                                ANSWER_DATA += listOfIds.get(iId);
                            }
                        }
                        ANSWER_DATA += "\"/>";
                        break;
                    case "value":
                        ANSWER_DATA += " option_ids=\"";
                        ANSWER_DATA += mEvaluationList.getValueFromQuestionId(questionId);
                        ANSWER_DATA += "\"/>";
                        break;
                    default:
                        Log.e(LOG, "Unknown element found during evaluation: " +
                                mEvaluationList.getAnswerTypeFromQuestionId(questionId));
                        break;
                }

                KEY_DATA += ANSWER_DATA;
                KEY_DATA += KEY_NEW_LINE;
            }
        }

        /* End Date */
        KEY_DATA += KEY_VALUE_OPEN;
        KEY_DATA += "end_date=\"";
        KEY_DATA += END_DATE;
        KEY_DATA += "\"";
        KEY_DATA += KEY_SHORT_CLOSE;
        KEY_DATA += KEY_NEW_LINE;

        /* End Date UTC */
        KEY_DATA += KEY_VALUE_OPEN;
        KEY_DATA += "end_date_UTC=\"";
        KEY_DATA += END_DATE_UTC;
        KEY_DATA += "\"";
        KEY_DATA += KEY_SHORT_CLOSE;
        KEY_DATA += KEY_NEW_LINE;

        KEY_DATA += KEY_RECORD_CLOSE;
        KEY_DATA += KEY_NEW_LINE;
        KEY_DATA += KEY_FOOT;
    }

    private String generateFileName() {
        return getOlmegaDeviceId() + "_" + generateTimeNowFilename() + ".xml";
    }

    private String generateQuestId() {
        return getOlmegaDeviceId() + "_" + getStartDateUTC();
    }

    private String generateDeviceId() {
        return Secure.getString(mContext.getContentResolver(), Secure.ANDROID_ID);
    }

    private String generateTimeNowFilename() {
        Calendar dateTime = Calendar.getInstance(TimeZone.getTimeZone("GMT+1"));
        return DATE_FORMAT_FILENAME.format(dateTime.getTime());
    }

    private String generateTimeNow() {
        Calendar dateTime = Calendar.getInstance(TimeZone.getTimeZone("GMT+1"));
        return DATE_FORMAT.format(dateTime.getTime());
    }

    private String generateTimeNowUTC() {
        Calendar dateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        return DATE_FORMAT.format(dateTime.getTime());
    }

    private String getQuestId() {
        return KEY_QUESTID;
    }

    private String getOlmegaDeviceId() {
        return DEVICE_Id;
    }

    private String getStartDate() {
        return START_DATE;
    }

    private String getStartDateUTC() {
        return START_DATE_UTC;
    }

    private String getEndDate() {
        return END_DATE;
    }

    private String getEndDateUTC() {
        return END_DATE_UTC;
    }

    private String getTimeNow() {
        if (mTimeQuery == 0) {
            mTimeQuery++;
            return getStartDate();
        } else {
            return getEndDate();
        }
    }

    private String getTimeNowUTC() {
        if (mTimeQueryUTC == 0) {
            mTimeQueryUTC++;
            return getStartDateUTC();
        } else {
            return getEndDateUTC();
        }
    }

    private String getTextFromId(int id) {
        try {
            return mEvaluationList.getTextFromQuestionId(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getData() {
        return KEY_DATA;
    }

    public String getFileName() {
        return FILE_NAME;
    }
}