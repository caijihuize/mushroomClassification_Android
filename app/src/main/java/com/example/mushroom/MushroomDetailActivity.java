package com.example.mushroom;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;

public class MushroomDetailActivity extends AppCompatActivity {
    private static final String TAG = "aabb";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MushroomDetailActivity: onCreate() 开始");
        setContentView(R.layout.activity_mushroom_detail);

        // 获取传递的数据，并进行类型转换
        Mushroom mushroom = (Mushroom) getIntent().getSerializableExtra("mushroom");

        if (mushroom != null) {
            Log.d(TAG, "接收到蘑菇数据: " + mushroom.getName());
            
            // 设置视图
            TextView nameTextView = findViewById(R.id.mushroomNameDetail);
            TextView descriptionTextView = findViewById(R.id.mushroomDescriptionDetail);
            TextView featureTextView = findViewById(R.id.mushroomFeatureDetail);

            nameTextView.setText(mushroom.getName());
            descriptionTextView.setText(mushroom.getDescription());
            featureTextView.setText(mushroom.getFeature());
            Log.d(TAG, "已设置蘑菇文本信息");

            // 加载图片
            int availableImageCount = mushroom.getImageUrls().size();
            Log.d(TAG, "蘑菇图片数量: " + availableImageCount);
            
            for (int i = 0; i < Math.min(availableImageCount, 5); i++) {
                ImageView imageView = findViewById(getImageViewId(i));
                if (imageView != null && i < mushroom.getImageUrls().size()) {
                    String imageUrl = mushroom.getImageUrls().get(i); // 根据索引获取对应的图片URL
                    Log.d(TAG, "加载第 " + (i+1) + " 张图片: " + imageUrl);
                    try {
                        Bitmap bitmap = loadBitmapFromAssets(imageUrl);
                        imageView.setImageBitmap(bitmap);
                        Log.d(TAG, "图片加载成功: " + imageUrl);
                    } catch (IOException e) {
                        Log.e(TAG, "图片加载失败: " + imageUrl + ", 错误: " + e.getMessage(), e);
                        // 使用默认图片
                        imageView.setImageResource(R.drawable.ic_launcher_foreground);
                    }
                }
            }
            Log.d(TAG, "蘑菇图片加载完成");
        } else {
            Log.e(TAG, "未接收到有效的蘑菇数据");
        }
        Log.d(TAG, "MushroomDetailActivity: onCreate() 完成");
    }

    private int getImageViewId(int index) {
        switch (index) {
            case 0:
                return R.id.mushroomImageDetail1;
            case 1:
                return R.id.mushroomImageDetail2;
            case 2:
                return R.id.mushroomImageDetail3;
            case 3:
                return R.id.mushroomImageDetail4;
            case 4:
                return R.id.mushroomImageDetail5;
            default:
                return -1;
        }
    }

    private Bitmap loadBitmapFromAssets(String path) throws IOException {
        Log.d(TAG, "从Assets加载图片: " + path);
        InputStream inputStream = getAssets().open(path); // 使用当前上下文获取输入流
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        if (bitmap != null) {
            Log.d(TAG, "图片解码成功: " + path + ", 尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        } else {
            Log.e(TAG, "图片解码失败: " + path);
        }
        return bitmap;
    }
    
    @Override
    protected void onDestroy() {
        Log.d(TAG, "MushroomDetailActivity: onDestroy()");
        super.onDestroy();
    }
}