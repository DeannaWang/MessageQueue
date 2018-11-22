// Written by Xiyan Wang, Nov 23rd, 2018

package MessageQueue.User;

import MessageQueue.Server.FileBasedQueueService;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TestFileBasedQueueService {
    static FileBasedQueueService queueService = new FileBasedQueueService();  // File-based message queue
    static Random random = new Random(0);
    static AtomicInteger idx = new AtomicInteger(0);
    static final Object lock = new Object();

    // Consumer
    static class TestFileBasedQueueServiceConsumer extends AbstractConsumer implements Consumer, Runnable {
        private int id = -1;
        public TestFileBasedQueueServiceConsumer (int id) {
            this.id = id;
        }
        @Override
        public void consume() throws InterruptedException {
            Integer data = -1;

            // This lock is only for showing the messages are delivered in FIFO order
            // It can be removed if we don't have to make sure the log strings are printed in order
            // All necessary concurrency works are done on the server side
            synchronized (lock) {
                data = (Integer) queueService.pull();
                System.out.println("Consumer " + this.id + " consuming message: " + data);
            }

            Thread.sleep(300 + random.nextInt(300));
            boolean succeed = queueService.delete();
            if (succeed) {
                System.out.println("Consumer " + this.id + " consumed message: " + data);
            }
            else {
                System.out.println("Consumer " + this.id + " message timeout: " + data);
            }
        }
    }

    // Producer
    static class TestFileBasedQueueServiceProducer extends AbstractProducer implements Producer, Runnable {
        private int id = -1;
        public TestFileBasedQueueServiceProducer (int id) {
            this.id = id;
        }
        @Override
        public void produce() throws InterruptedException {
            Thread.sleep(random.nextInt(10000));
            int index = idx.getAndIncrement();
            int msgId = queueService.push(index);
            System.out.println("Producer " + this.id + " produced message: " + index);
        }
    }

    static public void main (String[] args) throws InterruptedException{

        queueService.setTimeoutInterval(500);

        for (int i = 0; i < 5; i++) {
            new Thread(new TestFileBasedQueueServiceProducer(i)).start();
        }

        Thread.sleep(10000);

        for (int i = 0; i < 5; i++) {
            new Thread(new TestFileBasedQueueServiceConsumer(i)).start();
        }
    }
}
