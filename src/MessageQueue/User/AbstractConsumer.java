// Written by Xiyan Wang, Nov 22nd, 2018

package MessageQueue.User;

// Consumer abstract class
abstract public class AbstractConsumer implements Consumer, Runnable {
    @Override
    public void run() {
        while (true) {
            try {
                consume();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
