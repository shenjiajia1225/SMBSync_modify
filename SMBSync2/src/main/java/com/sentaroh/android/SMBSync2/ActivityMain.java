package com.sentaroh.android.SMBSync2;

/*
The MIT License (MIT)
Copyright (c) 2011 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sentaroh.android.SMBSync2.Log.LogFileListDialogFragment;
import com.sentaroh.android.SMBSync2.Log.LogUtil;
import com.sentaroh.android.Utilities.ContextButton.ContextButtonUtil;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.MessageDialogFragment;
import com.sentaroh.android.Utilities.Dialog.ProgressBarDialogFragment;
import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.MiscUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.SafManager;
import com.sentaroh.android.Utilities.StringUtil;
import com.sentaroh.android.Utilities.SystemInfo;
import com.sentaroh.android.Utilities.ThemeUtil;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.Widget.CustomViewPager;
import com.sentaroh.android.Utilities.Widget.CustomViewPagerAdapter;
import com.sentaroh.android.Utilities.Widget.NonWordwrapTextView;
import com.sentaroh.android.Utilities.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.sentaroh.android.SMBSync2.Constants.ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS;
import static com.sentaroh.android.SMBSync2.Constants.ACTIVITY_REQUEST_CODE_USB_STORAGE_ACCESS;
import static com.sentaroh.android.SMBSync2.Constants.APPLICATION_TAG;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_REQUEST_ARCHIVE_DATE_FROM_FILE;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_REQUEST_COPY;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_REQUEST_DELETE_DIR;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_REQUEST_DELETE_FILE;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_REQUEST_MOVE;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_RESP_CANCEL;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_RESP_NO;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_RESP_NOALL;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_RESP_YES;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_RESP_YESALL;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_KEY_STORE_ALIAS;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_SERIALIZABLE_FILE_NAME;
import static com.sentaroh.android.SMBSync2.Constants.SYNC_TASK_LIST_SEPARATOR;
import static com.sentaroh.android.SMBSync2.ScheduleConstants.SCHEDULER_INTENT_SET_TIMER;
import static com.sentaroh.android.SMBSync2.ScheduleConstants.SCHEDULER_INTENT_SET_TIMER_IF_NOT_SET;

@SuppressLint("NewApi")
public class ActivityMain extends AppCompatActivity {

    private boolean isTaskTermination = false; // kill is disabled(enable is kill by onDestroy)

    private Context mContext = null;
    private AppCompatActivity mActivity = null;

    private GlobalParameters mGp = null;
    private SyncTaskUtil mTaskUtil = null;

    private CommonUtilities mUtil = null;
    private CustomContextMenu ccMenu = null;

    private final static int START_STATUS_STARTING = 0;
    private final static int START_STATUS_COMPLETED = 1;
    private final static int START_STATUS_INITIALYZING = 2;
    private int mStartStatus = START_STATUS_STARTING;

    private final static int RESTART_BY_KILLED = 2;
    private final static int RESTART_BY_DESTROYED = 3;
    private int mRestoreType = 0;

    private ServiceConnection mSvcConnection = null;
    private CommonDialog mCommonDlg = null;
    private Handler mUiHandler = new Handler();

    private ActionBar mActionBar = null;

    private String mCurrentTab = null;

    private boolean enableMainUi = true;

    private boolean mSyncTaskListCreateRequired=false;

    private String mTabNameTask="Task", mTabNameSchedule="Schedule", mTabNameHistory="History", mTabNameMessage="Message";

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered.");
        out.putString("currentTab", mCurrentTab);
    }

    @Override
    protected void onRestoreInstanceState(Bundle in) {
        super.onRestoreInstanceState(in);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered.");
        mCurrentTab = in.getString("currentTab");
        if (mGp.activityIsFinished) mRestoreType = RESTART_BY_KILLED;
        else mRestoreType = RESTART_BY_DESTROYED;
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(new GlobalParameters().setNewLocale(base, true));
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        mActivity = ActivityMain.this;
        mContext = mActivity;
        mGp= GlobalWorkArea.getGlobalParameters(mContext);
        mGp.loadSettingsParms(mContext);
        setTheme(mGp.applicationTheme);
        mGp.themeColorList = CommonUtilities.getThemeColorList(mActivity);
//        getWindow().setNavigationBarColor(mGp.themeColorList.window_background_color_content);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

        mUtil = new CommonUtilities(mContext, "Main", mGp, getSupportFragmentManager());
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, " + "mStartStatus=" + mStartStatus +", settingScreenThemeLanguage="+mGp.settingScreenThemeLanguage);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setHomeButtonEnabled(false);
        if (mGp.settingFixDeviceOrientationToPortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        ccMenu = new CustomContextMenu(getResources(), getSupportFragmentManager());
        mCommonDlg = new CommonDialog(mActivity, getSupportFragmentManager());
        mTaskUtil = new SyncTaskUtil(mUtil, mActivity, mCommonDlg, ccMenu, mGp, getSupportFragmentManager());
        mGp.syncMessageListAdapter = new AdapterSyncMessage(mActivity, R.layout.msg_list_item_view, mGp.syncMessageList, mGp);

        mGp.syncScheduleList = ScheduleUtil.loadScheduleData(mActivity, mGp);

        if (mGp.syncTaskList == null) {
            mSyncTaskListCreateRequired=true;
            mGp.syncTaskList=new ArrayList<SyncTaskItem>();
            mGp.syncTaskAdapter = new AdapterSyncTask(mActivity, R.layout.sync_task_item_view, mGp.syncTaskList, mGp);
        } else {
            mSyncTaskListCreateRequired=false;
            mGp.syncTaskAdapter = new AdapterSyncTask(mActivity, R.layout.sync_task_item_view, mGp.syncTaskList, mGp);
        }

        if (mGp.syncHistoryList == null) mGp.syncHistoryList = mUtil.loadHistoryList();

        mGp.syncHistoryAdapter = new AdapterSyncHistory(mActivity, R.layout.sync_history_list_item_view, mGp.syncHistoryList);

        mTabNameTask=getString(R.string.msgs_tab_name_prof);
        mTabNameSchedule=getString(R.string.msgs_tab_name_schedule);
        mTabNameHistory=getString(R.string.msgs_tab_name_history);
        mTabNameMessage=getString(R.string.msgs_tab_name_msg);
        mCurrentTab = mTabNameTask;

        createTabView();
        initAdapterAndView();
        mGp.initJcifsOption(mContext);
        listSettingsOption();

        ScheduleUtil.sendTimerRequest(mContext, SCHEDULER_INTENT_SET_TIMER_IF_NOT_SET);
        setSyncTaskContextButtonHide();

        Calendar calendar = Calendar.getInstance();
//        calendar.set(2021, 5, 5, 0, 0);

        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        mUtil.addDebugMsg(1, "I", "date="+calendar.getTime().toString()+", no="+weekOfYear+", firstDay="+calendar.getFirstDayOfWeek());

    }

    @SuppressLint("NewApi")
    @Override
    protected void onStart() {
        super.onStart();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, " + "mStartStatus=" + mStartStatus+", mRestoreType="+ mRestoreType);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, " + "mStartStatus=" + mStartStatus+", mRestoreType="+ mRestoreType);

        if (mStartStatus == START_STATUS_COMPLETED) {
            mGp.safMgr.loadSafFile();
            setActivityForeground(true);
            ScheduleUtil.setSchedulerInfo(mActivity, mGp, mUtil);
            mGp.progressSpinSyncprof.setText(mGp.progressSpinSyncprofText);
            mGp.progressSpinMsg.setText(mGp.progressSpinMsgText);
        } else {
            if (mStartStatus==START_STATUS_STARTING) {
                mStartStatus=START_STATUS_INITIALYZING;
                final NotifyEvent ntfy_priv_key=new NotifyEvent(mContext);
                ntfy_priv_key.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        KeyStoreUtil.resetSavedKey(mContext);
                        processOnResumeForStart();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                        processOnResumeForStart();
                    }
                });

                Thread th=new Thread() {
                    @Override
                    public void run() {
                        final boolean corrupted=!isValidPrivateKey();
                        mUiHandler.post(new Runnable(){
                            @Override
                            public void run() {
                                if (corrupted) {
                                    mCommonDlg.showCommonDialog(true, "E",mContext.getString(R.string.msgs_smbsync_main_private_key_corrupted_title),
                                            mContext.getString(R.string.msgs_smbsync_main_private_key_corrupted_msg), ntfy_priv_key);
                                } else {
                                    processOnResumeForStart();
                                }
                            }
                        });
                    }
                };
                th.start();
            }
        }
    }

    private boolean isValidPrivateKey() {
        boolean result=false;
        try {
            KeyStoreUtil.getGeneratedPasswordNewVersion(mContext, SMBSYNC2_KEY_STORE_ALIAS);
            result=true;
        } catch (Exception e) {
            e.printStackTrace();
//                mUtil.addDebugMsg(1, "E", e.toString());
            String stm= MiscUtil.getStackTraceString(e);
            mUtil.addDebugMsg(1, "E", stm);
        }
        return result;
    }

    private void processOnResumeForStart() {
        mGp.syncTaskListView.setVisibility(ListView.INVISIBLE);
        final Dialog pd= CommonDialog.showProgressSpinIndicator(mActivity);
        pd.show();
        Thread th = new Thread() {
            @Override
            public void run() {
                if (mSyncTaskListCreateRequired) {
                    mSyncTaskListCreateRequired=false;
                    mUtil.addDebugMsg(1, "I", "Sync task list creation started.");
                    synchronized (mGp.syncTaskList) {
                        ArrayList<SyncTaskItem> task_list = SyncTaskUtil.createSyncTaskList(mContext, mGp, mUtil, false);
                        for(SyncTaskItem sti:task_list) {
                            mGp.syncTaskList.add(sti);
                            mUtil.addDebugMsg(1, "I", "Sync task list added, name="+sti.getSyncTaskName());
                        }
                    }
                    if (mGp.debuggable) {
                    }
                    mUtil.addDebugMsg(1, "I", "Sync task list creation ended.");
                } else {
                    mUtil.addDebugMsg(1, "I", "Sync task list was already created.");
                }
//                    ExportToSMBSync3.saveConfigListToExportFile(mContext, "/sdcard/smbsync2.xml", mGp.syncTaskList, mGp.syncScheduleList);
                mUiHandler.post(new Runnable(){
                    @Override
                    public void run() {
                        NotifyEvent stg__ntfy = new NotifyEvent(mContext);
                        stg__ntfy.setListener(new NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context context, Object[] objects) {
                                NotifyEvent svc_ntfy = new NotifyEvent(mContext);
                                svc_ntfy.setListener(new NotifyEventListener() {
                                    @Override
                                    public void positiveResponse(Context c, Object[] o) {
                                        mStartStatus= START_STATUS_COMPLETED;
                                        setMainListener();
                                        NotifyEvent app_pswd_ntfy = new NotifyEvent(mContext);
                                        app_pswd_ntfy.setListener(new NotifyEventListener() {
                                            @Override
                                            public void positiveResponse(Context c, Object[] o) {
                                                mGp.syncTaskListView.setVisibility(ListView.VISIBLE);
                                                if (mGp.syncTaskList.size()==0) mGp.syncTaskEmptyMessage.setVisibility(TextView.VISIBLE);
                                                else mGp.syncTaskEmptyMessage.setVisibility(TextView.GONE);
                                                if (mGp.syncThreadActive) {
                                                    mMainTabLayout.setCurrentTabByName(mTabNameMessage);
                                                } else {
                                                    mGp.messageListViewMoveToBottomRequired=true;
                                                }
                                                pd.dismiss();
                                            }
                                            @Override
                                            public void negativeResponse(Context c, Object[] o) {
                                                pd.dismiss();
                                                finish();
                                            }
                                        });
                                        ApplicationPasswordUtil.applicationPasswordAuthentication(mGp, mActivity, getSupportFragmentManager(),
                                                mUtil, false, app_pswd_ntfy, ApplicationPasswordUtil.APPLICATION_PASSWORD_RESOURCE_START_APPLICATION);
                                    }
                                    @Override
                                    public void negativeResponse(Context c, Object[] o) {}
                                });
                                openService(svc_ntfy);
                            }

                            @Override
                            public void negativeResponse(Context context, Object[] objects) {}
                        });
                        checkRequiredPermissions(stg__ntfy);

                    }
                });
            }
        };
        th.setPriority(Thread.NORM_PRIORITY);
        th.start();
    }

    private void setMainListener() {
        setCallbackListener();
        setActivityForeground(true);
        setUiEnabled();
        if (mRestoreType == RESTART_BY_KILLED) {
            restoreTaskData();
            mUtil.addLogMsg("W", mContext.getString(R.string.msgs_smbsync_main_restart_by_killed));
            mMainTabLayout.setCurrentTabByName(mTabNameMessage);
        } else if (mRestoreType == RESTART_BY_DESTROYED) {
            restoreTaskData();
            mUtil.addLogMsg("W", mContext.getString(R.string.msgs_smbsync_main_restart_by_destroyed));
            mMainTabLayout.setCurrentTabByName(mTabNameMessage);
        } else {
            if (mGp.syncThreadActive) mMainTabLayout.setCurrentTabByName(mTabNameMessage);
        }
        checkStorageStatus();
        setMessageContextButtonListener();
        setMessageContextButtonNormalMode();

        setSyncTaskContextButtonListener();
        setSyncTaskListItemClickListener();
        setSyncTaskListLongClickListener();
        setSyncTaskContextButtonNormalMode();

        setHistoryContextButtonListener();
        setHistoryViewItemClickListener();
        setHistoryViewLongClickListener();
        setHistoryContextButtonNormalMode();

        setScheduleContextButtonListener();
        setScheduleViewItemClickListener();
        setScheduleViewLongClickListener();
        setScheduleContextButtonNormalMode();

        mGp.syncTaskAdapter.notifyDataSetChanged();
        ScheduleUtil.setSchedulerInfo(mActivity, mGp, mUtil);
        setScheduleTabMessage();

        deleteTaskData();
//                    ScheduleUtil.setSchedulerInfo(mGp, mUtil);
        reshowDialogWindow();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, " + "mStartStatus=" + mStartStatus);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered " + ",currentView=" + mCurrentTab +
                ", getChangingConfigurations=" + String.format("0x%08x", getChangingConfigurations()));
        if (!isTaskTermination) saveTaskData();
        CommonUtilities.saveMsgList(mGp);//Save last updated message tab list
    }

    @Override
    protected void onStop() {
        super.onStop();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        setActivityForeground(false);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

//    @Override
//    protected void onNewIntent(Intent received_intent) {
//        super.onNewIntent(received_intent);
//        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
//
////        if (received_intent.getAction()!=null && !received_intent.getAction().equals("")) {
////            Intent in=new Intent(received_intent.getAction());
////            in.setClass(this, SyncReceiver.class);
////            if (received_intent.getExtras() != null) in.putExtras(received_intent.getExtras());
////            sendBroadcast(in,null);
////        }
//
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, " +
                "isFinishing=" + isFinishing() +
                ", changingConfigurations=" + String.format("0x%08x", getChangingConfigurations()));
        setActivityForeground(false);
        unsetCallbackListener();

        CommonUtilities.saveMsgList(mGp);

        if (isFinishing()) {
            deleteTaskData();
//            mGp.logCatActive=false;
//            mGp.clearParms();
        }
        mGp.appPasswordAuthValidated=false;
        mGp.activityIsFinished = isFinishing();
        closeService();
        LogUtil.flushLog(mContext, mGp);

        System.gc();

    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mUtil != null) {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " Entered, ",
                    "New orientation=" + newConfig.orientation +
                            ", New language=", newConfig.locale.getLanguage());
        }
        if (Build.VERSION.SDK_INT<26) mActivity.getResources().updateConfiguration(newConfig, mActivity.getResources().getDisplayMetrics());
        reloadScreen(false);
    }

//    private void applicationPasswordAuthentication(final NotifyEvent p_ntfy) {
//        NotifyEvent start_ntfy = new NotifyEvent(mContext);
//        start_ntfy.setListener(new NotifyEventListener() {
//            @Override
//            public void positiveResponse(Context context, Object[] objects) {
//                p_ntfy.notifyToListener(true, null);
//            }
//            @Override
//            public void negativeResponse(Context context, Object[] objects) {
//                p_ntfy.notifyToListener(false, null);
//            }
//        });
//        ApplicationPasswordUtil.applicationPasswordAuthentication(mGp, mActivity, getSupportFragmentManager(),
//                mUtil, false, start_ntfy, ApplicationPasswordUtil.APPLICATION_PASSWORD_RESOURCE_START_APPLICATION);
//    }

    private void setActivityForeground(boolean fore_ground) {
        if (mSvcClient != null) {
            try {
                if (fore_ground) mSvcClient.aidlSetActivityInForeground();
                else mSvcClient.aidlSetActivityInBackground();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    private void showSystemInfo() {
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.common_dialog);

        final LinearLayout ll_title=(LinearLayout) dialog.findViewById(R.id.common_dialog_title_view);
        ll_title.setBackgroundColor(mGp.themeColorList.title_background_color);
        final TextView tv_title=(TextView)dialog.findViewById(R.id.common_dialog_title);
        tv_title.setTextColor(mGp.themeColorList.title_text_color);
        final TextView tv_msg_old=(TextView)dialog.findViewById(R.id.common_dialog_msg);
        tv_msg_old.setVisibility(TextView.GONE);
        final NonWordwrapTextView tv_msg=(NonWordwrapTextView) dialog.findViewById(R.id.common_dialog_nonwordwrap_text_view);
        tv_msg.setVisibility(TextView.VISIBLE);
        tv_msg.setWordWrapEnabled(mGp.settingSyncMessageUseStandardTextView);
//        if (Build.VERSION.SDK_INT>=23) tv_msg.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY);
        final Button btn_copy=(Button)dialog.findViewById(R.id.common_dialog_btn_ok);
        final Button btn_close=(Button)dialog.findViewById(R.id.common_dialog_btn_cancel);
        final Button btn_send=(Button)dialog.findViewById(R.id.common_dialog_extra_button);
        btn_send.setText(mContext.getString(R.string.msgs_info_storage_send_btn_title));
        btn_send.setVisibility(Button.VISIBLE);

        tv_title.setText(mContext.getString(R.string.msgs_menu_list_storage_info));
        btn_close.setText(mContext.getString(R.string.msgs_common_dialog_close));
        btn_copy.setText(mContext.getString(R.string.msgs_info_storage_copy_clipboard));

        ArrayList<String>sil= CommonUtilities.listSystemInfo(mContext, mGp);
        String si_text="";
        for(String si_item:sil) si_text+=si_item+"\n";

        tv_msg.setText(si_text);

        CommonDialog.setDlgBoxSizeLimit(dialog,true);

        btn_copy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                android.content.ClipboardManager cm=(android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData cd=cm.getPrimaryClip();
                cm.setPrimaryClip(ClipData.newPlainText("SMBSync2 storage info", tv_msg.getOriginalText().toString()));
                CommonUtilities.showToastMessageLong(mActivity, mContext.getString(R.string.msgs_info_storage_copy_completed));
            }
        });

        btn_close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        btn_send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        String desc=(String)objects[0];
                        Intent intent=new Intent();
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setAction(Intent.ACTION_SEND);
                        intent.setType("message/rfc822");
//                intent.setType("text/plain");
//                intent.setType("application/zip");

                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"gm.developer.fhoshino@gmail.com"});
//                intent.putExtra(Intent.EXTRA_CC, new String[]{"cc@example.com"});
//                intent.putExtra(Intent.EXTRA_BCC, new String[]{"bcc@example.com"});
                        intent.putExtra(Intent.EXTRA_SUBJECT, "SMBSync2 System Info");
                        intent.putExtra(Intent.EXTRA_TEXT, desc+ "\n\n\n"+tv_msg.getOriginalText().toString());
//                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(lf));
                        mContext.startActivity(intent);
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                getProblemDescription(ntfy);
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                btn_close.performClick();
            }
        });

        dialog.show();
    }

    private void getProblemDescription(final NotifyEvent p_ntfy) {
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.single_item_input_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
        CommonUtilities.setDialogBoxOutline(mContext, ll_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
        final TextView tv_title = (TextView) dialog.findViewById(R.id.single_item_input_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        tv_title.setTextColor(mGp.themeColorList.title_text_color);
        tv_title.setText(mContext.getString(R.string.msgs_your_problem_title));

        final TextView tv_msg=(TextView)dialog.findViewById(R.id.single_item_input_msg);
        tv_msg.setVisibility(TextView.GONE);
        final TextView tv_desc=(TextView)dialog.findViewById(R.id.single_item_input_name);
        tv_desc.setText(mContext.getString(R.string.msgs_your_problem_msg));
        final EditText et_msg=(EditText)dialog.findViewById(R.id.single_item_input_dir);
        et_msg.setHint(mContext.getString(R.string.msgs_your_problem_hint));
        et_msg.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        final Button btn_ok=(Button)dialog.findViewById(R.id.single_item_input_ok_btn);
        final Button btn_cancel=(Button)dialog.findViewById(R.id.single_item_input_cancel_btn);

//        btn_cancel.setText(mContext.getString(R.string.msgs_common_dialog_close));

        CommonDialog.setDlgBoxSizeLimit(dialog,true);

        btn_ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                NotifyEvent ntfy_desc=new NotifyEvent(mContext);
                ntfy_desc.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        p_ntfy.notifyToListener(true, new Object[]{et_msg.getText().toString()});
                        dialog.dismiss();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                if (et_msg.getText().length()==0) {
                    mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_your_problem_no_desc),"",ntfy_desc);
                } else {
                    ntfy_desc.notifyToListener(true, null);
                }
            }
        });

        btn_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                btn_cancel.performClick();
            }
        });

        dialog.show();
    }

    private void showBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent();
//            String packageName = mContext.getPackageName();
//            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
//            if (pm.isIgnoringBatteryOptimizations(packageName)) {
//                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//                startActivity(intent);
//                mUtil.addDebugMsg(1, "I", "Invoke battery optimization settings");
//            } else {
//                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//                intent.setData(Uri.parse("package:" + packageName));
//                startActivity(intent);
//                mUtil.addDebugMsg(1, "I", "Request ignore battery optimization");
//            }
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            mUtil.addDebugMsg(1, "I", "Invoke battery optimization settings");
            try {
                startActivity(intent);
            } catch(Exception e) {
                mCommonDlg.showCommonDialog(false, "E", "Invoke Battery optimization error", "error="+e.getMessage(), null);
            }

        }
    }

    class ViewSaveArea {
        public int current_tab_pos = 0;
        public int current_pager_pos = 0;
        public int prof_list_view_pos_x = 0, prof_list_view_pos_y = 0;
        public boolean prof_adapter_show_cb = false;
        public int msg_list_view_pos_x = 0, msg_list_view_pos_y = 0;
        public int sync_list_view_pos_x = 0, sync_list_view_pos_y = 0;
        public boolean sync_adapter_show_cb = false;

        public int prog_bar_view_visibility = ProgressBar.GONE,
                prog_spin_view_visibility = ProgressBar.GONE, confirm_view_visibility = ProgressBar.GONE;

        public String prog_prof = "", prog_msg = "";

        public ArrayList<SyncHistoryItem> sync_hist_list = null;

        public String confirm_msg = "";
        public String progress_bar_msg = "";
        public int progress_bar_progress = 0, progress_bar_max = 0;

        public ButtonViewContent confirm_cancel = new ButtonViewContent();
        public ButtonViewContent confirm_yes = new ButtonViewContent();
        public ButtonViewContent confirm_yes_all = new ButtonViewContent();
        public ButtonViewContent confirm_no = new ButtonViewContent();
        public ButtonViewContent confirm_no_all = new ButtonViewContent();
        public ButtonViewContent prog_bar_cancel = new ButtonViewContent();
        public ButtonViewContent prog_bar_immed = new ButtonViewContent();
        public ButtonViewContent prog_spin_cancel = new ButtonViewContent();
    }

    class ButtonViewContent {
        public String button_text = "";
        public boolean button_visible = true, button_enabled = true, button_clickable = true;
    }

    private void saveButtonStatus(Button btn, ButtonViewContent sv) {
        sv.button_text = btn.getText().toString();
        sv.button_clickable = btn.isClickable();
        sv.button_enabled = btn.isEnabled();
        sv.button_visible = btn.isShown();
    }

    private void restoreButtonStatus(Button btn, ButtonViewContent sv, OnClickListener ocl) {
        btn.setText(sv.button_text);
        btn.setClickable(sv.button_clickable);
        btn.setEnabled(sv.button_enabled);
//		if (sv.button_visible) btn.setVisibility(Button.VISIBLE);
//		else btn.setVisibility(Button.GONE);
        btn.setOnClickListener(ocl);
    }

    private void reloadScreen(boolean force_reload) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " Entered, orientation="+mActivity.getResources().getConfiguration().orientation);
        ViewSaveArea vsa = null;
        vsa = saveViewContent();
        releaseImageResource();
        setContentView(R.layout.main_screen);
        mActionBar = getSupportActionBar();

        mGp.syncHistoryListView.setAdapter(null);

        mGp.syncTaskListView.setAdapter(null);
        ArrayList<SyncTaskItem> pfl = mGp.syncTaskAdapter.getArrayList();

        mGp.syncMessageListView.setAdapter(null);

        ArrayList<SyncMessageItem> mfl=new ArrayList<SyncMessageItem>();
        if (mGp.syncMessageListAdapter !=null) mfl=mGp.syncMessageListAdapter.getMessageList();

        boolean sync_schedule_adapter_select_mode=mGp.syncScheduleAdapter.isSelectMode();

        createTabView();

        mGp.syncTaskAdapter = new AdapterSyncTask(mActivity, R.layout.sync_task_item_view, pfl, mGp);
        mGp.syncTaskAdapter.setShowCheckBox(vsa.prof_adapter_show_cb);
        mGp.syncTaskAdapter.notifyDataSetChanged();
        if (mGp.syncTaskList.size()==0) mGp.syncTaskEmptyMessage.setVisibility(TextView.VISIBLE);
        else mGp.syncTaskEmptyMessage.setVisibility(TextView.GONE);

        mGp.syncMessageListAdapter = new AdapterSyncMessage(mActivity, R.layout.msg_list_item_view, mfl, mGp);

        mGp.syncHistoryAdapter = new AdapterSyncHistory(mActivity, R.layout.sync_history_list_item_view, vsa.sync_hist_list);
        mGp.syncHistoryAdapter.setShowCheckBox(vsa.sync_adapter_show_cb);
        mGp.syncHistoryAdapter.notifyDataSetChanged();

        mGp.syncScheduleAdapter.setSelectMode(sync_schedule_adapter_select_mode);

        initAdapterAndView();

        restoreViewContent(vsa);

        setMessageContextButtonListener();
        setMessageContextButtonNormalMode();

        setSyncTaskContextButtonListener();
        setSyncTaskListItemClickListener();
        setSyncTaskListLongClickListener();

        setHistoryContextButtonListener();

        setHistoryViewItemClickListener();
        setHistoryViewLongClickListener();

        setScheduleContextButtonMode(mGp.syncScheduleAdapter);
        setScheduleContextButtonListener();
        setScheduleViewItemClickListener();
        setScheduleViewLongClickListener();

        if (mCurrentTab.equals(mTabNameTask)) {
            if (mGp.syncHistoryAdapter.isShowCheckBox()) setHistoryContextButtonSelectMode();
            else setHistoryContextButtonNormalMode();

            if (mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
            else setSyncTaskContextButtonNormalMode();
        } else if (mCurrentTab.equals(mTabNameHistory)) {
            if (mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
            else setSyncTaskContextButtonNormalMode();

            if (mGp.syncHistoryAdapter.isShowCheckBox()) setHistoryContextButtonSelectMode();
            else setHistoryContextButtonNormalMode();
        }

        if (isUiEnabled()) setUiEnabled();
        else setUiDisabled();
        vsa = null;
    }

    private ViewSaveArea saveViewContent() {
        ViewSaveArea vsa = new ViewSaveArea();
        vsa.current_tab_pos = mMainTabLayout.getSelectedTabPosition();
        vsa.current_pager_pos = mMainViewPager.getCurrentItem();

        vsa.prof_list_view_pos_x = mGp.syncTaskListView.getFirstVisiblePosition();
        if (mGp.syncTaskListView.getChildAt(0) != null)
            vsa.prof_list_view_pos_y = mGp.syncTaskListView.getChildAt(0).getTop();
        vsa.prof_adapter_show_cb = mGp.syncTaskAdapter.isShowCheckBox();
        vsa.msg_list_view_pos_x = mGp.syncMessageListView.getFirstVisiblePosition();
        if (mGp.syncMessageListView.getChildAt(0) != null)
            vsa.msg_list_view_pos_y = mGp.syncMessageListView.getChildAt(0).getTop();
        vsa.sync_list_view_pos_x = mGp.syncHistoryListView.getFirstVisiblePosition();
        if (mGp.syncHistoryListView.getChildAt(0) != null)
            vsa.sync_list_view_pos_y = mGp.syncHistoryListView.getChildAt(0).getTop();
        vsa.sync_adapter_show_cb = mGp.syncHistoryAdapter.isShowCheckBox();

        vsa.prog_prof = mGp.progressSpinSyncprof.getText().toString();
        vsa.prog_msg = mGp.progressSpinMsg.getText().toString();
        vsa.progress_bar_progress = mGp.progressBarPb.getProgress();
        vsa.progress_bar_max = mGp.progressBarPb.getMax();

        vsa.prog_bar_view_visibility = mGp.progressBarView.getVisibility();
        vsa.confirm_view_visibility = mGp.confirmView.getVisibility();
        vsa.prog_spin_view_visibility = mGp.progressSpinView.getVisibility();

        saveButtonStatus(mGp.confirmCancel, vsa.confirm_cancel);
        saveButtonStatus(mGp.confirmYes, vsa.confirm_yes);
        saveButtonStatus(mGp.confirmYesAll, vsa.confirm_yes_all);
        saveButtonStatus(mGp.confirmNo, vsa.confirm_no);
        saveButtonStatus(mGp.confirmNoAll, vsa.confirm_no_all);
        saveButtonStatus(mGp.progressBarCancel, vsa.prog_bar_cancel);
        saveButtonStatus(mGp.progressSpinCancel, vsa.prog_spin_cancel);
        saveButtonStatus(mGp.progressBarImmed, vsa.prog_bar_immed);

        vsa.confirm_msg = mGp.confirmMsg.getText().toString();

        vsa.progress_bar_msg = mGp.progressBarMsg.getText().toString();

        vsa.sync_hist_list = mGp.syncHistoryAdapter.getSyncHistoryList();

        return vsa;
    }

    private void restoreViewContent(final ViewSaveArea vsa) {
        mWhileRestoreViewProcess=true;
        mMainTabLayout.setCurrentTabByPosition(vsa.current_tab_pos);
        mMainViewPager.setCurrentItem(vsa.current_pager_pos);
        mWhileRestoreViewProcess=false;

        mGp.syncTaskListView.setSelectionFromTop(vsa.prof_list_view_pos_x, vsa.prof_list_view_pos_y);
        mGp.syncMessageListView.setSelectionFromTop(vsa.msg_list_view_pos_x, vsa.msg_list_view_pos_y);
        mGp.syncHistoryListView.setSelectionFromTop(vsa.sync_list_view_pos_x, vsa.sync_list_view_pos_y);

        mGp.confirmMsg.setText(vsa.confirm_msg);

        restoreButtonStatus(mGp.confirmCancel, vsa.confirm_cancel, mGp.confirmCancelListener);
        restoreButtonStatus(mGp.confirmYes, vsa.confirm_yes, mGp.confirmYesListener);
        restoreButtonStatus(mGp.confirmYesAll, vsa.confirm_yes_all, mGp.confirmYesAllListener);
        restoreButtonStatus(mGp.confirmNo, vsa.confirm_no, mGp.confirmNoListener);
        restoreButtonStatus(mGp.confirmNoAll, vsa.confirm_no_all, mGp.confirmNoAllListener);
        restoreButtonStatus(mGp.progressBarCancel, vsa.prog_bar_cancel, mGp.progressBarCancelListener);
        restoreButtonStatus(mGp.progressSpinCancel, vsa.prog_spin_cancel, mGp.progressSpinCancelListener);
        restoreButtonStatus(mGp.progressBarImmed, vsa.prog_bar_immed, mGp.progressBarImmedListener);

        mGp.progressBarMsg.setText(vsa.progress_bar_msg);
        mGp.progressBarPb.setMax(vsa.progress_bar_max);
        mGp.progressBarPb.setProgress(vsa.progress_bar_progress);

        mGp.progressSpinSyncprof.setText(vsa.prog_prof);
        mGp.progressSpinMsg.setText(vsa.prog_msg);
        mGp.scheduleInfoView.setText(mGp.scheduleInfoText);
        mGp.scheduleErrorView.setText(mGp.scheduleErrorText);
        if (mGp.scheduleErrorText.equals("")) mGp.scheduleErrorView.setVisibility(TextView.GONE);
        else mGp.scheduleErrorView.setVisibility(TextView.VISIBLE);

        if (vsa.prog_bar_view_visibility != LinearLayout.GONE) {
            mGp.progressBarView.bringToFront();
//            mGp.progressBarView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
            mGp.progressBarView.setVisibility(LinearLayout.VISIBLE);
        } else mGp.progressBarView.setVisibility(LinearLayout.GONE);

        if (vsa.prog_spin_view_visibility != LinearLayout.GONE) {
            mGp.progressSpinView.bringToFront();
//            mGp.progressSpinView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
            mGp.progressSpinView.setVisibility(LinearLayout.VISIBLE);
        } else mGp.progressSpinView.setVisibility(LinearLayout.GONE);

        if (vsa.confirm_view_visibility != LinearLayout.GONE) {
//            mGp.confirmView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
            mGp.confirmView.setVisibility(LinearLayout.VISIBLE);
            mGp.confirmView.bringToFront();
        } else {
            mGp.confirmView.setVisibility(LinearLayout.GONE);
        }

    }

    private void initAdapterAndView() {
        mGp.syncMessageListView.setAdapter(mGp.syncMessageListAdapter);
        mGp.syncMessageListView.setDrawingCacheEnabled(true);
        mGp.syncMessageListView.setSelection(mGp.syncMessageListAdapter.getCount() - 1);

        mGp.syncTaskListView.setAdapter(mGp.syncTaskAdapter);
        mGp.syncTaskListView.setDrawingCacheEnabled(true);

        mGp.syncHistoryListView.setAdapter(mGp.syncHistoryAdapter);
        mGp.syncHistoryAdapter.notifyDataSetChanged();
    }

    private LinearLayout mSyncTaskView;
    private LinearLayout mScheduleView;
    private LinearLayout mHistoryView;
    private LinearLayout mMessageView;

    private CustomViewPager mMainViewPager;
    private CustomTabLayout mMainTabLayout;
    private boolean mWhileRestoreViewProcess=false;

    private void createTabView() {
        LinearLayout ll_main = (LinearLayout) findViewById(R.id.main_screen_view);
        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSyncTaskView = (LinearLayout) vi.inflate(R.layout.main_sync_task, null);
        mScheduleView = (LinearLayout) vi.inflate(R.layout.main_schedule, null);
        mHistoryView = (LinearLayout) vi.inflate(R.layout.main_history, null);
        mMessageView = (LinearLayout) vi.inflate(R.layout.main_message, null);

        mGp.syncMessageListView = (ListView) mMessageView.findViewById(R.id.main_message_list_view);
        mGp.syncMessageListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mGp.syncTaskListView = (ListView) mSyncTaskView.findViewById(R.id.main_sync_task_view_list);
        mGp.syncTaskEmptyMessage=(TextView)mSyncTaskView.findViewById(R.id.main_sync_task_view_empty_message);
        mGp.syncTaskEmptyMessage.setTextColor(mGp.themeColorList.text_color_warning);
        mGp.syncScheduleListView = (ListView) mScheduleView.findViewById(R.id.main_schedule_list_view);
        mGp.syncHistoryListView = (ListView) mHistoryView.findViewById(R.id.main_history_list_view);

        mGp.syncScheduleAdapter = new AdapterScheduleList(mActivity, R.layout.schedule_sync_list_item, mGp.syncScheduleList);
        mGp.syncScheduleListView.setAdapter(mGp.syncScheduleAdapter);
        mGp.syncScheduleMessage =(TextView)mScheduleView.findViewById(R.id.main_schedule_list_message);
        mGp.syncScheduleMessage.setTextColor(mGp.themeColorList.text_color_warning);
        setScheduleTabMessage();

        mGp.scheduleInfoView = (TextView) findViewById(R.id.main_schedule_view_info);
//        mGp.scheduleInfoView.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.scheduleInfoView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                CommonDialog.showPopupMessageAsUpAnchorView(mActivity, mGp.scheduleInfoView, mGp.scheduleInfoView.getText().toString(), 2,
                        -(int)CommonDialog.toPixel(mContext.getResources(), 50));
                return true;
            }
        });
        mGp.scheduleErrorView = (TextView) findViewById(R.id.main_schedule_view_error);
        mGp.scheduleErrorView.setText(mGp.scheduleErrorText);
        mGp.scheduleErrorView.setTextColor(mGp.themeColorList.text_color_warning);
        if (mGp.scheduleErrorText.equals("")) mGp.scheduleErrorView.setVisibility(TextView.GONE);
        else mGp.scheduleErrorView.setVisibility(TextView.VISIBLE);


        mGp.confirmView = (LinearLayout) findViewById(R.id.main_dialog_confirm_view);
//        mGp.confirmView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
        mGp.confirmView.setVisibility(LinearLayout.GONE);
        mGp.confirmOverrideView=(LinearLayout) findViewById(R.id.main_dialog_confirm_override_view);
        mGp.confirmConflictView=(LinearLayout) findViewById(R.id.main_dialog_confirm_conflict_view);
        mGp.confirmMsg = (TextView) findViewById(R.id.main_dialog_confirm_msg);
//        mGp.confirmMsg.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.confirmCancel = (Button) findViewById(R.id.main_dialog_confirm_sync_cancel);
        setButtonColor(mGp.confirmCancel);
//        if (mGp.themeColorList.theme_is_light)
//            mGp.confirmCancel.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.confirmYes = (Button) findViewById(R.id.copy_delete_confirm_yes);
        setButtonColor(mGp.confirmYes);
//        mGp.confirmYes.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.confirmNo = (Button) findViewById(R.id.copy_delete_confirm_no);
        setButtonColor(mGp.confirmNo);
//        mGp.confirmNo.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.confirmYesAll = (Button) findViewById(R.id.copy_delete_confirm_yesall);
        setButtonColor(mGp.confirmYesAll);
//        mGp.confirmYesAll.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.confirmNoAll = (Button) findViewById(R.id.copy_delete_confirm_noall);
        setButtonColor(mGp.confirmNoAll);
//        mGp.confirmNoAll.setTextColor(mGp.themeColorList.text_color_primary);

        mGp.confirmDialogConflictFilePathA=(TextView) findViewById(R.id.main_dialog_confirm_conflict_pair_a_path);
        mGp.confirmDialogConflictFileLengthA=(TextView) findViewById(R.id.main_dialog_confirm_conflict_pair_a_length);
        mGp.confirmDialogConflictFileLastModA=(TextView) findViewById(R.id.main_dialog_confirm_conflict_pair_a_last_mod);
        mGp.confirmDialogConflictFilePathB=(TextView) findViewById(R.id.main_dialog_confirm_conflict_pair_b_path);
        mGp.confirmDialogConflictFileLengthB=(TextView) findViewById(R.id.main_dialog_confirm_conflict_pair_b_length);
        mGp.confirmDialogConflictFileLastModB=(TextView) findViewById(R.id.main_dialog_confirm_conflict_pair_b_last_mod);
        mGp.confirmDialogConflictButtonSelectA=(Button) findViewById(R.id.main_dialog_confirm_conflict_select_pair_a_btn);
        setButtonColor(mGp.confirmDialogConflictButtonSelectA);
        mGp.confirmDialogConflictButtonSelectB=(Button) findViewById(R.id.main_dialog_confirm_conflict_select_pair_b_btn);
        setButtonColor(mGp.confirmDialogConflictButtonSelectB);
        mGp.confirmDialogConflictButtonSyncIgnoreFile=(Button) findViewById(R.id.main_dialog_confirm_conflict_ignore_file_btn);
        setButtonColor(mGp.confirmDialogConflictButtonSyncIgnoreFile);
        mGp.confirmDialogConflictButtonCancelSyncTask=(Button) findViewById(R.id.main_dialog_confirm_conflict_cancel_sync_task_btn);
        setButtonColor(mGp.confirmDialogConflictButtonCancelSyncTask);

        mGp.progressBarView = (LinearLayout) findViewById(R.id.main_dialog_progress_bar_view);
//        mGp.progressBarView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
        mGp.progressBarView.setVisibility(LinearLayout.GONE);
        mGp.progressBarMsg = (TextView) findViewById(R.id.main_dialog_progress_bar_msg);
//        mGp.progressBarMsg.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.progressBarPb = (ProgressBar) findViewById(R.id.main_dialog_progress_bar_progress);

        mGp.progressBarCancel = (Button) findViewById(R.id.main_dialog_progress_bar_btn_cancel);
        setButtonColor(mGp.progressBarCancel);
//        if (mGp.themeColorList.theme_is_light)
//            mGp.progressBarCancel.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.progressBarImmed = (Button) findViewById(R.id.main_dialog_progress_bar_btn_immediate);
        setButtonColor(mGp.progressBarImmed);
//        if (mGp.themeColorList.theme_is_light)
//            mGp.progressBarImmed.setTextColor(mGp.themeColorList.text_color_primary);


        mGp.progressSpinView = (LinearLayout) findViewById(R.id.main_dialog_progress_spin_view);
//        mGp.progressSpinView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
        mGp.progressSpinView.setVisibility(LinearLayout.GONE);
        mGp.progressSpinSyncprof = (TextView) findViewById(R.id.main_dialog_progress_spin_syncprof);
//        mGp.progressSpinSyncprof.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.progressSpinMsg = (TextView) findViewById(R.id.main_dialog_progress_spin_syncmsg);
//        mGp.progressSpinMsg.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.progressSpinCancel = (Button) findViewById(R.id.main_dialog_progress_spin_btn_cancel);
        setButtonColor(mGp.progressSpinCancel);
//        if (mGp.themeColorList.theme_is_light)
//            mGp.progressSpinCancel.setTextColor(mGp.themeColorList.text_color_primary);

        createContextView();

        mMainTabLayout = (CustomTabLayout) findViewById(R.id.main_tab_layout);
        mMainTabLayout.addTab(mTabNameTask);
        mMainTabLayout.addTab(mTabNameSchedule);
        mMainTabLayout.addTab(mTabNameHistory);
        mMainTabLayout.addTab(mTabNameMessage);
        mMainTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        mMainTabLayout.adjustTabWidth();

        View[] tab_view=new View[]{mSyncTaskView, mScheduleView, mHistoryView, mMessageView};
        CustomViewPagerAdapter adapter = new CustomViewPagerAdapter(mActivity, tab_view);
        mMainViewPager = (CustomViewPager) findViewById(R.id.main_screen_pager);
        mMainViewPager.setAdapter(adapter);
        mMainViewPager.setOffscreenPageLimit(tab_view.length);
        mMainViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mUtil.addDebugMsg(2,"I","onPageSelected entered, pos="+position);
                mMainTabLayout.setCurrentTabByPosition(position);
                if (isUiEnabled()) setUiEnabled();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                mUtil.addDebugMsg(2,"I","onPageScrollStateChanged entered, state="+state);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                mUtil.addDebugMsg(2,"I","onPageScrolled entered, pos="+position);
            }
        });

        mMainTabLayout.setCurrentTabByName(mTabNameTask);
        mMainViewPager.setCurrentItem(0);

        mMainTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String tabId=(String)tab.getTag();
                mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered. tab=" + tabId + ",v=" + mCurrentTab);

                mActionBar.setIcon(R.drawable.smbsync);
                mActionBar.setHomeButtonEnabled(false);
                mActionBar.setTitle(R.string.app_name);

                mMainViewPager.setCurrentItem(mMainTabLayout.getSelectedTabPosition());

                if (!mWhileRestoreViewProcess) {
                    if (mGp.syncTaskAdapter.isShowCheckBox()) {
                        mGp.syncTaskAdapter.setShowCheckBox(false);
                        mGp.syncTaskAdapter.setAllItemChecked(false);
                        mGp.syncTaskAdapter.notifyDataSetChanged();
                        setSyncTaskContextButtonNormalMode();
                    }

                    if (mGp.syncScheduleAdapter.isSelectMode()) {
                        mGp.syncScheduleAdapter.setSelectMode(false);
                        mGp.syncScheduleAdapter.unselectAll();
                        mGp.syncScheduleAdapter.notifyDataSetChanged();
                        setScheduleContextButtonNormalMode();
                    }

                    if (mGp.syncHistoryAdapter.isShowCheckBox()) {
                        mGp.syncHistoryAdapter.setShowCheckBox(false);
                        mGp.syncHistoryAdapter.setAllItemChecked(false);
                        mGp.syncHistoryAdapter.notifyDataSetChanged();
                        setHistoryContextButtonNormalMode();
                    } else {
                        mGp.syncHistoryAdapter.notifyDataSetChanged();
                    }
                }

                mCurrentTab = tabId;
                refreshOptionMenu();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

    }

    private void setButtonColor(Button btn) {
//		if (Build.VERSION.SDK_INT<11) {
//			btn.setBackgroundColor(Color.DKGRAY);
//		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName(), " entered");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_top, menu);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                //display toast info message for top action sync button
                View v_top_sync_btn = findViewById(R.id.menu_top_sync);
                if (v_top_sync_btn != null) {
                    v_top_sync_btn.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (v.getId()== R.id.menu_top_sync) {
                                if (mGp.syncTaskAdapter.isShowCheckBox())  {
                                    CommonDialog.showPopupMessageAsDownAnchorView(mActivity, v, mContext.getString(R.string.msgs_main_sync_selected_profiles_toast), 2);
                                } else {
                                    CommonDialog.showPopupMessageAsDownAnchorView(mActivity, v, mContext.getString(R.string.msgs_main_sync_auto_profiles_toast), 2);
                                }
                                return true;// notify long touch event is consumed
                            }
                            return false;
                        }
                    });
                }

                //display toast info message for top action schedule service button
                View v_top_schedule_btn = findViewById(R.id.menu_top_scheduler);
                if (v_top_schedule_btn != null) {
                    v_top_schedule_btn.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (v.getId()== R.id.menu_top_scheduler) {
                                if (mGp.settingScheduleSyncEnabled)  {
                                    CommonDialog.showPopupMessageAsDownAnchorView(mActivity, v, mContext.getString(R.string.msgs_schedule_list_edit_scheduler_service_toggle_disable), 2);
                                } else {
                                    CommonDialog.showPopupMessageAsDownAnchorView(mActivity, v, mContext.getString(R.string.msgs_schedule_list_edit_scheduler_service_toggle_enable), 2);
                                }
                                return true;// notify long touch event is consumed
                            }
                            return false;
                        }
                    });
                }

                //start schedules top button
                //display toast info message for top action execute button
                View v_top_execute_btn = findViewById(R.id.menu_top_exec_schedule);
                if (v_top_execute_btn != null) {
                    v_top_execute_btn.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (v.getId()== R.id.menu_top_exec_schedule) {
                                if (mGp.syncScheduleAdapter.isSelectMode())  {
                                    CommonDialog.showPopupMessageAsDownAnchorView(mActivity, v, mContext.getString(R.string.msgs_schedule_list_edit_execute_selected_schedule), 2);
                                } else {
                                    CommonDialog.showPopupMessageAsDownAnchorView(mActivity, v, mContext.getString(R.string.msgs_schedule_list_edit_execute_all_enabled_schedule), 2);
                                }
                                return true;// notify long touch event is consumed
                            }
                            return false;
                        }
                    });
                }
            }
        });
        return super.onCreateOptionsMenu(menu);

        //return true;//super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName(), " entered, isUiEnabled()="+isUiEnabled());
        boolean pm_bo = false;
//        if (Build.VERSION.SDK_INT >= 23) {
//            menu.findItem(R.id.menu_top_show_battery_optimization).setVisible(true);
//            String packageName = mContext.getPackageName();
//            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
//            pm_bo = pm.isIgnoringBatteryOptimizations(packageName);
//            String bo_title = "";
//            if (pm_bo)
//                bo_title = mContext.getString(R.string.msgs_menu_battery_optimization_disabled);
//            else bo_title = mContext.getString(R.string.msgs_menu_battery_optimization_enabled);
//            menu.findItem(R.id.menu_top_show_battery_optimization).setTitle(bo_title);
//        } else {
//            menu.findItem(R.id.menu_top_show_battery_optimization).setVisible(false);
//        }
//        LogCatUtil.prepareOptionMenu(mGp, mUtil, menu);

//        if (Build.VERSION.SDK_INT >= 27) {
//            if (Build.VERSION.SDK_INT>=29) {
//                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED &&
//                        checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED) {
//                    menu.findItem(R.id.menu_top_request_grant_coarse_location).setVisible(false);
//                } else {
//                    menu.findItem(R.id.menu_top_request_grant_coarse_location).setVisible(true);
//                }
//            } else {
//                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
//                    menu.findItem(R.id.menu_top_request_grant_coarse_location).setVisible(false);
//                } else {
//                    menu.findItem(R.id.menu_top_request_grant_coarse_location).setVisible(true);
//                }
//            }
//        } else {
//            menu.findItem(R.id.menu_top_request_grant_coarse_location).setVisible(false);
//        }

        if (mGp.settingScheduleSyncEnabled) menu.findItem(R.id.menu_top_scheduler).setIcon(R.drawable.ic_64_schedule);
        else menu.findItem(R.id.menu_top_scheduler).setIcon(R.drawable.ic_64_schedule_disabled);

        if (mGp.settingEnableUsbUuidList) {
            if (Build.VERSION.SDK_INT>=26) menu.findItem(R.id.menu_top_edit_force_usb_uuid_list).setVisible(true);
            else menu.findItem(R.id.menu_top_edit_force_usb_uuid_list).setVisible(false);
        } else {
            menu.findItem(R.id.menu_top_edit_force_usb_uuid_list).setVisible(false);
        }

        if (isUiEnabled()) {
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_housekeep), true);
            if (mGp.syncThreadActive) menu.findItem(R.id.menu_top_housekeep).setVisible(false);
            else menu.findItem(R.id.menu_top_housekeep).setVisible(true);

            //only show top sync button if there is at least one auto and valid sync task
            //in sync task select mode: always show the top sync button if there is at least one valid sync task
            //if we select non valid sync tasks, on start, a proper message is shown to help user understand the way button works
            if (mCurrentTab.equals(mTabNameTask) && mGp.syncTaskList!=null && mGp.syncTaskList.size()>0) {
                menu.findItem(R.id.menu_top_sync).setVisible(false);
                for(SyncTaskItem sti:mGp.syncTaskList) {
                    if ((sti.isSyncTaskAuto() && !sti.isSyncTestMode()) ||
                            mGp.syncTaskAdapter.isShowCheckBox()) {
                        if (!sti.isSyncTaskError()) {//check for invalid sync task with error in master/target name, etc...
                            menu.findItem(R.id.menu_top_sync).setVisible(true);
                            break;
                        }
                    }
                }
            } else {
                menu.findItem(R.id.menu_top_sync).setVisible(false);
            }

            //only show top start schedule button if there is at least one enabled and valid schedule
            //in schedule select mode: always show the start schedule button if there is at least one valid schedule
            //if we select non valid schedules, on start, a proper message is shown to help user understand the way button works
            if (mCurrentTab.equals(mTabNameSchedule) && mGp.syncScheduleList!=null && mGp.syncScheduleList.size()>0) {
                menu.findItem(R.id.menu_top_scheduler).setVisible(true);
                menu.findItem(R.id.menu_top_exec_schedule).setVisible(false);
                for(ScheduleItem si:mGp.syncScheduleList) {
                    if (si.scheduleEnabled || mGp.syncScheduleAdapter.isSelectMode()) {
                        if (ScheduleUtil.isValidScheduleItem(mContext, mGp, mGp.syncScheduleList, si, true, false).equals("")) {//check for invalid schedule (name, sync list)
                            menu.findItem(R.id.menu_top_exec_schedule).setVisible(true);
                            break;
                        }
                    }
                }
            } else {
                menu.findItem(R.id.menu_top_scheduler).setVisible(false);
                menu.findItem(R.id.menu_top_exec_schedule).setVisible(false);
            }
/*
            //issue: toast message state not updated on start
            MenuItem top_sync_btn = menu.findItem(R.id.menu_top_sync);
            View v = top_sync_btn.getActionView();
            //View v = findViewById(R.id.menu_top_sync);
            if (v != null) {
                v.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (SyncTaskUtil.getSyncTaskSelectedItemCount(mGp.syncTaskAdapter) > 0)  {
                            CommonUtilities.showToastMessageShort(mActivity, mContext.getString(R.string.msgs_main_sync_selected_profiles_toast));
                        } else {
                            CommonUtilities.showToastMessageShort(mActivity, mContext.getString(R.string.msgs_main_sync_auto_profiles_toast));
                        }
                        return true;// notify long touch event is consumed
                    }
                });
            }
*/
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_settings), true);
            if (!mGp.externalStorageIsMounted) {
                setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_browse_log), false);
                setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_export), false);
                setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_import), false);
                setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_log_management), false);
            } else {
                if (!mGp.settingLogOption)
                    menu.findItem(R.id.menu_top_browse_log).setVisible(false);
                else menu.findItem(R.id.menu_top_browse_log).setVisible(true);
                setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_export), true);
                setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_import), true);
                setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_log_management), true);
            }
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_add_shortcut), true);

