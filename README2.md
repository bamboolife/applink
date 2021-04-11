# Android中的红点提示怎么统一实现


![img](https:////upload-images.jianshu.io/upload_images/1621860-fc6cbdf822e39343.png?imageMogr2/auto-orient/strip|imageView2/2/w/532)

image.png

  App中的红点广泛用于提醒功能，虽然用在菜单上、Tab上、列表，但本质它就是一个红色的View，不就是放哪里就显示在哪里嘛，有什么难的？对！这是UI设计师和产品经理的一致观点，但是作为开发你可别信了他们的鬼话！
  这边文章讲红点，绝不是讲如何设计红点的UI，而是讲在代码层面如何实现，如何快速集成到业务中。如果你听了UI设计师和产品的鬼话你可能就真的哪里需要显示红点然后就在哪里放红点view了，然后通过外部代码控制它的隐藏和显示，你会很累，要定义很多key-value来存储记录各种红点显示的条件，零零碎碎很分散，这根本不是统一的解决方案，这是典型的想到哪里做到哪里。
  我的想法是创建一个红点View，你可以将此红点View放置在任何需要的View的右上方，然后初始化告知它显示的条件，即：接收哪几类通知提醒，比如告知它显示的条件是App有版本提醒就结束了，条件满足自动就会显示，条件不满足自动隐藏。

## 关于此红点View的接入方式，非常简单如下：



```java
// 红点提醒如果需要区分账户则需要设置账户，
// 比如：App升级提示无需区分账户，个人消息提醒需要区分账户
mRedDotView.setAccount(accountId);

// 设置当前红点监听的appLinks，只有收到了设置的AppLink才会触发当前红点的显示和隐藏
mRedDotView.setAppLinks("my-scheme://NewMsgAlert");
```

> 关于[AppLink](https://www.jianshu.com/p/fc6a44c298ea)之前一篇文章提到过，本质就是借助scheme定义的url解析规则，不同的AppLink对应不用的响应策略。

## 下面我们分析下红点显示的特性：

- 相似而不相同性：都是红点，只是不同位置的红点关注的提示内容不一样，所以更有必要定义统一规则来控制其显示和隐藏，因此我们使用AppLink来统一解析和管理，不同的红点View需要设置它所关注的AppLink，如上面接入方式所示，只要每个红点View在初始化时候指定想要关注的AppLinks即可，如果需要账户隔离则别忘记设置account：
- 持久性：如果不点击、不查看，它会一直显示，哪怕App被杀掉再打开。因此，红点触发的条件是存储下来的，常见办法是通过SharedPreference或Database，我采用了Database方式，因为可以便捷的通过SQL检索以及数据统一管理，当红点View加载后会在onAttachedToWindow()里根据注册的AppLinks查询是否有未读的AppLinks:



```java
public class PushMessageDbHelper extends SQLiteOpenHelper {
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS push_message (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "sub_title TEXT," +
                "pic_url TEXT," +
                "app_link TEXT," +
                "account TEXT," +
                "update_dt TEXT," +
                "read INTEGER)");
    }

    // ... others
}
```

> 数据库表结构定义，这是一个融合了AppLink、推送标题、子标题、推送通知图片、账户ID、是否已读等信息的表定义。



```java
@Override
protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    checkVisible();
    // ...other code
}
```

> 当view加载成功后查询之前有没有未读的AppLinks，如果有则设置红点显示。

- 即时性：红点是否要显示既可以是view初始化过程中查询数据库得知的，也可以是即时通知的，比如推送使得红点显示：



```java
public class RedDotView extends AppCompatImageView {
    private PushContentReceiver mContentReceiver = new PushContentReceiver() {

        @Override
        public String getAccount(@NonNull Context context) {
            return mAccount;
        }

        @Override
        public List<String> getAppLinks() {
            return mAppLinks;
        }

        @Override
        public boolean onReceive(@NonNull Context context, @NonNull AppLink appLink) {
            Log.d("DotView", "onReceive appLink: " + appLink);

            if (mAppLinks == null || mAppLinks.size() == 0) {
                return false;
            }

            boolean haveUnReadMsg = false;
            for (String item : mAppLinks) {
                if (PushMessageService.getInstance(getContext()).haveUnread(item, getAccount(context))) {
                    haveUnReadMsg = true;
                    break;
                }
            }

            setVisibility(haveUnReadMsg ? VISIBLE : GONE);
            return false;
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        PushContentReceiver.register(getContext(), mContentReceiver, false);
        checkVisible();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        PushContentReceiver.unregister(getContext(), mContentReceiver);
    }

    // ...others code
}
```

> 由上可见在View的onAttachedToWindow()里注册了一个监听AppLink的receiver。

需要注意的是这里的AppLink在定义的时候需要注明它是需要存储的，即overide shouldSave() 并返回true告知此AppLink收到后要存储在DB里，因为即便App杀掉再次打开还是需要显示红点的，如下案例：



```java
public class NewMsgAlert extends AppLink {

    @Override
    public boolean isPrivate() {
        return true;
    }

    @Override
    public boolean shouldSave() {
        return true;
    }
}
```

