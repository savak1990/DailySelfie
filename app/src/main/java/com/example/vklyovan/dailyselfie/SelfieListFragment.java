package com.example.vklyovan.dailyselfie;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.Toast;

import java.io.File;

public class SelfieListFragment extends Fragment
        implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    public static String TAG = "SelfieListFragment";

    private SelfieListAdapter mSelfieListAdapter;

    private OnSelfieListFragmentListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Creating...");

        if (StorageUtil.getSelfiePath() != null ) {
            mSelfieListAdapter = new SelfieListAdapter(
                    getActivity(), StorageUtil.getSelfiePath());
        } else {
            Log.e(TAG, "Adapter cannot be created due to null storage path");

            Toast.makeText(getActivity().getApplicationContext(),
                    "Cannot load images from external storage.", Toast.LENGTH_SHORT).show();
        }

        setRetainInstance(true);

        Log.i(TAG, "Created.");
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "Creating  view...");

        View view = inflater.inflate(R.layout.fragment_selfie_list, null);
        GridView selfieGridView = (GridView) view.findViewById(R.id.selfieGridView);
        selfieGridView.setAdapter(mSelfieListAdapter);
        selfieGridView.setOnItemClickListener(this);
        selfieGridView.setOnItemLongClickListener(this);

        Log.i(TAG, "Created.");
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i(TAG, "Selfie clicked at position " + position);

        File imageFile = mSelfieListAdapter.getSelfieFile(position);

        if (mListener != null) {
            mListener.onSelfieItemClicked(imageFile);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i(TAG, "Selfie long clicked at position " + position);

        CheckBox selected = (CheckBox) view.findViewById(R.id.selectedItemCheckBox);
        selected.setChecked(!selected.isChecked());

        return true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.i(TAG, "onAttach");
        try {
            mListener = (OnSelfieListFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach");
        mListener = null;
    }

    public void addSelfie(File file) {
        mSelfieListAdapter.insert(file, 0);
    }

    public void onSelectAll(boolean isSelect) {
        for (int i = 0; i < mSelfieListAdapter.getCount(); ++i) {
            mSelfieListAdapter.selectSelfieItem(i, isSelect);
        }
        mSelfieListAdapter.notifyDataSetInvalidated();
    }

    public void onRemoveSelected() {
        mSelfieListAdapter.removeSelected();
    }

    public interface OnSelfieListFragmentListener {

        /**
         * Open image file in new fragment
         *
         * @param selfieFile - File to open in new fragment
         */
        void onSelfieItemClicked(File selfieFile);
    }
}
