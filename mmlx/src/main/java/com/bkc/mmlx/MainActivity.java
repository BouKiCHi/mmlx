package com.bkc.mmlx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bkc.libformx.FileActivity;
import com.bkc.mmlx.R;

import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener 
{
    private final int REQUEST_PERMISSION = 1000;

    private static final String PRESET_JSON = "preset.json";
	private static final String UNTITLED_MML = "untitled.mml";
	private static final String USER_JSON = "user.json";
	private static final String TAG = "mmlx";

	MMLEdit edt = null;
	TextView tvPath = null;

	static SharedPreferences pref = null;
	static String editFile = null;
	
	static String presetTitle = null;
	static String templateText = null;
	static String compilerPackageName = null;
	static String manURL = null;

	
	// MMLキーボードの表示
	static boolean dispMMLkeyboard = true;

	static final String KEY_FILE = "filename";
	static final String KEY_FONT = "fontsize";
	static final String KEY_PKGN = "package_name";
	static final String KEY_SAVE = "save_folder";


	// アクティビティ戻り値
	private static final int REQ_FILEDIAG_SAVE = 0;
	private static final int REQ_FILEDIAG_OPEN = 1;
	private static final int REQ_FILEDIAG_IMPORT = 2;
	private static final int REQ_HELP = 3;
	private static final int REQ_DIRDIAG_FOLDER = 4;
		
	int[] chrBtnRes = 
	{ 
			R.id.bt0,
			R.id.bt1,
			R.id.bt2,
			R.id.bt3,
			R.id.bt4,
			R.id.bt5,
			R.id.bt6,
			R.id.bt7,
			R.id.bt8,
			R.id.bt9,
			
			R.id.bt2_0,
			R.id.bt2_1,
			R.id.bt2_2,
			R.id.bt2_3,
			R.id.bt2_4,
			R.id.bt2_5,
			R.id.bt2_6,
			R.id.bt2_7,
			R.id.bt2_8,
			R.id.bt2_9,

			
			R.id.bt3_0,
			R.id.bt3_1,
			R.id.bt3_2,
			R.id.bt3_3,
			R.id.bt3_4,
			R.id.bt3_5,
			R.id.bt3_6,
			R.id.bt3_7,
			R.id.bt3_8,
			R.id.bt3_9,
			
	};
	
	int[] SpBtnRes = 
	{
			R.id.bt_space,
			R.id.bt_back,
			R.id.bt_delete,
			R.id.bt_left,
			R.id.bt_right,
			R.id.bt_up,
			R.id.bt_down,
			R.id.bt_fav,
			R.id.bt_hdr,
			R.id.bt_cmd,
			R.id.bt_return			
	};
	InputMethodManager imm = null;

	private boolean needRefresh = false;
	
	ArrayList<HashMap<String,Object>> cmdlist = null;
	ArrayList<HashMap<String,Object>> hdrlist = null;
	ArrayList<HashMap<String,Object>> favlist = null;

	
	// アクティビティの作成
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		// ビュー関連
		setContentView(R.layout.activity_main);		
		edt = (MMLEdit)findViewById(R.id.edit1);
		setClickListener();

        // Android 6, API 23以上でパーミッシンの確認
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission();
        }

        // 設定関連
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		doMakeDefaultList();

		// パス表示用View
		tvPath = (TextView)findViewById(R.id.m_path);

		imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
	
		// MMLキーボードの設定
		setDispMMLKeyboard(true);
		needRefresh = true;
				
		// プリセットのJSONを読み出す
		doReadPreset();
		doImportFile(new File(getFilesDir(), USER_JSON).getPath(), true);
		
		if (presetTitle != null)
			setTitle(getString(R.string.app_name) + " [" + presetTitle + "]");
		
		edt.setTextSize(getEditorFontSize());
	}

	// 許可の確認
	private void checkPermission() {
		if ((ActivityCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
				PackageManager.PERMISSION_GRANTED)) return;

        requestLocationPermission();
	}


    // 許可を求める
    private void requestLocationPermission() {
	    String[] Permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this, Permissions, REQUEST_PERMISSION);

        } else {
            Toast toast =
                    Toast.makeText(this, "アプリ実行に許可が必要です", Toast.LENGTH_SHORT);
            toast.show();

            ActivityCompat.requestPermissions(this,
                    Permissions,
                    REQUEST_PERMISSION);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Permission: "+permissions[0]+ " was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }

	// クリックリスナーの設定
	private void setClickListener() 
	{
		for (int i = 0; i < chrBtnRes.length; i++)
		{
			Button btn = (Button)findViewById(chrBtnRes[i]);
			btn.setOnClickListener(this);
		}

		for (int i = 0; i < SpBtnRes.length; i++)
		{
			Button btn = (Button)findViewById(SpBtnRes[i]);
			btn.setOnClickListener(this);
		}
	}
	
	// ユーザー定義ファイルの削除
	void deleteUserJSON()
	{
		File file = new File(getFilesDir(), USER_JSON);
		if (file.exists()) 
			file.delete();
	}
	
	// 新規作成
	void makeNewFile()
	{
		String path = getSaveFolder() + File.separator + UNTITLED_MML ;
		setEditFile(path);
		
		String text = templateText;
		if (text == null)
			text = loadTemplateFile();
		
		if (text != null)
			edt.setText(text);
		
		tvPath.setText(getEditFile());
	}
	
	// セーブする
	void doSave()
	{
		saveEditFile(getEditFile());
	}
	
	// 変更を破棄する
	void doDiscard()
	{
		loadEditFile(getEditFile());
	}

	// アクティビティを復帰する
	@Override
	protected void onResume() 
	{
		super.onResume();

		if (!needRefresh)
			return;
		
		needRefresh = false;
		
		loadEditFile(getTempFile());
		tvPath.setText(getEditFile());
		
		// 保存ファイル名の設定
		if (getEditFile().equals(""))
		{
			makeNewFile();
		}
	}
	
	// 一時停止する
	@Override
	protected void onPause() 
	{
		saveEditFile(getTempFile());
		super.onPause();
	}
	
	// キーボードの表示
	private void setDispMMLKeyboard(boolean flag)
	{
		LinearLayout keyWrap = ((LinearLayout)findViewById(R.id.keyWrap));
		if (flag)
		{
			dispMMLkeyboard = true;
			keyWrap.setVisibility(View.VISIBLE);
			
		    getWindow().setFlags(
		    		WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
		            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);			
			
		}
		else
		{
			dispMMLkeyboard = false;
			keyWrap.setVisibility(View.GONE);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
		}
	}
	
	// 一時ファイルの取得
	private String getTempFile()
	{
		String path = new File(getFilesDir(), "temp.mml").getPath();
		return path;
	}

	// 編集ファイル
	private String getEditFile()
	{
		String path = pref.getString(KEY_FILE, "");
		return path;
	}
	
	// 現在の編集ファイルの設定
	private void setEditFile(String path)
	{
		Log.i(getPackageName(),"path: "+path);
		Editor edit = pref.edit();
		edit.putString(KEY_FILE, path);
		edit.commit();
	}
	
	// 保存フォルダ
	private String getSaveFolder()
	{
		String path = pref.getString(KEY_SAVE, Environment.getExternalStorageDirectory().toString());
		return path;
	}
	
	// 保存フォルダの設定
	private void setSaveFolder(String path)
	{
		Log.i(TAG, "SaveFolder:" + path);
		Editor edit = pref.edit();
		edit.putString(KEY_SAVE, path);
		edit.commit();
	}
	
	
	
	// フォントサイズの取得
	private int getEditorFontSize()
	{
		return pref.getInt(KEY_FONT, 20);
	}
	
	private void setEditorFontSize(int size)
	{
		Editor edit = pref.edit();
		edit.putInt(KEY_FONT, size);
		edit.commit();
	}
	
	// 現在のコンパイラのパッケージ名を取得
	private String getIntentPackage()
	{
		return pref.getString(KEY_PKGN, "");
	}

	// 現在のコンパイラのパッケージ名を設定	
	private void setIntentPackage(String name)
	{
		Editor edit = pref.edit();
		edit.putString(KEY_PKGN, name);
		edit.commit();
	}
	
	// 現在のオンラインヘルプURLを設定	
	private void setManualURL(String name)
	{
		Editor edit = pref.edit();
		edit.putString(HelpActivity.KEY_HELPURL, name);
		edit.commit();
	}
	
	// 編集ファイルの保存
	private void saveEditFile(String path)
	{
		OutputStreamWriter osw;
		
		Log.i(TAG, "edit:" + path);
		try {
			String text = edt.getText().toString();

			// LF -> CRLF
			text = text.replace("\n","\r\n");
			
			// remove NULL
			text = text.replace("\0","");
			
			FileOutputStream os = new FileOutputStream(new File(path));
		    osw = new OutputStreamWriter(os, "SJIS");
			osw.write(text);
			osw.close();
			os.close();
			showMessage("Saved!!");

		} catch (FileNotFoundException e) {
            showMessage("Failed to Save!(NotFound)");
			e.printStackTrace();
		} catch (IOException e) {
            showMessage("Failed to Save!(IO Exception)");
			e.printStackTrace();
		}
	}
	
	// テンポラリファイルの削除
	private void removeTempFile()
	{
		File file = new File(getTempFile());
		if (file.exists())
			file.delete();
	}
	
	// データを入力ストリームより読み込む
	private String loadDataFromIS(InputStream is, int len)
	{
		try {
			InputStreamReader in = 
				new InputStreamReader(is,"SJIS");

			int pos = 0;
			char [] data = new char[(int) len];			
						
			while(len > 0)
			{
				int buflen = in.read(data, pos, (int) len);
				if (buflen < 0)
					break;				
				len -= buflen;
				pos += buflen;
			}
			in.close();

			String text = String.valueOf(data);
			// CRLF -> LF
			text = text.replace("\r\n","\n");
			// CR -> LF
			text = text.replace("\r","\n");
			
			// remove NULL
			text = text.replace("\0","");
			
			return text;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	// テンプレートの読み込み
	private String loadTemplateFile()
	{
		int len = 0;

		String text = null;
		
		// read template
		InputStream is = getResources().openRawResource(R.raw.mml);
		try {
			if (is != null)
			{
				len = is.available();
				text = loadDataFromIS(is, len);
				is.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return text;		
	}

	// 編集ファイルの読み込み
	private void loadEditFile(String path) 
	{
		int len = 0;
		InputStream is = null;
		File file = new File(path);
		
		if (path.equals("") || !file.exists())
		{
			return;
		}
		else
		{
			// read file
			len = (int)file.length();
			try {
				is = new FileInputStream(path);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		if (is != null)
		{
			String text = loadDataFromIS(is, len);
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			edt.setText(text);			
		}
	}

    // 更新情報表示
    private void dispInfoDiag()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        sb.append(getString(R.string.app_name)).append(" ")
        .append(getString(R.string.app_version)).append("\n\n");
        
        try {
            Resources res = getResources();
            InputStream text = res.openRawResource(R.raw.changelog);
            
            byte[] data = new byte[text.available()];
            text.read(data);
            text.close();
            
            sb.append(new String(data));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        builder.setMessage(sb)
        .setCancelable(false)
        .setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog,int id)
            {
                dialog.cancel();
            }
        });
        
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    // ファイルダイアログの表示
    void doFileDiag(String path, boolean flagSave, int result)
    {
		Intent i;

		i = new Intent(this, FileActivity.class);
		i.setDataAndType(Uri.fromFile(new File(path)), "text/mml");
		i.putExtra(FileActivity.SAVEMODE, flagSave);
		i.putExtra(FileActivity.DIRMODE, false);
		startActivityForResult(i, result);
    }
    
    // ディレクトリダイアログの表示
    void doDirDiag(String path, int result)
    {
		Intent i;

		i = new Intent(this, FileActivity.class);
		i.setDataAndType(Uri.fromFile(new File(path)), "text/mml");
		i.putExtra(FileActivity.SAVEMODE, false);
		i.putExtra(FileActivity.DIRMODE, true);
		startActivityForResult(i, result);
    }
    
    // ヘルプの表示
    void doHelpDiag(int result)
    {
		Intent i;

		if (manURL != null)
			setManualURL(manURL);
		
		i = new Intent(this, HelpActivity.class);
		startActivityForResult(i, result);
    }

    // リソースからリストを作成
    ArrayList<HashMap<String,Object>> makeDefaultFromRes(int resid)
    {
		String[] rows = getResources().getStringArray(resid); 

    	ArrayList<HashMap<String,Object>> list = new ArrayList<HashMap<String,Object>>();
    	
    	for(int i = 0; i < rows.length; i++)
    	{
        	HashMap<String, Object> map = new HashMap<String, Object>();

			map.put("text", rows[i]);
			map.put("help", "help for " + rows[i]);

			list.add(map);
    	}
    	
    	return list;
    }
    
    // デフォルトリストの作成
    void doMakeDefaultList()
    {
    	cmdlist = makeDefaultFromRes(R.array.cmd);
    	hdrlist = makeDefaultFromRes(R.array.hdr);
    	favlist = makeDefaultFromRes(R.array.fav);
    }

    // JSONから文字列に変換する
    private String readJSONToString(byte[] data, String stringName)
    {    	
	    try {
			// 読み込み
			String json = new String(data);
			JSONObject jsonObject = new JSONObject(json);
 
			// 存在しない？
			if (!jsonObject.has(stringName))
				return null;
			
			// データ追加
			return jsonObject.getString(stringName);
			
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
    }
    
    // JSONからリストを読み出す
    private ArrayList<HashMap<String,Object>> readJSONToList(byte[] data, String listname)
    {
    	ArrayList<HashMap<String,Object>> list = new ArrayList<HashMap<String,Object>>();
    	
	    try {
			// 読み込み
			String json = new String(data);
			JSONObject jsonObject = new JSONObject(json);
 
			// 存在しない？
			if (!jsonObject.has(listname))
				return null;

			// データ追加
			JSONArray jsonArray = jsonObject.getJSONArray(listname);
			
			for (int i = 0; i < jsonArray.length(); i++) {
			    JSONObject obj = jsonArray.getJSONObject(i);
			    
				HashMap<String, Object> map = new HashMap<String, Object>();

				map.put("text",obj.getString("text"));
				map.put("help",obj.getString("help"));

				list.add(map);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
    	return list;
    }
    
    // データをファイルより読み込む
    byte[] readDataFromFile(File file)
    {
	    byte[] data = null;
		try {
			FileInputStream is = new FileInputStream(file);
			int size = is.available();
			data = new byte[size];
			is.read(data);
			is.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	    
	    return data;
    }
    
    // アセットのファイルからデータを読み出す
    byte[] readDataFromAsset(String file)
    {
	    byte[] data = null;
		try {
			AssetManager as = getResources().getAssets();   
			InputStream is = as.open(file);
			int size = is.available();
			data = new byte[size];
			is.read(data);
			is.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	    
	    return data;
    } 
    
    // データをJSONからインポートする
    private int doImportData(byte[] data)
    {
    	String str = null;
    	if (data == null)
    		return -1;
    	
	    ArrayList<HashMap<String,Object>> list = null;
	    
	    str = readJSONToString(data, "preset");
	    if (str != null)
	    	presetTitle = str;

	    str = readJSONToString(data, "packageName");
	    if (str != null)
	    	compilerPackageName = str;
	    
	    str = readJSONToString(data, "templateText");
	    if (str != null)
	    	templateText = str;
	    
	    str = readJSONToString(data, "manURL");
	    if (str != null)
	    	manURL = str;
	    
	    list = readJSONToList(data, "favlist");
	    if (list != null)
	    	favlist = list;
	    list = readJSONToList(data, "cmdlist");
	    if (list != null)
	    	cmdlist = list;
	    list = readJSONToList(data, "hdrlist");
	    if (list != null)
	    	hdrlist = list;
	    
	    return 0;
    }
    
    // 定義をインポートする
	private int doImportFile(String path, boolean silent) 
	{		
		File file = new File(path);
		
		if (!file.exists())
		{
			if (!silent)
				Toast.makeText(this, R.string.file_not_found_, Toast.LENGTH_LONG).show();
			return -1;
		}
		byte[] data = readDataFromFile(file);
		int ret = doImportData(data);
		if (ret < 0)
		{
			if (!silent)
				Toast.makeText(this, "Import failed!", Toast.LENGTH_LONG).show();
			return -1;
		}
		else
		{
			if (!silent)
				Toast.makeText(this, "Imported!", Toast.LENGTH_LONG).show();
		}
		return 0;
	}

	private void showMessage(String Text) {
        Toast.makeText(this, Text, Toast.LENGTH_LONG).show();
    }
	
	private void doReadPreset() 
	{
		byte[] data = readDataFromAsset(PRESET_JSON);
		doImportData(data);
	}
		
	JSONArray makeJSONArrayFromList(ArrayList<HashMap<String,Object>> list)
	{
		JSONArray jsonArary = new JSONArray();

		try {
			// データの作成
			JSONObject jsonOneData;
			
			for(int i = 0; i < list.size(); i++)
			{
				HashMap<String,Object> map = list.get(i);
				
				jsonOneData = new JSONObject();
				jsonOneData.put("text", map.get("text"));
				jsonOneData.put("help", map.get("help"));
				jsonArary.put(jsonOneData);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return jsonArary;
	}
	
	// 設定ファイルのエクスポート
	void doExportFile(String path, boolean silent)
	{
		File file = new File(path);
		
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("favlist", makeJSONArrayFromList(favlist));
			jsonObject.put("cmdlist", makeJSONArrayFromList(cmdlist));
			jsonObject.put("hdrlist", makeJSONArrayFromList(hdrlist));
			
			// ファイル出力
			FileWriter fw;

			fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);
			pw.write(jsonObject.toString(4));
			pw.close();

		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!silent)
			Toast.makeText(this, "Exported!", Toast.LENGTH_LONG).show();
		
	}
	
	// 外部ストレージのファイル名を取得する
	String extFile(String filename)
	{
		return getSaveFolder() + File.separator + filename;
	}

	// エクスポートする
	void doExport() {
		doExportFile(extFile(getString(R.string.export_json)), false);		
	}
	
	// コンパイルする
	void doCompile() {
		String editpath = getEditFile();

		doSave();
		Intent i = new Intent(Intent.ACTION_EDIT);
		
		// コンパイラの設定値を参照
		compilerPackageName = getIntentPackage();
		
		if (compilerPackageName != null && compilerPackageName != "")
			i.setPackage(compilerPackageName);
		
		i.setDataAndType(Uri.fromFile(new File(editpath)), "text/*");
		startActivity(i);
	}
	
	// キーボードイベントを実行する
	public boolean dispatchKeyEvent(KeyEvent e) {
	    if (e.getAction() == KeyEvent.ACTION_DOWN) {
	        int code = e.getKeyCode();
	        int meta = e.getMetaState();

			// Log.d(TAG, "code =" + code + " meta = " + meta);
			
	        // 0x3000 = CTRL
			if (code == KeyEvent.KEYCODE_S && (meta & 0x3000) == 0x3000)
			{
				doCompile();
				return false;
			}
	    }
	    return super.dispatchKeyEvent(e);
	}
	
	// メニューの選択
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		String editpath = getEditFile();
		
		switch(item.getItemId())
		{
		case R.id.menu_saveas:
			doFileDiag(editpath, true, REQ_FILEDIAG_SAVE);
		break;
		case R.id.menu_open:
			doFileDiag(editpath, false, REQ_FILEDIAG_OPEN);
		break;
		case R.id.menu_compile:
			doCompile();
		break;
		case R.id.menu_import:
			doFileDiag(extFile(getString(R.string.export_json)), false, REQ_FILEDIAG_IMPORT);
		break;
		case R.id.menu_export:
			doExport();
		break;
		case R.id.menu_reset:
			doReadPreset();
			deleteUserJSON();
			Toast.makeText(this, "Reset!", Toast.LENGTH_LONG).show();
		break;
		
		case R.id.menu_fav:
			doListDiagEx(favlist);
		break;
		case R.id.menu_cmd:
			doListDiagEx(cmdlist);
		break;
		case R.id.menu_hdr:
			doListDiagEx(hdrlist);
		break;
		case R.id.menu_keyboard:
			setDispMMLKeyboard(!dispMMLkeyboard);
		break;
		case R.id.menu_changelog:
			dispInfoDiag();
		break;
		case R.id.menu_newfile:
			makeNewFile();
		break;
		case R.id.menu_save:
			doSave();
		break;
		case R.id.menu_discard:
			doDiscard();
		break;
		case R.id.menu_font_small:
			edt.setTextSize(5);
			setEditorFontSize(5);
		break;
		case R.id.menu_font_normal:
			edt.setTextSize(10);
			setEditorFontSize(10);
		break;
		case R.id.menu_font_large:
			edt.setTextSize(20);
			setEditorFontSize(20);
		break;
		case R.id.menu_select_compiler:
			doIconListDiag();
		break;
		case R.id.menu_save_folder:
			doDirDiag(getSaveFolder(), REQ_DIRDIAG_FOLDER);
		break;
		
		case R.id.menu_help:
			doHelpDiag(REQ_HELP);
		break;
		}
		return true;
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		
		if (requestCode == REQ_FILEDIAG_SAVE)
		{
				if (resultCode == RESULT_OK)
				{
					String path = data.getData().getPath();
					setEditFile(path);
				}
		} else
		if (requestCode == REQ_FILEDIAG_OPEN)
		{
				if (resultCode == RESULT_OK)
				{
					String path = data.getData().getPath();
					removeTempFile();
					setEditFile(path);
					loadEditFile(path);
				}
		} else		
		if (requestCode == REQ_FILEDIAG_IMPORT)
		{
				if (resultCode == RESULT_OK)
				{
					String path = data.getData().getPath();
					
					// インポートに成功した場合、ユーザー定義として書き出す
					if (doImportFile(path, false) == 0)
					{
						setIntentPackage(compilerPackageName);
						doExportFile(new File(getFilesDir(), USER_JSON).getPath(), true);
					}
				}
		} else 
		if (requestCode == REQ_DIRDIAG_FOLDER) 
		{
			if (resultCode == RESULT_OK)
			{
				String path = data.getData().getPath();
				setSaveFolder(path);
			}
		}
		
		tvPath.setText(getEditFile());
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	private void insertText(CharSequence cs)
	{
		Editable edit = edt.getText();
		
		int st = edt.getSelectionStart();
		edit.insert(st, cs);
	}
	
	private void delText()
	{
		Editable edit = edt.getText();
		
		int st = edt.getSelectionStart();
		int ed = edt.getSelectionEnd();
		int len = edit.length();
		
		// Log.d(TAG,"st:" + st + " ed:" + ed);
		if (st == ed && st >= 0 && st + 1 <= len)
			edit.delete(st, st + 1);
		else
			if (st >= 0)
			edit.delete(st, ed);
	}
	
	private void bsText()
	{
		Editable edit = edt.getText();
		
		int st = edt.getSelectionStart();
		int ed = edt.getSelectionEnd();
		
		// Log.d(TAG,"st:" + st + " ed:" + ed);
		if (st == ed && st > 0)
			edit.delete(st-1, st);
		else
			if (st >= 0)
			edit.delete(st, ed);
	}

	@Override
	public void onClick(View arg0) 
	{		
		Button btn = (Button)arg0;
		
		int id = btn.getId();
		int key = -1;
		
		for (int i = 0; i < chrBtnRes.length; i++)
		{
			if (chrBtnRes[i] == id)
			{
				insertText(btn.getText());
			}
		}

		/* special button */
		switch (id)
		{
			case R.id.bt_space:
				insertText(" ");
			break;
			case R.id.bt_back:
				bsText();
			break;
			case R.id.bt_delete:
				delText();
			break;
			case R.id.bt_left:
				key = KeyEvent.KEYCODE_DPAD_LEFT;
			break;
			case R.id.bt_right:
				key = KeyEvent.KEYCODE_DPAD_RIGHT;
			break;
			case R.id.bt_up:
				key = KeyEvent.KEYCODE_DPAD_UP;
			break;
			case R.id.bt_down:
				key = KeyEvent.KEYCODE_DPAD_DOWN;
			break;
			case R.id.bt_return:
				key = KeyEvent.KEYCODE_ENTER;
			break;
			case R.id.bt_fav:
				doListDiagEx(favlist);
			break;
			case R.id.bt_cmd:
				doListDiagEx(cmdlist);
			break;
			case R.id.bt_hdr:
				doListDiagEx(hdrlist);
			break;
		}
		
		if (key >= 0)
		{
			KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, key);
			dispatchKeyEvent(event);
		}
		
	}
	
	// バインダー
	class IconListViewBinder implements SimpleAdapter.ViewBinder 
    {
		public boolean setViewValue(View view, Object data,
				String textRepresentation) {
			if ((view instanceof ImageView) & (data instanceof Drawable)) {
				ImageView iv = (ImageView) view;
				Drawable d = (Drawable) data;
				iv.setImageDrawable(d);
				return true;
			}
			return false;
		}
    }
	
	private AlertDialog il_dlg;
	
	//　アイコンダイアログの表示
	private void doIconListDiag() 
	{

		ListView lv = new ListView(this);
				
    	ArrayList<HashMap<String,Object>> list = new ArrayList<HashMap<String,Object>>();
    	
    	 PackageManager pm = this.getPackageManager();
    	 
         Intent intent = new Intent();
         intent.setAction(Intent.ACTION_EDIT);
         intent.setType("text/mml");
                  
         //カテゴリとアクションに一致するアクティビティの情報を取得する
         final List<ResolveInfo> appInfoList = pm.queryIntentActivities(intent, 0);
         
         for(ResolveInfo ri : appInfoList)
         {
         	HashMap<String, Object> map = new HashMap<String, Object>();

             // itemData = new CustomData();
  
         	String text = "";
             if(ri.loadLabel(pm).toString()!=null)
                 text = ri.loadLabel(pm).toString();
  
            Drawable icon = null;
             
			try {
				icon = pm.getApplicationIcon(ri.activityInfo.packageName);
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			
			map.put("package", ri.activityInfo.packageName);
 			map.put("text", text);
 			if (icon == null)
 	 			map.put("icon", R.drawable.ic_launcher);
 			else
 				map.put("icon", icon);
  
			list.add(map);

         }
    	/*
    	// データの作成
    	for(int i = 0; i < 5; i++)
    	{
        	HashMap<String, Object> map = new HashMap<String, Object>();

			map.put("text", String.format("row=%d",i));
			map.put("icon", R.drawable.ic_launcher);

			list.add(map);
    	}
    	*/
		
         SimpleAdapter adpt = new SimpleAdapter(
					this,
					list,
					R.layout.icon_list,
					new String[] {"text", "icon"},
					new int[] {R.id.il_text1, R.id.il_icon1}
			);
         
         adpt.setViewBinder(new IconListViewBinder());
		// アダプタのセット
		lv.setAdapter(adpt);
		
		il_dlg = new AlertDialog.Builder(this).setView(lv).create();
        
		// リスナー選択
		lv.setOnItemClickListener(new OnItemClickListener() 
		{

			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				ListView lv = (ListView)parent;
				HashMap<String,Object> map = (HashMap<String, Object>)(lv.getItemAtPosition(pos));
				
				String package_name = (String)map.get("package");
				Log.d(TAG, "package_name = " + package_name);
				setIntentPackage(package_name);
				il_dlg.dismiss();
			}
		});
        
		il_dlg.show();
	}
	
	private void doListDiagEx(ArrayList<HashMap<String,Object>> list) 
	{
		AlertDialog dlg;
		ListView lv = new ListView(this);
		
		// アダプタのセット
		lv.setAdapter(
			new SimpleAdapter(
					this,
					list,
					android.R.layout.simple_list_item_2,
					new String[] {"text", "help"},
					new int[] {android.R.id.text1, android.R.id.text2}
			));
				
		lv.setOnItemClickListener(new OnItemClickListener() 
		{

			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				ListView lv = (ListView)parent;
				HashMap<String,Object> map = (HashMap<String, Object>)(lv.getItemAtPosition(pos));
				insertText((String)map.get("text"));
			}
		});
		
        dlg = new AlertDialog.Builder(this).setView(lv).create();
        dlg.show();
	}

}
