package com.example.mushroom;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

public class MushroomLoader {
    private static final String TAG = "aabb";

    /**
     * 从assets目录下的JSON文件加载Mushroom对象列表
     *
     * @param context 上下文
     * @param fileName JSON文件名
     * @return Mushroom对象列表
     */
    public static List<Mushroom> loadMushroomsFromJson(Context context, String fileName) {
        Log.d(TAG, "开始从 " + fileName + " 加载蘑菇数据");
        String jsonString = loadJSONFromAsset(context, fileName);
        if (jsonString == null) {
            Log.e(TAG, "JSON文件加载失败: " + fileName);
            return null;
        }

        try {
            Gson gson = new Gson();
            Type mushroomListType = new TypeToken<List<Mushroom>>() {}.getType();
            List<Mushroom> mushrooms = gson.fromJson(jsonString, mushroomListType);
            Log.d(TAG, "成功加载蘑菇数据，数量: " + mushrooms.size());
            return mushrooms;
        } catch (Exception e) {
            Log.e(TAG, "解析JSON数据失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从assets目录读取JSON文件内容
     *
     * @param context 上下文
     * @param fileName JSON文件名
     * @return JSON字符串
     */
    private static String loadJSONFromAsset(Context context, String fileName) {
        Log.d(TAG, "开始读取JSON文件: " + fileName);
        String json = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            Log.d(TAG, "JSON文件大小: " + size + " 字节");
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
            Log.d(TAG, "JSON文件读取成功");
            return json;
        } catch (IOException ex) {
            Log.e(TAG, "读取JSON文件失败: " + ex.getMessage(), ex);
            ex.printStackTrace();
            return null;
        }
    }
}
