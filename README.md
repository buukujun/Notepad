# NotePad-Android应用的介绍文档
该项目在初始应用的功能之上，添加了时间戳和分类显示，增加了搜索和导出笔记功能，同时对记事本界面进行了一定的美化，是记事本的功能更加完善。
## 一、基本应用的功能
### 1、新建笔记
（1）在主界面点击红色矩形所示按钮，新建笔记并进入编辑界面

（2）进入笔记编辑界面后，可进行笔记标题和内容的输入以及笔记类别的选择
<img width="511" height="977" alt="img1" src="https://github.com/user-attachments/assets/602d5763-1a6d-4917-abc7-ca51ce736a62" />
（3）输入笔记信息后，在编辑界面点击红色矩形所示按钮，便可保存新建的笔记，新建的笔记会显示在主界面上
![图片3](images/img3.png)
![图片4](images/img4.png)
### 2、编辑笔记
（1）点击需要编辑的笔记，进入笔记编辑界面
![图片5](images/img5.png)
（2）修改完笔记内容后，点击保存即可完成笔记修改
![图片6](images/img6.png)
（3）如果想放弃修改笔记，点击红色矩形所示按钮即可放弃编辑返回主界面
![图片7](images/img7.png)
### 3、删除笔记
（1）在编辑界面，点击红色矩形所示按钮，跳出选项菜单
![图片8](images/img8.png)
（2）点击delete按键即可删除笔记
![图片9](images/img9.png)
## 二、扩展基本功能
### （一）.笔记条目增加时间戳，内容预览，类别显示和图标
#### 1、功能要求
每个新建笔记都会保存新建时间和类别，会显示部分内容用于预览，会根据类别不同显示不同的图标个背景色；在修改笔记后更新为修改时间
#### 2、实现思路和技术
(1)初始应用的笔记列表item只有一个标题，需要再添加其他组件用来显示时间等其他内容，布局使用 LinearLayout
```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
android:id="@+id/note_item_root"
android:layout_width="match_parent"
android:layout_height="wrap_content"
android:orientation="horizontal"
android:padding="12dp"
android:background="@android:color/white">

    <!-- 类别图标  -->
    <ImageView
        android:id="@+id/note_icon"
        android:layout_width="32dp"
        android:layout_height="32dp"
        android:layout_marginEnd="12dp"
        android:layout_marginRight="12dp"
        android:layout_marginTop="12dp"
        android:scaleType="fitCenter"
        android:adjustViewBounds="true"
        android:src="@drawable/flower"
        android:contentDescription="@string/note_category_icon" />

    <!-- 右侧内容区域 -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <!-- 标题和时间戳的水平布局 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/note_title"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textAppearance="?android:attr/textAppearanceMedium"
                android:textSize="16sp"
                android:textStyle="bold"
                android:singleLine="true"
                android:ellipsize="end"
                android:textColor="@android:color/black"/>


            <TextView
                android:id="@+id/note_time"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:paddingLeft="8dp"
                android:paddingRight="4dp"
                android:textAppearance="?android:attr/textAppearanceSmall"
                android:textColor="?android:attr/textColorSecondary"
                android:paddingStart="8dp"
                android:paddingEnd="4dp" />

        </LinearLayout>

        <!-- 分类和内容预览 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="4dp">

            <TextView
                android:id="@+id/note_category"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textAppearance="?android:attr/textAppearanceSmall"
                android:padding="4dp"
                android:background="@drawable/bg_category_tag"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/note_preview"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginLeft="8dp"
                android:layout_weight="1"
                android:layout_marginStart="8dp"
                android:textAppearance="?android:attr/textAppearanceSmall"
                android:textColor="?android:attr/textColorSecondary"
                android:maxLines="2"
                android:ellipsize="end" />

        </LinearLayout>

    </LinearLayout>

</LinearLayout>
```
(2)PROJECTION变量用来定义Java文件定义的Activity的数据，所以在PROJECTION中加入所需属性作为显示内容
```
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,//2 时间戳
            NotePad.Notes.COLUMN_NAME_NOTE,//3 内容预览
            NotePad.Notes.COLUMN_NAME_CATEGORY  // 4 类别列
    };
```
(3)笔记条目数据通过SimpleCursorAdapter装填，其中用到的dataColumns，viewIDs变量需要添加修改时间等
```
String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_NOTE,
                NotePad.Notes.COLUMN_NAME_CATEGORY,
                NotePad.Notes.COLUMN_NAME_CATEGORY,
                NotePad.Notes.COLUMN_NAME_CATEGORY,
        };

        int[] viewIDs = {
                R.id.note_title,
                R.id.note_time,
                R.id.note_preview,
                R.id.note_category,
                R.id.note_icon,
                R.id.note_item_root
        };

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,  // 使用单独的列表项布局
                cursor,
                dataColumns,
                viewIDs
        );
```
(4)设置自定义视图绑定器,对显示的各项内容进行处理
```
    // 设置自定义视图绑定器
    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
    if (view.getId() == R.id.note_time) {
    long timestamp = cursor.getLong(getColumnIndex(cursor, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE));
    TextView timeView = (TextView) view;
    timeView.setText(formatDate(timestamp));
    return true;
    } else if (view.getId() == R.id.note_preview) {
    String note = cursor.getString(getColumnIndex(cursor, NotePad.Notes.COLUMN_NAME_NOTE));
    TextView previewView = (TextView) view;
    if (note != null && note.length() > 50) {
    previewView.setText(note.substring(0, 50) + "...");
    } else {
    previewView.setText(note);
    }
    return true;
    }else if (view.getId() == R.id.note_category) {
    // 处理类别显示
    String category = cursor.getString(getColumnIndex(cursor, NotePad.Notes.COLUMN_NAME_CATEGORY));
    TextView categoryView = (TextView) view;
    categoryView.setText(category);

    // 根据类别设置不同颜色
    int colorRes = getCategoryColor(category);
    categoryView.setTextColor(getResources().getColor(colorRes));
    return true;
}else if (view.getId() == R.id.note_icon) {
    // 设置图片类别图标
    ImageView iconView = (ImageView) view;

    // 获取笔记的类别
    String category = cursor.getString(getColumnIndex(cursor, NotePad.Notes.COLUMN_NAME_CATEGORY));
    int categoryIndex = getCategoryIndex(category);

    // 优先使用自定义图片图标，如果不存在则使用系统图标
    if (categoryIndex >= 0 && categoryIndex < CATEGORY_ICON_RESOURCES.length) {
        int customIconRes = CATEGORY_ICON_RESOURCES[categoryIndex];

        // 检查自定义图标资源是否存在
        if (resourceExists(customIconRes)) {
            iconView.setImageResource(customIconRes);
        } else {
            // 使用系统图标作为备选
            int systemIconRes = SYSTEM_ICON_RESOURCES[categoryIndex];
            iconView.setImageResource(systemIconRes);
        }
    }
    else {
        // 默认图标
        iconView.setImageResource(R.drawable.glass);
    }

    // 设置图标颜色和透明度
    setupIconAppearance(iconView, category);

    String description = "类别：" + getCategoryDisplayName(category) + "，图标";
    iconView.setContentDescription(description);

    return true;
}else if (view.getId() == R.id.note_item_root) {
    // 设置不同类别的背景色
    String category = cursor.getString(getColumnIndex(cursor, NotePad.Notes.COLUMN_NAME_CATEGORY));
    LinearLayout rootLayout = (LinearLayout) view;

    // 根据类别设置背景色
    int backgroundColor = getBackgroundColorForCategory(category);
    rootLayout.setBackgroundColor(backgroundColor);

    return true;
}
return false;
}
});
```
#### 实现效果界面截图
(1)创建笔记时显示创建时间
![图片10](images/img10.png)
(2)修改笔记后显示的时间更新为最新修改的时间
![图片11](images/img11.png)
### .笔记查询功能（按标题查询）
#### 1、功能要求
点击搜索按钮，进行搜索界面。初始状态的搜索界面显示所有笔记条目。在输入搜索内容或回删一部分搜索内容后，系统根据输入内容和笔记的标题进行字符串匹配，刷新符合要求的笔记显示在笔记列表上，后续如果回删搜索内容至为空后，显示所有的笔记
#### 2、实现思路和技术实现
(1)在应用主界面添加一个搜索按钮。在list_options_menu.xml中添加一个搜索的item，使用Android自带的搜索图标
```
<item
    android:id="@+id/menu_search"
    android:icon="@android:drawable/ic_search_category_default"
    android:title="@string/menu_search"
    android:actionViewClass="android.widget.SearchView"
    android:showAsAction="ifRoom|collapseActionView"/>
```
(2)在安卓中有个用于搜索控件：'SearchView'，可以把'SearchView'跟'ListView'相结合，动态地显示搜索结果。首先实现搜索页面
```
<item
    android:id="@+id/menu_search"
    android:icon="@android:drawable/ic_search_category_default"
    android:title="@string/menu_search"
    android:actionViewClass="android.widget.SearchView"
    android:showAsAction="ifRoom|collapseActionView"/>
```
(3)修改NoteList.java中的 displayNotes函数，对显示的列表进行搜索筛选处理
```
String selection = null;
        String[] selectionArgs = null;

        // 构建查询条件：分类筛选 + 搜索筛选
        List<String> conditions = new ArrayList<>();
        List<String> args = new ArrayList<>();

        //分类筛选条件
        if (!"all".equals(mCurrentCategory)) {
            conditions.add(NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?");
            args.add(getCategoryConstant(mCurrentCategory));
        }

        // 如果有搜索过滤条件，添加到查询中
        if (!TextUtils.isEmpty(mCurrentFilter)) {
            conditions.add(NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?");
            args.add("%" + mCurrentFilter + "%");
        }

        // 组合所有条件
        if (!conditions.isEmpty()) {
            selection = TextUtils.join(" AND ", conditions);
            selectionArgs = args.toArray(new String[0]);
        }

        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );
        
        setListAdapter(adapter);
        // 更新搜索结果提示和空视图状态
        updateSearchResultInfo(cursor.getCount());
        
        /**
         * 更新搜索结果信息
         */
        private void updateSearchResultInfo(int count) {
            if (mSearchResultInfo != null) {
                if (!TextUtils.isEmpty(mCurrentFilter)) {
                    mSearchResultInfo.setText(getString(R.string.search_results_info, count, mCurrentFilter));
                    mSearchResultInfo.setVisibility(View.VISIBLE);
    
                    // 如果有搜索词但没有结果，显示空状态
                    if (count == 0) {
                        mEmptyView.setText(getString(R.string.no_search_results, mCurrentFilter));
                        mEmptyView.setVisibility(View.VISIBLE);
                    } else {
                        mEmptyView.setVisibility(View.GONE);
                    }
                } else {
                    mSearchResultInfo.setVisibility(View.GONE);
                    // 恢复默认的空列表提示
                    mEmptyView.setText(getString(R.string.empty_notes_list));
                    mEmptyView.setVisibility(View.GONE); // 有数据时隐藏空视图
                }
            }
        }
```
#### 3、实现效果界面截图
(1)点击搜索按钮进入搜索界面
![图片12](images/img12.png)
![图片13](images/img13.png)
(2)输入搜索内容，显示符合条件的笔记
![图片14](images/img14.png)
(3)回删搜素内容至空时，显示所有的笔记
![图片13](images/img13.png)
## 拓展附加功能
### （一）UI美化
#### 1、功能要求
界面背景显示未白色，不同笔记根据笔记类型的不同会显示不同的标签和背景颜色
#### 2、实现思路和技术实现
该部分在添加时间戳部分已进行详细介绍
#### 3、实现效果界面截图
（1）页面效果
![图片15](images/img15.png)
### （一）添加分类
#### 1、功能要求
不同的笔记可以设置为不同的分类，类别会在列表中显示，可以根据类别进行笔记筛选
#### 2、实现思路和技术实现
(1)在数据库契约类（NotePad.java）新增类别属性，定义类别
```
// 新增类别属性
        public static final String COLUMN_NAME_CATEGORY = "category";

        // 定义的类别
        public static final String CATEGORY_THING = "重要事件";
        public static final String CATEGORY_RECORD = "学习/会议记录";
        public static final String CATEGORY_ESSAY = "随笔";
        public static final String CATEGORY_OTHER = "其他";
```
(2)修改数据库帮助类（NotePadProvider.java）
创建投影映射
```
sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CATEGORY, NotePad.Notes.COLUMN_NAME_CATEGORY);
sLiveFolderProjectionMap.put("category", NotePad.Notes.COLUMN_NAME_CATEGORY);
```
修改onCreate和onUpgrade函数
```
       @Override
       public void onCreate(SQLiteDatabase db) {

           db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                   + NotePad.Notes._ID + " INTEGER PRIMARY KEY,"
                   + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                   + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                   + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                   + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
                   + NotePad.Notes.COLUMN_NAME_CATEGORY + " TEXT DEFAULT '" + NotePad.Notes.CATEGORY_OTHER + "'"
                   + ");");
       }
       
        @Override
       public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
           Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

           // 无论当前版本是什么，都确保有 category 列
           if (oldVersion < 2) {
               // 旧版本升级逻辑
               try {
                   db.execSQL("ALTER TABLE " + NotePad.Notes.TABLE_NAME +
                           " ADD COLUMN " + NotePad.Notes.COLUMN_NAME_CATEGORY +
                           " TEXT DEFAULT '" + NotePad.Notes.CATEGORY_OTHER + "'");
                   Log.i(TAG, "Added category column via ALTER TABLE");
               } catch (SQLException e) {
                   Log.e(TAG, "ALTER TABLE failed, recreating table...", e);
                   recreateTable(db);
               }
           }

           // 如果版本是2，也升级到3（确保category列存在）
           if (oldVersion == 2) {
               try {
                   // 检查category列是否存在
                   Cursor cursor = db.rawQuery("PRAGMA table_info(" + NotePad.Notes.TABLE_NAME + ")", null);
                   boolean hasCategory = false;
                   if (cursor != null) {
                       while (cursor.moveToNext()) {
                           if (NotePad.Notes.COLUMN_NAME_CATEGORY.equals(cursor.getString(1))) {
                               hasCategory = true;
                               break;
                           }
                       }
                       cursor.close();
                   }

                   if (!hasCategory) {
                       db.execSQL("ALTER TABLE " + NotePad.Notes.TABLE_NAME +
                               " ADD COLUMN " + NotePad.Notes.COLUMN_NAME_CATEGORY +
                               " TEXT DEFAULT '" + NotePad.Notes.CATEGORY_OTHER + "'");
                       Log.i(TAG, "Added missing category column for version 2->3 upgrade");
                   }
               } catch (SQLException e) {
                   Log.e(TAG, "Failed to add category column in version 2->3 upgrade", e);
                   recreateTable(db);
               }
           }
       }
```
（3）修改笔记编辑器（NoteEditor.java）
添加类别选择UI：
```
    private Spinner mCategorySpinner;
    private ArrayAdapter<String> mCategoryAdapter;
    private String[] mCategories;
    
     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        // 设置类别数组
        mCategories = new String[] {
                NotePad.Notes.CATEGORY_THING,
                NotePad.Notes.CATEGORY_RECORD,
                NotePad.Notes.CATEGORY_ESSAY,
                NotePad.Notes.CATEGORY_OTHER
        };

        mCategoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, mCategories);
        mCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, NotePad.Notes.CATEGORY_OTHER);
        ...//省略无关代码
        }
```
在populateFields设置类别选择
```
// 设置类别选择
if (mCategorySpinner != null && category != null) {
int position = Arrays.asList(mCategories).indexOf(category);
if (position >= 0) {
mCategorySpinner.setSelection(position);
}
```
修改编辑器布局（editor.xml):
```
    <!-- 类别选择 -->
    <Spinner
        android:id="@+id/category_spinner"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        android:minHeight="48dp"
        android:prompt="@string/select_category" />
```
（4）修改笔记列表（NotesList.java）：
```
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,//2 时间戳
            NotePad.Notes.COLUMN_NAME_NOTE,//3 内容预览
            NotePad.Notes.COLUMN_NAME_CATEGORY  // 4 类别列
    };

    //图片资源ID数组
    private static final int[] CATEGORY_ICON_RESOURCES = {
            R.drawable.star,    // 重要事件
            R.drawable.learn,   // 学习记录
            R.drawable.flower,    // 随笔
            R.drawable.glass     // 其他
    };
    
    // 创建适配器
        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_NOTE,
                NotePad.Notes.COLUMN_NAME_CATEGORY, // 新增类别列
                NotePad.Notes.COLUMN_NAME_CATEGORY,
                NotePad.Notes.COLUMN_NAME_CATEGORY,
        };

        int[] viewIDs = {
                R.id.note_title,
                R.id.note_time,
                R.id.note_preview,
                R.id.note_category, // 新增类别列
                R.id.note_icon,
                R.id.note_item_root
        };
        
        private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_CATEGORY  // 新增类别列
    };       
```
（5）修改列表布局（noteslist_item.xml)：
```
<!-- 类别图标  -->
    <ImageView
        android:id="@+id/note_icon"
        android:layout_width="32dp"
        android:layout_height="32dp"
        android:layout_marginEnd="12dp"
        android:layout_marginRight="12dp"
        android:layout_marginTop="12dp"
        android:scaleType="fitCenter"
        android:adjustViewBounds="true"
        android:src="@drawable/flower"
        android:contentDescription="@string/note_category_icon" />
        
  <TextView
                android:id="@+id/note_category"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textAppearance="?android:attr/textAppearanceSmall"
                android:padding="4dp"
                android:background="@drawable/bg_category_tag"
                android:textStyle="bold" />
```
（6）在list_options_menu.xml中添加筛选分类选项
```
<item
        android:id="@+id/menu_filter"
        android:title="@string/menu_filter"
        android:showAsAction="never">
        <menu>
            <item
                android:id="@+id/filter_all"
                android:title="@string/filter_all"
                android:checkable="true"
                android:checked="true"/>
            <item
                android:id="@+id/filter_thing"
                android:title="@string/category_thing"
                android:checkable="true"/>
            <item
                android:id="@+id/filter_record"
                android:title="@string/category_record"
                android:checkable="true"/>
            <item
                android:id="@+id/filter_essay"
                android:title="@string/category_essay"
                android:checkable="true"/>
            <item
                android:id="@+id/filter_other"
                android:title="@string/category_other"
                android:checkable="true"/>
        </menu>
    </item>
```
在NotesList中实现分类筛选
private String mCurrentCategory = "all"; // 当前选中的分类

