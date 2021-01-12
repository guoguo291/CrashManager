package com.guoj.crashmanager;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * crash处理类，协助应用发生异常后不再crash，记录crash信息
 *
 * @author guoj
 */
public class CrashManager {
    private static Context mContext;
    private final String TAG = "CrashManager";
    private boolean saveErrorInfo = true;
    private boolean blockANR= true;
    private CrashManager() {
    }

    private static class CrashManagerInstance {
        public static final CrashManager instance = new CrashManager();
    }

    public static CrashManager getInstance(@NotNull Context context) {
        //避免使用activity的context，防止内存泄露
        mContext = context.getApplicationContext();
        return CrashManagerInstance.instance;
    }

    public CrashManager init() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.i(TAG, "UncaughtException:" + e);
            e.printStackTrace();
            if (saveErrorInfo) {
                saveErrorToFile(e);
            }
            //处理主线程的crash，让应用不再无响应或异常退出
            if (blockANR){
                if (t == Looper.getMainLooper().getThread()) {
                    handleMainThread(e);
                }
            }
        });
        return this;
    }
    public CrashManager blockANR(boolean blockANR){
        this.blockANR=blockANR;
        return this;
    }
    public CrashManager saveErrorInfo(boolean saveError) {
        this.saveErrorInfo = saveError;
        return this;
    }

    private void handleMainThread(Throwable e) {
        while (true) {
            try {
                Looper.loop();
            } catch (Throwable e1) {
                Log.i(TAG, "UncaughtException:" + e1);
                e1.printStackTrace();
                if (saveErrorInfo) {
                    saveErrorToFile(e1);
                }
            }
        }
    }

    /**
     * 保存crash信息到文件
     *
     * @param e
     */
    private void saveErrorToFile(Throwable e) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        printWriter.close();
        String errorInfo = writer.toString();
        try {
            writer.close();
            //创建本地文件
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String time = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    //LocalDateTime是不可变的，线程安全的，建议替换Date
                    LocalDateTime localDateTime = LocalDateTime.now();
                    time = localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
                } else {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                    time = dateFormat.format(new Date());
                }
                String fileName = "crashErrorInfo" + time + ".txt";
                File cacheDir = mContext.getCacheDir();
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                File cacheFile = new File(cacheDir.getAbsolutePath(), fileName);
                if (!cacheFile.exists()) {
                    cacheFile.createNewFile();
                }
                FileOutputStream filterOutputStream = new FileOutputStream(cacheFile);
                filterOutputStream.write(errorInfo.getBytes());
                filterOutputStream.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


}
