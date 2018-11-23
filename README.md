# MessageQueue
In-memory and file-based message queue (for an interview)

## How to test

#### In-memory Message Queue
javac MessageQueue/User/TestInMemoryQueueService.java
java MessageQueue.User.TestInMemoryQueueService

#### File-based Message Queue
javac MessageQueue/User/TestFileBasedQueueService.java
java MessageQueue.User.TestFileBasedQueueService

**Note**: Multi-process provider / consumer is not tested, but it should work fine with file-based message queue.
