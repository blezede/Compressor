package com.blezede.demo;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.blezede.compressor.CompressListener;
import com.blezede.compressor.Compressor;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {
    static final int IMAGE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        findViewById(R.id.pick).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, IMAGE_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case IMAGE_REQUEST_CODE:

                if (resultCode == RESULT_OK) {
                    final Uri uri = data.getData();
                    Compressor.with(this).load(uri).maxWidthOrHeight(1024).launch(new CompressListener() {
                        @Override
                        public void onSuccess(String dest) {
                            Log.e("MainActivity", "onSuccess -->" + dest);
                        }

                        @Override
                        public void onFiled(String src) {
                            Log.e("MainActivity", "onFiled -->" + src);

                        }
                    });
                    new Thread(){
                        @Override
                        public void run() {
                            Compressor.with(MainActivity.this).load(uri).quality(100).ignoreBy(200 * 1024).get();
                        }
                    }.start();
                    break;
                }
        }
    }
}
