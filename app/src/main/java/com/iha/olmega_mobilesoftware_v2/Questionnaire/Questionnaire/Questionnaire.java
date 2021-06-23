package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;

import com.iha.olmega_mobilesoftware_v2.Core.FileIO;
import com.iha.olmega_mobilesoftware_v2.Questionnaire.QuestionnaireActivity;
import com.iha.olmega_mobilesoftware_v2.Questionnaire.Core.EvaluationList;
import com.iha.olmega_mobilesoftware_v2.Questionnaire.Core.MetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ulrikkowalk on 28.02.17.
 */

public class Questionnaire {

    private static final String LOG = "Questionnaire";
    // Accumulator for ids, values and texts gathered from user input
    private final EvaluationList mEvaluationList;
    // Context of QuestionnairePageAdapter for visibility
    private final QuestionnairePagerAdapter mContextQPA;
    // Context of MainActivity()
    private final QuestionnaireActivity mQuestionnaireActivity;
    // Basic information about all available questions
    private final ArrayList<QuestionInfo> mQuestionInfo;
    private final FileIO mFileIO;
    // Flag: display forced empty vertical spaces
    private final boolean acceptBlankSpaces = false;
    // Number of pages in questionnaire (visible and hidden)
    private int mNumPages;
    // ArrayList containing all questions (including all attached information)
    private ArrayList<String> mQuestionList;
    private MetaData mMetaData;
    private String mHead, mFoot, mSurveyURI, mMotivation, mVersion;
    private boolean isImmersive = false;
    private String clientID;

    public Questionnaire(QuestionnaireActivity questionnaireActivity, String head, String foot, String surveyUri,
                         String motivation, String version, QuestionnairePagerAdapter contextQPA,
                         String clientID) {

        this.mQuestionnaireActivity = questionnaireActivity;
        this.mContextQPA = contextQPA;
        this.mHead = head;
        this.mFoot = foot;
        this.mSurveyURI = surveyUri;
        this.mMotivation = motivation;
        this.mEvaluationList = new EvaluationList();
        this.mQuestionList = new ArrayList<>();
        this.mFileIO = new FileIO();
        this.mQuestionInfo = new ArrayList<>();
        this.mVersion = version;
        this.clientID = clientID;
    }

    public void setUp(ArrayList<String> questionList) {

        this.mMetaData = new MetaData(this.mQuestionnaireActivity, this.mHead, this.mFoot,
                this.mSurveyURI, this.mMotivation, this.mVersion);
        this.mMetaData.initialise();
        this.mQuestionList = questionList;
        this.mNumPages = this.mQuestionList.size();

        putAllQuestionsInQuestionInfo();
    }

    // Generate a Layout for Question at desired Position based on String Blueprint
    Question createQuestion(int position) {

        String sQuestionBlueprint = this.mQuestionList.get(position);
        Question question = new Question(sQuestionBlueprint, this.mQuestionnaireActivity);

        return question;
    }

