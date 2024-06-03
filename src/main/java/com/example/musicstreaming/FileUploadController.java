package com.example.musicstreaming;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.UUID;

public class FileUploadController {

    //קוד זה מופעל כאשר המשתמש מעלה שיר חדש לענן fbase

    // Map this method to respond to POST requests to /uploadPage
    @PostMapping("/uploadPage")
    public ModelAndView handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {
        System.out.println("HI");
        if (!file.isEmpty()) {
            String bucketName = "musicstream2-2d8e9.appspot.com";
            //יצירת שם קובץ ייחודי UUID
            String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename(); // Unique file name
            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();
            Blob blob = StorageClient.getInstance().bucket().create(fileName, file.getInputStream(), blobInfo.getContentType());

            // Generate a signed URL for the file
            String signedUrl = StorageClient.getInstance().bucket(bucketName).get(fileName)
                    .signUrl(15, java.util.concurrent.TimeUnit.MINUTES).toString(); // URL expires in 15 minutes

            ModelAndView modelAndView = new ModelAndView("uploadStatus");
            modelAndView.addObject("message", "File uploaded successfully: " + signedUrl);
            return modelAndView;
        } else {
            return new ModelAndView("uploadStatus", "message", "Please select a file to upload.");
        }
    }


}
