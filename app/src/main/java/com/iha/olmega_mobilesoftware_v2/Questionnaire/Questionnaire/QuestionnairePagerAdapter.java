package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.iha.olmega_mobilesoftware_v2.Questionnaire.QuestionnaireActivity;
import com.iha.olmega_mobilesoftware_v2.Questionnaire.Core.ListOfViews;
import com.iha.olmega_mobilesoftware_v2.R;

import java.util.ArrayList;

/**
 * Created by ulrikkowalk on 28.02.17.
 */

public class QuestionnairePagerAdapter extends PagerAdapter {
    private String TAG = this.getClass().getSimpleName();

    final ViewPager mViewPager;
    public QuestionnaireActivity mQuestionnaireActivity;
    private static final int UI_STATE_MENU = 1;
    private int mNUM_PAGES;
    private String mHead, mFoot, mSurveyURI, mVersion;
    private Questionnaire mQuestionnaire;
    private String clientID;

    private String mMotivation = "";
    private ArrayList<String> mQuestionList;
    private static final int UI_STATE_HELP = 2;
    private static final int UI_STATE_QUEST = 3;
    private static int UI_STATE;

    public ListOfViews mListOfViews;
    private boolean isImmersive;

    public QuestionnairePagerAdapter(QuestionnaireActivity questionnaireActivity, ViewPager viewPager, boolean immersive) {
        mQuestionnaireActivity = questionnaireActivity;
        mViewPager = viewPager;
        isImmersive = immersive;
        // Set controls and listeners
        mQuestionnaireActivity.findViewById(R.id.Action_Back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mViewPager.getCurrentItem() != 0) {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
                }
            }
        });
        mQuestionnaireActivity.findViewById(R.id.Action_Forward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mViewPager.getCurrentItem() < mViewPager.getAdapter().getCount() - 1)) {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
                }
            }
        });
        mQuestionnaireActivity.findViewById(R.id.Action_Revert).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mQuestionnaireActivity, R.string.infoTextRevert, Toast.LENGTH_SHORT).show();
                createQuestionnaire();
            }
        });
    }

    // Initialise questionnaire based on new input parameters
    public void createQuestionnaire(ArrayList<String> questionList, String head, String foot, String surveyUri, String motivation, String clientID) {
        this.clientID = clientID;
        mQuestionList = questionList;
        mHead = head;
        mFoot = foot;
        mSurveyURI = surveyUri;
        mMotivation = motivation;
        // Instantiates a Questionnaire Object based on Contents of raw XML File
        mQuestionnaire = new Questionnaire(mQuestionnaireActivity, mHead, mFoot, mSurveyURI, mMotivation, mVersion, this, this.clientID);
        mQuestionnaire.setUp(questionList);
        mNUM_PAGES = mQuestionnaire.getNumPages();
        mViewPager.setOffscreenPageLimit(1);
        mListOfViews = new ListOfViews();
        createQuestionnaireLayout();
        setControlsQuestionnaire();
        // Creates and destroys views based on filter id settings
        // First, all pages are created, then unsuitable pages are erased from the list.
        mQuestionnaire.checkVisibility();

        notifyDataSetChanged();
        mViewPager.setCurrentItem(0);
        setArrows(0);
        setQuestionnaireProgressBar();

        UI_STATE = UI_STATE_QUEST;
    }

    // Initialise questionnaire based on last input parameters (only used in case of reversion)
    public void createQuestionnaire() {

        // Instantiates a Questionnaire Object based on Contents of raw XML File
        mQuestionnaire = new Questionnaire(mQuestionnaireActivity, mHead, mFoot, mSurveyURI,
                mMotivation, mVersion, this, this.clientID);

        mQuestionnaire.setUp(mQuestionList);

        mNUM_PAGES = mQuestionnaire.getNumPages();
        mViewPager.setOffscreenPageLimit(1);

        mListOfViews = new ListOfViews();

        createQuestionnaireLayout();
        setControlsQuestionnaire();
        // Creates and destroys views based on filter id settings
        mQuestionnaire.checkVisibility();

        notifyDataSetChanged();
        mViewPager.setCurrentItem(0);
        setArrows(0);
        setQuestionnaireProgressBar();
        UI_STATE = UI_STATE_QUEST;
    }

    // Set the horizontal Indicator at the Top to follow Page Position
    public void setQuestionnaireProgressBar(int position) {

        View progress = mQuestionnaireActivity.findViewById(R.id.progress);
        View regress = mQuestionnaireActivity.findViewById(R.id.regress);

        int nAccuracy = 100;
        float nProgress = (float) (position + 1) / mViewPager.getAdapter().getCount() * nAccuracy;
        float nRegress = (nAccuracy - nProgress);

        LinearLayout.LayoutParams progParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nRegress
        );
        LinearLayout.LayoutParams regParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nProgress
        );

        progress.setLayoutParams(progParams);
        regress.setLayoutParams(regParams);
    }

    // Set the horizontal Indicator at the Top to follow Page Position
    public void setQuestionnaireProgressBar() {

        int nAccuracy = 100;

        View progress = mQuestionnaireActivity.findViewById(R.id.progress);
        View regress = mQuestionnaireActivity.findViewById(R.id.regress);

        float nProgress = (float) (mViewPager.getCurrentItem() + 1) /
                mViewPager.getAdapter().getCount() * nAccuracy;
        float nRegress = (nAccuracy - nProgress);

        LinearLayout.LayoutParams progParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nRegress
        );
        LinearLayout.LayoutParams regParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nProgress
        );

        progress.setLayoutParams(progParams);
        regress.setLayoutParams(regParams);

    }

    // Adjust visibility of navigation symbols to given state
    public void setArrows(int position) {

        if (position == 0) {
            mQuestionnaireActivity.findViewById(R.id.Action_Back).setVisibility(View.INVISIBLE);
        } else if (mQuestionnaireActivity.findViewById(R.id.Action_Back).getVisibility() == View.INVISIBLE) {
            mQuestionnaireActivity.findViewById(R.id.Action_Back).setVisibility(View.VISIBLE);
        }

        if (position == mViewPager.getAdapter().getCount() - 1) {
            mQuestionnaireActivity.findViewById(R.id.Action_Forward).setVisibility(View.INVISIBLE);
        } else if (mQuestionnaireActivity.findViewById(R.id.Action_Forward).getVisibility() == View.INVISIBLE) {
            mQuestionnaireActivity.findViewById(R.id.Action_Forward).setVisibility(View.VISIBLE);
        }
    }

    // Add new page to display
    public void addView(View view, boolean isForced,
                        ArrayList<Integer> listOfAnswerIds, ArrayList<Integer> listOfFilterIds) {
        mListOfViews.add(new QuestionView(view, view.getId(), isForced,
                listOfAnswerIds, listOfFilterIds));
    }

    // Sets up visible control elements for questionnaire i.e. navigation symbols
    private void setControlsQuestionnaire() {
        mQuestionnaireActivity.findViewById(R.id.Action_Forward).setVisibility(View.VISIBLE);
        mQuestionnaireActivity.findViewById(R.id.Action_Back).setVisibility(View.VISIBLE);
        mQuestionnaireActivity.findViewById(R.id.Action_Revert).setVisibility(View.VISIBLE);
        mQuestionnaireActivity.findViewById(R.id.progress).setBackgroundColor(ContextCompat.getColor(mQuestionnaireActivity, R.color.JadeRed));
        mQuestionnaireActivity.findViewById(R.id.regress).setBackgroundColor(ContextCompat.getColor(mQuestionnaireActivity, R.color.JadeGray));
    }

    // Inserts contents into questionnaire and appoints recycler
    private void createQuestionnaireLayout() {
        // Generate a view for each page/question and collect them in ArrayList
        for (int iQuestion = 0; iQuestion < mNUM_PAGES; iQuestion++) {
            // Extracts Question Details from Questionnaire and creates Question
            Question question = mQuestionnaire.createQuestion(iQuestion);
            // Inflates Question Layout based on Question Details
            LinearLayout layout = mQuestionnaire.generateView(question, isImmersive);

            mListOfViews.add(new QuestionView(layout, layout.getId(), question.getIsForced(),
                    question.getAnswerIds(), question.getFilterIds()));

        }
    }

    public boolean getHasQuestionBeenAnswered() {

        if (mViewPager.getCurrentItem() > 0) {
            return mQuestionnaire.getQuestionHasBeenAnswered(mListOfViews.get(
                    mViewPager.getCurrentItem() - 1).getId());
        } else {
            return true;
        }
    }

    public boolean getHasQuestionForcedAnswer() {
        if (mViewPager.getCurrentItem() > 0) {
            return mListOfViews.get(mViewPager.getCurrentItem() - 1).getIsForced();
        } else {
            return true;
        }
    }

    /**
     * Array Adapter Methods
     * */

    // Removes specific view from list and updates viewpager
    void removeView(int id) {
        int nCurrentItem = mViewPager.getCurrentItem();
        mViewPager.setAdapter(null);
        mListOfViews.removeFromId(id);
        mViewPager.setAdapter(this);
        mViewPager.setCurrentItem(nCurrentItem);
    }

    // Takes view out of viewpager and includes it in displayable collection
    @Override
    public Object instantiateItem(ViewGroup collection, int position) {

        View view = mListOfViews.get(position).getView();
        collection.addView(view);
        return view;
    }

    // Removes view from displayable collection
    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    // Returns number of pages in viewpager
    @Override
    public int getCount() {
        if (!(mListOfViews == null) && !(mListOfViews.size() == 0)) {
            mNUM_PAGES = mListOfViews.size();
        } else {
            mNUM_PAGES = 0;
        }
        return mNUM_PAGES;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    // Announce change in contents and invoke rehash
    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        // Make sure the correct navigation arrows (forward, backward) are displayed
        // (this becomes an issue once the set is shortened and then enlarged again)
        int nCurrentItem = mViewPager.getCurrentItem();
        setArrows(nCurrentItem);
    }

    // Returns position of object in displayed list
    @Override
    public int getItemPosition(Object object) {

        int index = mListOfViews.indexOf(object);

        if (index == -1)
            return POSITION_NONE;
        else
            return index;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "";
    }

    public void moveForward() {
        if ((mViewPager.getCurrentItem() < mViewPager.getAdapter().getCount() - 1)) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
        }
    }

}