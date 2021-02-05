package com.heao.criminalintent;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.heao.utils.PictureUtils;

public class DetailPhotoFragment extends DialogFragment {
    private static final String ARG_PHOTO = "photo";

    private ImageView mImageView;

    public static DetailPhotoFragment newInstance(String photoPath) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_PHOTO, photoPath);
        DetailPhotoFragment fragment = new DetailPhotoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        String path = (String) getArguments().getSerializable(ARG_PHOTO);
        Bitmap bitmap = PictureUtils.getScaledBitmap(path, getActivity());

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_photo, null);
        mImageView = v.findViewById(R.id.dialog_photo);
        mImageView.setImageBitmap(bitmap);

        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setNegativeButton(android.R.string.ok, (dialogInterface, i) -> {})
                .create();
    }

}
