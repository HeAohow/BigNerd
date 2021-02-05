package com.heao.criminalintent;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CrimeListActivity extends SingleFragmentActivity
        implements CrimeListFragment.Callbacks, CrimeFragment.Callbacks {
    @Override
    protected Fragment createFragment() {
        return new CrimeListFragment();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_masterdetail;
    }

    @Override
    public void onCrimeSelected(Crime crime, int REQUEST_CODE) {
        if (findViewById(R.id.detail_fragment_container) == null) {
            // single-pane
            Intent intent = CrimePagerActivity.newIntent(this, crime.getId());
            startActivityForResult(intent, REQUEST_CODE);
        } else {
            // two-pane
            Fragment newDetail = CrimeFragment.newInstance(crime.getId());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment_container, newDetail)
                    .commit();
        }
    }

    @Override
    public void onCrimeUpdated(Crime crime) {
        CrimeListFragment fragment = (CrimeListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        fragment.updateUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("@TAG", resultCode + " " + requestCode);
        CrimeListFragment fragment = (CrimeListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        fragment.updateUI();
    }
}
