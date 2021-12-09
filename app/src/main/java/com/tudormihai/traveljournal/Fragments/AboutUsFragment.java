package com.tudormihai.traveljournal.Fragments;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.tudormihai.traveljournal.R;

public class AboutUsFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_about_us, container, false);
        String aboutUsString = getActivity().getResources().getString(R.string.about_us);
        TextView aboutUsTextView = view.findViewById(R.id.about_us);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            aboutUsTextView.setText(Html.fromHtml(aboutUsString, Html.FROM_HTML_MODE_COMPACT));
        } else {
            aboutUsTextView.setText(Html.fromHtml("Incompatible version"));
        }

        return view;
    }
}