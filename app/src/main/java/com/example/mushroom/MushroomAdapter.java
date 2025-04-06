package com.example.mushroom;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MushroomAdapter extends RecyclerView.Adapter<MushroomAdapter.MushroomViewHolder> {
    private static final String TAG = "aabb";

    private List<Mushroom> mushrooms;
    private Context context;

    public MushroomAdapter(Context context, List<Mushroom> mushrooms) {
        this.context = context;
        this.mushrooms = mushrooms;
        Log.d(TAG, "MushroomAdapter: 初始化，蘑菇数量: " + mushrooms.size());
    }

    @NonNull
    @Override
    public MushroomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "MushroomAdapter: onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mushroom, parent, false);
        return new MushroomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MushroomViewHolder holder, int position) {
        Mushroom mushroom = mushrooms.get(position);
        holder.nameTextView.setText(mushroom.getName());
        Log.d(TAG, "绑定蘑菇项 #" + position + ": " + mushroom.getName());

        // 加载第一张图片
        if (mushroom.getImageUrls().size() > 0) {
            String imageUrl = mushroom.getImageUrls().get(0);
            Log.d(TAG, "加载蘑菇图片: " + imageUrl);
            try {
                Bitmap bitmap = loadBitmapFromAssets(imageUrl);
                holder.imageView.setImageBitmap(bitmap);
                Log.d(TAG, "图片加载成功: " + imageUrl);
            } catch (IOException e) {
                Log.e(TAG, "图片加载失败: " + imageUrl + ", 错误: " + e.getMessage(), e);
                // 使用默认图片
                holder.imageView.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            Log.w(TAG, "蘑菇没有图片: " + mushroom.getName());
            holder.imageView.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "点击了蘑菇项: " + mushroom.getName());
            Intent intent = new Intent(context, MushroomDetailActivity.class);
            intent.putExtra("mushroom", mushroom);
            context.startActivity(intent);
            Log.d(TAG, "启动蘑菇详情页面: " + mushroom.getName());
        });
    }

    @Override
    public int getItemCount() {
        return mushrooms.size();
    }

    static class MushroomViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameTextView;

        MushroomViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.mushroomImage);
            nameTextView = itemView.findViewById(R.id.mushroomName);
        }
    }

    private Bitmap loadBitmapFromAssets(String path) throws IOException {
        Log.d(TAG, "从Assets加载图片: " + path);
        InputStream inputStream = context.getAssets().open(path);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        if (bitmap != null) {
            Log.d(TAG, "图片解码成功: " + path + ", 尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        } else {
            Log.e(TAG, "图片解码失败: " + path);
        }
        return bitmap;
    }
}