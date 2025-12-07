/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotesList extends ListActivity implements SearchView.OnQueryTextListener{

    // For logging and debugging
    private static final String TAG = "NotesList";

    /**
     * 搜索栏相关
     */
    private String mCurrentFilter = "";
    private SearchView mSearchView;

    private ListView mListView;
    private TextView mEmptyView;
    private TextView mSearchResultInfo;

    /**
     * The columns needed by the cursor adapter
     */
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

    //备选图标
    private static final int[] SYSTEM_ICON_RESOURCES = {
            android.R.drawable.btn_star_big_on,
            android.R.drawable.ic_menu_agenda,
            android.R.drawable.ic_menu_edit,
            android.R.drawable.ic_menu_help
    };


    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;

    /**
     * onCreate is called when Android starts this Activity from scratch.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置自定义布局，包含搜索栏和列表
        setContentView(R.layout.notes_list_with_search);

        DatabaseInspector.inspectDatabase(this);
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);


        Intent intent = getIntent();


        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        mListView = getListView();
        mListView.setOnCreateContextMenuListener(this);

        displayNotes();
    }


    /**
     * 显示笔记列表（支持搜索过滤）
     */
    private void displayNotes() {
        String selection = null;
        String[] selectionArgs = null;

        // 如果有搜索过滤条件，添加到查询中
        if (!TextUtils.isEmpty(mCurrentFilter)) {
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
            String likePattern = "%" + mCurrentFilter + "%";
            selectionArgs = new String[]{likePattern};
        }

        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        // 创建适配器
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

                    // 获取笔记的类别（临时方法）
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



        setListAdapter(adapter);

        // 更新搜索结果提示和空视图状态
        updateSearchResultInfo(cursor.getCount());
    }

    private int getColumnIndex(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        if (index == -1) {
            Log.e(TAG, "列 '" + columnName + "' 不存在于游标中");
            // 尝试通过列名字符串查找
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                if (columnName.equals(cursor.getColumnName(i))) {
                    return i;
                }
            }
        }
        return index;
    }

    //检查资源是否存在
    private boolean resourceExists(int resourceId) {
        if (resourceId == 0) return false;
        try {
            getResources().getResourceTypeName(resourceId);
            return true;
        } catch (Resources.NotFoundException e) {
            return false;
        }
    }

    /**
     * 获取类别索引
     */
    private int getCategoryIndex(String category) {
        switch (category) {
            case NotePad.Notes.CATEGORY_THING:
                return 0;
            case NotePad.Notes.CATEGORY_RECORD:
                return 1;
            case NotePad.Notes.CATEGORY_ESSAY:
                return 2;
            case NotePad.Notes.CATEGORY_OTHER:
                return 3;
            default:
                return 3; // 默认返回"其他"的索引
        }
    }


    private int getCategoryColor(String category) {
        switch (category) {
            case NotePad.Notes.CATEGORY_THING:
                return android.R.color.holo_blue_dark;
            case NotePad.Notes.CATEGORY_RECORD:
                return android.R.color.holo_green_dark;
            case NotePad.Notes.CATEGORY_ESSAY:
                return android.R.color.holo_orange_dark;
            default:
                return android.R.color.darker_gray;
        }
    }

    /**
     * 根据类别获取对应的背景颜色
     */
    private int getBackgroundColorForCategory(String category) {
        if (category == null) {
            return getResources().getColor(android.R.color.white);
        }

        switch (category) {
            case NotePad.Notes.CATEGORY_THING:
                return getResources().getColor(R.color.category_thing_bg); // 重要事件背景色

            case NotePad.Notes.CATEGORY_RECORD:
                return getResources().getColor(R.color.category_record_bg); // 学习记录背景色

            case NotePad.Notes.CATEGORY_ESSAY:
                return getResources().getColor(R.color.category_essay_bg); // 随笔背景色

            case NotePad.Notes.CATEGORY_OTHER:
                return getResources().getColor(R.color.category_other_bg); // 其他背景色

            default:
                // 处理中文类别名称的情况
                if ("重要事件".equals(category)) {
                    return getResources().getColor(R.color.category_thing_bg);
                } else if ("学习/会议记录".equals(category)) {
                    return getResources().getColor(R.color.category_record_bg);
                } else if ("随笔".equals(category)) {
                    return getResources().getColor(R.color.category_essay_bg);
                } else {
                    return getResources().getColor(R.color.category_other_bg);
                }
        }
    }

    private void setupIconAppearance(ImageView iconView, String category) {
        // 根据类别设置不同的样式
        switch (category) {
            case NotePad.Notes.CATEGORY_THING:
                iconView.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
                iconView.setAlpha(0.9f);
                break;
            case NotePad.Notes.CATEGORY_RECORD:
                iconView.setColorFilter(getResources().getColor(android.R.color.holo_blue_dark));
                iconView.setAlpha(0.9f);
                break;
            case NotePad.Notes.CATEGORY_ESSAY:
                iconView.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                iconView.setAlpha(0.9f);
                break;
            case NotePad.Notes.CATEGORY_OTHER:
            default:
                iconView.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                iconView.setAlpha(0.7f);
                break;
        }
    }

    private String getCategoryForNote(Cursor cursor) {
        int categoryColumnIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CATEGORY);

        if (categoryColumnIndex == -1) {
            Log.w(TAG, "category列不存在，使用默认类别");
            return NotePad.Notes.CATEGORY_OTHER;
        }

        // 从游标读取真实的类别值
        String category = cursor.getString(categoryColumnIndex);

        // 如果数据库中的值为null或空，使用默认值
        if (category == null || category.trim().isEmpty()) {
            category = NotePad.Notes.CATEGORY_OTHER;
        }

        Log.d(TAG, "笔记ID: " + cursor.getLong(0) + ", 真实类别: " + category);
        return category;
    }

    private String getCategoryDisplayName(String category) {
        switch (category) {
            case NotePad.Notes.CATEGORY_THING:
                return "重要事件";
            case NotePad.Notes.CATEGORY_RECORD:
                return "学习/会议记录";
            case NotePad.Notes.CATEGORY_ESSAY:
                return "随笔";
            case NotePad.Notes.CATEGORY_OTHER:
            default:
                return "其他";
        }
    }

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



    //格式化时间函数
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

    private String formatDate(long timestamp) {
        return SDF.format(new Date(timestamp));
    }

    /**
     * Called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *
     * Sets up a menu that provides the Insert option plus a list of alternative actions for
     * this Activity. Other applications that want to handle notes can "register" themselves in
     * Android by providing an intent filter that includes the category ALTERNATIVE and the
     * mimeTYpe NotePad.Notes.CONTENT_TYPE. If they do this, the code in onCreateOptionsMenu()
     * will add the Activity that contains the intent filter to its list of options. In effect,
     * the menu will offer the user other applications that can handle notes.
     * @param menu A Menu object, to which menu items should be added.
     * @return True, always. The menu should be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // 设置搜索菜单项（如果使用的是动作栏搜索）
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem != null) {
            mSearchView = (SearchView) searchItem.getActionView();
            if (mSearchView != null) {
                mSearchView.setQueryHint(getString(R.string.search_hint));
                mSearchView.setOnQueryTextListener(this);
            }
        }

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);



        // Gets the number of notes currently being displayed.
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                Menu.NONE,                  // A unique item ID is not required.
                Menu.NONE,                  // The alternatives don't need to be in order.
                null,                       // The caller's name is not excluded from the group.
                specifics,                  // These specific options must appear first.
                intent,                     // These Intent objects map to the options in specifics.
                Menu.NONE,                  // No flags are required.
                items                       // The menu items generated from the specifics-to-
                                            // Intents mapping
            );
                // If the Edit menu item exists, adds shortcuts for it.
                if (items[0] != null) {

                    // Sets the Edit menu item shortcut to numeric "1", letter "e"
                    items[0].setShortcut('1', 'e');
                }
            } else {
                // If the list is empty, removes any existing alternative actions from the menu
                menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
            }

        // Displays the menu
        return true;
    }

    /**
     * This method is called when the user selects an option from the menu, but no item
     * in the list is selected. If the option was INSERT, then a new Intent is sent out with action
     * ACTION_INSERT. The data from the incoming Intent is put into the new Intent. In effect,
     * this triggers the NoteEditor activity in the NotePad application.
     *
     * If the item was not INSERT, then most likely it was an alternative option from another
     * application. The parent method is called to process the item.
     * @param item The menu item that was selected by the user
     * @return True, if the INSERT menu item was selected; otherwise, the result of calling
     * the parent method.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add) {
            /*
             * Launches a new Activity using an Intent. The intent filter for the Activity
             * has to have action ACTION_INSERT. No category is set, so DEFAULT is assumed.
             * In effect, this starts the NoteEditor Activity in NotePad.
             */
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public boolean onQueryTextSubmit(String query) {
        mCurrentFilter = query;
        displayNotes();
        // 隐藏键盘
        hideKeyboard();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mCurrentFilter = newText;
        displayNotes();
        return true;
    }

    /**
     * 隐藏软键盘
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }



    /**
     * This method is called when the user context-clicks a note in the list. NotesList registers
     * itself as the handler for context menus in its ListView (this is done in onCreate()).
     *
     * The only available options are COPY and DELETE.
     *
     * Context-click is equivalent to long-press.
     *
     * @param menu A ContexMenu object to which items should be added.
     * @param view The View for which the context menu is being constructed.
     * @param menuInfo Data associated with view.
     * @throws ClassCastException
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * Gets the data associated with the item at the selected position. getItem() returns
         * whatever the backing adapter of the ListView has associated with the item. In NotesList,
         * the adapter associated all of the data for a note with its list item. As a result,
         * getItem() returns that data as a Cursor.
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // If the cursor is empty, then for some reason the adapter can't get the data from the
        // provider, so returns null to the caller.
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Append to the
        // menu items for any other activities that can do stuff with it
        // as well.  This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(), 
                                        Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    /**
     * This method is called when the user selects an item from the context menu
     * (see onCreateContextMenu()). The only menu items that are actually handled are DELETE and
     * COPY. Anything else is an alternative option, for which default handling should be done.
     *
     * @param item The selected menu item
     * @return True if the menu item was DELETE, and no default processing is need, otherwise false,
     * which triggers the default handling of the item.
     * @throws ClassCastException
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        /*
         * Gets the extra info from the menu item. When an note in the Notes list is long-pressed, a
         * context menu appears. The menu items for the menu automatically get the data
         * associated with the note that was long-pressed. The data comes from the provider that
         * backs the list.
         *
         * The note's data is passed to the context menu creation routine in a ContextMenuInfo
         * object.
         *
         * When one of the context menu items is clicked, the same data is passed, along with the
         * note ID, to onContextItemSelected() via the item parameter.
         */
        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }
        // Appends the selected note's ID to the URI sent with the incoming Intent.
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        int id = item.getItemId();
        if (id == R.id.context_open) {
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
        } else if (id == R.id.context_copy) { //BEGIN_INCLUDE(copy)
            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);

            // Copies the notes URI to the clipboard. In effect, this copies the note itself
            clipboard.setPrimaryClip(ClipData.newUri(   // new clipboard item holding a URI
                    getContentResolver(),               // resolver to retrieve URI info
                    "Note",                             // label for the clip
                    noteUri));                          // the URI

            // Returns to the caller and skips further processing.
            return true;
            //END_INCLUDE(copy)
        } else if (id == R.id.context_delete) {
            // Deletes the note from the provider by passing in a URI in note ID format.
            // Please see the introductory note about performing provider operations on the
            // UI thread.
            getContentResolver().delete(
                    noteUri,  // The URI of the provider
                    null,     // No where clause is needed, since only a single note ID is being
                    // passed in.
                    null      // No where clause is used, so no where arguments are needed.
            );

            // Returns to the caller and skips further processing.
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * This method is called when the user clicks a note in the displayed list.
     *
     * This method handles incoming actions of either PICK (get data from the provider) or
     * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
     * new Intent to start NoteEditor.
     * @param l The ListView that contains the clicked item
     * @param v The View of the individual item
     * @param position The position of v in the displayed list
     * @param id The row ID of the clicked item
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