```
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ... ...
        
        // 恢复保存的分类状态
        if (savedInstanceState != null) {
            mCurrentCategory = savedInstanceState.getString("current_category", "all");
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("current_category", mCurrentCategory);
    }
    
    /**
     * 修改后的显示笔记方法，支持分类筛选
     */
    private void displayNotes() {
        String selection = null;
        String[] selectionArgs = null;
        
        // 构建查询条件：分类筛选 + 搜索筛选
        List<String> conditions = new ArrayList<>();
        List<String> args = new ArrayList<>();
        
        // 分类筛选条件
        if (!"all".equals(mCurrentCategory)) {
            conditions.add(NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?");
            args.add(getCategoryConstant(mCurrentCategory));
        }
        
        // 搜索筛选条件
        if (!TextUtils.isEmpty(mCurrentFilter)) {
            conditions.add(NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?");
            args.add("%" + mCurrentFilter + "%");
        }
        
        // 组合所有条件
        if (!conditions.isEmpty()) {
            selection = TextUtils.join(" AND ", conditions);
            selectionArgs = args.toArray(new String[0]);
        }
        
        Cursor cursor = getContentResolver().query(
            getIntent().getData(),
            PROJECTION,
            selection,
            selectionArgs,
            NotePad.Notes.DEFAULT_SORT_ORDER
        );
        
        // ...  ...
    }
    /**
     * 处理分类筛选菜单点击
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter_all:
                setCategoryFilter("all");
                break;
            case R.id.filter_thing:
                setCategoryFilter("filter_thing");
                break;
            case R.id.filter_record:
                setCategoryFilter("filter_record");
                break;
            case R.id.filter_essay:
                setCategoryFilter("filter_essay");
                break;
            case R.id.filter_other:
                setCategoryFilter("filter_other");
                break;
            case R.id.menu_add:
                // 现有添加逻辑
                startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
    
    /**
     * 设置分类筛选
     */
    private void setCategoryFilter(String category) {
        mCurrentCategory = category;
        displayNotes();
        
        // 更新菜单项的选中状态
        invalidateOptionsMenu();
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        // 设置分类菜单项的选中状态
        MenuItem allItem = menu.findItem(R.id.filter_all);
        MenuItem thingItem = menu.findItem(R.id.filter_thing);
        MenuItem recordItem = menu.findItem(R.id.filter_record);
        MenuItem essayItem = menu.findItem(R.id.filter_essay);
        MenuItem otherItem = menu.findItem(R.id.filter_other);
        
        // 重置所有项为未选中
        allItem.setChecked(false);
        thingItem.setChecked(false);
        recordItem.setChecked(false);
        essayItem.setChecked(false);
        otherItem.setChecked(false);
        
        // 设置当前选中项
        switch (mCurrentCategory) {
            case "all":
                allItem.setChecked(true);
                break;
            case "filter_thing":
                thingItem.setChecked(true);
                break;
            case "filter_record":
                recordItem.setChecked(true);
                break;
            case "filter_essay":
                essayItem.setChecked(true);
                break;
            case "filter_other":
                otherItem.setChecked(true);
                break;
        }
        
        return true;
    }
}
```
#### 3、实现效果界面截图
(1)用户可在编辑界面选择类别
![图片16](images/img16.png)
(2)类别会在列表界面显示
![图片17](images/img17.png)
(3)在主页可以按照类别进行笔记筛选
![图片18](images/img18.png)
![图片19](images/img19.png)
### （三）导出笔记
#### 1、功能要求
笔记可以被以JSON、CSV、TXT格式导出，并保存到手机的“下载”文件夹中
#### 2、实现思路和技术实现
(1)首先在笔记编辑界面的菜单中添加导出选项
```
    <item android:id="@+id/menu_export"
        android:icon="@drawable/ic_menu_save"
        android:showAsAction="ifRoom|withText"
        android:title="@string/menu_export" />
```
(2）在 NoteEditor.java中添加代码：
```
 @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        int id = item.getItemId();
        if(id== R.id.menu_save) {
            String text = mText.getText().toString();
            updateNote(text, null);
            finish();
        } else if (id == R.id.menu_delete) {
            deleteNote();
            finish();
        }
        else if (id == R.id.menu_revert) {
            cancelNote();
        }
        else if (id == R.id.menu_export) {
            // 新增：处理导出功能
            showExportOptions();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    /**
     * 显示导出选项对话框
     */
    private void showExportOptions() {
        final String[] formats = {
                getString(R.string.export_format_txt),
                getString(R.string.export_format_md),
                getString(R.string.export_format_html)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_export_format);
        builder.setItems(formats, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case EXPORT_FORMAT_TXT:
                        exportNoteToTxt();
                        break;
                    case EXPORT_FORMAT_MD:
                        exportNoteToMarkdown();
                        break;
                    case EXPORT_FORMAT_HTML:
                        exportNoteToHtml();
                        break;
                }
            }
        });
        builder.show();
    }

    /**
     * 导出笔记为TXT格式
     */
    private void exportNoteToTxt() {
        try {
            // 获取笔记数据
            String title = mTitle != null ? mTitle.getText().toString() : "";
            String content = mText.getText().toString();
            String category = "";
            if (mCategorySpinner != null && mCategorySpinner.getSelectedItem() != null) {
                category = mCategorySpinner.getSelectedItem().toString();
            }
            long modificationDate = System.currentTimeMillis();

            // 获取当前笔记的修改时间（从数据库）
            if (mCursor != null && mCursor.moveToFirst()) {
                int dateColumnIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);
                if (dateColumnIndex != -1) {
                    modificationDate = mCursor.getLong(dateColumnIndex);
                }
            }

            // 创建导出目录
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // 生成安全的文件名
            String safeTitle = title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
            if (safeTitle.isEmpty()) {
                safeTitle = "untitled";
            }
            String fileName = "note_" + safeTitle + "_" + System.currentTimeMillis() + ".txt";
            File exportFile = new File(exportDir, fileName);

            // 写入文件
            FileWriter writer = new FileWriter(exportFile);
            writer.write("笔记标题: " + title + "\n");
            writer.write("分类: " + category + "\n");
            writer.write("修改时间: " + formatDate(modificationDate) + "\n");
            writer.write("导出时间: " + formatDate(System.currentTimeMillis()) + "\n");
            writer.write("\n" + repeat("=", 50) + "\n\n");
            writer.write(content);
            writer.close();

            String message = getString(R.string.export_single_note_success, exportFile.getAbsolutePath());
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            String errorMessage = getString(R.string.export_single_note_failed, e.getMessage());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            Log.e(TAG, "TXT导出失败", e);
        }
    }
    /**
     * 导出笔记为Markdown格式
     */
    private void exportNoteToMarkdown() {
        try {
            // 获取笔记数据
            String title = mTitle != null ? mTitle.getText().toString() : "";
            String content = mText.getText().toString();
            String category = "";
            if (mCategorySpinner != null && mCategorySpinner.getSelectedItem() != null) {
                category = mCategorySpinner.getSelectedItem().toString();
            }
            long modificationDate = System.currentTimeMillis();

            // 获取当前笔记的修改时间
            if (mCursor != null && mCursor.moveToFirst()) {
                int dateColumnIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);
                if (dateColumnIndex != -1) {
                    modificationDate = mCursor.getLong(dateColumnIndex);
                }
            }

            // 创建导出目录
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // 生成安全的文件名
            String safeTitle = title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
            if (safeTitle.isEmpty()) {
                safeTitle = "untitled";
            }
            String fileName = "note_" + safeTitle + "_" + System.currentTimeMillis() + ".md";
            File exportFile = new File(exportDir, fileName);

            // 写入Markdown文件
            FileWriter writer = new FileWriter(exportFile);
            writer.write("# " + title + "\n\n");
            writer.write("**分类:** " + category + "  \n");
            writer.write("**修改时间:** " + formatDate(modificationDate) + "  \n");
            writer.write("**导出时间:** " + formatDate(System.currentTimeMillis()) + "  \n\n");
            writer.write("---\n\n");
            writer.write(content);
            writer.write("\n");
            writer.close();

            String message = getString(R.string.export_single_note_success, exportFile.getAbsolutePath());
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            String errorMessage = getString(R.string.export_single_note_failed, e.getMessage());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Markdown导出失败", e);
        }
    }

    /**
     * 导出笔记为HTML格式
     */
    private void exportNoteToHtml() {
        try {
            // 获取笔记数据
            String title = mTitle != null ? mTitle.getText().toString() : "";
            String content = mText.getText().toString();
            String category = "";
            if (mCategorySpinner != null && mCategorySpinner.getSelectedItem() != null) {
                category = mCategorySpinner.getSelectedItem().toString();
            }
            long modificationDate = System.currentTimeMillis();

            // 获取当前笔记的修改时间
            if (mCursor != null && mCursor.moveToFirst()) {
                int dateColumnIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);
                if (dateColumnIndex != -1) {
                    modificationDate = mCursor.getLong(dateColumnIndex);
                }
            }

            // 转义HTML特殊字符
            content = escapeHtml(content);
            title = escapeHtml(title);
            category = escapeHtml(category);

            // 创建导出目录
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // 生成安全的文件名
            String safeTitle = title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
            if (safeTitle.isEmpty()) {
                safeTitle = "untitled";
            }
            String fileName = "note_" + safeTitle + "_" + System.currentTimeMillis() + ".html";
            File exportFile = new File(exportDir, fileName);

            // 写入HTML文件
            FileWriter writer = new FileWriter(exportFile);
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html lang=\"zh-CN\">\n");
            writer.write("<head>\n");
            writer.write("    <meta charset=\"UTF-8\">\n");
            writer.write("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            writer.write("    <title>" + title + "</title>\n");
            writer.write("    <style>\n");
            writer.write("        body { font-family: Arial, sans-serif; max-width: 800px; margin: 40px auto; padding: 20px; line-height: 1.6; }\n");
            writer.write("        .header { border-bottom: 2px solid #333; padding-bottom: 10px; margin-bottom: 20px; }\n");
            writer.write("        .title { font-size: 24px; font-weight: bold; margin-bottom: 10px; }\n");
            writer.write("        .meta { color: #666; font-size: 14px; }\n");
            writer.write("        .content { white-space: pre-wrap; }\n");
            writer.write("    </style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");
            writer.write("    <div class=\"header\">\n");
            writer.write("        <div class=\"title\">" + title + "</div>\n");
            writer.write("        <div class=\"meta\">\n");
            writer.write("            <strong>分类:</strong> " + category + "<br>\n");
            writer.write("            <strong>修改时间:</strong> " + formatDate(modificationDate) + "<br>\n");
            writer.write("            <strong>导出时间:</strong> " + formatDate(System.currentTimeMillis()) + "\n");
            writer.write("        </div>\n");
            writer.write("    </div>\n");
            writer.write("    <div class=\"content\">" + content + "</div>\n");
            writer.write("</body>\n");
            writer.write("</html>");
            writer.close();

            String message = getString(R.string.export_single_note_success, exportFile.getAbsolutePath());
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            String errorMessage = getString(R.string.export_single_note_failed, e.getMessage());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            Log.e(TAG, "HTML导出失败", e);
        }
    }

    /**
     * 转义HTML特殊字符
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
```
(3)在 AndroidManifest.xml中添加存储权限：
```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />b
```
#### 3、实现效果界面截图
（1）进入所需要导出的笔记中，点击导出按钮
![图片20.png](images/img20.png)
（2）选择导出类型
![图片21.png](images/img21.png)
（3）底部会跳出保存成功的提示和位置
![图片22.png](images/img22.png)