    // Builds the Layout of each Stage Question
    LinearLayout generateView(Question question, boolean immersive) {

        this.isImmersive = immersive;

        // Are the answers to this specific Question grouped as Radio Button Group?
        boolean isRadio = false;
        boolean isCheckBox = false;
        boolean isSliderFix = false;
        boolean isSliderFree = false;
        boolean isEmoji = false;
        boolean isText = false;
        boolean isWebsite = false;
        boolean isFinish = false;
        boolean isPhotograph = false;
        boolean isInfo = false;
        boolean isInfoScreen = false;
        boolean isTime = false;

        LinearLayout answerContainer = new LinearLayout(mQuestionnaireActivity);
        answerContainer.setId(question.getQuestionId());
        LinearLayout.LayoutParams linearContainerParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
        answerContainer.setOrientation(LinearLayout.VERTICAL);
        answerContainer.setLayoutParams(linearContainerParams);
        answerContainer.setBackgroundColor(Color.WHITE);

        // TextView carrying the Question
        QuestionText questionText = new QuestionText(mQuestionnaireActivity, question.getQuestionId(),
                question.getQuestionText(), answerContainer);
        questionText.addQuestion();

        // Creates a Canvas for the Answer Layout
        final AnswerLayout answerLayout = new AnswerLayout(mQuestionnaireActivity);
        answerContainer.addView(answerLayout.scrollContent);

        // Format of Answer e.g. "radio", "checkbox", ...
        String sType = question.getTypeAnswer();

        final AnswerTypeRadio answerTypeRadio = new AnswerTypeRadio(
                mQuestionnaireActivity, this, answerLayout, question.getQuestionId());

        // In case of checkbox type
        final AnswerTypeCheckBox answerTypeCheckBox = new AnswerTypeCheckBox(
                mQuestionnaireActivity, this, answerLayout, question.getQuestionId());

        // In case of emoji type
        final AnswerTypeEmoji answerTypeEmoji = new AnswerTypeEmoji(
                mQuestionnaireActivity, this, answerLayout, question.getQuestionId(), isImmersive);

        // In case of sliderFix type
        final AnswerTypeSliderFix answerSliderFix = new AnswerTypeSliderFix(
                mQuestionnaireActivity, this, answerLayout, question.getQuestionId(), questionText, isImmersive);

        // In case of sliderFree type
        final AnswerTypeSliderFree answerSliderFree = new AnswerTypeSliderFree(
                mQuestionnaireActivity, this, answerLayout, question.getQuestionId(), questionText, isImmersive);

        final AnswerTypeText answerTypeText = new AnswerTypeText(
                mQuestionnaireActivity, this, answerLayout, question.getQuestionId(), isImmersive);

        final AnswerTypeWebsite answerTypeWebsite = new AnswerTypeWebsite(
                mQuestionnaireActivity, this, answerLayout, question.getQuestionId(), isImmersive,
                this.clientID);

        final AnswerTypeFinish answerTypeFinish = new AnswerTypeFinish(
                mQuestionnaireActivity, this, answerLayout);

        final AnswerTypePhotograph answerTypePhotograph = new AnswerTypePhotograph(
                mQuestionnaireActivity, answerLayout);

        final AnswerTypeInfo answerTypeInfo = new AnswerTypeInfo(
                mQuestionnaireActivity, this, answerLayout);

        final AnswerTypeInfoScreen answerTypeInfoScreen = new AnswerTypeInfoScreen(
                mQuestionnaireActivity, this, answerLayout);

        final AnswerTypeTime answerTypeTime = new AnswerTypeTime(
                mQuestionnaireActivity, this, question.getQuestionId());

        // Number of possible Answers
        int nNumAnswers = question.getNumAnswers();
        // List carrying all Answers and Answer Ids
        List<Answer> answerList = question.getAnswers();

        // Iteration over all possible Answers attributed to current question
        for (int iAnswer = 0; iAnswer < nNumAnswers; iAnswer++) {

            // Obtain Answer specific Parameters
            Answer currentAnswer = answerList.get(iAnswer);
            String sAnswer = currentAnswer.Text;
            int nAnswerId = currentAnswer.Id;
            int nAnswerGroup = currentAnswer.Group;
            boolean isDefault = currentAnswer.isDefault();
            boolean isExclusive = currentAnswer.isExclusive();

            if (((nAnswerId == 66666) && (acceptBlankSpaces)) || (nAnswerId != 66666)) {

                switch (sType) {

                    case "radio": {
                        isRadio = true;
                        answerTypeRadio.addAnswer(nAnswerId, sAnswer, isDefault);
                        break;
                    }
                    case "checkbox": {
                        isCheckBox = true;
                        answerTypeCheckBox.addAnswer(nAnswerId, sAnswer, nAnswerGroup,
                                isDefault, isExclusive);
                        break;
                    }
                    case "text": {
                        isText = true;
                        if (nNumAnswers > 0) {
                            answerTypeText.addQuestion(sAnswer);
                        }
                        break;
                    }
                    case "finish": {
                        isFinish = true;
                        answerTypeFinish.addAnswer();
                        break;
                    }
                    case "sliderFix": {
                        isSliderFix = true;
                        answerSliderFix.addAnswer(nAnswerId, sAnswer, isDefault);
                        break;
                    }
                    case "sliderFree": {
                        isSliderFree = true;
                        answerSliderFree.addAnswer(nAnswerId, sAnswer, isDefault);
                        break;
                    }
                    case "emoji": {
                        isEmoji = true;
                        answerTypeEmoji.addAnswer(nAnswerId, sAnswer, isDefault);
                        break;
                    }
                    case "website": {
                        isWebsite = true;
                        answerTypeWebsite.addAnswer(sAnswer);
                        break;
                    }
                    case "photograph": {
                        isPhotograph = true;
                        answerTypePhotograph.addAnswer(sAnswer, nAnswerId);
                        break;
                    }
                    case "info": {
                        isInfo = true;
                        answerTypeInfo.addAnswer();
                        break;
                    }
                    case "infoscreen": {
                        isInfoScreen = true;
                        answerTypeInfoScreen.addAnswer(sAnswer);
                        break;
                    }
                    case "time": {
                        isTime = true;
                        answerTypeTime.addAnswer(nAnswerId, sAnswer);
                        break;
                    }
                    default: {
                        isRadio = false;
                        break;
                    }
                }
            }
        }

        if (isText) {
            answerTypeText.buildView();
            answerTypeText.addClickListener();
        }

        if (isCheckBox) {
            answerTypeCheckBox.buildView();
            answerTypeCheckBox.addClickListener();
        }

        if (isEmoji) {
            answerTypeEmoji.buildView();
            answerTypeEmoji.addClickListener();
        }

        if (isSliderFix) {
            answerSliderFix.buildView();
            answerSliderFix.addClickListener();
        }

        if (isSliderFree) {
            answerSliderFree.buildView();
            answerSliderFree.addClickListener();
        }

        if (isRadio) {
            answerTypeRadio.buildView();
            answerTypeRadio.addClickListener();
        }

        if (isWebsite) {
            answerTypeWebsite.buildView();
            answerTypeWebsite.addClickListener();
        }

        if (isFinish) {
            answerTypeFinish.addClickListener();
        }

        if (isPhotograph) {
            answerTypePhotograph.buildView();
            answerTypePhotograph.addClickListener();
        }

        if (isInfo) {
            answerTypeInfo.addClickListener();
        }

        if (isInfoScreen) {
            //answerTypeInfo.addClickListener();
        }

        if (isTime) {
            //answerTypeTime.buildView();
        }

        return answerContainer;
    }

