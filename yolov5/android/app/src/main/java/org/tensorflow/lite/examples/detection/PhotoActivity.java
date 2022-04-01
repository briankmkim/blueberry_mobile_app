package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.DetectorFactory;
import org.tensorflow.lite.examples.detection.tflite.YoloV5Classifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PhotoActivity extends AppCompatActivity {

    private ImageView image;

    private int mCounter = 0;
    Button btn;
    TextView txv;

    private String imagePath;
    private Bitmap bitmap = null;

    private YoloV5Classifier detector;

    private String ASSET_PATH = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        image = findViewById(R.id.imageView);

        Intent intent = getIntent();
        imagePath = intent.getStringExtra("imagePath");
        imagePath += "/image.jpg";

        ActivityCompat.requestPermissions(PhotoActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        ActivityCompat.requestPermissions(PhotoActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        System.out.println("TRYING BITMAP");

        try {
            detector = DetectorFactory.getDetector(getAssets(), "best-fp16.tflite");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            bitmap = BitmapFactory.decodeFile(imagePath);
            detect(bitmap);
            // image.setImageBitmap(bitmap);
            System.out.printf("IMAGE DIMS: %d, %d%n", bitmap.getWidth(), bitmap.getHeight());
        } catch (Exception e) {
            System.out.println("ERROR ERROR");
            e.printStackTrace();
        }

        btn = (Button) findViewById(R.id.countButton);
        txv = (TextView) findViewById(R.id.counter);

        btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                mCounter++;
                txv.setText(Integer.toString(mCounter));
            }
        });

    }

    public void detect(Bitmap b) {
        final long startTime = SystemClock.uptimeMillis();
        final List<Classifier.Recognition> results = detector.recognizeImage(b);

        Log.e("CHECK", "run: " + results.size());

        Bitmap copyBitmap = Bitmap.createBitmap(b);
        Bitmap mutableBitmap = copyBitmap.copy(Bitmap.Config.ARGB_8888, true);
        final Canvas canvas = new Canvas(mutableBitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        float minimumConfidence = 0.2f;
        final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);

                // cropToFrameTransform.mapRect(location);

                result.setLocation(location);
                mappedRecognitions.add(result);
                System.out.println("DETECTED ONE");
            }
        }

        image.setImageBitmap(mutableBitmap);
        //saveToGallery();
    }

    public void saveToGallery(){
        BitmapDrawable bitmapDrawable = (BitmapDrawable) image.getDrawable();
        Bitmap bitmap = bitmapDrawable.getBitmap();

        FileOutputStream outputStream = null;
        File file = Environment.getExternalStorageDirectory();
        File dir = new File(file.getAbsolutePath() + "/MyPics");
        dir.mkdirs();

        String filename = String.format("%d.png",System.currentTimeMillis());
        File outFile = new File(dir,filename);
        try{
            outputStream = new FileOutputStream(outFile);
        }catch (Exception e){
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream);
        try{
            outputStream.flush();
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void onBack(View view) {
        Intent intent = new Intent(this, DetectorActivity.class);
        startActivity(intent);
    }

    public void onSave(View view) {
        saveToGallery();
        Intent intent = new Intent(this, DetectorActivity.class);
        startActivity(intent);
    }



}