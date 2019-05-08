[![](https://jitpack.io/v/blezede/Compressor.svg)](https://jitpack.io/#blezede/Compressor)
# Compressor
Android 本地图片压缩工具

# How to use
Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.blezede:Compressor:1.2'
	}
  
Compress in the asynchronous thread:
```
  Compressor.with(this).load(uri).launch(new CompressListener() {
      @Override
      public void onSuccess(String dest) {
      }

      @Override
      public void onFiled(String src) {

      }
  });
```
Compress in the current thread:
```
  Compressor.with(context).load(uri).get();
```
