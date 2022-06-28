package net.pistonmaster.pistonpost.manager;

import com.luciad.imageio.webp.WebPWriteParam;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.gif.GifSequenceWriter;
import net.pistonmaster.pistonpost.gif.ImageFrame;
import net.pistonmaster.pistonpost.storage.ImageStorage;
import net.pistonmaster.pistonpost.storage.VideoStorage;
import org.apache.commons.io.FilenameUtils;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.ContentDisposition;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static net.pistonmaster.pistonpost.gif.GifUtil.readGif;

@RequiredArgsConstructor
public class StaticFileManager {
    private static final List<String> ALLOWED_IMAGE_EXTENSION = List.of("png", "jpg", "jpeg", "webp", "gif", "tiff", "bmp", "wbmp");
    private static final List<String> ALLOWED_VIDEO_EXTENSION = List.of("mp4", "mov", "webm", "mpeg", "mpg", "avi");
    private static final int MAX_IMAGE_SIZE_MB = 5;
    private static final int MAX_VIDEO_SIZE_MB = 50;
    private static final long MEGABYTE = 1024L * 1024L;
    private final Path imagesPath;
    private final Path videosPath;
    private final PistonPostApplication application;

    public StaticFileManager(String staticFilesDir, PistonPostApplication application) {
        this.application = application;
        Path staticFilesPath = Path.of(staticFilesDir);
        this.imagesPath = staticFilesPath.resolve("images");
        this.videosPath = staticFilesPath.resolve("videos");
    }

    public static long bytesToMB(long bytes) {
        return bytes / MEGABYTE;
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

        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            ImageReader reader = ImageIO.getImageReaders(in).next();
            reader.setInput(in, true, false);
            BufferedImage image = reader.read(0);
            IIOMetadata metadata = reader.getImageMetadata(0);
            reader.dispose();

            Path imagePath = imagesPath.resolve(imageId + "." + reader.getFormatName().toLowerCase());

            try (ImageOutputStream out = ImageIO.createImageOutputStream(Files.newOutputStream(imagePath))) {
                if (reader.getFormatName().equalsIgnoreCase("gif")) {
                    ImageFrame[] frames = readGif(reader);
                    GifSequenceWriter writer =
                            new GifSequenceWriter(out, frames[0].getImage().getType(), frames[0].getDelay(), true, "PistonPost");

                    writer.writeToSequence(frames[0].getImage());
                    for (int i = 1; i < frames.length; i++) {
                        BufferedImage nextImage = frames[i].getImage();
                        writer.writeToSequence(nextImage);
                    }

                    writer.close();
                } else {
                    ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
                    ImageWriter writer = ImageIO.getImageWriters(type, fileExtension).next();

                    ImageWriteParam param = writer.getDefaultWriteParam();
                    if (param.canWriteCompressed()) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

                        if (param instanceof JPEGImageWriteParam jpegParam) {
                            jpegParam.setOptimizeHuffmanTables(true);
                        }

                        if (param instanceof WebPWriteParam) {
                            param.setCompressionType(param.getCompressionTypes()[WebPWriteParam.LOSSLESS_COMPRESSION]);
                        } else {
                            param.setCompressionType(param.getCompressionTypes()[0]);
                        }

                        param.setCompressionQuality(1.0f);
                    }

                    writer.setOutput(out);
                    writer.write(null, new IIOImage(image, null, metadata), param);
                    writer.dispose();
                }
            }

            try (MongoClient mongoClient = application.createClient()) {
                MongoDatabase mongoDatabase = mongoClient.getDatabase("pistonpost");
                MongoCollection<ImageStorage> images = mongoDatabase.getCollection("images", ImageStorage.class);
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
}
