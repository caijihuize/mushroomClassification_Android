package com.example.mushroom;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.graphics.drawable.BitmapDrawable;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.util.Arrays;
import java.lang.Math;
import java.lang.StringBuilder;

public class IdentifyFragment extends Fragment {
    private static final String TAG = "aabb";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 2;

    private static final int IMAGE_LENGTH = 224;
    private static final int IMAGE_WIDTH = 224;
    private static final int SPECIES_NUM = 36;

    private ImageView selectedImageView;
    private RecyclerView resultRecyclerView;
    private TextView emptyResultText;
    private Button selectButton;
    private Button cameraButton;

    private ResultAdapter resultAdapter;
    private Interpreter tfliteInterpreter;
    private List<String> labels;
    
    // 使用ActivityResultLauncher代替startActivityForResult
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private Spinner modelSpinner;
    private String currentModelPath = "models/MobileNetV1.tflite"; // 修改默认模型路径为实际存在的模型
    
    // 模型映射：显示名称 -> 文件路径
    private Map<String, String> modelPathMap = new HashMap<>();

    // 在类顶部声明模型配置对象
    private class ModelConfig {
        float[] mean;
        float[] std;
        boolean bgr;
        
        ModelConfig(float[] mean, float[] std, boolean bgr) {
            this.mean = mean;
            this.std = std;
            this.bgr = bgr;
        }
    }
    
    // 模型配置映射
    private Map<String, ModelConfig> modelConfigMap = new HashMap<>();

