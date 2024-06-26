package com.example.musicstreaming;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.ui.Model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;


@RestController
public class WebController {

    private String bucketName = "musicstream2-2d8e9.appspot.com";


    @GetMapping("/main")
    @ResponseBody
    public String home() throws ExecutionException, InterruptedException, IOException {
        List<Song> songs = getSongsFromFirestore(); // Get list of songs from Firestore

        String htmlContent = loadHtmlFromResource("static/index.html");

        // Generate HTML content for displaying songs
        StringBuilder songsHtml = new StringBuilder();
        for (Song song : songs) {
            songsHtml.append("<div>");
            songsHtml.append("<h3>").append(song.getSongName()).append("</h3>");
            songsHtml.append("<p>").append("<img src='").append(song.getSongImageURL()).append("'/>").append("</p>");
            songsHtml.append("<p>").append("<audio controls><source src='").append(song.getSongFileURL()).append("' type='audio/mpeg'/></audio>").append("</p>");
            songsHtml.append("</div>");
        }

        // Replace the placeholder with the generated HTML content in the main HTML content
        htmlContent = htmlContent.replace("{{songs}}", songsHtml.toString());

        return htmlContent;
    }

    private List<Song> getSongsFromFirestore() throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> querySnapshot = firestore.collection("songs").get();
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

        List<Song> songs = new ArrayList<>();
        for (DocumentSnapshot document : documents) {
            Song song = document.toObject(Song.class);
            songs.add(song);
        }

        return songs;
    }

    private String loadHtmlFromResource(String path) throws IOException {
        Resource resource = new ClassPathResource(path);
        InputStream inputStream = resource.getInputStream();
        byte[] bdata = FileCopyUtils.copyToByteArray(inputStream);
        return new String(bdata, StandardCharsets.UTF_8);
    }

    @GetMapping("/uploadPage")
    @ResponseBody
    public String showUploadPage(Model model) throws Exception {
        String htmlContent = loadHtmlFromResource("static/uploadPage.html");

        model.addAttribute("song", new Song());
        return htmlContent; // Assuming "uploadPage" is the name of your HTML view
    }

    @PostMapping("/uploadPage")
    public ModelAndView handleFileUpload(@RequestParam("songFile") MultipartFile file,
                                         @RequestParam("songName") String songName,
                                         @RequestParam("songImage") MultipartFile songImageFile) throws IOException {
        if (!file.isEmpty()) {
            // Upload the file to Firebase Storage and get the signed URL
            String signedUrl = uploadFileToStorage(file);
            String songImageURL = uploadFileToStorage(songImageFile);

            // Create a Song object
            Song song = new Song(songName, signedUrl, songImageURL);

            // Upload the Song object to Firestore
            uploadSongToFirestore(song);

            ModelAndView modelAndView = new ModelAndView("uploadStatus");
            modelAndView.addObject("message", "Song uploaded successfully!");
            return modelAndView;
        } else {
            ModelAndView modelAndView = new ModelAndView("uploadStatus");
            modelAndView.addObject("message", "Please select a file to upload.");
            return modelAndView;
        }
    }


    private String uploadFileToStorage(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename(); // Unique file name
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();

        // Upload the file to Firebase Storage
        StorageClient.getInstance().bucket(bucketName).create(fileName, file.getBytes());

        // Generate and return the signed URL for the file
        return StorageClient.getInstance().bucket(bucketName).get(fileName)
                .signUrl(9999, java.util.concurrent.TimeUnit.MINUTES).toString(); // URL expires in 15 minutes
    }

    private void uploadSongToFirestore(Song song) {
        // Get a Firestore instance
        Firestore firestore = FirestoreClient.getFirestore();

        // Create a document reference with a random ID
        DocumentReference docRef = firestore.collection("songs").document();

        // Convert the Song object to a Map
        Map<String, Object> songData = new HashMap<>();
        songData.put("songName", song.getSongName());
        songData.put("songFileURL", song.getSongFileURL());
        songData.put("songImageURL", song.getSongImageURL());

        // Upload the song data to Firestore
        docRef.set(songData);
    }

    @GetMapping("/")
    @ResponseBody
    public String showLoginPage(Model model) throws Exception {
        String htmlContent = loadHtmlFromResource("static/login.html");

        model.addAttribute("user", new User());
        return htmlContent;
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model) {
        Firestore firestore = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> querySnapshot = firestore.collection("users").whereEqualTo("username", username).get();

        try {
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            if (!documents.isEmpty()) {
                QueryDocumentSnapshot document = documents.get(0);
                String storedPassword = document.getString("password");
                System.out.println("hi");
                if (storedPassword.equals(password)) {
                    model.addAttribute("message", "Login successful!");
                    return "redirect:/main"; // Redirect to a main page
                } else {
                    model.addAttribute("message", "Incorrect password. Please try again.");
                    return "login";
                }
            } else {
                model.addAttribute("message", "User not found. Please try again.");
                return "login";
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "An error occurred. Please try again.");
            return "index";
        }
    }


    @GetMapping("/signup")
    @ResponseBody
    public String showSignupPage(Model model) throws Exception {
        String htmlContent = loadHtmlFromResource("static/signup.html");

        model.addAttribute("user", new User());
        return htmlContent; // Assuming "uploadPage" is the name of your HTML view
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String username, @RequestParam String password, Model model) {
        Firestore firestore = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> querySnapshot = firestore.collection("users").whereEqualTo("username", username).get();

        try {
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            if (!documents.isEmpty()) {
                model.addAttribute("message", "Username already exists. Please choose another.");
                return "redirect:/signup";
            } else {
                Map<String, Object> newUser = new HashMap<>();
                newUser.put("username", username);
                newUser.put("password", password);

                ApiFuture<WriteResult> writeResult = firestore.collection("users").document().set(newUser);
                writeResult.get(); // Ensure the write operation completes

                model.addAttribute("message", "Signup successful! Please log in.");
                return "redirect:/main";
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "An error occurred. Please try again.");
            return "redirect:/signup";
        }
    }
}