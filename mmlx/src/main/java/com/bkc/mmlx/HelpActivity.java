package com.bkc.mmlx;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

public class HelpActivity extends Activity implements OnClickListener {

	static final String KEY_HELPURL = "HELPURL";
	SharedPreferences pref = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		
		// 設定
		pref = PreferenceManager.getDefaultSharedPreferences(this);

		settingView();
	}

	WebView webview;
	EditText edt;

	// ビューの設定
	void settingView()
	{
		edt = (EditText)findViewById(R.id.text_url);
		webview = (WebView)findViewById(R.id.help_web);
		Button btn = (Button)findViewById(R.id.btn_go);
		btn.setOnClickListener(this);
		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setUseWideViewPort(true);
		// webview.getSettings().setLoadWithOverviewMode(true);
		
		// リンクを自前処理
		webview.setWebViewClient(new WebViewClient());
		
		edt.setText(getHelpUrl());

		goUrl();
	}
	
	private String getHelpUrl()
	{
		String default_url = "http://nrtdrv.sakura.ne.jp/";
		String path = pref.getString(KEY_HELPURL, default_url);
		return path;
	}
	
	private void setHelpUrl(String url)
	{
		Editor edit = pref.edit();
		edit.putString(KEY_HELPURL, url);
		edit.commit();
	}
	
	// 表示を実行
	void goUrl()
	{
		String url = edt.getText().toString();
		webview.loadUrl(url);
		setHelpUrl(url);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.help, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View arg0) {
		goUrl();		
	}
}
