package me.chunyu.spike.wcl_rxandroid_threads_demo;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DEBUG-WCL: " + MainActivity.class.getSimpleName();

    private static final int MIN = 1;
    private static final int MAX = 1000;
    
    private static final Random rand = new Random();

    @Bind(R.id.main_tv_compute_value) TextView mTvComputeValue; // 计算线程时间
    @Bind(R.id.main_tv_custom_value) TextView mTvCustomValue; // 定制线程时间
    @Bind(R.id.main_tv_grouped_value) TextView mTvGroupedValue; // 定制线程时间
    @Bind(R.id.main_tv_core_num) TextView mTvCoreNum; // 定制线程时间

    private long mComputeStart; // 计算线程起始时间
    private long mComputeEnd; // 计算线程结束时间
    private long mCustomStart; // 定制线程起始时间
    private long mCustomEnd; // 定制线程结束时间
    private long mGroupedStart; // 定制线程起始时间
    private long mGroupedEnd; // 定制线程结束时间

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mTvCoreNum.setText(String.valueOf(getNumberOfCores() + "核"));
    }

    // 计算线程并行, 8核
    public void computePara(View view) {
        mTvComputeValue.setText("计算中");
        Observable.range(MIN, MAX)
                .flatMap(i -> Observable.just(i)
                                .subscribeOn(Schedulers.computation()) // 使用Rx的计算线程
                                .map(this::intenseCalculation)
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::computeTag);
    }

    // 定制线程并行, 9核
    public void customPara(View view) {
        int threadCt = Runtime.getRuntime().availableProcessors() + 1;
        mTvCustomValue.setText(String.valueOf("计算中(" + threadCt + "线程)"));

        ExecutorService executor = Executors.newFixedThreadPool(threadCt);
        Scheduler scheduler = Schedulers.from(executor);

        Observable.range(MIN, MAX)
                .flatMap(i -> Observable.just(i)
                                .subscribeOn(scheduler)
                                .map(this::intenseCalculation)
                ).observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::customTag);
    }

    // 定制线程+组合并行, 9核
    public void groupedPara(View view) {
        int threadCt = Runtime.getRuntime().availableProcessors() + 1;
        mTvGroupedValue.setText(String.valueOf("计算中(" + threadCt + "线程)"));

        ExecutorService executor = Executors.newFixedThreadPool(threadCt);
        Scheduler scheduler = Schedulers.from(executor);

        final AtomicInteger batch = new AtomicInteger(0);

        Observable.range(MIN, MAX)
                .groupBy(i -> batch.getAndIncrement() % threadCt)
                .flatMap(g -> g.observeOn(scheduler).map(this::intenseCalculation))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::groupedTag);
    }

    // 模拟耗时计算
    private int intenseCalculation(int i) {
        try {
            tag("Calculating " + i + " on " + Thread.currentThread().getName());
            Thread.sleep(randInt(100, 500));
            return i;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 随机数
    public static int randInt(int min, int max) {
        return rand.nextInt((max - min) + 1) + min;
    }

    // 字符串标签
    private void tag(String s) {
        Log.e(TAG, s);
    }

    // 计算线程的标签
    private void computeTag(int i) {
        if (mComputeStart == 0L) {
            mComputeStart = System.currentTimeMillis();
        }
        mComputeEnd = System.currentTimeMillis();
        mTvComputeValue.setText(String.valueOf((mComputeEnd - mComputeStart) + "毫秒"));

        tag(String.valueOf(i));
    }

    // 定制线程的标签
    private void customTag(int i) {
        if (mCustomStart == 0L) {
            mCustomStart = System.currentTimeMillis();
        }
        mCustomEnd = System.currentTimeMillis();
        mTvCustomValue.setText(String.valueOf((mCustomEnd - mCustomStart) + "毫秒"));

        tag(String.valueOf(i));
    }

    // 定制+组合线程的标签
    private void groupedTag(int i) {
        if (mGroupedStart == 0L) {
            mGroupedStart = System.currentTimeMillis();
        }
        mGroupedEnd = System.currentTimeMillis();
        mTvGroupedValue.setText(String.valueOf((mGroupedEnd - mGroupedStart) + "毫秒"));

        tag(String.valueOf(i));
    }

    // 获取CPU的核数
    // 参考: http://stackoverflow.com/questions/30119604/how-to-get-the-number-of-cores-of-an-android-device
    private int getNumberOfCores() {
        if (Build.VERSION.SDK_INT >= 17) {
            return Runtime.getRuntime().availableProcessors();
        } else {
            // Use saurabh64's answer
            return getNumCoresOldPhones();
        }
    }

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     *
     * @return The number of cores, or 1 if failed to get result
     */
    private int getNumCoresOldPhones() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]+", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            //Default to return 1 core
            return 1;
        }
    }
}
