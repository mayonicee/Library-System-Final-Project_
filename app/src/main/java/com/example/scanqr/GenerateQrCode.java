package com.example.scanqr;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class GenerateQrCode extends BaseActivity {
// Ini biisa di hapus kalo udah bikin auto generate qr
    private EditText edtBookId;
    private ImageView imgQr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr_code);

        edtBookId = findViewById(R.id.edtBookId);
        imgQr = findViewById(R.id.imgQr);
        Button btnGenerate = findViewById(R.id.btnGenerate);
        setupBottomBar(R.id.nav_library);
        btnGenerate.setOnClickListener(v -> {
            String bookId = edtBookId.getText().toString().trim();
            if (TextUtils.isEmpty(bookId)) {
                Toast.makeText(this, "Book ID tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Bitmap qrBitmap = generateQrBitmap(bookId, 512, 512);
                imgQr.setImageBitmap(qrBitmap);
                Toast.makeText(this, "QR untuk \"" + bookId + "\" dibuat", Toast.LENGTH_SHORT).show();
            } catch (WriterException e) {
                Toast.makeText(this, "Gagal generate QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap generateQrBitmap(String text, int width, int height) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return bmp;
    }
}
