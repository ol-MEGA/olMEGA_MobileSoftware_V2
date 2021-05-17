package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ulrikkowalk on 06.03.17.
 */

class QuestionInfo {

    private final String LOG_STRING = "QuestionInfo";
    private final int mId;
    private final ArrayList<Integer> mFilterId;
    private final boolean mHidden;
    private final Question mQuestion;
    private final List<Integer> mListOfAnswerIds;
    private boolean mActive;
    private int mPositionInPager;
    private boolean mIsForced;

    QuestionInfo(Question question, int id, ArrayList<Integer> filterId,
                 boolean hidden,
                 List<Integer> answerIds, boolean isForced) {
        mQuestion = question;
        mId = id;
        mFilterId = filterId;
        mActive = true;
        mHidden = hidden;
        mListOfAnswerIds = answerIds;
        mIsForced = isForced;
    }

    QuestionInfo(Question question) {
        mQuestion = question;
        mId = question.getQuestionId();
        mFilterId = question.getFilterIds();
        mActive = true;
        mHidden = question.isHidden();
        mListOfAnswerIds = question.getAnswerIds();
        mIsForced = question.getIsForced();
    }

    boolean isActive() {
        return mActive;
    }

    boolean getIsForced() {
        return mIsForced;
    }

    public int getId() {
        return mId;
    }

    ArrayList<Integer> getFilterIdPositive() {
        // Function returns all positive Filter IDs which represent the MUST EXIST cases
        ArrayList<Integer> listOfPositiveIds = new ArrayList<>();
        for (int iElement = 0; iElement < mFilterId.size(); iElement++) {
            if (mFilterId.get(iElement) >= 0) {
                listOfPositiveIds.add(mFilterId.get(iElement));
            }
        }
        return listOfPositiveIds;
    }

    ArrayList<Integer> getFilterIdNegative() {
        // Function returns all negative IDs (only absolute values), which represent the MUST NOT
        // EXIST case.
        ArrayList<Integer> listOfNegativeIds = new ArrayList<>();
        for (int iElement = 0; iElement < mFilterId.size(); iElement++) {
            if (mFilterId.get(iElement) < 0) {
                listOfNegativeIds.add((-1) * mFilterId.get(iElement));
            }
        }
        return listOfNegativeIds;
    }

    void setInactive() {
        mActive = false;
    }

    void setActive() {
        mActive = true;
    }

    public Question getQuestion() {
        return mQuestion;
    }

    boolean isHidden() {
        return mHidden;
    }

    List<Integer> getAnswerIds() {
        return mListOfAnswerIds;
    }

}
