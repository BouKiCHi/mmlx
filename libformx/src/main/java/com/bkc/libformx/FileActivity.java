package com.bkc.libformx;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.os.EnvironmentCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import com.bkc.libformx.R;

public class FileActivity extends Activity implements OnClickListener {
	// private static final String TAG = "FileActivity";

	public static final String DIRMODE = "fs_dirmode";
	public static final String SAVEMODE = "fs_savemode";
	public static final String EXTFILTER = "fs_extfilter";

	static boolean dirMode = false;

	static String sDir = null;
	static String sFile = null;
	static String sPath = null;
	static String sExt = null;

	TextView tvDir = null;
	TextView tvText = null;
	ListView lvFiles = null;

	List<String> filenames = null;
	List<File> fileobjs = null;

	ArrayList<HashMap<String, Object>> items;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// View関連初期化
		setContentView(R.layout.activity_file);
		tvDir = (TextView) findViewById(R.id.fs_dir);
		tvText = (TextView) findViewById(R.id.fs_text);

		// インテントの処理
		Intent intent = getIntent();
		sPath = intent.getData().getPath();

		if (sPath.equals("/")) {
			sDir = Environment.getExternalStorageDirectory().toString();
			sFile = getString(R.string.untitled_mml);
			sPath = sDir + File.separator + sFile;
		} else {
			sDir = new File(sPath).getParent().toString();
			sFile = new File(sPath).getName();
		}

		tvText.setText(sFile);

		dirMode = false;

		// ディレクトリモード
		if (intent.getBooleanExtra(DIRMODE, false)) {
			((TextView) findViewById(R.id.fs_info)).setText(R.string.fs_choose_folder);
			tvText.setText("");
			tvText.setEnabled(false);
			dirMode = true;
		} else if (intent.getBooleanExtra(SAVEMODE, true)) {
			// セーブテキストモード

			((TextView) findViewById(R.id.fs_info)).setText(R.string.fs_save_as);
		} else {
			((TextView) findViewById(R.id.fs_info)).setText(R.string.fs_open);
		}

		sExt = intent.getStringExtra(EXTFILTER);

		(findViewById(R.id.btn_fs_ok)).setOnClickListener(this);
		(findViewById(R.id.btn_fs_newdir)).setOnClickListener(this);
		(findViewById(R.id.btn_fs_sd)).setOnClickListener(this);
        (findViewById(R.id.btn_fs_home)).setOnClickListener(this);

