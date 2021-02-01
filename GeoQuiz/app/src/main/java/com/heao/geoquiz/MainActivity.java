package com.heao.geoquiz;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private Button mTrueButton;
    private Button mFalseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTrueButton = findViewById(R.id.true_button);
        mFalseButton = findViewById(R.id.false_button);

        Toast mTrueToast = Toast.makeText(MainActivity.this, R.string.correct_toast, Toast.LENGTH_SHORT);
        Toast mFalseToast = Toast.makeText(MainActivity.this, R.string.incorrect_toast, Toast.LENGTH_SHORT);

        mTrueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTrueToast.setGravity(Gravity.TOP, 0, 0);
                mTrueToast.show();
            }
        });

        mFalseButton.setOnClickListener(v -> {
            mFalseToast.setGravity(Gravity.TOP, 0, 0);
            mFalseToast.show();
        });
    }
}