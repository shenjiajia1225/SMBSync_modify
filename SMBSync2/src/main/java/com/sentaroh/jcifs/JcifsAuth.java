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

import java.util.Properties;

public class JcifsAuth {
    final static public int JCIFS_FILE_SMB1 = 1;
    final static public int JCIFS_FILE_SMB2 = 2;
    final static public int JCIFS_FILE_SMB3 = 3;

    private jcifs.CIFSContext mSmbAuth = null;
    private int mSmbLevel = JCIFS_FILE_SMB2;

    private String mDomain = null, mUserName = null, mUserPass = null;

    /**
     * SMB1 or SMB2 Constructor
     *
     * @param smb1   1 is use jcifs-1.3.17, 2 is use jcifs-ng 2.1.0, 3 is jcifs-ng 2.1.1, 4 is jcifs-ng 2.1.2
     * @param domain A domain name
     * @param user   A user name
     * @param pass   A password for user
     * @throws JcifsException
     */
    @SuppressWarnings("deprecation")
    public JcifsAuth(int smb_level, String domain, String user, String pass) {
        mSmbLevel = JCIFS_FILE_SMB2;
        mDomain = domain;
        mUserName = user;
        mUserPass = pass;
        try {
            Properties prop = new Properties();
            prop.setProperty("jcifs.smb.client.minVersion", "SMB202");
            prop.setProperty("jcifs.smb.client.maxVersion", "SMB311");
            jcifs.context.BaseContext bc = new jcifs.context.BaseContext(new jcifs.config.PropertyConfiguration(prop));
            jcifs.smb.NtlmPasswordAuthentication creds = new jcifs.smb.NtlmPasswordAuthentication(bc, domain, user, pass);
            mSmbAuth = bc.withCredentials(creds);
        } catch (jcifs.CIFSException e) {
            e.printStackTrace();
        }
    }

    /**
     * SMB2 Constructor
     *
     * @param smb1   		1 is use jcifs-1.3.17, 2 is use jcifs-ng 2.1.0, 3 is jcifs-ng 2.1.1, 4 is jcifs-ng 2.1.2
     * @param domain        A domain name
     * @param user          A user name
     * @param pass          A password for user
     * @param ipc_signing_enforced true is use IpcSigningEnforced
     * @throws JcifsException
     */
    @SuppressWarnings("deprecation")
    public JcifsAuth(int smb_level, String domain, String user, String pass, boolean ipc_signing_enforced) {
        mSmbLevel = JCIFS_FILE_SMB2;
        mDomain = domain;
        mUserName = user;
        mUserPass = pass;
        try {
            Properties prop = new Properties();
            if (ipc_signing_enforced)
                prop.setProperty("jcifs.smb.client.ipcSigningEnforced", "true");
            else prop.setProperty("jcifs.smb.client.ipcSigningEnforced", "false");

            prop.setProperty("jcifs.smb.client.minVersion", "SMB202");
            prop.setProperty("jcifs.smb.client.maxVersion", "SMB311");

            jcifs.context.BaseContext bc = new jcifs.context.BaseContext(new jcifs.config.PropertyConfiguration(prop));
            jcifs.smb.NtlmPasswordAuthentication creds = new jcifs.smb.NtlmPasswordAuthentication(bc, domain, user, pass);
            mSmbAuth = bc.withCredentials(creds);
        } catch (jcifs.CIFSException e) {
            e.printStackTrace();
        }

    }

    /**
     * SMB2 Constructor
     *
     * @param smb1   		1 is use jcifs-1.3.17, 2 is use jcifs-ng 2.1.0, 3 is jcifs-ng 2.1.1, 4 is jcifs-ng 2.1.2
     * @param domain        A domain name
     * @param user          A user name
     * @param pass          A password for user
     * @param ipc_signing_enforced true is use IpcSigningEnforced
     * @param use_smb2_nego true is use SMB2 Negotiation 
     * @throws JcifsException
     */
    @SuppressWarnings("deprecation")
    public JcifsAuth(int smb_level, String domain, String user, String pass, boolean ipc_signing_enforced, boolean use_smb2_nego) {
        mSmbLevel = JCIFS_FILE_SMB2;
        mDomain = domain;
        mUserName = user;
        mUserPass = pass;
        try {
            Properties prop = new Properties();
            if (ipc_signing_enforced)
                prop.setProperty("jcifs.smb.client.ipcSigningEnforced", "true");
            else prop.setProperty("jcifs.smb.client.ipcSigningEnforced", "false");

            if (use_smb2_nego) prop.setProperty("jcifs.smb.client.useSMB2Negotiation", "true");
            else prop.setProperty("jcifs.smb.client.useSMB2Negotiation", "false");

            prop.setProperty("jcifs.smb.client.minVersion", "SMB202");
            prop.setProperty("jcifs.smb.client.maxVersion", "SMB311");

            jcifs.context.BaseContext bc = new jcifs.context.BaseContext(new jcifs.config.PropertyConfiguration(prop));
            jcifs.smb.NtlmPasswordAuthentication creds = new jcifs.smb.NtlmPasswordAuthentication(bc, domain, user, pass);
            mSmbAuth = bc.withCredentials(creds);
        } catch (jcifs.CIFSException e) {
            e.printStackTrace();
        }

    }

