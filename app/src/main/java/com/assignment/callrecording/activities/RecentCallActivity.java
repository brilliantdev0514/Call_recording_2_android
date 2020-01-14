package com.assignment.callrecording.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.WindowCallbackWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.axet.androidlibrary.widgets.PopupWindowCompat;
import com.assignment.callrecording.R;
import com.assignment.callrecording.app.CallApplication;
import com.assignment.callrecording.app.Storage;

public class RecentCallActivity extends AppCompatActivity {

    public static int AUTO_CLOSE = 5; // secs

    Handler handler = new Handler();
    Runnable update = new Runnable() {
        @Override
        public void run() {
            update();
        }
    };
    View close;
    TextView count;
    ImageView fav;
    ProgressBar progressBar;
    EditText name;
    View msg;
    int c = 0;
    Uri uri;
    Storage storage;
    String old;
    String ext;
    AlertDialog alertDialog;

    public static void startActivity(Context context, Uri targetUri, boolean count) {
        Intent intent = new Intent(context, RecentCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("uri", targetUri);
        intent.putExtra("count", count);
        context.startActivity(intent);
    }

    public static void showLocked(Window w) {
        w.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        // enable popup keyboard while locked
        w.addFlags(android.view.WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 21)
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    }

    int getAppTheme() {
        return CallApplication.getTheme(this, R.style.AppThemeDialogLight, R.style.Theme_AppCompat_DayNight_Dialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showLocked(getWindow());

        storage = new Storage(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        final View v = inflater.inflate(R.layout.activity_recentcall, null);

        close = v.findViewById(R.id.callrecent_close);
        progressBar = (ProgressBar) v.findViewById(R.id.callrecent_progress);
        count = (TextView) v.findViewById(R.id.callrecent_autosave);
        msg = v.findViewById(R.id.callrecent_msg);
        name = (EditText) v.findViewById(R.id.callrecent_name);
        fav = (ImageView) v.findViewById(R.id.callrecent_fav);

        final boolean count = getIntent().getBooleanExtra("count", true);
        uri = getIntent().getParcelableExtra("uri");
        String f = Storage.getName(this, uri);
        old = Storage.getNameNoExt(f);
        ext = Storage.getExt(f);
        name.setText(old);

        fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean b = CallApplication.getStar(RecentCallActivity.this, uri);
                CallApplication.setStar(RecentCallActivity.this, uri, !b);
                updateFav();
            }
        });

        alertDialog = new AlertDialog.Builder(this, getAppTheme())
                .setTitle(R.string.app_name)
                .setIcon(R.drawable.ic_mic_24dp)
                .setPositiveButton(R.string.save_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        save();
                    }
                })
                .setNeutralButton(R.string.delete_recording, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        close();
                    }
                })
                .setView(v).create();

        alertDialog.setCancelable(false);

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        delete();
                    }
                });

                final Window w = alertDialog.getWindow();

                showLocked(w);

                final Window.Callback c = w.getCallback();
                w.setCallback(new WindowCallbackWrapper(c) {
                    @Override
                    public boolean dispatchKeyEvent(KeyEvent event) {
                        onUserInteraction();
                        return c.dispatchKeyEvent(event);
                    }

                    @Override
                    public boolean dispatchTouchEvent(MotionEvent event) {
                        Rect rect = PopupWindowCompat.getOnScreenRect(w.getDecorView());
                        if (rect.contains((int) event.getRawX(), (int) event.getRawY()))
                            onUserInteraction();
                        return c.dispatchTouchEvent(event);
                    }
                });

                name.setSelection(name.getText().length());
                if (count)
                    update();
                else
                    countClose();
            }
        });

        alertDialog.show();
    }

    void update() {
        int p = c / 100;

        if (p >= AUTO_CLOSE) {
            close();
            return;
        }

        count.setText(getString(R.string.callrecent_auto_save, AUTO_CLOSE - p));
        progressBar.setProgress(c * 100 / AUTO_CLOSE / 100);
        updateFav();

        c++;

        handler.removeCallbacks(update);
        handler.postDelayed(update, 10);
    }

    void updateFav() {
        fav.setImageResource(CallApplication.getStar(this, uri) ? R.drawable.ic_star_black_24dp : R.drawable.ic_star_border_black_24dp);
    }

    void countClose() {
        handler.removeCallbacks(update);
        msg.setVisibility(View.GONE);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        countClose();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setVisible(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        countClose();
    }

    void close() {
        finish();
    }

    void save() {
        String n = name.getText().toString();
        if (!old.equals(n)) {
            String s = String.format("%s.%s", n, ext);
            Storage.rename(this, uri, s);
            MainActivity.last(this);
        }
    }

    void delete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(com.github.axet.audiolibrary.R.string.delete_recording);
        builder.setMessage("...\\" + Storage.getName(this, uri) + "\n\n" + getString(R.string.are_you_sure));
        builder.setPositiveButton(com.github.axet.audiolibrary.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Storage.delete(RecentCallActivity.this, uri);
                MainActivity.last(RecentCallActivity.this);
                alertDialog.dismiss();
            }
        });
        builder.setNegativeButton(com.github.axet.audiolibrary.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

}
