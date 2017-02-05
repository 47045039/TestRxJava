package com.liuqunshan.testrxjava;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.android.ActivityEvent;
import com.trello.rxlifecycle.android.RxLifecycleAndroid;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Subscription subscription;

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

    Subscriber<Object> subscriber = new Subscriber<Object>() {

        @Override
        public void setProducer(Producer p) {
            super.setProducer(p);
            Log.i(TAG, "subscriber @@@@ setProducer: " + p + " " + Thread.currentThread());
        }

        @Override
        public void onStart() {
            super.onStart();
            Log.i(TAG, "subscriber @@@@ onStart: " + Thread.currentThread());
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
        public void onNext(Object s) {
            Log.i(TAG, "subscriber @@@@ onNext: " + s + " " + Thread.currentThread());
        }
    };

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

//        testBase();
//        testOperatorAndScheduler();
//        testOtherOperator();

        testExtra();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    // 测试一些基本用法
    private void testBase() {
        // 同一个Observer可以subscribe多次，每次都将调用其onNext()和onCompleted()回调
        Observable.from(new String[]{"a1", "b1", "c1"}).subscribe(observer);
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

    // 测试各种各样的Operator和scheduler线程切换
    private void testOperatorAndScheduler() {
        // FIXME: subscribeOn()可以调用多次，会影响到之前的lift operation call线程
        // FIXME: observeOn()可以调用多次，会影响到之前的lift operation callback线程，进而影响到之后的lift operation call线程
        // FIXME: 一个operator上边用subscribeOn()指定了call线程，下边用observeOn()指定了callback线程时，以上边的subscribeOn()线程优先
        // FIXME: 一般情况下，只需要在整个Observable链式调用的末尾subscribe之前各调用一次subscribeOn()和observeOn()就可以了
        // FIXME: delay等部分operator会切换线程

        // 每隔1s产生一个Observable<Integer>
//        subscription = Observable.interval(1, TimeUnit.SECONDS)
        // 连续产生3个Observable
        subscription =  Observable.range(1, 3)

                // 展开成多个Observable并映射成另外多个Observable
                .flatMap(new Func1<Object, Observable<String>>() {
                    @Override
                    public Observable<String> call(Object s) {
                        Log.w(TAG, "flat map Observable.call(): " + s + " " + Thread.currentThread());
                        return Observable.from(new String[]{s + "_new1"/*, "_new2_" + s*/});
                    }
                })

                // FIXME: 上边的flatMap操作call将发生在新线程
                // Observable call方法（事件生产）线程，创建新线程
                .subscribeOn(Schedulers.newThread())

                // FIXME: 设置上边的flatMap操作callback将发生在Computation线程，因此下边的filter操作call都将发生在Computation线程
                // Observer回调方法（事件消费）线程，使用Computation线程
                .observeOn(Schedulers.computation())

                // 过滤部分Observable，返回false时被过滤掉
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        Log.w(TAG, "filter Observable.call(): " + s + " " + Thread.currentThread());
                        return s != null;
                    }
                })

                // FIXME: 设置上边的filter call发生在IO线程，但是上边的observeOn()已经指定了Computation线程，所以此处指定io线程不成功
                // Observable call方法（事件生产）线程，使用IO线程
                .subscribeOn(Schedulers.io())

                // FIXME: 设置上边的filter callback发生在Main线程，所以下边的map call发生在Main线程
                // Observer回调方法（事件消费）线程，使用MAIN线程
                .observeOn(AndroidSchedulers.from(getMainLooper()))

                // 映射Observable<String> => Observable<Integer>
                .map(new Func1<String, Integer>() {
                    @Override
                    public Integer call(String s) {
                        Log.w(TAG, "map Observable.call(): " + s + " "
                                + (int) s.charAt(0) + " " + Thread.currentThread());
                        return (int) s.charAt(0);
                    }
                })

                // FIXME: delay调用将导致上边的map callback发生在Computation线程，所以最终的subscriber callback也将转移到Computation线程
                // Observable call方法调用之后延时1s再到observer回调方法，默认是Computation线程
                .delay(1, TimeUnit.SECONDS/*, AndroidSchedulers.mainThread()*/)

                // 在subscriber的onStart()回调之后同线程调用，可以用来做界面初始化之类的工作
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        Log.w(TAG, "@@@@@@@@ do on subscribe: " + Thread.currentThread());
                    }
                })

                // 达成订阅
                .subscribe(observer);
    }

    // 测试各种各样的Operator
    private void testOtherOperator() {
        // 手动控制subscriber的回调
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                Log.w(TAG, "@@@@@@@@ create OnSubscribe: " + subscriber + " " + Thread.currentThread());
                subscriber.onNext("xxx");
                subscriber.onNext("yyy");
                subscriber.onNext("zzz");
                subscriber.onCompleted();
            }
        }).subscribe(subscriber);

        // 将多个Observable的回调结果统一处理
        // 任意一个Observable complete时，合并的Observable将complete
        // observer.onNext()回调 = 多个Observable中最少的onNext回调次数，回调结果将合并体现在call回调中
        Observable.zip(
                Observable.just(11, 22).delay(2, TimeUnit.SECONDS),
                Observable.just("_test_string_1", "_test_string_2", "_test_string_3"),
                new Func2<Integer, String, Object>() {
            @Override
            public Object call(Integer integer, String s) {
                Log.w(TAG, "@@@@@@@@ zip observable call: " + integer + " " + s + " " + Thread.currentThread());
                return integer + s;
            }
        }).subscribe(observer);

        // 延时创建Observable，避免了同步调用
        // Observable.just()代码在subscribe时才会执行
        Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                return Observable.just("12345");
            }
        }).subscribe(observer);

        // 只发送一个Observable的数据集，多个Observable中只有最先的那个可以发送数据
        Observable.amb(
                Observable.just(11, 22).delay(2, TimeUnit.SECONDS),
                Observable.just("_test_string_1", "_test_string_2", "_test_string_3")
        ).subscribe(observer);
    }

    private void testExtra() {
        Observable.just("_test_extra_").compose(RxLifecycleAndroid.bindActivity(Observable.defer(new Func0<Observable<ActivityEvent>>() {
            @Override
            public Observable<ActivityEvent> call() {
                return Observable.from(new ActivityEvent[]{
                        ActivityEvent.CREATE, ActivityEvent.START,
                        ActivityEvent.RESUME, ActivityEvent.PAUSE,
                        ActivityEvent.STOP, ActivityEvent.DESTROY
                });
            }
        }))).subscribe(new Action1<Object>() {

            @Override
            public void call(Object o) {
                Log.w(TAG, "@@@@@@@@ onNext call: " + o + " " + Thread.currentThread());
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Log.w(TAG, "@@@@@@@@ onError call: " + throwable + " " + Thread.currentThread());
            }
        }, new Action0() {
            @Override
            public void call() {
                Log.w(TAG, "@@@@@@@@ onComplete call: " + Thread.currentThread());
            }
        });
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
