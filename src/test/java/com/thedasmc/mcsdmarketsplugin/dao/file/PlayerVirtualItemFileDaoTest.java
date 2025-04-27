package com.thedasmc.mcsdmarketsplugin.dao.file;

import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static com.thedasmc.mcsdmarketsplugin.util.PlayerVirtualItemUtil.createSamplePlayerVirtualItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayerVirtualItemFileDaoTest {

    private static final String SAVES_DIR = "Test-Plugin-Directory";
    private static TestablePlayerVirtualItemFileDao dao;

    @BeforeAll
    public static void beforeAll() {
        MCSDMarkets plugin = mock(MCSDMarkets.class);
        File savesDir = new File(SAVES_DIR);
        //noinspection ResultOfMethodCallIgnored
        savesDir.mkdir();
        when(plugin.getDataFolder()).thenReturn(savesDir);

        dao = new TestablePlayerVirtualItemFileDao(plugin);
    }

    @AfterAll
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void afterAll() {
        dao.shutdown();
        File rootTestDir = new File(SAVES_DIR);
        File savesDir = new File(SAVES_DIR, "saves");

        if (savesDir.exists()) {
            File[] files = savesDir.listFiles();

            if (files != null) {
                for (File file : files) {
                    file.delete();
                }

                savesDir.delete();
            }

            savesDir.delete();
        }

        rootTestDir.delete();
    }

    @Test
    public void ensureVersionFieldIsPopulatedWhenSavingNewEntity() {
        PlayerVirtualItem pvi = createSamplePlayerVirtualItem();
        pvi.setVersion(null);//Not needed but better to explicitly set to null
        PlayerVirtualItem saved = dao.save(pvi);

        assertNotNull(saved.getVersion());
        assertNotNull(pvi.getVersion());
        assertTrue(new File(SAVES_DIR, "saves/" + pvi.getId().getUuid() + ".json").exists());
    }

    @Test
    public void ensureOptimisticLockExceptionIsThrownWhenStateIsChangedAfterFetchingEntity() {
        //STEP 1: Create sample data
        PlayerVirtualItem pvi = createSamplePlayerVirtualItem();
        dao.save(pvi);

        //STEP 2: Fetch the same object twice
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        PlayerVirtualItem fetched1 = dao.findById(pvi.getId()).get();

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        PlayerVirtualItem fetched2 = dao.findById(pvi.getId()).get();

        //STEP 3: Modify and save fetched1
        fetched1.setQuantity(fetched1.getQuantity() + 5);
        dao.save(fetched1);

        //STEP 4: Try to modify and save fetched2 after modifying and saving fetched1
        fetched2.setQuantity(fetched2.getQuantity() + 5);
        assertThrows(OptimisticLockException.class, () -> dao.save(fetched2));
    }

    @Test
    public void ensureOnlyOneThreadHasAccessToSameSaveAtATime() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long wait = Duration.ofSeconds(3).toMillis();
        ReentrantLock lock = new ReentrantLock();
        lock.lock();

        final PlayerVirtualItem pvi = createSamplePlayerVirtualItem();
        Thread thread = new Thread(() -> {
            dao.save(pvi, lock);
            latch.countDown();
        });

        thread.start();
        //Thread should not be able to acquire the lock until the lock is released
        thread.join(wait);

        try {
            //Ensure the thread is still running since the lock hasn't been released yet
            assertTrue(thread.isAlive() && latch.getCount() == 1);
            lock.unlock();
            boolean released = latch.await(wait, TimeUnit.MILLISECONDS);//Once the thread has finished saving the data, the latch will be released
            assertTrue(released);//Make sure the work was done once the lock was released
            assertFalse(lock.isLocked());//Make sure the lock is unlocked after work is completed
            assertFalse(dao.containsLockFor(UUID.fromString(pvi.getId().getUuid())));//Make sure the lock was removed from the map in the dao since there is no other thread holding the lock
        } finally {
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException ignored) {
            }
        }
    }

}