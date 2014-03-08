package com.metodica.wemoplugin.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.metodica.wemoplugin.R;

/**
 * Created by Jacob on 1/11/14.
 */
public class eKamenoClientFoundFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_wemo_config, container, false);
        ((TextView)(v.findViewById(R.id.eKsubtitle))).setText(getActivity().getString(R.string.ClientInstalled));
        ((TextView)(v.findViewById(R.id.info))).setText(getActivity().getString(R.string.ClientInstalledInfo));

        ((Button)v.findViewById(R.id.actionButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse("http://blog.ekameno.com/") );
                startActivity(browse);
            }
        });
        return v;
    }
}