//            menu.findItem(R.id.menu_top_select_storage).setVisible(true);
            if (mGp.debuggable) menu.findItem(R.id.menu_top_select_storage).setVisible(true);
            else menu.findItem(R.id.menu_top_select_storage).setVisible(false);

            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_about), true);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_show_battery_optimization), true);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_list_storage), true);
        } else {
            menu.findItem(R.id.menu_top_sync).setVisible(false);
            if (!mGp.settingLogOption) menu.findItem(R.id.menu_top_browse_log).setVisible(false);
            else menu.findItem(R.id.menu_top_browse_log).setVisible(true);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_browse_log), true);
            if (!mGp.externalStorageIsMounted) {
                setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_browse_log), false);
            }
            if (!mGp.settingLogOption) {
                setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_browse_log), false);
            }

            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_export), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_import), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_settings), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_log_management), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_housekeep), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_add_shortcut), false);

            menu.findItem(R.id.menu_top_select_storage).setVisible(false);

            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_about), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_show_battery_optimization), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_list_storage), false);
            menu.findItem(R.id.menu_top_scheduler).setVisible(false);
            menu.findItem(R.id.menu_top_exec_schedule).setVisible(false);
        }
        menu.findItem(R.id.menu_top_add_shortcut).setVisible(false);
        if (Build.VERSION.SDK_INT>=30) menu.findItem(R.id.menu_top_select_storage).setVisible(false);

        return super.onPrepareOptionsMenu(menu);
    }

    private void setMenuItemEnabled(Menu menu, MenuItem menu_item, boolean enabled) {
        CommonDialog.setMenuItemEnabled(mActivity, menu, menu_item, enabled);
    }


    private boolean mScheduleEditorAvailable = true;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                processHomeButtonPress();
                return true;
            case R.id.menu_top_sync:
                if (isUiEnabled()) {
                    if (mGp.syncTaskAdapter.isShowCheckBox()) {
                        if (SyncTaskUtil.getSyncTaskSelectedItemCount(mGp.syncTaskAdapter) > 0) {
                            syncSelectedSyncTask();
                        } else {
                            //no sync task is selected
                            mUtil.showCommonDialog(false, "W", mContext.getString(R.string.msgs_main_sync_select_prof_no_active_profile), "", null);
                            return true;//do not reset to normal view to let user select a task
                        }
                    } else {
                        syncAutoSyncTask();
                    }
                    SyncTaskUtil.setAllSyncTaskToUnchecked(true, mGp.syncTaskAdapter);
                    setSyncTaskContextButtonNormalMode();
                }
                return true;
            case R.id.menu_top_exec_schedule:
                if (isUiEnabled()) {
                    if (mGp.syncScheduleAdapter.isSelectMode()) {
                        if (mGp.syncScheduleAdapter.getSelectedItemCount() > 0) {
                            executeSelectedSchedule();
                        } else {
                            //no schedule is selected
                            mUtil.showCommonDialog(false, "W", mContext.getString(R.string.msgs_schedule_sync_selected_schedule_not_found), "", null);
                            return true;//do not reset to normal view to let user select a schedule
                        }
                    } else {
                        executeAllEnabledSchedule();
                    }
                    SyncTaskUtil.setAllSyncTaskToUnchecked(true, mGp.syncTaskAdapter);
                    setScheduleContextButtonNormalMode();
                }
                return true;
            case R.id.menu_top_browse_log:
                invokeLogFileBrowser();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_export:
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        mTaskUtil.exportSyncTaskListDlg();
                        setContextButtonNormalMode();
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                ApplicationPasswordUtil.applicationPasswordAuthentication(mGp, mActivity, getSupportFragmentManager(),
                        mUtil, false, ntfy, ApplicationPasswordUtil.APPLICATION_PASSWORD_RESOURCE_EXPORT_TASK_LIST);
                return true;
            case R.id.menu_top_import:
                importSyncTaskAndParms();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_log_management:
                invokeLogManagement();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_scheduler:
                toggleScheduleEnabled();
                return true;
            case R.id.menu_top_about:
                aboutSMBSync();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_settings:
                invokeSettingsActivity();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_edit_force_usb_uuid_list:
                EditUsbUuidList eu=new EditUsbUuidList(mActivity, mUtil);
                return true;
            case R.id.menu_top_kill:
                killTerminateApplication();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_housekeep:
                houseKeepManagementFile();
                return true;
            case R.id.menu_top_add_shortcut:
                addShortcut();
                return true;
            case R.id.menu_top_show_battery_optimization:
                showBatteryOptimization();
                return true;
            case R.id.menu_top_list_storage:
                showSystemInfo();
                return true;
            case R.id.menu_top_select_storage:
                reselectSdcard("", "");
                return true;
