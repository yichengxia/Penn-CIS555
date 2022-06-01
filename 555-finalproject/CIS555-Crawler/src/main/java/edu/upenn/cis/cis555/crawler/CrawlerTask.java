package edu.upenn.cis.cis555.crawler;

import java.io.Serializable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import edu.upenn.cis.cis555.crawler.info.URLInfo;

/**
 * This is the class for executing crawler tasks.
 */
public class CrawlerTask implements Delayed, Serializable {

    private final URLInfo url;
    private final long timestamp = System.nanoTime();
    private int count = 0;
    private long delayed = 0L;
    
    public CrawlerTask(URLInfo url) {
        this.url = url;
        delayed = System.currentTimeMillis();
    }

    public CrawlerTask(URLInfo url, long delayed) {
        this.url = url;
        this.delayed = delayed;
    }

    public final URLInfo getUrl() {
        return url;
    }

    public final long getTimestamp() {
        return timestamp;
    }

    public final int getCount() {
        return count;
    }

    private final void setCount(int count) {
        this.count = count;
    }

    public final void setDelay(long delayed) {
        this.delayed = delayed;
    }

    /**
     * Update directory with the given one and get redirected task with it
     * @param directory
     * @return task
     */
    public CrawlerTask getRedirectedTask(String directory) {
        if (directory.startsWith("/")) {
            directory = getUrl().toRootURLString() + directory;
        }
        CrawlerTask task = new CrawlerTask(new URLInfo(directory), delayed);
        task.setCount(count + 1);
        return task;
    }

    /**
     * Override compareTo method to compare delays
     * @return res
     */
    @Override
    public int compareTo(Delayed o) {
        int res = Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
        if (res == 0) {
            return Long.compare(getTimestamp(), ((CrawlerTask) o).getTimestamp());
        }
        return res;
    }

    /**
     * Get delay with given time unit
     * @return delay
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delayed - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
}
