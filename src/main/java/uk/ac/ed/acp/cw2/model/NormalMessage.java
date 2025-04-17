package uk.ac.ed.acp.cw2.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NormalMessage {
    private String key;

    //private int version;
    // 方便Jackson判断
    private Integer version;  // Integer 而不是 int，可以判断 null

    private Float value;
}