//            case R.id.menu_top_request_grant_coarse_location:
//                mGp.setSettingGrantCoarseLocationRequired(mContext, true);
//                checkLocationPermission(false);
//                return true;
//            case R.id.menu_top_start_logcat:
//                LogCatUtil.startLogCat(mGp, mGp.getLogDirName(),"logcat.txt");
//                return true;
//            case R.id.menu_top_stop_logcat:
//                LogCatUtil.stopLogCat(mGp, mUtil);
//                return true;
//            case R.id.menu_top_send_logcat:
//                LogCatUtil.sendLogCat(mActivity, mGp, mUtil, mGp.getLogDirName(), "logcat.txt");
//                return true;
        }
        if (isUiEnabled()) {
        }
        return false;
    }

    private void toggleScheduleEnabled() {
        NotifyEvent ntfy=new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                mGp.setScheduleEnabled(mContext, !mGp.settingScheduleSyncEnabled);
                invalidateOptionsMenu();
                setScheduleTabMessage();
                ScheduleUtil.sendTimerRequest(mContext, SCHEDULER_INTENT_SET_TIMER);
                ScheduleUtil.setSchedulerInfo(mActivity, mGp, mUtil);
            }
            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        String msg="";
        if (mGp.settingScheduleSyncEnabled) msg=mContext.getString(R.string.msgs_schedule_list_edit_confirm_scheduler_to_disabled);
        else msg=mContext.getString(R.string.msgs_schedule_list_edit_confirm_scheduler_to_enabled);
        mCommonDlg.showCommonDialog(true,"W",msg,"",ntfy);
    }

    private void addShortcut() {

//        String shortcutName=getString(R.string.app_name_auto_sync);
//        Intent shortcutIntent=new Intent(Intent.ACTION_VIEW);
//        shortcutIntent.setClassName(this, ShortcutAutoSync.class.getName());
//
//        if (Build.VERSION.SDK_INT>=26) {
//            // Android 8 O API26 以降
//            Icon icon = Icon.createWithResource(getApplicationContext(), R.drawable.auto_sync);
//            ShortcutInfo shortcut = new ShortcutInfo.Builder(getApplicationContext(), shortcutName)
//                    .setShortLabel(shortcutName)
//                    .setLongLabel(shortcutName)
//                    .setIcon(icon)
//                    .setIntent(shortcutIntent)
//                    .build();
//
//            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
//            shortcutManager.requestPinShortcut(shortcut, null); // フツーのショートカット
////            shortcutManager.addDynamicShortcuts(Arrays.asList(shortcut)); // ダイナミックショートカット
//        } else {
//            Intent intent = new Intent();
//            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
//            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
//            Parcelable iconResource=Intent.ShortcutIconResource.fromContext(this, R.drawable.auto_sync);
//            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
//            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
//            setResult(RESULT_OK, intent);
//            sendBroadcast(intent);
//        }
//
//        if (Build.VERSION.SDK_INT<26) {
//            mUiHandler.postDelayed(new Runnable(){
//                @Override
//                public void run() {
////		        Intent swhome=new Intent();
////		        swhome.setAction(Intent.ACTION_MAIN);
////		        swhome.addCategory(Intent.CATEGORY_HOME);
////		        swhome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////				startActivity(swhome);
//                    mUtil.showCommonDialog(false, "I",
//                            mContext.getString(R.string.msgs_main_shortcut_shortcut_added), "", null);
//                }
//            }, 100);
//        }
    }

    private void houseKeepThreadOpenDialog() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        setUiDisabled();
        mGp.progressSpinView.setVisibility(LinearLayout.VISIBLE);
//        mGp.progressSpinView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
        mGp.progressSpinView.bringToFront();
        mGp.progressSpinSyncprof.setVisibility(TextView.GONE);
        mGp.progressSpinMsg.setText(getString(R.string.msgs_progress_spin_dlg_housekeep_running));
        mGp.progressSpinCancel.setText(getString(R.string.msgs_progress_spin_dlg_housekeep_cancel));
        mGp.progressSpinCancel.setEnabled(true);
        // CANCELボタンの指定
        mGp.progressSpinCancelListener = new OnClickListener() {
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        mTcHousekeep.setDisabled();
//                        mGp.progressSpinCancel.setText(getString(R.string.msgs_progress_dlg_canceling));
                        mGp.progressSpinCancel.setEnabled(false);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                mUtil.showCommonDialog(true, "W",
                        getString(R.string.msgs_progress_spin_dlg_housekeep_cancel_confirm),
                        "", ntfy);
            }
        };
        mGp.progressSpinCancel.setOnClickListener(mGp.progressSpinCancelListener);

        LogUtil.flushLog(mContext, mGp);
    }

    private void houseKeepThreadCloseDialog() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " ended");
        LogUtil.flushLog(mContext, mGp);

        mGp.progressBarCancelListener = null;
        mGp.progressBarImmedListener = null;
        mGp.progressSpinCancelListener = null;
        mGp.progressBarCancel.setOnClickListener(null);
        mGp.progressSpinCancel.setOnClickListener(null);
        mGp.progressBarImmed.setOnClickListener(null);

        mGp.progressSpinView.setVisibility(LinearLayout.GONE);

        setUiEnabled();
    }

    private int mResultLogDeleteCount = 0;

    private void houseKeepResultLog() {
        final ArrayList<String> del_list = new ArrayList<String>();
        mResultLogDeleteCount = 0;
        File rlf = new File(mGp.internalRootDirectory + "/" + APPLICATION_TAG + "/result_log");
        File[] fl = rlf.listFiles();
//		Log.v("","list="+fl);
        if (fl != null && fl.length > 0) {
            String del_msg = "", sep = "";
            for (final File ll : fl) {
                boolean found = false;
                if (mGp.syncHistoryList.size() > 0) {
                    for (SyncHistoryItem shi : mGp.syncHistoryList) {
//						Log.v("","check list="+shi.sync_result_file_path);
//						Log.v("","check file="+ll.getPath());
                        if (shi.sync_result_file_path.equals(ll.getPath())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    del_list.add(ll.getPath());
                    del_msg += sep + ll.getPath();
                }
            }
            if (del_list.size() > 0) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        for (String del_fp : del_list) {
                            if (!deleteResultLogFile(del_fp)) {
                                break;
                            }
//							Log.v("","del="+ll.getPath());
                        }
                        mUtil.addLogMsg("I",
                                String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_count), mResultLogDeleteCount));
                        synchronized (mTcHousekeep) {
                            mTcHousekeep.notify();
                        }
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        mUtil.addLogMsg("I",
                                String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_count), mResultLogDeleteCount));
                        synchronized (mTcHousekeep) {
                            mTcHousekeep.notify();
                        }
                    }
                });
                mUtil.showCommonDialog(true, "W",
                        mContext.getString(R.string.msgs_maintenance_result_log_list_del_title), del_msg, ntfy);
                synchronized (mTcHousekeep) {
                    try {
                        mTcHousekeep.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean deleteResultLogFile(String fp) {
        boolean result = false;
        File lf = new File(fp);
        if (lf.isDirectory()) {
            File[] fl = lf.listFiles();
            for (File item : fl) {
                if (item.isDirectory()) {
                    if (!deleteResultLogFile(item.getPath())) {
                        mUtil.addLogMsg("I", "Delete failed, path=" + item.getPath());
                        return false;
                    }
                } else {
                    result = item.delete();
                    if (result) {
                        mResultLogDeleteCount++;
                        String msg = String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_file), item.getPath());
                        mUtil.addLogMsg("I", msg);
                    } else {
                        mUtil.addLogMsg("I", "Delete file failed, path=" + item.getPath());
                    }
                }
            }
            result = lf.delete();
            if (result) {
                mResultLogDeleteCount++;
                String msg = String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_file), lf.getPath());
                mUtil.addLogMsg("I", msg);
            } else {
                mUtil.addLogMsg("I", "Delete directory failed, path=" + lf.getPath());
            }
        } else {
            result = lf.delete();
            if (result) {
                mResultLogDeleteCount++;
                String msg = String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_file), lf.getPath());
                mUtil.addLogMsg("I", msg);
            } else {
                mUtil.addLogMsg("I", "Delete file failed, path=" + lf.getPath());
            }
        }
        return result;
    }

    private void houseKeepLocalFileLastModList() {
        ArrayList<FileLastModifiedTimeEntry> mCurrLastModifiedList = new ArrayList<FileLastModifiedTimeEntry>();
        ArrayList<FileLastModifiedTimeEntry> mNewLastModifiedList = new ArrayList<FileLastModifiedTimeEntry>();
        ArrayList<FileLastModifiedTimeEntry> del_list = new ArrayList<FileLastModifiedTimeEntry>();
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                String en = (String) o[0];
                mUtil.addLogMsg("W", "Duplicate local file last modified entry was ignored, name=" + en);
            }
        });
        FileLastModifiedTime.loadLastModifiedList(mGp.settingMgtFileDir, mCurrLastModifiedList, mNewLastModifiedList, ntfy);
        if (mCurrLastModifiedList.size() > 0) {
            for (FileLastModifiedTimeEntry li : mCurrLastModifiedList) {
                if (!mTcHousekeep.isEnabled()) break;
                if (li.getFilePath().startsWith(mGp.internalRootDirectory)) {
                    File lf = new File(li.getFilePath());
                    if (!lf.exists()) {
                        del_list.add(li);
                        mUtil.addDebugMsg(1, "I", "Entery was deleted, fp=" + li.getFilePath());
                    }
                }
            }
            for (FileLastModifiedTimeEntry li : del_list) {
                if (!mTcHousekeep.isEnabled()) break;
                mCurrLastModifiedList.remove(li);
            }
        }
        if (mTcHousekeep.isEnabled()) {
            mUtil.addLogMsg("I",
                    String.format(mContext.getString(R.string.msgs_maintenance_last_mod_list_del_count), del_list.size()));
            if (del_list.size() > 0)
                FileLastModifiedTime.saveLastModifiedList(mGp.settingMgtFileDir, mCurrLastModifiedList, mNewLastModifiedList);
        }
    }

    private ThreadCtrl mTcHousekeep = null;

    private void houseKeepManagementFile() {
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                mTcHousekeep = new ThreadCtrl();
                Thread th2 = new Thread() {
                    @Override
                    public void run() {
                        mUtil.addLogMsg("I", mContext.getString(R.string.msgs_maintenance_last_mod_list_start_msg));
                        if (!mGp.syncThreadActive) {
                            mGp.syncThreadEnabled = false;
                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    houseKeepThreadOpenDialog();
                                }
                            });

                            houseKeepResultLog();

                            houseKeepLocalFileLastModList();

                            String msg_txt = "";
                            if (mTcHousekeep.isEnabled()) {
                                msg_txt = mContext.getString(R.string.msgs_maintenance_last_mod_list_end_msg);
                            } else
                                msg_txt = mContext.getString(R.string.msgs_maintenance_last_mod_list_cancel_msg);
                            mUtil.addLogMsg("I", msg_txt);
                            mUtil.showCommonDialog(false, "W", msg_txt, "", null);
                            mGp.uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    houseKeepThreadCloseDialog();
                                    mGp.syncThreadEnabled = true;
                                }
                            });

                        } else {
                            mUtil.addLogMsg("I", mContext.getString(R.string.msgs_maintenance_last_mod_list_can_not_start_msg));
                            mUtil.showCommonDialog(false, "W",
                                    mContext.getString(R.string.msgs_maintenance_last_mod_list_can_not_start_msg), "", null);
                        }
                    }
                };
                th2.setPriority(Thread.MAX_PRIORITY);
                th2.start();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        if (!mGp.syncThreadActive) {
            mUtil.showCommonDialog(true, "W",
                    mContext.getString(R.string.msgs_maintenance_last_mod_list_confirm_start_msg), "", ntfy);
        } else {
            mUtil.addLogMsg("I", mContext.getString(R.string.msgs_maintenance_last_mod_list_can_not_start_msg));
            mUtil.showCommonDialog(false, "W",
                    mContext.getString(R.string.msgs_maintenance_last_mod_list_can_not_start_msg), "", null);
        }
    }

    private void setContextButtonNormalMode() {
        mActionBar.setIcon(R.drawable.smbsync);
        mActionBar.setHomeButtonEnabled(false);
        mActionBar.setTitle(R.string.app_name);

        mGp.syncTaskAdapter.setShowCheckBox(false);
        mGp.syncTaskAdapter.setAllItemChecked(false);
        mGp.syncTaskAdapter.notifyDataSetChanged();
        setSyncTaskContextButtonNormalMode();

        mGp.syncHistoryAdapter.setShowCheckBox(false);
        mGp.syncHistoryAdapter.setAllItemChecked(false);
        mGp.syncHistoryAdapter.notifyDataSetChanged();
        setHistoryContextButtonNormalMode();
    }

    private void processHomeButtonPress() {
        if (mCurrentTab.equals(mTabNameTask)) {
            if (mGp.syncTaskAdapter.isShowCheckBox()) {
                mGp.syncTaskAdapter.setShowCheckBox(false);
                mGp.syncTaskAdapter.notifyDataSetChanged();

                setSyncTaskContextButtonNormalMode();
            }
        } else if (mCurrentTab.equals(mTabNameMessage)) {
        } else if (mCurrentTab.equals(mTabNameHistory)) {
            if (mGp.syncHistoryAdapter.isShowCheckBox()) {
                mGp.syncHistoryAdapter.setShowCheckBox(false);
                mGp.syncHistoryAdapter.notifyDataSetChanged();
                setHistoryItemUnselectAll();

                setHistoryContextButtonNormalMode();
            }
        }
    }

    private void invokeLogManagement() {
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                mGp.setSettingOptionLogEnabled(mContext, (boolean) o[0]);
                reloadSettingParms();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        LogUtil.flushLog(mContext, mGp);
        LogFileListDialogFragment lfm =
                LogFileListDialogFragment.newInstance(mGp.settingScreenTheme, false, getString(R.string.msgs_log_management_title),
                        getString(R.string.msgs_log_management_send_log_file_warning),
                        getString(R.string.msgs_log_management_enable_log_file_warning),
                        "SMBSync2 log file");
        lfm.showDialog(getSupportFragmentManager(), lfm, mGp, ntfy);
    }

    private void importSyncTaskAndParms() {
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (mGp.syncTaskList.size()==0) mGp.syncTaskEmptyMessage.setVisibility(TextView.VISIBLE);
                else mGp.syncTaskEmptyMessage.setVisibility(TextView.GONE);
                reloadSettingParms();
                ScheduleUtil.sendTimerRequest(mContext, SCHEDULER_INTENT_SET_TIMER);
                ScheduleUtil.setSchedulerInfo(mActivity, mGp, mUtil);
                setSyncTaskContextButtonNormalMode();
                mGp.syncTaskAdapter.setShowCheckBox(false);

                ArrayList<ScheduleItem>sl=ScheduleUtil.loadScheduleData(mActivity, mGp);
                mGp.syncScheduleList.clear();
                mGp.syncScheduleList.addAll(sl);

                mGp.syncScheduleAdapter.notifyDataSetChanged();
                setScheduleTabMessage();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mTaskUtil.importSyncTaskListDlg(ntfy);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mUtil.addDebugMsg(2, "i", "main onKeyDown enterd, kc=" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (isUiEnabled()) {
                    terminateApplication();
                } else {
                    Intent in = new Intent();
                    in.setAction(Intent.ACTION_MAIN);
                    in.addCategory(Intent.CATEGORY_HOME);
                    startActivity(in);
                }
                return true;
            // break;
            default:
                return super.onKeyDown(keyCode, event);
            // break;
        }
    }

    private void checkStorageStatus() {
        if (mGp.externalStorageAccessIsPermitted) {
            if (!mGp.externalStorageIsMounted) {
                mUtil.addLogMsg("W", getString(R.string.msgs_smbsync_main_no_external_storage));
                mUtil.showCommonDialog(false, "W",
                        getString(R.string.msgs_smbsync_main_no_external_storage), "", null);
            }
        }
    }

    private void aboutSMBSync() {
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.about_dialog);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.about_dialog_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.about_dialog_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);
        title.setText(getString(R.string.msgs_dlg_title_about) + " (Ver " + SystemInfo.getApplVersionName(mContext) + ")");

        // get our tabHost from the xml
        final CustomTabLayout tab_layout = (CustomTabLayout) dialog.findViewById(R.id.tab_layout);
        tab_layout.addTab(mContext.getString(R.string.msgs_about_dlg_func_btn));
        tab_layout.addTab(mContext.getString(R.string.msgs_about_dlg_privacy_btn));
        tab_layout.addTab(mContext.getString(R.string.msgs_about_dlg_change_btn));

        tab_layout.adjustTabWidth();

        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int zf=(int)((float)100* GlobalParameters.getFontScaleFactorValue(mActivity));

        LinearLayout ll_func = (LinearLayout) vi.inflate(R.layout.about_dialog_func, null);
        final WebView func_view = (WebView) ll_func.findViewById(R.id.about_dialog_function);
//        String html_func=CommonUtilities.convertMakdownToHtml(mContext, getString(R.string.msgs_dlg_title_about_func_desc));
//        func_view.loadData(html_func, "text/html; charset=UTF-8", null);
        func_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        setWebViewListener(func_view, zf);

        LinearLayout ll_privacy = (LinearLayout) vi.inflate(R.layout.about_dialog_privacy, null);
        final WebView privacy_view = (WebView) ll_privacy.findViewById(R.id.about_dialog_privacy);
//        String html_privacy=CommonUtilities.convertMakdownToHtml(mContext, getString(R.string.msgs_dlg_title_about_privacy_desc));
//        privacy_view.loadData(html_privacy, "text/html; charset=UTF-8", null);
        privacy_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        setWebViewListener(privacy_view, zf);

        LinearLayout ll_change = (LinearLayout) vi.inflate(R.layout.about_dialog_change, null);
        final WebView change_view = (WebView) ll_change.findViewById(R.id.about_dialog_change_history);
//        String html_change=CommonUtilities.convertMakdownToHtml(mContext, getString(R.string.msgs_dlg_title_about_change_desc));
//        change_view.loadData(html_change, "text/html; charset=UTF-8", null);
        change_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        setWebViewListener(change_view, zf);

        loadHelpFile(func_view, getString(R.string.msgs_dlg_title_about_func_desc));
        loadHelpFile(privacy_view, getString(R.string.msgs_dlg_title_about_privacy_desc));
        loadHelpFile(change_view, getString(R.string.msgs_dlg_title_about_change_desc));

        final CustomViewPagerAdapter adapter = new CustomViewPagerAdapter(mActivity,
                new WebView[]{func_view, privacy_view, change_view});
        final CustomViewPager viewPager = (CustomViewPager) dialog.findViewById(R.id.about_view_pager);
//	    mMainViewPager.setBackgroundColor(mThemeColorList.window_color_background);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.setSwipeEnabled(false);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
//                mUtil.addDebugMsg(2,"I","onPageSelected entered, pos="+position);
                tab_layout.getTabAt(position).select();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
//                mUtil.addDebugMsg(2,"I","onPageScrollStateChanged entered, state="+state);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//		    	util.addDebugMsg(2,"I","onPageScrolled entered, pos="+position);
            }
        });

        tab_layout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
