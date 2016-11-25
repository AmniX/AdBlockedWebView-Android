package com.amnix.adblockwebview.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amnix.adblockwebview.R;
import com.amnix.adblockwebview.presenter.WebViewPresenterImpl;
import com.amnix.adblockwebview.util.AdBlocker;
import com.amnix.adblockwebview.util.ClipboardUtils;
import com.amnix.adblockwebview.util.FileUtils;
import com.amnix.adblockwebview.util.PermissionUtils;

import java.util.HashMap;
import java.util.Map;


public class AdBlocksWebViewActivity extends AppCompatActivity implements WebViewPresenterImpl.View,
        View.OnClickListener, SwipeRefreshLayout.OnRefreshListener, DownloadListener, View.OnCreateContextMenuListener {


    private static final String TAG = "AdBlocksWebViewActivity";
    private static final int REQUEST_FILE_CHOOSER = 0;
    private static final int REQUEST_FILE_CHOOSER_FOR_LOLLIPOP = 1;
    private static final int REQUEST_PERMISSION_SETTING = 2;

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_COLOR = "clr";

    private String mUrl;
    private ValueCallback<Uri[]> filePathCallbackLollipop;
    private ValueCallback<Uri> filePathCallbackNormal;

    private WebViewPresenterImpl mPresenter;
    private DownloadManager mDownloadManager;
    private long mLastDownloadId;

    // Toolbar
    private TextView mTvTitle;
    private TextView mTvUrl;

    private CoordinatorLayout mCoordinatorLayout;
    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private WebView mWebView;

    // PopupWindow
    private PopupWindow mPopupMenu;
    private RelativeLayout mLlControlButtons;
    private AppCompatImageButton mBtnMore;
    private AppCompatImageButton mBtnBack;
    private AppCompatImageButton mBtnFoward;

    public static void init(Context context) {
        AdBlocker.init(context);
    }

    public static void startWebView(Activity context, @NonNull final String URL, @ColorInt final int toolbarColor) {
        Intent intent = new Intent(context, AdBlocksWebViewActivity.class);
        intent.putExtra(AdBlocksWebViewActivity.EXTRA_URL, URL);
        intent.putExtra(AdBlocksWebViewActivity.EXTRA_COLOR, toolbarColor);
        context.startActivity(intent);
        context.overridePendingTransition(com.amnix.adblockwebview.R.anim.open_translate, com.amnix.adblockwebview.R.anim.close_scale);
    }

    public static void startWebViewForResult(Activity context, @NonNull final String URL, @ColorInt final int toolbarColor, int requestCode) {
        Intent intent = new Intent(context, AdBlocksWebViewActivity.class);
        intent.putExtra(AdBlocksWebViewActivity.EXTRA_URL, URL);
        intent.putExtra(AdBlocksWebViewActivity.EXTRA_COLOR, toolbarColor);
        context.startActivityForResult(intent, requestCode);
        context.overridePendingTransition(com.amnix.adblockwebview.R.anim.open_translate, com.amnix.adblockwebview.R.anim.close_scale);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setStatusBarColor(getIntent().getIntExtra(EXTRA_COLOR, Color.BLACK));
        super.onCreate(savedInstanceState);

        //noinspection ConstantConditions

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        registerReceiver(mDownloadReceiver, filter);

        mUrl = getIntent().getStringExtra(EXTRA_URL);

        setContentView(R.layout.a_web_viewer);
        bindView();

        mPresenter = new WebViewPresenterImpl(this, this);
        mPresenter.validateUrl(mUrl);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mDownloadReceiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        WebView.HitTestResult result = mWebView.getHitTestResult();

        mPresenter.onLongClick(result);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mUrl != null) {
            outState.putString(EXTRA_URL, mUrl);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mUrl = savedInstanceState.getString(EXTRA_URL);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK == resultCode) {
            switch (requestCode) {
                case REQUEST_FILE_CHOOSER: {
                    if (filePathCallbackNormal == null) {
                        return;
                    }
                    Uri result = data == null ? null : data.getData();
                    filePathCallbackNormal.onReceiveValue(result);
                    filePathCallbackNormal = null;
                    break;
                }
                case REQUEST_FILE_CHOOSER_FOR_LOLLIPOP: {
                    if (filePathCallbackLollipop == null) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        filePathCallbackLollipop.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                    }
                    filePathCallbackLollipop = null;
                    break;
                }
                case REQUEST_PERMISSION_SETTING: {
                    mLastDownloadId = FileUtils.downloadFile(this, mDownloadUrl, mDownloadMimetype);
                    break;
                }
            }
        } else {
            switch (requestCode) {
                case REQUEST_PERMISSION_SETTING: {
                    Toast.makeText(this, R.string.write_permission_denied_message, Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void bindView() {
        // Toolbar
        mTvTitle = (TextView) findViewById(R.id.toolbar_tv_title);
        mTvUrl = (TextView) findViewById(R.id.toolbar_tv_url);
        findViewById(R.id.toolbar_root).setBackgroundColor(getIntent().getIntExtra(EXTRA_COLOR, Color.BLACK));

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.a_web_viewer_coordinatorlayout);
        mProgressBar = (ProgressBar) findViewById(R.id.a_web_viewer_pb);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.a_web_viewer_srl);
        mWebView = (WebView) findViewById(R.id.a_web_viewer_wv);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            webSettings.setDisplayZoomControls(false);
        }
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);
        webSettings.setDomStorageEnabled(true);

        mWebView.setWebChromeClient(new MyWebChromeClient());
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.setDownloadListener(this);
        mWebView.setOnCreateContextMenuListener(this);

        mBtnMore = (AppCompatImageButton) findViewById(R.id.toolbar_btn_more);

        //noinspection ConstantConditions
        findViewById(R.id.toolbar_btn_close).setOnClickListener(this);
        //noinspection ConstantConditions
        mBtnMore.setOnClickListener(this);

        // PopupWindow
        initPopupMenu();
    }


    private void initPopupMenu() {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(this).inflate(R.layout.popup_menu, null);
        mPopupMenu = new PopupWindow(this);
        mPopupMenu.setBackgroundDrawable(new BitmapDrawable());
        mPopupMenu.setContentView(view);
        mPopupMenu.setOutsideTouchable(true);
        mPopupMenu.setFocusable(true);

        mLlControlButtons = (RelativeLayout) view.findViewById(R.id.popup_menu_rl_arrows);
        mBtnBack = (AppCompatImageButton) view.findViewById(R.id.popup_menu_btn_back);
        mBtnFoward = (AppCompatImageButton) view.findViewById(R.id.popup_menu_btn_forward);

        mBtnBack.setOnClickListener(this);
        mBtnFoward.setOnClickListener(this);
        view.findViewById(R.id.popup_menu_btn_refresh).setOnClickListener(this);
        view.findViewById(R.id.popup_menu_btn_copy_link).setOnClickListener(this);
        view.findViewById(R.id.popup_menu_btn_open_with_other_browser).setOnClickListener(this);
        view.findViewById(R.id.popup_menu_btn_share).setOnClickListener(this);
    }

    @Override
    public void loadUrl(String url) {
        mWebView.loadUrl(url);
        mPresenter.onReceivedTitle("", mUrl);
    }

    @Override
    public void close() {
        finish();

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.open_scale, R.anim.close_translate);
    }

    @Override
    public void closeMenu() {
        mPopupMenu.dismiss();
    }

    @Override
    public void openMenu() {
        mPopupMenu.showAsDropDown(mBtnMore);
    }

    @Override
    public void setEnabledGoBackAndGoFoward() {
        mLlControlButtons.setVisibility(View.VISIBLE);
    }

    @Override
    public void setDisabledGoBackAndGoFoward() {
        mLlControlButtons.setVisibility(View.GONE);
    }

    @Override
    public void setEnabledGoBack() {
        mBtnBack.setEnabled(true);
    }

    @Override
    public void setDisabledGoBack() {
        mBtnBack.setEnabled(false);
    }

    @Override
    public void setEnabledGoFoward() {
        mBtnFoward.setEnabled(true);
    }

    @Override
    public void setDisabledGoFoward() {
        mBtnFoward.setEnabled(false);
    }

    @Override
    public void goBack() {
        mWebView.goBack();
    }

    @Override
    public void goFoward() {
        mWebView.goForward();
    }

    @Override
    public void copyLink(String url) {
        ClipboardUtils.copyText(this, url);
    }

    @Override
    public void showToast(Toast toast) {
        toast.show();
    }

    @Override
    public void openBrowser(Uri uri) {
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    @Override
    public void openShare(String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.menu_share)));
    }

    @Override
    public void setToolbarTitle(String title) {
        mTvTitle.setText(title);
    }

    @Override
    public void setToolbarUrl(String url) {
        mTvUrl.setText(url);
    }

    @Override
    public void onDownloadStart(String url) {
        onDownloadStart(url, null, null, "image/jpeg", 0);
    }


    @Override
    public void setProgressBar(int progress) {
        mProgressBar.setProgress(progress);
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
    }

    @Override
    public void openEmail(String email) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null));
        startActivity(Intent.createChooser(intent, getString(R.string.email)));
    }

    @Override
    public void openPopup(String url) {
        Intent intent = new Intent(this, AdBlocksWebViewActivity.class);
        intent.putExtra(AdBlocksWebViewActivity.EXTRA_URL, url);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PermissionUtils.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE == requestCode) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLastDownloadId = FileUtils.downloadFile(this, mDownloadUrl, mDownloadMimetype);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!shouldShowRequestPermissionRationale(permissions[0])) {
                        new AlertDialog.Builder(AdBlocksWebViewActivity.this)
                                .setTitle(R.string.write_permission_denied_title)
                                .setMessage(R.string.write_permission_denied_message)
                                .setNegativeButton(R.string.dialog_dismiss, null)
                                .setPositiveButton(R.string.dialog_settings, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                                    }
                                })
                                .show();
                    }
                }
            }
        }
    }

    private String mDownloadUrl;
    private String mDownloadMimetype;

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                String mimeType, long contentLength) {
        if (mDownloadManager == null) {
            mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        }
        Log.d(TAG, "onDownloadStart url: " + url);
        Log.d(TAG, "onDownloadStart userAgent: " + userAgent);
        Log.d(TAG, "onDownloadStart contentDisposition: " + contentDisposition);
        Log.d(TAG, "onDownloadStart mimeType: " + mimeType);

        mDownloadUrl = url;
        mDownloadMimetype = mimeType;

        boolean hasPermission = PermissionUtils.hasPermission(
                AdBlocksWebViewActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                PermissionUtils.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);

        if (hasPermission) {
            mLastDownloadId = FileUtils.downloadFile(this, url, mimeType);
        }
    }

    @Override
    public void onClick(View v) {
        mPresenter.onClick(v.getId(), mWebView.getUrl(), mPopupMenu);
    }

    @Override
    public void onRefresh() {
        mWebView.reload();
    }

    public class MyWebChromeClient extends WebChromeClient {

        // For Android < 3.0
        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            openFileChooser(uploadMsg, "");
        }

        // For Android 3.0+
        @SuppressWarnings("WeakerAccess")
        public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                    @SuppressWarnings("UnusedParameters") String acceptType) {
            filePathCallbackNormal = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(Intent.createChooser(i, getString(R.string.select_image)),
                    REQUEST_FILE_CHOOSER);
        }

        // For Android 5.0+
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            if (filePathCallbackLollipop != null) {
                filePathCallbackLollipop.onReceiveValue(null);
                filePathCallbackLollipop = null;
            }
            filePathCallbackLollipop = filePathCallback;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");

            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)),
                    REQUEST_FILE_CHOOSER_FOR_LOLLIPOP);

            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            AlertDialog dialog = new AlertDialog.Builder(AdBlocksWebViewActivity.this)
                    .setMessage(message)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            result.confirm();
                        }
                    })
                    .create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();

            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            AlertDialog dialog = new AlertDialog.Builder(AdBlocksWebViewActivity.this)
                    .setMessage(message)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            result.confirm();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            result.cancel();
                        }
                    })
                    .create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();

            return true;
        }

        @Override
        public void onProgressChanged(WebView view, int progress) {
            mPresenter.onProgressChanged(mSwipeRefreshLayout, progress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            mPresenter.onReceivedTitle(title, view.getUrl());
        }
    }

    public class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            mPresenter.onReceivedTitle(view.getTitle(), url);
            mPresenter.setEnabledGoBackAndGoFoward(view.canGoBack(), view.canGoForward());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.endsWith(".mp4")) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), "video/*");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                view.getContext().startActivity(intent);

                return true;
            } else if (url.startsWith("tel:") || url.startsWith("sms:") || url.startsWith("smsto:")
                    || url.startsWith("mms:") || url.startsWith("mmsto:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                view.getContext().startActivity(intent);

                return true;
            } else {
                return super.shouldOverrideUrlLoading(view, url);
            }
        }

        private Map<String, Boolean> loadedUrls = new HashMap<>();

        @SuppressWarnings("deprecation")
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            boolean ad;
            if (!loadedUrls.containsKey(url)) {
                ad = AdBlocker.isAd(url);
                loadedUrls.put(url, ad);
            } else {
                ad = loadedUrls.get(url);
            }
            return ad ? AdBlocker.createEmptyResource() :
                    super.shouldInterceptRequest(view, url);
        }
    }

    @Override
    public void onBackPressed() {
        mPresenter.onBackPressed(mPopupMenu, mWebView);
    }

    private BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                if (mDownloadManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        Uri downloadedUri = mDownloadManager.getUriForDownloadedFile(mLastDownloadId);
                        String mimeType = mDownloadManager.getMimeTypeForDownloadedFile(mLastDownloadId);

                        new NotifyDownloadedTask().execute(downloadedUri.toString(), mimeType);
                    }
                }
            } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
                Intent notiIntent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
                notiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(notiIntent);
            }
        }
    };

    private class NotifyDownloadedTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            if (params == null || params.length != 2) {
                return null;
            }
            String uriStr = params[0];
            String mimeType = params[1];
            String fileName = "";

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(mLastDownloadId);
            Cursor c = mDownloadManager.query(query);

            if (c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (DownloadManager.STATUS_SUCCESSFUL == status) {
                    fileName = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
                }
            }

            return new String[]{uriStr, fileName, mimeType};
        }

        @Override
        protected void onPostExecute(String[] results) {
            if (results != null && results.length == 3) {
                final String uriStr = results[0];
                final String fileName = results[1];
                final String mimeType = results[2];

                //noinspection WrongConstant
                Snackbar.make(mCoordinatorLayout, fileName + getString(R.string.downloaded_message),
                        Snackbar.LENGTH_LONG)
                        .setDuration(getResources().getInteger(R.integer.snackbar_duration))
                        .setAction(getString(R.string.open), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                                viewIntent.setDataAndType(Uri.parse(uriStr), mimeType);
                                mPresenter.startActivity(viewIntent);
                            }
                        })
                        .show();
            }
        }
    }
}
