package com.example.bmd_filemanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuItemImpl;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private TextView pathTextView;
    private BottomNavigationView bottomNavigationView;
    private Menu bottomNavMenu;

    private static final int REQUEST_PERMISSIONS = 1234;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PERMISSIONS_COUNT = 2;

    private boolean isFileManagerInitialized = false;
    private boolean[] selection;
    private File[] files;
    private File dir;
    private ListAdapter listAdapter;
    private String rootPath;
    private String currentPath;

    private boolean isBackPressedOnce = false;
    private boolean canRename = false;

    private int selectedPosition;

    private ArrayList<String> copyPath = new ArrayList<>();
    private ArrayList<String> cutPath = new ArrayList<>();

    private boolean isCutSelected = false;

    private DialogInterface.OnClickListener setNegativeButton = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.cancel();
            updateView();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pathTextView = findViewById(R.id.path_text_view);
    }

    private boolean arePermissionsDenied() {
        int p = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (p = 0; p < PERMISSIONS_COUNT; p++) {
                if (checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
        }

        if (!isFileManagerInitialized) {

            currentPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
            rootPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
            dir = new File(currentPath);

            pathTextView.setText(currentPath.substring(currentPath.lastIndexOf("/") + 1));

            bottomNavigationView = findViewById(R.id.bottom_navigation_view);
            bottomNavMenu = bottomNavigationView.getMenu();

            final ListView listView = findViewById(R.id.list_view);
            listAdapter = new ListAdapter();
            listView.setAdapter(listAdapter);

            updateView();

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (files[i].isDirectory()) {
                        currentPath = files[i].getAbsolutePath();
                        dir = new File(currentPath);
                        updateView();
                    }
                }
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    selection[i] = !selection[i];
                    listAdapter.setSelection(selection);
                    int count = 0;
                    for (int c = 0; c < selection.length; c++) {
                        if (selection[c]) {
                            bottomNavigationView.setVisibility(View.VISIBLE);
                            selectedPosition = c;
                            count++;
                        }
                        if (count == 0) {
                            bottomNavigationView.setVisibility(View.GONE);
                            canRename = false;
                            invalidateOptionsMenu();
                        } else if (count == 1) {
                            canRename = true;
                            invalidateOptionsMenu();
                        } else {
                            canRename = false;
                            invalidateOptionsMenu();
                        }
                    }
                    return true;
                }
            });

            bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.delete:
                            deleteDialog(files);
                            break;

                        case R.id.copy:
                            bottomNavMenu.getItem(0).setVisible(false);
                            bottomNavMenu.getItem(1).setVisible(false);
                            bottomNavMenu.getItem(2).setVisible(false);
                            bottomNavMenu.getItem(3).setVisible(true);
                            copyPasteFunction();
                            break;

                        case R.id.cut_file:
                            bottomNavMenu.getItem(0).setVisible(false);
                            bottomNavMenu.getItem(1).setVisible(false);
                            bottomNavMenu.getItem(2).setVisible(false);
                            bottomNavMenu.getItem(3).setVisible(true);
                            cutPasteFunction();
                            break;

                        case R.id.paste:
                            bottomNavMenu.getItem(0).setVisible(true);
                            bottomNavMenu.getItem(1).setVisible(true);
                            bottomNavMenu.getItem(2).setVisible(true);
                            bottomNavMenu.getItem(3).setVisible(false);
                            if(isCutSelected)
                                cutPasteFunction();
                            else
                                copyPasteFunction();
                            break;
                    }
                    return false;
                }
            });

            isFileManagerInitialized = true;
        } else {
            updateView();
        }
    }

    private void deleteDialog(File[] files) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Delete");
        builder.setMessage("Do you really want to delete?");
        final File[] files1 = files;

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                for (int c = 0; c < files1.length; c++) {
                    if (selection[c]) {
                        selection[c] = false;
                        deleteFileOrFolder(files1[c]);
                    }
                }
                updateView();
            }
        });

        builder.setNegativeButton("NO", setNegativeButton);

        builder.show();
    }

    private void deleteFileOrFolder(File fileOrFolder) {
        if (fileOrFolder.isDirectory()) {
            if (Objects.requireNonNull(fileOrFolder.list()).length == 0) {
                fileOrFolder.delete();
            } else {
                String[] files = fileOrFolder.list();
                assert files != null;
                for (String temp : files) {
                    File fileToDelete = new File(fileOrFolder, temp);
                    deleteFileOrFolder(fileToDelete);
                }
                if (Objects.requireNonNull(fileOrFolder.list()).length == 0) {
                    fileOrFolder.delete();
                }
            }
        } else {
            fileOrFolder.delete();
        }
    }

    private void createNewFolder() {
        AlertDialog.Builder createFolderDialog = new AlertDialog.Builder(this);
        createFolderDialog.setTitle("New Folder");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        createFolderDialog.setView(input);
        createFolderDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        final File newFolder = new File(currentPath + "/" + input.getText());
                        if (!newFolder.exists()) {
                            newFolder.mkdir();
                            updateView();
                        }
                    }
                });
        createFolderDialog.setNegativeButton("CANCEL", setNegativeButton);
        createFolderDialog.show();
    }

    private void copyPasteFunction() {
        if (copyPath.isEmpty()) {
            for (int i = 0; i < selection.length; i++) {
                if (selection[i]) {
                    copyPath.add(files[i].getAbsolutePath());
                }
            }
            Toast.makeText(this, "Copy Successful", Toast.LENGTH_SHORT).show();
        } else {
            for (String path : copyPath) {
                String destination = currentPath + "/" + path.substring(path.lastIndexOf('/') + 1);
                copy(new File(path), new File(destination));
            }
            copyPath = new ArrayList<>();
            Toast.makeText(this, "Paste Successful", Toast.LENGTH_SHORT).show();
        }
        updateView();
    }

    private void cutPasteFunction() {
        if (cutPath.isEmpty()) {
            isCutSelected = true;
            for (int i = 0; i < selection.length; i++) {
                if (selection[i]) {
                    cutPath.add(files[i].getAbsolutePath());
                }
            }
            Toast.makeText(this, "Cut Successful", Toast.LENGTH_SHORT).show();
        } else {
            for (String path : cutPath) {
                String destination = currentPath + "/" + path.substring(path.lastIndexOf('/') + 1);
                copy(new File(path), new File(destination));
            }
            cutPath = new ArrayList<>();
            isCutSelected = false;
            Toast.makeText(this, "Paste Successful", Toast.LENGTH_SHORT).show();
        }
        updateView();
    }

    private void copy(File src, File out) {
        if (!src.isDirectory()) {
            try {
                InputStream inputStream = new FileInputStream(src);
                OutputStream outputStream = new FileOutputStream(out);
                byte[] buf = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) >= 0) {
                    outputStream.write(buf, 0, len);
                }
                outputStream.close();
                inputStream.close();
                if(isCutSelected){
                    deleteFileOrFolder(src);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            File[] f = src.listFiles();
            assert f != null;
            final File newFolder = new File(out.getParent() + "/" + src.getAbsolutePath().substring(src.getAbsolutePath().lastIndexOf("/") + 1));
            if (!newFolder.exists()) {
                newFolder.mkdir();
                updateView();
            }
            Log.d("checking", src.getAbsolutePath() + "  :copy:  " + newFolder.getAbsolutePath());
            for (File file : f) {
                File newFile = new File(newFolder.getAbsolutePath() + file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf('/')));
                Log.d("checking", file.getAbsolutePath() + "  :copy:  " + newFile.getAbsolutePath());
                copy(file, newFile);
            }
            if(isCutSelected){
                deleteFileOrFolder(src);
            }
        }
    }

    private boolean canGoBack() {
        return !currentPath.equals(rootPath);
    }

    private void goBack() {
        if (canGoBack()) {
            currentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
            dir = new File(currentPath);
            updateView();
        }
    }

    private void renameFile() {
        AlertDialog.Builder renameDialog = new AlertDialog.Builder(this);
        renameDialog.setTitle("Rename");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        final String renamePath = files[selectedPosition].getAbsolutePath();
        input.setText(renamePath.substring(renamePath.lastIndexOf('/')));
        renameDialog.setView(input);
        renameDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                String s = new File(renamePath).getParent() + "/" + input.getText();
                File file = new File(s);
                new File(renamePath).renameTo(file);
                canRename = false;
                updateView();
            }
        });
        renameDialog.setNegativeButton("Cancel", setNegativeButton);
        renameDialog.show();
    }

    private void updateView() {
        pathTextView.setText(currentPath.substring(currentPath.lastIndexOf("/") + 1));
        files = dir.listFiles();
        List<String> filesList = new ArrayList<>();
        assert files != null;
        for (File file : MainActivity.this.files) {
            filesList.add(file.getAbsolutePath());
        }
        listAdapter.setData(filesList);
        selection = new boolean[files.length];
        listAdapter.setSelection(selection);
        for (boolean selected : selection) {
            if (selected || !copyPath.isEmpty() || !cutPath.isEmpty()) {
                bottomNavigationView.setVisibility(View.VISIBLE);
                break;
            }
            bottomNavigationView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS && grantResults.length > 0) {
            if (arePermissionsDenied()) {
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            } else {
                onResume();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (canRename) {
            menu.getItem(1).setVisible(true);
        } else {
            menu.getItem(1).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_folder:
                createNewFolder();
                return true;
            case R.id.back:
                goBack();
                return true;
            case R.id.rename:
                renameFile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (canGoBack()) {
            goBack();
            isBackPressedOnce = false;
        } else {
            if (!isBackPressedOnce) {
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                isBackPressedOnce = true;
            } else {
                super.onBackPressed();
            }
        }
    }
}