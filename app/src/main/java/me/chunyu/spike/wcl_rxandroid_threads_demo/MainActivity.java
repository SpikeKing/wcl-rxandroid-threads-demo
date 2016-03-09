package me.chunyu.spike.wcl_rxandroid_threads_demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "DEBUG-WCL: " + MainActivity.class.getSimpleName();
    private static final int MIN = 1;
    private static final int MAX = 50;
    private static final Random rand = new Random();

    @Bind(R.id.main_tv_compute_value) TextView mTvComputeValue; // 计算线程时间
    @Bind(R.id.main_tv_custom_value) TextView mTvCustomValue; // 定制线程时间

    private Long mComputeStart; // 计算线程起始时间
    private Long mComputeEnd; // 计算线程结束时间
    private Long mCustomStart; // 定制线程起始时间
    private Long mCustomEnd; // 定制线程结束时间

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    // 计算线程并行
    public void computePara(View view) {
        mTvComputeValue.setText("计算中");
        Observable.range(MIN, MAX)
                .flatMap(i -> Observable.just(i)
                        .subscribeOn(Schedulers.computation())
                        .map(this::intenseCalculation)
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::computeTag);
    }

    // 定制线程并行
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

    // 耗时计算
    private int intenseCalculation(int i) {
        try {
            tag("Calculating " + i +
                    " on " + Thread.currentThread().getName());
            Thread.sleep(randInt(1000, 5000));
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
        if (i == MIN) {
            mComputeStart = System.currentTimeMillis();
        } else if (i == MAX) {
            mComputeEnd = System.currentTimeMillis();
            mTvComputeValue.setText(String.valueOf((mComputeEnd - mComputeStart) + "毫秒"));
        }

        tag(String.valueOf(i));
    }

    // 定制线程的标签
    private void customTag(int i) {
        if (i == MIN) {
            mCustomStart = System.currentTimeMillis();
        } else if (i == MAX) {
            mCustomEnd = System.currentTimeMillis();
            mTvCustomValue.setText(String.valueOf((mCustomEnd - mCustomStart) + "毫秒"));
        }

        tag(String.valueOf(i));
    }
}