    /**
     * SMB2 Constructor
     *
     * @param smb1   		1 is use jcifs-1.3.17, 2 is use jcifs-ng 2.1.0, 3 is jcifs-ng 2.1.1, 4 is jcifs-ng 2.1.2
     * @param domain        A domain name
     * @param user          A user name
     * @param pass          A password for user
     * @param ipc_signing_enforced true is use IpcSigningEnforced
     * @param use_smb2_nego true is use SMB2 Negotiation 
     * @param ptop			 jcifs option property
     * @throws JcifsException
     */
    @SuppressWarnings("deprecation")
    public JcifsAuth(int smb_level, String domain, String user, String pass, boolean ipc_signing_enforced, boolean use_smb2_nego, 
    		Properties prop) {
        mSmbLevel = JCIFS_FILE_SMB2;
        mDomain = domain;
        mUserName = user;
        mUserPass = pass;
        Properties prop_new = new Properties(prop);
        try {
            if (ipc_signing_enforced)
                prop_new.setProperty("jcifs.smb.client.ipcSigningEnforced", "true");
            else prop_new.setProperty("jcifs.smb.client.ipcSigningEnforced", "false");

            if (use_smb2_nego) prop_new.setProperty("jcifs.smb.client.useSMB2Negotiation", "true");
            else prop_new.setProperty("jcifs.smb.client.useSMB2Negotiation", "false");

            prop_new.setProperty("jcifs.smb.client.minVersion", "SMB202");
            prop_new.setProperty("jcifs.smb.client.maxVersion", "SMB311");

            jcifs.context.BaseContext bc = new jcifs.context.BaseContext(new jcifs.config.PropertyConfiguration(prop_new));
            jcifs.smb.NtlmPasswordAuthentication creds = new jcifs.smb.NtlmPasswordAuthentication(bc, domain, user, pass);
            mSmbAuth = bc.withCredentials(creds);
        } catch (jcifs.CIFSException e) {
            e.printStackTrace();
        }
    }

    /**
     * SMB2 Constructor
     *
     * @param smb1   1 is use jcifs-1.3.17, 2 is use jcifs-ng 2.1.0, 3 is jcifs-ng 2.1.1, 4 is jcifs-ng 2.1.2
     * @param domain               A domain name
     * @param user                 A user name
     * @param pass                 A password for user
     * @param ipc_signing_enforced true is use IpcSigningEnforced
     * @param min_version          min SMB version ("SMB1" or "SMB210")
     * @param max_version          max SMB version ("SMB1" or "SMB210")
     * @throws JcifsException
     */
    @SuppressWarnings("deprecation")
    public JcifsAuth(int smb_level, String domain, String user, String pass, boolean ipc_signing_enforced, String min_version, String max_version) {
        mSmbLevel = JCIFS_FILE_SMB2;
        mDomain = domain;
        mUserName = user;
        mUserPass = pass;
        try {
            Properties prop = new Properties();
            if (ipc_signing_enforced)
                prop.setProperty("jcifs.smb.client.ipcSigningEnforced", "true");
            else prop.setProperty("jcifs.smb.client.ipcSigningEnforced", "false");
            prop.setProperty("jcifs.smb.client.useSMB2Negotiation", "true");
            prop.setProperty("jcifs.smb.client.minVersion", min_version);
            prop.setProperty("jcifs.smb.client.maxVersion", max_version);

            jcifs.context.BaseContext bc = new jcifs.context.BaseContext(new jcifs.config.PropertyConfiguration(prop));
            jcifs.smb.NtlmPasswordAuthentication creds = new jcifs.smb.NtlmPasswordAuthentication(bc, domain, user, pass);
            mSmbAuth = bc.withCredentials(creds);
        } catch (jcifs.CIFSException e) {
            e.printStackTrace();
        }
    }

    public int getSmbLevel() {
        return mSmbLevel;
    }

    public jcifs.CIFSContext getSmbAuth() {
        return mSmbAuth;
    }

    public String getDomain() {
        return mDomain;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getUserPass() {
        return mUserPass;
    }
}