    boolean addValueToEvaluationList(int questionId, float value) {
        mEvaluationList.add(questionId, value);
        return true;
    }

    boolean addTextToEvaluationLst(int questionId, String text) {
        mEvaluationList.add(questionId, text);
        return true;
    }

    boolean addIdToEvaluationList(int questionId, int id) {
        mEvaluationList.add(questionId, id);
        return true;
    }

    boolean removeIdFromEvaluationList(int id) {
        mEvaluationList.removeAnswerId(id);
        return true;
    }

    boolean removeQuestionIdFromEvaluationList(int questionId) {
        mEvaluationList.removeQuestionId(questionId);
        return true;
    }

    boolean finaliseEvaluation() {

        mMetaData.finalise(mEvaluationList);
        mFileIO.saveDataToFile(mQuestionnaireActivity, mMetaData.getFileName(), mMetaData.getData());
        returnToMenu();
        return true;
    }

    int getNumPages() {
        return mNumPages;
    }

    public int getId(Question question) {
        return question.getQuestionId();
    }

    // Function checks all available pages on whether their filtering condition has been met and
    // toggles visibility by destroying or creating the views and adding them to the list of
    // views which is handled by QuestionnairePagerAdapter
    boolean checkVisibility() {

        String sid = "";
        for (int iQ = 0; iQ < mEvaluationList.size(); iQ++) {
            sid += mEvaluationList.get(iQ).getValue();
            sid += ", ";
        }

        boolean wasChanged = true;

        // Repeat until nothing changes anymore
        while (wasChanged) {
            wasChanged = false;

            //Log.e(LOG, "Size of MQuestionInfo: " + mQuestionInfo.size());


            for (int iPos = 0; iPos < mQuestionInfo.size(); iPos++) {

                QuestionInfo qI = mQuestionInfo.get(iPos);

                if (qI.isActive()) {                                                                    // View is active but might be obsolete

                    if (qI.isHidden()) {                                                                // View is declared "Hidden"
                        removeQuestion(iPos);
                        wasChanged = true;
                    } else if (!mEvaluationList.containsAtLeastOneAnswerId(qI.getFilterIdPositive())    // Not even 1 positive Filter Id exists OR No positive filter Ids declared
                            && qI.getFilterIdPositive().size() > 0) {
                        removeQuestion(iPos);
                        wasChanged = true;
                    } else if (mEvaluationList.containsAtLeastOneAnswerId(qI.getFilterIdNegative())) {  // At least 1 negative filter Id exists
                        removeQuestion(iPos);
                        wasChanged = true;
                    }

                } else {                                                                                // View is inactive but should possibly be active

                    if (!qI.isHidden()
                            && (mEvaluationList.containsAtLeastOneAnswerId(qI.getFilterIdPositive())    // View is not declared "Hidden"
                            || qI.getFilterIdPositive().size() == 0)                                    // && (At least 1 positive Filter Id exists OR No positive filter Ids declared)
                            && (!mEvaluationList.containsAtLeastOneAnswerId(qI.getFilterIdNegative())   // && (Not even 1 negative Filter Id exists OR No negative filter Ids declared)
                            || qI.getFilterIdNegative().size() == 0)
                    ) {
                        addQuestion(iPos);
                        wasChanged = true;
                    }
                }
                if (qI.getQuestion().getTypeAnswer().equals("time")) {
                    removeViewOnly(iPos);
                }
            }
        }

        // Sort the List of Views by Id
        Collections.sort(mContextQPA.mListOfViews);
        // Force a reload of the List of Views
        mContextQPA.notifyDataSetChanged();

        return true;
    }

