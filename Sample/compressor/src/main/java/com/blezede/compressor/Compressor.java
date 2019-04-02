package com.blezede.compressor;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * com.blezede.compressor
 * Time: 2019/3/27 9:36
 * Description:
 */
public class Compressor {

    private static final String CACHE_DIR = "compressor";
    private static final float INITIAL_VALUE = -1;
    private static final float DEFAULT_IGNORE_SIZE = 100 * 1024;
    private static final int DEFAULT_QUALITY = 60;
    private Bitmap.CompressFormat mCompressFormat = Bitmap.CompressFormat.JPEG;
    private float mLeastCompressSize = DEFAULT_IGNORE_SIZE; //default 100KB
    private float mMaxCompressSize = INITIAL_VALUE;
    private String mTargetDir;
    private float mMaxWidthOrHeight = INITIAL_VALUE;
    private List<InputStreamProvider> mInputStreamProviders;
    private static final int MSG_COMPRESS_SUCCESS = 1;
    private static final int MSG_COMPRESS_FAILED = 2;
    private CompressListener mCompressListener;
    private int mQuality = DEFAULT_QUALITY;

    private Compressor() {

    }

    private Compressor(Builder builder) {
        this.mTargetDir = builder.targetDir;
        this.mCompressFormat = builder.compressFormat;
        this.mLeastCompressSize = builder.leastCompressSize;
        this.mMaxCompressSize = builder.maxCompressSize;
        this.mMaxWidthOrHeight = builder.maxWidthOrHeight;
        this.mInputStreamProviders = builder.streamProviders;
        this.mQuality = builder.quality;
    }

    public static Builder with(Context c) {
        if (c == null) {
            throw new IllegalArgumentException("illegal argument error:context can not be null");
        }
        return new Builder(c);
    }

    private void launch() {
        if (mInputStreamProviders == null || mInputStreamProviders.size() <= 0) {
            throw new IllegalArgumentException("IllegalArgumentException : no source image found here");
        }
        launch(null);
    }


    private void launch(CompressListener listener) {
        this.mCompressListener = listener;
        Iterator<InputStreamProvider> iterator = mInputStreamProviders.iterator();

        while (iterator.hasNext()) {
            final InputStreamProvider provider = iterator.next();

            AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    String result = Engine.compress(provider, mTargetDir, mCompressFormat, mLeastCompressSize, mMaxWidthOrHeight, mMaxCompressSize, mQuality);
                    if (TextUtils.isEmpty(result)) {
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_FAILED, provider.getPath()));
                    } else if (new File(result).exists()) {
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_SUCCESS, result));
                    }
                }
            });

            iterator.remove();
        }
    }

    public List<String> get() {
        List<String> results = new ArrayList<>();
        Iterator<InputStreamProvider> iterator = mInputStreamProviders.iterator();

        while (iterator.hasNext()) {
            String result = Engine.compress(iterator.next(), mTargetDir, mCompressFormat, mLeastCompressSize, mMaxWidthOrHeight, mMaxCompressSize, mQuality);
            if (!TextUtils.isEmpty(result)) {
                results.add(result);
            }
            iterator.remove();
        }
        return results;
    }

    public static class Builder {

        private Context context;
        private Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
        private float leastCompressSize = DEFAULT_IGNORE_SIZE; //default 100KB
        private float maxCompressSize = INITIAL_VALUE;
        private String targetDir;
        private float maxWidthOrHeight = INITIAL_VALUE;
        private List<InputStreamProvider> streamProviders = new ArrayList<>();
        private int quality = DEFAULT_QUALITY;

        private Builder(Context c) {
            this.context = c;
            if (c.getExternalCacheDir() != null) {
                this.targetDir = c.getExternalCacheDir().getAbsolutePath() + File.separator + CACHE_DIR;
            } else {
                if (c.getCacheDir() != null) {
                    this.targetDir = c.getCacheDir().getAbsolutePath() + File.separator + CACHE_DIR;
                }
            }
        }

        public Builder targetDirPath(String targetDir) {
            this.targetDir = targetDir;
            return this;
        }

        public Builder ignoreBy(float size) {
            if (size > 0)
                this.leastCompressSize = size;
            return this;
        }

        public Builder compressFormat(Bitmap.CompressFormat format) {
            if (format != compressFormat) {
                this.compressFormat = format;
            }
            return this;
        }

        /**
         * Limit the size of the File.
         */
        public Builder maxFileSize(float size) {
            if (size > 0)
                this.maxCompressSize = size;
            return this;
        }

        /**
         * Limit image quality.
         */
        public Builder quality(int quality) {
            if (quality > 0)
                this.quality = quality;
            return this;
        }

        public Builder maxWidthOrHeight(float maxWidthOrHeight) {
            if (maxWidthOrHeight > 0) this.maxWidthOrHeight = maxWidthOrHeight;
            return this;
        }

        public <T> Builder load(List<T> list) {
            if (list == null)
                throw new IllegalArgumentException("illegal argument error:source list can not be null");
            streamProviders.clear();
            for (T src : list) {
                if (src instanceof String) {
                    load((String) src);
                } else if (src instanceof Uri) {
                    load((Uri) src);
                } else if (src instanceof File) {
                    load((File) src);
                } else if (src instanceof InputStreamProvider) {
                    load((InputStreamProvider) src);
                } else {
                    throw new IllegalArgumentException("illegal argument error:it must be String, Uri, File or InputStreamProvider");
                }
            }
            return this;
        }

        public Builder load(InputStreamProvider provider) {
            if (provider == null) return this;
            streamProviders.add(provider);
            return this;
        }

        public Builder load(final File file) {
            if (!file.exists()) {
                return this;
            }
            streamProviders.add(new InputStreamProvider() {
                @Override
                public InputStream open() throws IOException {
                    return new FileInputStream(file);
                }

                @Override
                public String getPath() {
                    return file.getAbsolutePath();
                }
            });
            return this;
        }

        public Builder load(final Uri uri) {
            if (uri == null) return this;
            streamProviders.add(new InputStreamProvider() {
                @Override
                public InputStream open() throws IOException {
                    return context.getContentResolver().openInputStream(uri);
                }

                @Override
                public String getPath() {
                    return Common.getFilePath(context, uri);
                }
            });
            return this;
        }

        public Builder load(final String src) {
            if (!new File(src).exists()) {
                return this;
            }
            streamProviders.add(new InputStreamProvider() {
                @Override
                public InputStream open() throws IOException {
                    return new FileInputStream(src);
                }

                @Override
                public String getPath() {
                    return src;
                }
            });
            return this;
        }

        /**
         * start asynchronous compress thread
         */
        public void launch() {
            if (streamProviders.size() <= 0) return;
            new Compressor(this).launch();
        }

        /**
         * start asynchronous compress thread
         */
        public void launch(CompressListener listener) {
            if (streamProviders.size() <= 0) return;
            new Compressor(this).launch(listener);
        }

        /**
         * start compress and return the file
         */
        public List<String> get() {
            if (streamProviders.size() <= 0) return new ArrayList<>();
            return new Compressor(this).get();
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_COMPRESS_SUCCESS:
                    if (msg.obj instanceof String) {
                        String result = (String) msg.obj;
                        if (mCompressListener != null) {
                            mCompressListener.onSuccess(result);
                        }
                    }
                    break;
                case MSG_COMPRESS_FAILED:
                    if (msg.obj instanceof String) {
                        String result = (String) msg.obj;
                        if (mCompressListener != null) {
                            mCompressListener.onFiled(result);
                        }
                    }
                    break;
            }

        }
    };

}
