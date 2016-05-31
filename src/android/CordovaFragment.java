/*
 * Copyright (c) 2015 Moodstocks SAS and imactivate
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.moodstocks.phonegap.plugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaInterfaceImpl;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewImpl;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.ConfigXmlParser;

import uk.ac.ox.museums.hiddenmuseum.R;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.RelativeLayout;

// Code as per https://github.com/Adobe-Marketing-Cloud/app-sample-android-phonegap/wiki/Embed-Webview-in-Android-Fragment

public class CordovaFragment extends Fragment implements CordovaInterface {
	CordovaWebView myWebView;

	// public static CordovaFragment newInstance() {
	// CordovaFragment fragment = new CordovaFragment();
	// return fragment;
	// }

	protected CordovaPreferences preferences;
	protected String launchUrl;
	protected ArrayList<PluginEntry> pluginEntries;
	protected CordovaInterfaceImpl cordovaInterface;

	protected void loadConfig() {
		ConfigXmlParser parser = new ConfigXmlParser();
		parser.parse(getActivity());
		preferences = parser.getPreferences();
		preferences.setPreferencesBundle(getActivity().getIntent().getExtras());
		// preferences.copyIntoIntentExtras(getActivity());
		launchUrl = parser.getLaunchUrl();
		pluginEntries = parser.getPluginEntries();
		// Config.parser = parser;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		LayoutInflater localInflater = inflater.cloneInContext(new CordovaContext(getActivity(), this));
		View rootView = localInflater.inflate(R.layout.fragment_cordova, container, false);

		cordovaInterface = new CordovaInterfaceImpl(getActivity());
		if (savedInstanceState != null)
			cordovaInterface.restoreInstanceState(savedInstanceState);

		loadConfig();

		myWebView = new CordovaWebViewImpl(CordovaWebViewImpl.createEngine(getActivity(), preferences));
		myWebView.getView().setId(100);
		RelativeLayout.LayoutParams wvlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		myWebView.getView().setLayoutParams(wvlp);

		myWebView.getView().setBackgroundColor(Color.argb(1, 0, 0, 0));

		// fixes a bug in android 3.0 - 4.0.3 that causes an issue with
		// transparent webviews.
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB
				&& android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			myWebView.getView().setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
		}

		if (!myWebView.isInitialized()) {
			myWebView.init(cordovaInterface, pluginEntries, preferences);
		}
		cordovaInterface.onCordovaInit(myWebView.getPluginManager());

		// Config.init(getActivity());
		((RelativeLayout) rootView).addView(myWebView.getView());
		myWebView.loadUrl(launchUrl);

		return rootView;
	}

	// Plugin to call when activity result is received
	protected CordovaPlugin activityResultCallback = null;
	protected boolean activityResultKeepRunning;

	// Keep app running when pause is received. (default = true)
	// If true, then the JavaScript and native code continue to run in the
	// background
	// when another application (activity) is started.
	protected boolean keepRunning = true;

	private final ExecutorService threadPool = Executors.newCachedThreadPool();

	public Object onMessage(String id, Object data) {
		return null;
	}

	public void onDestroy() {
		super.onDestroy();
		if (myWebView.getPluginManager() != null) {
			myWebView.getPluginManager().onDestroy();
		}
	}

	@Override
	public ExecutorService getThreadPool() {
		return threadPool;
	}

	@Override
	public void setActivityResultCallback(CordovaPlugin plugin) {
		this.activityResultCallback = plugin;
	}

	public void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode) {
		this.activityResultCallback = command;
		this.activityResultKeepRunning = this.keepRunning;

		// If multitasking turned on, then disable it for activities that return
		// results
		if (command != null) {
			this.keepRunning = false;
		}

		// Start activity
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		CordovaPlugin callback = this.activityResultCallback;
		if (callback != null) {
			callback.onActivityResult(requestCode, resultCode, intent);
		}
	}

	private class CordovaContext extends ContextWrapper implements CordovaInterface {
		CordovaInterface cordova;

		public CordovaContext(Context base, CordovaInterface cordova) {
			super(base);
			this.cordova = cordova;
		}

		public void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode) {
			cordova.startActivityForResult(command, intent, requestCode);
		}

		public void setActivityResultCallback(CordovaPlugin plugin) {
			cordova.setActivityResultCallback(plugin);
		}

		public Activity getActivity() {
			return cordova.getActivity();
		}

		public Object onMessage(String id, Object data) {
			return cordova.onMessage(id, data);
		}

		public ExecutorService getThreadPool() {
			return cordova.getThreadPool();
		}
	}

}
