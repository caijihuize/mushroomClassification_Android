package com.example.mushroom;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ResultViewHolder> {
    private static final String TAG = "aabb";

    private List<IdentifyResult> results;
    private Context context;
    private MushroomLoader mushroomLoader;

    public ResultAdapter(Context context) {
        this.context = context;
        this.results = new ArrayList<>();
        this.mushroomLoader = new MushroomLoader();
        Log.d(TAG, "ResultAdapter: 初始化");
    }

    public void setResults(List<IdentifyResult> results) {
        Log.d(TAG, "设置识别结果数据，项目数: " + results.size());
        this.results = results;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "ResultAdapter: onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        IdentifyResult result = results.get(position);
        // 从 "编号 蘑菇名称" 格式中提取编号和蘑菇名称
        String[] parts = result.getClassName().split("\\s+", 2);
        String classNumber = parts.length > 0 ? parts[0] : "";
        String mushroomName = parts.length > 1 ? parts[1] : result.getClassName();

        Log.d(TAG, "绑定结果项 #" + position + ": " + mushroomName + " (编号:" + classNumber + "), 概率: " + result.getProbability());

        // 设置蘑菇名称和概率
        holder.nameTextView.setText(mushroomName);
        holder.numberTextView.setText("分类编号: " + classNumber);
        holder.probabilityTextView.setText(String.format("%.2f%%", result.getProbability() * 100));

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "点击了识别结果项: " + mushroomName);
            // 加载所有蘑菇数据
            List<Mushroom> mushrooms = MushroomLoader.loadMushroomsFromJson(context, "mushrooms.json");
            
            if (mushrooms != null) {
                Log.d(TAG, "加载蘑菇数据成功，总项目数: " + mushrooms.size());
                // 获取该蘑菇的索引（基于labels.txt中的编号）
                int index = Integer.parseInt(classNumber);
                
                // 确保索引有效
                if (index >= 0 && index < mushrooms.size()) {
                    // 找到对应的蘑菇对象
                    Mushroom mushroom = mushrooms.get(index);
                    Log.d(TAG, "找到匹配的蘑菇详情: " + mushroom.getName());
                    
                    // 启动详情页
                    Intent intent = new Intent(context, MushroomDetailActivity.class);
                    intent.putExtra("mushroom", mushroom);
                    context.startActivity(intent);
                    Log.d(TAG, "启动蘑菇详情页面");
                } else {
                    Log.e(TAG, "无效的蘑菇索引: " + index);
                }
            } else {
                Log.e(TAG, "无法加载蘑菇数据");
            }
        });
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView numberTextView;
        TextView probabilityTextView;

        ResultViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.resultItemName);
            numberTextView = itemView.findViewById(R.id.resultItemNumber);
            probabilityTextView = itemView.findViewById(R.id.resultItemProbability);
        }
    }

    // 内部类用于存储识别结果
    public static class IdentifyResult {
        private String className;
        private float probability;

        public IdentifyResult(String className, float probability) {
            this.className = className;
            this.probability = probability;
        }

        public String getClassName() {
            return className;
        }

        public float getProbability() {
            return probability;
        }
    }
} 