    // 添加调试标志
    private static final boolean DEBUG_MODE = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "IdentifyFragment: onCreate() 开始");
        
        // 注册图库选择结果回调
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "图库选择结果回调");
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    try {
                        Log.d(TAG, "图库选择成功，开始加载图片");
                        InputStream inputStream = requireContext().getContentResolver()
                            .openInputStream(result.getData().getData());
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (bitmap == null) {
                            Log.e(TAG, "图库图片解码失败");
                            Toast.makeText(requireContext(), R.string.image_load_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectedImageView.setImageBitmap(bitmap);
                        Log.d(TAG, "开始识别图库图片，尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                        classifyImage(bitmap);
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "图库图片加载失败: " + e.getMessage());
                        Toast.makeText(requireContext(), R.string.image_load_error, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "图库选择取消或失败");
                }
            }
        );
        
        // 注册相机结果回调
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "相机拍照结果回调");
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String photoPath = result.getData().getStringExtra("photo_path");
                    Log.d(TAG, "相机返回图片路径: " + (photoPath != null ? photoPath : "null"));
                    if (photoPath != null) {
                        Log.d(TAG, "开始从文件加载相机拍摄的图片");
                        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
                        if (bitmap == null) {
                            Log.e(TAG, "相机图片解码失败");
                            Toast.makeText(requireContext(), R.string.image_load_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        Log.d(TAG, "加载相机图片成功，尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                        // 设置图像并执行识别
                        selectedImageView.setImageBitmap(bitmap);
                        classifyImage(bitmap);
                        
                        // 识别完成后删除临时文件
                        boolean deleteResult = new File(photoPath).delete();
                        Log.d(TAG, "临时文件删除结果: " + deleteResult);
                    } else {
                        Log.e(TAG, "相机返回结果中没有图片路径");
                        Toast.makeText(requireContext(), R.string.image_load_error, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "相机拍照取消或失败");
                }
            }
        );
        
        // 注册权限请求回调
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(requireContext(), R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
                }
            }
        );
        
        Log.d(TAG, "IdentifyFragment: onCreate() 完成");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "IdentifyFragment: onCreateView() 开始");
        View view = inflater.inflate(R.layout.fragment_identify, container, false);
        
        selectedImageView = view.findViewById(R.id.selectedImageView);
        resultRecyclerView = view.findViewById(R.id.resultRecyclerView);
        selectButton = view.findViewById(R.id.button);
        cameraButton = view.findViewById(R.id.cameraButton);
        emptyResultText = view.findViewById(R.id.emptyResultText);
        
        // 初始化RecyclerView和适配器
        resultAdapter = new ResultAdapter(requireContext());
        resultRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        resultRecyclerView.setAdapter(resultAdapter);
        
        // 初始化模型选择Spinner
        modelSpinner = view.findViewById(R.id.modelSpinner);
        setupModelSpinner();
        
        // 设置相册按钮点击事件 - 使用现代的MediaStore API
        selectButton.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });
        
        // 设置相机按钮点击事件
        cameraButton.setOnClickListener(v -> {
            Log.d(TAG, "相机按钮点击");
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "请求相机权限");
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                Log.d(TAG, "已有相机权限，打开相机");
                openCamera();
            }
        });

        // 初始化模型和标签
        initModelAndLabels();
        
        Log.d(TAG, "IdentifyFragment: onCreateView() 完成");
        return view;
    }

    private void setupModelSpinner() {
        Log.d(TAG, "开始设置模型选择器");
        // 准备模型列表（文件名和大小）
        modelPathMap.clear();
        List<String> modelNames = new ArrayList<>();

        // 添加新模型
        modelPathMap.put("MobileNetV1.tflite", "models/MobileNetV1.tflite");
        modelNames.add("MobileNetV1.tflite");
        
        modelPathMap.put("MobileNetV2.tflite", "models/MobileNetV2.tflite");
        modelNames.add("MobileNetV2.tflite");
        
        modelPathMap.put("ResNet101.tflite", "models/ResNet101.tflite");
        modelNames.add("ResNet101.tflite");
        
        modelPathMap.put("EfficientNetB0.tflite", "models/EfficientNetB0.tflite");
        modelNames.add("EfficientNetB0.tflite");
        
        // 初始化各模型配置
        setupModelConfigs();
        
        // 创建简单的ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), 
                android.R.layout.simple_spinner_item, 
                modelNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // 设置适配器
        modelSpinner.setAdapter(adapter);
        
        // 设置默认选择为MobileNetV1
        for (int i = 0; i < modelNames.size(); i++) {
            if (modelNames.get(i).startsWith("MobileNetV1")) {
                modelSpinner.setSelection(i);
                break;
            }
        }
        
        // 设置选择监听器
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String displayName = modelNames.get(position);
                String newModelPath = modelPathMap.get(displayName);
                
                // 仅当选择了不同的模型时才重新加载
                if (newModelPath != null && !currentModelPath.equals(newModelPath)) {
                    currentModelPath = newModelPath;
                    try {
                        loadModel();
                        // 重新启用按钮
                        selectButton.setEnabled(true);
                        cameraButton.setEnabled(true);
                        emptyResultText.setText(R.string.please_select_image);
                        
                        Toast.makeText(requireContext(), 
                                String.format(getString(R.string.model_switched), displayName), 
                                Toast.LENGTH_SHORT).show();
                        
                        // 如果当前有图像，重新识别
                        if (selectedImageView.getDrawable() != null && 
                                selectedImageView.getVisibility() == View.VISIBLE) {
                            BitmapDrawable drawable = (BitmapDrawable) selectedImageView.getDrawable();
                            if (drawable != null) {
                                classifyImage(drawable.getBitmap());
                            }
                        }
                    } catch (IOException e) {
                        // 出错时更新UI状态
                        Toast.makeText(requireContext(), 
                                "模型加载失败: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                        
                        emptyResultText.setText("模型 " + displayName + " 加载失败，请选择其他模型");
                        emptyResultText.setVisibility(View.VISIBLE);
                        resultRecyclerView.setVisibility(View.GONE);
                        
                        // 禁用按钮，防止用户使用未加载的模型
                        selectButton.setEnabled(false);
                        cameraButton.setEnabled(false);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不做任何操作
            }
        });
        Log.d(TAG, "模型选择器设置完成，可选模型数量: " + modelNames.size());
    }
    
    private void setupModelConfigs() {
        // 根据TensorFlow官方预处理方法设置参数
        
        // MobileNet 使用 [-1,1] 归一化
        // x = (x - 127.5) / 127.5 = (x/255 - 0.5) / 0.5
        float[] mobilenetMean = {0.5f, 0.5f, 0.5f};
        float[] mobilenetStd = {0.5f, 0.5f, 0.5f};
        
        // MobileNetV2 也使用 [-1,1] 归一化
        // 与MobileNetV1相同: x = (x - 127.5) / 127.5
        float[] mobilenetv2Mean = {0.5f, 0.5f, 0.5f};
        float[] mobilenetv2Std = {0.5f, 0.5f, 0.5f};
        
        // EfficientNet 通常使用 [0,1] 归一化，无需额外处理
        float[] efficientnetMean = {0.0f, 0.0f, 0.0f};
        float[] efficientnetStd = {1.0f, 1.0f, 1.0f};
        
        // ResNet 使用 Caffe 预处理: BGR通道顺序, 减去均值 [103.939, 116.779, 123.68]
        // 注意：Android中图像是RGB格式，需要逆序为BGR
        // 均值换算为[0,1]范围: [103.939/255, 116.779/255, 123.68/255]
        float[] resnetMean = {0.406f, 0.456f, 0.485f}; // BGR顺序(R=123.68, G=116.779, B=103.939)/255
        float[] resnetStd = {1.0f, 1.0f, 1.0f}; // ResNet不进行标准化除法
        
        // 为每个模型设置正确的配置
        modelConfigMap.put("MobileNetV1.tflite", new ModelConfig(mobilenetMean, mobilenetStd, false));
        modelConfigMap.put("MobileNetV2.tflite", new ModelConfig(mobilenetv2Mean, mobilenetv2Std, false));
        modelConfigMap.put("EfficientNetB0.tflite", new ModelConfig(efficientnetMean, efficientnetStd, false));
        modelConfigMap.put("ResNet101.tflite", new ModelConfig(resnetMean, resnetStd, true)); // ResNet使用BGR
        
        Log.d(TAG, "已初始化" + modelConfigMap.size() + "个模型配置，与训练时使用的预处理方法匹配");
    }
    
    private void initModelAndLabels() {
        Log.d(TAG, "开始初始化模型和标签");
        try {
            loadModel();
            labels = loadLabels();
            Log.d(TAG, "模型和标签加载成功, 标签数量: " + labels.size());
            // 加载成功，可以启用识别功能
            selectButton.setEnabled(true);
            cameraButton.setEnabled(true);
        } catch (IOException e) {
            Log.e(TAG, "模型或标签加载失败: " + e.getMessage());
            // 更详细的错误信息显示
            Toast.makeText(requireContext(), 
                    "模型或标签文件加载失败: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
            emptyResultText.setText("模型加载错误，请选择其他模型");
            emptyResultText.setVisibility(View.VISIBLE);
            selectButton.setEnabled(false);
            cameraButton.setEnabled(false);
        }
    }

    private void loadModel() throws IOException {
        Log.d(TAG, "开始加载模型: " + currentModelPath);
        // 如果之前已经有解释器，先关闭它
        if (tfliteInterpreter != null) {
            tfliteInterpreter.close();
        }
        
        try {
            MappedByteBuffer modelBuffer = loadModelFile(currentModelPath);
            
            // 创建解释器选项
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2); // 设置合理的线程数
            
            // 使用选项创建解释器
            Log.d(TAG, "创建TensorFlow Lite解释器");
            tfliteInterpreter = new Interpreter(modelBuffer, options);
            
            // 获取模型信息
            Log.d(TAG, "模型输入张量数量: " + tfliteInterpreter.getInputTensorCount());
            Log.d(TAG, "模型输出张量数量: " + tfliteInterpreter.getOutputTensorCount());
            
            // 获取张量形状信息
            int[] inputShape = tfliteInterpreter.getInputTensor(0).shape();
            int[] outputShape = tfliteInterpreter.getOutputTensor(0).shape();
            Log.d(TAG, "模型输入形状: " + Arrays.toString(inputShape) + 
                  ", 输出形状: " + Arrays.toString(outputShape));
            
            Log.d(TAG, "模型加载成功: " + currentModelPath);
        } catch (Throwable t) {
            Log.e(TAG, "加载模型失败 (严重错误): " + t.getClass().getName() + ": " + t.getMessage(), t);
            Toast.makeText(requireContext(), 
                    "加载模型失败: " + currentModelPath + "，" + t.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            throw new IOException("加载模型失败: " + t.getMessage(), t);
        }
    }

    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        Log.d(TAG, "开始从资源文件加载模型: " + modelPath);
        try {
            AssetFileDescriptor fileDescriptor = requireContext().getAssets().openFd(modelPath);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            
            if (DEBUG_MODE) {
                Log.d(TAG, "模型文件大小: " + declaredLength + " 字节");
            }
            
            Log.d(TAG, "模型文件加载成功: " + modelPath);
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        } catch (IOException e) {
            Log.e(TAG, "模型文件加载失败: " + modelPath + ", " + e.getMessage());
            Toast.makeText(requireContext(), 
                    "模型文件不存在或无法打开: " + modelPath, 
                    Toast.LENGTH_SHORT).show();
            throw e;
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        return loadModelFile(currentModelPath);
    }

    private List<String> loadLabels() throws IOException {
        Log.d(TAG, "开始加载标签文件");
        List<String> labels = new ArrayList<>();
        try {
            InputStream inputStream = requireContext().getAssets().open("labels.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
            reader.close();
            Log.d(TAG, "标签文件加载成功, 标签数量: " + labels.size());
            return labels;
        } catch (IOException e) {
            Log.e(TAG, "标签文件加载失败: " + e.getMessage());
            Toast.makeText(requireContext(), 
                    "标签文件加载失败: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            throw e;
        }
    }

    private void classifyImage(Bitmap bitmap) {
        Log.d(TAG, "开始识别图像, 尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        if (tfliteInterpreter == null) {
            Log.e(TAG, "模型未加载，无法进行识别");
            Toast.makeText(requireContext(), "模型未加载，请重新选择模型", Toast.LENGTH_SHORT).show();
            emptyResultText.setText("模型未加载，请重新选择模型");
            emptyResultText.setVisibility(View.VISIBLE);
            resultRecyclerView.setVisibility(View.GONE);
            return;
        }
        
        if (labels == null || labels.isEmpty()) {
            Toast.makeText(requireContext(), "标签文件未加载，无法识别", Toast.LENGTH_SHORT).show();
            emptyResultText.setText("标签文件未加载，无法识别");
            emptyResultText.setVisibility(View.VISIBLE);
            resultRecyclerView.setVisibility(View.GONE);
            return;
        }

        try {
            if (DEBUG_MODE) {
                Log.d(TAG, "当前使用模型: " + getCurrentModelName());
            }
            
            Log.d(TAG, "调整图像尺寸为 " + IMAGE_LENGTH + "x" + IMAGE_WIDTH);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_LENGTH, IMAGE_WIDTH, true);
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(resizedBitmap);
            
            // 获取模型输入输出形状
            int[] inputShape = tfliteInterpreter.getInputTensor(0).shape();
            int[] outputShape = tfliteInterpreter.getOutputTensor(0).shape();
            Log.d(TAG, "模型输入形状: " + Arrays.toString(inputShape) + 
                  ", 输出形状: " + Arrays.toString(outputShape));
            
            if (DEBUG_MODE) {
                Log.d(TAG, "模型输入数据类型: " + tfliteInterpreter.getInputTensor(0).dataType());
                Log.d(TAG, "模型输出数据类型: " + tfliteInterpreter.getOutputTensor(0).dataType());
            }
            
            // 执行推理
            float[][] output = new float[1][SPECIES_NUM];
            
            // 记录推理开始时间
            long inferenceStartTime = System.currentTimeMillis();
            tfliteInterpreter.run(byteBuffer, output);
            // 记录推理耗时
            long inferenceTime = System.currentTimeMillis() - inferenceStartTime;
            Log.d(TAG, "模型推理耗时: " + inferenceTime + "ms");
            
            // 获取分类结果
            List<ResultAdapter.IdentifyResult> results = getSortedResults(output[0]);
            updateUI(results);
            
        } catch (Exception e) {
            Log.e(TAG, "图像识别过程出错: " + e.getMessage(), e);
            Toast.makeText(requireContext(), 
                    "图像识别过程出错: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            emptyResultText.setText("识别失败: " + e.getMessage());
            emptyResultText.setVisibility(View.VISIBLE);
            resultRecyclerView.setVisibility(View.GONE);
        }
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        Log.d(TAG, "开始将图像转换为模型输入格式");
        
        // 对于浮点模型 (FLOAT32)，使用标准预处理
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_LENGTH * IMAGE_WIDTH * 3);
        byteBuffer.order(java.nio.ByteOrder.nativeOrder());
        
        int[] intValues = new int[IMAGE_LENGTH * IMAGE_WIDTH];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        
        // 获取当前模型的配置 - 仅用于日志输出
        String currentModel = getCurrentModelName();
        
        if (DEBUG_MODE) {
            // 输出前10个像素值用于调试
            StringBuilder pixelSample = new StringBuilder("前10个像素原始RGB值: ");
            for (int i = 0; i < 10; i++) {
                int val = intValues[i];
                int r = (val >> 16) & 0xFF;
                int g = (val >> 8) & 0xFF;
                int b = val & 0xFF;
                pixelSample.append(String.format("[%d,%d,%d] ", r, g, b));
            }
            Log.d(TAG, pixelSample.toString());
        }
        
        // 不再进行归一化处理，直接将像素值转换为浮点数并存入ByteBuffer
        // MobileNetV1的预处理已经集成在模型中
        int pixel = 0;
        for (int i = 0; i < IMAGE_LENGTH; ++i) {
            for (int j = 0; j < IMAGE_WIDTH; ++j) {
                final int val = intValues[pixel++];
                // 直接输入RGB通道值，范围0-255
                float r = (val >> 16) & 0xFF;
                float g = (val >> 8) & 0xFF;
                float b = val & 0xFF;

                // RGB顺序
                byteBuffer.putFloat(r);
                byteBuffer.putFloat(g);
                byteBuffer.putFloat(b);
            }
        }
        
        if (DEBUG_MODE) {
            // 输出处理后的前5个像素值，每个像素3个通道
            byteBuffer.rewind();
            StringBuilder processedPixels = new StringBuilder("处理后的前5个像素值: ");
            for (int i = 0; i < 5; i++) {
                processedPixels.append("[");
                for (int c = 0; c < 3; c++) {
                    processedPixels.append(byteBuffer.getFloat()).append(", ");
                }
                processedPixels.append("] ");
            }
            byteBuffer.rewind(); // 重置位置以便TFLite使用
            Log.d(TAG, processedPixels.toString());
        }
        
        byteBuffer.rewind();
        Log.d(TAG, "图像转换完成，未应用归一化（由模型内部处理）");
        return byteBuffer;
    }
    
    // 辅助方法：获取当前选择的模型名称
    private String getCurrentModelName() {
        String modelPath = this.currentModelPath;
        for (Map.Entry<String, String> entry : modelPathMap.entrySet()) {
            if (entry.getValue().equals(modelPath)) {
                return entry.getKey();
            }
        }
        // 如果找不到对应的名称，返回路径的最后部分
        return modelPath.substring(modelPath.lastIndexOf("/") + 1);
    }

    private List<ResultAdapter.IdentifyResult> getSortedResults(float[] probabilities) {
        Log.d(TAG, "开始对识别结果排序，原始输出值: " + Arrays.toString(probabilities));
        
        // 检查输出是否已经是概率分布（总和接近1）
        float sum = 0.0f;
        for (float prob : probabilities) {
            sum += prob;
        }
        
        float[] processedProbs;
        if (Math.abs(sum - 1.0f) < 0.1f) {
            // 如果总和接近1，输出可能已经是概率分布
            Log.d(TAG, "模型输出似乎已经是概率分布 (sum=" + sum + ")，跳过softmax");
            processedProbs = probabilities;
        } else {
            // 否则应用softmax函数归一化
            Log.d(TAG, "模型输出不是概率分布 (sum=" + sum + ")，应用softmax");
            processedProbs = applySoftmax(probabilities);
        }
        
        Log.d(TAG, "处理后的概率: " + Arrays.toString(processedProbs));
        
        // 筛选掉低概率的结果
        float probabilityThreshold = 0.00f; // 只保留概率>1%的结果
        
        List<ResultAdapter.IdentifyResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(labels.size(), processedProbs.length); i++) {
            if (processedProbs[i] >= probabilityThreshold) {
                results.add(new ResultAdapter.IdentifyResult(labels.get(i), processedProbs[i]));
            }
        }
        
        // 降序排序（概率从高到低）
        Collections.sort(results, (o1, o2) -> -Float.compare(o1.getProbability(), o2.getProbability()));
        
        // 记录前三个识别结果
        StringBuilder topResults = new StringBuilder("前三识别结果: ");
        for (int i = 0; i < Math.min(3, results.size()); i++) {
            ResultAdapter.IdentifyResult result = results.get(i);
            topResults.append(result.getClassName())
                    .append(" (").append(String.format("%.2f%%", result.getProbability() * 100))
                    .append("), ");
        }
        Log.d(TAG, topResults.toString());
        
        return results;
    }
    
    /**
     * 应用softmax函数将原始输出转换为概率分布
     */
    private float[] applySoftmax(float[] modelOutput) {
        // 找最大值（防止数值溢出）
        float max = Float.NEGATIVE_INFINITY;
        for (float value : modelOutput) {
            max = Math.max(max, value);
        }
        
        // 计算exp并求和
        float sum = 0.0f;
        float[] result = new float[modelOutput.length];
        for (int i = 0; i < modelOutput.length; i++) {
            result[i] = (float) Math.exp(modelOutput[i] - max);
            sum += result[i];
        }
        
        // 归一化
        if (sum != 0) {
            for (int i = 0; i < result.length; i++) {
                result[i] /= sum;
            }
        }
        
        return result;
    }
    
    private void openCamera() {
        Log.d(TAG, "打开相机");
        Intent intent = new Intent(requireActivity(), CameraActivity.class);
        cameraLauncher.launch(intent);
    }

    // 将UI更新代码单独提取为一个方法
    private void updateUI(List<ResultAdapter.IdentifyResult> results) {
        Log.d(TAG, "识别结果数量: " + results.size());
        
        // 更新UI
        if (results.size() > 0) {
            Log.d(TAG, "第一个识别结果: " + results.get(0).getClassName() + 
                    ", 概率: " + (results.get(0).getProbability() * 100) + "%");
            emptyResultText.setVisibility(View.GONE);
            resultRecyclerView.setVisibility(View.VISIBLE);
            resultAdapter.setResults(results);
        } else {
            Log.e(TAG, "没有识别结果");
            emptyResultText.setText(R.string.recognition_error);
            emptyResultText.setVisibility(View.VISIBLE);
            resultRecyclerView.setVisibility(View.GONE);
        }
    }
} 