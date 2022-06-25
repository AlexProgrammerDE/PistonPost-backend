package net.pistonmaster.pistonpost.manager;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.storage.ImageStorage;
import net.pistonmaster.pistonpost.storage.VideoStorage;
import org.apache.commons.io.FilenameUtils;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.ContentDisposition;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
public class StaticFileManager {
    private final Path imagesPath;
    private final Path videosPath;
    private final PistonPostApplication application;
    private static final List<String> ALLOWED_IMAGE_EXTENSION = List.of("png", "jpg", "jpeg", "webp", "gif");
    private static final List<String> ALLOWED_VIDEO_EXTENSION = List.of("mp4", "mov", "webm", "mpeg", "mpg", "avi");
    private static final int MAX_IMAGE_SIZE_MB = 5;
    private static final int MAX_VIDEO_SIZE_MB = 50;

    public StaticFileManager(String staticFilesDir, PistonPostApplication application) {
        this.application = application;
        Path staticFilesPath = Path.of(staticFilesDir);
        this.imagesPath = staticFilesPath.resolve("images");
        this.videosPath = staticFilesPath.resolve("videos");
    }

    public void init() {
        try {
            Files.createDirectories(imagesPath);
            Files.createDirectories(videosPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ObjectId uploadImage(byte[] imageData, ContentDisposition imageMetaData) {
        if (bytesToMB(imageData.length) > MAX_IMAGE_SIZE_MB) {
            throw new WebApplicationException("Image is too big", 413);
        }

        ObjectId imageId = new ObjectId();
        String fileExtension = FilenameUtils.getExtension(imageMetaData.getFileName());
        if (!ALLOWED_IMAGE_EXTENSION.contains(fileExtension)) {
            throw new WebApplicationException("Invalid image extension!", 400);
        }
        System.out.println("Uploading image " + imageId + "." + fileExtension + " " + imageMetaData.getFileName());
        try {
            Path imagePath = imagesPath.resolve(imageId + "." + fileExtension);
            Files.write(imagePath, imageData);
            try (MongoClient mongoClient = application.createClient()) {
                MongoDatabase mongoDatabase = mongoClient.getDatabase("pistonpost");
                MongoCollection<ImageStorage> images = mongoDatabase.getCollection("images", ImageStorage.class);
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
                int width = image.getWidth();
                int height = image.getHeight();
                ImageStorage imageStorage = new ImageStorage(imageId, fileExtension, width, height);
                images.insertOne(imageStorage);
            }

            return imageId;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ObjectId uploadVideo(byte[] videoData, ContentDisposition videoMetaData) {
        if (bytesToMB(videoData.length) > MAX_VIDEO_SIZE_MB) {
            throw new WebApplicationException("Video is too big", 413);
        }

        ObjectId videoId = new ObjectId();
        String fileExtension = FilenameUtils.getExtension(videoMetaData.getFileName()).toLowerCase();
        if (!ALLOWED_VIDEO_EXTENSION.contains(fileExtension)) {
            throw new WebApplicationException("Invalid video extension!", 400);
        }
        try {
            Path imagePath = videosPath.resolve(videoId + "." + fileExtension);
            Files.write(imagePath, videoData);
            try (MongoClient mongoClient = application.createClient()) {
                MongoDatabase mongoDatabase = mongoClient.getDatabase("pistonpost");
                MongoCollection<VideoStorage> videos = mongoDatabase.getCollection("videos", VideoStorage.class);
                VideoStorage videoStorage = new VideoStorage(videoId, fileExtension, null);
                videos.insertOne(videoStorage);
            }

            return videoId;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final long MEGABYTE = 1024L * 1024L;

    public static long bytesToMB(long bytes) {
        return bytes / MEGABYTE;
    }
}
