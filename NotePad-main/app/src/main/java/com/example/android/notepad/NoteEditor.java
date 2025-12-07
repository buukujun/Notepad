package com.example.android.notepad;

import static com.example.android.notepad.R.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class NoteEditor extends Activity {
    // For logging and debugging purposes
    private static final String TAG = "NoteEditor";

    private Spinner mCategorySpinner;
    private ArrayAdapter<String> mCategoryAdapter;
    private String[] mCategories;

    private String mOriginalCategory;//原始类别

    private EditText mTitle;

    // 导出相关常量
    private static final int EXPORT_FORMAT_TXT = 0;
    private static final int EXPORT_FORMAT_MD = 1;
    private static final int EXPORT_FORMAT_HTML = 2;

    // 添加权限相关
    private static final int REQUEST_WRITE_STORAGE = 112;
    /*
     * Creates a projection that returns the note ID and the note contents.
     */
    private static final String[] PROJECTION =
        new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_CATEGORY
    };



    // A label for the saved state of the activity
    private static final String ORIGINAL_CONTENT = "origContent";

    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;


    // Global mutable variables
    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private String mOriginalContent;


    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        // This constructor is used by LayoutInflater
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);

            // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        /**
         * This is called to draw the LinedEditText object
         * @param canvas The canvas on which the background is drawn.
         */
        @Override
        protected void onDraw(Canvas canvas) {

            // Gets the number of lines of text in the View.
            int count = getLineCount();

            // Gets the global Rect and Paint objects
            Rect r = mRect;
            Paint paint = mPaint;

            /*
             * Draws one line in the rectangle for every line of text in the EditText
             */
            for (int i = 0; i < count; i++) {

                // Gets the baseline coordinates for the current line of text
                int baseline = getLineBounds(i, r);


                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            // Finishes up by calling the parent method
            super.onDraw(canvas);
        }
    }

    /**
     * This method is called by Android when the Activity is first started. From the incoming
     * Intent, it determines what kind of editing is desired, and then does it.
     */
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

        final String action = intent.getAction();

        if (Intent.ACTION_EDIT.equals(action)) {

            // Sets the Activity state to EDIT, and gets the URI for the data to be edited.
            mState = STATE_EDIT;
            mUri = intent.getData();

            // For an insert or paste action:
        } else if (Intent.ACTION_INSERT.equals(action)
                || Intent.ACTION_PASTE.equals(action)) {

            // Sets the Activity state to INSERT, gets the general note URI, and inserts an
            // empty record in the provider
            mState = STATE_INSERT;

            // 创建初始数据，包含空标题
            ContentValues values = new ContentValues();
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, ""); // 初始空标题
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");   // 初始空内容
            values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, NotePad.Notes.CATEGORY_OTHER); // 默认类别
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

            mUri = getContentResolver().insert(intent.getData(), null);

            if (mUri == null) {

                // Writes the log identifier, a message, and the URI that failed.
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());

                // Closes the activity.
                finish();
                return;
            }

            // Since the new entry was created, this sets the result to be returned
            // set the result to be returned.
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

        // If the action was other than EDIT or INSERT:
        } else {

            // Logs an error that the action was not understood, finishes the Activity, and
            // returns RESULT_CANCELED to an originating Activity.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }


        mCursor = managedQuery(
            mUri,         // The URI that gets multiple notes from the provider.
            PROJECTION,   // A projection that returns the note ID and note content for each note.
            null,         // No "where" clause selection criteria.
            null,         // No "where" clause selection values.
            null          // Use the default sort order (modification date, descending)
        );

        // For a paste, initializes the data from clipboard.
        // (Must be done after mCursor is initialized.)
        if (Intent.ACTION_PASTE.equals(action)) {
            // Does the paste
            performPaste();
            // Switches the state to EDIT so the title can be modified.
            mState = STATE_EDIT;
        }

        // Sets the layout for this Activity. See res/layout/note_editor.xml
        setContentView(R.layout.note_editor);

        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
            // 新增：恢复原始类别
            mOriginalCategory = savedInstanceState.getString("original_category");
        }

        // Gets a handle to the EditText in the the layout.
        mText = (EditText) findViewById(R.id.note);

        /*
         * If this Activity had stopped previously, its state was written the ORIGINAL_CONTENT
         * location in the saved Instance state. This gets the state.
         */
        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }

        mTitle = (EditText) findViewById(id.note_title);
        mCategorySpinner = (Spinner) findViewById(id.category_spinner);
        if (mCategorySpinner != null) {
            mCategorySpinner.setAdapter(mCategoryAdapter);
            mCategorySpinner.setPrompt(getString(R.string.select_category));
        }

