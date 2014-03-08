package com.metodica.wemoplugin;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;

import com.metodica.wemoplugin.fragments.eKamenoClientFoundFragment;
import com.metodica.wemoplugin.fragments.eKamenoNodeFoundFragment;
import com.metodica.wemoplugin.fragments.eKamenoNotFoundFragment;

public class WeMoConfigActivity extends FragmentActivity {
    static final String classTAG = "WeMoConfigActivity";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wemo_config);
        Log.d(classTAG, "onCreate ConfigActivity");

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (isPackageInstalled("com.metodica.ekamenoclient", this))
            ft.replace(R.id.fragmentPlaceholder, new eKamenoClientFoundFragment());
        else
        if (isPackageInstalled("com.metodica.ekamenoserver", this))
            ft.replace(R.id.fragmentPlaceholder, new eKamenoNodeFoundFragment());
        else
            ft.replace(R.id.fragmentPlaceholder, new eKamenoNotFoundFragment());

        ft.commit();
    }

    private boolean isPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