//                mUtil.addDebugMsg(2,"I","onTabSelected entered, state="+tab);
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
//                mUtil.addDebugMsg(2,"I","onTabUnselected entered, state="+tab);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
//                mUtil.addDebugMsg(2,"I","onTabReselected entered, state="+tab);
            }

        });

        final Button btnOk = (Button) dialog.findViewById(R.id.about_dialog_btn_ok);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        // OKボタンの指定
        btnOk.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btnOk.performClick();
            }
        });

        dialog.show();
    }

    private void loadHelpFile(final WebView web_view, String fn) {
        final Handler hndl=new Handler();
        Thread th1=new Thread(){
            @Override
            public void run() {
                String html=CommonUtilities.convertMakdownToHtml(mContext, fn);
                final String b64=Base64.encodeToString(html.getBytes(), Base64.DEFAULT);
                hndl.post(new Runnable(){
                    @Override
                    public void run() {
//                        web_view.loadData(html_func, "text/html; charset=UTF-8", null);
                        web_view.loadData(b64, null, "base64");
                    }
                });
            }
        };
        th1.start();
    }

    private void setWebViewListener(WebView wv, int zf) {
        wv.getSettings().setTextZoom(zf);
        wv.getSettings().setBuiltInZoomControls(true);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading (WebView view, String url) {
                return false;
            }
        });
        wv.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event){
                if(event.getAction() == KeyEvent.ACTION_DOWN){
                    WebView webView = (WebView) v;
                    switch(keyCode){
                        case KeyEvent.KEYCODE_BACK:
                            if(webView.canGoBack()){
                                webView.goBack();
                                return true;
                            }
                            break;
                    }
                }
                return false;
            }
        });

    }

    private void terminateApplication() {
        if (mMainTabLayout.getSelectedTabName().equals(mTabNameTask)) {//
            if (mGp.syncTaskAdapter.isShowCheckBox()) {
                mGp.syncTaskAdapter.setShowCheckBox(false);
                mGp.syncTaskAdapter.notifyDataSetChanged();
                setSyncTaskContextButtonNormalMode();
                return;
            }
        } else if (mMainTabLayout.getSelectedTabName().equals(mTabNameSchedule)) {
            if (mGp.syncScheduleAdapter.isSelectMode()) {
                mGp.syncScheduleAdapter.setSelectMode(false);
                mGp.syncScheduleAdapter.notifyDataSetChanged();
                setScheduleContextButtonNormalMode();
                return;
            }
        } else if (mMainTabLayout.getSelectedTabName().equals(mTabNameMessage)) {
        } else if (mMainTabLayout.getSelectedTabName().equals(mTabNameHistory)) {
            if (mGp.syncHistoryAdapter.isShowCheckBox()) {
                mGp.syncHistoryAdapter.setShowCheckBox(false);
                mGp.syncHistoryAdapter.notifyDataSetChanged();
                setHistoryItemUnselectAll();
                setHistoryContextButtonNormalMode();
                return;
            }
        }
//		mUtil.addLogMsg("I",mContext.getString(R.string.msgs_smbsync_main_end));
        isTaskTermination = true; // exit cleanly
        finish();
    }

    private void killTerminateApplication() {

        final NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
//				terminateApplication();
                deleteTaskData();
                LogUtil.flushLog(mContext, mGp);
                android.os.Process.killProcess(android.os.Process.myPid());
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        Handler hndl = new Handler();
        hndl.post(new Runnable() {
            @Override
            public void run() {
                LogUtil.flushLog(mContext, mGp);
                mUtil.showCommonDialog(true, "W",
                        mContext.getString(R.string.msgs_smnsync_main_kill_application), "", ntfy);
            }
        });
    }

    private void reloadSettingParms() {

        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

        String p_dir = mGp.settingMgtFileDir;
        String p_theme = mGp.settingScreenTheme;
        boolean p_log_option = mGp.settingLogOption;

        mGp.loadSettingsParms(mContext);
        mGp.setLogParms(mContext, mGp);
        if ((p_log_option && !mGp.settingLogOption) || (!p_log_option && mGp.settingLogOption))
            mUtil.resetLogReceiver();

        if (!mGp.settingMgtFileDir.equals(p_dir) && mGp.settingLogOption) {// option was changed
            LogUtil.closeLog(mContext, mGp);
        }

        mGp.refreshMediaDir(mContext);

        if (!p_theme.equals(mGp.settingScreenTheme) || checkThemeLanguageChanged()) {
            NotifyEvent ntfy=new NotifyEvent(mContext);
            ntfy.setListener(new NotifyEventListener() {
                @Override
                public void positiveResponse(Context context, Object[] objects) {
                    mUtil.flushLog();
                    mGp.activityRestartRequired=true;
                    mGp.settingExitClean=false;
                    finish();
//                    mUiHandler.postDelayed(new Runnable(){
//                        @Override
//                        public void run() {
//                            Intent intent = new Intent(mContext, ActivityMain.class);
//                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity(intent);
//                        }
//                    }, 500);
                }
                @Override
                public void negativeResponse(Context context, Object[] objects) {}
            });
            mUtil.showCommonDialog(true, "W",
                    mUtil.getStringWithLocale(mActivity, mGp.settingScreenThemeLanguage, R.string.msgs_smbsync_ui_settings_language_changed_restart), "",
                    mUtil.getStringWithLocale(mActivity, mGp.settingScreenThemeLanguage, R.string.msgs_smbsync_ui_settings_language_changed_restart_immediate),
                    mUtil.getStringWithLocale(mActivity, mGp.settingScreenThemeLanguage, R.string.msgs_smbsync_ui_settings_language_changed_restart_later),
                    ntfy);
        }

        mGp.setDisplayFontScale(mActivity);
        reloadScreen(false);

        if (mGp.settingFixDeviceOrientationToPortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        checkJcifsOptionChanged();
    }

    private void listSettingsOption() {
        mUtil.addDebugMsg(1, "I", "Option: " +
                "debugLevel=" + mGp.settingDebugLevel +
                ", settingErrorOption=" + mGp.settingErrorOption +

                ", settingWifiLockRequired=" + mGp.settingWifiLockRequired +
                ", settingNoCompressFileType=" + mGp.settingNoCompressFileType +
                ", settingNotificationMessageWhenSyncEnded="+mGp.settingNotificationMessageWhenSyncEnded +
                ", settingVibrateWhenSyncEnded=" + mGp.settingVibrateWhenSyncEnded +
                ", settingRingtoneWhenSyncEnded=" + mGp.settingRingtoneWhenSyncEnded +
                ", settingNotificationVolume="+mGp.settingNotificationVolume +
                ", settingPreventSyncStartDelay="+mGp.settingPreventSyncStartDelay +
                ", Force Screen on at start of the sync="+mGp.settingScreenOnIfScreenOnAtStartOfSync +
                ", settingGrantCoarseLocationRequired="+mGp.settingGrantLocationRequired +

                ", settingSupressAppSpecifiDirWarning=" + mGp.settingSupressAppSpecifiDirWarning +
                ", settingFixDeviceOrientationToPortrait=" + mGp.settingFixDeviceOrientationToPortrait +
                ", settingForceDeviceTabletViewInLandscape=" + mGp.settingForceDeviceTabletViewInLandscape +
                ", settingScreenThemeLanguage=" + mGp.settingScreenThemeLanguage + " (value=" + mGp.settingScreenThemeLanguageValue + ")" +
                ", settingExportedProfileEncryptRequired=" + mGp.settingExportedProfileEncryptRequired +
                ", settingScreenTheme=" + mGp.applicationTheme+//.settingScreenTheme +

                ", settingLogOption=" + mGp.settingLogOption +
                ", settingMgtFileDir=" + mGp.settingMgtFileDir +
                ", settingLogMsgFilename=" + mGp.settingLogMsgFilename +
                ", settiingLogGeneration=" + mGp.settingLogMaxFileCount +

                ", settingExitClean=" + mGp.settingExitClean +
                "");
    }

    private void invokeLogFileBrowser() {
        mUtil.addDebugMsg(1, "I", "Invoke log file browser.");
        LogUtil.flushLog(mContext, mGp);
        if (mGp.settingLogOption) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                if (Build.VERSION.SDK_INT>=24) {
                    Uri uri= FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", new File(LogUtil.getLogFilePath(mGp)));
                    intent.setDataAndType(uri, "text/plain");
                } else {
                    intent.setDataAndType(Uri.parse("file://"+LogUtil.getLogFilePath(mGp)), "text/plain");
                }
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                mUtil.showCommonDialog(false, "E",
                        mContext.getString(R.string.msgs_log_file_browse_app_can_not_found), e.getMessage(), null);
            }
        }
    }

    private void invokeSettingsActivity() {
        mUtil.addDebugMsg(1, "I", "Invoke Settings.");
        Intent intent = null;
        intent = new Intent(mContext, ActivitySettings.class);
        startActivityForResult(intent, 0);
    }

    private boolean mIsStorageSelectorActivityNotFound = false;

    public void invokeSdcardSelector(final NotifyEvent p_ntfy) {
        mSafSelectActivityNotify = p_ntfy;
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                startSdcardSelectorActivity();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        if (Build.VERSION.SDK_INT>=24 && Build.VERSION.SDK_INT<=28) {
            ntfy.notifyToListener(true, null);
        } else {
            mTaskUtil.showSelectSdcardMsg(ntfy);
        }

    }

    public void invokeUsbSelector(final NotifyEvent p_ntfy) {
        mSafSelectActivityNotify = p_ntfy;
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                startUsbSelectorActivity();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        if (Build.VERSION.SDK_INT>=24 && Build.VERSION.SDK_INT<=28) {
            ntfy.notifyToListener(true, null);
        } else {
            mTaskUtil.showSelectUsbMsg(ntfy);
        }
    }

    private final int REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;

    private NotifyEvent mNtfyExternalStoragePermission=null;
    private void checkRequiredPermissions(final NotifyEvent p_ntfy) {
        if (Build.VERSION.SDK_INT >= 23) {
            mUtil.addDebugMsg(1, "I", "Prermission WriteExternalStorage=" + checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                    ", WakeLock=" + checkSelfPermission(Manifest.permission.WAKE_LOCK));
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        mNtfyExternalStoragePermission=p_ntfy;
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        NotifyEvent ntfy_term = new NotifyEvent(mContext);
                        ntfy_term.setListener(new NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context c, Object[] o) {
                                isTaskTermination = true;
                                finish();
                            }

                            @Override
                            public void negativeResponse(Context c, Object[] o) {}
                        });
                        mUtil.showCommonDialog(false, "W",
                                mContext.getString(R.string.msgs_main_permission_external_storage_title),
                                mContext.getString(R.string.msgs_main_permission_external_storage_denied_msg), ntfy_term);
                    }
                });
                mUtil.showCommonDialog(false, "W",
                        mContext.getString(R.string.msgs_main_permission_external_storage_title),
                        mContext.getString(R.string.msgs_main_permission_external_storage_request_msg), ntfy);
            } else {
                p_ntfy.notifyToListener(true, null);
            }
        } else {
            p_ntfy.notifyToListener(true, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mUtil.addDebugMsg(1, "I", "onRequestPermissionsResult="+requestCode);
        if (REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mGp.initStorageStatus(mContext);
                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mNtfyExternalStoragePermission!=null) mNtfyExternalStoragePermission.notifyToListener(true, null);
                    }
                }, 500);
            } else {
                NotifyEvent ntfy_term = new NotifyEvent(mContext);
                ntfy_term.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        isTaskTermination = true;
                        finish();
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {}
                });
                mUtil.showCommonDialog(false, "W",
                        mContext.getString(R.string.msgs_main_permission_external_storage_title),
                        mContext.getString(R.string.msgs_main_permission_external_storage_denied_msg), ntfy_term);
            }
        }
    }

    private void showLocationPermissionMessage(final String title_text, final String msg_text, String image_fn, final NotifyEvent p_ntfy) {
        final Dialog dialog=new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.dialog_with_image_view_dlg);

        TextView dlg_title=(TextView)dialog.findViewById(R.id.dialog_with_image_view_dlg_title);
        TextView dlg_msg=(TextView)dialog.findViewById(R.id.dialog_with_image_view_dlg_msg);
        ImageView dlg_image=(ImageView)dialog.findViewById(R.id.dialog_with_image_view_dlg_image);

        Button dlg_ok=(Button)dialog.findViewById(R.id.dialog_with_image_view_dlg_btn_ok);
        Button dlg_cancel=(Button)dialog.findViewById(R.id.dialog_with_image_view_dlg_btn_cancel);

        dlg_title.setText(title_text);
        dlg_msg.setText(msg_text);

        try {
            InputStream is = mContext.getResources().getAssets().open(image_fn);
            Bitmap bm = BitmapFactory.decodeStream(is);
            dlg_image.setImageBitmap(bm);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        dlg_ok.setText(mContext.getString(R.string.msgs_storage_permission_all_file_access_button_text));

        dlg_ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                p_ntfy.notifyToListener(true, null);
                dialog.dismiss();
            }
        });

        dlg_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                p_ntfy.notifyToListener(false, null);
                dialog.dismiss();;
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dlg_cancel.performClick();
            }
        });

        dialog.show();
    }


    private NotifyEvent mSafSelectActivityNotify = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            mUtil.addDebugMsg(1, "I", "Return from Settings.");
            reloadSettingParms();
            if (mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
            else setSyncTaskContextButtonNormalMode();
        } else if (requestCode == (ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS + 1)) {
            mUtil.addDebugMsg(1, "I", "Return from Storage Picker. id=" + requestCode);
        } else if (requestCode == ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS) {
            mUtil.addDebugMsg(1, "I", "Return from Storage Picker. id=" + requestCode + ", result=" + resultCode);
            if (resultCode == Activity.RESULT_OK) {
                if (data==null || data.getDataString()==null) {
                    mUtil.showCommonDialog(false, "W", "SDCARD Grant write permission failed because null intent data was returned.", "", null);
                    mUtil.addLogMsg("E", "SDCARD Grant write permission failed because null intent data was returned.", "");
                    return;
                }
                mUtil.addDebugMsg(1, "I", "Intent=" + data.getData().toString());
//                if (SafManager.getUuidFromUri(data.getData().toString()).equals("0000-0000")) {
//                    reselectSdcard(mContext.getString(R.string.msgs_main_external_sdcard_select_uuid_invalid_msg));
//                } else
                if (!mGp.safMgr.isUsbUuid(SafManager.getUuidFromUri(data.getData().toString()))) {
                    if (!mGp.safMgr.isRootTreeUri(data.getData())) {
                        mUtil.addDebugMsg(1, "I", "Selected UUID="+ SafManager.getUuidFromUri(data.getData().toString()));
                        String em=mGp.safMgr.getLastErrorMessage();
                        if (em.length()>0) mUtil.addDebugMsg(1, "I", "SafMessage="+em);
                        reselectSdcard(mContext.getString(R.string.msgs_main_external_sdcard_select_retry_select_msg), data.getData().getPath());
                    } else {
                        mUtil.addDebugMsg(1, "I", "Selected UUID="+ SafManager.getUuidFromUri(data.getData().toString()));
                        String em=mGp.safMgr.getLastErrorMessage();
                        if (em.length()>0) mUtil.addDebugMsg(1, "I", "SafMessage="+em);
                        if (mGp.safMgr.isRootTreeUri(data.getData())) {
                            boolean rc=mGp.safMgr.addSdcardUuid(data.getData());
                            if (!rc) {
                                String saf_msg=mGp.safMgr.getLastErrorMessage();
                                mUtil.showCommonDialog(false, "W", "SDCARD UUID registration failed, please reselect SDCARD", saf_msg, null);
                                mUtil.addLogMsg("E", "SDCARD UUID registration failed, please reselect SDCARD\n", saf_msg);
                            }
                            mGp.syncTaskAdapter.notifyDataSetChanged();
                            if (mSafSelectActivityNotify != null)
                                mSafSelectActivityNotify.notifyToListener(true, new Object[]{data.getData()});
                        } else {
                            reselectSdcard(mContext.getString(R.string.msgs_main_external_sdcard_select_retry_select_msg), data.getData().getPath());
                        }
                    }
                } else {
                    reselectSdcard(mContext.getString(R.string.msgs_main_external_sdcard_select_retry_select_usb_selected_msg), data.getData().getPath());
                }
            } else {
                if (mGp.safMgr.getSdcardRootSafFile() == null && !mIsStorageSelectorActivityNotFound) {
                    SyncTaskItem pli = SyncTaskUtil.getExternalSdcardUsedSyncTask(mGp);
                    if (pli != null) {
                        String msg = String.format(mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg),
                                pli.getSyncTaskName());
                        mUtil.showCommonDialog(false, "W",
                                mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
                                msg,
                                null);
                    }
                }
            }
        } else if (requestCode == ACTIVITY_REQUEST_CODE_USB_STORAGE_ACCESS) {
            mUtil.addDebugMsg(1, "I", "Return from Storage Picker. id=" + requestCode + ", result=" + resultCode);
            if (resultCode == Activity.RESULT_OK) {
                if (data==null || data.getDataString()==null) {
                    mUtil.showCommonDialog(false, "W", "USB Media Grant write permission failed because null intent data was returned.", "", null);
                    mUtil.addLogMsg("E", "USB Media Grant write permission failed because null intent data was returned.", "");
                    return;
                }
                mUtil.addDebugMsg(1, "I", "Intent=" + data.getData().toString());
                if (mGp.safMgr.isUsbUuid(SafManager.getUuidFromUri(data.getData().toString()))) {
                    if (!mGp.safMgr.isRootTreeUri(data.getData())) {
                        mUtil.addDebugMsg(1, "I", "Selected UUID="+ SafManager.getUuidFromUri(data.getData().toString()));
                        String em=mGp.safMgr.getLastErrorMessage();
                        if (em.length()>0) mUtil.addDebugMsg(1, "I", "SafMessage="+em);
                        reselectUsb(mContext.getString(R.string.msgs_main_external_usb_select_retry_select_msg), data.getData().getPath());
                    } else {
                        mUtil.addDebugMsg(1, "I", "Selected UUID="+ SafManager.getUuidFromUri(data.getData().toString()));
                        String em=mGp.safMgr.getLastErrorMessage();
                        if (em.length()>0) mUtil.addDebugMsg(1, "I", "SafMessage="+em);
                        if (mGp.safMgr.isRootTreeUri(data.getData())) {
                            String uuid=mGp.safMgr.getUuidFromUri(data.getData().toString());
                            File tf=new File("/storage/"+uuid);
                            if (!tf.exists()) {
                                String e_msg=String.format(mContext.getString(R.string.msgs_main_external_usb_select_path_not_available_msg), uuid);
                                mUtil.showCommonDialog(false, "W", e_msg, "", null);
                                mUtil.addLogMsg("E", e_msg);
                            } else {
                                boolean rc=mGp.safMgr.addUsbUuid(data.getData());
                                if (!rc) {
                                    String saf_msg=mGp.safMgr.getLastErrorMessage();
                                    mUtil.showCommonDialog(false, "W", "USB Media UUID registration failed, please reselect USB Media", saf_msg, null);
                                    mUtil.addLogMsg("E", "USB Media UUID registration failed, please reselect USB Media\n", saf_msg);
                                }
                                mGp.syncTaskAdapter.notifyDataSetChanged();
                                if (mSafSelectActivityNotify != null) mSafSelectActivityNotify.notifyToListener(true, new Object[]{data.getData()});
                            }
                        } else {
                            reselectUsb(mContext.getString(R.string.msgs_main_external_usb_select_retry_select_msg), data.getData().getPath());
                        }
                    }
                } else {
                    reselectUsb(mContext.getString(R.string.msgs_main_external_usb_select_retry_select_sdcard_selected_msg), data.getData().getPath());
                }
            } else {
                if (mGp.safMgr.getSdcardRootSafFile() == null && !mIsStorageSelectorActivityNotFound) {
                    SyncTaskItem pli = SyncTaskUtil.getExternalSdcardUsedSyncTask(mGp);
                    if (pli != null) {
                        String msg = String.format(mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg),
                                pli.getSyncTaskName());
                        mUtil.showCommonDialog(false, "W",
                                mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
                                msg,
                                null);
                    }
                }
            }
        }
    }

    private void startSdcardSelectorActivity() {
        try {
            mIsStorageSelectorActivityNotFound = false;
            if (Build.VERSION.SDK_INT>=24 && Build.VERSION.SDK_INT<=28) {
                StorageVolume sv=SyncTaskEditor.getSdcardStorageVolume(mContext, mUtil);
                if (sv==null) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    startActivityForResult(intent, ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS);
                } else {
                    if (sv.getUuid()!=null) {
                        File lf=new File("/storage/"+sv.getUuid());
                        if (lf.exists()) {
                            Intent intent = sv.createAccessIntent(null);
                            try {
                                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS);
                            } catch(ActivityNotFoundException e) {
                                Intent intent_x = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                startActivityForResult(intent_x, ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS);
                            }
                        } else {
                            String emsg=String.format(
                                    mContext.getString(R.string.msgs_main_external_sdcard_select_required_mountpoint_does_not_exists),
                                    lf.getPath());
                            mUtil.addLogMsg("E", emsg);
                            mUtil.showCommonDialog(false, "E",
                                    mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
                                    emsg,
                                    null);
                        }
                    }
                }
            } else {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS);
            }
        } catch (Exception e) {
            mIsStorageSelectorActivityNotFound = true;
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            pw.close();
            String emsg=mContext.getString(R.string.msgs_main_external_sdcard_select_activity_not_found_msg)+"\n"+e.getMessage();
            mUtil.addLogMsg("E", emsg+"\n"+sw.toString());
            mUtil.showCommonDialog(false, "E",
                    mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
                    emsg,
                    null);
        }
    }

    private void startUsbSelectorActivity() {
        try {
            mIsStorageSelectorActivityNotFound = false;
            if (Build.VERSION.SDK_INT>=24 && Build.VERSION.SDK_INT<=28) {
                StorageVolume sv=SyncTaskEditor.getStorageVolume(mContext, mUtil, "USB");
                if (sv==null) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    startActivityForResult(intent, ACTIVITY_REQUEST_CODE_USB_STORAGE_ACCESS);
                } else {
                    try {
//                        String npe=null;
//                        npe.length();
                        Intent intent = sv.createAccessIntent(null);
                        startActivityForResult(intent, ACTIVITY_REQUEST_CODE_USB_STORAGE_ACCESS);
                    } catch(Exception e) {//Retry for startActivity error
//                        e.printStackTrace();
                        mUtil.addDebugMsg(1,"W", "startActivity failed with createAccessIntent, retry legacy method initiated.");
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        startActivityForResult(intent, ACTIVITY_REQUEST_CODE_USB_STORAGE_ACCESS);
                    }
                }
            } else {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_USB_STORAGE_ACCESS);
            }
        } catch (Exception e) {
            mIsStorageSelectorActivityNotFound = true;
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            pw.close();
            String emsg=mContext.getString(R.string.msgs_main_external_usb_select_activity_not_found_msg)+"\n"+e.getMessage();
            mUtil.addLogMsg("E", emsg+"\n"+sw.toString());
            mUtil.showCommonDialog(false, "E",
                    mContext.getString(R.string.msgs_main_external_usb_select_required_title),
                    emsg,
                    null);
        }
    }

    private void reselectSdcard(String msg, String intent_data) {
        NotifyEvent ntfy_retry = new NotifyEvent(mContext);
        ntfy_retry.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        startSdcardSelectorActivity();
                    }
                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        if (!mGp.safMgr.isSdcardMounted()) {
                            SyncTaskItem pli = SyncTaskUtil.getExternalSdcardUsedSyncTask(mGp);
                            if (pli != null) {
                                String msg = String.format(mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg),
                                        pli.getSyncTaskName());
                                mUtil.showCommonDialog(false, "W",
                                        mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
                                        msg,
                                        null);
                            }
                        }
                    }
                });
                if (Build.VERSION.SDK_INT>=24) ntfy.notifyToListener(true, null);
                else mTaskUtil.showSelectSdcardMsg(ntfy);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                if (!mGp.safMgr.isSdcardMounted()) {
                    SyncTaskItem pli = SyncTaskUtil.getExternalSdcardUsedSyncTask(mGp);
                    if (pli != null) {
                        String msg = String.format(mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg),
                                pli.getSyncTaskName());
                        mUtil.showCommonDialog(false, "W",
                                mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
                                msg,
                                null);
                    }
                }
            }
        });
        if (msg.equals("")) ntfy_retry.notifyToListener(true, null);
        else mUtil.showCommonDialog(true, "W", msg, intent_data, ntfy_retry);

    }

    private void reselectUsb(String msg, String intent_data) {
        NotifyEvent ntfy_retry = new NotifyEvent(mContext);
        ntfy_retry.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        startUsbSelectorActivity();
                    }
                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        if (!mGp.safMgr.isUsbMounted()) {
                            SyncTaskItem pli = SyncTaskUtil.getUsbMediaUsedSyncTask(mGp);
                            if (pli != null) {
                                String msg = String.format(mContext.getString(R.string.msgs_main_external_usb_select_required_cancel_msg),
                                        pli.getSyncTaskName());
                                mUtil.showCommonDialog(false, "W",
                                        mContext.getString(R.string.msgs_main_external_usb_select_required_title),
                                        msg,
                                        null);
                            }
                        }
                    }
                });
                mTaskUtil.showSelectUsbMsg(ntfy);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                if (!mGp.safMgr.isUsbMounted()) {
                    SyncTaskItem pli = SyncTaskUtil.getExternalSdcardUsedSyncTask(mGp);
                    if (pli != null) {
                        String msg = String.format(mContext.getString(R.string.msgs_main_external_usb_select_required_cancel_msg),
                                pli.getSyncTaskName());
                        mUtil.showCommonDialog(false, "W",
                                mContext.getString(R.string.msgs_main_external_usb_select_required_title),
                                msg,
                                null);
                    }
                }
            }
        });
        if (msg.equals("")) ntfy_retry.notifyToListener(true, null);
        else mUtil.showCommonDialog(true, "W", msg, intent_data, ntfy_retry);

    }

    private void setHistoryViewItemClickListener() {
        mGp.syncHistoryListView.setEnabled(true);
        mGp.syncHistoryListView
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        mGp.syncHistoryListView.setEnabled(false);
                        SyncHistoryItem item = mGp.syncHistoryAdapter.getItem(position);
                        if (mGp.syncHistoryAdapter.isShowCheckBox()) {
                            item.isChecked = !item.isChecked;
                            setHistoryContextButtonSelectMode();
                            mGp.syncHistoryListView.setEnabled(true);
                        } else {
                            if (item.sync_result_file_path!=null && !item.sync_result_file_path.equals("")) {
                                File lf=new File(item.sync_result_file_path);
                                if (lf.exists()) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    if (Build.VERSION.SDK_INT>=24) {
                                        Uri uri= FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", new File(item.sync_result_file_path));
                                        intent.setDataAndType(uri, "text/plain");
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                    } else {
                                        intent.setDataAndType(Uri.parse("file://"+item.sync_result_file_path),"text/plain");
                                    }
                                    try {
                                        mActivity.startActivity(intent);
                                    } catch(ActivityNotFoundException e) {
                                        mUtil.showCommonDialog(false, "E",
                                                mContext.getString(R.string.msgs_main_sync_history_result_activity_not_found_for_log_display), "", null);
                                    }
                                }
                            }
                            mUiHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mGp.syncHistoryListView.setEnabled(true);
                                }
                            }, 1000);
                        }
                        mGp.syncHistoryAdapter.notifyDataSetChanged();
                    }
                });

        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                setHistoryContextButtonSelectMode();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mGp.syncHistoryAdapter.setNotifyCheckBoxEventHandler(ntfy);
    }

    private void setHistoryViewLongClickListener() {
        mGp.syncHistoryListView
                .setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                                   int pos, long arg3) {
                        if (mGp.syncHistoryAdapter.isEmptyAdapter()) return true;
                        if (!isUiEnabled()) return true;

                        if (!mGp.syncHistoryAdapter.getItem(pos).isChecked) {
                            if (mGp.syncHistoryAdapter.isAnyItemSelected()) {
                                int down_sel_pos = -1, up_sel_pos = -1;
                                int tot_cnt = mGp.syncHistoryAdapter.getCount();
                                if (pos + 1 <= tot_cnt) {
                                    for (int i = pos + 1; i < tot_cnt; i++) {
                                        if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
                                            up_sel_pos = i;
                                            break;
                                        }
                                    }
                                }
                                if (pos > 0) {
                                    for (int i = pos; i >= 0; i--) {
                                        if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
                                            down_sel_pos = i;
                                            break;
                                        }
                                    }
                                }
//						Log.v("","up="+up_sel_pos+", down="+down_sel_pos);
                                if (up_sel_pos != -1 && down_sel_pos == -1) {
                                    for (int i = pos; i < up_sel_pos; i++)
                                        mGp.syncHistoryAdapter.getItem(i).isChecked = true;
                                } else if (up_sel_pos != -1 && down_sel_pos != -1) {
                                    for (int i = down_sel_pos + 1; i < up_sel_pos; i++)
                                        mGp.syncHistoryAdapter.getItem(i).isChecked = true;
                                } else if (up_sel_pos == -1 && down_sel_pos != -1) {
                                    for (int i = down_sel_pos + 1; i <= pos; i++)
                                        mGp.syncHistoryAdapter.getItem(i).isChecked = true;
                                }
                                mGp.syncHistoryAdapter.notifyDataSetChanged();
                            } else {
                                mGp.syncHistoryAdapter.setShowCheckBox(true);
                                mGp.syncHistoryAdapter.getItem(pos).isChecked = true;
                                mGp.syncHistoryAdapter.notifyDataSetChanged();
                            }
                            setHistoryContextButtonSelectMode();
                        }
                        return true;
                    }
                });
    }

    private void sendHistoryFile() {
        final String zip_file_name = mGp.getLogDirName() + "log.zip";

        int no_of_files = 0;
        for (int i = 0; i < mGp.syncHistoryAdapter.getCount(); i++) {
//			Log.v("","name="+mGp.syncHistoryAdapter.getItem(i).sync_result_file_path);
            if (mGp.syncHistoryAdapter.getItem(i).isChecked && !mGp.syncHistoryAdapter.getItem(i).sync_result_file_path.equals("")) {
                no_of_files++;
            }
        }

        if (no_of_files == 0) {
            MessageDialogFragment mdf = MessageDialogFragment.newInstance(false, "E",
                    mContext.getString(R.string.msgs_main_sync_history_result_log_not_found),
                    "");
            mdf.showDialog(getSupportFragmentManager(), mdf, null);
            return;
        }


//		Log.v("","file="+no_of_files);
        final String[] file_name = new String[no_of_files];
        int files_pos = 0;
        for (int i = 0; i < mGp.syncHistoryAdapter.getCount(); i++) {
            if (mGp.syncHistoryAdapter.getItem(i).isChecked && !mGp.syncHistoryAdapter.getItem(i).sync_result_file_path.equals("")) {
                file_name[files_pos] = mGp.syncHistoryAdapter.getItem(i).sync_result_file_path;
                files_pos++;
            }
        }

        final ThreadCtrl tc = new ThreadCtrl();
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                tc.setDisabled();
            }
        });

        final ProgressBarDialogFragment pbdf = ProgressBarDialogFragment.newInstance(
                mContext.getString(R.string.msgs_log_file_list_dlg_send_zip_file_creating),
                "",
                mContext.getString(R.string.msgs_common_dialog_cancel),
                mContext.getString(R.string.msgs_common_dialog_cancel));
        pbdf.showDialog(getSupportFragmentManager(), pbdf, ntfy, true);
        Thread th = new Thread() {
            @Override
            public void run() {
                File lf = new File(zip_file_name);
                lf.delete();
                String[] lmp = LocalMountPoint.convertFilePathToMountpointFormat(mContext, file_name[0]);
                ZipUtil.createZipFile(mContext, tc, pbdf, zip_file_name, lmp[0], file_name);
                if (tc.isEnabled()) {
                    Intent intent = new Intent();
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setAction(Intent.ACTION_SEND);
//				    intent.setType("message/rfc822");
//				    intent.setType("text/plain");
                    intent.setType("application/zip");

//                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(lf));
                    if (Build.VERSION.SDK_INT>=24) {
                        Uri uri= FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", lf);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                    } else {
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(lf));
                    }
                    mContext.startActivity(intent);

                } else {
                    lf.delete();
                    MessageDialogFragment mdf = MessageDialogFragment.newInstance(false, "W",
                            mContext.getString(R.string.msgs_log_file_list_dlg_send_zip_file_cancelled),
                            "");
                    mdf.showDialog(getSupportFragmentManager(), mdf, null);
                }
                pbdf.dismiss();
            }

            ;
        };
        th.start();
    }

    private void setScheduleTabMessage() {
        if (mGp.syncScheduleAdapter.getCount() == 0) {
            mGp.syncScheduleMessage.setVisibility(TextView.VISIBLE);
            mGp.syncScheduleMessage.setText(mContext.getString(R.string.msgs_schedule_list_edit_no_schedule));
        } else {
            if (mGp.settingScheduleSyncEnabled) {
                if (mGp.syncScheduleAdapter.getCount()!=0) {
                    mGp.syncScheduleMessage.setVisibility(TextView.GONE);
                }
            } else {
                mGp.syncScheduleMessage.setVisibility(TextView.VISIBLE);
                mGp.syncScheduleMessage.setText(mContext.getString(R.string.msgs_schedule_list_edit_scheduler_disabled));
            }
        }
        mGp.syncScheduleAdapter.notifyDataSetChanged();
    }

    //action when bottom buttons are pressed in schedule view when a schedule task is selected
    private void setScheduleContextButtonListener() {
        mContextScheduleButtonAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        ScheduleItem si = (ScheduleItem) objects[0];
                        mGp.syncScheduleAdapter.add(si);
                        mGp.syncScheduleAdapter.sort();
                        mGp.syncScheduleAdapter.notifyDataSetChanged();
                        setScheduleContextButtonMode(mGp.syncScheduleAdapter);
                        saveScheduleList();
                        refreshOptionMenu();
                        setScheduleTabMessage();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                ScheduleItemEditor sm = new ScheduleItemEditor(mUtil, mActivity, mContext, mCommonDlg, ccMenu, mGp,
                        false, mGp.syncScheduleList, new ScheduleItem(), ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonAdd, mContext.getString(R.string.msgs_schedule_cont_label_add));

        mContextScheduleButtonDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        for (int i = mGp.syncScheduleAdapter.getCount() - 1; i >= 0; i--) {
                            if (mGp.syncScheduleAdapter.getItem(i).isChecked) {
                                mGp.syncScheduleAdapter.remove(mGp.syncScheduleAdapter.getItem(i));
                            }
                        }
                        if (mGp.syncScheduleAdapter.getCount() == 0) {
                            mGp.syncScheduleMessage.setVisibility(TextView.VISIBLE);
                        }
                        mGp.syncScheduleAdapter.setSelectMode(false);
                        mGp.syncScheduleAdapter.sort();
                        mGp.syncScheduleAdapter.unselectAll();
                        setScheduleContextButtonMode(mGp.syncScheduleAdapter);
                        mGp.syncScheduleAdapter.notifyDataSetChanged();
                        saveScheduleList();
                        refreshOptionMenu();
                        setScheduleTabMessage();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                String del_list = "";
                for (int i = 0; i < mGp.syncScheduleAdapter.getCount(); i++) {
                    if (mGp.syncScheduleAdapter.getItem(i).isChecked) {
                        del_list += "- "+mGp.syncScheduleAdapter.getItem(i).scheduleName + "\n";
                    }
                }
                mUtil.showCommonDialog(true, "W",
                        mContext.getString(R.string.msgs_schedule_confirm_title_delete),
                        mContext.getString(R.string.msgs_schedule_confirm_msg_delete) + "\n" + del_list, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonDelete, mContext.getString(R.string.msgs_schedule_cont_label_delete));

        mContextScheduleButtonActivate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        for (int i = mGp.syncScheduleAdapter.getCount() - 1; i >= 0; i--) {
                            if (mGp.syncScheduleAdapter.getItem(i).isChecked && !mGp.syncScheduleAdapter.getItem(i).scheduleEnabled) {
                                mGp.syncScheduleAdapter.getItem(i).scheduleEnabled = true;
                                mGp.syncScheduleAdapter.getItem(i).isChanged = true;
                                mGp.syncScheduleAdapter.getItem(i).scheduleLastExecTime = System.currentTimeMillis();
                            }
                        }
                        mGp.syncScheduleAdapter.setSelectMode(false);
                        mGp.syncScheduleAdapter.unselectAll();
                        setScheduleContextButtonMode(mGp.syncScheduleAdapter);
                        saveScheduleList();
                        mGp.syncScheduleAdapter.notifyDataSetChanged();
                        refreshOptionMenu();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                String del_list = "";
                for (int i = 0; i<mGp.syncScheduleAdapter.getCount(); i++) {
                    if (mGp.syncScheduleAdapter.getItem(i).isChecked && !mGp.syncScheduleAdapter.getItem(i).scheduleEnabled) {
                        del_list += "- "+mGp.syncScheduleAdapter.getItem(i).scheduleName + "\n";
                    }
                }
                mUtil.showCommonDialog(true, "W",
                        mContext.getString(R.string.msgs_schedule_confirm_title_enable),
                        mContext.getString(R.string.msgs_schedule_confirm_msg_enable) + "\n" + del_list, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonActivate, mContext.getString(R.string.msgs_schedule_cont_label_activate));

        mContextScheduleButtonInactivate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        for (int i = mGp.syncScheduleAdapter.getCount() - 1; i >= 0; i--) {
                            if (mGp.syncScheduleAdapter.getItem(i).isChecked && mGp.syncScheduleAdapter.getItem(i).scheduleEnabled) {
                                mGp.syncScheduleAdapter.getItem(i).scheduleEnabled = false;
                                mGp.syncScheduleAdapter.getItem(i).isChanged = true;
                            }
                        }
                        mGp.syncScheduleAdapter.setSelectMode(false);
                        mGp.syncScheduleAdapter.unselectAll();
                        setScheduleContextButtonMode(mGp.syncScheduleAdapter);
                        saveScheduleList();
                        mGp.syncScheduleAdapter.notifyDataSetChanged();
                        refreshOptionMenu();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                String del_list = "";
                for (int i = 0; i<mGp.syncScheduleAdapter.getCount(); i++) {
                    if (mGp.syncScheduleAdapter.getItem(i).isChecked && mGp.syncScheduleAdapter.getItem(i).scheduleEnabled) {
                        del_list += "- "+mGp.syncScheduleAdapter.getItem(i).scheduleName + "\n";
                    }
                }
                mUtil.showCommonDialog(true, "W",
                        mContext.getString(R.string.msgs_schedule_confirm_title_disable),
                        mContext.getString(R.string.msgs_schedule_confirm_msg_disable) + "\n" + del_list, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonInactivate, mContext.getString(R.string.msgs_schedule_cont_label_inactivate));

        mContextScheduleButtonRename.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        mGp.syncScheduleAdapter.setSelectMode(false);
                        mGp.syncScheduleAdapter.sort();
                        mGp.syncScheduleAdapter.unselectAll();
                        setScheduleContextButtonMode(mGp.syncScheduleAdapter);
                        saveScheduleList();
                        mGp.syncScheduleAdapter.notifyDataSetChanged();
                        refreshOptionMenu();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                ScheduleItem si = null;
                for (int i = mGp.syncScheduleAdapter.getCount() - 1; i >= 0; i--) {
                    if (mGp.syncScheduleAdapter.getItem(i).isChecked) {
                        si = mGp.syncScheduleAdapter.getItem(i);
                        break;
                    }
                }
                if (si==null || si.scheduleName==null) {
                    mUtil.addLogMsg("E","renameSchedule error, schedule item can not be found.");
                    mUtil.showCommonDialog(false, "E", "renameSchedule error, schedule item can not be found.", "", null);
                } else {
                    renameSchedule(si, ntfy);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonRename, mContext.getString(R.string.msgs_schedule_cont_label_rename));

        mContextScheduleButtonCopy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        ScheduleItem si = (ScheduleItem) objects[0];
                        mGp.syncScheduleAdapter.setSelectMode(false);
                        mGp.syncScheduleAdapter.add(si);
                        mGp.syncScheduleAdapter.unselectAll();
                        mGp.syncScheduleAdapter.sort();
                        saveScheduleList();
                        mGp.syncScheduleAdapter.notifyDataSetChanged();
                        refreshOptionMenu();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                ScheduleItem si = null;
                for (int i = mGp.syncScheduleAdapter.getCount() - 1; i >= 0; i--) {
                    if (mGp.syncScheduleAdapter.getItem(i).isChecked) {
                        si = mGp.syncScheduleAdapter.getItem(i);
                        ScheduleItem new_si = si.clone();
                        ScheduleItemEditor sm = new ScheduleItemEditor(mUtil, mActivity, mContext, mCommonDlg, ccMenu, mGp,
                                false, mGp.syncScheduleList, new_si, ntfy);
                        break;
                    }
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonCopy, mContext.getString(R.string.msgs_schedule_cont_label_copy));

        mContextScheduleButtonSelectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mGp.syncScheduleAdapter.setSelectMode(true);
                mGp.syncScheduleAdapter.selectAll();
                setScheduleContextButtonMode(mGp.syncScheduleAdapter);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonSelectAll, mContext.getString(R.string.msgs_schedule_cont_label_select_all));

        mContextScheduleButtonUnselectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mGp.syncScheduleAdapter.unselectAll();
                setScheduleContextButtonMode(mGp.syncScheduleAdapter);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonUnselectAll, mContext.getString(R.string.msgs_schedule_cont_label_unselect_all));

    }

    private void renameSchedule(final ScheduleItem si, final NotifyEvent p_ntfy) {

        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.setContentView(R.layout.single_item_input_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
        CommonUtilities.setDialogBoxOutline(mContext, ll_dlg_view);

//        Drawable db = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.dialog_box_outline, null);
//        ll_dlg_view.setBackground(db);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.single_item_input_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);

        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
        final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
        final Button btn_ok = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
        final EditText etInput = (EditText) dialog.findViewById(R.id.single_item_input_dir);

        title.setText(mContext.getString(R.string.msgs_schedule_rename_schedule));

        dlg_cmp.setVisibility(TextView.VISIBLE);
        dlg_cmp.setText(mContext.getString(R.string.msgs_schedule_confirm_msg_rename_warning));
        CommonDialog.setDlgBoxSizeCompactWithInput(dialog);
        etInput.setText(si.scheduleName);

        //do not check for whole schedule item validity, but only for schedule name
        //this is to allow renaming a schedule that has an invalid name AND an invalid sync task list
        //else the schedule can no more be edited: EditSchedule cannot change name
        String e_msg=ScheduleUtil.isValidScheduleName(mContext, mGp, mGp.syncScheduleList, si.scheduleName, false, false);
        dlg_msg.setText(e_msg);
        if (!e_msg.equals("")) dlg_msg.setVisibility(TextView.VISIBLE);

        btn_ok.setEnabled(false);
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                String error_msg = ScheduleUtil.isValidScheduleName(mContext, mGp, mGp.syncScheduleList, arg0.toString(), false, false);
                if (!error_msg.equals("")) {
                    dlg_msg.setVisibility(TextView.VISIBLE);
                    dlg_msg.setText(error_msg);
                    dlg_msg.setTextColor(mGp.themeColorList.text_color_error);
                    CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    return;
                } else {
                    btn_ok.setEnabled(true);
                    dlg_msg.setVisibility(TextView.GONE);
                    dlg_msg.setText("");
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }
        });

        //OK button
        btn_ok.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                String new_name = etInput.getText().toString();

                si.scheduleName = new_name;
                si.isChanged = true;

                p_ntfy.notifyToListener(true, null);
            }
        });
        // CANCELボタンの指定
        btn_cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btn_cancel.performClick();
            }
        });
        dialog.show();

    }

    private void setScheduleViewItemClickListener() {
        mGp.syncScheduleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                Log.v("","before="+adapter.getItem(i).scheduleName);
                if (isUiEnabled()) {
                    if (mGp.syncScheduleAdapter.isSelectMode()) {
                        if (mGp.syncScheduleAdapter.getItem(i).isChecked) {
                            mGp.syncScheduleAdapter.getItem(i).isChecked = false;
                        } else {
                            mGp.syncScheduleAdapter.getItem(i).isChecked = true;
                        }
                        mGp.syncScheduleAdapter.notifyDataSetChanged();
                        setScheduleContextButtonMode(mGp.syncScheduleAdapter);
                    } else {
                        NotifyEvent ntfy = new NotifyEvent(mContext);
                        ntfy.setListener(new NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context context, Object[] objects) {
                                saveScheduleList();
                                mGp.syncScheduleAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void negativeResponse(Context context, Object[] objects) {
                            }
                        });
                        ScheduleItemEditor sm = new ScheduleItemEditor(mUtil, mActivity, mContext, mCommonDlg, ccMenu, mGp,
                                true, mGp.syncScheduleList, mGp.syncScheduleList.get(i), ntfy);
                    }
                }
            }
        });

        //toggle switch to enable/disable schedule item in Schedule TAB
        NotifyEvent ntfy_sw = new NotifyEvent(mContext);
        ntfy_sw.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                int pos=(int)objects[0];
                if (mGp.syncScheduleAdapter.getItem(pos).scheduleEnabled) {
                    mGp.syncScheduleAdapter.getItem(pos).scheduleLastExecTime = System.currentTimeMillis();
                }
                saveScheduleList();
                mGp.syncScheduleAdapter.notifyDataSetChanged();
                refreshOptionMenu();
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {
            }
        });
        mGp.syncScheduleAdapter.setSwNotify(ntfy_sw);

        //start schedule button in front of each schedule item
        NotifyEvent ntfy_sync=new NotifyEvent(mContext);
        ntfy_sync.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                ScheduleItem sched_item=(ScheduleItem)objects[0];
                String e_msg= checkExecuteScheduleConditions(sched_item);
                if (e_msg.equals("")) {
                    try {
                        mSvcClient.aidlStartSchedule(new String[]{sched_item.scheduleName});
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_schedule_sync_task_dialog_title), e_msg, null);
                }
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        mGp.syncScheduleAdapter.setSyncButtonNotify(ntfy_sync);

        NotifyEvent ntfy_check=new NotifyEvent(mContext);
        ntfy_check.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                refreshOptionMenu();
                setScheduleContextButtonNormalMode();
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        mGp.syncScheduleAdapter.setCbNotify(ntfy_check);
    }

    private void executeSelectedSchedule() {
        ArrayList<String>sched_list=new ArrayList<String>();
        String e_msg="";
        for(ScheduleItem si:mGp.syncScheduleList) {
            if (si.isChecked) {
                e_msg=ScheduleUtil.isValidScheduleItem(mContext, mGp, mGp.syncScheduleList, si, true, false);
                if (e_msg.equals("")) {
                    e_msg=checkExecuteScheduleConditions(si);
                    if (e_msg.equals("")) {
                        sched_list.add(si.scheduleName);
                    } else {
                        mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_schedule_sync_task_dialog_title),
                                mContext.getString(R.string.msgs_schedule_sync_selected_schedule_invalid_schedule_conditions_error, si.scheduleName, e_msg), null);
                        break;
                    }
                } else {
                    mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_schedule_sync_task_dialog_title),
                            mContext.getString(R.string.msgs_schedule_sync_selected_schedule_invalid_schedule_error, si.scheduleName), null);
                    break;
                }
            }
        }
        if (e_msg.equals("")) {
            if (sched_list.size() > 0) {
                String[]start_array=sched_list.toArray(new String[sched_list.size()]);
                try {
                    mSvcClient.aidlStartSchedule(start_array);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_schedule_sync_task_dialog_title),
                        mContext.getString(R.string.msgs_schedule_sync_selected_schedule_not_found), null);
            }
        }
    }

    private void executeAllEnabledSchedule() {
        ArrayList<String>sched_list=new ArrayList<String>();
        String e_msg="";
        for(ScheduleItem si:mGp.syncScheduleList) {
            if (si.scheduleEnabled) {
                e_msg=ScheduleUtil.isValidScheduleItem(mContext, mGp, mGp.syncScheduleList, si, true, false);
                if (e_msg.equals("")) {
                    e_msg=checkExecuteScheduleConditions(si);
                    if (e_msg.equals("")) {
                        sched_list.add(si.scheduleName);
                    } else {
                        mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_schedule_sync_task_dialog_title),
                                mContext.getString(R.string.msgs_schedule_sync_enabled_schedule_invalid_schedule_conditions_error, si.scheduleName, e_msg), null);
                        break;
                    }
                } else {
                    mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_schedule_sync_task_dialog_title),
                            mContext.getString(R.string.msgs_schedule_sync_enabled_schedule_invalid_schedule_error, si.scheduleName), null);
                    break;
                }
            }
        }
        if (e_msg.equals("")) {
            if (sched_list.size()>0) {
                String[]start_array=sched_list.toArray(new String[sched_list.size()]);
                try {
                    mSvcClient.aidlStartSchedule(start_array);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_schedule_sync_task_dialog_title),
                        mContext.getString(R.string.msgs_schedule_sync_enabled_schedule_not_found), null);
            }
        }
    }

    private String checkExecuteScheduleConditions(ScheduleItem sched_item) {
        String e_msg="";
        if (sched_item.syncOverrideOptionCharge.equals(ScheduleItem.OVERRIDE_SYNC_OPTION_ENABLED)) {
            if (!CommonUtilities.isCharging(mContext, mUtil)) {
                e_msg=mContext.getString(R.string.msgs_mirror_sync_cancelled_battery_option_not_satisfied);
            }
        }

        return e_msg;
    }


    private void setScheduleViewLongClickListener() {
        mGp.syncScheduleListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (isUiEnabled()) {
                    mGp.syncScheduleAdapter.setSelectMode(true);
                    mGp.syncScheduleAdapter.getItem(i).isChecked = !mGp.syncScheduleAdapter.getItem(i).isChecked;
                    mGp.syncScheduleAdapter.notifyDataSetChanged();
                    setScheduleContextButtonMode(mGp.syncScheduleAdapter);
                }
                return true;
            }
        });
    }

    private void saveScheduleList() {
        mGp.syncScheduleAdapter.sort();
        ScheduleUtil.saveScheduleData(mActivity, mGp, mGp.syncScheduleList);
        ScheduleUtil.sendTimerRequest(mContext, SCHEDULER_INTENT_SET_TIMER);
        ScheduleUtil.setSchedulerInfo(mActivity, mGp, mUtil);
        SyncTaskUtil.autosaveSyncTaskList(mGp, mActivity, mUtil, mCommonDlg, mGp.syncTaskList);
    }

    private void setScheduleContextButtonNormalMode() {
        setScheduleContextButtonMode(mGp.syncScheduleAdapter);
    }

    private void setScheduleContextButtonMode(AdapterScheduleList adapter) {
        boolean selected = false;
        int sel_cnt = 0;
        boolean enabled = false, disabled = false;
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).isChecked) {
                selected = true;
                sel_cnt++;
                if (adapter.getItem(i).scheduleEnabled) enabled = true;
                else disabled = true;
            }
        }

        mContextScheduleButtonAddView.setVisibility(LinearLayout.VISIBLE);
        mContextScheduleButtonActivateView.setVisibility(LinearLayout.INVISIBLE);
        mContextScheduleButtonInactivateView.setVisibility(LinearLayout.INVISIBLE);
        mContextScheduleButtonCopyView.setVisibility(LinearLayout.INVISIBLE);
        mContextScheduleButtonRenameView.setVisibility(LinearLayout.INVISIBLE);
        mContextScheduleButtonDeleteView.setVisibility(LinearLayout.INVISIBLE);
        mContextScheduleButtonSelectAllView.setVisibility(LinearLayout.VISIBLE);
        mContextScheduleButtonUnselectAllView.setVisibility(LinearLayout.INVISIBLE);

