package de.adorsys.android.smsparsertest;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefUtils {

    private static final String PREF_NAME = "ru.soyer.tom.smsparsertest";

    public static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static SharedPreferences.Editor getEditor(Context context) {
        return getPref(context).edit();
    }

}