    private void putAllQuestionsInQuestionInfo() {

        for (int iQuestion = 0; iQuestion < mQuestionList.size(); iQuestion++) {
            Question question = createQuestion(iQuestion);
            mQuestionInfo.add(new QuestionInfo(question));
            mQuestionInfo.get(iQuestion).setActive();

            // Question is added to MetaData so the class always holds a complete set of all questions
            // (empty questions are still included in output xml file)
            mMetaData.addQuestion(question);
        }
    }

    // Adds the question to the displayed list
    private boolean addQuestion(int iPos) {

        mQuestionInfo.get(iPos).setActive();

        Question question = createQuestion(iPos);
        View view = generateView(question, isImmersive);

        // View is fetched from Storage List and added to Active List
        mContextQPA.addView(view,
                question.getIsForced(),                                  // this is were the injection happens
                question.getAnswerIds(),
                question.getFilterIds());

        mContextQPA.notifyDataSetChanged();
        mContextQPA.setQuestionnaireProgressBar();

        return true;
    }

    // Removes the question from the displayed list and all given answer ids from memory
    public boolean removeQuestion(int iPos) {

        mQuestionInfo.get(iPos).setInactive();
        mEvaluationList.removeQuestionId(mQuestionInfo.get(iPos).getId());

        // Remove View from Active List
        mContextQPA.removeView(mQuestionInfo.get(iPos).getId());
        mContextQPA.notifyDataSetChanged();
        mContextQPA.setQuestionnaireProgressBar();

        return true;
    }

    public void removeViewOnly(int iPos) {
        mContextQPA.removeView(mQuestionInfo.get(iPos).getId());
        mContextQPA.notifyDataSetChanged();
        mContextQPA.setQuestionnaireProgressBar();
    }

    // Returns answers given by user for specific question
    public boolean getQuestionHasBeenAnswered(int id) {
        return mEvaluationList.containsQuestionId(id);
    }

    private void returnToMenu() {
        Intent returnIntent = new Intent();
        mQuestionnaireActivity.setResult(Activity.RESULT_OK, returnIntent);
        mQuestionnaireActivity.finish();
    }

    public void moveForward() {
        mContextQPA.moveForward();
    }
}