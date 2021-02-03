package com.heao.criminalintent;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CrimeLab {
    // 单例模式
    private static CrimeLab sCrimeLab;
    private List<Crime> mCrimes;
    private HashMap<UUID, Crime> mCrimeMap;

    public static CrimeLab get(Context context) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(context);
        }
        return sCrimeLab;
    }

    private CrimeLab(Context context) {
        mCrimes = new ArrayList<>();
        mCrimeMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            Crime crime = new Crime();
            crime.setTitle("Crime #" + i);
            crime.setSolved(i % 2 == 0); // Every other one
            mCrimes.add(crime);
            mCrimeMap.put(crime.getId(), crime);
        }
    }

    public List<Crime> getCrimes() {
        return mCrimes;
    }

    public Crime getCrime(UUID id) {
//        for (Crime crime : mCrimes) {
//            if (crime.getId().equals(id)) {
//                return crime;
//            }
//        }
//        return null;
        if (mCrimeMap.containsKey(id)) {
            return mCrimeMap.get(id);
        } else {
            return null;
        }
    }

    public int getPosition(UUID id) {
        int position = 0;
        for (Crime crime : mCrimes) {
            if (crime.getId().equals(id)) {
                return position;
            }
            position++;
        }
        return -1;
    }
}
