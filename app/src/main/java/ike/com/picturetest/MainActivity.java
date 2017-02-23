package ike.com.picturetest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import net.bither.util.NativeUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import ike.com.picturetest.threadManager.ThreadManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button up,choose;
    private ImageView iv;
    private int IMAGE=1;
    private String Tag="MainActivity";
    private List<String> mPics=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        up= (Button) findViewById(R.id.up);
        choose= (Button) findViewById(R.id.choose);
        iv= (ImageView) findViewById(R.id.iv);
        up.setOnClickListener(this);
        choose.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.up:
             upLoadPictures();
                mPics.clear();
                break;
            case R.id.choose:
                //调用相册
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, IMAGE);
                break;
        }
    }
    private void upFile(String filePath) {
        File f = new File(filePath);
        String desPath= BaseApplication.context.getCacheDir().getPath() + (BaseApplication.context.getCacheDir().getPath().endsWith(File.separator) ? "" : File.separator) +System.currentTimeMillis()+".jpg";
        NativeUtil.compressBitmap(BitmapFactory.decodeFile(filePath), desPath);
        Log.e(Tag,"desPath:"+desPath);
        String path="https://cms.scbrkj.com/lzh-api/api/basic/upload";
        RequestParams param=new RequestParams(path);
        param.setConnectTimeout(10*1000);
        param.addBodyParameter("sk","ca8f200c3884d0f8f89eceef002a4005d3e457922441d0d4ad34ee86b4cfe334f5e36e2695e32b757452c696f1f447672309da68cb5b9b35e2542f7b7836d03e");
        param.addBodyParameter("fileType",10+"");
        param.addBodyParameter("docType",20+"");
        File f1 = new File(desPath);
       // param.addBodyParameter("files",f1.getName());
        param.addBodyParameter("files",f1);
        x.http().post(param, new Callback.ProgressCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.e(Tag,"onSuccess:"+result);
                try {
                    JSONObject object=new JSONObject(result);
                    String data = object.getString("data");
                    JSONObject object1=new JSONObject(data);
                    JSONArray sourceUrls = object1.getJSONArray("sourceUrls");
                    String o = (String) sourceUrls.get(0);

                    Glide.with(MainActivity.this).load(o).into(iv);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
               // Log.e(Tag,"onError:"+ex.getMessage());
            }

            @Override
            public void onCancelled(CancelledException cex) {
              //  Log.e(Tag,"onCancelled:"+cex.getMessage());
            }

            @Override
            public void onFinished() {
                Log.e(Tag,"onFinished:");
            }

            @Override
            public void onWaiting() {
             //   Log.e(Tag,"onWaiting:");
            }

            @Override
            public void onStarted() {
               // Log.e(Tag,"onStarted:");
            }

            @Override
            public void onLoading(long total, long current, boolean isDownloading) {
                Log.e(Tag,"onLoading:"+total+",current:"+current);
            }
        });

    }
    private  String imagePath;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //获取图片路径
        if (requestCode == IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumns = {MediaStore.Images.Media.DATA};
            Cursor c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePathColumns[0]);
            imagePath = c.getString(columnIndex);
            Log.e(Tag,"imagePath:"+("file://"+imagePath));
            mPics.add(imagePath);


           // upFile(desPath);
            c.close();
        }
    }
    public void upLoadPictures(){
        for (String path:mPics){
            ThreadManager.getLongPool().execute(new UpTask(path));
        }
    }
    class UpTask implements Runnable{
        String path;

        public UpTask(String path) {
            this.path = path;
        }

        @Override
        public void run() {
            upFile(path);
        }
    }
}