//        final ImageButton btn_ok = (ImageButton) mDialog.findViewById(R.id.schedule_list_edit_dlg_save);
//        final ImageButton btn_cancel = (ImageButton) mDialog.findViewById(R.id.schedule_list_edit_dlg_close);

        if (adapter.isSelectMode()) {
            if (sel_cnt == 0) {
                mContextScheduleButtonAddView.setVisibility(LinearLayout.INVISIBLE);
                mContextScheduleButtonActivateView.setVisibility(LinearLayout.INVISIBLE);
                mContextScheduleButtonInactivateView.setVisibility(LinearLayout.INVISIBLE);
                mContextScheduleButtonCopyView.setVisibility(LinearLayout.INVISIBLE);
                mContextScheduleButtonRenameView.setVisibility(LinearLayout.INVISIBLE);
                mContextScheduleButtonDeleteView.setVisibility(LinearLayout.INVISIBLE);
                mContextScheduleButtonSelectAllView.setVisibility(LinearLayout.VISIBLE);
                mContextScheduleButtonUnselectAllView.setVisibility(LinearLayout.INVISIBLE);
            } else if (sel_cnt == 1) {
                mContextScheduleButtonAddView.setVisibility(LinearLayout.INVISIBLE);
                if (disabled) mContextScheduleButtonActivateView.setVisibility(LinearLayout.VISIBLE);
                if (enabled) mContextScheduleButtonInactivateView.setVisibility(LinearLayout.VISIBLE);
                mContextScheduleButtonCopyView.setVisibility(LinearLayout.VISIBLE);
                mContextScheduleButtonRenameView.setVisibility(LinearLayout.VISIBLE);
                mContextScheduleButtonDeleteView.setVisibility(LinearLayout.VISIBLE);
                mContextScheduleButtonSelectAllView.setVisibility(LinearLayout.VISIBLE);
                mContextScheduleButtonUnselectAllView.setVisibility(LinearLayout.VISIBLE);
            } else if (sel_cnt >= 2) {
                mContextScheduleButtonAddView.setVisibility(LinearLayout.INVISIBLE);
                if (disabled) mContextScheduleButtonActivateView.setVisibility(LinearLayout.VISIBLE);
                if (enabled) mContextScheduleButtonInactivateView.setVisibility(LinearLayout.VISIBLE);
                mContextScheduleButtonCopyView.setVisibility(LinearLayout.INVISIBLE);
                mContextScheduleButtonRenameView.setVisibility(LinearLayout.INVISIBLE);
                mContextScheduleButtonDeleteView.setVisibility(LinearLayout.VISIBLE);
                mContextScheduleButtonSelectAllView.setVisibility(LinearLayout.VISIBLE);
                mContextScheduleButtonUnselectAllView.setVisibility(LinearLayout.VISIBLE);
            }
//            btn_ok.setVisibility(Button.INVISIBLE);
//            btn_cancel.setVisibility(Button.INVISIBLE);
//            btn_ok.setEnabled(false);
//            btn_cancel.setEnabled(false);
        } else {
            mContextScheduleButtonAddView.setVisibility(LinearLayout.VISIBLE);
            mContextScheduleButtonActivateView.setVisibility(LinearLayout.INVISIBLE);
            mContextScheduleButtonInactivateView.setVisibility(LinearLayout.INVISIBLE);
            mContextScheduleButtonCopyView.setVisibility(LinearLayout.INVISIBLE);
            mContextScheduleButtonRenameView.setVisibility(LinearLayout.INVISIBLE);
            mContextScheduleButtonDeleteView.setVisibility(LinearLayout.INVISIBLE);
            mContextScheduleButtonUnselectAllView.setVisibility(LinearLayout.INVISIBLE);
            if (adapter.getCount() == 0) {
                mContextScheduleButtonSelectAllView.setVisibility(LinearLayout.INVISIBLE);
            }
//            if (isScheduleListChanged()) btn_ok.setEnabled(true);
//            else btn_ok.setEnabled(false);
//            btn_cancel.setEnabled(true);
        }

    }

    private boolean canListViewScrollDown(ListView lv) {
        if (lv == null) return false;
        if (lv.getChildAt(lv.getChildCount() - 1) == null) return false;

        boolean result=true;
        if (lv.getLastVisiblePosition() == lv.getAdapter().getCount()-1 &&
                lv.getChildAt(lv.getChildCount() - 1).getBottom() <= lv.getHeight()) {
            result=false;
        }

        return result;
    }

    private boolean canListViewScrollUp(ListView lv) {
        if (lv == null) return false;
        if (lv.getChildAt(0) == null) return false;

        boolean result=true;
        if (lv.getFirstVisiblePosition() == 0 && lv.getChildAt(0).getTop() >= 0) {
            result=false;
        }

        return result;
    }

    private void setHistoryScrollButtonVisibility() {
        Handler hndl=new Handler();
        hndl.post(new Runnable(){
            @Override
            public void run() {
                if (canListViewScrollDown(mGp.syncHistoryListView)) {
                    mContextHistoryButtonScrollDown.setVisibility(LinearLayout.VISIBLE);
                    mContextHistoryButtonPageDown.setVisibility(LinearLayout.VISIBLE);
//                    mContextHistoryButtonMoveBottom.setVisibility(LinearLayout.VISIBLE);
                } else {
                    mContextHistoryButtonScrollDown.setVisibility(LinearLayout.INVISIBLE);
                    mContextHistoryButtonPageDown.setVisibility(LinearLayout.INVISIBLE);
//                    mContextHistoryButtonMoveBottom.setVisibility(LinearLayout.INVISIBLE);
                }
                if (canListViewScrollUp(mGp.syncHistoryListView)) {
                    mContextHistoryButtonScrollUp.setVisibility(LinearLayout.VISIBLE);
                    mContextHistoryButtonPageUp.setVisibility(LinearLayout.VISIBLE);
//                    mContextHistoryButtonMoveTop.setVisibility(LinearLayout.VISIBLE);
                } else {
                    mContextHistoryButtonScrollUp.setVisibility(LinearLayout.INVISIBLE);
                    mContextHistoryButtonPageUp.setVisibility(LinearLayout.INVISIBLE);
//                    mContextHistoryButtonMoveTop.setVisibility(LinearLayout.INVISIBLE);
                }
            }
        });
    }

    private final static int HISTORY_SCROLL_AMOUNT=1;
    private void setHistoryContextButtonListener() {
        setHistoryScrollButtonVisibility();
        mGp.syncHistoryListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                setHistoryScrollButtonVisibility();
            }
        });

        mContextHistoryButtonSendTo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextHistoryButtonSendTo, false);
                if (isUiEnabled()) {
                    sendHistoryFile();
                    mGp.syncHistoryAdapter.setAllItemChecked(false);
                    mGp.syncHistoryAdapter.setShowCheckBox(false);
                    mGp.syncHistoryAdapter.notifyDataSetChanged();
                    setHistoryContextButtonNormalMode();
                }
                setContextButtonEnabled(mContextHistoryButtonSendTo, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistoryButtonSendTo, mContext.getString(R.string.msgs_hist_cont_label_share));

        mContextHistoryButtonScrollUp.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, DEFAULT_LONG_PRESS_REPEAT_INTERVAL, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int sel = mGp.syncHistoryListView.getFirstVisiblePosition() - HISTORY_SCROLL_AMOUNT;
                if (sel > mGp.syncHistoryAdapter.getCount() - 1) sel = mGp.syncHistoryAdapter.getCount() - 1;
                if (sel < 0) sel = 0;
                mGp.syncHistoryListView.setSelection(sel);
                setHistoryScrollButtonVisibility();
            }
        }));

        mContextHistoryButtonScrollDown.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, DEFAULT_LONG_PRESS_REPEAT_INTERVAL, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int sel = mGp.syncHistoryListView.getFirstVisiblePosition() + HISTORY_SCROLL_AMOUNT;
                if (sel > mGp.syncHistoryAdapter.getCount() - 1) sel = mGp.syncHistoryAdapter.getCount() - 1;
                if (sel < 0) sel = 0;
                mGp.syncHistoryListView.setSelection(sel);
                setHistoryScrollButtonVisibility();
            }
        }));

        mContextHistoryButtonPageUp.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, DEFAULT_LONG_PRESS_REPEAT_INTERVAL, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int lv_height = mGp.syncHistoryListView.getHeight();
                int first_item_y_top =  mGp.syncHistoryListView.getChildAt(0).getTop();
                int first_item_y_bottom =  mGp.syncHistoryListView.getChildAt(0).getBottom();
                int first_item_height = first_item_y_bottom - first_item_y_top;
                int y_offset = 0;
                if (first_item_y_top < 0) {
                    // part of first item is hidden on top
                    y_offset = first_item_height;
                    if (y_offset > lv_height) {
                        //item is more than one page: position to the bottom, the current top exact last visible position, minus 3 text lines
                        TextView listTextView = (TextView) mGp.syncHistoryListView.getChildAt(0).findViewById(R.id.sync_history_list_view_date);
                        int text_context_size = 0;
                        if (listTextView != null) text_context_size = (int)(listTextView.getTextSize() * 3);
                        y_offset = first_item_height - first_item_y_bottom + text_context_size;
                    }
                }

                //mUtil.addDebugMsg(2, "I", "lv_height="+lv_height + " first_item_height="+first_item_height + " first_item_y_top="+first_item_y_top + " first_item_y_bottom="+first_item_y_bottom);
                mGp.syncHistoryListView.setItemChecked(mGp.syncHistoryListView.getFirstVisiblePosition(), true);//needed on app start to set touch focus
                mGp.syncHistoryListView.setSelectionFromTop(mGp.syncHistoryListView.getFirstVisiblePosition(), lv_height - y_offset);
                setHistoryScrollButtonVisibility();
            }
        }));

        mContextHistoryButtonPageDown.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, DEFAULT_LONG_PRESS_REPEAT_INTERVAL, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int last_item_pos = mGp.syncHistoryListView.getLastVisiblePosition() - mGp.syncHistoryListView.getFirstVisiblePosition();
                int lv_height = mGp.syncHistoryListView.getHeight();
                int last_item_y_top =  mGp.syncHistoryListView.getChildAt(last_item_pos).getTop();
                int last_item_y_bottom =  mGp.syncHistoryListView.getChildAt(last_item_pos).getBottom();
                int last_item_height = last_item_y_bottom - last_item_y_top;
                int y_offset = 0;

                if (last_item_height > lv_height) {
                    //item is more than one page: position to the top, the current bottom exat last visible position, minus 3 text lines
                    TextView listTextView = (TextView) mGp.syncHistoryListView.getChildAt(last_item_pos).findViewById(R.id.sync_history_list_view_date);
                    int text_context_size = 0;
                    if (listTextView != null) text_context_size = (int)(listTextView.getTextSize() * 3);
                    y_offset = -(lv_height - last_item_y_top - text_context_size);
                }

                //mUtil.addDebugMsg(2, "I", "y_offset="+y_offset + " last_item_height="+last_item_height + " last_item_y_top="+last_item_y_top);
                mGp.syncHistoryListView.setItemChecked(mGp.syncHistoryListView.getLastVisiblePosition(), true);
                mGp.syncHistoryListView.setSelectionFromTop(mGp.syncHistoryListView.getLastVisiblePosition(), y_offset);
                setHistoryScrollButtonVisibility();
            }
        }));

        mContextHistoryButtonMoveTop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mGp.syncHistoryListView.setSelection(0);
                setHistoryScrollButtonVisibility();
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistoryButtonMoveTop, mContext.getString(R.string.msgs_hist_cont_label_move_top));

        mContextHistoryButtonMoveBottom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mGp.syncHistoryListView.setSelection(mGp.syncHistoryAdapter.getCount() - 1);
                setHistoryScrollButtonVisibility();
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistoryButtonMoveBottom, mContext.getString(R.string.msgs_hist_cont_label_move_bottom));

        mContextHistoryButtonDeleteHistory.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    confirmDeleteHistory();
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistoryButtonDeleteHistory, mContext.getString(R.string.msgs_hist_cont_label_delete));

        mContextHistoryButtonHistiryCopyClipboard.setOnClickListener(new OnClickListener() {
            private long last_show_time = 0;

            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextHistoryButtonHistiryCopyClipboard, false);
                if (isUiEnabled()) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    StringBuilder out = new StringBuilder(256);
                    for (int i = 0; i < mGp.syncHistoryAdapter.getCount(); i++) {
                        if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
                            SyncHistoryItem hli = mGp.syncHistoryAdapter.getItem(i);
                            out.append(hli.sync_date).append(" ");
                            out.append(hli.sync_time).append(" ");
                            out.append(hli.sync_prof).append("\n");
                            if (hli.sync_status == SyncHistoryItem.SYNC_STATUS_SUCCESS) {
                                out.append(mContext.getString(R.string.msgs_main_sync_history_status_success)).append("\n");
                            } else if (hli.sync_status == SyncHistoryItem.SYNC_STATUS_ERROR) {
                                out.append(mContext.getString(R.string.msgs_main_sync_history_status_error)).append("\n");
                            } else if (hli.sync_status == SyncHistoryItem.SYNC_STATUS_CANCEL) {
                                out.append(mContext.getString(R.string.msgs_main_sync_history_status_cancel)).append("\n");
                            }
                            out.append(mContext.getString(R.string.msgs_main_sync_history_count_copied))
                                    .append(Integer.toString(hli.sync_result_no_of_copied)).append(" ");
                            out.append(mContext.getString(R.string.msgs_main_sync_history_count_deleted))
                                    .append(Integer.toString(hli.sync_result_no_of_deleted)).append(" ");
                            out.append(mContext.getString(R.string.msgs_main_sync_history_count_ignored))
                                    .append(Integer.toString(hli.sync_result_no_of_ignored)).append(" ");
                            out.append("\n").append(hli.sync_error_text);
                        }
                    }
                    if (out.length() > 0) cm.setText(out);
                    if ((last_show_time + Toast.LENGTH_SHORT) < System.currentTimeMillis()) {
                        CommonUtilities.showToastMessageShort(mActivity, mContext.getString(R.string.msgs_main_sync_history_copy_completed));
                        last_show_time = System.currentTimeMillis();
                    }
                    mGp.syncHistoryAdapter.setAllItemChecked(false);
                    mGp.syncHistoryAdapter.setShowCheckBox(false);
                    mGp.syncHistoryAdapter.notifyDataSetChanged();
                    setHistoryContextButtonNormalMode();
                }
                setContextButtonEnabled(mContextHistoryButtonHistiryCopyClipboard, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistoryButtonHistiryCopyClipboard, mContext.getString(R.string.msgs_hist_cont_label_copy));

        mContextHistiryButtonSelectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextHistiryButtonSelectAll, false);
                    setHistoryItemSelectAll();
                    mGp.syncHistoryAdapter.setShowCheckBox(true);
                    setHistoryContextButtonSelectMode();
                    setContextButtonEnabled(mContextHistiryButtonSelectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistiryButtonSelectAll, mContext.getString(R.string.msgs_hist_cont_label_select_all));

        mContextHistiryButtonUnselectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextHistiryButtonUnselectAll, false);
                    setHistoryItemUnselectAll();
                    //				mGp.syncHistoryAdapter.setShowCheckBox(false);
                    //				setHistoryContextButtonNotselected();
                    setContextButtonEnabled(mContextHistiryButtonUnselectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistiryButtonUnselectAll, mContext.getString(R.string.msgs_hist_cont_label_unselect_all));
    }

    private void setHistoryContextButtonSelectMode() {
        int sel_cnt = mGp.syncHistoryAdapter.getItemSelectedCount();
        int tot_cnt = mGp.syncHistoryAdapter.getCount();
        setActionBarSelectMode(sel_cnt, tot_cnt);

//        mContextHistiryViewMoveTop.setVisibility(ImageButton.VISIBLE);
//        mContextHistiryViewMoveBottom.setVisibility(ImageButton.VISIBLE);

//		if (sel_cnt==1) ll_show_log.setVisibility(ImageButton.VISIBLE);
//		else ll_show_log.setVisibility(ImageButton.INVISIBLE);
        if (sel_cnt > 0) {
            mContextHistiryViewShare.setVisibility(ImageButton.VISIBLE);
            mContextHistiryViewDeleteHistory.setVisibility(ImageButton.VISIBLE);
            mContextHistiryViewHistoryCopyClipboard.setVisibility(ImageButton.VISIBLE);
            mContextHistiryViewUnselectAll.setVisibility(ImageButton.VISIBLE);
        } else {
            mContextHistiryViewShare.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewDeleteHistory.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewHistoryCopyClipboard.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewUnselectAll.setVisibility(ImageButton.INVISIBLE);
        }

        if (tot_cnt != sel_cnt) mContextHistiryViewSelectAll.setVisibility(ImageButton.VISIBLE);
        else mContextHistiryViewSelectAll.setVisibility(ImageButton.INVISIBLE);

    }

    private void setHistoryContextButtonNormalMode() {
        setActionBarNormalMode();

        if (!mGp.syncHistoryAdapter.isEmptyAdapter()) {
            mContextHistiryViewShare.setVisibility(ImageButton.INVISIBLE);
//            mContextHistiryViewMoveTop.setVisibility(ImageButton.INVISIBLE);
//            mContextHistiryViewMoveBottom.setVisibility(ImageButton.VISIBLE);
            mContextHistiryViewScrollDown.setVisibility(ImageButton.VISIBLE);
            mContextHistiryViewScrollUp.setVisibility(ImageButton.VISIBLE);
            mContextHistiryViewDeleteHistory.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewHistoryCopyClipboard.setVisibility(ImageButton.INVISIBLE);
            if (isUiEnabled()) mContextHistiryViewSelectAll.setVisibility(ImageButton.VISIBLE);
            else mContextHistiryViewSelectAll.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewUnselectAll.setVisibility(ImageButton.INVISIBLE);
        } else {
            mContextHistiryViewShare.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewShare.setVisibility(ImageButton.INVISIBLE);
//            mContextHistiryViewMoveTop.setVisibility(ImageButton.INVISIBLE);
//            mContextHistiryViewMoveBottom.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewScrollDown.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewScrollUp.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewDeleteHistory.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewHistoryCopyClipboard.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewSelectAll.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewUnselectAll.setVisibility(ImageButton.INVISIBLE);
        }
    }

    private void setHistoryItemUnselectAll() {
        mGp.syncHistoryAdapter.setAllItemChecked(false);
//		for (int i=0;i<mGp.syncHistoryAdapter.getCount();i++) mGp.syncHistoryAdapter.getItem(i).isChecked=false;
//		mGp.syncHistoryAdapter.setShowCheckBox(false);
        mGp.syncHistoryAdapter.notifyDataSetChanged();
        setHistoryContextButtonSelectMode();
    }

    private void setHistoryItemSelectAll() {
        mGp.syncHistoryAdapter.setAllItemChecked(true);
//		for (int i=0;i<mGp.syncHistoryAdapter.getCount();i++) mGp.syncHistoryAdapter.getItem(i).isChecked=true;
        mGp.syncHistoryAdapter.setShowCheckBox(true);
        mGp.syncHistoryAdapter.notifyDataSetChanged();
        setHistoryContextButtonSelectMode();
    }

    @SuppressWarnings("unused")
    private void setHistoryItemChecked(int pos, boolean p) {
        mGp.syncHistoryAdapter.getItem(pos).isChecked = p;
    }

    private void confirmDeleteHistory() {
        String conf_list = "";
        boolean del_all_history = false;
        int del_cnt = 0;
        String sep = "";
        for (int i = 0; i < mGp.syncHistoryAdapter.getCount(); i++) {
            if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
                del_cnt++;
                conf_list += sep + mGp.syncHistoryAdapter.getItem(i).sync_date + " " +
                        mGp.syncHistoryAdapter.getItem(i).sync_time + " " +
                        mGp.syncHistoryAdapter.getItem(i).sync_prof + " ";
                sep = "\n";
            }
        }
        if (del_cnt == mGp.syncHistoryAdapter.getCount()) del_all_history = true;
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                for (int i = mGp.syncHistoryAdapter.getCount() - 1; i >= 0; i--) {
                    if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
                        String result_fp = mGp.syncHistoryAdapter.getItem(i).sync_result_file_path;
                        if (!result_fp.equals("")) {
                            File lf = new File(result_fp);
                            if (lf.exists()) {
                                lf.delete();
                                mUtil.addDebugMsg(1, "I", "Sync history log file deleted, fp=" + result_fp);
                            }
                        }
                        mUtil.addDebugMsg(1, "I", "Sync history item deleted, item=" + mGp.syncHistoryAdapter.getItem(i).sync_prof);
                        mGp.syncHistoryAdapter.remove(mGp.syncHistoryAdapter.getItem(i));
                    }
                }
                mUtil.saveHistoryList(mGp.syncHistoryList);
