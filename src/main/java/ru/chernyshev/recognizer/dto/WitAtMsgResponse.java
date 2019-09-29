package ru.chernyshev.recognizer.dto;

public class WitAtMsgResponse {
    private String _text;
    private String msg_id;

    public WitAtMsgResponse() {
    }

    public String get_text() {
        return _text;
    }

    public void set_text(String _text) {
        this._text = _text;
    }

    public String getMsg_id() {
        return msg_id;
    }

    public void setMsg_id(String msg_id) {
        this.msg_id = msg_id;
    }

    @Override
    public String toString() {
        return "WitAtMsgResponse{" +
                "_text='" + _text + '\'' +
                ", msg_id='" + msg_id + '\'' +
                '}';
    }
}
