package com.heao.criminalintent;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.heao.database.CrimeBaseHelper;
import com.heao.database.CrimeCursorWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.heao.database.CrimeDbSchema.CrimeTable;

public class CrimeLab {
    // 单例模式
    private static CrimeLab sCrimeLab;
    private List<Crime> mCrimes;
    private HashMap<UUID, Crime> mCrimeMap;
    private Context mContext;
    private SQLiteDatabase mDatabase;

    private static ContentValues getContentValues(Crime crime) {
        ContentValues values = new ContentValues();
        values.put(CrimeTable.Cols.UUID, crime.getId().toString());
        values.put(CrimeTable.Cols.TITLE, crime.getTitle());
        values.put(CrimeTable.Cols.DATE, crime.getDate().getTime());
        values.put(CrimeTable.Cols.SOLVED, crime.isSolved() ? 1 : 0);
        values.put(CrimeTable.Cols.SUSPECT, crime.getSuspect());
        values.put(CrimeTable.Cols.PHONE, crime.getPhone());

        return values;
    }

    private CrimeLab(Context context) {
        mCrimeMap = new HashMap<>();
        mContext = context.getApplicationContext();
        mDatabase = new CrimeBaseHelper(mContext).getWritableDatabase();
    }

    public static CrimeLab get(Context context) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(context);
        }
        return sCrimeLab;
    }

    public List<Crime> getCrimes() {
        if (mCrimes == null) {
            mCrimes = new ArrayList<>();
            // 从数据库中取出数据
            try (CrimeCursorWrapper cursorWrapper = queryCrimes(null, null)) {
                cursorWrapper.moveToFirst();
                while (!cursorWrapper.isAfterLast()) {
                    mCrimes.add(cursorWrapper.getCrime());
                    cursorWrapper.moveToNext();
                }
            }
            // 加入Map中方便以后查询
            for (Crime crime : mCrimes) {
                mCrimeMap.put(crime.getId(), crime);
            }
        }

        return mCrimes;
    }

    public Crime getCrime(UUID id) {
        if (mCrimeMap.containsKey(id)) {
            return mCrimeMap.get(id);
        }
        // Map中未查询到
        try (CrimeCursorWrapper cursorWrapper = queryCrimes(
                CrimeTable.Cols.UUID + " = ?",
                new String[]{id.toString()}
        )) {
            if (cursorWrapper.getCount() == 0) {
                return null;
            }
            cursorWrapper.moveToFirst();
            return cursorWrapper.getCrime();
        }
    }

    public int getPosition(UUID id) {
        for (int i = 0; i < mCrimes.size(); i++) {
            if (mCrimes.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public void addCrime(Crime c) {
        ContentValues values = getContentValues(c);
        mDatabase.insert(CrimeTable.NAME, null, values);
        mCrimes.add(c);
        mCrimeMap.put(c.getId(), c);
    }

    public void deleteCrime(Crime c) {
        mCrimes.remove(c);
        mCrimeMap.remove(c.getId());
        mDatabase.delete(CrimeTable.NAME,
                CrimeTable.Cols.UUID + " = ?",
                new String[]{c.getId().toString()});
    }

    public void updateCrime(Crime crime) {
        String uuidString = crime.getId().toString();
        ContentValues values = getContentValues(crime);
        mDatabase.update(CrimeTable.NAME, values, CrimeTable.Cols.UUID + " = ?",
                new String[]{uuidString});
    }

    private CrimeCursorWrapper queryCrimes(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                CrimeTable.NAME,
                null, // Columns - null selects all columns
                whereClause,
                whereArgs,
                null, // groupBy
                null, // having
                null // orderBy
        );
        return new CrimeCursorWrapper(cursor);
    }

    public File getPhotoFile(Crime crime) {
        // 获取 ?/<packageName>/files 目录
        File filesDir = mContext.getFilesDir();
        return new File(filesDir, crime.getPhotoFilename());
    }
}
