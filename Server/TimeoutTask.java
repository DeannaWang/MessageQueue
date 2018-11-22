// Written by Xiyan Wang, Nov 22nd, 2018

package MessageQueue.Server;

// TimeoutTask interface (visibility timeout)
public interface TimeoutTask {
    void onTimeout() throws InterruptedException;
}
