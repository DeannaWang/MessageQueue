// Written by Xiyan Wang, Nov 22nd, 2018

package MessageQueue.Server;

import java.util.TimerTask;

// TimeoutTask abstract class (visibility timeout)
abstract public class AbstractTimeoutTask extends TimerTask implements TimeoutTask {
    private Message msg = null;
    private boolean timeout = false;

    public AbstractTimeoutTask (Message msg) {
        this.msg = msg;
    }

    public Message getMsg () {
        return this.msg;
    }

    public boolean isTimeout () {
        return this.timeout;
    }

    protected void timeout () {
        this.timeout = true;
    }

    @Override
    public void run () {
        try {
            onTimeout();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
