package uk.ac.ed.acp.cw2.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransformMessagesRequest {
    public String readQueue;
    public String writeQueue;
    public int messageCount;
}
