package com.cds.scanner.tc3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity
{
    //Declaracion de variables
    WebView webView;
    private static final String TAG = MainActivity.class.getSimpleName();
    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR=1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        if(Build.VERSION.SDK_INT >= 21)
        {
            Uri[] results = null;

           //Checar resultados del item para tomar fotos o insertar documento desde el telefono
            if(resultCode== Activity.RESULT_OK)
            {
                if(requestCode == FCR){
                    if(null == mUMA)
                    {
                        return;
                    }
                    if(intent == null || intent.getData() == null)
                    {
                        //Intrucion paraCapturar foto si no hay imagen disponible
                        if(mCM != null)
                        {
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    }
                    else
                        {
                        String dataString = intent.getDataString();
                        if(dataString != null)
                        {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        }
        else
        {
            if(requestCode == FCR)
            {
                if(null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Pedir permisos para el uso de la camara y abrir archivos en telefono
        if(Build.VERSION.SDK_INT >=23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED))
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
        }
        //Declarar webview
        webView = (WebView) findViewById(R.id.ifView);
        assert webView != null;
        //Declarar websettings para el uso de controles
        WebSettings webSettings = webView.getSettings();
        //Utilizar javascrito
        webSettings.setJavaScriptEnabled(true);
        //herramienta que Activa o desactiva
        webSettings.setAllowFileAccess(true);
        //Identificar que la app se puede utilzar desde la version igual o mayor al sdk 21
        if(Build.VERSION.SDK_INT >= 21)
        {
            webSettings.setMixedContentMode(0);
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        //Metodo para identificar si la app cargo bien o mandar mensajes de errores
        webView.setWebViewClient(new Callback());
        //Incrustar direcion https
        webView.loadUrl("https://s3v1.solucionesdigitales.com.mx/Turycon33");
       //Utilizar controles de webchrome
        webView.setWebChromeClient(new WebChromeClient()
        {
            //For Android 5.0+
            //Elegir dentro del fileChooser capturar imagen o obtenerla desde el dispositivo
            public boolean onShowFileChooser
            (
                    //Control para insetar documento desde el dispositivo o capturarlo con la camara y agregarlo a la appweb
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams){
                        if(mUMA != null)
                        {
                            mUMA.onReceiveValue(null);
                        }
                        mUMA = filePathCallback;
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if(takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null){
                            File photoFile = null;
                            try
                            {
                                photoFile = createImageFile();
                                takePictureIntent.putExtra("PhotoPath", mCM);
                            }
                            catch(IOException ex)
                            {
                                Log.e(TAG, "Fallo al capturar el reporte", ex);
                            }
                            if(photoFile != null)
                            {
                                mCM = "file:" + photoFile.getAbsolutePath();
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                            }
                            else
                            {
                                takePictureIntent = null;
                            }
                        }
                        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                        contentSelectionIntent.setType("*/*");
                        Intent[] intentArray;
                        if(takePictureIntent != null)
                        {
                            intentArray = new Intent[]{takePictureIntent};
                        }
                        else
                        {
                            intentArray = new Intent[0];
                        }

                        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                        startActivityForResult(chooserIntent, FCR);
                        return true;
                    }
        });
    }
    //Comprobar si la aplicacion inicio y no tuvo problemas de conexion
    public class Callback extends WebViewClient
    {
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
        {
            Toast.makeText(getApplicationContext(), "No hay conexion de internet!", Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), "Fallo al iniciar!", Toast.LENGTH_SHORT).show();

        }
    }
    // Funcion para Crear imagen con la camara
    private File createImageFile() throws IOException
    {
        //Generar nombre con formato Foto_yyyyMMddhhmmssSS
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSS").format(new Date());
        String imageFileName = "Foto_"+timeStamp+".jpg";
        //Crear directorio publico en el telefono
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //Asignar nombre y ubicacion de la foto
        File file = new File(storageDir, imageFileName);
        file.createNewFile();
        return file;


    }
    //Accion para el boton volver en la aplicacion
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event){
        if(event.getAction() == KeyEvent.ACTION_DOWN)
        {
            switch(keyCode)
            {
                case KeyEvent.KEYCODE_BACK:
                    if(webView.canGoBack())
                    {
                        webView.goBack();
                    }
                    else
                    {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }

}
