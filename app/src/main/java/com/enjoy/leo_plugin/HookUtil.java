package com.enjoy.leo_plugin;

import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class HookUtil {

    private static final String TARGET_INTENT = "target_intent";

    /*
     *（1）通过动态代理技术替换了ActivityManager的IActivityManagerSingleton中的mInstance字段，
     * 这实质上改变了系统服务ActivityManager的实例。这意味着，当应用程序或系统尝试通过ActivityManager启动活动时（即调用startActivity方法），
     * 实际上会通过这个被替换的代理实例进行。由于这个代理实例是通过Proxy.newProxyInstance方法创建的，
     * 并且在创建时指定了一个InvocationHandler，因此每当通过代理实例调用方法时，都会触发这个InvocationHandler的invoke方法。
     *（2）动态代理通过在运行时创建代理对象，并将所有对代理对象的方法调用转发到InvocationHandler的invoke方法上。
     * 这意味着，无论何时有代码调用代理对象上的任何方法，InvocationHandler的invoke方法都会被触发。
     *（3）这个过程发生在startActivity方法的外部逻辑中，也就是说，在实际的startActivity内部逻辑执行之前。
     * InvocationHandler的invoke方法首先被调用，然后在这个invoke方法中，
     * 有机会对传入的参数进行修改、记录日志、进行安全检查或执行其他自定义逻辑。
     * 也可以决定是否要继续向下调用原始的startActivity方法（通过反射调用method.invoke(mInstance, args)），
     * 或者完全替换startActivity的行为。这也是为什么没有影响系统的流程。
     *（4）动态代理的核心在于它允许你在运行时创建一个实现了一组给定接口的新对象（代理对象）。
     * 这个代理对象可以拦截到对任何接口方法的调用，然后将这些调用转发到一个InvocationHandler实例
     * 动态代理的核心在于它允许你在运行时创建一个实现了一组给定接口的新对象（代理对象）。
     * 这个代理对象可以拦截到对任何接口方法的调用，然后将这些调用转发到一个InvocationHandler实例。
     * 这意味着，当通过代理对象调用任何方法时，实际上是在调用InvocationHandler的invoke方法。
     * 在这个invoke方法里，你有机会在调用实际方法之前执行自定义逻辑。
     * 使用动态代理拦截startActivity方法的调用时，
     * 实际上首先执行的是通过动态代理设置的InvocationHandler中的invoke方法逻辑。
     * invoke方法中的逻辑会在任何实际的startActivity逻辑执行之前运行。
     */
    /**
     * 第一阶段， 替换我们的Activity为ProxyActivity
     */
    public static void hookAMS() {
        try {
            // 获取 singleton 对象
            Field singletonField;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // 小于8.0
                Class<?> clazz = Class.forName("android.app.ActivityManagerNative");
                singletonField = clazz.getDeclaredField("gDefault");
            } else {
                Class<?> clazz = Class.forName("android.app.ActivityManager");
                singletonField = clazz.getDeclaredField("IActivityManagerSingleton");
            }
            singletonField.setAccessible(true);
            // 拿到系统的IActivityManagerSingleton单例对象，并在后边替换他，用来执行我们自己的流程。
            Object singleton = singletonField.get(null);
            // 获取 系统的 IActivityManager 对象
            Class<?> singletonClass = Class.forName("android.util.Singleton");
            Field mInstanceField = singletonClass.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);
            // 获取mInstance，这个时候还没有替换，这个是系统原有的mInstance, 我们就是不想改变原有执行流程。这个值必须保存下来。
            final Object mInstance = mInstanceField.get(singleton);
            // 获取动态代理的接口，我们代理的就是mInstance对象。
            Class<?> iActivityManager = Class.forName("android.app.IActivityManager");

            // 创建动态代理对象,用来替换系统的IActivityManagerSingleton，用动态代理才能实现我们修改intent参数的效果。
            Object proxyInstance = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    // 这里为什么是一个数组？因为一个接口可能有多个实现类。
                    new Class[]{iActivityManager}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args/*通过他来获取intent*/) throws Throwable {
                            // do something
                            // Intent的修改 -- 过滤
                            /**
                             * IActivityManager类的方法
                             * startActivity(whoThread, who.getBasePackageName(), intent,
                             *                         intent.resolveTypeIfNeeded(who.getContentResolver()),
                             *                         token, target != null ? target.mEmbeddedID : null,
                             *                         requestCode, 0, null, options)
                             *                         method表示的是方法对应的对象。
                             */
                            // 过滤
                            if ("startActivity".equals(method.getName())) {
                                int index = -1;

                                for (int i = 0; i < args.length; i++) {
                                    if (args[i] instanceof Intent) {
                                        index = i;
                                        break;
                                    }
                                }
                                // 启动插件的
                                Intent intent = (Intent) args[index];
                                Intent proxyIntent = new Intent();
                                proxyIntent.setClassName("com.enjoy.leo_plugin",
                                        "com.enjoy.leo_plugin.ProxyActivity");
                                proxyIntent.putExtra(TARGET_INTENT, intent);
                                args[index] = proxyIntent;
                            }

                            // args  method需要的参数  --- 不改变原有的执行流程
                            // mInstance 系统的 IActivityManager 对象，这样就不会影响原有的系统流程了，此时的intent内部的activity参数已经被修改了。这里会走startactivity的真实流程。
                            return method.invoke(mInstance, args);
                        }
                    });

            // ActivityManager.getService() 替换成 proxyInstance，这样就能不影响系统的执行流程了。这样当系统走到
            // int result = ActivityTaskManager.getService().startActivity(whoThread,
            //                    who.getOpPackageName(), who.getAttributionTag(), intent,
            //                    intent.resolveTypeIfNeeded(who.getContentResolver()), token,
            //                    target != null ? target.mEmbeddedID : null, requestCode, 0, null, options);
            //                    ActivityTaskManager.getService()实际上使用的是我们替换后的单例了。调用startActivity走的是我们的invoke方法。
            // ActivityManager.getService() 替换成 proxyInstance，只有通过代理对象，
            // 这样我们才能真正的执行proxyInstance.startActivity()，所以我们需要进行替换。否则上边的逻辑根本不会触发。
            mInstanceField.set(singleton, proxyInstance);
            // 我们不能这样调用，因为我们需要通过startActivity(intent);一步一步的走到hook点，
            // 也就是ActivityTaskManager.getService().startActivity，之后才去进行intent的替换，因此一定不能自己执行
            // proxyInstance.startActivity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 第二阶段，将proxyactivity替换回来为我们的目标Activity。
     */
    public static void hookHandler() {
        try {
            // 获取 ActivityThread 类的 Class 对象
            Class<?> clazz = Class.forName("android.app.ActivityThread");

            // 获取 ActivityThread 对象
            Field activityThreadField = clazz.getDeclaredField("sCurrentActivityThread");
            activityThreadField.setAccessible(true);
            Object activityThread = activityThreadField.get(null);

            // 获取 mH 对象
            Field mHField = clazz.getDeclaredField("mH");
            mHField.setAccessible(true);
            final Handler mH = (Handler) mHField.get(activityThread);

            Field mCallbackField = Handler.class.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);

            // 创建的 callback
            Handler.Callback callback = new Handler.Callback() {

                @Override
                public boolean handleMessage(@NonNull Message msg) {
                    // 通过msg  可以拿到 Intent，可以换回执行插件的Intent
                    // 找到 Intent的方便替换的地方  --- 在这个类里面 ActivityClientRecord --- Intent intent 非静态
                    // msg.obj == ActivityClientRecord
                    switch (msg.what) {
                        case 100:
                            try {
                                Field intentField = msg.obj.getClass()/*ActivityClientRecord*/.getDeclaredField("intent");
                                intentField.setAccessible(true);
                                // 这个是启动代理的Intent
                                Intent proxyIntent = (Intent) intentField.get(msg.obj);
                                // 启动插件的 Intent
                                Intent intent = proxyIntent.getParcelableExtra(TARGET_INTENT);
                                if (intent != null) {
                                    intentField.set(msg.obj, intent);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case 159:
                            try {
                                // 获取 mActivityCallbacks 对象
                                Field mActivityCallbacksField = msg.obj.getClass().getDeclaredField("mActivityCallbacks");
                                mActivityCallbacksField.setAccessible(true);
                                List mActivityCallbacks = (List) mActivityCallbacksField.get(msg.obj);

                                for (int i = 0; i < mActivityCallbacks.size(); i++) {
                                    if (mActivityCallbacks.get(i).getClass().getName().equals("android.app.servertransaction.LaunchActivityItem")) {
                                        Object launchActivityItem = mActivityCallbacks.get(i);
                                        // 获取启动代理的 Intent
                                        Field mIntentField = launchActivityItem.getClass().getDeclaredField("mIntent");
                                        mIntentField.setAccessible(true);
                                        Intent proxyIntent = (Intent) mIntentField.get(launchActivityItem);
                                        // 目标 intent 替换 proxyIntent
                                        Intent intent = proxyIntent.getParcelableExtra(TARGET_INTENT);
                                        if (intent != null) {
                                            mIntentField.set(launchActivityItem, intent);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                    // 必须 return false，否则会影响原有流程。
                    return false;
                }
            };
            // 替换系统的 callBack
            mCallbackField.set(mH, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * 我们要替换的代理就是系统的mInstance
 * public abstract class Singleton<T> {
 *
 *     @UnsupportedAppUsage
 *     public Singleton() {
 *     }
 *
 *     @UnsupportedAppUsage
 *     private T mInstance;
 *
 *     protected abstract T create();
 *
 *     @UnsupportedAppUsage
 *     public final T get() {
 *         synchronized (this) {
 *             if (mInstance == null) {
 *                 mInstance = create();
 *             }
 *             return mInstance;
 *         }
 *     }
 * }
 */
