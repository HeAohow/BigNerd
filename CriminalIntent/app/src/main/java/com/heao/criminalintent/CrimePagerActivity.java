package com.heao.criminalintent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;
import java.util.UUID;

public class CrimePagerActivity extends AppCompatActivity
        implements CrimeFragment.Callbacks {
    private static final String EXTRA_CRIME_ID = "com.heao.criminalintent.crime_id";
    private ViewPager2 mViewPager;
    private Button mToFirstButton;
    private Button mToLastButton;
    private List<Crime> mCrimes;

    public static Intent newIntent(Context packageContext, UUID crimeId) {
        Intent intent = new Intent(packageContext, CrimePagerActivity.class);
        intent.putExtra(EXTRA_CRIME_ID, crimeId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_pager);

        mViewPager = findViewById(R.id.crime_view_pager);
        mToFirstButton = findViewById(R.id.to_first_button);
        mToLastButton = findViewById(R.id.to_last_button);
        mCrimes = CrimeLab.get(this).getCrimes();

        mViewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                Crime crime = mCrimes.get(position);
                return CrimeFragment.newInstance(crime.getId());
            }

            @Override
            public int getItemCount() {
                return mCrimes.size();
            }
        });
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mToFirstButton.setEnabled(true);
                mToLastButton.setEnabled(true);
                if (position == 0) {
                    mToFirstButton.setEnabled(false);
                }
                if (position == mCrimes.size() - 1) {
                    mToLastButton.setEnabled(false);
                }
            }
        });
        mToFirstButton.setOnClickListener((v) -> mViewPager.setCurrentItem(0));
        mToLastButton.setOnClickListener((v) -> mViewPager.setCurrentItem(mCrimes.size() - 1));

        // ViewPager控件加载完成后再设置当前显示页面
        UUID crimeId = (UUID) getIntent().getSerializableExtra(EXTRA_CRIME_ID);
        mViewPager.setCurrentItem(CrimeLab.get(this).getPosition(crimeId));
    }

    @Override
    public void onCrimeUpdated(Crime crime) {
        // 空方法
    }
}