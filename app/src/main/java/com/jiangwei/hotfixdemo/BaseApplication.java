package com.jiangwei.hotfixdemo;

import android.app.Application;

import com.jiangwei.hotutils.AssetsUtil;
import com.jiangwei.hotutils.FindDex;
import com.jiangwei.hotutils.ReflectUtils;

import java.io.File;
import java.io.IOException;

/**
 * author: jiangwei18 on 17/5/3 20:12 email: jiangwei18@baidu.com Hi: jwill金牛
 */

public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        File remoteDexDir = getDir("remoteDexDir", 0);
        File tagDex = new File(remoteDexDir,"tag_dex.jar");
        try {
            AssetsUtil.copyAssets(this,"tag_dex.jar", tagDex.getAbsolutePath());
            ReflectUtils.inject(tagDex.getAbsolutePath(), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FindDex.findDex("patch_dex.jar", this);
    }
}
