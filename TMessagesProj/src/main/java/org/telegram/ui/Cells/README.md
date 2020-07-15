形如 XXXCell 类是用于 RecyclerView 每一个 Item 的 ViewHolder 的 View

```
@Override
public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = new XXXCell(context);
    return new RecyclerListView.Holder(view);
} 
```
