package com.heao.geoquiz;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "QuizActivity";
    private static final String KEY_INDEX = "index";

    private Button mTrueButton;
    private Button mFalseButton;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;
    private TextView mQuestionTextView;

    private int mCurrentIndex;
    private int mCorrectCount;

    private Question[] mQuestionBank = new Question[]{
            new Question(R.string.question_australia, true),
            new Question(R.string.question_oceans, true),
            new Question(R.string.question_mideast, false),
            new Question(R.string.question_africa, false),
            new Question(R.string.question_americas, true),
            new Question(R.string.question_asia, true)
    };
    private boolean[] mIsAnswered = new boolean[mQuestionBank.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate(Bundle) called");
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            mCurrentIndex = savedInstanceState.getInt(KEY_INDEX, 0);
        }

        // 通过findViewById初始化view
        mTrueButton = findViewById(R.id.true_button);
        mFalseButton = findViewById(R.id.false_button);
        mNextButton = findViewById(R.id.next_button);
        mPrevButton = findViewById(R.id.prev_button);
        mQuestionTextView = findViewById(R.id.question_text_view);

        // 组件初始化完毕后再执行更新操作
        updateQuestion();

        // 为view绑定相应的事件
        mTrueButton.setOnClickListener(v -> {
            if(!mIsAnswered[mCurrentIndex]){
                mIsAnswered[mCurrentIndex] = true;
                checkAnswer(true);
                setButtonStatus();
            }
            else{
                Toast.makeText(this, "This question is answered.", Toast.LENGTH_SHORT).show();
            }
        });
        mFalseButton.setOnClickListener(v -> {
            if(!mIsAnswered[mCurrentIndex]){
                mIsAnswered[mCurrentIndex] = true;
                checkAnswer(false);
                setButtonStatus();
            }
            else{
                Toast.makeText(this, "This question is answered.", Toast.LENGTH_SHORT).show();
            }
        });
        mPrevButton.setOnClickListener(v -> {
            mCurrentIndex = (mCurrentIndex - 1 + mQuestionBank.length) % mQuestionBank.length;
            updateQuestion();
        });
        mNextButton.setOnClickListener(v -> {
            mCurrentIndex = (mCurrentIndex + 1) % mQuestionBank.length;
            updateQuestion();
        });
        mQuestionTextView.setOnClickListener(v -> {
            mCurrentIndex = (mCurrentIndex + 1) % mQuestionBank.length;
            updateQuestion();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.i(TAG, "onSaveInstanceState");
        savedInstanceState.putInt(KEY_INDEX, mCurrentIndex);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }

    private void updateQuestion() {
        setButtonStatus();
        mQuestionTextView.setText(mQuestionBank[mCurrentIndex].getTextResId());
        if(isAllAnswered()){
            double accuracy = (double) 100 * mCorrectCount / mQuestionBank.length;
            DecimalFormat df = new DecimalFormat("##.##");
            Toast.makeText(this, String.format("Accuracy : " + df.format(accuracy) + "%%"), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAnswer(boolean userPressedTrue) {
        boolean answerIsTrue = mQuestionBank[mCurrentIndex].isAnswerTrue();
        int messageResId = 0;
        if (userPressedTrue == answerIsTrue) {
            messageResId = R.string.correct_toast;
            // 记录回答正确的题目数
            mCorrectCount++;
        } else {
            messageResId = R.string.incorrect_toast;
        }
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    public void setButtonStatus(){
        if(!mIsAnswered[mCurrentIndex]){
            mTrueButton.setEnabled(true);
            mFalseButton.setEnabled(true);
        }
        else{
            mTrueButton.setEnabled(false);
            mFalseButton.setEnabled(false);
        }
    }

    public boolean isAllAnswered() {
        for (boolean answered : mIsAnswered) {
            if (!answered) {
                return false;
            }
        }
        return true;
    }
}