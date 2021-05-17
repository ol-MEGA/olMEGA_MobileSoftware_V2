package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.iha.olmega_mobilesoftware_v2.Questionnaire.Core.Units;
import com.iha.olmega_mobilesoftware_v2.R;

/**
 * Created by ul1021 on 21.07.2017.
 */

public class AnswerTypePhotograph extends AppCompatActivity {

    private final Context mContext;
    private String LOG_STRING = "AnswerTypePhotograph";
    private String mString = "";
    private Button mButton1, mButton2;
    private ImageView mPreview;
    private AnswerLayout mParent;
    private LinearLayout.LayoutParams mPreviewParams, mAnswerParams;
    private LinearLayout mContainer;
    private Units mUnits;
    private int mId, mUsableHeight;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private boolean isImmersive = false;
    PackageManager pm;

    //TODO:  check camera availability up front

    public AnswerTypePhotograph(Context context, AnswerLayout parent) {
        mContext = context;
        mParent = parent;
        mUnits = new Units(mContext);
        mUsableHeight = mUnits.getUsableSliderHeight(isImmersive);

        // Slider Layout is predefined in XML
        LayoutInflater inflater = LayoutInflater.from(context);
        int width = Units.getScreenWidth();

        mContainer = (LinearLayout) inflater.inflate(
                R.layout.answer_type_photograph, parent.scrollContent, false);


        mPreview = (ImageView) mContainer.findViewById(R.id.photographyPreview);
        mButton1 = (Button) mContainer.findViewById(R.id.photographyButton1);
        mButton2 = (Button) mContainer.findViewById(R.id.photographyButton2);

        mPreview.getLayoutParams().height = mUsableHeight - 500;


        mButton1.setText("yes");
        mButton1.setTextSize(mContext.getResources().getDimension(R.dimen.textSizeAnswer));
        mButton1.setBackgroundResource(R.drawable.button);
        /*mButton1.setGravity(Gravity.CENTER_HORIZONTAL);
        mButton1.setTextColor(ContextCompat.getColor(mContext, R.color.TextColor));
        mButton1.setBackgroundColor(ContextCompat.getColor(mContext, R.color.BackgroundColor));
        mButton1.setAllCaps(false);
        mButton1.setTypeface(null, Typeface.NORMAL);
        */

        mButton2.setText("no");
        mButton2.setTextSize(mContext.getResources().getDimension(R.dimen.textSizeAnswer));
        mButton2.setBackgroundResource(R.drawable.button);
        /*mButton2.setGravity(Gravity.CENTER_HORIZONTAL);
        mButton2.setTextColor(ContextCompat.getColor(mContext, R.color.TextColor));
        mButton2.setBackgroundColor(ContextCompat.getColor(mContext, R.color.BackgroundColor));
        mButton2.setAllCaps(false);
        mButton2.setTypeface(null, Typeface.NORMAL);
        */

        pm = mContext.getPackageManager();

    }

    public void addAnswer(String sAnswer, int id) {
        mString = sAnswer;
        mId = id;

        //mButton1.setText(mString);
        //mButton1.setId(mId);


    }

    public void buildView() {

        mParent.layoutAnswer.addView(mContainer);

    }

    public void addClickListener() {

        mButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e(LOG_STRING,"Click1");

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(pm) != null) {
                    Log.e(LOG_STRING,"tpi: "+takePictureIntent.resolveActivity(pm));
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }


/*
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
*/


            }
        });

        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(LOG_STRING,"Click2");
            }
        });

    }


/*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mPreview.setImageBitmap(imageBitmap);
        }
    }
*/

}
