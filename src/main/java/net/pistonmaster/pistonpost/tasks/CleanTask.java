package net.pistonmaster.pistonpost.tasks;

import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.manager.StaticFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

@RequiredArgsConstructor
public class CleanTask extends TimerTask {
    private final StaticFileManager manager;
    private final Logger logger = LoggerFactory.getLogger(CleanTask.class);

    @Override
    public void run() {
        logger.info("Cleaning up files...");
        manager.cleanUnusedFiles();
    }
}
