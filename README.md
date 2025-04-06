# 蘑菇识别应用

## 项目介绍

蘑菇识别应用是一款基于Android平台的移动应用程序，目的在帮助用户识别各种蘑菇品种。该应用使用机器学习技术（TensorFlow Lite）对蘑菇图像进行分析和识别，并提供详细的蘑菇信息，包括名称、特征、描述等。应用采用了现代化的底部导航栏设计，使用户可以方便地在不同功能之间切换。

## 主要功能

1. **蘑菇图鉴浏览**：用户可以浏览应用内置的蘑菇图鉴，了解各种蘑菇的详细信息。
2. **图像识别**：用户可以通过相机拍照或从相册中选择蘑菇照片，应用会自动分析并识别蘑菇种类。
3. **识别结果列表**：识别结果以列表形式展示，按可能性排序，用户可点击查看详情。
4. **详细信息展示**：提供蘑菇的详细描述，包括名称、特征、描述和多张参考图片。

## 技术架构

### 主要组件

- **MainContainerActivity**：应用的主容器Activity，管理底部导航栏和Fragment切换。
- **HomeFragment**：展示蘑菇列表的Fragment。
- **IdentifyFragment**：处理图像选择和识别功能的Fragment。
- **CameraActivity**：使用CameraX实现的相机功能，用于拍摄蘑菇照片。
- **MushroomDetailActivity**：展示单个蘑菇的详细信息。
- **MushroomAdapter**：负责蘑菇列表的适配器。
- **ResultAdapter**：负责识别结果列表的适配器。
- **MushroomLoader**：加载蘑菇数据的工具类。
- **Mushroom**：蘑菇数据模型类。

### 现代化技术特性

- **ActivityResultAPI**：使用现代的Activity结果注册系统，替代旧的startActivityForResult。
- **MediaStore API**：利用Android 10的现代媒体存储API访问图片。
- **Scoped Storage**：遵循Android 10引入的范围存储原则，提高用户隐私保护。
- **CameraX**：使用Jetpack的CameraX库，提供更稳定的相机体验。
- **Lambda表达式**：大量使用Java Lambda表达式，简化代码。

### 导航架构

应用使用底部导航栏实现主要功能的切换：
- 蘑菇图鉴（HomeFragment）
- 识别功能（IdentifyFragment）

### 数据存储

- 蘑菇数据存储在`assets/mushrooms.json`文件中，包含各种蘑菇的详细信息。
- 图像存储在`assets/images/`目录下，按蘑菇名称分类。

### 机器学习模型

- 使用TensorFlow Lite实现蘑菇识别功能。
- 模型文件存储在`assets/model.tflite`中。
- 标签信息存储在`assets/labels.txt`中。

### 相机功能

- 使用CameraX库实现实时相机预览和拍照功能。
- 支持后置相机拍摄高质量照片用于识别。
- 实现了图像旋转和处理，确保识别准确性。
- 支持长按控制闪光灯功能，方便在暗处拍摄。

## 蘑菇数据

应用包含大量蘑菇数据，覆盖了常见的食用菌和野生蘑菇，例如：

- 羊肚菌、牛肝菌、鸡油菌
- 鸡枞菌、青头菌、奶浆菌
- 干巴菌、虎掌菌、猪肚菌
- 黑木耳、银耳、金耳、猴头菇
- 香菇、平菇、金针菇等栽培菌
- 以及各种野生蘑菇和珍稀菌种

每种蘑菇都提供了详细的描述、特征和多张参考图片，帮助用户准确识别。

## 使用方法

1. **浏览蘑菇图鉴**：
   - 打开应用，默认显示蘑菇图鉴页面
   - 点击任意蘑菇可查看详细信息

2. **识别蘑菇**：
   - 点击底部导航栏的"识别"选项
   - 点击"拍照"按钮使用相机拍摄蘑菇照片，或点击"选择图片"按钮从相册选择照片
   - 在相机界面长按可切换闪光灯
   - 应用将自动分析图片并显示识别结果列表
   - 点击列表中的任意结果可查看该种蘑菇的详细信息

## 注意事项

- 应用的识别结果仅供参考，不应作为食用野生蘑菇的唯一依据。
- 野生蘑菇中有许多有毒品种，请勿在未经专业人士确认的情况下食用野生蘑菇。
- 请确保应用有访问相机的权限，以便拍摄图片进行识别。

## 开发环境

- Android Studio
- Gradle 版本: 7.0+
- 最低支持 Android API 级别: 29 (Android 10.0)
- 推荐设备: Android 10.0 或更高版本

## 依赖库

- androidx.appcompat:appcompat
- androidx.recyclerview:recyclerview
- com.google.android.material:material
- com.google.code.gson:gson
- org.tensorflow:tensorflow-lite
- androidx.camera:camera-core:1.3.1
- androidx.camera:camera-camera2:1.3.1
- androidx.camera:camera-lifecycle:1.3.1
- androidx.camera:camera-view:1.3.1 