package android.com.saveimage;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;



public class MainActivity extends AppCompatActivity implements View.OnClickListener {

   private SharedPreferences preferences;
   private ImageView imageView;
   private TextView textView;
   private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("key", 0);
        String img_str = preferences.getString("bitmap", BuildConfig.FLAVOR);

        textView = findViewById(R.id.tv_text);
        imageView = findViewById(R.id.imageView);

        if (!img_str.equals(BuildConfig.FLAVOR)) {
            textView.setText("");
            byte[] imageAsBytes = Base64.decode(img_str.getBytes(), 0);
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length));
        }

        imageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                dialog = initDialog();
                Window window = dialog.getWindow();
                window.setGravity(Gravity.BOTTOM);
                dialog.show();
            }
        });

    }

    private Dialog initDialog() {
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(getDialogLayout());
        return dialog;
    }


    private View getDialogLayout() {
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog, null);
        TextView tvPhoto = dialogView.findViewById(R.id.btn_choose_photo);
        TextView tvCamera = dialogView.findViewById(R.id.btn_choose_camera);
        TextView tvCancel = dialogView.findViewById(R.id.btn_choose_cancel);
        tvPhoto.setOnClickListener(this);
        tvCamera.setOnClickListener(this);
        tvCancel.setOnClickListener(this);
        return dialogView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_choose_photo: {

                Intent choosePicIntent = new Intent(Intent.ACTION_PICK);
                File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS);
                String pictureDirectoriPath = pictureDirectory.getPath();
                Uri data = Uri.parse(pictureDirectoriPath);
                choosePicIntent.setDataAndType(data, "image/*");
                startActivityForResult(choosePicIntent, 100);
                dialog.dismiss();
                break;
            }
            case R.id.btn_choose_camera: {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 200);
                dialog.dismiss();
                break;
            }
            case R.id.btn_choose_cancel: {
                dialog.dismiss();
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 100: {
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        Bitmap bitmap;
                        Uri selectImage = data.getData();
                        textView.setText("");
                        bitmap = optimizeImageForListView(selectImage);
                        imageView.setImageBitmap(bitmap);
                        savaImageInSharedPref(bitmap);
                    }
                }
                break;
            }
            case 200: {
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        textView.setText("");
                        Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                        imageView.setImageBitmap(bitmap);
                        String img_str = bitmapToString(bitmap);
                        clearImageFromShared();
                        addImageInShared(img_str);
                    }
                }
            }
            break;

        }
    }

    private void savaImageInSharedPref(Bitmap bitmap) {
        String img_str = bitmapToString(bitmap);
        clearImageFromShared();
        addImageInShared(img_str);
    }

    private String bitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        byte[] image = stream.toByteArray();
        return Base64.encodeToString(image, 0);
    }

    private void addImageInShared(String img_str) {
        SharedPreferences preferences = getSharedPreferences("key", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("bitmap", img_str);
        editor.apply();
    }

    private void clearImageFromShared() {
        SharedPreferences preferences = getSharedPreferences("key", 0);
        if (preferences.contains("bitmap")) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("Bitmap");
            editor.apply();
        }
    }

    private Bitmap optimizeImageForListView(Uri uri){
        Bitmap bitmap = null;
        int origHeight,origWidth;
        try {
            InputStream in = getContentResolver().openInputStream(uri);

            BitmapFactory.Options bitmapOption = new BitmapFactory.Options();
            bitmapOption.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, bitmapOption);
            assert in != null;
            in.close();

            int orientation = getOrientation(this, uri);

            if (orientation == 90 || orientation == 270) {
                origHeight = bitmapOption.outWidth;
                origWidth = bitmapOption.outHeight;
            } else {
                origWidth = bitmapOption.outWidth;
                origHeight = bitmapOption.outHeight;
            }

            int bytesPerPixel = 2 ;
            int maxSize = 480 * 800 * bytesPerPixel;
            int desiredWidth = 400;
            int desiredHeight =300;
            int desiredSize = desiredWidth * desiredHeight * bytesPerPixel;
            if (desiredSize < maxSize) maxSize = desiredSize;
            int scale = 1;
            if (origWidth > origHeight) {
                scale = Math.round((float) origHeight / (float) desiredHeight);
            } else {
                scale = Math.round((float) origWidth / (float) desiredWidth);
            }

            bitmapOption = new BitmapFactory.Options();
            bitmapOption.inSampleSize = scale;
            bitmapOption.inPreferredConfig = Bitmap.Config.RGB_565;


            in = getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(in, null, bitmapOption);

            if (orientation > 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    public static int getOrientation(Context context, Uri photoUri) {
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor.getCount() != 1) {
            return -1;
        }
        cursor.moveToFirst();
        return cursor.getInt(0);
    }
}
