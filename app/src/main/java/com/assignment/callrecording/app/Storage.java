package com.assignment.callrecording.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;

import com.github.axet.audiolibrary.encoders.Factory;
import com.github.axet.audiolibrary.encoders.Format3GP;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class Storage extends com.github.axet.audiolibrary.app.Storage {
    public static String TAG = Storage.class.getSimpleName();

    public static final String EXT_3GP = Format3GP.EXT;
    public static final String EXT_3GP16 = Format3GP.EXT + "16"; // sample rate 16Hz
    public static final String EXT_AAC = "aac"; // MPEG_4 / AAC
    public static final String EXT_AACHE = EXT_AAC + "he"; // MPEG_4 / HE AAC
    public static final String EXT_AACELD = EXT_AAC + "eld"; // MPEG_4 / AAC ELD
    public static final String EXT_WEBM = "webm";

    public static CharSequence[] getEncodingTexts(Context context) {
        CharSequence[] ee = Factory.getEncodingTexts(context);
        ArrayList<CharSequence> ll = new ArrayList<>(Arrays.asList(ee));
        ll.add("." + EXT_3GP + " (MediaRecorder)"); // AMRNB 8kHz
//        if (Build.VERSION.SDK_INT >= 10)
//            ll.add(".3gp (MediaRecorder AMRWB 16kHz)");
        if (Build.VERSION.SDK_INT >= 10)
            ll.add("." + EXT_AAC + " (MediaRecorder)");
//        if (Build.VERSION.SDK_INT >= 16)
//            ll.add(".aac (MediaRecorder AACHE)");
//        if (Build.VERSION.SDK_INT >= 16)
//            ll.add(".aac (MediaRecorder AACELD)");
//        if (Build.VERSION.SDK_INT >= 21)
//            ll.add(".webm (MediaRecorder WEBM)");
        return ll.toArray(new CharSequence[]{});
    }

    public static String[] getEncodingValues(Context context) {
        String[] ee = Factory.getEncodingValues(context);
        ArrayList<String> ll = new ArrayList<>(Arrays.asList(ee));
        ll.add(EXT_3GP);
//        if (Build.VERSION.SDK_INT >= 10)
//            ll.add(EXT_3GP16);
        if (Build.VERSION.SDK_INT >= 10)
            ll.add(EXT_AAC);
//        if (Build.VERSION.SDK_INT >= 16)
//            ll.add(EXT_AACHE);
//        if (Build.VERSION.SDK_INT >= 16)
//            ll.add(EXT_AACELD);
//        if (Build.VERSION.SDK_INT >= 21)
//            ll.add(EXT_WEBM);
        return ll.toArray(new String[]{});
    }

    public static boolean isMediaRecorder(String ext) {
        switch (ext) {
            case EXT_3GP:
            case EXT_3GP16:
            case EXT_AAC:
            case EXT_AACHE:
            case EXT_AACELD:
            case EXT_WEBM:
                return true;
        }
        return false;
    }

    public static String filterMediaRecorder(String ext) {
        if (ext.startsWith(EXT_3GP))
            ext = EXT_3GP; // replace "3gp16" -> "3gp"
        if (ext.startsWith(EXT_AAC))
            ext = EXT_AAC; // replace "aache" / "aaceld" -> "aac"
        return ext;
    }

    public static String escape(String s) {
        return s.replace("/", "\\\\");
    }

    public static String getFormatted(String format, long now, String phone, String contact, String call) {
        if (contact != null && !contact.isEmpty()) {
            format = format.replaceAll("%c", escape(contact));
        } else {
            if (phone != null && !phone.isEmpty())
                format = format.replaceAll("%c", phone);
            else
                format = format.replaceAll("%c", "");
        }

        if (phone != null && !phone.isEmpty())
            format = format.replaceAll("%p", phone);
        else
            format = format.replaceAll("%p", "");

        format = format.replaceAll("%T", "" + now / 1000);
        format = format.replaceAll("%s", SIMPLE.format(new Date()));
        format = format.replaceAll("%I", ISO8601.format(new Date()));

        if (call == null || call.isEmpty()) {
            format = format.replaceAll("%i", "");
        } else {
            switch (call) {
                case CallApplication.CALL_IN:
                    format = format.replaceAll("%i", "↓");
                    break;
                case CallApplication.CALL_OUT:
                    format = format.replaceAll("%i", "↑");
                    break;
            }
        }

        format = format.replaceAll("  ", " ");

        format = format.trim();

        return format;
    }

    public Storage(Context context) {
        super(context);
    }

    @Override
    public Uri migrate(File ff, Uri tt) {
        Uri t = super.migrate(ff, tt);
        if (t == null)
            return null;
        Uri f = Uri.fromFile(ff);
        CallApplication.setContact(context, t, CallApplication.getContact(context, f)); // copy contact to migrated file
        CallApplication.setCall(context, t, CallApplication.getCall(context, f)); // copy call to migrated file
        return t;
    }

    @Override
    public Uri rename(Uri f, String tt) {
        Uri t = super.rename(f, tt);
        if (t == null)
            return null;
        CallApplication.setContact(context, t, CallApplication.getContact(context, f)); // copy contact to new name
        CallApplication.setCall(context, t, CallApplication.getCall(context, f)); // copy call to new name
        return t;
    }

    public boolean recordingNextPending() {
        File tmp = getTempRecording();
        if (tmp.exists())
            return true;
        File parent = tmp.getParentFile();
        File[] ff = parent.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().startsWith(TMP_REC);
            }
        });
        return ff != null && ff.length > 0;
    }

    public File getTempNextRecording() {
        File tmp = super.getTempRecording();
        if (tmp.exists())
            return tmp;
        File parent = tmp.getParentFile();
        File[] ff = parent.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().startsWith(TMP_REC);
            }
        });
        if (ff == null)
            return null;
        if (ff.length == 0)
            return null;
        return ff[0];
    }

    public Uri getNewFile(long now, String phone, String contact, String call) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        String ext = shared.getString(com.github.axet.audiolibrary.app.MainApplication.PREFERENCE_ENCODING, "");
        ext = filterMediaRecorder(ext);

        String format = "%s";
        format = shared.getString(CallApplication.PREFERENCE_FORMAT, format);

        format = getFormatted(format, now, phone, contact, call);

        Uri parent = getStoragePath();
        String s = parent.getScheme();
        if (s.equals(ContentResolver.SCHEME_FILE)) {
            File f = getFile(parent);
            if (!f.exists() && !f.mkdirs())
                throw new RuntimeException("Unable to create: " + f);
        }
        return getNextFile(context, parent, format, ext);
    }

}
