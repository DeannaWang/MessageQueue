// Written by Xiyan Wang, Nov 22nd, 2018

package MessageQueue.User;

// Producer abstract class
abstract public class AbstractProducer implements Producer, Runnable {
    @Override
    public void run() {
        while (true) {
            try {
                produce();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