//				mGp.syncHistoryAdapter.setSyncHistoryList(mUtil.loadHistoryList());
                mGp.syncHistoryAdapter.setShowCheckBox(false);
                mGp.syncHistoryAdapter.notifyDataSetChanged();
                setHistoryContextButtonNormalMode();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });

        if (del_all_history) {
//			subtitle=getString(R.string.msgs_main_sync_history_del_conf_subtitle);
            mUtil.showCommonDialog(true, "W", getString(R.string.msgs_main_sync_history_del_conf_all_history),
                    "", ntfy);
        } else {
//			subtitle=getString(R.string.msgs_main_sync_history_del_conf_subtitle);
            mUtil.showCommonDialog(true, "W", getString(R.string.msgs_main_sync_history_del_conf_selected_history),
                    conf_list, ntfy);
        }
    }

    private void setSyncTaskListItemClickListener() {
        mGp.syncTaskListView.setEnabled(true);
        mGp.syncTaskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (isUiEnabled()) {
                    mGp.syncTaskListView.setEnabled(false);
                    SyncTaskItem item = mGp.syncTaskAdapter.getItem(position);
                    if (!mGp.syncTaskAdapter.isShowCheckBox()) {
                        editSyncTask(item.getSyncTaskName(), item.isSyncTaskAuto(), position);
                        mUiHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mGp.syncTaskListView.setEnabled(true);
                            }
                        }, 1000);
                    } else {
                        item.setChecked(!item.isChecked());
                        setSyncTaskContextButtonSelectMode();
                        mGp.syncTaskListView.setEnabled(true);
                    }
                    mGp.syncTaskAdapter.notifyDataSetChanged();
                }
            }
        });

        NotifyEvent ntfy_cb = new NotifyEvent(mContext);
        ntfy_cb.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (!mGp.syncTaskAdapter.isShowCheckBox()) {
//					syncTaskListAdapter.setShowCheckBox(false);
                    mGp.syncTaskAdapter.notifyDataSetChanged();
                    setSyncTaskContextButtonNormalMode();
                } else {
                    setSyncTaskContextButtonSelectMode();
                }
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mGp.syncTaskAdapter.setNotifyCheckBoxEventHandler(ntfy_cb);

        NotifyEvent ntfy_sync = new NotifyEvent(mContext);
        ntfy_sync.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (isUiEnabled()) {
                    SyncTaskItem sti=(SyncTaskItem)o[0];
                    syncSpecificSyncTask(sti);
                }
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mGp.syncTaskAdapter.setNotifySyncButtonEventHandler(ntfy_sync);

    }

    private void setSyncTaskListLongClickListener() {
        mGp.syncTaskListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> list_view, final View item_view,
                                           int pos, long arg3) {
                if (mGp.syncTaskAdapter.isEmptyAdapter()) return true;
                if (!isUiEnabled()) return true;

                if (!mGp.syncTaskAdapter.getItem(pos).isChecked()) {
                    if (SyncTaskUtil.isSyncTaskSelected(mGp.syncTaskAdapter)) {

                        int down_sel_pos = -1, up_sel_pos = -1;
                        int tot_cnt = mGp.syncTaskAdapter.getCount();
                        if (pos + 1 <= tot_cnt) {
                            for (int i = pos + 1; i < tot_cnt; i++) {
                                if (mGp.syncTaskAdapter.getItem(i).isChecked()) {
                                    up_sel_pos = i;
                                    break;
                                }
                            }
                        }
                        if (pos > 0) {
                            for (int i = pos; i >= 0; i--) {
                                if (mGp.syncTaskAdapter.getItem(i).isChecked()) {
                                    down_sel_pos = i;
                                    break;
                                }
                            }
                        }
    //						Log.v("","up="+up_sel_pos+", down="+down_sel_pos);
                        if (up_sel_pos != -1 && down_sel_pos == -1) {
                            for (int i = pos; i < up_sel_pos; i++)
                                mGp.syncTaskAdapter.getItem(i).setChecked(true);
                        } else if (up_sel_pos != -1 && down_sel_pos != -1) {
                            for (int i = down_sel_pos + 1; i < up_sel_pos; i++)
                                mGp.syncTaskAdapter.getItem(i).setChecked(true);
                        } else if (up_sel_pos == -1 && down_sel_pos != -1) {
                            for (int i = down_sel_pos + 1; i <= pos; i++)
                                mGp.syncTaskAdapter.getItem(i).setChecked(true);
                        }
                        mGp.syncTaskAdapter.notifyDataSetChanged();
                    } else {
                        mGp.syncTaskAdapter.setShowCheckBox(true);
                        mGp.syncTaskAdapter.getItem(pos).setChecked(true);
                        mGp.syncTaskAdapter.notifyDataSetChanged();
                    }
                    setSyncTaskContextButtonSelectMode();
                }
                return true;
            }
        });
    }

    private ImageButton mContextSyncTaskButtonAutoTask = null;
    private ImageButton mContextSyncTaskButtonManualTask = null;
    private ImageButton mContextSyncTaskButtonAddSync = null;
    private ImageButton mContextSyncTaskButtonCopySyncTask = null;
    private ImageButton mContextSyncTaskButtonDeleteSyncTask = null;
    private ImageButton mContextSyncTaskButtonRenameSyncTask = null;
    private ImageButton mContextSyncTaskButtonMoveToUp = null;
    private ImageButton mContextSyncTaskButtonMoveToDown = null;
    private ImageButton mContextSyncTaskButtonSelectAll = null;
    private ImageButton mContextSyncTaskButtonUnselectAll = null;

    private LinearLayout mContextSyncTaskViewAutoTask = null;
    private LinearLayout mContextSyncTaskViewManualTask = null;
    private LinearLayout mContextSyncTaskViewAddSync = null;
    private LinearLayout mContextSyncTaskViewCopySyncTask = null;
    private LinearLayout mContextSyncTaskViewDeleteSyncTask = null;
    private LinearLayout mContextSyncTaskViewRenameSyncTask = null;
    private LinearLayout mContextSyncTaskViewMoveToUp = null;
    private LinearLayout mContextSyncTaskViewMoveToDown = null;
    private LinearLayout mContextSyncTaskViewSelectAll = null;
    private LinearLayout mContextSyncTaskViewUnselectAll = null;

    private ImageButton mContextHistoryButtonSendTo = null;
    private ImageButton mContextHistoryButtonMoveBottom = null;
    private ImageButton mContextHistoryButtonMoveTop = null;
    private ImageButton mContextHistoryButtonScrollDown = null;
    private ImageButton mContextHistoryButtonScrollUp = null;
    private ImageButton mContextHistoryButtonPageDown = null;
    private ImageButton mContextHistoryButtonPageUp = null;
    private ImageButton mContextHistoryButtonDeleteHistory = null;
    private ImageButton mContextHistoryButtonHistiryCopyClipboard = null;
    private ImageButton mContextHistiryButtonSelectAll = null;
    private ImageButton mContextHistiryButtonUnselectAll = null;

    private LinearLayout mContextHistiryViewShare = null;
    private LinearLayout mContextHistiryViewMoveTop = null;
    private LinearLayout mContextHistiryViewMoveBottom = null;
    private LinearLayout mContextHistiryViewScrollDown = null;
    private LinearLayout mContextHistiryViewScrollUp = null;
    private LinearLayout mContextHistiryViewPageDown = null;
    private LinearLayout mContextHistiryViewPageUp = null;
    private LinearLayout mContextHistiryViewDeleteHistory = null;
    private LinearLayout mContextHistiryViewHistoryCopyClipboard = null;
    private LinearLayout mContextHistiryViewSelectAll = null;
    private LinearLayout mContextHistiryViewUnselectAll = null;

    private ImageButton mContextScheduleButtonAdd = null;
    private ImageButton mContextScheduleButtonActivate = null;
    private ImageButton mContextScheduleButtonInactivate = null;
    private ImageButton mContextScheduleButtonCopy = null;
    private ImageButton mContextScheduleButtonRename = null;
    private ImageButton mContextScheduleButtonDelete = null;
    private ImageButton mContextScheduleButtonSelectAll = null;
    private ImageButton mContextScheduleButtonUnselectAll = null;

    private LinearLayout mContextScheduleView = null;

    private LinearLayout mContextScheduleButtonAddView = null;
    private LinearLayout mContextScheduleButtonActivateView = null;
    private LinearLayout mContextScheduleButtonInactivateView = null;
    private LinearLayout mContextScheduleButtonCopyView = null;
    private LinearLayout mContextScheduleButtonRenameView = null;
    private LinearLayout mContextScheduleButtonDeleteView = null;
    private LinearLayout mContextScheduleButtonSelectAllView = null;
    private LinearLayout mContextScheduleButtonUnselectAllView = null;


    private ImageButton mContextMessageButtonMoveTop = null;
    private ImageButton mContextMessageButtonPinned = null;
    private ImageButton mContextMessageButtonMoveBottom = null;
    private ImageButton mContextMessageButtonScrollDown = null;
    private ImageButton mContextMessageButtonScrollUp = null;
    private ImageButton mContextMessageButtonPageDown = null;
    private ImageButton mContextMessageButtonPageUp = null;
    private ImageButton mContextMessageButtonClear = null;

    private LinearLayout mContextMessageViewMoveTop = null;
    private LinearLayout mContextMessageViewPinned = null;
    private LinearLayout mContextMessageViewMoveBottom = null;
    private LinearLayout mContextMessageViewScrollDown = null;
    private LinearLayout mContextMessageViewScrollUp = null;
    private LinearLayout mContextMessageViewPageDown = null;
    private LinearLayout mContextMessageViewPageUp = null;
    private LinearLayout mContextMessageViewClear = null;

    private void releaseImageResource() {
        releaseImageBtnRes(mContextSyncTaskButtonAutoTask);
        releaseImageBtnRes(mContextSyncTaskButtonManualTask);
        releaseImageBtnRes(mContextSyncTaskButtonAddSync);
        releaseImageBtnRes(mContextSyncTaskButtonCopySyncTask);
        releaseImageBtnRes(mContextSyncTaskButtonDeleteSyncTask);
        releaseImageBtnRes(mContextSyncTaskButtonRenameSyncTask);
        releaseImageBtnRes(mContextSyncTaskButtonMoveToUp);
        releaseImageBtnRes(mContextSyncTaskButtonMoveToDown);
        releaseImageBtnRes(mContextSyncTaskButtonSelectAll);
        releaseImageBtnRes(mContextSyncTaskButtonUnselectAll);

        releaseImageBtnRes(mContextHistoryButtonSendTo);
        releaseImageBtnRes(mContextHistoryButtonMoveTop);
        releaseImageBtnRes(mContextHistoryButtonMoveBottom);
        releaseImageBtnRes(mContextHistoryButtonDeleteHistory);
        releaseImageBtnRes(mContextHistoryButtonHistiryCopyClipboard);
        releaseImageBtnRes(mContextHistiryButtonSelectAll);
        releaseImageBtnRes(mContextHistiryButtonUnselectAll);

        releaseImageBtnRes(mContextMessageButtonMoveTop);
        releaseImageBtnRes(mContextMessageButtonPinned);
        releaseImageBtnRes(mContextMessageButtonMoveBottom);
        releaseImageBtnRes(mContextMessageButtonClear);

        mGp.syncTaskListView.setAdapter(null);
        mGp.syncHistoryListView.setAdapter(null);
    }

    private void releaseImageBtnRes(ImageButton ib) {
//    	((BitmapDrawable) ib.getDrawable()).getBitmap().recycle();
        ib.setImageDrawable(null);
//    	ib.setBackground(null);
        ib.setBackgroundDrawable(null);
        ib.setImageBitmap(null);
    }

    private void createContextView() {
        mContextSyncTaskButtonAutoTask = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_auto_task);
        if (ThemeUtil.isLightThemeUsed(mActivity)) mContextSyncTaskButtonAutoTask.setImageResource(R.drawable.smbsync_auto_task_black);
        mContextSyncTaskButtonManualTask = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_inactivate);
        mContextSyncTaskButtonAddSync = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_add_sync);
        mContextSyncTaskButtonCopySyncTask = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_copy);
        mContextSyncTaskButtonDeleteSyncTask = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_delete);
        mContextSyncTaskButtonRenameSyncTask = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_rename);
        mContextSyncTaskButtonMoveToUp = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_up_arrow);
        mContextSyncTaskButtonMoveToDown = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_down_arrow);
        mContextSyncTaskButtonSelectAll = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_select_all);
        mContextSyncTaskButtonUnselectAll = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_unselect_all);

        mContextSyncTaskViewAutoTask = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_auto_task_view);
        mContextSyncTaskViewManualTask = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_inactivate_view);
        mContextSyncTaskViewAddSync = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_add_sync_view);
        mContextSyncTaskViewCopySyncTask = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_copy_view);
        mContextSyncTaskViewDeleteSyncTask = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_delete_view);
        mContextSyncTaskViewRenameSyncTask = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_rename_view);
        mContextSyncTaskViewMoveToUp = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_up_arrow_view);
        mContextSyncTaskViewMoveToDown = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_down_arrow_view);

        mContextSyncTaskViewSelectAll = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_select_all_view);
        mContextSyncTaskViewUnselectAll = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_unselect_all_view);

        mContextScheduleView=(LinearLayout)mScheduleView.findViewById(R.id.context_view_schedule);

        mContextScheduleButtonAddView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_add_view);
        mContextScheduleButtonActivateView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_activate_view);
        mContextScheduleButtonInactivateView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_inactivate_view);
        mContextScheduleButtonCopyView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_copy_view);
        mContextScheduleButtonRenameView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_rename_view);
        mContextScheduleButtonDeleteView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_delete_view);
        mContextScheduleButtonSelectAllView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_select_all_view);
        mContextScheduleButtonUnselectAllView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_unselect_all_view);

        mContextScheduleButtonAdd = (ImageButton) mScheduleView.findViewById(R.id.context_button_add);
        mContextScheduleButtonActivate = (ImageButton) mScheduleView.findViewById(R.id.context_button_activate);
        mContextScheduleButtonInactivate = (ImageButton) mScheduleView.findViewById(R.id.context_button_inactivate);
        mContextScheduleButtonCopy = (ImageButton) mScheduleView.findViewById(R.id.context_button_copy);
        mContextScheduleButtonRename = (ImageButton) mScheduleView.findViewById(R.id.context_button_rename);
        mContextScheduleButtonDelete = (ImageButton) mScheduleView.findViewById(R.id.context_button_delete);
        mContextScheduleButtonSelectAll = (ImageButton) mScheduleView.findViewById(R.id.context_button_select_all);
        mContextScheduleButtonUnselectAll = (ImageButton) mScheduleView.findViewById(R.id.context_button_unselect_all);

        mContextHistoryButtonSendTo = (ImageButton) mHistoryView.findViewById(R.id.context_button_share);
        mContextHistoryButtonMoveTop = (ImageButton) mHistoryView.findViewById(R.id.context_button_move_to_top);
        mContextHistoryButtonMoveBottom = (ImageButton) mHistoryView.findViewById(R.id.context_button_move_to_bottom);
        mContextHistoryButtonScrollDown = (ImageButton) mHistoryView.findViewById(R.id.context_button_scroll_down);
        mContextHistoryButtonScrollUp = (ImageButton) mHistoryView.findViewById(R.id.context_button_scroll_up);
        mContextHistoryButtonPageDown = (ImageButton) mHistoryView.findViewById(R.id.context_button_page_down);
        mContextHistoryButtonPageUp = (ImageButton) mHistoryView.findViewById(R.id.context_button_page_up);
        mContextHistoryButtonDeleteHistory = (ImageButton) mHistoryView.findViewById(R.id.context_button_delete);
        mContextHistoryButtonHistiryCopyClipboard = (ImageButton) mHistoryView.findViewById(R.id.context_button_copy_to_clipboard);
        mContextHistiryButtonSelectAll = (ImageButton) mHistoryView.findViewById(R.id.context_button_select_all);
        mContextHistiryButtonUnselectAll = (ImageButton) mHistoryView.findViewById(R.id.context_button_unselect_all);

        mContextHistiryViewShare = (LinearLayout) mHistoryView.findViewById(R.id.context_button_share_view);
        mContextHistiryViewMoveTop = (LinearLayout) mHistoryView.findViewById(R.id.context_button_move_to_top_view);
        mContextHistiryViewMoveTop.setVisibility(LinearLayout.GONE);
        mContextHistiryViewMoveBottom = (LinearLayout) mHistoryView.findViewById(R.id.context_button_move_to_bottom_view);
        mContextHistiryViewMoveBottom.setVisibility(LinearLayout.GONE);
        mContextHistiryViewScrollDown = (LinearLayout) mHistoryView.findViewById(R.id.context_button_scroll_down_view);
        mContextHistiryViewScrollUp = (LinearLayout) mHistoryView.findViewById(R.id.context_button_scroll_up_view);
        mContextHistiryViewPageDown = (LinearLayout) mHistoryView.findViewById(R.id.context_button_page_down_view);
        mContextHistiryViewPageUp = (LinearLayout) mHistoryView.findViewById(R.id.context_button_page_up_view);
        mContextHistiryViewDeleteHistory = (LinearLayout) mHistoryView.findViewById(R.id.context_button_delete_view);
        mContextHistiryViewHistoryCopyClipboard = (LinearLayout) mHistoryView.findViewById(R.id.context_button_copy_to_clipboard_view);
        mContextHistiryViewSelectAll = (LinearLayout) mHistoryView.findViewById(R.id.context_button_select_all_view);
        mContextHistiryViewUnselectAll = (LinearLayout) mHistoryView.findViewById(R.id.context_button_unselect_all_view);

        mContextMessageButtonPinned = (ImageButton) mMessageView.findViewById(R.id.context_button_pinned);
        mContextMessageButtonMoveTop = (ImageButton) mMessageView.findViewById(R.id.context_button_move_to_top);
        mContextMessageButtonMoveBottom = (ImageButton) mMessageView.findViewById(R.id.context_button_move_to_bottom);
        mContextMessageButtonScrollDown = (ImageButton) mMessageView.findViewById(R.id.context_button_scroll_down);
        mContextMessageButtonScrollUp = (ImageButton) mMessageView.findViewById(R.id.context_button_scroll_up);
        mContextMessageButtonPageDown = (ImageButton) mMessageView.findViewById(R.id.context_button_page_down);
        mContextMessageButtonPageUp = (ImageButton) mMessageView.findViewById(R.id.context_button_page_up);
        mContextMessageButtonClear = (ImageButton) mMessageView.findViewById(R.id.context_button_delete);

        mContextMessageViewPinned = (LinearLayout) mMessageView.findViewById(R.id.context_button_pinned_view);
        mContextMessageViewMoveTop = (LinearLayout) mMessageView.findViewById(R.id.context_button_move_to_top_view);
        mContextMessageViewMoveBottom = (LinearLayout) mMessageView.findViewById(R.id.context_button_move_to_bottom_view);
        mContextMessageViewScrollDown = (LinearLayout) mMessageView.findViewById(R.id.context_button_scroll_down_view);
        mContextMessageViewScrollUp = (LinearLayout) mMessageView.findViewById(R.id.context_button_scroll_up_view);
        mContextMessageViewPageDown = (LinearLayout) mMessageView.findViewById(R.id.context_button_page_down_view);
        mContextMessageViewPageUp = (LinearLayout) mMessageView.findViewById(R.id.context_button_page_up_view);
        mContextMessageViewClear = (LinearLayout) mMessageView.findViewById(R.id.context_button_delete_view);
    }

    private void setContextButtonEnabled(final ImageButton btn, boolean enabled) {
        if (enabled) {
            btn.postDelayed(new Runnable() {
                @Override
                public void run() {
                    btn.setEnabled(true);
                }
            }, 1000);
        } else {
            btn.setEnabled(false);
        }
    }

    //process bottom action buttons press (Auto, set to Manual, delete task, move task...)
    private void setSyncTaskContextButtonListener() {
        final NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
                else setSyncTaskContextButtonNormalMode();
//				checkSafExternalSdcardTreeUri(null);
                ScheduleUtil.setSchedulerInfo(mActivity, mGp, mUtil);
                SyncTaskUtil.autosaveSyncTaskList(mGp, mActivity, mUtil, mCommonDlg, mGp.syncTaskList);
                if (mGp.syncTaskList.size()==0) mGp.syncTaskEmptyMessage.setVisibility(TextView.VISIBLE);
                else mGp.syncTaskEmptyMessage.setVisibility(TextView.GONE);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
//				checkSafExternalSdcardTreeUri(null);
            }
        });

        mContextSyncTaskButtonAutoTask.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) confirmActivate(mGp.syncTaskAdapter, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonAutoTask, mContext.getString(R.string.msgs_prof_cont_label_activate));

        mContextSyncTaskButtonManualTask.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) confirmInactivate(mGp.syncTaskAdapter, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonManualTask, mContext.getString(R.string.msgs_prof_cont_label_inactivate));

        mContextSyncTaskButtonAddSync.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonAddSync, false);
                    SyncTaskItem pfli = new SyncTaskItem();
