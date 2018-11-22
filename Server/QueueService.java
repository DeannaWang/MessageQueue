// Written by Xiyan Wang, Nov 22nd, 2018

package MessageQueue.Server;

public interface QueueService {
    int push(Object data) throws InterruptedException;
    Object pull() throws InterruptedException;
    boolean delete() throws InterruptedException;
}