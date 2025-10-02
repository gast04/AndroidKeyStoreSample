/*
 * Copyright (c) 2020 Castle
 */

package com.sample.demo_keystore.castle.queue;

import android.content.Context;
import android.util.Log;

import com.sample.demo_keystore.castle.Castle;
import com.sample.demo_keystore.castle.CastleLogger;
import com.sample.demo_keystore.castle.Utils;
import com.sample.demo_keystore.castle.api.CastleAPIService;
import com.sample.demo_keystore.castle.api.model.Event;
import com.sample.demo_keystore.castle.api.model.Monitor;
import com.squareup.tape2.ObjectQueue;
import com.squareup.tape2.QueueFile;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventQueue implements Callback<Void> {

    private static final String BATCH_QUEUE_FILENAME = "castle-queue";
    private static final String QUEUE_FILENAME = "castle-monitor-queue";
    private static final int MAX_BATCH_SIZE = 20;

    private ObjectQueue<Event> eventObjectQueue;

    private Call<Void> flushCall;
    private boolean flushOngoing = false;
    private int flushCount;

    private ExecutorService executor;

    public EventQueue(Context context) {
        try {
            init(context);
        } catch (IOException e) {
            CastleLogger.e("Failed to create queue", e);

            // Delete the file and try again
            getFile(context).delete();

            try {
                init(context);
            } catch (IOException eRetry) {
                CastleLogger.e("Deleted queue file. Retried. Failed.", eRetry);
            }
        }
    }

    private synchronized File getFile(Context context) {
        return getFile(context, QUEUE_FILENAME);
    }

    private synchronized File getFile(Context context, String filename) {
        return new File(context.getApplicationContext().getFilesDir().getAbsoluteFile(), filename);
    }

    private synchronized void init(Context context) throws IOException {
        executor = Executors.newSingleThreadExecutor();

        // Delete old queue file
        try {
            getFile(context, BATCH_QUEUE_FILENAME).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File file = getFile(context);
        QueueFile queueFile = new QueueFile.Builder(file).build();
        Log.e("DEBUG_NIKU", "Create Queue File: " + file);

        eventObjectQueue = ObjectQueue.create(queueFile, new GsonConverter<>(Event.class));
    }

    public synchronized void add(Event event) {
        executor.execute(
                () -> {
                    try {
                        if (Castle.configuration().debugLoggingEnabled()) {
                            CastleLogger.d(
                                    "Tracking event " + Utils.getGsonInstance().toJson(event));
                        }

                        eventObjectQueue.add(event);

                        if (needsFlush()) {
                            flush();
                        }
                    } catch (IOException e) {
                        CastleLogger.e("Add to queue failed", e);
                    }
                });
    }

    private synchronized void trim() throws IOException {
        if (!isFlushing() && this.size() > Castle.configuration().maxQueueLimit()) {
            int eventsToTrim = this.size() - Castle.configuration().maxQueueLimit();
            remove(eventsToTrim);
            CastleLogger.d("Trimmed " + eventsToTrim + " events from queue");
        }
    }

    public synchronized void flush() {
        CastleLogger.d("EventQueue size " + eventObjectQueue.size());
        if (!isFlushing() && (!eventObjectQueue.isEmpty())) {
            flushOngoing = true; // set flush flag before thread is started
            executor.execute(
                    () -> {
                        try {
                            trim();

                            int end = Math.min(MAX_BATCH_SIZE, eventObjectQueue.size());
                            List<Event> subList = new ArrayList<>(end);
                            Iterator<Event> iterator = eventObjectQueue.iterator();
                            for (int i = 0; i < end; i++) {
                                try {
                                    Event event = iterator.next();

                                    if (event != null) {
                                        subList.add(event);
                                    }
                                } catch (Exception | Error exception) {
                                    CastleLogger.e("Unable to read from queue", exception);
                                }
                            }
                            List<Event> events = Collections.unmodifiableList(subList);
                            Monitor monitor = Monitor.monitorWithEvents(events);
                            if (monitor != null) {
                                // CastleLogger.d("Flushing EventQueue " + end);

                                flushCount = end;
                                try {
                                    flushCall = CastleAPIService.getInstance().monitor(monitor);
                                } catch (NullPointerException npe) {
                                    // Band aid for
                                    // https://github.com/castle/castle-android/issues/37
                                    CastleLogger.d(
                                            "Did not flush EventQueue because NPE, clearing"
                                                + " EventQueue");
                                    eventObjectQueue.clear();
                                }
                                flushCall.enqueue(this);
                            } else {
                                CastleLogger.d("Did not flush EventQueue");

                                // If events is empty and end is greater than zero, we just have
                                // unreadable data in the queue
                                if (end > 0) {
                                    eventObjectQueue.clear();
                                    CastleLogger.d(
                                            "Clearing EventQueue because of unreadable data");
                                }
                            }
                        } catch (IOException exception) {
                            CastleLogger.e("Unable to flush queue", exception);
                        }
                    });
        }
    }

    public synchronized boolean isFlushing() {
        return flushOngoing;
    }

    public synchronized boolean needsFlush() {
        return eventObjectQueue.size() >= Castle.configuration().flushLimit();
    }

    private synchronized void flushed() {
        flushCall = null;
        flushOngoing = false;
        flushCount = 0;
    }

    public synchronized int size() {
        return eventObjectQueue.size();
    }

    private synchronized void remove(int count) {
        try {
            eventObjectQueue.remove(count);

            CastleLogger.d("Removed " + count + " events from EventQueue");
        } catch (Exception e) {
            CastleLogger.e("Failed to remove events from queue", e);

            try {
                CastleLogger.d("Clearing EventQueue");
                eventObjectQueue.clear();
            } catch (Exception e1) {
                CastleLogger.d("Unable to clear EventQueue");
                e1.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void onResponse(Call<Void> call, Response<Void> response) {
        if (response.isSuccessful()) {
            // CastleLogger.i(response.code() + " " + response.message());
            // CastleLogger.i("Monitor request successful");

            executor.execute(
                    () -> {
                        remove(flushCount);

                        flushed();

                        // Check if queue size still exceed the flush limit and if it does, flush.
                        if (needsFlush()) {
                            Castle.flush();
                        }
                    });
        } else {
            CastleLogger.e(response.code() + " " + response.message());
            try {
                CastleLogger.e("Monitor request error:" + response.errorBody().string());
            } catch (Exception e) {
                CastleLogger.e("Monitor request error", e);
            }

            flushed();
        }
    }

    @Override
    public void onFailure(Call<Void> call, Throwable t) {
        CastleLogger.e("Monitor request failed", t);
        flushed();
    }

    public synchronized void destroy() {
        if (flushOngoing && flushCall != null) {
            flushCall.cancel();
        }
        flushed();
        executor.shutdown();
    }
}