//					pfli.setMasterDirectoryName("from");
//					pfli.setTargetDirectoryName("to");
                    pfli.setSyncTaskAuto(true);
                    pfli.setMasterLocalMountPoint(mGp.internalRootDirectory);
                    pfli.setTargetLocalMountPoint(mGp.internalRootDirectory);
                    pfli.setSyncOptionUseExtendedDirectoryFilter1(true);
                    SyncTaskEditor pmsp = SyncTaskEditor.newInstance();
                    pmsp.showDialog(getSupportFragmentManager(), pmsp, "ADD", pfli,
                            mTaskUtil, mUtil, mCommonDlg, mGp, ntfy);
                    setContextButtonEnabled(mContextSyncTaskButtonAddSync, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonAddSync, mContext.getString(R.string.msgs_prof_cont_label_add_sync));

        mContextSyncTaskButtonCopySyncTask.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonCopySyncTask, false);
                    for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
                        SyncTaskItem item = mGp.syncTaskAdapter.getItem(i);
                        if (item.isChecked()) {
                            mTaskUtil.copySyncTask(item, ntfy);
                            break;
                        }
                    }
                    setContextButtonEnabled(mContextSyncTaskButtonCopySyncTask, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonCopySyncTask, mContext.getString(R.string.msgs_prof_cont_label_copy));

        mContextSyncTaskButtonDeleteSyncTask.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonDeleteSyncTask, false);
                    mTaskUtil.deleteSyncTask(ntfy);
                    setContextButtonEnabled(mContextSyncTaskButtonDeleteSyncTask, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonDeleteSyncTask, mContext.getString(R.string.msgs_prof_cont_label_delete));

        mContextSyncTaskButtonRenameSyncTask.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonRenameSyncTask, false);
                    for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
                        SyncTaskItem item = mGp.syncTaskAdapter.getItem(i);
                        if (item.isChecked()) {
                            mTaskUtil.renameSyncTask(item, ntfy);
                            break;
                        }
                    }
                    setContextButtonEnabled(mContextSyncTaskButtonRenameSyncTask, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonRenameSyncTask, mContext.getString(R.string.msgs_prof_cont_label_rename));

        mContextSyncTaskButtonMoveToUp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
                        SyncTaskItem item = mGp.syncTaskAdapter.getItem(i);
                        if (item.isChecked()) {
                            int c_pos = item.getSyncTaskPosition();
                            if (c_pos > 0) {
                                for (int j = 0; j < mGp.syncTaskAdapter.getCount(); j++) {
                                    if (mGp.syncTaskAdapter.getItem(j).getSyncTaskPosition() == (c_pos - 1)) {
                                        mGp.syncTaskAdapter.getItem(j).setSyncTaskPosition(c_pos);
                                    }
                                }
                                item.setSyncTaskPosition(c_pos - 1);
                                mGp.syncTaskAdapter.sort();
                                SyncTaskUtil.saveSyncTaskListToFile(mGp, mContext, mUtil, false, "", "", mGp.syncTaskList, false);
                                mGp.syncTaskAdapter.notifyDataSetChanged();
                                //mUtil.addDebugMsg(2, "I", "c_pos="+c_pos + ", first v_pos="+mGp.syncTaskListView.getFirstVisiblePosition() + ", last v_pos="+mGp.syncTaskListView.getLastVisiblePosition() + ", tot="+mGp.syncTaskAdapter.getCount());
                                int set_pos = c_pos < 3 ? c_pos:c_pos-3;//show the 2 items before (moved item is set at c_pos-1)
                                //mGp.syncTaskListView.setSelection(c_pos-1);
                                mGp.syncTaskListView.smoothScrollToPosition(set_pos);

                                if (item.getSyncTaskPosition() == 0) {
                                    mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.INVISIBLE);
                                    mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.VISIBLE);
                                }
                                if (item.getSyncTaskPosition() == (mGp.syncTaskAdapter.getCount() - 1)) {
                                    mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.VISIBLE);
                                    mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.INVISIBLE);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonMoveToUp, mContext.getString(R.string.msgs_prof_cont_label_up));

        mContextSyncTaskButtonMoveToDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
                        SyncTaskItem item = mGp.syncTaskAdapter.getItem(i);
                        if (item.isChecked()) {
                            int c_pos = item.getSyncTaskPosition();
                            if (item.getSyncTaskPosition() < (mGp.syncTaskAdapter.getCount() - 1)) {
                                for (int j = 0; j < mGp.syncTaskAdapter.getCount(); j++) {
                                    if (mGp.syncTaskAdapter.getItem(j).getSyncTaskPosition() == (c_pos + 1)) {
                                        mGp.syncTaskAdapter.getItem(j).setSyncTaskPosition(c_pos);
                                    }
                                }
                                item.setSyncTaskPosition(c_pos + 1);
                                mGp.syncTaskAdapter.sort();
                                SyncTaskUtil.saveSyncTaskListToFile(mGp, mContext, mUtil, false, "", "", mGp.syncTaskList, false);
                                mGp.syncTaskAdapter.notifyDataSetChanged();
                                int last_pos = mGp.syncTaskAdapter.getCount() - 1;
                                int set_pos = c_pos > (last_pos - 3) ? c_pos:c_pos+3;//show next 2 items (moved item is set at c_pos+1)
                                mGp.syncTaskListView.smoothScrollToPosition(set_pos);

                                if (item.getSyncTaskPosition() == 0) {
                                    mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.INVISIBLE);
                                    mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.VISIBLE);
                                }
                                if (item.getSyncTaskPosition() == (mGp.syncTaskAdapter.getCount() - 1)) {
                                    mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.VISIBLE);
                                    mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.INVISIBLE);
                                }

                            }
                            break;
                        }
                    }
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonMoveToDown, mContext.getString(R.string.msgs_prof_cont_label_down));

        mContextSyncTaskButtonSelectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonSelectAll, false);
                    for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
                        mGp.syncTaskAdapter.getItem(i).setChecked(true);
                    }
                    mGp.syncTaskAdapter.notifyDataSetChanged();
                    mGp.syncTaskAdapter.setShowCheckBox(true);
                    setSyncTaskContextButtonSelectMode();
                    setContextButtonEnabled(mContextSyncTaskButtonSelectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonSelectAll, mContext.getString(R.string.msgs_prof_cont_label_select_all));

        mContextSyncTaskButtonUnselectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonUnselectAll, false);
                    SyncTaskUtil.setAllSyncTaskToUnchecked(false, mGp.syncTaskAdapter);
                    //				for (int i=0;i<syncTaskListAdapter.getCount();i++) {
                    //					ProfileUtility.setProfileToChecked(false, syncTaskListAdapter, i);
                    //				}
                    mGp.syncTaskAdapter.notifyDataSetChanged();
                    setSyncTaskContextButtonSelectMode();
                    setContextButtonEnabled(mContextSyncTaskButtonUnselectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonUnselectAll, mContext.getString(R.string.msgs_prof_cont_label_unselect_all));

    }

    private void confirmActivate(AdapterSyncTask pa, final NotifyEvent p_ntfy) {
        boolean is_task_error_tmp = false;
        String msg = "";
        String sep = "";
        for (int i = 0; i < pa.getCount(); i++) {
            if (pa.getItem(i).isChecked() && pa.getItem(i).isSyncTaskError()) {
                is_task_error_tmp = true;
                msg = "- "+pa.getItem(i).getSyncTaskName();
                break;
            } else if (pa.getItem(i).isChecked() && !pa.getItem(i).isSyncTaskAuto()) {
                msg += sep + "- "+pa.getItem(i).getSyncTaskName();
                sep = "\n";
            }
        }

        final boolean is_task_error = is_task_error_tmp;
        final String e_msg = msg;
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (is_task_error) {
                    mUtil.addLogMsg("E", mContext.getString(R.string.msgs_prof_cont_to_activate_inactivate_profile_error));
                    mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_prof_cont_to_activate_inactivate_profile_error, e_msg), "", null);
                } else {
                    mTaskUtil.setSyncTaskToAuto(mGp);
                    SyncTaskUtil.setAllSyncTaskToUnchecked(true, mGp.syncTaskAdapter);
                    mGp.syncScheduleAdapter.notifyDataSetChanged();
                    refreshOptionMenu();
                    p_ntfy.notifyToListener(true, null);
                }
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mUtil.showCommonDialog(true, "W",
                mContext.getString(R.string.msgs_prof_cont_to_activate_profile),
                msg, ntfy);
    }

    private void confirmInactivate(AdapterSyncTask pa, final NotifyEvent p_ntfy) {
        boolean is_task_error_tmp = false;
        String msg = "";
        String sep = "";
        for (int i = 0; i < pa.getCount(); i++) {
            if (pa.getItem(i).isChecked() && pa.getItem(i).isSyncTaskError()) {
                is_task_error_tmp = true;
                msg = "- "+pa.getItem(i).getSyncTaskName();
                break;
            } else if (pa.getItem(i).isChecked() && pa.getItem(i).isSyncTaskAuto()) {
                msg += sep + "- "+pa.getItem(i).getSyncTaskName();
                sep = "\n";
            }
        }

        final boolean is_task_error = is_task_error_tmp;
        final String e_msg = msg;
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (is_task_error) {
                    mUtil.addLogMsg("E", mContext.getString(R.string.msgs_prof_cont_to_activate_inactivate_profile_error));
                    mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_prof_cont_to_activate_inactivate_profile_error, e_msg), "", null);
                } else {
                    mTaskUtil.setSyncTaskToManual();
                    SyncTaskUtil.setAllSyncTaskToUnchecked(true, mGp.syncTaskAdapter);
                    mGp.syncScheduleAdapter.notifyDataSetChanged();
                    refreshOptionMenu();
                    p_ntfy.notifyToListener(true, null);
                }
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mUtil.showCommonDialog(true, "W",
                mContext.getString(R.string.msgs_prof_cont_to_inactivate_profile),
                msg, ntfy);
    }

    //set bottom action buttons visibility (Auto, Manual, delete, move... buttons when a task is selected in main view)
    private void setSyncTaskContextButtonSelectMode() {
        int sel_cnt = SyncTaskUtil.getSyncTaskSelectedItemCount(mGp.syncTaskAdapter);
        int tot_cnt = mGp.syncTaskAdapter.getCount();
        setActionBarSelectMode(sel_cnt, tot_cnt);

        boolean any_selected = SyncTaskUtil.isSyncTaskSelected(mGp.syncTaskAdapter);

        boolean act_prof_selected = false, inact_prof_selected = false;
        if (any_selected) {
            for (int i = 0; i < tot_cnt; i++) {
                if (mGp.syncTaskAdapter.getItem(i).isChecked()) {
                    if (mGp.syncTaskAdapter.getItem(i).isSyncTestMode()) {
                        //A Test Sync Task is selected: hide set to Auto/Manual bottom buttons
                        act_prof_selected = false;
                        inact_prof_selected = false;
                        break;
                    }
                    if (mGp.syncTaskAdapter.getItem(i).isSyncTaskAuto()) act_prof_selected = true;
                    else inact_prof_selected = true;
                }
            }
        }

        if (inact_prof_selected) {
            if (any_selected) mContextSyncTaskViewAutoTask.setVisibility(ImageButton.VISIBLE);
            else mContextSyncTaskViewAutoTask.setVisibility(ImageButton.INVISIBLE);
        } else mContextSyncTaskViewAutoTask.setVisibility(ImageButton.INVISIBLE);

        if (act_prof_selected) {
            if (any_selected) mContextSyncTaskViewManualTask.setVisibility(ImageButton.VISIBLE);
            else mContextSyncTaskViewManualTask.setVisibility(ImageButton.INVISIBLE);
        } else mContextSyncTaskViewManualTask.setVisibility(ImageButton.INVISIBLE);

        mContextSyncTaskViewAddSync.setVisibility(ImageButton.INVISIBLE);

        if (sel_cnt == 1) mContextSyncTaskViewCopySyncTask.setVisibility(ImageButton.VISIBLE);
        else mContextSyncTaskViewCopySyncTask.setVisibility(ImageButton.INVISIBLE);

        if (any_selected) mContextSyncTaskViewDeleteSyncTask.setVisibility(ImageButton.VISIBLE);
        else mContextSyncTaskViewDeleteSyncTask.setVisibility(ImageButton.INVISIBLE);

        if (sel_cnt == 1) mContextSyncTaskViewRenameSyncTask.setVisibility(ImageButton.VISIBLE);
        else mContextSyncTaskViewRenameSyncTask.setVisibility(ImageButton.INVISIBLE);

        if (sel_cnt == 1) {
            for (int i = 0; i < tot_cnt; i++) {
                if (mGp.syncTaskAdapter.getItem(i).isChecked()) {
                    if (i == 0) mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.INVISIBLE);
                    else mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.VISIBLE);
                    if (i == (tot_cnt - 1))
                        mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.INVISIBLE);
                    else mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.VISIBLE);
                    break;
                }
            }
        } else {
            mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.INVISIBLE);
            mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.INVISIBLE);
        }

        if (tot_cnt != sel_cnt) mContextSyncTaskViewSelectAll.setVisibility(ImageButton.VISIBLE);
        else mContextSyncTaskViewSelectAll.setVisibility(ImageButton.INVISIBLE);

        if (any_selected) mContextSyncTaskViewUnselectAll.setVisibility(ImageButton.VISIBLE);
        else mContextSyncTaskViewUnselectAll.setVisibility(ImageButton.INVISIBLE);

        refreshOptionMenu();
    }

    private void setSyncTaskContextButtonHide() {
        mActionBar.setIcon(R.drawable.smbsync);
        mActionBar.setHomeButtonEnabled(false);
        mActionBar.setTitle(R.string.app_name);

        mGp.syncTaskAdapter.setAllItemChecked(false);
        mGp.syncTaskAdapter.setShowCheckBox(false);
        mGp.syncTaskAdapter.notifyDataSetChanged();

        mContextSyncTaskViewAutoTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewManualTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewAddSync.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewCopySyncTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewDeleteSyncTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewRenameSyncTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewSelectAll.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewUnselectAll.setVisibility(ImageButton.INVISIBLE);

    }

    private void setActionBarSelectMode(int sel_cnt, int tot_cnt) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        String sel_txt = "" + sel_cnt + "/" + tot_cnt;
        actionBar.setTitle(sel_txt);
    }

    private void setActionBarNormalMode() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.app_name);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
    }

    private void setSyncTaskContextButtonNormalMode() {
        setActionBarNormalMode();

        mGp.syncTaskAdapter.setAllItemChecked(false);
        mGp.syncTaskAdapter.setShowCheckBox(false);
        mGp.syncTaskAdapter.notifyDataSetChanged();

        mContextSyncTaskViewAutoTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewManualTask.setVisibility(ImageButton.INVISIBLE);
        if (isUiEnabled()) mContextSyncTaskViewAddSync.setVisibility(ImageButton.VISIBLE);
        else mContextSyncTaskViewAddSync.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewCopySyncTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewDeleteSyncTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewRenameSyncTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.INVISIBLE);
        if (isUiEnabled()) {
            if (!mGp.syncTaskAdapter.isEmptyAdapter())
                mContextSyncTaskViewSelectAll.setVisibility(ImageButton.VISIBLE);
            else mContextSyncTaskViewSelectAll.setVisibility(ImageButton.INVISIBLE);
        } else {
            mContextSyncTaskViewSelectAll.setVisibility(ImageButton.INVISIBLE);
        }
        mContextSyncTaskViewUnselectAll.setVisibility(ImageButton.INVISIBLE);

        refreshOptionMenu();
    }

    private void setMessageScrollButtonVisibility() {
        Handler hndl=new Handler();
        hndl.post(new Runnable(){
            @Override
            public void run() {
                if (canListViewScrollDown(mGp.syncMessageListView)) {
                    mContextMessageButtonScrollDown.setVisibility(LinearLayout.VISIBLE);
                    mContextMessageButtonPageDown.setVisibility(LinearLayout.VISIBLE);
                    mContextMessageButtonMoveBottom.setVisibility(LinearLayout.VISIBLE);
                } else {
                    mContextMessageButtonScrollDown.setVisibility(LinearLayout.INVISIBLE);
                    mContextMessageButtonPageDown.setVisibility(LinearLayout.INVISIBLE);
                    mContextMessageButtonMoveBottom.setVisibility(LinearLayout.INVISIBLE);
                }
                if (canListViewScrollUp(mGp.syncMessageListView)) {
                    mContextMessageButtonScrollUp.setVisibility(LinearLayout.VISIBLE);
                    mContextMessageButtonPageUp.setVisibility(LinearLayout.VISIBLE);
                    mContextMessageButtonMoveTop.setVisibility(LinearLayout.VISIBLE);
                } else {
                    mContextMessageButtonScrollUp.setVisibility(LinearLayout.INVISIBLE);
                    mContextMessageButtonPageUp.setVisibility(LinearLayout.INVISIBLE);
                    mContextMessageButtonMoveTop.setVisibility(LinearLayout.INVISIBLE);
                }
            }
        });
    }

    private final static int MESSAGE_SCROLL_AMOUNT=1;
    private void setMessageContextButtonListener() {
        setMessageScrollButtonVisibility();
        mGp.syncMessageListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                setMessageScrollButtonVisibility();
            }
        });

        mContextMessageButtonScrollUp.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, 50, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int sel = mGp.syncMessageListView.getFirstVisiblePosition() - MESSAGE_SCROLL_AMOUNT;

                if (sel > mGp.syncMessageListAdapter.getCount() - 1) sel = mGp.syncMessageListAdapter.getCount() - 1;
                if (sel < 0) sel = 0;
                mGp.syncMessageListView.setSelection(sel);
                setMessageScrollButtonVisibility();
            }
        }));

        mContextMessageButtonScrollDown.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, 50, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int sel = mGp.syncMessageListView.getFirstVisiblePosition() + MESSAGE_SCROLL_AMOUNT;

                if (sel > mGp.syncMessageListAdapter.getCount() - 1) sel = mGp.syncMessageListAdapter.getCount() - 1;
                if (sel < 0) sel = 0;

                mGp.syncMessageListView.setSelection(sel);
                setMessageScrollButtonVisibility();
            }
        }));

        mContextMessageButtonPageUp.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, DEFAULT_LONG_PRESS_REPEAT_INTERVAL, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int lv_height = mGp.syncMessageListView.getHeight();
                int first_item_y_top =  mGp.syncMessageListView.getChildAt(0).getTop();
                int first_item_y_bottom =  mGp.syncMessageListView.getChildAt(0).getBottom();
                int first_item_height = first_item_y_bottom - first_item_y_top;
                int y_offset = 0;
                if (first_item_y_top < 0) {
                    // part of first item is hidden on top
                    y_offset = first_item_height;
                    if (y_offset > lv_height) {
                        //item is more than one page: position to the bottom, the current top exact last visible position, minus 3 text lines
                        TextView listTextView = (TextView) mGp.syncMessageListView.getChildAt(0).findViewById(R.id.msg_list_view_item_date);
                        int text_context_size = 0;
                        if (listTextView != null) text_context_size = (int)(listTextView.getTextSize() * 3);
                        y_offset = first_item_height - first_item_y_bottom + text_context_size;
                    }
                }

                //mUtil.addDebugMsg(2, "I", "lv_height="+lv_height + " first_item_height="+first_item_height + " first_item_y_top="+first_item_y_top + " first_item_y_bottom="+first_item_y_bottom);
                mGp.syncMessageListView.setSelectionFromTop(mGp.syncMessageListView.getFirstVisiblePosition(), lv_height - y_offset);
                setMessageScrollButtonVisibility();
            }
        }));

        mContextMessageButtonPageDown.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, DEFAULT_LONG_PRESS_REPEAT_INTERVAL, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int last_item_pos = mGp.syncMessageListView.getLastVisiblePosition() - mGp.syncMessageListView.getFirstVisiblePosition();
                int lv_height = mGp.syncMessageListView.getHeight();
                int last_item_y_top =  mGp.syncMessageListView.getChildAt(last_item_pos).getTop();
                int last_item_y_bottom =  mGp.syncMessageListView.getChildAt(last_item_pos).getBottom();
                int last_item_height = last_item_y_bottom - last_item_y_top;
                int y_offset = 0;

                if (last_item_height > lv_height) {
                    //item is more than one page: position to the top, the current bottom exat last visible position, minus 3 text lines
                    TextView listTextView = (TextView) mGp.syncMessageListView.getChildAt(last_item_pos).findViewById(R.id.msg_list_view_item_date);
                    int text_context_size = 0;
                    if (listTextView != null) text_context_size = (int)(listTextView.getTextSize() * 3);
                    y_offset = -(lv_height - last_item_y_top - text_context_size);
                }

                //mUtil.addDebugMsg(2, "I", "y_offset="+y_offset + " last_item_height="+last_item_height + " last_item_y_top="+last_item_y_top);
                mGp.syncMessageListView.setSelectionFromTop(mGp.syncMessageListView.getLastVisiblePosition(), y_offset);
                setMessageScrollButtonVisibility();
            }
        }));

        mContextMessageButtonPinned.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextMessageButtonPinned, false);
                mGp.freezeMessageViewScroll = !mGp.freezeMessageViewScroll;
                if (mGp.freezeMessageViewScroll) {
                    mContextMessageButtonPinned.setImageResource(R.drawable.context_button_pinned_active);
                    CommonUtilities.showToastMessageShort(mActivity, mContext.getString(R.string.msgs_log_activate_pinned));
                    ContextButtonUtil.setButtonLabelListener(mActivity, mContextMessageButtonPinned,
                            mContext.getString(R.string.msgs_msg_cont_label_pinned_active));
                } else {
                    mContextMessageButtonPinned.setImageResource(R.drawable.context_button_pinned_inactive);
                    mGp.syncMessageListView.setSelection(mGp.syncMessageListAdapter.getCount() - 1);
                    CommonUtilities.showToastMessageShort(mActivity, mContext.getString(R.string.msgs_log_inactivate_pinned));
                    ContextButtonUtil.setButtonLabelListener(mActivity, mContextMessageButtonPinned,
                            mContext.getString(R.string.msgs_msg_cont_label_pinned_inactive));
                }
                setContextButtonEnabled(mContextMessageButtonPinned, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextMessageButtonPinned, mContext.getString(R.string.msgs_msg_cont_label_pinned_inactive));

        mContextMessageButtonMoveTop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextMessageButtonMoveTop, false);
                mGp.syncMessageListView.setSelection(0);
                setContextButtonEnabled(mContextMessageButtonMoveTop, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextMessageButtonMoveTop, mContext.getString(R.string.msgs_msg_cont_label_move_top));

        mContextMessageButtonMoveBottom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextMessageButtonMoveBottom, false);
                mGp.syncMessageListView.setSelection(mGp.syncMessageListAdapter.getCount() - 1);
                setContextButtonEnabled(mContextMessageButtonMoveBottom, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextMessageButtonMoveBottom, mContext.getString(R.string.msgs_msg_cont_label_move_bottom));

        mContextMessageButtonClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        mGp.syncMessageListView.setSelection(0);
                        if (mGp.syncMessageListAdapter !=null) mGp.syncMessageListAdapter.clear();
                        mUtil.addLogMsg("W", getString(R.string.msgs_log_msg_cleared));
                        CommonUtilities.saveMsgList(mGp);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                mUtil.showCommonDialog(true, "W",
                        mContext.getString(R.string.msgs_log_confirm_clear_all_msg), "", ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextMessageButtonClear, mContext.getString(R.string.msgs_msg_cont_label_clear));
    }

    private void setMessageContextButtonNormalMode() {
        mContextMessageViewPinned.setVisibility(LinearLayout.VISIBLE);
        if (mGp.freezeMessageViewScroll) {
            mContextMessageButtonPinned.setImageResource(R.drawable.context_button_pinned_active);
        } else {
            mContextMessageButtonPinned.setImageResource(R.drawable.context_button_pinned_inactive);
        }
        mContextMessageViewMoveTop.setVisibility(LinearLayout.VISIBLE);
        mContextMessageViewMoveBottom.setVisibility(LinearLayout.VISIBLE);
        mContextMessageViewScrollDown.setVisibility(LinearLayout.VISIBLE);
        mContextMessageViewScrollUp.setVisibility(LinearLayout.VISIBLE);
        mContextMessageViewClear.setVisibility(LinearLayout.VISIBLE);
    }

    private void editSyncTask(String prof_name, boolean prof_act, int prof_num) {
        NotifyEvent ntfy_check = new NotifyEvent(mContext);
        ntfy_check.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                SyncTaskItem item = mGp.syncTaskAdapter.getItem(prof_num);
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                SyncTaskEditor pmp = SyncTaskEditor.newInstance();
                pmp.showDialog(getSupportFragmentManager(), pmp, "EDIT", item,
                        mTaskUtil, mUtil, mCommonDlg, mGp, ntfy);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        ApplicationPasswordUtil.applicationPasswordAuthentication(mGp, mActivity, getSupportFragmentManager(),
                mUtil, false, ntfy_check, ApplicationPasswordUtil.APPLICATION_PASSWORD_RESOURCE_EDIT_SYNC_TASK);
    }

    private void syncSpecificSyncTask(SyncTaskItem sti) {
        final ArrayList<SyncTaskItem> t_list = new ArrayList<SyncTaskItem>();
        t_list.add(sti);
        mUtil.addLogMsg("I", mContext.getString(R.string.msgs_main_sync_selected_profiles));
        mUtil.addLogMsg("I", mContext.getString(R.string.msgs_main_sync_prof_name_list) + " " + sti.getSyncTaskName());
        CommonUtilities.showToastMessageShort(mActivity, mContext.getString(R.string.msgs_main_sync_selected_profiles));
        startSyncTask(t_list);
    }

    private void syncSelectedSyncTask() {
        final ArrayList<SyncTaskItem> t_list = new ArrayList<SyncTaskItem>();
        SyncTaskItem item;
        String sync_list_tmp = "";
        String sep = "";
        boolean test_sync_task_found = false;
        boolean error_sync_task_found_tmp = false;
        for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
            item = mGp.syncTaskAdapter.getItem(i);
            if (item.isChecked() && !item.isSyncTaskError()) {
                t_list.add(item);
                sync_list_tmp += sep + item.getSyncTaskName();
                sep = SYNC_TASK_LIST_SEPARATOR;
                if (item.isSyncTestMode()) test_sync_task_found = true;
            } else if (item.isChecked() && item.isSyncTaskError()) {
                error_sync_task_found_tmp = true;
                sync_list_tmp = item.getSyncTaskName();
                break;//fail start sync
            }
        }
        final String sync_list = sync_list_tmp;
        final boolean error_sync_task_found = error_sync_task_found_tmp;

        NotifyEvent ntfy_test_mode = new NotifyEvent(mContext);
        ntfy_test_mode.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (error_sync_task_found) {
                    mUtil.addLogMsg("E", mContext.getString(R.string.msgs_main_sync_selected_task_has_error));
                    mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_main_sync_selected_task_has_error, sync_list), "", null);
                } else if (t_list.isEmpty()) {
                    mUtil.addLogMsg("E", mContext.getString(R.string.msgs_main_sync_select_prof_no_active_profile));
                    mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_main_sync_select_prof_no_active_profile), "", null);
                } else {
                    mUtil.addLogMsg("I", mContext.getString(R.string.msgs_main_sync_selected_profiles_checked_boxes));
                    mUtil.addLogMsg("I", mContext.getString(R.string.msgs_main_sync_prof_name_list) + " " + sync_list);
                    CommonUtilities.showToastMessageShort(mActivity, mContext.getString(R.string.msgs_main_sync_selected_profiles_checked_boxes));
                    startSyncTask(t_list);
                }
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }

        });
        if (test_sync_task_found) {
            mUtil.showCommonDialog(true, "W",
                    mContext.getString(R.string.msgs_main_sync_test_mode_warnning), "", ntfy_test_mode);
        } else {
            ntfy_test_mode.notifyToListener(true, null);
        }
    }

    private void syncAutoSyncTask() {
        final ArrayList<SyncTaskItem> t_list = new ArrayList<SyncTaskItem>();
        String sync_list_tmp = "", sep = "";
        for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
            SyncTaskItem item = mGp.syncTaskAdapter.getItem(i);
            if (item.isSyncTaskAuto() && !item.isSyncTestMode() && !item.isSyncTaskError()) {
                t_list.add(item);
                sync_list_tmp += sep + item.getSyncTaskName();
                sep = SYNC_TASK_LIST_SEPARATOR;
            }
        }

        if (t_list.isEmpty()) {
            mUtil.addLogMsg("E", mContext.getString(R.string.msgs_active_sync_prof_not_found));
            mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_active_sync_prof_not_found), "", null);
        } else {
            final String sync_list = sync_list_tmp;
            mUtil.addLogMsg("I", mContext.getString(R.string.msgs_main_sync_all_active_profiles));
            mUtil.addLogMsg("I", mContext.getString(R.string.msgs_main_sync_prof_name_list) + sync_list);
//			tabHost.setCurrentTabByTag(TAB_TAG_MSG);
            CommonUtilities.showToastMessageShort(mActivity, mContext.getString(R.string.msgs_main_sync_all_active_profiles));
            startSyncTask(t_list);
        }

    }

    private void setUiEnabled() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");
        enableMainUi = true;

        if (!mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonNormalMode();
        else setSyncTaskContextButtonSelectMode();

        if (!mGp.syncHistoryAdapter.isShowCheckBox()) setHistoryContextButtonNormalMode();
        else setHistoryContextButtonSelectMode();

        mContextScheduleView.setVisibility(LinearLayout.VISIBLE);

        refreshOptionMenu();
    }

    private void setUiDisabled() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");
        enableMainUi = false;

        if (!mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonNormalMode();
        else setSyncTaskContextButtonSelectMode();

        if (!mGp.syncHistoryAdapter.isShowCheckBox()) setHistoryContextButtonNormalMode();
        else setHistoryContextButtonSelectMode();

        mContextScheduleView.setVisibility(LinearLayout.INVISIBLE);

        refreshOptionMenu();
    }

    private boolean isUiEnabled() {
        return enableMainUi;
    }

    final public void refreshOptionMenu() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");
