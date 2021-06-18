package com.iha.olmega_mobilesoftware_v2.Questionnaire;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.iha.olmega_mobilesoftware_v2.Core.FileIO;
import com.iha.olmega_mobilesoftware_v2.Core.XMLReader;
import com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire.QuestionnairePagerAdapter;
import com.iha.olmega_mobilesoftware_v2.R;

import java.util.ArrayList;


public class QuestionnaireActivity extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName();
    public static AppCompatActivity thisAppCompatActivity;
    public ViewPager mViewPager;
    private QuestionnairePagerAdapter mAdapter;
    private boolean forceAnswer, isAdmin;
    String clientID, selectedQuest;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        thisAppCompatActivity = this;
        setContentView(R.layout.activity_main_questionaire);
        forceAnswer = getIntent().getExtras().getBoolean("forceAnswer");
        isAdmin = getIntent().getExtras().getBoolean("isAdmin");
        clientID = getIntent().getExtras().getString("clientID");
        selectedQuest = getIntent().getExtras().getString("selectedQuest");

        mViewPager = null;
        mViewPager = findViewById(R.id.viewpager);
        mAdapter = new QuestionnairePagerAdapter(this, mViewPager, !isAdmin);
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(myOnPageChangeListener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            if (getWindow().getInsetsController() != null) {
                getWindow().getInsetsController().hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                getWindow().getInsetsController().setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        findViewById(R.id.logo2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAdmin)
                    finish();
            }
        });
        if (isAdmin)
            findViewById(R.id.logo2).setBackgroundResource(R.color.BatteryGreen);
        else
            findViewById(R.id.logo2).setBackgroundResource(R.color.lighterGray);

        startQuestionnaire(getIntent().getExtras().getString("motivation"));
    }

    private ViewPager.OnPageChangeListener myOnPageChangeListener =
            new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrollStateChanged(int state) { }
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
                @Override
                public void onPageSelected(int position) {
                    if (!mAdapter.getHasQuestionBeenAnswered() && mAdapter.getHasQuestionForcedAnswer() && forceAnswer) {
                        mAdapter.setQuestionnaireProgressBar(position - 1);
                        mAdapter.setArrows(position - 1);
                    } else {
                        mAdapter.setQuestionnaireProgressBar(position);
                        mAdapter.setArrows(position);
                        mViewPager.setCurrentItem(position, true);
                    }
                }
            };

    // Starts a new questionnaire, motivation can be {"auto", "manual"}
    private void startQuestionnaire(String motivation) {
        FileIO mFileIO = new FileIO();
        XMLReader mXmlReader = null;
        try {
            mXmlReader = new XMLReader(this, selectedQuest);
        }
        catch (Exception e) {
            Toast.makeText(this, "No valid Questionnaire selected!", Toast.LENGTH_LONG).show();
            this.finish();
        }
        if (mXmlReader != null && mFileIO.setupFirstUse(this)) {
            ArrayList<String> questionList = mXmlReader.getQuestionList();
            String head = mXmlReader.getHead();
            String foot = mXmlReader.getFoot();
            String surveyUri = mXmlReader.getSurveyURI();
            motivation = "<motivation motivation =\"" + motivation + "\"/>";
            mAdapter.createQuestionnaire(questionList, head, foot, surveyUri, motivation, clientID);
        }
    }

    public void incrementPage() {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
    }

    public void hideSystemUI(boolean isImmersive) {
        if (isImmersive) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_VISIBLE
            );
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
    }
}
