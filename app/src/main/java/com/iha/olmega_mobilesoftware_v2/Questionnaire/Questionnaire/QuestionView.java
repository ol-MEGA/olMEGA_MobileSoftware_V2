package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import android.view.View;

import java.util.ArrayList;

/**
 * Created by ulrikkowalk on 09.03.17.
 */

public class QuestionView implements Comparable<QuestionView> {

    private final View mView;
    private final Integer mId;
    private final boolean mIsForced;
    private final ArrayList<Integer> mListOfAnswerIds;
    private final ArrayList<Integer> mListOfFilterIds;

    public QuestionView(View view, int id, boolean isForced,
                        ArrayList<Integer> listOfAnswerIds,
                        ArrayList<Integer> listOfFilterIds) {
        mView = view;
        mId = id;
        mIsForced = isForced;
        mListOfAnswerIds = listOfAnswerIds;
        mListOfFilterIds = listOfFilterIds;
    }

    @Override
    public int compareTo(QuestionView questionView) {
        return this.mId.compareTo(questionView.getId());
    }

    public View getView() {
        return mView;
    }

    public int getId() {
        return mId;
    }

    boolean getIsForced() {
        return mIsForced;
    }

    public ArrayList<Integer> getListOfAnswerIds() {
        return mListOfAnswerIds;
    }

    public ArrayList<Integer> getListOfFilterIds() {
        return mListOfFilterIds;
    }

    ArrayList<Integer> getFilterIdPositive() {
        // Function returns all positive Filter IDs which represent the MUST EXIST cases
        ArrayList<Integer> listOfPositiveIds = new ArrayList<>();
        for (int iElement = 0; iElement < mListOfFilterIds.size(); iElement++) {
            if (mListOfFilterIds.get(iElement) >= 0) {
                listOfPositiveIds.add(mListOfFilterIds.get(iElement));
            }
        }
        return listOfPositiveIds;
    }

    ArrayList<Integer> getFilterIdNegative() {
        // Function returns all negative IDs (only absolute values), which represent the MUST NOT
        // EXIST case.
        ArrayList<Integer> listOfNegativeIds = new ArrayList<>();
        for (int iElement = 0; iElement < mListOfFilterIds.size(); iElement++) {
            if (mListOfFilterIds.get(iElement) < 0) {
                listOfNegativeIds.add((-1) * mListOfFilterIds.get(iElement));
            }
        }
        return listOfNegativeIds;
    }
}