// 填充数据
        populateFields();

        if (savedInstanceState != null && mOriginalCategory == null) {
            mOriginalCategory = savedInstanceState.getString("original_category");
        }

        checkStoragePermission();
    }

    /**
     * 检查存储权限
     */
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要存储权限才能导出笔记", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final void populateFields() {
        if (mUri == null) {
            setTitle(getText(R.string.error_title));
            return;
        }

        Cursor cursor = managedQuery(mUri, PROJECTION, null, null, null);
        if (cursor == null) {
            setTitle(getText(R.string.error_title));
            return;
        }

        if (cursor.moveToFirst()) {
            int titleColumnIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
            int noteColumnIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            int categoryColumnIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CATEGORY);

            String title = cursor.getString(titleColumnIndex);
            String note = cursor.getString(noteColumnIndex);
            String category = cursor.getString(categoryColumnIndex);

            mText.setTextKeepState(note);
            if (mTitle != null) {
                if (title != null) {
                    mTitle.setTextKeepState(title);
                } else {
                    mTitle.setTextKeepState(""); // 确保空标题也显示
                }
            }

            // 设置类别选择
            if (mCategorySpinner != null && category != null) {
                int position = Arrays.asList(mCategories).indexOf(category);
                if (position >= 0) {
                    mCategorySpinner.setSelection(position);
                }
            }
            mOriginalCategory = category;
            mOriginalContent = note;
        }

        // 关闭游标
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        /*
         * mCursor is initialized, since onCreate() always precedes onResume for any running
         * process. This tests that it's not null, since it should always contain data.
         */
        if (mCursor != null) {
            // Requery in case something changed while paused (such as the title)
            mCursor.requery();
            mCursor.moveToFirst();

            // 更新标题
            if (mTitle != null) {
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                mTitle.setTextKeepState(title);
            }

            // 更新类别
            if (mCategorySpinner != null) {
                int categoryColumnIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CATEGORY);
                String category = mCursor.getString(categoryColumnIndex);
                if (category != null) {
                    int position = Arrays.asList(mCategories).indexOf(category);
                    if (position >= 0) {
                        mCategorySpinner.setSelection(position);
                    }
                }
            }

            // Modifies the window title for the Activity according to the current Activity state.
            if (mState == STATE_EDIT) {
                // Set the title of the Activity to include the note title
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
            // Sets the title to "create" for inserts
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            /*
             * onResume() may have been called after the Activity lost focus (was paused).
             * The user was either editing or creating a note when the Activity paused.
             * The Activity should re-display the text that had been retrieved previously, but
             * it should not move the cursor. This helps the user to continue editing or entering.
             */

            // Gets the note text from the Cursor and puts it in the TextView, but doesn't change
            // the text cursor's position.
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            String note = mCursor.getString(colNoteIndex);
            mText.setTextKeepState(note);

            // Stores the original note text, to allow the user to revert changes.
            if (mOriginalContent == null) {
                mOriginalContent = note;
            }

        /*
         * Something is wrong. The Cursor should always contain data. Report an error in the
         * note.
         */
        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
    }

    /**
     * This method is called when an Activity loses focus during its normal operation, and is then
     * later on killed. The Activity has a chance to save its state so that the system can restore
     * it.
     *
     * Notice that this method isn't a normal part of the Activity lifecycle. It won't be called
     * if the user simply navigates away from the Activity.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);

        outState.putString("original_category", mOriginalCategory);
    }

    /**
     * This method is called when the Activity loses focus.
     *
     * For Activity objects that edit information, onPause() may be the one place where changes are
     * saved. The Android application model is predicated on the idea that "save" and "exit" aren't
     * required actions. When users navigate away from an Activity, they shouldn't have to go back
     * to it to complete their work. The act of going away should save everything and leave the
     * Activity in a state where Android can destroy it if necessary.
     *
     * If the user hasn't done anything, then this deletes or clears out the note, otherwise it
     * writes the user's work to the provider.
     */
    @Override
    protected void onPause() {
        super.onPause();

        if (mCursor != null) {

            // Get the current note text.
            String text = mText.getText().toString();
            String title = "";
            if (mTitle != null) {
                title = mTitle.getText().toString().trim();
            }
            int length = text.length();

            // 检查标题是否为空且内容不为空
            boolean hasContentButNoTitle = !text.isEmpty() && title.isEmpty();


            if (isFinishing() && length == 0 && title.isEmpty()) {
                // 如果正在结束且没有任何内容，删除笔记
                setResult(RESULT_CANCELED);
                deleteNote();
            } else if (hasContentButNoTitle) {
                // 如果有内容但没有标题，显示一个标题或使用默认标题
                if (mState == STATE_EDIT) {
                    // 自动生成一个标题
                    String generatedTitle = generateTitleFromContent(text);
                    if (mTitle != null) {
                        mTitle.setText(generatedTitle);
                    }
                    updateNote(text, generatedTitle);
                }
                mState = STATE_EDIT;
            } else if (mState == STATE_EDIT) {
                // 正常保存编辑的内容
                updateNote(text, null);
            } else if (mState == STATE_INSERT) {
                // 保存新插入的内容
                updateNote(text, null);
                mState = STATE_EDIT;
            }
        }
    }

    /**
     * 从内容生成标题
     */
    private String generateTitleFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return getString(R.string.default_title);
        }

        int length = content.length();
        String title = content.substring(0, Math.min(30, length));
        if (length > 30) {
            int lastSpace = title.lastIndexOf(' ');
            if (lastSpace > 0) {
                title = title.substring(0, lastSpace);
            }
        }
        return title;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


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

    public static String repeat(String str, int count) {
        if (str == null || count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
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

    /**
     * 格式化日期（复用NotesList中的方法）
     */
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }


//BEGIN_INCLUDE(paste)
    /**
     * A helper method that replaces the note's data with the contents of the clipboard.
     */
    private final void performPaste() {

        // Gets a handle to the Clipboard Manager
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        // Gets a content resolver instance
        ContentResolver cr = getContentResolver();

        // Gets the clipboard data from the clipboard
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {

            String text=null;
            String title=null;

            // Gets the first item from the clipboard data
            ClipData.Item item = clip.getItemAt(0);

            // Tries to get the item's contents as a URI pointing to a note
            Uri uri = item.getUri();

            // Tests to see that the item actually is an URI, and that the URI
            // is a content URI pointing to a provider whose MIME type is the same
            // as the MIME type supported by the Note pad provider.
            if (uri != null && NotePad.Notes.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {

                // The clipboard holds a reference to data with a note MIME type. This copies it.
                Cursor orig = cr.query(
                        uri,            // URI for the content provider
                        PROJECTION,     // Get the columns referred to in the projection
                        null,           // No selection variables
                        null,           // No selection variables, so no criteria are needed
                        null            // Use the default sort order
                );

                // If the Cursor is not null, and it contains at least one record
                // (moveToFirst() returns true), then this gets the note data from it.
                if (orig != null) {
                    if (orig.moveToFirst()) {
                        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                        int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                        text = orig.getString(colNoteIndex);
                        title = orig.getString(colTitleIndex);
                    }

                    // Closes the cursor.
                    orig.close();
                }
            }

            // If the contents of the clipboard wasn't a reference to a note, then
            // this converts whatever it is to text.
            if (text == null) {
                text = item.coerceToText(this).toString();
            }

            // Updates the current note with the retrieved title and text.
            updateNote(text, title);
        }
    }
//END_INCLUDE(paste)

    /**
     * Replaces the current note contents with the text and title provided as arguments.
     * @param text The new note contents to use.
     * @param title The new note title to use
     */
    private final void updateNote(String text, String title) {

        // Sets up a map to contain values to be updated in the provider.
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

        // 获取当前用户输入的标题
        String currentTitle = "";
        if (mTitle != null) {
            currentTitle = mTitle.getText().toString().trim();
        }

        // If the action is to insert a new note, this creates an initial title for it.
        if (mState == STATE_INSERT) {
            // 插入新笔记时
            if (title == null) {
                // 如果没有通过参数传入标题，使用用户输入的标题
                if (!currentTitle.isEmpty()) {
                    title = currentTitle;
                } else {
                    // 如果用户输入的标题也为空，从内容生成标题
                    int length = text.length();
                    if (length > 0) {
                        title = text.substring(0, Math.min(30, length));
                        if (length > 30) {
                            int lastSpace = title.lastIndexOf(' ');
                            if (lastSpace > 0) {
                                title = title.substring(0, lastSpace);
                            }
                        }
                    } else {
                        title = getString(string.default_title); // 需要在strings.xml中定义
                    }
                }
            }
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);

        } else {
            // 编辑现有笔记时，始终使用用户输入的标题
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, currentTitle);
        }

        // This puts the desired notes text into the map.
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);


        if (mCategorySpinner != null) {
            values.put(NotePad.Notes.COLUMN_NAME_CATEGORY,
                    mCategories[mCategorySpinner.getSelectedItemPosition()]);
        }

        // 更新数据库
        int rowsUpdated = getContentResolver().update(
                mUri,    // The URI for the record to update.
                values,  // The map of column names and new values to apply to them.
                null,    // No selection criteria are used, so no where columns are necessary.
                null     // No where arguments are used, so no where arguments are necessary.
        );

        if (rowsUpdated > 0) {
            // 更新成功，刷新显示
            mOriginalContent = text;
            mOriginalCategory = mCategories[mCategorySpinner.getSelectedItemPosition()];

            // 如果是编辑状态，更新窗口标题
            if (mState == STATE_EDIT && mTitle != null) {
                String newTitle = mTitle.getText().toString();
                if (!newTitle.isEmpty()) {
                    Resources res = getResources();
                    String format = res.getString(R.string.title_edit);
                    setTitle(String.format(format, newTitle));
                }
            }
        }




    }

    /**
     * This helper method cancels the work done on a note.  It deletes the note if it was
     * newly created, or reverts to the original text of the note i
     */
    private final void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                // Put the original note text back into the database
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent);

                if (mOriginalCategory != null) {
                    values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, mOriginalCategory);
                } else {
                    // 如果原始类别为空，设置为默认类别
                    values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, NotePad.Notes.CATEGORY_OTHER);
                }

                getContentResolver().update(mUri, values, null, null);

                Toast.makeText(this, R.string.menu_revert, Toast.LENGTH_SHORT).show();
            } else if (mState == STATE_INSERT) {
                // We inserted an empty note, make sure to delete it
                deleteNote();
                Toast.makeText(this, string.menu_delete, Toast.LENGTH_SHORT).show();
            }

        }
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Take care of deleting a note.  Simply deletes the entry.
     */
    private final void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
            mText.setText("");
        }
    }
}