		makeListView();

	}

	// ファイル比較
	class FileComparator implements java.util.Comparator<File> {
		@Override
		public int compare(File lhs, File rhs) {
			if (lhs.isDirectory() && rhs.isFile())
				return -1;

			if (lhs.isFile() && rhs.isDirectory())
				return 1;

			return lhs.compareTo(rhs);
		}
	}

	// ファイルフィルタ
	public FileFilter lfFilter(String ext) {
		final String _ext = ext;
		return new FileFilter() {
			public boolean accept(File file) {
				String name = file.getName().toLowerCase(Locale.US);

				if (name.startsWith("."))
					return false;

				if (file.isDirectory())
					return true;

				if (_ext == null)
					return true;

				boolean ret = name.endsWith(_ext);
				return ret;
			}
		};
	}

	// リストビューの作成
	void makeListView() {
		lvFiles = (ListView) findViewById(R.id.fs_lv);

		// ディレクトリを得る
		File f_dir = new File(sDir);
		tvDir.setText(f_dir.getAbsolutePath());

		fileobjs = new ArrayList<File>();

		File[] lf = f_dir.listFiles(lfFilter(sExt));

		Collections.addAll(fileobjs, lf);
		Collections.sort(fileobjs, new FileComparator());

		filenames = new ArrayList<String>();

		items = new ArrayList<HashMap<String, Object>>();

		String pdir = getString(R.string.parent_dir);

		HashMap<String, Object> map = new HashMap<String, Object>();

		// 親ディレクトリ
		filenames.add(pdir);

		map.put("title", pdir);
		map.put("image", R.drawable.ic_folder);

		items.add(map);

		// ファイルリスト作成
		if (fileobjs != null) {
			int length = fileobjs.size();
			for (int i = 0; i < length; i++) {
				map = new HashMap<String, Object>();

				File obj = fileobjs.get(i);

				int icon_res = R.drawable.ic_folder;
				if (!obj.isDirectory())
					icon_res = R.drawable.ic_mml;

				map.put("title", obj.getName());
				map.put("image", icon_res);

				items.add(map);
			}
		}

		// アダプタのセット
		lvFiles.setAdapter(
				new SimpleAdapter(
						this,
						items,
						R.layout.fileinfo,
						new String[]{"image", "title"},
						new int[]{R.id.imageView1, R.id.textView1}
				));

		lvFiles.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(
					AdapterView<?> parent, View view, int pos,
					long id) {

				if (pos == 0) {
					sDir = new File(sDir).getParent();
					makeListView();
					return;
				}

				File f = fileobjs.get(pos - 1);

				if (f.isDirectory()) {
					File n_dir = new File(sDir, f.getName());
					sDir = n_dir.toString();
					makeListView();
				} else {
					String item = f.getName();
					tvText.setText(item);
					lvFiles.requestFocus();
				}
			}
		});

		lvFiles.requestFocus();
	}

	@Override
	public void onClick(View v) {
		// OKボタン
		if (v.getId() == R.id.btn_fs_ok) {
			buttonOk(v);
			return;
		}
		if (v.getId() == R.id.btn_fs_newdir) {
			buttonNewDir(v);
			return;
		}
		if (v.getId() == R.id.btn_fs_sd) {
			buttonSd(v);
			return;
		}
        if (v.getId() == R.id.btn_fs_home) {
            buttonHome(v);
            return;
        }

    }

	private void buttonSd(View v) {
		String[] dirs = getExternalStorageDirectories();
		if (dirs == null || dirs.length == 0) return;

		sDir = dirs[0];
		makeListView();
	}

    private void buttonHome(View v) {
        sDir = Environment.getExternalStorageDirectory().toString();
        makeListView();
    }


    private void buttonOk(View v) {
		Intent i = new Intent();
		if (dirMode)
			i.setData(Uri.fromFile(new File(sDir) ));
		else
			i.setData(Uri.fromFile(new File(sDir, tvText.getText().toString()) ));
		setResult(RESULT_OK, i);
		finish();
	}

	private  void buttonNewDir(View v) {
		// 新規フォルダ

		final EditText edit = new EditText(this);
		edit.setText(R.string.fs_newfolder);

		AlertDialog.Builder abld = new AlertDialog.Builder(this);
		abld.setTitle(R.string.fs_newfolder_title);
		abld.setView(edit);
		abld.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				new File(sDir, edit.getText().toString()).mkdir();
				makeListView();
			}
		});
		abld.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		abld.create().show();
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public String[] getExternalStorageDirectories() {

		List<String> results = new ArrayList<>();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //Method 1 for KitKat & above
			File[] externalDirs = getExternalFilesDirs(null);

			for (File file : externalDirs) {
				String path = file.getPath().split("/Android")[0];

				boolean addPath = false;

				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					addPath = Environment.isExternalStorageRemovable(file);
				}
				else{
					addPath = Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(file));
				}

				if(addPath){
					results.add(path);
				}
			}
		}

		if(results.isEmpty()) { //Method 2 for all versions
			// better variation of: http://stackoverflow.com/a/40123073/5002496
			String output = "";
			try {
				final Process process = new ProcessBuilder().command("mount | grep /dev/block/vold")
						.redirectErrorStream(true).start();
				process.waitFor();
				final InputStream is = process.getInputStream();
				final byte[] buffer = new byte[1024];
				while (is.read(buffer) != -1) {
					output = output + new String(buffer);
				}
				is.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
			if(!output.trim().isEmpty()) {
				String devicePoints[] = output.split("\n");
				for(String vp: devicePoints) {
					results.add(vp.split(" ")[2]);
				}
			}
		}

		//Below few lines is to remove paths which may not be external memory card, like OTG (feel free to comment them out)
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			for (int i = 0; i < results.size(); i++) {
				if (!results.get(i).toLowerCase().matches(".*[0-9a-f]{4}[-][0-9a-f]{4}")) {
					results.remove(i--);
				}
			}
		} else {
			for (int i = 0; i < results.size(); i++) {
				if (!results.get(i).toLowerCase().contains("ext") && !results.get(i).toLowerCase().contains("sdcard")) {
					results.remove(i--);
				}
			}
		}

		String[] storageDirectories = new String[results.size()];
		for(int i=0; i<results.size(); ++i) storageDirectories[i] = results.get(i);

		return storageDirectories;
	}
}
