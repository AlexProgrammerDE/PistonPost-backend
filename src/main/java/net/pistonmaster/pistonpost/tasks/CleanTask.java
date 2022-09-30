package net.pistonmaster.pistonpost.tasks;

import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.manager.StaticFileManager;

import java.util.TimerTask;

@RequiredArgsConstructor
public class CleanTask extends TimerTask {
    private final StaticFileManager manager;

    @Override
    public void run() {
        manager.cleanUnusedFiles();
    }
}
