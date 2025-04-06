package com.example.mushroom;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Mushroom implements Serializable {
    private static final long serialVersionUID = 1L; // 添加序列化版本UID

    @SerializedName("Name")
    private String name;

    @SerializedName("Description")
    private String description;

    @SerializedName("Feature")
    private String feature;

    @SerializedName("ImageUrls")
    private List<String> imageUrls;

    public Mushroom(String name, String description, String feature, List<String> imageUrls) {
        this.name = name;
        this.description = description;
        this.feature = feature;
        this.imageUrls = imageUrls;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getFeature() {
        return feature;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }
}