- [recyclerview闪烁问题](#recyclerview闪烁问题)
	- [notifyItemInserted()](#notifyiteminserted)
	- [notifyItemInserted()](#notifyiteminserted-1)
	- [notifyDataSetChanged()](#notifydatasetchanged)
### recyclerview闪烁问题

第一周有写过关于RecyclerView的笔记，不过比较基础，而今天突然发现每次上拉加载更多图片的一瞬间都会导致屏幕有闪烁，很不舒服，因此趁这个机会来细细研究RecyclerView的相关操作。

#### notifyItemInserted()

开始的时候，每次加载一张图片都会用notifyItemInserted()把其插入RecyclerView中。

下面是其源码

```java
/**
* Notify any registered observers that the item reflected at <code>position</code>
* has been newly inserted. The item previously at <code>position</code> is now at
* position <code>position + 1</code>.
*
* <p>This is a structural change event. Representations of other existing items in 
* the data set are still considered up to date and will not be rebound, though their
* positions may be altered.</p>
*
* @param position Position of the newly inserted item in the data set
*
* @see #notifyItemRangeInserted(int, int)
*/
public final void notifyItemInserted(int position) {
	mObservable.notifyItemRangeInserted(position, 1);
}
```

首先 *notifyItemInserted()* 的类型是 **结构变化事件** ，调用该函数并不会使Adapter里其他的数据被重新绑定。其次这个函数 **一次只能插入一个元素** ，若要一次插入多个元素，那就要用到另一个函数了。

参数 **position** 指的是当前要插入的数据的显示位置，举个例子，若position=0，则往RecyclerView的最前面插入，也就是在页面的最顶端插入；若position=size-1，则在页面最尾部插入。还要注意，调用此函数后整个RecyclerView的布局会根据插入的元素位置来移动其他元素，若从位置0插入，则后面的元素会逐一往后移，在用户看来会有明显的移动过程。所以元素插入也不能乱插，必须要有讲究，要考虑用户体验。

#### notifyItemInserted()

由于 *notifyItemInserted()* 一次只能插入一个元素，所以在某些情况需要插入大量数据的时候就不是很方便，比如刷新页面数据的情况。下面介绍另一个函数 **notifyItemRangeInserted()** ，那就直接上源码吧。

```java
/**
* Notify any registered observers that the currently reflected<code>itemCount</code>
* items starting at <code>positionStart</code> have been newly inserted. The items
* previously located at <code>positionStart</code> and beyond can now be found      * starting at position <code>positionStart + itemCount</code>.
*
* <p>This is a structural change event. Representations of other existing items in  * the data set are still considered up to date and will not be rebound, though their  * positions may be altered.</p>
*
* @param positionStart Position of the first item that was inserted
* @param itemCount Number of items inserted
*
* @see #notifyItemInserted(int)
*/
public final void notifyItemRangeInserted(int positionStart, int itemCount) {
            mObservable.notifyItemRangeInserted(positionStart, itemCount);
}
```

可以看到，与 *notifyItemInserted()* 相似， **notifyItemRangeInserted()** 也是结构更新，但从函数名就能看出来二者不同之处，range指范围，也就是它可以一次更新某个数量范围的数据。

来看看它的参数。第一个 **positionStart** ，和 *notifyItemInserted(int position)* 的 **position** 一个意思，就是要插入的数据集的插入位置，而 **itemCount** 告诉RecyclerView我一次要插入这么多的数据。举个例子，若positionStart=0，itemCount=8，调用 *notifyItemRangeInserted()* 则一次将8个数据插入到页面最顶端，直接把原本顶端的数据挤下去。

**一句话形象概括总结： *notifyItemInserted()* 是一个人插队， *notifyItemRangeInserted()* 是一群人插队，你想插哪里都行，当然你也可以乖乖有序排队**

#### notifyDataSetChanged()

后面去网上找资料才知道，我是调用了一个叫 **notifyDataSetChanged()** 的函数才导致的屏闪

```java
/**
* Notify any registered observers that the data set has changed.
*
* <p>There are two different classes of data change events, item changes and        * structural changes. Item changes are when a single item has its data updated but  * no positional changes have occurred. Structural changes are when items are        * inserted, removed or moved within the data set.</p>
*
* <p>This event does not specify what about the data set has changed, forcing
* any observers to assume that all existing items and structure may no longer be    * valid. LayoutManagers will be forced to fully rebind and relayout all visible     * views.</p>
*
* <p><code>RecyclerView</code> will attempt to synthesize visible structural change * events for adapters that report that they have {@link #hasStableIds() stable IDs} * when this method is used. This can help for the purposes of animation and visual
* object persistence but individual item views will still need to be rebound
* and relaid out.</p>
*
* <p>If you are writing an adapter it will always be more efficient to use the more
* specific change events if you can. Rely on <code>notifyDataSetChanged()</code>
* as a last resort.</p>
*
* @see #notifyItemChanged(int)
* @see #notifyItemInserted(int)
* @see #notifyItemRemoved(int)
* @see #notifyItemRangeChanged(int, int)
* @see #notifyItemRangeInserted(int, int)
* @see #notifyItemRangeRemoved(int, int)
*/
public final void notifyDataSetChanged() {
	mObservable.notifyChanged();
}
```

这个函数的注释能然我们更进一步地了解这背后的机制，下面来细看。

首先一共有两种数据变化的事件类型： **子项变化** 、和上面说到的 **结构变化**

*  *子项变化* 就是指当数据集中的 **数据本身需要更新** ，而它的显示位置不需要改变的情况 

*  *结构变化* 就是指当数据本身不需要更新但它需要变动位置时的情况

**而notifyDataSetChanged()并没有指明会发生哪一种的数据变化**

而调用notifyDataSetChanged()，会强制使RecyclerView认为同时发生了子项变化和结构变化，这就会导致整个RecyclerView再次重新绑定所有数据并重新布局当前可见的子视图。这就很可能会导致一些不稳定的现象发生。所以Google官方在注释里这样写道 **“使用那些有明确数据变化类型的函数会让你的Adapter的效率更高”** ，并且给出建议“rely on notifyDataSetChanged() as a last resort”，也就是不到万不得已不要使用notifyDataSetChanged()

