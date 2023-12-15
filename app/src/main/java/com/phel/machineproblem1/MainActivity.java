package com.phel.machineproblem1;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView progressText;
    private Button downloadButton;

    // Request code for directory selection
    private static final int REQUEST_CODE_PICK_DIRECTORY = 123;

    // Declare fileUrl as a class member
    private String fileUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
        downloadButton = findViewById(R.id.downloadButton);

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText linkEditText = findViewById(R.id.linkEditText);
                // Assign the value to fileUrl
                fileUrl = linkEditText.getText().toString();

                // Launch the directory picker Intent
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_CODE_PICK_DIRECTORY);
            }
        });
    }

    // Handle the result from the directory picker
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_DIRECTORY && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            getContentResolver().takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            new DownloadFileTask(treeUri).execute(fileUrl);
        }
    }

    private class DownloadFileTask extends AsyncTask<String, Integer, Boolean> {

        private Uri treeUri;

        public DownloadFileTask(Uri treeUri) {
            this.treeUri = treeUri;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String fileUrl = params[0];
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                int fileLength = urlConnection.getContentLength();
                InputStream input = new BufferedInputStream(url.openStream());

                // Get the DocumentFile representing the selected directory
                DocumentFile pickedDir = DocumentFile.fromTreeUri(MainActivity.this, treeUri);

                // Create a file within the picked directory
                DocumentFile file = pickedDir.createFile("", "downloaded_file.zip");

                // Open the OutputStream for the file
                FileOutputStream output = (FileOutputStream) getContentResolver().openOutputStream(file.getUri());

                byte[] data = new byte[1024];
                long total = 0;
                int count;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    int progress = (int) ((total * 100) / fileLength);
                    publishProgress(progress);
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("DownloadFileTask", "Error: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
            progressText.setText("Download Progress: " + values[0] + "%");
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            downloadButton.setEnabled(true);
            if (result) {
                progressText.setText("Download Complete");
            } else {
                progressText.setText("Download Failed. Check Logcat for details.");
            }
        }
    }
}
