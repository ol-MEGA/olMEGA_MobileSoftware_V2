package com.iha.olmega_mobilesoftware_v2.Questionnaire.Core;

import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.Questionnaire.DataTypes.QuestionIdTypeAndValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ulrikkowalk on 09.05.17.
 */

public class EvaluationList extends ArrayList<QuestionIdTypeAndValue> {

    private String LOG_STRING = "EvaluationList";
    private List<QuestionIdTypeAndValue> mEvaluationList;
    private boolean isVerbose = false;

    public EvaluationList() {
        mEvaluationList = new ArrayList<>();
    }

    // For answer Ids
    public boolean add(int nQuestionId, int nAnswerId) {
        mEvaluationList.add(new QuestionIdTypeAndValue(
                nQuestionId, "id", Integer.toString(nAnswerId)));
        if (isVerbose) {
            Log.i(LOG_STRING, "Entry added: " + nAnswerId);
        }
        return true;
    }

    // For answer texts
    public boolean add(int nQuestionId, String sText) {
        mEvaluationList.add(new QuestionIdTypeAndValue(
                nQuestionId, "text", sText));
        return true;
    }

    // For floating point values
    public boolean add(int nQuestionId, float nValue) {
        mEvaluationList.add(new QuestionIdTypeAndValue(
                nQuestionId, "value", Float.toString(nValue)));
        return true;
    }

    public boolean add(int nQuestionId, List<Integer> listOfIds) {
        for (int iId = 0; iId < listOfIds.size(); iId++) {
            mEvaluationList.add(new QuestionIdTypeAndValue(nQuestionId,
                    "id", listOfIds.get(iId).toString()));
        }
        return true;
    }

    //Remove all answers with given Ids in input list
    public boolean removeAllAnswerIds(ArrayList<Integer> listOfIds) {
        int nRemoved = 0;
        for (int iId = 0; iId < listOfIds.size(); iId++) {
            int currentId = listOfIds.get(iId);
            for (int iAnswer = mEvaluationList.size() - 1; iAnswer >= 0; iAnswer--) {
                if ((mEvaluationList.get(iAnswer).getAnswerType().equals("id")) &&
                        (mEvaluationList.get(iAnswer).getValue().equals(
                                Integer.toString(currentId)))) {
                    mEvaluationList.remove(iAnswer);
                    nRemoved++;
                }
            }
        }
        if (isVerbose) {
            Log.i(LOG_STRING, "Entries removed: " + nRemoved);
        }
        return true;
    }

    //Remove all answers from question with given QuestionId
    public boolean removeQuestionId(int QuestionId) {
        int nRemoved = 0;
        for (int iAnswer = mEvaluationList.size() - 1; iAnswer >= 0; iAnswer--) {
            if (mEvaluationList.get(iAnswer).getQuestionId() == QuestionId) {
                mEvaluationList.remove(iAnswer);
                nRemoved++;
            }
        }

        if (isVerbose) {
            Log.i(LOG_STRING, "Entries removed: " + nRemoved);
        }

        return true;
    }

    //Remove all answers of given type
    public boolean removeAllOfType(String sType) {
        int nRemoved = 0;
        for (int iAnswer = mEvaluationList.size() - 1; iAnswer >= 0; iAnswer--) {
            if (mEvaluationList.get(iAnswer).getAnswerType().equals(sType)) {
                mEvaluationList.remove(iAnswer);
                nRemoved++;
            }
        }

        if (isVerbose) {
            Log.i(LOG_STRING, "Entries removed of Type " + sType + ":" + nRemoved);
        }
        return true;
    }

    //Remove given AnswerId
    public boolean removeAnswerId(int Id) {
        int nRemoved = 0;
        for (int iAnswer = mEvaluationList.size() - 1; iAnswer >= 0; iAnswer--) {
            if ((mEvaluationList.get(iAnswer).getAnswerType().equals("id")) &&
                    (mEvaluationList.get(iAnswer).getValue().equals(Integer.toString(Id)))) {
                mEvaluationList.remove(iAnswer);
                nRemoved++;
            }
        }

        if (isVerbose) {
            Log.i(LOG_STRING, "Entries removed: " + nRemoved);
        }
        return true;
    }

    //Check whether List contains question Id
    public boolean containsQuestionId(int id) {
        for (int iItem = 0; iItem < mEvaluationList.size(); iItem++) {
            if (mEvaluationList.get(iItem).getQuestionId() == id) {
                return true;
            }
        }
        return false;
    }

