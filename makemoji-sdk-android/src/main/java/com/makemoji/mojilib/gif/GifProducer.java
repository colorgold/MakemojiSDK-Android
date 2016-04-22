package com.makemoji.mojilib.gif;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.Spanimator;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * wraps the decoder so it can be used by multiple targets
 * Created by Scott Baar on 4/19/2016.
 */
public class GifProducer implements Runnable{
    private static String TAG = "GifProducer";
    static Map<String,GifProducer> producerMap = Collections.synchronizedMap(new HashMap<String,GifProducer>());
    public static synchronized GifProducer getProducerAndSub(GifConsumer consumer, @Nullable byte[] bytes, String url){
        GifProducer producer = producerMap.get(url);
        if (producer!=null){
            producer.subscribe(consumer);
            return producer;
        }
        if (bytes==null)return null;
        producer = new GifProducer(consumer,bytes,url);
        producerMap.put(url,producer);
        return producer;
    }
    private static void removeProducer(GifProducer producer){
        producerMap.remove(producer.url);
    }
    final List<SoftReference<GifConsumer>> consumers = Collections.synchronizedList(new ArrayList<SoftReference<GifConsumer>>());
    GifDecoder gifDecoder;
    Thread animationThread;
    Bitmap tmpBitmap;
    boolean shouldClear;
    static Handler handler = new Handler(Looper.getMainLooper());
    private long framesDisplayDuration = -1L;
    String url;
    final static Object syncObject = new Object();//wait while activity is paused

    private GifProducer(GifConsumer consumer,byte[] bytes,String url) {
        this.url = url;
        consumers.add(new SoftReference<>(consumer));
        gifDecoder = new GifDecoder();
        try {
            gifDecoder.read(bytes);
            gifDecoder.advance();
        } catch (final OutOfMemoryError e) {
            gifDecoder = null;
            Log.e(TAG, e.getMessage(), e);
            return;
        }

        start();
    }
    public int getHeight(){
        return gifDecoder==null?0:gifDecoder.getHeight();
    }
    public int getWidth(){
        return gifDecoder==null?0:gifDecoder.getWidth();
    }
    void start(){
        if (animationThread==null && canStart()){
                animationThread = new Thread(this);
                animationThread.setName(TAG+ url);
                animationThread.start();
        }
    }

    public static void onStop(){
    }
    public static void onStart(){
        synchronized (syncObject){
            syncObject.notifyAll();
        }

    }
    public boolean animating(){
        return Spanimator.isGifRunning();
    }
    private boolean canStart() {
        return animating() && gifDecoder != null && animationThread == null;
    }
    private final Runnable cleanupRunnable = new Runnable() {
        @Override
        public void run() {
            tmpBitmap = null;
            gifDecoder = null;
            animationThread = null;
            shouldClear = false;
        }
    };
    @Override
    public void run() {
        if (shouldClear) {
            handler.post(cleanupRunnable);
            return;
        }

        final int n = gifDecoder.getFrameCount();
        do {
            for (int i = 0; i < n; i++) {
                if (!animating()) {
                    break;
                }
                //milliseconds spent on frame decode
                long frameDecodeTime = 0;
                try {
                    long before = System.nanoTime();
                    tmpBitmap = Bitmap.createBitmap(gifDecoder.getNextFrame());
                    frameDecodeTime = (System.nanoTime() - before) / 1000000;
                    synchronized (consumers) {
                        for (SoftReference<GifConsumer> sr : consumers) {
                            GifConsumer c = sr.get();
                            if (c!=null)c.onFrameAvailable(tmpBitmap);
                        }
                    }

                    if (!animating()) {
                        break;
                    }
                    //handler.post(updateResults);
                } catch (final ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
                    Log.w(TAG, e);
                }
                if (!animating()) {
                    break;
                }
                gifDecoder.advance();
                try {
                    int delay = gifDecoder.getNextDelay();
                    // Sleep for frame duration minus time already spent on frame decode
                    // Actually we need next frame decode duration here,
                    // but I use previous frame time to make code more readable
                    delay -= frameDecodeTime;
                    if (delay > 0) {
                        Thread.sleep(framesDisplayDuration > 0 ? framesDisplayDuration : delay);
                    }
                } catch (final Exception e) {
                    // suppress any exception
                    // it can be InterruptedException or IllegalArgumentException
                }
            }
            if (!animating()){
                try {

                    synchronized (syncObject) {
                        syncObject.wait();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            ListIterator<SoftReference<GifConsumer>> iterator = consumers.listIterator();
            while (iterator.hasNext()) {
                GifConsumer c = iterator.next().get();
                if (c==null) iterator.remove();
            }
        } while (!consumers.isEmpty());
        synchronized (consumers) {
            ListIterator<SoftReference<GifConsumer>> iterator = consumers.listIterator();
             while (iterator.hasNext()) {
                GifConsumer c = iterator.next().get();
                if (c!=null)c.onStopped();
                 else iterator.remove();
            }
        }
        animationThread =null;
        removeProducer(this);
    }
    public void subscribe(GifConsumer consumer){
        consumers.add(new SoftReference<>(consumer));
        start();
        if (tmpBitmap!=null)
            consumer.onFrameAvailable(tmpBitmap);
    }
    public void unsubscribe(GifConsumer consumer){
        ListIterator<SoftReference<GifConsumer>> iterator = consumers.listIterator();
        while (iterator.hasNext()) {
            if (consumer.equals(iterator.next().get()))
                iterator.remove();

        }
    }
}