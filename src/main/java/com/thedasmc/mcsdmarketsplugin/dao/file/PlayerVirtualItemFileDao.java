package com.thedasmc.mcsdmarketsplugin.dao.file;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.dao.PlayerVirtualItemDao;
import com.thedasmc.mcsdmarketsplugin.json.PlayerVirtualItemJsonConverter;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItemPK;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PlayerVirtualItemFileDao implements PlayerVirtualItemDao {

    private final Map<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private final File savesDir;
    private final Gson gson;

    public PlayerVirtualItemFileDao(MCSDMarkets plugin) {
        this.savesDir = new File(plugin.getDataFolder(), "saves");

        this.gson = new GsonBuilder()
            .registerTypeAdapter(PlayerVirtualItem.class, new PlayerVirtualItemJsonConverter())
            .create();
    }

    @Override
    public Optional<PlayerVirtualItem> findById(PlayerVirtualItemPK pk) {
        validatorFactory.getValidator().validate(pk);

        try {
            return runWithLock(UUID.fromString(pk.getUuid()), () -> loadPlayerVirtualItems(UUID.fromString(pk.getUuid())).stream()
                .filter(pvi -> pvi.getId().equals(pk))
                .findAny());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public PlayerVirtualItem save(PlayerVirtualItem playerVirtualItem) {
        validatorFactory.getValidator().validate(playerVirtualItem);

        try {
            return runWithLock(UUID.fromString(playerVirtualItem.getId().getUuid()), () -> {
                Map<String, PlayerVirtualItem> playerVirtualItems = loadPlayerVirtualItems(UUID.fromString(playerVirtualItem.getId().getUuid())).stream()
                    .collect(Collectors.toMap(pvi -> pvi.getId().getMaterial(), Function.identity()));

                PlayerVirtualItemPK id = playerVirtualItem.getId();

                //Check if already exists and check the version
                if (playerVirtualItems.containsKey(id.getMaterial())) {
                    PlayerVirtualItem existingPlayerVirtualItem = playerVirtualItems.get(id.getMaterial());
                    Integer existingVersion = existingPlayerVirtualItem.getVersion();
                    Integer version = playerVirtualItem.getVersion();

                    //A change to the entity was made, and saving would override those changes, throw exception
                    if (!Objects.equals(existingVersion, version))
                        throw new OptimisticLockException();

                    //Up the version to indicate the state was changed
                    playerVirtualItem.setVersion(existingVersion + 1);
                } else {
                    playerVirtualItem.setVersion(1);
                }

                playerVirtualItems.put(id.getMaterial(), playerVirtualItem);
                saveFile(UUID.fromString(id.getUuid()), playerVirtualItems.values());

                return playerVirtualItem;
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(PlayerVirtualItemPK pk) {
        validatorFactory.getValidator().validate(pk);

        try {
            runWithLock(UUID.fromString(pk.getUuid()), () -> {
                Map<String, PlayerVirtualItem> playerVirtualItems = loadPlayerVirtualItems(UUID.fromString(pk.getUuid())).stream()
                    .collect(Collectors.toMap(pvi -> pvi.getId().getMaterial(), Function.identity()));

                PlayerVirtualItem removed = playerVirtualItems.remove(pk.getMaterial());

                if (removed != null)
                    saveFile(UUID.fromString(pk.getUuid()), playerVirtualItems.values());

                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() {
        validatorFactory.close();
    }

    //Loads player's save file from disk
    private List<PlayerVirtualItem> loadPlayerVirtualItems(UUID uuid) {
        File file = new File(savesDir, uuid.toString() + ".json");

        try {
            //noinspection UnstableApiUsage
            return gson.fromJson(new FileReader(file, StandardCharsets.UTF_8), new TypeToken<List<PlayerVirtualItem>>(){}.getType());
        } catch (FileNotFoundException e) {
            return new LinkedList<>();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    //Writes player's save file to disk
    private void saveFile(UUID uuid, Collection<PlayerVirtualItem> playerVirtualItems) throws IOException {
        String json = gson.toJson(playerVirtualItems);
        File file = new File(savesDir, uuid + ".json");
        Files.writeString(file.toPath(), json);
    }

    //Run code locked on the specified uuid
    private <T> T runWithLock(UUID uuid, Callable<T> action) throws Exception {
        ReentrantLock lock = locks.computeIfAbsent(uuid, k -> new ReentrantLock());
        lock.lock();

        try {
            return action.call();
        } finally {
            lock.unlock();
            locks.compute(uuid, (k, currentLock) -> {
                try {
                    if (currentLock == lock && lock.tryLock()) {
                        return null;
                    }
                } finally {
                    lock.unlock();
                }

                return currentLock;
            });
        }
    }
}
