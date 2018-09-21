package com.shiqian.matrix;
/*
 * 包名：Matrix
 * 文件名： BaseApplication
 * 创建者：wushiqian
 * 创建时间 2018/9/20 下午9:35
 * 描述： TODO//
 */

import android.app.Application;
import android.graphics.Color;

import es.dmoral.toasty.Toasty;

public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Toasty.Config.getInstance()
                .setErrorColor(Color.RED) // optional
                .setInfoColor(Color.BLUE) // optional
                .setSuccessColor(Color.GREEN) // optional
                .tintIcon(true) // optional (apply textColor also to the icon)
//                .setToastTypeface(@NonNull Typeface typeface) // optional
                .setTextSize(5) // optional
                .apply(); // required
    }

}