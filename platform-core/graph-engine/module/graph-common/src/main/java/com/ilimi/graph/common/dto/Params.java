package com.ilimi.graph.common.dto;

/**
 * 
 * @author santhosh
 * 
 */
public class Params extends BaseValueObject {

    private static final long serialVersionUID = 6772142067149203497L;

    private String resmsgid;
    private String msgid;
    private String err;
    private String status;
    private String errmsg;

    public enum StatusType {

        SUCCESS, WARNING, ERROR;
    }

    public String getResmsgid() {
        return resmsgid;
    }

    public void setResmsgid(String resmsgid) {
        this.resmsgid = resmsgid;
    }

    public String getMsgid() {
        return msgid;
    }

    public void setMsgid(String msgid) {
        this.msgid = msgid;
    }

    public String getErr() {
        return err;
    }

    public void setErr(String err) {
        this.err = err;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String message) {
        this.errmsg = message;
    }

    @Override
    public String toString() {
        return "Params [" + (resmsgid !=null ? "resmsgid=" + resmsgid + ", " : "") + (msgid != null ? "msgid=" + msgid + ", " : "") + (err != null ? "err=" + err + ", " : "") + (status != null ? "status=" + status + ", " : "") + (errmsg != null ? "errmsg=" + errmsg :"") + "]";
    }

    // @Override
    // public String toString() {
    // return "Params [" + (err != null ? "err=" + err + ", " : "") + (status !=
    // null ? "statusType=" + status + ", " : "")
    // + (errmsg != null ? "errmsg=" + errmsg + ", " : "") + "]";
    // }

}
