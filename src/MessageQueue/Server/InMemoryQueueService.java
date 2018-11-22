// Written by Xiyan Wang, Nov 22nd, 2018

package MessageQueue.Server;

import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryQueueService implements QueueService {

    // For implementing visibility timeout
    static class InMemoryQueueServiceTimeoutTask extends AbstractTimeoutTask implements TimeoutTask, Runnable {

        // Record which consumer is processing the message
        long consumerThreadId = -1;

        public InMemoryQueueServiceTimeoutTask (Message msg, long consumerThreadId) {
            super(msg);
            this.consumerThreadId = consumerThreadId;
        }

        // Make message visible again (put the message back to queue) when timeout
        @Override
        public void onTimeout() throws InterruptedException {
            synchronized (this) {
                if (pullTasks.containsKey(this.consumerThreadId)) {
                    this.timeout();
                    pullTasks.remove(this.consumerThreadId);
                    queue.put(this.getMsg());
                }
            }
        }
    }

    // Message queue
    static private final BlockingQueue<Message> queue = new PriorityBlockingQueue<>();

    // Generate message id
    static private final AtomicInteger idx = new AtomicInteger(1);

    // Store timeout tasks so that they can be cancelled when consumer delete the corresponding message
    // key: consumer thread id, value: TimeoutTask
    static private final ConcurrentHashMap<Long, InMemoryQueueServiceTimeoutTask> pullTasks = new ConcurrentHashMap<>();

    // Timer which handles the timeout tasks
    static private final Timer timer = new Timer();

    // Timeout interval
    private int timeoutInterval = 1000;

    // Interface action: push
    // Put message in queue
    public int push(Object data) throws InterruptedException {
        int id = idx.getAndIncrement();
        queue.put(new Message(id, data));
        return id;
    }

    // Interface action: pull
    // Take the oldest message and make it invisible (remove from queue and set up a timeout task)
    // Timeout task will make the message visible again when timeout
    public Object pull() throws InterruptedException {
        InMemoryQueueServiceTimeoutTask task = new InMemoryQueueServiceTimeoutTask(queue.take(), Thread.currentThread().getId());
        pullTasks.put(Thread.currentThread().getId(), task);
        timer.schedule(task, this.timeoutInterval);
        return task.getMsg().getData();
    }

    // Interface action: delete
    // Cancel the corresponding timeout task
    // Return true if successfully deleted the message, return false if timeout
    public boolean delete() {
        InMemoryQueueServiceTimeoutTask task = pullTasks.get(Thread.currentThread().getId());
        if (task != null) {
            synchronized (task) {
                if (!task.isTimeout()) {
                    task.cancel();
                    timer.purge();
                    pullTasks.remove(Thread.currentThread().getId());
                    return true;
                }
            }
        }
        return false;
    }

    // Set timeout interval
    public void setTimeoutInterval (int timeoutInterval) {
        this.timeoutInterval = timeoutInterval;
    }
}
