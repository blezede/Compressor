package com.blezede.compressor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.TextUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * com.blezede.compressor
 * Time: 2019/4/1 13:43
 * Description:
 */
public class Engine {

    private static final String DOT = ".";
    private static final String EMPTY_STRING = "";

    private static String prepare(String targetDir, Bitmap.CompressFormat compressFormat) {
        File target = new File(targetDir);
        boolean isMade = target.mkdirs();
        if (isMade) {
            return target.getAbsolutePath() + File.separator + System.currentTimeMillis() + DOT + compressFormat.name().toLowerCase();
        }
        return EMPTY_STRING;
    }

    public static String compress(InputStreamProvider source, String targetDirPath, Bitmap.CompressFormat compressFormat, float leastSize, float maxWidthOrHeight, float maxSize, int quality) {
        if (source == null || !new File(source.getPath()).exists() || TextUtils.isEmpty(targetDirPath) || compressFormat == null) {
            return EMPTY_STRING;
        }
        String destPath = prepare(targetDirPath, compressFormat);
        if (TextUtils.isEmpty(destPath)) {
            return EMPTY_STRING;
        }
        File src = new File(source.getPath());
        if (leastSize > 0 && src.length() <= leastSize) {
            return Common.copyFile(src.getAbsolutePath(), destPath);
        }
        Bitmap targetBitmap = null;
        FileOutputStream fileOps = null;
        ByteArrayOutputStream byteArrayOps = null;
        BufferedOutputStream bufferedOps = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(source.open(), null, options);
            int srcWidth = options.outWidth;
            int srcHeight = options.outHeight;
            if (maxWidthOrHeight > 0) {
                options.inSampleSize = calculateInSampleSize(options, (int) maxWidthOrHeight, (int) maxWidthOrHeight);
            } else {
                options.inSampleSize = computeSize(srcWidth, srcHeight);
            }
            if (compressFormat != Bitmap.CompressFormat.PNG) {
                options.inPreferredConfig = Bitmap.Config.RGB_565;
            }
            options.inJustDecodeBounds = false;
            targetBitmap = BitmapFactory.decodeStream(source.open(), null, options);
            if (targetBitmap == null) {
                return EMPTY_STRING;
            }
            float radio = 0;
            if (maxWidthOrHeight > 0 && (maxWidthOrHeight < targetBitmap.getHeight() || maxWidthOrHeight < targetBitmap.getWidth())) {
                int max = Math.max(targetBitmap.getHeight(), targetBitmap.getWidth());
                radio = (maxWidthOrHeight / (float) max);
            }
            int degree = Common.readImageDegree(src.getAbsolutePath());
            if (degree != 0 || radio != 0) {
                targetBitmap = rotatingOrScaleImage(targetBitmap, degree, radio);
            }
            byteArrayOps = new ByteArrayOutputStream();
            int imgQuality = quality;
            targetBitmap.compress(compressFormat, imgQuality, byteArrayOps);
            if (compressFormat != Bitmap.CompressFormat.PNG) {
                while (maxSize > 0 && byteArrayOps.toByteArray().length > maxSize && imgQuality >= 10) {
                    byteArrayOps.reset();
                    imgQuality -= 10;
                    targetBitmap.compress(compressFormat, imgQuality, byteArrayOps);
                }
            }
            fileOps = new FileOutputStream(destPath);
            bufferedOps = new BufferedOutputStream(fileOps);
            bufferedOps.write(byteArrayOps.toByteArray());
            bufferedOps.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return EMPTY_STRING;
        } catch (IOException e) {
            e.printStackTrace();
            return EMPTY_STRING;
        } finally {
            try {
                if (byteArrayOps != null) {
                    byteArrayOps.close();
                }
                if (fileOps != null) {
                    fileOps.close();
                }
                if (bufferedOps != null) {
                    bufferedOps.close();
                }
                if (targetBitmap != null && !targetBitmap.isRecycled()) {
                    targetBitmap.recycle();
                }
            } catch (IOException ignored) {
            }
        }

        return destPath;
    }

    private static int computeSize(int srcWidth, int srcHeight) {
        srcWidth = srcWidth % 2 == 1 ? srcWidth + 1 : srcWidth;
        srcHeight = srcHeight % 2 == 1 ? srcHeight + 1 : srcHeight;

        int longSide = Math.max(srcWidth, srcHeight);
        int shortSide = Math.min(srcWidth, srcHeight);

        float scale = ((float) shortSide / longSide);
        if (scale <= 1 && scale > 0.5625) {
            if (longSide < 1664) {
                return 1;
            } else if (longSide < 4990) {
                return 2;
            } else if (longSide > 4990 && longSide < 10240) {
                return 4;
            } else {
                return longSide / 1280;
            }
        } else if (scale <= 0.5625 && scale > 0.5) {
            return longSide / 1280 == 0 ? 1 : longSide / 1280;
        } else {
            return (int) Math.ceil(longSide / (1280.0 / scale)) + 1;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;

        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    private static Bitmap rotatingOrScaleImage(Bitmap bitmap, int angle, float radio) {
        if (angle == 0 && radio == 0) {
            return bitmap;
        }
        Matrix matrix = new Matrix();

        matrix.postRotate(angle);

        if (radio > 0)
            matrix.postScale(radio, radio);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
