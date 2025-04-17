package uk.ac.ed.acp.cw2.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TotalMessage {
    private int totalMessagesWritten = 0;
    private int totalMessagesProcessed = 0;
    private int totalRedisUpdates = 0;
    private float totalValueWritten = 0.0f;
    private float totalAdded = 0.0f;

    public TotalMessage(int totalMessagesWritten, int totalMessagesProcessed,
                        int totalRedisUpdates, float totalValueWritten, float totalAdded) {
        this.totalMessagesWritten = totalMessagesWritten;
        this.totalMessagesProcessed = totalMessagesProcessed;
        this.totalRedisUpdates = totalRedisUpdates;
        this.totalValueWritten = totalValueWritten;
        this.totalAdded = totalAdded;
    }
}
