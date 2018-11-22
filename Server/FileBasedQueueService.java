// Written by Xiyan Wang, Nov 23rd, 2018

package MessageQueue.Server;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FileBasedQueueService implements QueueService {

    // For implementing visibility timeout
    static class FileBasedQueueServiceTimeoutTask extends AbstractTimeoutTask implements TimeoutTask, Runnable {

        // Record which consumer is processing the message
        String consumerId = null;

        public FileBasedQueueServiceTimeoutTask (Message msg, String consumerId) {
            super(msg);
            this.consumerId = consumerId;
        }

        // Make message visible again (put the message back to queue) when timeout
        @Override
        public void onTimeout() throws InterruptedException {
            synchronized (this) {
                if (pullTasks.containsKey(this.consumerId)) {
                    this.timeout();
                    pullTasks.remove(this.consumerId);
                    queue.put(this.getMsg());
                }
            }
        }

        public String getConsumerId () {
            return this.consumerId;
        }
    }

    // Message queue
    static private final BlockingQueue<Message> queue = new PriorityBlockingQueue<>();

    // Generate message id
    static private AtomicInteger idx = null;

    // Store timeout tasks so that they can be cancelled when consumer delete the corresponding message
    // key: consumer id, value: TimeoutTask
    static private final ConcurrentHashMap<String, FileBasedQueueServiceTimeoutTask> pullTasks = new ConcurrentHashMap<>();

    // Timer which handles the timeout tasks
    static private final Timer timer = new Timer();

    // File directory
    private String fileDir;

    // Timeout interval
    private int timeoutInterval = 1000;

    // Constructors
    public FileBasedQueueService () {
        this(System.getProperty("user.dir"));
    }

    public FileBasedQueueService (String fileDir) throws NullPointerException {
        this.fileDir = fileDir;
        File dir = new File(fileDir);
        if (!dir.isDirectory()) {
            if (!dir.mkdir()) {
                System.out.println("Invalid file path!");
                System.exit(1);
            }
        }
        File[] fileList = dir.listFiles();
        int maxId = 0;

        for(File f: fileList){
            if(!f.isDirectory()) {
                int msgId = -1;
                try {
                    msgId = Integer.parseInt(f.getName());
                }
                catch (NumberFormatException e) {
                    continue;
                }
                try {
                    queue.put(new Message(msgId, msgId));
                    maxId = msgId > maxId ? msgId : maxId;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        idx = new AtomicInteger(maxId + 1);
    }

    // Interface action: push
    // Write file and put simplified message (without data) in queue
    public int push(Object data) throws InterruptedException {
        int id = idx.getAndIncrement();
        try {
            writeMessage(new Message(id, data));
        }
        catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        queue.put(new Message(id, null));
        return id;
    }

    // Interface action: pull
    // Take the oldest message and make it invisible (remove from queue and set up a timeout task)
    // Timeout task will make the message visible again when timeout
    public Object pull() throws InterruptedException {
        FileBasedQueueServiceTimeoutTask task = new FileBasedQueueServiceTimeoutTask(queue.take(), getConsumerId());
        pullTasks.put(task.getConsumerId(), task);
        timer.schedule(task, this.timeoutInterval);
        try {
            return readMessage(task.getMsg().getId()).getData();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Interface action: delete
    // Cancel the corresponding timeout task and delete file
    // Return true if successfully deleted the message, return false if timeout
    public boolean delete() {
        FileBasedQueueServiceTimeoutTask task = pullTasks.get(getConsumerId());
        if (task != null) {
            synchronized (task) {
                if (!task.isTimeout()) {
                    task.cancel();
                    timer.purge();
                    pullTasks.remove(getConsumerId());
                    return deleteMessage(task.getMsg().getId());
                }
            }
        }
        return false;
    }

    // Set timeout interval
    public void setTimeoutInterval (int timeoutInterval) {
        this.timeoutInterval = timeoutInterval;
    }

    // Read message from file
    private Message readMessage (int id) throws IOException, ClassNotFoundException {
        Path path = Paths.get(fileDir, String.valueOf(id));
        FileInputStream fis = new FileInputStream(path.toString());
        ObjectInputStream ois = new ObjectInputStream(fis);
        Message msg = (Message) ois.readObject();
        ois.close();
        fis.close();
        return msg;
    }

    // Write message to file
    private void writeMessage (Message msg) throws IOException {
        Path path = Paths.get(fileDir, String.valueOf(msg.getId()));
        FileOutputStream fos = new FileOutputStream(path.toString());
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(msg);
        oos.flush();
        oos.close();
        fos.close();
    }

    // Delete message file
    private boolean deleteMessage (int id) {
        Path path = Paths.get(fileDir, String.valueOf(id));
        return new File(path.toString()).delete();
    }

    // Return a consumer id which is combined with the consumer's process id and thread id
    private String getConsumerId () {
        return String.valueOf(Thread.currentThread().getId()) + " " +
                String.valueOf(Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]));
    }

    // Return next message id
    public int getNextMessageId () {
        return idx.get();
    }
}
