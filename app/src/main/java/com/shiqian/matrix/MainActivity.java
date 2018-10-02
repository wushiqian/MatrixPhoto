package com.shiqian.matrix;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.jaeger.library.StatusBarUtil;
import com.shiqian.matrix.utils.FilterUtils;
import com.shiqian.matrix.utils.PhotoUtils;
import com.shiqian.matrix.view.DrawingView;
import com.shiqian.matrix.view.TransformativeImageView;
import com.shiqian.photoedit.utils.MatisseGlide;
import com.xw.repo.BubbleSeekBar;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.List;

import es.dmoral.toasty.Toasty;

//TODO 分享，保存, 手势
//FIXME

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    /**
     * DATA
     */
    private static final String FILE_PATH = "com.shiqian.matrix.fileprovider";
    private static final String TAG = "MainActivity";
    private int REQUEST_CODE_CHOOSE = 2;

    //按钮状态
    private static int COLOR_PANEL = 0;
    private static int BRUSH = 0;
    private static int FILTER = 0;
    private static int DETAILS = 0;
    private static int CROP = 0;
    private static int DRAW = 0;

    private List<Uri> mSelected;
    private Uri photo = null;
    private Bitmap mBitmap;

    private float mHue = 0.0f;
    private float mSaturation = 1f;
    private float mLum = 1f;
    private int MID_VALUE = 127;
    private int MAX_VALUE = 255;

    private static final int REQUEST_PERMISSION_CODE = 0;

    /**
     * UI
     */
    private TransformativeImageView mDrawingView;
    private ImageButton mColorPanel;
    private ImageButton mBrush;
    private ImageButton mUndo;
    private ImageButton mSave;
    private ImageButton mFilter;
    private ImageButton mDetails;
    private ImageButton mCrop;
    private ImageButton mDraw;
    private ImageButton mOK;
    private ImageButton mCancel;
    private SeekBar hueSeekBar;
    private SeekBar satSeekBar;
    private SeekBar lumSeekBar;
    private BubbleSeekBar rotateSeekBar;
    private LinearLayout detailsBar;
    private LinearLayout rotateBar;
    private LinearLayout filterBar;
    private LinearLayout cropBar;
    private LinearLayout paintBar;
    private LinearLayout okBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initPaintMode();
    }

    /**
     * 初始化模式
     */
    private void initPaintMode() {
        mDrawingView.initializePen();
        mDrawingView.setPenSize(10);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mDrawingView.setPenColor(getColor(R.color.red));
        }
    }

    /**
     * 加载菜单
     *
     * @param menu 菜单
     * @return 是否有菜单
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    /**
     * 菜单点击事件
     *
     * @param item 子选项
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION_CODE);
                } else {
                    Matisse.from(MainActivity.this)
                            .choose(MimeType.ofImage())
                            .countable(false)
                            .capture(true) //使用拍照功能
                            .captureStrategy(new CaptureStrategy(true, FILE_PATH))
                            .maxSelectable(1)
                            .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.media_big_size))
                            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                            .thumbnailScale(0.85f)
                            .theme(R.style.Matisse_Zhihu)//主题
                            .imageEngine(new MatisseGlide())
                            .forResult(REQUEST_CODE_CHOOSE);
                }
                break;
            case R.id.share:
                String shareString = "分享照片";
                shareImage(shareString);
                break;
        }
        return true;
    }


    /**
     * 初始化界面
     */
    private void initView() {
        StatusBarUtil.setColor(this, getResources().getColor(R.color.zhihu_primary));

        detailsBar = findViewById(R.id.details_bar);
        filterBar = findViewById(R.id.filter_bar);
        rotateBar = findViewById(R.id.rotate_bar);
        cropBar = findViewById(R.id.crop_bar);
        paintBar = findViewById(R.id.paint_bar);
        okBar = findViewById(R.id.ok_bar);

        mDrawingView = findViewById(R.id.iv_main);
//        mBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
//        mDrawingView.loadImage(mBitmap);
        mBrush = findViewById(R.id.brush);
        mColorPanel = findViewById(R.id.color_panel);
        mUndo = findViewById(R.id.undo);
        mSave = findViewById(R.id.save);

        mBrush.setOnClickListener(this);
        mColorPanel.setOnClickListener(this);
        mUndo.setOnClickListener(this);
        mSave.setOnClickListener(this);

        mFilter = findViewById(R.id.filter);
        mDetails = findViewById(R.id.details);
        mCrop = findViewById(R.id.Crop);
        mDraw = findViewById(R.id.Draw);

        mFilter.setOnClickListener(this);
        mDetails.setOnClickListener(this);
        mCrop.setOnClickListener(this);
        mDraw.setOnClickListener(this);

        mOK = findViewById(R.id.OK);
        mCancel = findViewById(R.id.Cancel);
        mOK.setOnClickListener(this);
        mCancel.setOnClickListener(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        hueSeekBar = findViewById(R.id.sb_hue);
        satSeekBar = findViewById(R.id.sb_saturation);
        lumSeekBar = findViewById(R.id.sb_lum);
        rotateSeekBar = findViewById(R.id.sb_rotate);
        hueSeekBar.setMax(MAX_VALUE);
        lumSeekBar.setMax(MAX_VALUE);
        satSeekBar.setMax(MAX_VALUE);
//        rotateSeekBar.setMax(180);
//        rotateSeekBar.setMin(-180);
        rotateSeekBar.setProgress(0);
        hueSeekBar.setProgress(MID_VALUE);
        satSeekBar.setProgress(MID_VALUE);
        lumSeekBar.setProgress(MID_VALUE);
        hueSeekBar.setOnSeekBarChangeListener(this);
        satSeekBar.setOnSeekBarChangeListener(this);
        lumSeekBar.setOnSeekBarChangeListener(this);
        rotateSeekBar.setOnProgressChangedListener((new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                switch (bubbleSeekBar.getId()) {
                    case R.id.sb_rotate:
                        mDrawingView.loadImage(PhotoUtils.rotateImage(mBitmap, progress));
                        break;
                }
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {

            }
        }));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    /*
     * SeekBar滚动时的回调函数
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        Log.d(TAG, "seekid:" + seekBar.getId() + ", progess" + progress);
        boolean isChanged = false;
        switch (seekBar.getId()) {
            case R.id.sb_hue:
                mHue = (progress - MID_VALUE) * 1.0F / MID_VALUE * 180;
                isChanged = true;
                break;
            case R.id.sb_saturation:
                mSaturation = progress * 1.0F / MID_VALUE;
                isChanged = true;
                break;
            case R.id.sb_lum:
                mLum = progress * 1.0F / MID_VALUE;
                isChanged = true;
                break;
        }
        if (isChanged)
            mDrawingView.loadImage(PhotoUtils.handleImageEffect(mBitmap, mHue, mSaturation, mLum));
    }

    //申请权限回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //用户已授权
                    Matisse.from(MainActivity.this)
                            .choose(MimeType.ofAll())
                            .countable(false)
                            .capture(true) //使用拍照功能
                            .captureStrategy(new CaptureStrategy(true, FILE_PATH))
                            .maxSelectable(1)
                            .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.media_big_size))
                            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                            .thumbnailScale(0.85f)
                            .imageEngine(new MatisseGlide())
                            .forResult(REQUEST_CODE_CHOOSE);
                } else {
                    //用户未授权
                    Toasty.error(this, "未授权", Toast.LENGTH_SHORT, true).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            mSelected = Matisse.obtainResult(data);
            Log.d("Matisse", "mSelected: " + mSelected);
            startCrop();
        }
        if (resultCode == RESULT_OK) {
            //裁切成功
            if (requestCode == UCrop.REQUEST_CROP) {
                Uri croppedFileUri = UCrop.getOutput(data);
//                savePhoto(croppedFileUri);
                try {
                    mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(croppedFileUri));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                // 使用Glide显示图片
//                Glide.with(this)
//                        .load(mBitmap)
//                        .into(mDrawingView);
//                mDrawingView.loadImage(mBitmap);
                mBitmap = PhotoUtils.ScaleImage(mBitmap, 2, 2);
                mDrawingView.loadImage(mBitmap);
                photo = croppedFileUri;
                hueSeekBar.setProgress(MID_VALUE);
                satSeekBar.setProgress(MID_VALUE);
                lumSeekBar.setProgress(MID_VALUE);
            }
        }
        //裁切失败
        if (resultCode == UCrop.RESULT_ERROR) {
            Toasty.error(this, "裁切图片失败", Toast.LENGTH_SHORT, true).show();
        }
    }

    //保存裁剪后的照片
    private void savePhoto(Uri croppedFileUri) {
        //获取默认的下载目录
        String downloadsDirectoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String filename = String.format("%d_%s", Calendar.getInstance().getTimeInMillis(), croppedFileUri.getLastPathSegment());
        File saveFile = new File(downloadsDirectoryPath, filename);
        //保存下载的图片
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inStream = new FileInputStream(new File(croppedFileUri.getPath()));
            outStream = new FileOutputStream(saveFile);
            inChannel = inStream.getChannel();
            outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            Toasty.success(this, "Success!", Toast.LENGTH_SHORT, true).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outChannel.close();
                outStream.close();
                inChannel.close();
                inStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //裁剪框架配置
    private void startCrop() {
        Uri sourceUri = mSelected.get(0);
        //裁剪后保存到文件中
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "SampleCropImage.jpeg"));
        UCrop uCrop = UCrop.of(sourceUri, destinationUri);
        // 修改配置参数
        UCrop.Options options = new UCrop.Options();
        // 修改标题栏颜色
        options.setToolbarColor(getResources().getColor(R.color.zhihu_primary));
        // 修改状态栏颜色
        options.setStatusBarColor(getResources().getColor(R.color.zhihu_primary));
        //设置裁剪图片可操作的手势
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
        //设置图片在切换比例时的动画
//        options.setImageToCropBoundsAnimDuration(666);
        //设置最大缩放比例
        options.setMaxScaleMultiplier(4);
        // 设置图片压缩质量
//        options.setCompressionQuality(100);
        //设置裁剪框横竖线的颜色
//        options.setCropGridColor(Color.GREEN);
        uCrop.withOptions(options);
        uCrop = uCrop.useSourceImageAspectRatio();
        uCrop.withMaxResultSize(800, 800).start(this);
    }

    /**
     * 滤镜
     */
    public void btnNegative(View view) {
        mDrawingView.loadImage(FilterUtils.handleImageNegative(mBitmap));
    }

    public void btnOld(View view) {
        mDrawingView.loadImage(FilterUtils.handleImageOld(mBitmap));
    }

    public void btnEmboss(View view) {
        mDrawingView.loadImage(FilterUtils.handleImageEmboss(mBitmap));
    }

    public void btnPolaroid(View view) {
        mDrawingView.loadImage(FilterUtils.handleImagePolaroid(mBitmap));
    }

    public void btnCool(View view) {
        mDrawingView.loadImage(FilterUtils.handleImageCool(mBitmap));
    }

    public void btnNeon(View view) {
        mDrawingView.loadImage(FilterUtils.handleImageNeon(mBitmap));
    }

    public void btnRotate(View view) {
        mDrawingView.loadImage(PhotoUtils.rotateImage(mBitmap, 90));
        mBitmap = PhotoUtils.rotateImage(mBitmap, 90);
    }

    public void btnReRotate(View view) {
        mDrawingView.loadImage(PhotoUtils.rotateImage(mBitmap, -90));
        mBitmap = PhotoUtils.rotateImage(mBitmap, -90);
    }

    public void btnScale(View view) {
        mDrawingView.loadImage(PhotoUtils.ScaleImage(mBitmap, 2, 2));
    }

    public void btnTranslate(View view) {
        Matrix matrix = new Matrix();
        matrix.setTranslate(100, 100);
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight(); // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix, true);
        mBitmap = resizedBitmap;
        mDrawingView.loadImage(mBitmap);
    }

    public void btnSkew(View view) {
        Matrix matrix = new Matrix();
        matrix.setSkew(0.5f, 2);
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight(); // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix, true);
        mBitmap = resizedBitmap;
        mDrawingView.loadImage(mBitmap);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.brush:
                mBrush.setImageResource(BRUSH == 0 ? R.drawable.ic_brush : R.drawable.ic_pen);
                mDrawingView.setPenSize(BRUSH == 0 ? 40 : 10);
                BRUSH = 1 - BRUSH;
                break;
            case R.id.color_panel:
                mColorPanel.setImageResource(COLOR_PANEL == 0 ? R.drawable.ic_color_blue : R.drawable.ic_color_red);
                mDrawingView.setPenColor(COLOR_PANEL == 0 ? getColor(R.color.blue) : getColor(R.color.red));
                COLOR_PANEL = 1 - COLOR_PANEL;
                break;
            case R.id.undo:
                mDrawingView.undo();
                break;
            case R.id.save:
                String sdcardPath = Environment.getExternalStorageDirectory().toString();
                if (mDrawingView.saveImage(sdcardPath, "DrawImg", Bitmap.CompressFormat.PNG, 100)) {
                    Toasty.success(this, "Save Success", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.filter:
                close();
                okBar.setVisibility(FILTER == 0 ? View.VISIBLE : View.GONE);
                filterBar.setVisibility(FILTER == 0 ? View.VISIBLE : View.GONE);
                if (FILTER == 0) allZero();
                FILTER = 1 - FILTER;
                break;
            case R.id.details:
                close();
                okBar.setVisibility(DETAILS == 0 ? View.VISIBLE : View.GONE);
                detailsBar.setVisibility(DETAILS == 0 ? View.VISIBLE : View.GONE);
                if (DETAILS == 0) allZero();
                DETAILS = 1 - DETAILS;
                break;
            case R.id.Crop:
                close();
                okBar.setVisibility(CROP == 0 ? View.VISIBLE : View.GONE);
                rotateBar.setVisibility(CROP == 0 ? View.VISIBLE : View.GONE);
                cropBar.setVisibility(CROP == 0 ? View.VISIBLE : View.GONE);
                if (CROP == 0) allZero();
                CROP = 1 - CROP;
                break;
            case R.id.Draw:
                close();
//                mDrawingView.setImageBitmap(mBitmap);
                mDrawingView.loadImage(mBitmap);
//                Intent intent = new Intent(this,PaintActivity.class);
//                startActivity(intent);
                mDrawingView.setDrawMode(DrawingView.DRAW);
                if (DRAW == 1) mDrawingView.setDrawMode(DrawingView.SCALE);
                mDrawingView.loadImage(mBitmap);
                okBar.setVisibility(DRAW == 0 ? View.VISIBLE : View.GONE);
                paintBar.setVisibility(DRAW == 0 ? View.VISIBLE : View.GONE);
                if (DRAW == 0) allZero();
                DRAW = 1 - DRAW;
                break;
            case R.id.OK:
                mBitmap = mDrawingView.getImageBitmap();
                allZero();
                mDrawingView.setDrawMode(DrawingView.SCALE);
                close();
                break;
            case R.id.Cancel:
                mDrawingView.loadImage(mBitmap);
                mDrawingView.clear();
                allZero();
                mDrawingView.setDrawMode(DrawingView.SCALE);
                close();
                break;
            default:
                break;
        }
    }

    private void allZero() {
        FILTER = 0;
        CROP = 0;
        DETAILS = 0;
        DRAW = 0;
    }

    private void close() {
        if (okBar.getVisibility() == View.VISIBLE) okBar.setVisibility(View.GONE);
        if (filterBar.getVisibility() == View.VISIBLE) filterBar.setVisibility(View.GONE);
        else if (paintBar.getVisibility() == View.VISIBLE) paintBar.setVisibility(View.GONE);
        else if (cropBar.getVisibility() == View.VISIBLE) {
            cropBar.setVisibility(View.GONE);
            rotateBar.setVisibility(View.GONE);
        } else if (detailsBar.getVisibility() == View.VISIBLE) detailsBar.setVisibility(View.GONE);
    }

    public void shareImage(String title) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();
        Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), mBitmap, null,null));
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);//设置分享行为
        intent.setType("image/*");//设置分享内容的类型
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent = Intent.createChooser(intent, title);
        startActivity(intent);
    }

}