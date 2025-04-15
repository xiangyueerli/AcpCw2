package uk.ac.ed.acp.cw2.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessMessagesRequest {
    public String readTopic;
    public String writeQueueGood;
    public String writeQueueBad;
    public int messageCount;
}