//		if (Build.VERSION.SDK_INT>=11)
//			this.invalidateOptionsMenu();
        supportInvalidateOptionsMenu();
    }

    private void startSyncTask(ArrayList<SyncTaskItem> alp) {
        String[] task_name = new String[alp.size()];
        for (int i = 0; i < alp.size(); i++) task_name[i] = alp.get(i).getSyncTaskName();
        try {
            mSvcClient.aidlStartSpecificSyncTask(task_name);
//			mMainTabHost.setCurrentTab(2);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void syncThreadStarted() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        setUiDisabled();
        mGp.progressSpinView.setVisibility(LinearLayout.VISIBLE);
//        mGp.progressSpinView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
        mGp.progressSpinView.bringToFront();
        mGp.progressSpinSyncprof.setVisibility(TextView.VISIBLE);
        mGp.progressSpinCancel.setText(getString(R.string.msgs_progress_spin_dlg_sync_cancel));
        mGp.progressSpinCancel.setEnabled(true);
        // CANCELボタンの指定
        mGp.progressSpinCancelListener = new OnClickListener() {
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        try {
                            mSvcClient.aidlCancelSyncTask();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
//                        mGp.progressSpinCancel.setText(getString(R.string.msgs_progress_dlg_canceling));
                        mGp.progressSpinCancel.setEnabled(false);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                mUtil.showCommonDialog(true, "W", getString(R.string.msgs_main_sync_cancel_confirm), "", ntfy);
            }
        };
        mGp.progressSpinCancel.setOnClickListener(mGp.progressSpinCancelListener);

        ScheduleUtil.setSchedulerInfo(mActivity, mGp, mUtil);

        LogUtil.flushLog(mContext, mGp);
    }

    private void syncThreadEnded() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        LogUtil.flushLog(mContext, mGp);

        mGp.progressBarCancelListener = null;
        mGp.progressBarImmedListener = null;
        mGp.progressSpinCancelListener = null;
        mGp.progressBarCancel.setOnClickListener(null);
        mGp.progressSpinCancel.setOnClickListener(null);
        mGp.progressBarImmed.setOnClickListener(null);

        mGp.progressSpinView.setVisibility(LinearLayout.GONE);

        mGp.syncHistoryAdapter.notifyDataSetChanged();

        setUiEnabled();
    }

    private ISvcCallback mSvcCallbackStub = new ISvcCallback.Stub() {
        @Override
        public void cbThreadStarted() throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    syncThreadStarted();
                }
            });
        }

        @Override
        public void cbThreadEnded() throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    syncThreadEnded();
                }
            });
        }

        @Override
        public void cbShowConfirmDialog(final String method, final String msg,
                                        final String pair_a_path, final long pair_a_length, final long pair_a_last_mod,
                                        final String pair_b_path, final long pair_b_length, final long pair_b_last_mod) throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    showConfirmDialog(method, msg, pair_a_path, pair_a_length, pair_a_last_mod, pair_b_path, pair_b_length, pair_b_last_mod);
                }
            });
        }

        @Override
        public void cbHideConfirmDialog() throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    hideConfirmDialog();
                }
            });
        }

        @Override
        public void cbWifiStatusChanged(String status, String ssid) throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    refreshOptionMenu();
                    if (mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
                    else setSyncTaskContextButtonNormalMode();
                }
            });
        }

        @Override
        public void cbMediaStatusChanged() throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    refreshOptionMenu();
//					mGp.syncTaskAdapter.notifyDataSetChanged();
                    mGp.syncTaskAdapter.notifyDataSetChanged();
                }
            });
        }

    };

    private ISvcClient mSvcClient = null;

    private void openService(final NotifyEvent p_ntfy) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        mGp.activityIsBackground=false;
        mSvcConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName arg0, IBinder service) {
                mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
                mSvcClient = ISvcClient.Stub.asInterface(service);
                p_ntfy.notifyToListener(true, null);
            }

            public void onServiceDisconnected(ComponentName name) {
                mSvcConnection = null;
                mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            }
        };

        Intent intmsg = new Intent(mContext, SyncService.class);
        bindService(intmsg, mSvcConnection, BIND_AUTO_CREATE);
    }

    private void closeService() {

        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, conn=" + mSvcConnection);

        if (mSvcConnection != null) {
//    		try {
//				if (mSvcClient!=null) mSvcClient.aidlStopService();
//			} catch (RemoteException e) {
//				e.printStackTrace();
//			}
            mSvcClient = null;
            try {
                unbindService(mSvcConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mSvcConnection = null;
//	    	Log.v("","close service");
        }
//        Intent intent = new Intent(this, SyncService.class);
//        stopService(intent);
    }

    final private void setCallbackListener() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        try {
            mSvcClient.setCallBack(mSvcCallbackStub);
        } catch (RemoteException e) {
            e.printStackTrace();
            mUtil.addDebugMsg(1, "E", "setCallbackListener error :" + e.toString());
        }
    }

    final private void unsetCallbackListener() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        if (mSvcClient != null) {
            try {
                mSvcClient.removeCallBack(mSvcCallbackStub);
            } catch (RemoteException e) {
                e.printStackTrace();
                mUtil.addDebugMsg(1, "E", "unsetCallbackListener error :" + e.toString());
            }
        }
    }

    private void reshowDialogWindow() {
        if (mGp.dialogWindowShowed) {
            syncThreadStarted();
            mGp.progressSpinSyncprof.setText(mGp.progressSpinSyncprofText);
            mGp.progressSpinMsg.setText(mGp.progressSpinMsgText);
            if (mGp.confirmDialogShowed)
                showConfirmDialog(mGp.confirmDialogMethod, mGp.confirmDialogMessage,
                        mGp.confirmDialogFilePathPairA, mGp.confirmDialogFileLengthPairA, mGp.confirmDialogFileLastModPairA,
                        mGp.confirmDialogFilePathPairB, mGp.confirmDialogFileLengthPairB, mGp.confirmDialogFileLastModPairB);
        }
    }

    private void hideConfirmDialog() {
        mGp.confirmView.setVisibility(LinearLayout.GONE);
    }

    private void showConfirmDialog(final String method, final String msg,
                                   final String pair_a_path, final long pair_a_length, final long pair_a_last_mod,
                                   final String pair_b_path, final long pair_b_length, final long pair_b_last_mod) {
//        if (method.equals(SMBSYNC2_CONFIRM_REQUEST_CONFLICT_FILE)) {
//            TwoWaySyncFile.showConfirmDialogConflict(mGp, mUtil, mSvcClient,
//                    method, msg, pair_a_path, pair_a_length, pair_a_last_mod, pair_b_path, pair_b_length, pair_b_last_mod);
//        } else {
//            showConfirmDialogOverride(method, msg, pair_a_path);
//        }
        showConfirmDialogOverride(method, msg, pair_a_path);

    }

    private void showConfirmDialogOverride(String method, String msg, String fp) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        final int prog_view=mGp.progressSpinView.getVisibility();
        mGp.progressSpinView.setVisibility(ProgressBar.GONE);

        mGp.confirmOverrideView.setVisibility(LinearLayout.VISIBLE);
        mGp.confirmConflictView.setVisibility(LinearLayout.GONE);
        mGp.confirmDialogShowed = true;
        mGp.confirmDialogFilePathPairA = fp;
        mGp.confirmDialogMethod = method;
        mGp.confirmDialogMessage = msg;

        mGp.confirmView.setVisibility(LinearLayout.VISIBLE);
        mGp.confirmView.setBackgroundColor(mGp.themeColorList.text_background_color);
        mGp.confirmView.bringToFront();
        String msg_text = "";
        if (method.equals(SMBSYNC2_CONFIRM_REQUEST_COPY)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_copy_confirm), fp);
        } else if (method.equals(SMBSYNC2_CONFIRM_REQUEST_DELETE_FILE)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_delete_file_confirm), fp);
        } else if (method.equals(SMBSYNC2_CONFIRM_REQUEST_DELETE_DIR)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_delete_dir_confirm), fp);
        } else if (method.equals(SMBSYNC2_CONFIRM_REQUEST_MOVE)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_move_confirm), fp);
        } else if (method.equals(SMBSYNC2_CONFIRM_REQUEST_ARCHIVE_DATE_FROM_FILE)) {
            long fd=(new File(fp)).lastModified();
            String date_time= StringUtil.convDateTimeTo_YearMonthDayHourMinSec(fd);
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_archive_date_time_from_file_confirm), date_time, fp);
        }
        mGp.confirmMsg.setText(msg_text);

        // Yesボタンの指定
        mGp.confirmYesListener = new OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                mGp.progressSpinView.setVisibility(prog_view);
                sendConfirmResponse(mGp, mSvcClient, SMBSYNC2_CONFIRM_RESP_YES);
            }
        };
        mGp.confirmYes.setOnClickListener(mGp.confirmYesListener);
        // YesAllボタンの指定
        mGp.confirmYesAllListener = new OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                mGp.progressSpinView.setVisibility(prog_view);
                sendConfirmResponse(mGp, mSvcClient, SMBSYNC2_CONFIRM_RESP_YESALL);
            }
        };
        mGp.confirmYesAll.setOnClickListener(mGp.confirmYesAllListener);
        // Noボタンの指定
        mGp.confirmNoListener = new OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                mGp.progressSpinView.setVisibility(prog_view);
                sendConfirmResponse(mGp, mSvcClient, SMBSYNC2_CONFIRM_RESP_NO);
            }
        };
        mGp.confirmNo.setOnClickListener(mGp.confirmNoListener);
        // NoAllボタンの指定
        mGp.confirmNoAllListener = new OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                mGp.progressSpinView.setVisibility(prog_view);
                sendConfirmResponse(mGp, mSvcClient, SMBSYNC2_CONFIRM_RESP_NOALL);
            }
        };
        mGp.confirmNoAll.setOnClickListener(mGp.confirmNoAllListener);
        // Task cancelボタンの指定
        mGp.confirmCancelListener = new OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                mGp.progressSpinView.setVisibility(prog_view);
                sendConfirmResponse(mGp, mSvcClient, SMBSYNC2_CONFIRM_RESP_CANCEL);
            }
        };
        mGp.confirmCancel.setOnClickListener(mGp.confirmCancelListener);
    }

    static public void sendConfirmResponse(GlobalParameters gp, ISvcClient sc, int response) {
        gp.confirmDialogShowed = false;
        try {
            sc.aidlConfirmReply(response);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        gp.confirmYesListener = null;
        gp.confirmYesAllListener = null;
        gp.confirmNoListener = null;
        gp.confirmNoAllListener = null;
        gp.confirmCancelListener = null;
        gp.confirmCancel.setOnClickListener(null);
        gp.confirmYes.setOnClickListener(null);
        gp.confirmYesAll.setOnClickListener(null);
        gp.confirmNo.setOnClickListener(null);
        gp.confirmNoAll.setOnClickListener(null);

        gp.confirmDialogConflictButtonSelectAListener = null;
        gp.confirmDialogConflictButtonSelectBListener = null;
        gp.confirmDialogConflictButtonSyncIgnoreFileListener = null;
        gp.confirmDialogConflictButtonCancelSyncTaskListener = null;

    }

    final private boolean checkJcifsOptionChanged() {
        boolean changed = false;

        String  prevSmbLmCompatibility = mGp.settingsSmbLmCompatibility,
                prevSmbUseExtendedSecurity = mGp.settingsSmbUseExtendedSecurity;
        String p_response_timeout=mGp.settingsSmbClientResponseTimeout;
        String p_disable_plain_text_passwords=mGp.settingsSmbDisablePlainTextPasswords;

        mGp.initJcifsOption(mContext);

        if (!mGp.settingsSmbLmCompatibility.equals(prevSmbLmCompatibility)) changed = true;
        else if (!mGp.settingsSmbUseExtendedSecurity.equals(prevSmbUseExtendedSecurity)) changed = true;
        else if (!mGp.settingsSmbClientResponseTimeout.equals(p_response_timeout)) changed = true;
        else if (!mGp.settingsSmbDisablePlainTextPasswords.equals(p_disable_plain_text_passwords)) changed = true;

        if (changed) {
            listSettingsOption();
            NotifyEvent ntfy=new NotifyEvent(mContext);
            ntfy.setListener(new NotifyEventListener() {
                @Override
                public void positiveResponse(Context context, Object[] objects) {
                    mUtil.flushLog();
//                    Intent in_act = new Intent(context, ActivityMain.class);
//                    int pi_id = R.string.app_name;
//                    PendingIntent pi = PendingIntent.getActivity(context, pi_id, in_act, PendingIntent.FLAG_CANCEL_CURRENT);
//                    AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
//                    am.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pi);
//                    Runtime.getRuntime().exit(0);
                    mGp.settingExitClean=true;
                    finish();
                }
                @Override
                public void negativeResponse(Context context, Object[] objects) {
                    mGp.settingExitClean=true;
                }
            });
            mUtil.showCommonDialog(true, "W",
                    mContext.getString(R.string.msgs_smbsync_main_settings_jcifs_changed_restart), "", ntfy);
        }

        return changed;
    }

    //will trigger prompt to restart app if language changed by user or when import settings with a new language
    //when language is changed by user or import settings, ActivityMain onActivityResult() -> reloadSettingParms() -> mGp.loadSettingsParms() refreshes settingScreenThemeLanguageValue
    //reloadSettingParms() -> checkThemeLanguageChanged() triggers prompt to restart if language changed
    final private boolean checkThemeLanguageChanged() {
        boolean changed = false;

        if (!mGp.settingScreenThemeLanguageValue.equals(mGp.onStartSettingScreenThemeLanguageValue)) {
            changed = true;
            mGp.onStartSettingScreenThemeLanguageValue = mGp.settingScreenThemeLanguageValue;//do not prompt again to restart if it is cancelled, apply language on next restart
        }
        return changed;
    }

    private void saveTaskData() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");

        if (!isTaskTermination) {
            if (!isTaskDataExisted() || mGp.syncMessageListAdapter.resetDataChanged()) {
            }
        }
    }

    private String printStackTraceElement(StackTraceElement[] ste) {
        String st_msg = "";
        for (int i = 0; i < ste.length; i++) {
            st_msg += "\n at " + ste[i].getClassName() + "." +
                    ste[i].getMethodName() + "(" + ste[i].getFileName() +
                    ":" + ste[i].getLineNumber() + ")";
        }
        return st_msg;
    }

    private void restoreTaskData() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");
        File lf = new File(mGp.applicationRootDirectory + "/" + SMBSYNC2_SERIALIZABLE_FILE_NAME);
        if (lf.exists()) {
        }
    }

    private boolean isTaskDataExisted() {
        File lf = new File(getFilesDir() + "/" + SMBSYNC2_SERIALIZABLE_FILE_NAME);
        return lf.exists();
    }

    private void deleteTaskData() {
        File lf =
                new File(mGp.applicationRootDirectory + "/" + SMBSYNC2_SERIALIZABLE_FILE_NAME);
        if (lf.exists()) {
            lf.delete();
            mUtil.addDebugMsg(1, "I", "RestartData was delete.");
        }
    }

    /*
     * A class, that can be used as a TouchListener on any view (e.g. a Button).
     * It cyclically runs a clickListener, emulating keyboard-like behaviour. First
     * click is fired immediately, next one after the initialDelay, and subsequent
     * ones after the repeatDelay.
     *
     * Android default onLongClick can be returned by int ViewConfiguration.getLongPressTimeout() : 500 msec default
     * Interval is scheduled after the onClick completes, so it has to run fast.
     * If it runs slow, it does not generate skipped onClicks. Can be rewritten to
     * achieve this.
     */
    final static private int ANDROID_LONG_PRESS_TIMEOUT = 500;
    final static private int DEFAULT_LONG_PRESS_REPEAT_INTERVAL = 100;
    private class RepeatListener implements View.OnTouchListener {

        private Handler handler = new Handler();

        private final int mLongPressTimeout;
        private final int mRepeatInterval;
        private final boolean mConsumeEvent;
        private final OnClickListener mClickListener;
        private View mTouchedView;
//      private Rect mRect; // Variable to hold the bounds of the view rectangle

        private Runnable handlerRunnable = new Runnable() {
            @Override
            public void run() {
//                mUtil.addDebugMsg(1, "I", "runnable enterd, enabled="+mTouchedView.isEnabled());
                if (mTouchedView.isEnabled()) {
                    handler.postDelayed(this, mRepeatInterval);
                    mClickListener.onClick(mTouchedView);
                } else {
                    //view was disabled by the clickListener: remove the callback
                    //mUtil.addDebugMsg(2, "I", "runnable cancelled by View Removed");
                    handler.removeCallbacks(handlerRunnable);
                    mTouchedView.setPressed(false);
                    mTouchedView = null;
                }
                //mUtil.addDebugMsg(2, "I", "runnable running");
            }
        };

        /**
         * @param initialDelay The interval after first click event
         * @param repeatDelay The interval after second and subsequent click
         *        events (100 msec recommended)
         * @param clickListener The OnClickListener, that will be called
         *        periodically
         * @param consumeEvent: return value after touch event used
         *        set to false to be able to use the event by other methods directly by caller listener or to pass to parent view if needed
         * [@param] speedIncrementDelay: Optional, not implemented here
         *         delay after which the speed is even more incremented
         *         by default we will increment the speed by a 3x factor
         *         set to 0 to disable
         */
        public RepeatListener(int initialDelay, int repeatDelay, boolean consumeEvent, OnClickListener clickListener) {
            if (clickListener == null)
                throw new IllegalArgumentException("null runnable");
            if (initialDelay < 0 || repeatDelay < 0)
                throw new IllegalArgumentException("negative interval");

            mLongPressTimeout = initialDelay;
            mRepeatInterval = repeatDelay;
            mClickListener = clickListener;
            mConsumeEvent = consumeEvent;
        }

        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
//                    mUtil.addDebugMsg(1, "I", "runnable cancelled by ACTION_DOWN");
                    handler.removeCallbacks(handlerRunnable);
                    handler.postDelayed(handlerRunnable, mLongPressTimeout);
                    mTouchedView = view;
                    mTouchedView.setPressed(true);
                    mClickListener.onClick(view);
                    return mConsumeEvent;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_CANCEL:
//                    mUtil.addDebugMsg(1, "I", "runnable cancelled by ACTION_CANCEL");
                case MotionEvent.ACTION_UP:
//                    mUtil.addDebugMsg(1, "I", "runnable cancelled by Finger UP");
                    handler.removeCallbacks(handlerRunnable);
                    mTouchedView.setPressed(false);
                    mTouchedView = null;
                    return mConsumeEvent;
            }
            return false;
        }
    }

}

//class ActivityDataHolder implements Serializable {
//    private static final long serialVersionUID = 1L;
//}
