package net.pistonmaster.pistonpost.manager;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.MongoManager;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.storage.ImageStorage;
import net.pistonmaster.pistonpost.storage.PostStorage;
import net.pistonmaster.pistonpost.storage.VideoStorage;
import net.pistonmaster.pistonpost.utils.IDGenerator;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.ContentDisposition;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

@RequiredArgsConstructor
public class StaticFileManager {
    private final Path imagesPath;
    private final Path videosPath;
    private final PistonPostApplication application;

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
        ObjectId imageId = new ObjectId();
        try {
            Path imagePath = imagesPath.resolve(imageId + ".png");
            Files.write(imagePath, imageData);
            try (MongoClient mongoClient = application.createClient()) {
                MongoDatabase mongoDatabase = mongoClient.getDatabase("pistonpost");
                MongoCollection<ImageStorage> images = mongoDatabase.getCollection("images", ImageStorage.class);
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
                int width = image.getWidth();
                int height = image.getHeight();
                ImageStorage imageStorage = new ImageStorage(imageId, width, height);
                images.insertOne(imageStorage);
            }

            return imageId;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ObjectId uploadVideo(byte[] videoData, ContentDisposition videoMetaData) {
        ObjectId videoId = new ObjectId();
        try {
            Path imagePath = videosPath.resolve(videoId + ".mp4");
            Files.write(imagePath, videoData);
            try (MongoClient mongoClient = application.createClient()) {
                MongoDatabase mongoDatabase = mongoClient.getDatabase("pistonpost");
                MongoCollection<VideoStorage> videos = mongoDatabase.getCollection("videos", VideoStorage.class);
                VideoStorage videoStorage = new VideoStorage(videoId, "");
                videos.insertOne(videoStorage);
            }

            return videoId;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
