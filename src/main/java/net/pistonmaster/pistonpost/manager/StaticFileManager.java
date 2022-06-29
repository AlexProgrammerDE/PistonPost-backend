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
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;

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
import java.util.Map;

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
    private final Path videoTempDir;
    private final PistonPostApplication application;

    public StaticFileManager(String staticFilesDir, PistonPostApplication application) {
        this.application = application;
        Path staticFilesPath = Path.of(staticFilesDir);
        this.imagesPath = staticFilesPath.resolve("images");
        this.videosPath = staticFilesPath.resolve("videos");
        try {
            this.videoTempDir = Files.createTempDirectory("pistonpost-video-temp");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

            Path imagePath = imagesPath.resolve(imageId + "." + fileExtension.toLowerCase());

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
                ImageStorage imageStorage = new ImageStorage(imageId, fileExtension.toLowerCase(), width, height);
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
        Path videoTempPath = videoTempDir.resolve(videoId + "-uncompressed." + fileExtension);
        Path videoPath = videosPath.resolve(videoId + ".mp4");
        try {
            Files.write(videoTempPath, videoData);

            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("aac");
            audio.setBitRate(128000);

            VideoAttributes video = new VideoAttributes();
            video.setCodec("libx264");
            video.setFaststart(true);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setInputFormat(fileExtension);
            attrs.setOutputFormat("mp4");
            attrs.setAudioAttributes(audio);
            attrs.setVideoAttributes(video);
            attrs.setExtraContext(Map.of("crf", "23", "preset", "superfast"));

            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(videoTempPath.toFile()), videoPath.toFile(), attrs);

            Files.delete(videoTempPath);

            ImageStorage videoThumbnail = generateThumbnail(videoPath);

            try (MongoClient mongoClient = application.createClient()) {
                MongoDatabase mongoDatabase = mongoClient.getDatabase("pistonpost");
                MongoCollection<VideoStorage> videos = mongoDatabase.getCollection("videos", VideoStorage.class);
                VideoStorage videoStorage = new VideoStorage(videoId, "mp4", videoThumbnail.getId(), videoThumbnail.getWidth(), videoThumbnail.getHeight());
                videos.insertOne(videoStorage);
            }

            return videoId;
        } catch (IOException | EncoderException e) {
            try {
                Files.deleteIfExists(videoTempPath);
                Files.deleteIfExists(videoPath);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            throw new RuntimeException(e);
        }
    }

    public ImageStorage generateThumbnail(Path video) {
        try {
            ObjectId imageId = new ObjectId();
            Path imagePath = imagesPath.resolve(imageId + ".png");

            FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(video.toFile()));
            grab.seekToFrameSloppy(0);

            Picture picture = grab.getNativeFrame();
            BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
            try (ImageOutputStream out = ImageIO.createImageOutputStream(Files.newOutputStream(imagePath))) {
                ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(bufferedImage);
                ImageWriter writer = ImageIO.getImageWriters(type, "png").next();

                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(0.8f);
                }

                writer.setOutput(out);
                writer.write(null, new IIOImage(bufferedImage, null, null), param);
                writer.dispose();
            }

            try (MongoClient mongoClient = application.createClient()) {
                MongoDatabase mongoDatabase = mongoClient.getDatabase("pistonpost");
                MongoCollection<ImageStorage> images = mongoDatabase.getCollection("images", ImageStorage.class);
                int width = bufferedImage.getWidth();
                int height = bufferedImage.getHeight();
                ImageStorage imageStorage = new ImageStorage(imageId, "png", width, height);
                images.insertOne(imageStorage);
                return imageStorage;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
