细探内存优化

Android对应用所占的内存资源还是比较敏感的，搞不好就容易出现常见的内存泄漏、内存溢出等问题，因此任何一个app必须在内存优化上做大功夫来确保应用的稳定性。内存优化有很多种分类，有从Java代码本身语法方面的优化（如Java静态对象会比起实例对象占用较多内存）、从应用自身使用较为频繁的对象类型进行的优化，等等。因为这次做的是一个浏览图片的app，所以从图片本身占用资源来着重讲解如何优化内存。

由于手机内存有限的缘故，一个应用所占用的内存应该在合理的范围内，对于专门浏览图片的app更是如此，因为图片所占用的内存资源比任何其他东西都要大，举个例子，一张分辨率为2048x1536的图片会占用手机12MB的内存，而一张512x384的图片只占大约0.75MB内存，差距有多明显就一目了然了。 

其实Google官方已经有非常好的图片加载框架Glide、Picasso供给我们使用，但处于初学阶段，一定要知其然，所以我们来从底层学起。

#### 对图片进行合理的采样

在浏览的缩略图上显示原图显然不是一个合理的举措，因此我们必须要对其进行合理的采样。图片在Android中通常是通过Bitmap对象表示，而 `BitmapFactory.Options` 是一个描述bitmap属性的静态类，用它可以获取到原图的宽高等信息。但我们必须要有图片的资源才能读取，这就需要 `BitmapFactory`提供的解码方法来获取，常用的有 `BitmapFactory.decodeResource` 、`BitmapFactory.decodeFile`、`BitmapFactory.decodeStream`，然后在我们读取的时候传入Options就能获取到图片的所有信息了。**注意：必须通过将Options传入decode方法才能获取相关属性，否则options所有属性都是null**

但是一定要注意一点：默认情况下 `BitmapFactory` 的解码方法会占用部分内存，而如果我们只是需要图片的宽高这样解码会很划不来，所以有一个小技巧：`将Options的inJustDecodeBounds属性设置为true`，这样又能获取到需要的宽高信息又不会占用内存。不过也要注意这样设置后decode返回的bitmap是null，所以千万不要忘了将 `inJustDecodeBounds属性设回false`。具体操作看下图代码

```java
BitmapFactory.Options options = new BitmapFactory.Options();
options.inJustDecodeBounds = true;
BitmapFactory.decodeResource(getResources(), R.id.myimage, options);
int imageHeight = options.outHeight;
int imageWidth = options.outWidth;
String imageType = options.outMimeType;
```

而要真正的采样，需要用到  `inSampleSize` 。`inSampleSize = 1`时表示不采样，`inSampleSize = 2`则说明在宽高上分别采样`1/2`，也就是整个图像的1/4。通过传入需要显示的图片大小 `reqWidth、reqHeight`，可以计算出合适的 `inSampleSize`对图片进行采样，设置完 `Options.inSampleSize`属性后 最后再通过decode返回需要的图片大小即可

```java
public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
    //图片的原始宽高
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

        final int halfHeight = height / 2;
        final int halfWidth = width / 2;

        while ((halfHeight / inSampleSize) >= reqHeight
                && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2;
        }
    }

    return inSampleSize;
}
```

采样完后，图片本身占用的内存已经大大减少了，当然还不够，还得高效地**读图片**，也就是之前的三级缓存读取图片，这也是谷歌官方对于缓存bitmap的建议。除此之外，还有一个很好的技巧，利用 `Options.inBitmap`

#### 用 Options.inBitmap 储存图像以便后续的使用

什么是`inBitmap`？这是Google的解释： 如果设置了inBitmap属性，则采用Options对象的解码方法将在加载图像时尝试复用bitmap。也就是在含options的decode方法中，若`inBitmap`存在，则不会请求内存来解码图片，而是直接用`inBitmap`返回，也就是decode层面的缓存。这样在用decodeFile读取文件缓存时就可以节约读取时间并且节省内存。

那么什么时候设置 `inBitmap`呢？在内存缓存中，我们使用了LruCache，它会不断把那些访问频率较低的图片缓存清除出去，但这些被清除出去的bitmap并不代表着我们以后都不会访问，所以把这些清除出去的bitmap放入一个容器里，这样我们就能从中挑选合适的bitmap来设置 `inBitmap`，当然这只是一个辅助容器，所以不能让它也占用比LruCache还多的内存，所以选用软引用 `SoftReference` 来存放被清除出来的bitmap，因为垃圾回收器更倾向于回收 `SoftReference`指向的引用，所以选用它作为bitmap的引用载体不会长时间的占用内存，是个很好的选择

```java
Set<SoftReference<Bitmap>> reusableBitmaps;
private LruCache<String, BitmapDrawable> memoryCache;

reusableBitmaps = Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());

memoryCache = new LruCache<String, BitmapDrawable>(cacheParams.memCacheSize) {

    // Notify the removed entry that is no longer being cached.
    @Override
    protected void entryRemoved(boolean evicted, String key,
            BitmapDrawable oldValue, BitmapDrawable newValue) {
			reusableBitmaps.add(new SoftReference<Bitmap>(oldValue.getBitmap()));
    }
....
}
```

