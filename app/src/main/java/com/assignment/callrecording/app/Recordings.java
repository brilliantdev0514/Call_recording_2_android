package com.assignment.callrecording.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.axet.androidlibrary.widgets.ErrorDialog;
import com.assignment.callrecording.R;
import com.assignment.callrecording.activities.SettingsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeSet;

import static android.content.Context.AUDIO_SERVICE;
import static android.widget.Toast.LENGTH_LONG;

public class Recordings extends com.github.axet.audiolibrary.app.Recordings {
    public static final String ID = "_id";



    protected View toolbar_i;
    protected View toolbar_o;
    View refresh;
    public TextView progressText;
    public View progressEmpty;

    boolean toolbarFilterIn;
    boolean toolbarFilterOut;


    public void volumeControl(){
        Random mRandom = new Random();
        AudioManager mAudioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        int media_max_volume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        int random_volume = mRandom.nextInt(((media_max_volume - 0) + 1) + 0);

        mAudioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC, // Stream type
                random_volume, // Index
                AudioManager.FLAG_SHOW_UI // Flags
        );
    }

    public class RecordingHolder extends com.github.axet.audiolibrary.app.Recordings.RecordingHolder {
        LinearLayout s;
        ImageView i;
        public ImageView volume;

        public RecordingHolder(View v) {
            super(v);
            s = (LinearLayout) v.findViewById(R.id.recording_status);
            i = (ImageView) v.findViewById(R.id.recording_call);
            volume = (ImageView) v.findViewById(R.id.recording_volume_controller);
            volume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    volumeControl();
                }
            });
        }
    }

    public static class SortByContact implements Comparator<com.github.axet.audiolibrary.app.Storage.RecordingUri> {
        Context context;
        HashMap<Uri, String> contacts = new HashMap<>();

        public SortByContact(Context context) {
            this.context = context;
        }

        public String getContact(Uri uri) {
            String c = contacts.get(uri);
            if (c == null) {
                c = "";
                String id = CallApplication.getContact(context, uri);
                if (!id.isEmpty() && Storage.permitted(context, SettingsActivity.CONTACTS)) {
                    ContentResolver resolver = context.getContentResolver();
                    Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, BaseColumns._ID + " == ?", new String[]{id}, null);
                    if (cursor != null) {
                        try {
                            if (cursor.moveToNext())
                                c = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        } finally {
                            cursor.close();
                        }
                    }
                }
                contacts.put(uri, c);
            }
            return c;
        }

        @Override
        public int compare(Storage.RecordingUri file, Storage.RecordingUri file2) {
            String c1 = getContact(file.uri);
            String c2 = getContact(file2.uri);
            return c1.compareTo(c2);
        }
    }

    public Recordings(Context context, RecyclerView list) {
        super(context, list);
    }

    public void setEmptyView(View empty) {
        this.empty.setEmptyView(empty);
        refresh = empty.findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                load(false, null);
            }
        });
        progressText = (TextView) empty.findViewById(android.R.id.text1);
        progressEmpty = empty.findViewById(R.id.progress_empty);
    }

    @Override
    public void load(Uri mount, boolean clean, Runnable done) {
        progressText.setText(R.string.recording_list_is_empty);
        refresh.setVisibility(View.GONE);
        if (!com.github.axet.audiolibrary.app.Storage.exists(context, mount)) { // folder may not exist, do not show error
            scan(new ArrayList<com.github.axet.audiolibrary.app.Storage.Node>(), clean, done);
            return;
        }
        try {
            super.load(mount, clean, done);
        } catch (RuntimeException e) {
            Log.e(TAG, "unable to load", e);
            refresh.setVisibility(View.VISIBLE);
            progressText.setText(ErrorDialog.toMessage(e));
            scan(new ArrayList<com.github.axet.audiolibrary.app.Storage.Node>(), clean, done);
        }
    }

    @Override
    public String[] getEncodingValues() {
        return Storage.getEncodingValues(context);
    }

    @Override
    public void cleanDelete(TreeSet<String> delete, Uri f) {
        super.cleanDelete(delete, f);
        String p = CallApplication.getFilePref(f);
        delete.remove(p + CallApplication.PREFERENCE_DETAILS_CONTACT);
        delete.remove(p + CallApplication.PREFERENCE_DETAILS_CALL);
    }

    @Override
    public RecordingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View convertView = inflate(inflater, R.layout.recording, parent);
        return new RecordingHolder(convertView);
    }

    @Override
    public void onBindViewHolder(com.github.axet.audiolibrary.app.Recordings.RecordingHolder hh, int position) {
        super.onBindViewHolder(hh, position);
        RecordingHolder h = (RecordingHolder) hh;
        Storage.RecordingUri u = getItem(position);
        String call = CallApplication.getCall(context, u.uri);
        if (call == null || call.isEmpty()) {
            h.i.setVisibility(View.GONE);
        } else {
            switch (call) {
                case CallApplication.CALL_IN:
                    h.i.setVisibility(View.VISIBLE);
                    h.i.setImageResource(R.drawable.ic_call_received_black_24dp);
                    break;
                case CallApplication.CALL_OUT:
                    h.i.setVisibility(View.VISIBLE);
                    h.i.setImageResource(R.drawable.ic_call_made_black_24dp);
                    break;
            }
        }


    }

    @Override
    public Comparator<Storage.RecordingUri> getSort() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        int selected = context.getResources().getIdentifier(shared.getString(CallApplication.PREFERENCE_SORT, context.getResources().getResourceEntryName(R.id.sort_name_ask)), "id", context.getPackageName());
        switch (selected) {
            case R.id.sort_contact_ask:
                return new SortByContact(context);
            case R.id.sort_contact_desc:
                return Collections.reverseOrder(new SortByContact(context));
            default:
                return super.getSort();
        }
    }

    @Override
    protected boolean filter(Storage.RecordingUri f) {
        boolean include = super.filter(f);
        if (include) {
            if (!toolbarFilterIn && !toolbarFilterOut)
                return true;
            String call = CallApplication.getCall(context, f.uri);
            if (call == null || call.isEmpty())
                return false;
            if (toolbarFilterIn)
                return call.equals(CallApplication.CALL_IN);
            if (toolbarFilterOut)
                return call.equals(CallApplication.CALL_OUT);
        }
        return include;
    }

    public void setToolbar(ViewGroup v) {
        toolbar_i = v.findViewById(R.id.toolbar_in);
        toolbar_i.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toolbarFilterIn = !toolbarFilterIn;
                if (toolbarFilterIn)
                    toolbarFilterOut = false;
                selectToolbar();
                load(false, null);
                save();
            }
        });
        toolbar_o = v.findViewById(R.id.toolbar_out);
        toolbar_o.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toolbarFilterOut = !toolbarFilterOut;
                if (toolbarFilterOut)
                    toolbarFilterIn = false;
                selectToolbar();
                load(false, null);
                save();
            }
        });
        super.setToolbar(v);
    }

    protected void selectToolbar() {
        super.selectToolbar();
        selectToolbar(toolbar_i, toolbarFilterIn);
        selectToolbar(toolbar_o, toolbarFilterOut);
    }

    protected void save() {
        super.save();
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = shared.edit();
        edit.putBoolean(CallApplication.PREFERENCE_FILTER_IN, toolbarFilterIn);
        edit.putBoolean(CallApplication.PREFERENCE_FILTER_OUT, toolbarFilterOut);
        edit.commit();
    }

    protected void load() {
        super.load();
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        toolbarFilterIn = shared.getBoolean(CallApplication.PREFERENCE_FILTER_IN, false);
        toolbarFilterOut = shared.getBoolean(CallApplication.PREFERENCE_FILTER_OUT, false);
    }


}
