package overdector.com.overdrawdector;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import overdector.com.bean.RGBbean;
import overdector.com.data.MyAdapter;
import overdector.com.permission.PermissionsManager;
import overdector.com.permission.PermissionsResultAction;
import overdector.com.view.MyGlsurfaceView;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button mOverdrawBtn;
    private TextView mOverdrawRresult;
    private ListView mColorList;
    private MyAdapter myAdapter;
    private Uri mUri;

    private MyGlsurfaceView myGlsurfaceView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mOverdrawBtn = (Button) findViewById(R.id.overdraw_btn);
        mOverdrawRresult = (TextView) findViewById(R.id.overdraw_result);
        mOverdrawBtn.setOnClickListener(this);
        mColorList = (ListView) findViewById(R.id.overdraw_listview);
        myAdapter = new MyAdapter();
        mColorList.setAdapter(myAdapter);

        myGlsurfaceView = new MyGlsurfaceView(this);
        myGlsurfaceView.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                Log.d("huhao-", "onSurfaceCreated");
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                Log.d("huhao-", "onSurfaceChanged-width: " + width + "-height: " + height);
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                Log.d("huhao-", "onDrawFrame");
            }
        });
        myGlsurfaceView.setEnabled(true);
        myGlsurfaceView.setVisibility(View.VISIBLE);
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        int key = v.getId();
        switch (key){
            case R.id.overdraw_btn:
                pickPhoto();
                break;
        }
    }

    /**
     * 添加照片
     */
    private void pickPhoto() {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, new PermissionsResultAction() {
            @Override
            public void onGranted() {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 70);
            }

            @Override
            public void onDenied(String permission) {
//                new PermissionsDialog(TestOptionActivity.this, PermissionsDialog.PermissionsStyle.STORAGE).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 70){
            if(resultCode == Activity.RESULT_OK){
                mUri = data.getData();
                jumpToImageEdit(mUri);
            }else if(resultCode == Activity.RESULT_CANCELED){
            }
        }
        if(requestCode == 71){
            if(resultCode == RESULT_OK){
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final int width = bitmap.getWidth();
                final int height = bitmap.getHeight();
                final int[] sum = {0};
                final Bitmap finalBitmap = bitmap;
                Observable.create(new Observable.OnSubscribe<RGBbean>() {
                    @Override
                    public void call(Subscriber<? super RGBbean> subscriber) {
                        Bundle bundle = data.getExtras();
                        ArrayList<RGBbean> rgBbeenlist = bundle.getParcelableArrayList("rgblist");
                        if(rgBbeenlist != null){
                            for(RGBbean rgBbean : rgBbeenlist){
                                subscriber.onNext(rgBbean);
                            }
                        }
                        subscriber.onCompleted();
                    }
                }).subscribe(new Observer<RGBbean>() {

                    @Override
                    public void onCompleted() {
                        if(sum[0] != 0){
                            float total = width * height;
                            mOverdrawRresult.setText(((((float) sum[0])/total) * 100) + "%");
                            Log.d("TestOptionActivity", "totle=" + width * height);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(RGBbean rgBbean) {
                        if(mUri != null) {
                            if(width < 0 || height < 0){
                                return;
                            }
                            int[] pixels = new int[finalBitmap.getWidth() * finalBitmap.getHeight()];
                            finalBitmap.getPixels(pixels,0,finalBitmap.getWidth(),0,0,finalBitmap.getWidth(),finalBitmap.getHeight());
                            for(int i = 0; i < pixels.length; i++){
                                int clr = pixels[i];
                                int  r   = (clr & 0x00ff0000) >> 16;
                                int  g = (clr & 0x0000ff00) >> 8;
                                int  b  =  clr & 0x000000ff;
                                if(r == rgBbean.getR() && g == rgBbean.getG() && b == rgBbean.getB()){
                                    sum[0]++;
                                }
                            }
                        }
                        Log.d("TestOptionActivity", "rgBbean=" + rgBbean.getR() + "," + rgBbean.getG() + "," + rgBbean.getB());
                        Log.d("TestOptionActivity", "sum=" + sum[0]);
                    }
                });
            }
        }

    }

    /**
     * 跳转到图片编辑页面
     * @param uri
     */
    private void jumpToImageEdit(Uri uri) {
        Intent intent = new Intent(this, ImageViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("image", uri);
        intent.putExtras(bundle);
        startActivityForResult(intent, 71);
    }
}
