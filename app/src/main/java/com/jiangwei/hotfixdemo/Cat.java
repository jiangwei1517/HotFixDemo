package com.jiangwei.hotfixdemo;

import android.content.Context;
import android.widget.Toast;

/**
 * author: jiangwei18 on 17/5/3 19:19 email: jiangwei18@baidu.com Hi: jwill金牛
 */

public class Cat {
    private Context mContext;

    public Cat(Context context) {
        mContext = context;
    }

    public void say() {
        Toast.makeText(mContext, "汪汪汪", Toast.LENGTH_SHORT).show();
    }
}
