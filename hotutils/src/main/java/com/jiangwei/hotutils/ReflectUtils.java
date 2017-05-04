package com.jiangwei.hotutils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import dalvik.system.DexClassLoader;

/**
 * author: jiangwei18 on 17/5/4 13:43 email: jiangwei18@baidu.com Hi: jwill金牛
 */

public class ReflectUtils {

    public static void inject(String path, Context context) {
        File file = new File(path);
        if (file.exists()) {
            try {
                // 获取classes的dexElements
                Class<?> cl = Class.forName("dalvik.system.BaseDexClassLoader");
                Field pathListField = cl.getDeclaredField("pathList");
                pathListField.setAccessible(true);
                pathListField.get(context.getClassLoader());
                Object pathList = getField(cl, "pathList", context.getClassLoader());
                Object baseElements = getField(pathList.getClass(), "dexElements", pathList);

                // 获取patch_dex的dexElements（需要先加载dex）
                String dexopt = context.getDir("dexopt", 0).getAbsolutePath();
                // optimizedDirector 优化后的dex文件存放目录，不能为null
                // libraryPath 目标类中使用的C/C++库的列表,每个目录用File.pathSeparator间隔开; 可以为 null
                // parent 该类装载器的父装载器，一般用当前执行类的装载器
                DexClassLoader dexClassLoader = new DexClassLoader(path, dexopt, dexopt, context.getClassLoader());
                Object obj = getField(cl, "pathList", dexClassLoader);
                Object dexElements = getField(obj.getClass(), "dexElements", obj);

                // 合并两个Elements
                Object combineElements = combineArray(dexElements, baseElements);

                // 将合并后的Element数组重新赋值给app的classLoader
                setField(pathList.getClass(), "dexElements", pathList, combineElements);

                // ======== 以下是测试是否成功注入 AndroidStudio因为开启了instant run 即使注入失败也不会提示=================
                Object object = getField(pathList.getClass(), "dexElements", pathList);
                int length = Array.getLength(object);
                for (Object o : (Object[]) object) {
                    System.out.println(o);
                }
                Log.e("BugFixApplication", "length = " + length);

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("HotFix:", "File not exist");
        }
    }

    private static Object getField(Class<?> cl, String fieldName, Object object)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = cl.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }

    /**
     * 通过反射设置对象的属性值
     */
    private static void setField(Class<?> cl, String fieldName, Object object, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = cl.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    /**
     * 通过反射合并两个数组
     */
    private static Object combineArray(Object firstArr, Object secondArr) {
        int firstLength = Array.getLength(firstArr);
        int secondLength = Array.getLength(secondArr);
        int length = firstLength + secondLength;

        Class<?> componentType = firstArr.getClass().getComponentType();
        Object newArr = Array.newInstance(componentType, length);
        for (int i = 0; i < length; i++) {
            if (i < firstLength) {
                Array.set(newArr, i, Array.get(firstArr, i));
            } else {
                Array.set(newArr, i, Array.get(secondArr, i - firstLength));
            }
        }
        return newArr;
    }
}
