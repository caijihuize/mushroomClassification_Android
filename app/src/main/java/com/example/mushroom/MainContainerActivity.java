package com.example.mushroom;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainContainerActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    private Fragment homeFragment;
    private Fragment identifyFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        // 初始化底部导航栏
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // 初始化Fragment管理器
        fragmentManager = getSupportFragmentManager();
        
        // 创建Fragment实例
        homeFragment = new HomeFragment();
        identifyFragment = new IdentifyFragment();
        
        // 默认显示Home Fragment
        replaceFragment(homeFragment);
        
        // 设置底部导航栏点击事件
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_home) {
                replaceFragment(homeFragment);
                return true;
            } else if (itemId == R.id.navigation_identify) {
                replaceFragment(identifyFragment);
                return true;
            }
            
            return false;
        });
    }
    
    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment);
        transaction.commit();
    }
} 