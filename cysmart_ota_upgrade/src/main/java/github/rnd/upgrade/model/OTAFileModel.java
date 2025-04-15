package github.rnd.upgrade.model;

import android.net.Uri;

/**
 * Data Model class for OTA File
 * 2025/4/15
 * Add FileUri use Android 14+
 */
public class OTAFileModel {
    /**
     *File name
     */
    private String mFileName = null;
    /**
     *File path
     */
    private String mFilePath = null;


    /**
     * File Uri
     */
    private Uri mFileUri = null;


    /**
     * File parent
     */
    private String mFileParent = null;
    /**
     *Selection Flag
     *
     */
    private boolean mSelected = false;


    // Constructor
    public OTAFileModel(String fileName, String filePath, Uri fileUri, boolean selected, String fileParent) {
        super();
        this.mFileName = fileName;
        this.mFilePath = filePath;
        this.mFileUri = fileUri;
        this.mSelected = selected;
        this.mFileParent = fileParent;
    }

    public OTAFileModel() {
        super();
    }

    public String getFileName() {
        return mFileName;
    }

    public String getmFileParent() {
        return mFileParent;
    }

    public void setmFileParent(String mFileParent) {
        this.mFileParent = mFileParent;
    }

    public void setFileName(String mFileName) {
        this.mFileName = mFileName;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public Uri getmFileUri() {
        return mFileUri;
    }

    public void setmFileUri(Uri mFileUri) {
        this.mFileUri = mFileUri;
    }

    public void setName(String mFilePath) {
        this.mFilePath = mFilePath;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        this.mSelected = selected;
    }
}