    //Check whether List contains answer Id
    public boolean containsAnswerId(int id) {
        for (int iItem = 0; iItem < mEvaluationList.size(); iItem++) {
            if (mEvaluationList.get(iItem).getAnswerType().equals("id") &&
                    Integer.parseInt(mEvaluationList.get(iItem).getValue()) == id) {
                return true;
            }
        }
        return false;
    }

    // Check whether List contains member of answer Id List
    public boolean containsAnswerId(ArrayList<Integer> listOfIds) {

        for (int iId = 0; iId < listOfIds.size(); iId++) {

            for (int iItem = 0; iItem < mEvaluationList.size(); iItem++) {
                if (mEvaluationList.get(iItem).getAnswerType().equals("id") &&
                        Integer.parseInt(mEvaluationList.get(iItem).getValue()) ==
                                listOfIds.get(iId)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Check whether List contains member of answer Id List
    public boolean containsAtLeastOneAnswerId(ArrayList<Integer> listOfIds) {

        for (int iId = 0; iId < listOfIds.size(); iId++) {

            for (int iItem = 0; iItem < mEvaluationList.size(); iItem++) {
                if (mEvaluationList.get(iItem).getAnswerType().equals("id") &&
                        Integer.parseInt(mEvaluationList.get(iItem).getValue()) ==
                                listOfIds.get(iId)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Check whether List contains member of answer Id List
    public boolean containsAllAnswerIds(ArrayList<Integer> listOfIds) {

        int[] resultArray = new int[listOfIds.size()];
        Arrays.fill(resultArray, 0);

        for (int iId = 0; iId < listOfIds.size(); iId++) {

            for (int iItem = 0; iItem < mEvaluationList.size(); iItem++) {
                if (mEvaluationList.get(iItem).getAnswerType().equals("id") &&
                        Integer.parseInt(mEvaluationList.get(iItem).getValue()) ==
                                listOfIds.get(iId)) {
                    resultArray[iId] = 1;
                }
            }
        }

        int resultValue = 1;
        for (int iRes = 0; iRes < listOfIds.size(); iRes++) {
            resultValue *= resultArray[iRes];
        }

        if (resultValue == 0) {
            return false;
        } else {
            return true;
        }
    }

    public String getTextFromQuestionId(int id) {
        for (int iItem = 0; iItem < mEvaluationList.size(); iItem++) {
            if (mEvaluationList.get(iItem).getAnswerType().equals("text") &&
                    mEvaluationList.get(iItem).getQuestionId() == id) {
                return mEvaluationList.get(iItem).getValue();
            }
        }
        return "none";
    }

    public String getAnswerTypeFromQuestionId(int id) {
        for (int iItem = 0; iItem < mEvaluationList.size(); iItem++) {
            if (mEvaluationList.get(iItem).getQuestionId() == id) {
                return mEvaluationList.get(iItem).getAnswerType();
            }
        }
        return "none";
    }

    public ArrayList<String> getCheckedAnswerIdsFromQuestionId(int id) {
        ArrayList<String> listOfAnswerIds = new ArrayList<>();
        for (int iItem = 0; iItem < mEvaluationList.size(); iItem++) {
            if (mEvaluationList.get(iItem).getQuestionId() == id &&
                    mEvaluationList.get(iItem).getAnswerType() == "id") {
                listOfAnswerIds.add(mEvaluationList.get(iItem).getValue());
            }
        }
        return listOfAnswerIds;
    }

    public ArrayList<String> getCheckedAnswerValuesFromQuestionId(int id) {
        ArrayList<String> listOfAnswerValues = new ArrayList<>();
        for (int iItem = 0; iItem < mEvaluationList.size(); iItem++) {
            if (mEvaluationList.get(iItem).getQuestionId() == id &&
                    mEvaluationList.get(iItem).getAnswerType() == "value") {
                listOfAnswerValues.add(mEvaluationList.get(iItem).getValue());
            }
        }
        return listOfAnswerValues;
    }

    public String getValueFromQuestionId(int id) {
        for (int iItem = 0; iItem < mEvaluationList.size(); iItem++) {
            if (mEvaluationList.get(iItem).getAnswerType().equals("value") &&
                    mEvaluationList.get(iItem).getQuestionId() == id) {
                return mEvaluationList.get(iItem).getValue();
            }
        }
        return "none";
    }

    public int size() {
        return mEvaluationList.size();
    }

    public QuestionIdTypeAndValue get(int item) {
        return mEvaluationList.get(item);
    }

}
