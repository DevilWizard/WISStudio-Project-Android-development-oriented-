
- [RecyclerView](#recyclerview)
  - [使用前的准备](#使用前的准备)
    - [1.视图适配器Adapter](#1视图适配器adapter)
    - [2.子布局的编写](#2子布局的编写)
    - [3.RecyclerView的注册](#3recyclerview的注册)

## RecyclerView

RecyclerView是app中非常常用而且强大的一个组件，用它可以写出聊天窗口等等许多实用的功能。按官方的定义来说，就是一个 **“为大型数据集提供窗口的灵活视图”**

### 	使用前的准备

#### 	1.视图适配器Adapter

当使用RecyclerView时，通常需要显示很多item，这就需要一个数组适配器来存放这里item，这样才能实现“滚到哪看到哪”的效果。

- 编写item类

    item类就是适配器中存放的元素，也是整个RecyclerView显示的基础。举个例子，如果想做聊天窗口，那么item就对应Conversation类。RecyclerView显示的内容也就是一个个item类的成员变量。

    ```java
    class Item {
        //Tn表示Java数据类型、membern表示Item各属性
        private T1 member1;
        private T2 member2;
        /*
        	.
        	.
        	.
        */
        public Item(T1 member1, T2 member2/*, ...*/) {
            this.member1 = member1;
            this.member2 = member2;
            /*
        		.
        		.
        		.
            */
        }
        
        public Tn getMembern(){
            return membern;
        }
        
    }
    ```

    

- 编写ItemAdapter类

    只有编写了这个才能让RecyclerView发挥它全部的实力。在这个类里面，还需要有一个专门处理承载Item的子视图的ViewHolder类，以让Adpater知晓子视图中都有什么组件，方便对其进行操作。

    ```java
    import androidx.recyclerview.widget.RecyclerView;
    
    public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
        List<Item> itemList;  //用于存放Item对象的List接口引用
        
        private static class ViewHolder extends RecyclerView.ViewHolder {
            //  保存ViewHolder中子视图组件的引用，这样就能避免不必要的view调用(即        	    findVfindViewById())
            View view1;  
            View view2;
            /*
        		.
        		.
        		.
            */        
            
            /*
                只需要编写构造函数，itemView是RecyclerView布局中的子视图，构造时将其传入，
           	  这样就能通过itemView中子view的id，获得其实例  
            */
            public ViewHolder(View itemView) {
                super(view);
                viewn = (View) itemView.findVfindViewById(R.id.viewn);
            }
        }
        
        //初始化Item对象列表
        public ItemAdapter (List<Item> itemList) {
            this.itemList = itemList;
        }
        
        //然后需要重写Adapter的三个方法
        
        /*
        	此方法用于创建ViewHolder对象以显示在RecyclerView中
        	并且ViewHolder会不断被复用以提高性能
        	parent：子ViewHolder创建后被添加到的父布局
        */
        @Override
        public ViewHolder onCreateViewHolder
            (@NonNull ViewGroup parent, int viewType) {
            //直接通过inflate方法将layout构造成在RecyclerView中显示的子view对象
            View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.subView, parent, false);
            return new ViewHolder(view);
        }
    
        /*
        	此方法会在滚动时被调用，以实现边滚动边更新内容
       		holder：待更新的ViewHolder对象
       		position：该ViewHolder对象在Adapter内的位置
        */
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Item item = itemList.get(position);  //获取滚动所在位置的Item对象
            /*
            	这里根据需要对holder内的view组件进行UI更新等操作
            */
        }
    
        //返回Adapter内的元素个数
        @Override
        public int getItemCount() {
            return itemList.size();
        }
        
    }
    ```

    

#### 	2.子布局的编写

​		也就是subView.xml[^1]的布局，还拿上面的聊天窗口举例，就是每个对话气泡布局样式的编写。

#### 	3.RecyclerView的注册

​		这也是实现RecyclerView的最后一步——在使用RecyclerView的Activity中注册。

```java
List<Item> itemList = new ArrayList<>();  //实例化Item数组列表对象
RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
LayoutManager layoutManager = new LayoutManager(this);
recyclerView.setLayoutManager(layoutManager); //必备操作，为RecyclerView设定布局方式，按需传入不同的LayoutManager子对象
ItemAdapter itemAdapter = new ItemAdapter(itemList);
recyclerView.setAdapter(itemAdapter);  //必备操作，为RecyclerView设置数组适配器
```







































[^1]: R.layout.subView

