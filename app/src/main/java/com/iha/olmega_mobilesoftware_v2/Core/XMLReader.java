package com.iha.olmega_mobilesoftware_v2.Core;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Central class for XML sheet info extraction tasks
 */

public class XMLReader {

    private static String LOG = "XMLReader";
    private Context mContext;
    private FileIO mFileIO;
    private String mHead, mFoot, mSurveyURI, KEY_NEW_LINE;
    private int mTimerMean, mTimerDeviation, mTimerInterval;
    // List containing all questions (including attached information)
    private ArrayList<String> mQuestionList;
    private int nDefaultTimerMean = 30, nDefaultTimerDeviation = 5, nSecondsInMinute = 60;
    private ArrayList<String> mDateList;
    private String mTimerLayout = "";

    public XMLReader(Context context, String fileName) {

        mContext = context;
        mFileIO = new FileIO();
        mQuestionList = new ArrayList<>();
        String rawInput = mFileIO.readRawTextFile(fileName);
        KEY_NEW_LINE = "\n";

        String[] timerTemp = rawInput.split("<timer|</timer>");

        // timerTemp.length == 0 means no timer information can be found
        if (timerTemp.length > 1) {

            if (timerTemp[1].split("mean").length > 1) {
                try {
                    mTimerMean = Integer.parseInt(timerTemp[1].split("\"")[1]);
                } catch (Exception e) {
                    mTimerMean = nDefaultTimerMean * nSecondsInMinute;
                    Log.e(LOG, "Invalid entry. Timer mean set to " + mTimerMean + " seconds.");
                }
                mTimerLayout = "single";
            }

            if (timerTemp[1].split("deviation").length > 1) {
                try {
                    mTimerDeviation = Integer.parseInt(timerTemp[1].split("\"")[3]);
                } catch (Exception e) {
                    mTimerDeviation = nDefaultTimerDeviation * nSecondsInMinute;
                    Log.e(LOG, "Invalid entry. Timer mean set to 300 seconds.");
                }
                mTimerLayout = "single";
            }

            if (timerTemp[1].split("date").length > 1) {
                try {
                    mDateList = new ArrayList<>();

                    String[] tmp_entries = timerTemp[1].split("\"")[1].split(";");
                    // Sort list
                    java.util.Arrays.sort(tmp_entries, 1, tmp_entries.length);

                    if (tmp_entries.length > 1) {

                        for (int iDate = 0; iDate < tmp_entries.length; iDate++) {

                            if (Integer.parseInt(tmp_entries[iDate].split(":")[0]) > 23 ||
                                    Integer.parseInt(tmp_entries[iDate].split(":")[1]) > 59) {
                                Log.e(LOG, "Invalid entry: " + tmp_entries[iDate]);
                            } else {
                                Log.e(LOG, "Entry added: " + tmp_entries[iDate]);
                                mDateList.add(tmp_entries[iDate]);
                            }
                        }
                        mTimerMean = -255;
                    } else {
                        mTimerMean = 0;
                        mTimerDeviation = 0;
                    }

                } catch (Exception e) {
                    Log.e(LOG, "Invalid date specified.");
                }
                mTimerLayout = "multi";
            }
        } else {
            mTimerMean = 0;
            mTimerDeviation = 0;
        }

        // Split basis data into question segments
        String[] questionnaire = rawInput.split("<question|</question>|<finish>|</finish>");
        mHead = extractHead(rawInput);
        mFoot = extractFoot(rawInput);
        mSurveyURI = extractSurveyURI(fileName);

        mQuestionList = stringArrayToListString(questionnaire);
        mQuestionList = thinOutList(mQuestionList);
    }

    private String extractHead(String rawInput) {
        String head = "";
        String[] tempHead = rawInput.split("<|>");

        head += "<";
        head += tempHead[1];
        head +=">";
        head += KEY_NEW_LINE;
        head +="<";
        head += tempHead[3];
        head += ">";

        return head;
    }

    /*private String extractSurveyURI(String rawInput) {
        return rawInput.split("<survey uri=\"")[1].split("\">")[0];
    }*/

    private String extractSurveyURI(String inString) {
        return inString;
    }

    private String extractFoot(String rawInput) {
        String[] rawInputLines = rawInput.split("\n");
        return rawInputLines[rawInputLines.length - 1];
    }

    public int getNewTimerInterval() {

        if (mTimerLayout.equalsIgnoreCase("single")) {

            mTimerInterval = ThreadLocalRandom.current().nextInt(
                    mTimerMean - mTimerDeviation,
                    mTimerMean + mTimerDeviation + 1);

        } else if (mTimerLayout.equalsIgnoreCase("multi")) {

            Date tmp_date = new Date();
            int tmp_dateTime = tmp_date.getHours() * 60 * 60 + tmp_date.getMinutes() * 60 +
                    tmp_date.getSeconds();
            int tmp_result;
            int nextTimer = 0;

            for (int iDate = 0; iDate < mDateList.size(); iDate ++) {
                int tmp_hours = Integer.parseInt(mDateList.get(iDate).split(":")[0]);
                int tmp_minutes = Integer.parseInt(mDateList.get(iDate).split(":")[1]);
                int tmp_time = tmp_hours * 60 * 60 + tmp_minutes * 60;

                tmp_result = tmp_time > tmp_dateTime ? +1 : tmp_time < tmp_dateTime ? -1 : 0;

                if (tmp_result == 1) {
                    nextTimer = iDate;
                    break;
                }
            }

            mTimerInterval = Integer.parseInt(mDateList.get(nextTimer).split(":")[0]) * 60 * 60 +
                    Integer.parseInt(mDateList.get(nextTimer).split(":")[1]) * 60 -
                    tmp_dateTime;

            if (mTimerInterval <= 0) {
                mTimerInterval +=  + 24 * 60 * 60 + 60 * 60;
            }

        }

        return mTimerInterval;
    }
    
    public boolean getQuestionnaireHasTimer() {
        return (mTimerMean != 0);
    }

    public String getHead() {
        return mHead;
    }

    public String getFoot() {
        return mFoot;
    }

    public String getSurveyURI() {
        return mSurveyURI;
    }

    public ArrayList<String> getQuestionList() {
        return mQuestionList;
    }

    private ArrayList<String> thinOutList(ArrayList<String> mQuestionList) {
        // Removes irrelevant data from question sheet

        for (int iItem = mQuestionList.size() - 1; iItem >= 0; iItem = iItem - 2) {
            mQuestionList.remove(iItem);
        }
        return mQuestionList;
    }

    private ArrayList<String> stringArrayToListString(String[] stringArray) {
        // Turns an array of Strings into a List of Strings
        ArrayList<String> listString = new ArrayList<>();
        Collections.addAll(listString, stringArray);
        return listString;
    }
}
