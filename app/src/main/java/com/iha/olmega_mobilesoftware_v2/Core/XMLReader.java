package com.iha.olmega_mobilesoftware_v2.Core;

import android.content.Context;
import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.MainActivity;
import com.iha.olmega_mobilesoftware_v2.Questionnaire.DataTypes.StartAndStop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Calendar;

/**
 * Central class for XML sheet info extraction tasks
 */

public class XMLReader {

    private static String LOG = "XMLReader";
    private Context mContext;
    private FileIO mFileIO;
    private String mHead, mFoot, mSurveyURI, KEY_NEW_LINE;

    private Calendar calendar = Calendar.getInstance();
    private int mTimerMean, mTimerDeviation, mTimerInterval,
            mDonotdisturbStart_hour = -255, mDonotdisturbStart_minute = -255,
            mDonotdisturbStop_hour = -255, mDonotdisturbStop_minute = -255;
    // List containing all questions (including attached information)
    private ArrayList<String> mQuestionList;
    private int nDefaultTimerMean = 30, nDefaultTimerDeviation = 5, nSecondsInMinute = 60;
    private ArrayList<String> mDateList;
    private ArrayList<StartAndStop> mBetweenList;
    private String mTimerLayout = "";

    // do not disturb interval active
    private boolean use_dnd = false;
    private boolean use_timer = false;

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
                mTimerLayout = "random";
                use_timer = true;
            }

            if (timerTemp[1].split("deviation").length > 1) {
                try {
                    mTimerDeviation = Integer.parseInt(timerTemp[1].split("\"")[3]);
                } catch (Exception e) {
                    mTimerDeviation = nDefaultTimerDeviation * nSecondsInMinute;
                    Log.e(LOG, "Invalid entry. Timer mean set to 300 seconds.");
                }
                mTimerLayout = "random";
                use_timer = true;
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
                use_timer = true;
            }

            // timer set once between each set of start and stop
            if (timerTemp[1].split("interval").length > 1) {
                try {
                    mBetweenList = new ArrayList<>();

                    String[] tmp_entries = timerTemp[1].split("\"")[1].split(";");

                    if (tmp_entries.length > 0) {

                        Log.i(LOG, "now here");

                        for (int iEntry = 0; iEntry < tmp_entries.length; iEntry++) {

                            String[] tmp_dates = tmp_entries[iEntry].split("-");


                            if (tmp_dates.length > 1) {

                                Log.i(LOG, "dates" + tmp_dates[0] + ", " + tmp_dates[1]);

                                int start_hour = Integer.parseInt(tmp_dates[0].split(":")[0]);
                                Log.i(LOG, "start hour: " + start_hour);
                                int start_minute = Integer.parseInt(tmp_dates[0].split(":")[1]);
                                Log.i(LOG, "start minute: " + start_minute);
                                int stop_hour = Integer.parseInt(tmp_dates[1].split(":")[0]);
                                Log.i(LOG, "stop hour: " + stop_hour);
                                int stop_minute = Integer.parseInt(tmp_dates[1].split(":")[1]);
                                Log.i(LOG, "stop minute: " + stop_minute);


                                // check if entries make sense as hours and minutes
                                if ((start_hour <= 23 && start_minute <= 59) &&
                                        (stop_hour <= 23 && stop_minute <= 59)) {
                                    Log.i(LOG, "checks out.");
                                    // check if second date is later than first date
                                    if (((start_hour == stop_hour) && (start_minute < stop_minute)) ||
                                        (start_hour < stop_hour)) {
                                        Log.i(LOG, "totally valid");
                                        mBetweenList.add(new StartAndStop(start_hour, start_minute, stop_hour, stop_minute));
                                    }
                                } else {
                                    Log.e(LOG, "Invalid date specified.");
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    Log.e(LOG, "Invalid date specified.");
                }
                mTimerLayout = "interval";
                use_timer = true;
            }
        }

        // Blackout region where no alerts are set, e.g. at night
        String[] donotdisturb = rawInput.split("<donotdisturb|</donotdisturb>");

        if (donotdisturb.length > 1) {

            String[] tmp_entries = donotdisturb[1].split("\"")[1].split("-");
            if (tmp_entries.length > 1) {

                String[] tmp_start = tmp_entries[0].split(":");
                if (tmp_start.length > 1) {
                    mDonotdisturbStart_hour = Integer.parseInt(tmp_start[0]);
                    mDonotdisturbStart_minute = Integer.parseInt(tmp_start[1]);
                }

                String[] tmp_stop = tmp_entries[1].split(":");
                if (tmp_stop.length > 1) {
                    mDonotdisturbStop_hour = Integer.parseInt(tmp_stop[0]);
                    mDonotdisturbStop_minute = Integer.parseInt(tmp_stop[1]);
                }

                Log.i(LOG, "Do not disturb between: " + mDonotdisturbStart_hour + ":" +
                        mDonotdisturbStart_minute + " - " + mDonotdisturbStop_hour + ":" +
                        mDonotdisturbStop_minute);
                use_dnd = true;
            }
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

    private String extractSurveyURI(String inString) {
        return inString;
    }

    private String extractFoot(String rawInput) {
        String[] rawInputLines = rawInput.split("\n");
        return rawInputLines[rawInputLines.length - 1];
    }

    public int getNewTimerInterval() {

        if (mTimerLayout.equalsIgnoreCase("random")) {
            // alarm is set randomly based on uniform distribution created from mean and deviation

            /* in order to place an alarm outside "do not disturb" dnd time frame, we must first
            find the limits and their temporal distance relative to now */

            if (use_dnd) {
                // time until dnd starts
                int donotdisturb_start_hours = mDonotdisturbStart_hour - calendar.get(Calendar.HOUR_OF_DAY);
                if (donotdisturb_start_hours < 0) {
                    donotdisturb_start_hours += 24;
                }
                int donot_disturb_start_minutes = mDonotdisturbStart_minute - calendar.get(Calendar.MINUTE);
                // beginning of dnd in seconds
                int donotdisturb_start = (donotdisturb_start_hours * 60 + donot_disturb_start_minutes) * 60;

                // time until dnd ends
                int donotdisturb_stop_hours = mDonotdisturbStop_hour - calendar.get(Calendar.HOUR_OF_DAY);
                if (donotdisturb_stop_hours < 0) {
                    donotdisturb_stop_hours += 24;
                }
                int donotdisturb_stop_minutes = mDonotdisturbStop_minute - calendar.get(Calendar.MINUTE);
                // end of dnd in seconds
                int donotdisturb_stop = (donotdisturb_stop_hours * 60 + donotdisturb_stop_minutes) * 60;

                /* Four cases exist:
                - case 0: do not disturb interval is currently active
                - case 1: do not disturb interval is close but there is room for 1 more alarm
                - case 2: do not disturb interval is too close for another alarm
                - case 3: do not disturb interval has no effect (or doesn't exist at all)
                 */

                if (donotdisturb_stop < donotdisturb_start) { // case 0
                    /* If the end of dnd is closer than the beginning, it means that we're within said
                    interval. In this case create new random timer after dnd has ended, based on regular
                    user-specified mean and deviation (uniform distribution), like the device was just
                    switched on right after the end of dnd */
                    mTimerInterval = ThreadLocalRandom.current().nextInt(
                            donotdisturb_stop + mTimerMean - mTimerDeviation + 1,
                            donotdisturb_stop + mTimerMean + mTimerDeviation);
                } else if (((mTimerMean + mTimerDeviation) > donotdisturb_start) && (mTimerMean < donotdisturb_start)) { // case 1
                    // If there is time for one more alarm before dnd, set the upper limit close to dnd
                    mTimerInterval = ThreadLocalRandom.current().nextInt(
                            mTimerMean - mTimerDeviation,
                            donotdisturb_start - 1);
                } else if (mTimerMean > donotdisturb_start) { // case 2
                    // if dnd is too close for new timer, set timer after dnd
                    mTimerInterval = ThreadLocalRandom.current().nextInt(
                            donotdisturb_stop + mTimerMean - mTimerDeviation + 1,
                            donotdisturb_stop + mTimerMean + mTimerDeviation);
                } else { // case 3
                    // create random timer based on user-specified values for mean and deviation
                    mTimerInterval = ThreadLocalRandom.current().nextInt(
                            mTimerMean - mTimerDeviation,
                            mTimerMean + mTimerDeviation + 1);
                }
            } else {
                // no dnd interval -> create random timer based on user-specified values for mean
                // and deviation
                mTimerInterval = ThreadLocalRandom.current().nextInt(
                        mTimerMean - mTimerDeviation,
                        mTimerMean + mTimerDeviation + 1);
            }

        } else if (mTimerLayout.equalsIgnoreCase("multi")) {
            // user specified times of alarm

            int tmp_dateTime = calendar.get(Calendar.HOUR_OF_DAY) * 60 * 60 + calendar.get(Calendar.MINUTE) * 60 +
                    calendar.get(Calendar.SECOND);
            int tmp_result;
            int nextTimer = 0;

            // check which user-specified time comes next
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
            // set the timer for next user-specified time in seconds
            mTimerInterval = Integer.parseInt(mDateList.get(nextTimer).split(":")[0]) * 60 * 60 +
                    Integer.parseInt(mDateList.get(nextTimer).split(":")[1]) * 60 -
                    tmp_dateTime;

            // in case of the next timer being after 24:00, add one day
            if (mTimerInterval <= 0) {
                mTimerInterval +=  + 24 * 60 * 60 + 60 * 60;
            }

        } else if (mTimerLayout.equalsIgnoreCase("interval"))  {
            // one alarm is set randomly for each user specified interval using uniform distribution

            // current time in hours and minutes
            int now_hour = calendar.get(Calendar.HOUR_OF_DAY);
            int now_minute = calendar.get(Calendar.MINUTE);

            // find the next time frame, keeping in mind it might be on the next day (after 24:00)
            int tmp_time_diff_to_now;
            int tmp = 24*60;
            int next_start = 0;

            for (int iDate = 0; iDate < mBetweenList.size(); iDate++) {
                tmp_time_diff_to_now = (mBetweenList.get(iDate).getStart_Hour() - now_hour) * 60 + (mBetweenList.get(iDate).getStart_minute() - now_minute);
                // if next interval is after 24:00
                if (tmp_time_diff_to_now < 0) {
                    tmp_time_diff_to_now += 24*60;
                }
                // find minimum time distance to next time frame
                if (tmp_time_diff_to_now < tmp) {
                    tmp = tmp_time_diff_to_now;
                    next_start = iDate;
                }
            }

            // hours and minutes of next time frame
            int next_start_hour = mBetweenList.get(next_start).getStart_Hour();
            int next_start_minute = mBetweenList.get(next_start).getStart_minute();
            int next_stop_hour = mBetweenList.get(next_start).getStop_Hour();
            int next_stop_minute = mBetweenList.get(next_start).getStop_minute();

            // in case the next time frame is after 24:00
            if (next_start_hour < now_hour) {
                next_start_hour += 24;
                next_stop_hour += 24;
            }
            // calculate minutes until next time frame beginning and end
            int diff_minutes_to_start = (next_start_hour - now_hour) * 60 + (next_start_minute - now_minute);
            int diff_minutes_to_stop = (next_stop_hour - now_hour) * 60 + (next_stop_minute - now_minute);

            // interval to set the timer at
            mTimerInterval = ThreadLocalRandom.current().nextInt(
                   diff_minutes_to_start,
                   diff_minutes_to_stop + 1) * 60;
        }

        Log.i(LOG, "Timer set to: " + mTimerInterval);
        return mTimerInterval;
    }
    
    public boolean getQuestionnaireHasTimer() {
        return use_timer;
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
