package com.example.vklyovan.dailyselfie;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;


/**
 * TODO
 */
public class SelfieViewFragment extends Fragment {

    public static final String TAG = "SelfieViewFragment";

    private static final String IMAGE_FILE_PATH = "imageFilePath";

    private Bitmap mImageBitmap;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param imageFilePath path to the selfie file
     * @return A new instance of fragment SelfieViewFragment.
     */
    public static SelfieViewFragment newInstance(File imageFilePath) {
        SelfieViewFragment fragment = new SelfieViewFragment();
        Bundle args = new Bundle();
        args.putSerializable(IMAGE_FILE_PATH, imageFilePath);
        fragment.setArguments(args);
        return fragment;
    }

    public SelfieViewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            File imageFilePath = (File) getArguments().getSerializable(IMAGE_FILE_PATH);

            if (imageFilePath != null && imageFilePath.exists()) {
                mImageBitmap = BitmapFactory.decodeFile(imageFilePath.getAbsolutePath());
                Log.i(TAG, "Bitmap with size (" + mImageBitmap.getWidth() + ", "
                        + mImageBitmap.getHeight() + ") successfully loaded");
            } else {
                Log.e(TAG, "Failed to get correct image");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.fragment_selfie_view, null);
        ImageView imageView = (ImageView) fragView.findViewById(R.id.selfieImageView);
        imageView.setImageBitmap(mImageBitmap);
        return fragView;
    }

}
