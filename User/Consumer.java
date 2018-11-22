// Written by Xiyan Wang, Nov 22nd, 2018

package MessageQueue.User;

// Consumer interface
public interface Consumer {
    void consume() throws InterruptedException;
}
