package com.example.mushroom;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "aabb";
    
    private PreviewView viewFinder;
    private Button captureButton;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "CameraActivity: onCreate() 开始");
        setContentView(R.layout.activity_camera);
        
        viewFinder = findViewById(R.id.viewFinder);
        captureButton = findViewById(R.id.image_capture_button);
        
        // 设置拍照按钮点击事件
        captureButton.setOnClickListener(v -> takePhoto());
        
        // 设置相机执行器
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // 启动相机
        startCamera();
        Log.d(TAG, "CameraActivity: onCreate() 完成");
    }
    
    private void startCamera() {
        Log.d(TAG, "开始启动相机");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(this);
                
        cameraProviderFuture.addListener(() -> {
            try {
                // 获取相机提供者实例
                Log.d(TAG, "正在获取相机提供者");
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                // 创建预览用例
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
                Log.d(TAG, "相机预览设置完成");
                
                // 设置图像捕获配置 - 简化配置
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .build();
                Log.d(TAG, "相机捕获配置设置完成");
                
                // 选择后置相机
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                
                // 需要先解绑现有用例
                cameraProvider.unbindAll();
                Log.d(TAG, "已解绑之前的相机用例");
                
                try {
                    // 绑定用例到相机
                    Camera camera = cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageCapture);
                    Log.d(TAG, "相机启动成功");
                } catch (Exception e) {
                    Log.e(TAG, "相机绑定失败: " + e.getMessage(), e);
                    Toast.makeText(this, R.string.camera_error + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
                
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "相机启动失败: " + e.getMessage(), e);
                Toast.makeText(this, R.string.camera_error, Toast.LENGTH_SHORT).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void takePhoto() {
        Log.d(TAG, "开始拍照");
        if (imageCapture == null) {
            Log.e(TAG, "imageCapture为空，无法拍照");
            Toast.makeText(this, R.string.camera_error, Toast.LENGTH_SHORT).show();
            return;
        }
        
        captureButton.setEnabled(false);
        Log.d(TAG, "已禁用拍照按钮");
        
        imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        Log.d(TAG, "照片拍摄成功，开始处理图像");
                        // 简化处理：直接将图像数据保存到文件
                        try {
                            // 获取图像数据
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);
                            Log.d(TAG, "获取图像数据完成，大小: " + bytes.length + " 字节");
                            
                            // 关闭图像代理
                            image.close();
                            
                            // 创建临时文件
                            File cacheDir = getCacheDir();
                            String fileName = "photo_" + UUID.randomUUID().toString() + ".jpg";
                            File photoFile = new File(cacheDir, fileName);
                            Log.d(TAG, "创建临时文件路径: " + photoFile.getAbsolutePath());
                            
                            // 保存JPEG数据到文件
                            FileOutputStream fos = new FileOutputStream(photoFile);
                            
                            // 调整图像旋转方向
                            int rotation = image.getImageInfo().getRotationDegrees();
                            Log.d(TAG, "图像旋转角度: " + rotation);
                            if (rotation != 0) {
                                Log.d(TAG, "需要旋转图像");
                                // 旋转图像需要使用bitmap来处理
                                // 但要节省内存使用，所以使用适当的缩小尺寸
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                // 减小图像尺寸以防止OOM
                                options.inSampleSize = 2;
                                
                                Log.d(TAG, "开始解码图像数据");
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                                if (bitmap == null) {
                                    Log.e(TAG, "图像解码失败");
                                    throw new IOException("图像解码失败");
                                }
                                Log.d(TAG, "图像解码成功，尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                                
                                Matrix matrix = new Matrix();
                                matrix.postRotate(rotation);
                                Bitmap rotatedBitmap = Bitmap.createBitmap(
                                        bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                
                                // 回收原始bitmap
                                if (bitmap != rotatedBitmap) {
                                    bitmap.recycle();
                                    Log.d(TAG, "已回收原始bitmap");
                                }
                                
                                // 转换为JPEG并保存到文件
                                Log.d(TAG, "开始将旋转后的图像压缩并保存到文件");
                                boolean compressResult = rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                                if (!compressResult) {
                                    Log.e(TAG, "图像压缩保存失败");
                                }
                                rotatedBitmap.recycle();
                                Log.d(TAG, "已回收旋转后的bitmap");
                            } else {
                                // 无需旋转，直接保存原始JPEG数据
                                Log.d(TAG, "无需旋转，直接写入JPEG数据");
                                fos.write(bytes);
                            }
                            
                            fos.close();
                            Log.d(TAG, "文件保存完成: " + photoFile.length() + " 字节");
                            
                            // 创建返回意图并返回文件路径
                            Intent resultIntent = new Intent();
                            // 只传递文件路径，不传递大数据
                            resultIntent.putExtra("photo_path", photoFile.getAbsolutePath());
                            setResult(RESULT_OK, resultIntent);
                            Log.d(TAG, "返回照片路径: " + photoFile.getAbsolutePath());
                            
                            // 结束活动
                            Log.d(TAG, "照片处理完成，结束相机活动");
                            finish();
                            
                        } catch (Exception e) {
                            Log.e(TAG, "照片处理失败: " + e.getMessage(), e);
                            onError(new ImageCaptureException(-1, "照片处理失败: " + e.getMessage(), e));
                        } finally {
                            // 确保按钮重新启用
                            captureButton.setEnabled(true);
                            Log.d(TAG, "已重新启用拍照按钮");
                        }
                    }
                    
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "照片拍摄失败: " + exception.getMessage(), exception);
                        Toast.makeText(CameraActivity.this, 
                                getString(R.string.camera_error) + ": " + exception.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        captureButton.setEnabled(true);
                        Log.d(TAG, "拍照失败后重新启用按钮");
                    }
                }
        );
    }
    
    @Override
    protected void onDestroy() {
        Log.d(TAG, "CameraActivity: onDestroy()");
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            Log.d(TAG, "相机执行器已关闭");
        }
    }
} 