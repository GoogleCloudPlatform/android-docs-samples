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

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
