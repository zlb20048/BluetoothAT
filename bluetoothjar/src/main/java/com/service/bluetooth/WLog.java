/*
 * 文 件 名:  WLog.java
 * 版    权:  SmartAuto Co., Ltd. Copyright YYYY-YYYY,  All rights reserved
 * 描    述:  <描述>
 * 修 改 人:  zixiangliu
 * 修改时间:  2014-4-25
 * 跟踪单号:  <跟踪单号>
 * 修改单号:  <修改单号>
 * 修改内容:  <修改内容>
 */
package com.service.bluetooth;

/**
 * <日志工具类>
 * <日志工具类，对原先的Log进行相关的封装>
 *
 * @author zixiangliu
 * @version [版本号, 2014-4-25]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class WLog
{
    private static final int DUMP_LENGTH = 4000;

    public static final int LEVEL_VERBOSE = 0;

    public static final int LEVEL_DEBUG = 1;

    public static final int LEVEL_INFO = 2;

    public static final int LEVEL_WARNING = 3;

    public static final int

            LEVEL_ERROR = 4;

    public static final int LEVEL_NONE = 5;

    private static String mTag = "bt_service";

    private static String mRemoteUrl;

    private static int mLevel;

    public static String getMethodName()
    {
        StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
        return stacks[4].getMethodName();
    }

    private static String generateTag(StackTraceElement caller)
    {
        String tag = "%s.%s(Line:%d)"; // 占位符
        String callerClazzName = caller.getClassName(); // 获取到类名
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
        tag = String.format(tag, callerClazzName, caller.getMethodName(), caller.getLineNumber()); // 替换
        return tag;
    }

    public static String getNameAndLine(StackTraceElement[] stacks, String msg)
    {
        String clz = stacks[4].getFileName();
        int line = stacks[4].getLineNumber();
        StringBuffer sb = new StringBuffer();
        sb.append("[").append(clz).append("]").append("<").append(line).append(">").append(":").append(msg);
        return sb.toString();
    }

    public static void initialize(String tag, int level)
    {
        mLevel = level;
        mTag = tag;
    }

    public static int v(String msg)
    {
        if (mLevel <= LEVEL_VERBOSE)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.v(mTag, getNameAndLine(stacks, msg));
        }
        return 0;
    }

    public static int v(String tag, String msg)
    {
        if (mLevel <= LEVEL_VERBOSE)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.v(mTag, getNameAndLine(stacks, msg));
        }
        return 0;
    }

    public static int v(String msg, Throwable tr)
    {
        if (mLevel <= LEVEL_VERBOSE)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.v(mTag, getNameAndLine(stacks, msg), tr);
        }
        return 0;
    }

    public static int v(String tag, String msg, Throwable tr)
    {
        if (mLevel <= LEVEL_VERBOSE)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.v(mTag, getNameAndLine(stacks, msg), tr);
        }
        return 0;
    }

    public static int d(String msg)
    {
        if (mLevel <= LEVEL_DEBUG)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.d(mTag, getNameAndLine(stacks, msg));
        }
        return 0;
    }

    public static int d(String tag, String msg)
    {
        if (mLevel <= LEVEL_DEBUG)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.d(mTag, getNameAndLine(stacks, msg));
        }
        return 0;
    }

    public static int d(String msg, Throwable tr)
    {
        if (mLevel <= LEVEL_DEBUG)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.d(mTag, getNameAndLine(stacks, msg), tr);
        }
        return 0;
    }

    public static int d(String tag, String msg, Throwable tr)
    {
        if (mLevel <= LEVEL_DEBUG)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.d(mTag, getNameAndLine(stacks, msg), tr);
        }
        return 0;
    }

    public static int i(String msg)
    {
        if (mLevel <= LEVEL_INFO)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.i(mTag, getNameAndLine(stacks, msg));
        }
        return 0;
    }

    public static int i(String tag, String msg)
    {
        if (mLevel <= LEVEL_INFO)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.i(mTag, getNameAndLine(stacks, msg));
        }
        return 0;
    }

    public static int i(String msg, Throwable tr)
    {
        if (mLevel <= LEVEL_INFO)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.i(mTag, getNameAndLine(stacks, msg), tr);
        }
        return 0;
    }

    public static int i(String tag, String msg, Throwable tr)
    {
        if (mLevel <= LEVEL_INFO)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.i(mTag, getNameAndLine(stacks, msg), tr);
        }
        return 0;
    }

    public static int w(String msg)
    {
        if (mLevel <= LEVEL_WARNING)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.w(mTag, getNameAndLine(stacks, msg));
        }
        return 0;
    }

    public static int w(String tag, String msg)
    {
        if (mLevel <= LEVEL_WARNING)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.w(mTag, getNameAndLine(stacks, msg));
        }
        return 0;
    }

    public static int w(String msg, Throwable tr)
    {
        if (mLevel <= LEVEL_WARNING)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.w(mTag, getNameAndLine(stacks, msg), tr);
        }
        return 0;
    }

    public static int w(String tag, String msg, Throwable tr)
    {
        if (mLevel <= LEVEL_WARNING)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.w(mTag, getNameAndLine(stacks, msg), tr);
        }
        return 0;
    }

    public static int e(String msg)
    {
        if (mLevel <= LEVEL_ERROR)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.e(mTag, getNameAndLine(stacks, msg));
        }
        return 0;
    }

    public static int e(String tag, String msg)
    {
        if (mLevel <= LEVEL_ERROR)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.e(mTag, getNameAndLine(stacks, msg));
        }
        return 0;
    }

    public static int e(String msg, Throwable tr)
    {
        if (mLevel <= LEVEL_ERROR)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.e(mTag, getNameAndLine(stacks, msg), tr);
        }
        return 0;
    }

    public static int e(String tag, String msg, Throwable tr)
    {
        if (mLevel <= LEVEL_ERROR)
        {
            StackTraceElement[] stacks = Thread.getAllStackTraces().get(Thread.currentThread());
            return android.util.Log.e(mTag, getNameAndLine(stacks, msg), tr);
        }
        return 0;
    }

    public static int t(String msg, Object... args)
    {
        return android.util.Log.v("test", String.format(msg, args));
    }

    public static void remote(final String msg)
    {
        if (mRemoteUrl == null)
        {
            return;
        }

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // we can send the log to the remote service
                // try
                // {
                // DefaultHttpClient httpClient = new DefaultHttpClient();
                // HttpPost httpPost = new HttpPost(mRemoteUrl);
                //
                // List<NameValuePair> params = new ArrayList<NameValuePair>();
                // params.add(new BasicNameValuePair("package_name",
                // mPackageName));
                // params.add(new BasicNameValuePair("package_version",
                // mPackageVersion));
                // params.add(new BasicNameValuePair("phone_model",
                // Build.MODEL));
                // params.add(new BasicNameValuePair("sdk_version",
                // Build.VERSION.RELEASE));
                // params.add(new BasicNameValuePair("message", msg));
                //
                // httpPost.setEntity(new UrlEncodedFormEntity(params,
                // HTTP.UTF_8));
                // httpClient.execute(httpPost);
                // }
                // catch (Exception e)
                // {
                // }
            }
        }).start();
    }

    public static void dump(String longMsg)
    {
        dump(mTag, longMsg, LEVEL_INFO);
    }

    public static void dump(String longMsg, int level)
    {
        dump(mTag, longMsg, level);
    }

    public static void dump(String tag, String longMsg)
    {
        dump(mTag, "[" + tag + "]" + longMsg, LEVEL_INFO);
    }

    public static void dump(String tag, String longMsg, int level)
    {
        int len = longMsg.length();
        String curr;
        for (int a = 0; a < len; a += DUMP_LENGTH)
        {
            if (a + DUMP_LENGTH < len)
            {
                curr = longMsg.substring(a, a + DUMP_LENGTH);
            }
            else
            {
                curr = longMsg.substring(a);
            }

            switch (level)
            {
                case LEVEL_ERROR:
                    e(tag, curr);
                    break;
                case LEVEL_WARNING:
                    w(tag, curr);
                    break;
                case LEVEL_INFO:
                    i(tag, curr);
                    break;
                case LEVEL_DEBUG:
                    d(tag, curr);
                    break;
                case LEVEL_VERBOSE:
                default:
                    v(tag, curr);
                    break;
            }
        }
    }
}
