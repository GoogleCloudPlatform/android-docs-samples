package com.google.cloud.examples.dialogflow.model;

public class ChatMsgModel {

    private String msg;
    private int type;

    public ChatMsgModel(String msg, int type) {
        this.msg = msg;
        this.type = type;
    }

    public String getMsg() {
        return msg;
    }

    public int getType() {
        return type;
    }

}
