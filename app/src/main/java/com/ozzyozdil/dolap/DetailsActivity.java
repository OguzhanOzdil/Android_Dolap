package com.ozzyozdil.dolap;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.ozzyozdil.dolap.databinding.ActivityDetailsBinding;

import java.io.ByteArrayOutputStream;


public class DetailsActivity extends AppCompatActivity {

    private ActivityDetailsBinding binding;

    // Galeriye gitmek için
    ActivityResultLauncher<Intent> activityResultLauncher;
    // izin istemek için
    ActivityResultLauncher<String> permissionLauncher;

    Bitmap selectedImage;

    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        // Yeni obje mi veya listedeki obje mi onun ayrımını yapıyor
        if (info.equals("new")){

            // new art
            binding.etxtArt.setText("");
            binding.etxtArtist.setText("");
            binding.etxtYear.setText("");
            binding.btnSave.setVisibility(View.VISIBLE);
            binding.imageView.setImageResource(R.drawable.selectedimage);

        }
        else{

            int artId = intent.getIntExtra("artId", 0);
            binding.btnSave.setVisibility(View.INVISIBLE);

            try {
                                                                                        // ? yerine burdaki artId gelicek
                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", new String[] {String.valueOf(artId)});
                int nameArtIndex = cursor.getColumnIndex("artname");
                int nameArtistIndex = cursor.getColumnIndex("paintername");
                int yearIndex = cursor.getColumnIndex("year");
                int imageIndex = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){

                    binding.etxtArt.setText(cursor.getString(nameArtIndex));
                    binding.etxtArtist.setText(cursor.getString(nameArtistIndex));
                    binding.etxtYear.setText(cursor.getString(yearIndex));

                    byte[] bytes = cursor.getBlob(imageIndex);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.imageView.setImageBitmap(bitmap);

                }
                cursor.close();

            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    // Save Button OnClick
    public void save (View view){

        String nameArt = binding.etxtArt.getText().toString();
        String nameArtist = binding.etxtArtist.getText().toString();
        String year = binding.etxtYear.getText().toString();


        Bitmap smallImage = makeSmallerImage(selectedImage, 1000);

        // Görseli SQL için veriye çevirme
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
        byte[] byteArray = outputStream.toByteArray();

        // Verileri database e kaydetme
        try {

            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");

            String sqlString = "INSERT INTO arts (artname, paintername, year, image) VALUES (?, ?, ?, ?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);

            sqLiteStatement.bindString(1, nameArt);
            sqLiteStatement.bindString(2, nameArtist);
            sqLiteStatement.bindString(3, year);
            sqLiteStatement.bindBlob(4, byteArray);

            sqLiteStatement.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Save butonuna tıkladıktan sonra ana ekrana dönmek için
        Intent intent = new Intent(DetailsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    // Bitmap (Görsel Küçültme)
    public Bitmap makeSmallerImage(Bitmap image, int maxSize) {

        int width =image.getWidth();
        int height =image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1){

            // Landscape image
            width = maxSize;
            height = (int) (width / bitmapRatio);
        }
        else{

            // Portrait image
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image,width,height,true);
    }

    // ImageView OnClick
    public void selectImage (View view){

        // izin verilip verilmediği kontrol ediliyor
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

            // izin verilmediyse Snackbar ile açıklama yapılsın mı konrol ediliyor
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){

                Snackbar.make(view, "Fotoğraf eklemek için galeri erişimi gerekiyor!", Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // Request Permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            }
            else{

                // Request Permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        else{

            // Galeriye gidiyor
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }
    }

    // Activity Result Launcher
    private void registerLauncher(){

        // Galeriye gitmek için
                                                                                    // StartActivityForResult() = Bir sonuç için activity başlatıyorum
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {

                // getResultCode() = kullanıcının sonuç olarak ne yaptığını kontorleder örn: RESULT_OK kullanıcı resmi seçti
                if (result.getResultCode() == RESULT_OK){

                    Intent intentFromResult = result.getData();
                    if (intentFromResult != null){  // Geriye gerçekten veri dondü ise

                        Uri imageData = intentFromResult.getData();  // Verinin url ini verir (seçilen görselin nerede kayıtlı olduğunu verir)

                        try {

                            //if (Build.VERSION.SDK_INT >= 28){

                                // Burada url i görsele çeviriyoruz
                                ImageDecoder.Source source = ImageDecoder.createSource(DetailsActivity.this.getContentResolver(), imageData);
                                // Görseli bitmap e çeviriyoruz
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);
                            /*}
                            else{

                                selectedImage = MediaStore.Images.Media.getBitmap(DetailsActivity.this.getContentResolver(), imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }*/

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        // İzin istemek için
                          // registerForActivityResult() = sonunda bir cevap alacağımız işlemler yapmamızı sağlar
                                                                                // İzin işlemleri
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {

                if (result){

                    // Permission Granted = izin verildi
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                }
                else{

                    // Permission Denied = izin verilmedi
                    Toast.makeText(DetailsActivity.this, "İzin Gerekli!", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }
}