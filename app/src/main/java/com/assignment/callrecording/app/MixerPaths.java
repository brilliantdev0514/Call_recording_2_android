package com.assignment.callrecording.app;

import android.util.Log;

import com.github.axet.androidlibrary.app.MountInfo;
import com.github.axet.androidlibrary.app.SuperUser;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://gitlab.com/axet/android-call-recorder/merge_requests/4
//
public class MixerPaths {
    public static final String TAG = MixerPaths.class.getSimpleName();

    public static final Pattern MIXER_PATHS = Pattern.compile(Storage.wildcard("mixer_paths*.xml"));
    public static final Pattern VOC_REC = Pattern.compile("VOC_REC.*value=\"(\\d+)\"");
    public static final String VENDOR = "/vendor";
    public static final String REMOUNT_VENDOR = SuperUser.BIN_MOUNT + " -o remount,rw " + VENDOR;

    public static final String PATH = find(MIXER_PATHS, SuperUser.SYSTEM + SuperUser.ETC, VENDOR + SuperUser.ETC, SuperUser.ETC);

    public static final String TRUE = "1";
    public static final String FALSE = "0";

    protected String xml;

    public static String find(final Pattern p, String... dd) {
        for (String d : dd) {
            File f = new File(d);
            File[] ff = f.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    Matcher m = p.matcher(name.toLowerCase());
                    return m.find();
                }
            });
            if (ff != null && ff.length > 0) {
                Arrays.sort(ff);
                return ff[0].getAbsolutePath();
            }
        }
        return null;
    }

    public MixerPaths() {
        if (PATH == null)
            return;
        load();
    }

    public void load() {
        try {
            xml = null;
            xml = IOUtils.toString(new FileReader(PATH));
        } catch (IOException e) {
            Log.d(TAG, "Unable to read mixers", e);
        }
    }

    public void save() {
        SuperUser.Commands args = new SuperUser.Commands();
        if (PATH.startsWith(VENDOR) && new MountInfo().findMount(new File(VENDOR)) != null)
            args.add(REMOUNT_VENDOR);
        else
            args.add(SuperUser.REMOUNT_SYSTEM);
        args.add(MessageFormat.format(SuperUser.CAT_TO, PATH, xml.trim()));
        SuperUser.su(args).must();
    }

    public void save(boolean b) {
        setEnabled(b);
        save();
        load();
        if (b != isEnabled()) {
            throw new RuntimeException("Unable to write changes");
        }
    }

    public boolean isCompatible() {
        if (!SuperUser.isRooted())
            return false;
        return isSupported();
    }

    public boolean isSupported() {
        if (xml == null || xml.isEmpty())
            return false;
        Matcher m = VOC_REC.matcher(xml);
        if (m.find()) {
            return true;
        }
        return false;
    }

    public boolean isEnabled() {
        Matcher m = VOC_REC.matcher(xml);
        while (m.find()) {
            String v = m.group(1);
            if (!v.equals(TRUE))
                return false;
        }
        return true;
    }

    public void setEnabled(boolean b) {
        Matcher m = VOC_REC.matcher(xml);
        StringBuffer sb = new StringBuffer(xml.length());
        while (m.find()) {
            m.appendReplacement(sb, m.group().replaceFirst(Pattern.quote(m.group(1)), b ? TRUE : FALSE));
        }
        m.appendTail(sb);
        xml = sb.toString();
    }
}
