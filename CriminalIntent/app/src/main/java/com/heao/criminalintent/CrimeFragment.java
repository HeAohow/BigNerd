package com.heao.criminalintent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.heao.utils.PictureUtils;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CrimeFragment extends Fragment {
    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String DIALOG_DETAIL_PHOTO = "DialogDetailPhoto";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO = 2;

    private final String PHOTO_AUTHORITY = "com.heao.criminalintent.fileprovider";

    private Crime mCrime;
    private EditText mTitleField;
    private Button mDateButton;
    private CheckBox mSolvedCheckBox;
    private Button mReportButton;
    private Button mSuspectButton;
    private Button mCallButton;
    private ImageButton mPhotoButton;
    private ImageView mPhotoView;
    private int photoWidth;
    private int photoHeight;
    private File mPhotoFile;
    private Callbacks mCallbacks;

    public static CrimeFragment newInstance(UUID crimeId) {
        // 绑定Fragment参数
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);
        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public interface Callbacks {
        void onCrimeUpdated(Crime crime);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 获取Bundle中的参数
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_crime, container, false);
        // 初始化控件，绑定视图资源
        mTitleField = v.findViewById(R.id.crime_title);
        mSolvedCheckBox = v.findViewById(R.id.crime_solved);
        mDateButton = v.findViewById(R.id.crime_date);
        mReportButton = v.findViewById(R.id.crime_report);
        mSuspectButton = v.findViewById(R.id.crime_suspect);
        mCallButton = v.findViewById(R.id.call_the_suspect);
        mPhotoButton = v.findViewById(R.id.crime_camera);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);
        mPhotoView = v.findViewById(R.id.crime_photo);

        // 为控件绑定事件
        mTitleField.setText(mCrime.getTitle());
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mDateButton.setText(mCrime.getDateString());
        mDateButton.setOnClickListener((view) -> {
            FragmentManager fm = getFragmentManager();
            DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
            // Fragment之间通信 1. 设置target 2. 直接调用target的onActivityResult方法
            dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
            // 将fragment添加给fragmentManager并显示在屏幕上
            // String用于在fragment队列中进行唯一标识
            dialog.show(fm, DIALOG_DATE);
        });

        mPhotoView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                photoWidth = mPhotoView.getMeasuredWidth();
                photoHeight = mPhotoView.getMeasuredHeight();
                mPhotoView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                updatePhotoView();
            }
        });

        mPhotoView.setOnClickListener(v5 -> {
            if (mPhotoFile == null || !mPhotoFile.exists()) {
                return;
            }
            FragmentManager fm = getFragmentManager();
            DetailPhotoFragment dialog = DetailPhotoFragment.newInstance(mPhotoFile.getPath());
            dialog.show(fm, DIALOG_DETAIL_PHOTO);
        });

        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mCrime.setTitle(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mCrime.setTitle(mTitleField.getText().toString());
                // 修改
                updateCrime();
                returnResult();
            }
        });

        mSolvedCheckBox.setOnCheckedChangeListener((compoundButton, b) -> {
            mCrime.setSolved(b);
            // 修改
            updateCrime();
            returnResult();
        });

        mReportButton.setOnClickListener(v1 -> {
            // 挑战之ShareCompat
            ShareCompat.IntentBuilder intentBuilder = ShareCompat.IntentBuilder.from(getActivity());
            intentBuilder.setType("text/plain");
            intentBuilder.setText(getCrimeReport());
            intentBuilder.setSubject(getString(R.string.crime_report_subject));
            intentBuilder.createChooserIntent();
            intentBuilder.startChooser();
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton.setOnClickListener(v2 -> {
            // TODO 权限请求弹窗
//            ContextCompat.checkSelfPermission(getContext().getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            startActivityForResult(pickContact, REQUEST_CONTACT);
        });
        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(mCrime.getSuspect());
        }
        // 验证设备是否具有隐式intent请求的功能的应用
        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact, PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        mCallButton.setOnClickListener(v3 -> {
            if (mCrime.getSuspect() == null) {
                Toast.makeText(getActivity(), R.string.choose_suspect_prompt, Toast.LENGTH_SHORT).show();
            } else {
                Intent i = new Intent(Intent.ACTION_DIAL);
                Uri phone = Uri.parse("tel:" + mCrime.getPhone());
                i.setData(phone);
                startActivity(i);
            }
        });

        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        boolean canTakePhoto = mPhotoFile != null && captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);
        mPhotoButton.setOnClickListener(v4 -> {
            // 通过FileProvider将文件路径转化为uri
            Uri uri = FileProvider.getUriForFile(getActivity(),
                    PHOTO_AUTHORITY,
                    mPhotoFile);
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            // 获取intent的所有目标请求应用
            List<ResolveInfo> cameraActivities = getActivity()
                    .getPackageManager()
                    .queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY);
            // 为intent的所有目标请求应用的activity都授予 FLAG_GRANT_WRITE_URI_PERMISSION 写入uri指定位置的权限
            for (ResolveInfo activity : cameraActivities) {
                getActivity().grantUriPermission(activity.activityInfo.packageName,
                        uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }

            startActivityForResult(captureImage, REQUEST_PHOTO);
        });

        return v;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_crime:
                CrimeLab.get(getActivity()).deleteCrime(mCrime);
                // 删除
                updateCrime();
                returnResult();
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // 接收从日期选择Fragment返回的结果
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_DATE) {
            Date date = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            mDateButton.setText(mCrime.getDateString());
            // 修改
            updateCrime();
            returnResult();
        } else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            // 返回全部联系人姓名
            String[] queryFields = new String[]{ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID};
            // 相当于执行一次查询 这里contactUri作用相当于"where"
            try (Cursor c = getActivity().getContentResolver().
                    query(contactUri, queryFields, null, null, null)) {
                // 再次检验确实获取了返回结果
                if (c.getCount() == 0) {
                    return;
                }
                // 获取第一行第一列的代码，即嫌疑人姓名
                c.moveToFirst();
                String suspect = c.getString(0);
                mCrime.setSuspect(suspect);
                mSuspectButton.setText(suspect);

                String contactId = c.getString(1);
                Cursor phoneCursor = getActivity().getContentResolver()
                        .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                                null,
                                null);
                if (phoneCursor.moveToNext()) {
                    String phoneNumber = phoneCursor
                            .getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    mCrime.setPhone(phoneNumber);
                }
            }
        } else if (requestCode == REQUEST_PHOTO) {
            Uri uri = FileProvider.getUriForFile(getActivity(), PHOTO_AUTHORITY, mPhotoFile);
            getActivity().revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            updatePhotoView();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    public void returnResult() {
        // 借助托管的子Activity，将结果返回至主Activity
        // 改变crime相关参数时调用，更新返回后的视图
        Intent data = CrimeListFragment.ChangedCrimeIntent(mCrime.getId());
        getActivity().setResult(Activity.RESULT_OK, data);
    }

    private String getCrimeReport() {
        String solvedString = null;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }
        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();
        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }
        String report = getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);
        return report;
    }

    private void updatePhotoView() {
        if (mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFile.getPath(), photoWidth, photoHeight);
            mPhotoView.setImageBitmap(bitmap);
        }
    }

    private void updateCrime(){
        CrimeLab.get(getActivity()).updateCrime(mCrime);
        mCallbacks.onCrimeUpdated(mCrime);
    }
}
