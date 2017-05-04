package com.jiangwei.hotfixdemo;

import android.content.Context;
import android.widget.Toast;

/**
 * author: jiangwei18 on 17/5/3 19:19 email: jiangwei18@baidu.com Hi: jwill金牛
 */

public class Person {
    private Context mContext;

    public Person(Context context) {
        mContext = context;
    }

    public void name() {
        Toast.makeText(mContext, "姜威", Toast.LENGTH_SHORT).show();
    }
}
