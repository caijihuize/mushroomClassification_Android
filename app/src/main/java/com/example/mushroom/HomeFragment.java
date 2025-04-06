package com.example.mushroom;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "aabb";

    private RecyclerView recyclerView;
    private List<Mushroom> mushrooms;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "HomeFragment: onCreateView() 开始");
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 加载蘑菇数据
        Log.d(TAG, "开始加载蘑菇图鉴数据");
        mushrooms = MushroomLoader.loadMushroomsFromJson(requireContext(), "mushrooms.json");

        if (mushrooms != null) {
            Log.d(TAG, "成功加载蘑菇图鉴，数量: " + mushrooms.size());
            for (Mushroom mushroom : mushrooms) {
                // 处理每个蘑菇对象
                Log.d(TAG, "加载蘑菇: " + mushroom.getName() + ", 图片数量: " + mushroom.getImageUrls().size());
            }

            // 设置适配器
            MushroomAdapter adapter = new MushroomAdapter(requireContext(), mushrooms);
            recyclerView.setAdapter(adapter);
            Log.d(TAG, "蘑菇图鉴显示适配器设置完成");
        } else {
            Log.e(TAG, "蘑菇图鉴数据加载失败");
        }
        
        Log.d(TAG, "HomeFragment: onCreateView() 完成");
        return view;
    }
} 