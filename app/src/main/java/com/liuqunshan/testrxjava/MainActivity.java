package com.liuqunshan.testrxjava;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Producer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observers.Subscribers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        testBase();
    }

    // 测试一些基本用法
    private void testBase() {
        Observer<Object> observer = new Observer<Object>() {

            @Override
            public void onCompleted() {
                Log.i(TAG, "observer @@@@ onCompleted: " + Thread.currentThread());
            }

            @Override
            public void onError(Throwable throwable) {
                Log.i(TAG, "observer @@@@ onError: " + throwable + " " + Thread.currentThread());
            }

            @Override
            public void onNext(Object s) {
                Log.i(TAG, "observer @@@@ onNext: " + s + " " + Thread.currentThread());
            }
        };

        Subscriber<String> subscriber = new Subscriber<String>() {

            @Override
            public void onStart() {
                super.onStart();
                Log.i(TAG, "subscriber @@@@ onStart: " + Thread.currentThread());
            }

            @Override
            public void setProducer(Producer p) {
                super.setProducer(p);
                Log.i(TAG, "subscriber @@@@ setProducer: " + p + " " + Thread.currentThread());
            }

            @Override
            public void onCompleted() {
                Log.i(TAG, "subscriber @@@@ onCompleted: " + Thread.currentThread());
            }

            @Override
            public void onError(Throwable throwable) {
                Log.i(TAG, "subscriber @@@@ onError: " + throwable + " " + Thread.currentThread());
            }

            @Override
            public void onNext(String s) {
                Log.i(TAG, "subscriber @@@@ onNext: " + s + " " + Thread.currentThread());
            }
        };


        // 同一个Observer可以subscribe多次，每次都将调用其onNext()和onCompleted()回调
        Observable.from(new String[]{null, "a1", "b1", "c1", ""}).subscribe(observer);
        Observable.just("a2", "b2", "c2").subscribe(observer);

        // FIXME: 同一个Subscriber只能subscribe一次，
        // FIXME: 第一次将调用其onStart(),onNext()和onCompleted()回调，
        // FIXME: 第二次将只调用其onStart()回调
        // 因为第一次onCompleted()时，Subscriber的封装SafeSubscriber将他们共享的SubscriptionList
        // 已经unsubscribe了；而第二次subscribe时，Subscriber不变，其封装SafeSubscriber继续和
        // Subscriber共享同一个已经unsubscribe的SubscriptionList。
        Observable.from(new String[]{"d1", "e1", "f1"}).subscribe(subscriber);
        Observable.from(new String[]{"d2", "e2", "f2"}).subscribe(subscriber);

        Log.w(TAG, "subscribe: " + Thread.currentThread());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
