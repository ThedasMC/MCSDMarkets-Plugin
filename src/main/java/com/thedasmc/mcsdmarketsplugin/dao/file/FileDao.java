package com.thedasmc.mcsdmarketsplugin.dao.file;

import com.google.gson.Gson;
import jakarta.validation.ValidatorFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public abstract class FileDao<LK> {

    protected final Map<LK, ReentrantLock> locks = new ConcurrentHashMap<>();

    protected final Gson gson;
    protected final ValidatorFactory validatorFactory;
    protected final File savesDir;

    /*
     * All Entities must be registered with gson here if they have serializer/deserializer
     */
    public FileDao(Gson gson, ValidatorFactory validatorFactory, File savesDir) {
        this.gson = gson;
        this.validatorFactory = validatorFactory;
        this.savesDir = savesDir;
    }

    protected void validate(Object object) {
        validatorFactory.getValidator().validate(object);
    }

    protected void saveContent(String content, File saveFile) throws IOException {
        Files.writeString(saveFile.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    protected <T> T runWithLock(LK key, Callable<T> action) throws Exception {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();

        try {
            return action.call();
        } finally {
            lock.unlock();
            locks.compute(key, (k, currentLock) -> {
                if (currentLock == lock && lock.tryLock()) {
                    try {
                        return null;
                    } finally {
                        lock.unlock();
                    }
                }

                return currentLock;
            });
        }
    }
}
