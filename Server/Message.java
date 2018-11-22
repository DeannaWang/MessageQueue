// Written by Xiyan Wang, Nov 22nd, 2018

package MessageQueue.Server;

import java.io.Serializable;

// Message class
// Data can be any object that implemented Serializable interface
public class Message implements Comparable <Message>, Serializable {
    private static final long serialVersionUID = 1L;
    private int id;                 // Message id which is determined by produce order
    private Object data;            // Data of message

    // Constructor
    public Message (int id, Object data) {
        this.id = id;
        this.data = data;
    }

    public int getId () {
        return this.id;
    }

    public Object getData () {
        return this.data;
    }

    // Use id which reflects produce order to order messages (For implementing FIFO)
    @Override
    public int compareTo(Message msg) {
        return this.id - msg.getId();
    }
}
