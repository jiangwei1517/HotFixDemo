package com.jiangwei.hotutils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * author: jiangwei18 on 17/5/4 13:42 email: jiangwei18@baidu.com Hi: jwill金牛
 */

public class FindDex {

    public static void findDex(String patchName, Context context) {
        String dexPath =
                Environment.getExternalStorageDirectory().getAbsolutePath().concat(File.separator + patchName);
        File file = new File(dexPath);
        if (file.exists()) {
            ReflectUtils.inject(dexPath, context);
            Log.e("BugFixApplication", dexPath + "存在");
        } else {
            Log.e("BugFixApplication", dexPath + "不存在");
        }
    }
}
