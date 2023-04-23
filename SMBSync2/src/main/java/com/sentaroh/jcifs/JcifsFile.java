package com.sentaroh.jcifs;

/*
The MIT License (MIT)
Copyright (c) 2018 Sentaroh

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;


public class JcifsFile {

    final static public int JCIFS_FILE_SMB1 = JcifsAuth.JCIFS_FILE_SMB1;
    final static public int JCIFS_FILE_SMB2 = JcifsAuth.JCIFS_FILE_SMB2;
    final static public int JCIFS_FILE_SMB3 = JcifsAuth.JCIFS_FILE_SMB3;

    private int mSmbLevel = JCIFS_FILE_SMB2;

    private JcifsAuth mAuth = null;

    private jcifs.smb.SmbFile mSmbFile = null;

    public JcifsFile(String url, JcifsAuth auth) throws MalformedURLException, JcifsException {
        if (auth==null) {
            throw new JcifsException("JcifsAuth is null.");
        }
        mSmbLevel = auth.getSmbLevel();
        mAuth = auth;
        mSmbFile = new jcifs.smb.SmbFile(url, auth.getSmbAuth());
    }

    private JcifsFile(jcifs.smb.SmbFile smbFile, JcifsAuth auth) throws JcifsException {
        if (auth==null) {
            throw new JcifsException("JcifsAuth is null");
        }
        mSmbLevel = JCIFS_FILE_SMB2;
        mAuth = auth;
        mSmbFile = smbFile;
    }

    public jcifs.smb.SmbFile getSmbFile() {
        return mSmbFile;
    }

    public boolean exists() throws JcifsException {
        try {
            return mSmbFile.exists();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public void delete() throws JcifsException {
        try {
            mSmbFile.delete();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public void mkdir() throws JcifsException {
        try {
            mSmbFile.mkdir();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public void mkdirs() throws JcifsException {
        try {
            mSmbFile.mkdirs();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public int getAttributes() throws JcifsException {
        try {
            return mSmbFile.getAttributes();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public InputStream getInputStream() throws JcifsException {
        try {
            return mSmbFile.getInputStream();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        } catch (IOException e) {
            throw (new JcifsException(e, 0, e.getCause()));
        }
    }


    public OutputStream getOutputStream() throws JcifsException {
        try {
            return mSmbFile.getOutputStream();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        } catch (IOException e) {
            throw (new JcifsException(e, 0, e.getCause()));
        }
    }

    public void close() throws JcifsException {
        mSmbFile.close();
    }

    public void connect() throws JcifsException {
        try {
            mSmbFile.connect();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        } catch (IOException e) {
            throw (new JcifsException(e, 0, e.getCause()));
        }
    }

    public void createNew() throws JcifsException {
        try {
            mSmbFile.createNewFile();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public String getName() {
        return mSmbFile.getName();
    }

    public String getPath() {
        return mSmbFile.getPath();
    }

    public String getCanonicalPath() {
        return mSmbFile.getCanonicalPath();
    }

    public String getShare() {
        return mSmbFile.getShare();
    }

    public int getType() throws JcifsException {
        try {
            return mSmbFile.getType();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public String getUncPath() {
        return mSmbFile.getUncPath();
    }

    public String getParent() {
        return mSmbFile.getParent();
    }

    public boolean canRead() throws JcifsException {
        try {
            return mSmbFile.canRead();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public boolean canWrite() throws JcifsException {
        try {
            return mSmbFile.canWrite();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public boolean isDirectory() throws JcifsException {
        try {
            return mSmbFile.isDirectory();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public boolean isFile() throws JcifsException {
        try {
            return mSmbFile.isFile();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public boolean isHidden() throws JcifsException {
        try {
            return mSmbFile.isHidden();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public long length() throws JcifsException {
        try {
            return mSmbFile.length();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public String[] list() throws JcifsException {
        try {
            return mSmbFile.list();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public JcifsFile[] listFiles() throws JcifsException {
        try {
            jcifs.smb.SmbFile[] smbFiles = mSmbFile.listFiles();
            if (smbFiles == null) return null;
            JcifsFile[] result = new JcifsFile[smbFiles.length];
            for (int i = 0; i < smbFiles.length; i++)
                result[i] = new JcifsFile(smbFiles[i], mAuth);
            return result;
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public void renameTo(JcifsFile d) throws JcifsException {
        try {
            if (d.getSmbFile() == null)
                throw new JcifsException("Null SMB file specified.");
            else mSmbFile.renameTo(d.getSmbFile());
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public JcifsAuth getAuth() {
        return mAuth;
    }

    public void setLastModified(long lm) throws JcifsException {
        try {
            mSmbFile.setLastModified(lm);
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public long getLastModified() throws JcifsException {
        try {
            return mSmbFile.lastModified();
        } catch (jcifs.smb.SmbException e) {
            throw (new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

}
