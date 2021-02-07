package com.heao.criminalintent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CrimeListFragment extends Fragment {
    private static final int REQUEST_CRIME = 1;
    private static final int REQUEST_ADD = 2;
    private static final String CHANGED_CRIME_ID = "com.heao.criminalintent.crimelistfragment.crime_id";
    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle";

    private Callbacks mCallbacks;
    private RecyclerView mCrimeRecyclerView;
    private CrimeAdapter mAdapter;
    private TextView mNoCrimeText;
    private boolean mSubtitleVisible;
    private ItemTouchHelper mItemTouchHelper;

    public static Intent ChangedCrimeIntent(UUID crimeId) {
        Intent intent = new Intent();
        intent.putExtra(CHANGED_CRIME_ID, crimeId);
        return intent;
    }

    public interface Callbacks {
        void onCrimeSelected(Crime crime, int REQUEST_CODE);
    }

    public interface ItemTouchHelperAdapter {
        void onItemMove(int fromPosition, int toPosition);

        void onItemDismiss(int position);
    }

    private class ItemTouchHelperCallBack extends ItemTouchHelper.Callback {
        private  ItemTouchHelperAdapter mAdapter;

        public ItemTouchHelperCallBack(ItemTouchHelperAdapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }
    }

    private class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mTitleTextView;
        private TextView mDateTextView;
        private ImageView mSolvedImageView;

        private Crime mCrime;

        public CrimeHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_crime, parent, false));

            itemView.setOnClickListener(this);
            mTitleTextView = itemView.findViewById(R.id.crime_title);
            mDateTextView = itemView.findViewById(R.id.crime_date);
            mSolvedImageView = itemView.findViewById(R.id.crime_solved);
        }

        public void bind(Crime crime) {
            // 绑定视图资源
            mCrime = crime;
            mTitleTextView.setText(mCrime.getTitle() == null ? "no title" : mCrime.getTitle());
            mDateTextView.setText(mCrime.getDateString());
            mSolvedImageView.setVisibility(crime.isSolved() ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onClick(View view) {
//            Intent intent = CrimePagerActivity.newIntent(getActivity(), mCrime.getId());
//            startActivityForResult(intent, REQUEST_CRIME);
            mCallbacks.onCrimeSelected(mCrime, REQUEST_CRIME);
        }
    }

    private class CrimeAdapter extends RecyclerView.Adapter<CrimeHolder>
            implements ItemTouchHelperAdapter{
        private List<Crime> mCrimes;

        public CrimeAdapter(List<Crime> crimes) {
            mCrimes = crimes;
        }

        @NonNull
        @Override
        public CrimeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new CrimeHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull CrimeHolder holder, int position) {
            Crime crime = mCrimes.get(position);
            holder.bind(crime);
        }

        @Override
        public int getItemCount() {
            return mCrimes.size();
        }

        @Override
        public void onItemMove(int fromPosition, int toPosition) {
            Collections.swap(mCrimes, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onItemDismiss(int position) {
            Crime c = mCrimes.remove(position);
//            notifyItemRemoved(position);
            updateUI();
            CrimeLab.get(getActivity()).deleteCrime(c);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
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
        // 让FragmentManager知道CrimeListFragment需接收选项菜单方法回调
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime_list, container, false);
        mCrimeRecyclerView = view.findViewById(R.id.crime_recycler_view);
        mCrimeRecyclerView.setLayoutManager(new LinearLayoutManager((getActivity())));

        mAdapter = new CrimeAdapter(CrimeLab.get(getActivity()).getCrimes());
        mCrimeRecyclerView.setAdapter(mAdapter);

        mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallBack(mAdapter));
        mItemTouchHelper.attachToRecyclerView(mCrimeRecyclerView);

        mNoCrimeText = view.findViewById(R.id.no_crime);
        if (savedInstanceState != null) {
            mSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE);
        }

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("@TAG", resultCode + " " + requestCode);

        if (requestCode == REQUEST_ADD) {
            // 添加crime
            updateUI();
        } else if (requestCode == REQUEST_CRIME) {
            // 只读、删除、修改crime
            // 只读会返回Activity.RESULT_CANCEL，其他返回RESULT_OK
            if (resultCode == Activity.RESULT_OK) {
                UUID crimeId = (UUID) data.getSerializableExtra(CHANGED_CRIME_ID);
                int position = CrimeLab.get(getActivity()).getPosition(crimeId);
                if (position == -1) {
                    // 删除
                    updateUI();
                } else {
                    // 修改
                    updateItemUI(position);
                }
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        updateSubtitle();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_crime_list, menu);
        MenuItem subtitleItem = menu.findItem(R.id.show_subtitle);
        if (mSubtitleVisible) {
            subtitleItem.setTitle(R.string.hide_subtitle);
        } else {
            subtitleItem.setTitle(R.string.show_subtitle);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_crime:
                Crime crime = new Crime();
                CrimeLab.get(getActivity()).addCrime(crime);
//                Intent intent = CrimePagerActivity.newIntent(getActivity(), crime.getId());
//                startActivityForResult(intent, REQUEST_ADD);
                updateUI();
                mCallbacks.onCrimeSelected(crime, REQUEST_ADD);
                return true;
            case R.id.show_subtitle:
                mSubtitleVisible = !mSubtitleVisible;
                getActivity().invalidateOptionsMenu();
                updateSubtitle();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_SUBTITLE_VISIBLE, mSubtitleVisible);
    }

    public void updateItemUI(int position) {
        // 修改数据项
        if (mAdapter != null) {
            mAdapter.notifyItemChanged(position);
        }
    }

    public void updateUI() {
        // 增删数据项
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    public void updateSubtitle() {
        int crimeCount = CrimeLab.get(getActivity()).getCrimes().size();
        if (crimeCount == 0) {
            mCrimeRecyclerView.setVisibility(View.GONE);
            mNoCrimeText.setVisibility(View.VISIBLE);
        } else {
            mCrimeRecyclerView.setVisibility(View.VISIBLE);
            mNoCrimeText.setVisibility(View.GONE);
        }
        String subtitle = getResources().getQuantityString(R.plurals.subtitle_plural, crimeCount, crimeCount);
        if (!mSubtitleVisible) {
            subtitle = null;
        }
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setSubtitle(subtitle);
    }
}
