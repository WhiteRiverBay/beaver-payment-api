package ltd.wrb.payment.util;

import java.util.HashMap;
import java.util.Map;

public class Result {

    final public static Result SUCCESS = new Result(1, "success");
    final public static Result ERROR = new Result(0, "error");
    final public static Result NOT_REG = new Result(401, "not reg");
    final public static Result NOT_AUTHED = new Result(1001, "not login");
    final public static Result NOT_ACCESS = new Result(1002, "not access");
    final public static Result EXPIRED = new Result(1003, "out of service");
    final public static Result NOT_VERIFY = new Result(1004, "not verify");
    final public static Result ERROR_SYSTEM = new Result(40001, "system error");
    final public static Result ERROR_PARAM = new Result(40011, "Request Params error");
    final public static Result EROOR_VERIFY = new Result(40012, "signature error.");

    private int code;
    private String msg;
    private Object data;
    private long systemTime;

    public Result(int code, String msg) {
        this(code, msg, null);
    }

    public Result(int code, String msg, Object context) {
        this.code = code;
        this.msg = msg;
        this.data = context;
        this.systemTime = System.currentTimeMillis();
    }

    public long getSystemTime() {
        return systemTime;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Result copyThis() {
        return new Result(code, msg, null);
    }

    public static Result error(String error) {
        return new Result(ERROR.code, error);
    }

    public static Result error(int code, String error) {
        return new Result(code, error);
    }

    public static Result error(String error, Object ctx) {
        return new Result(ERROR.code, error, ctx);
    }

    public static Result success(Object ctx) {
        Result r = Result.SUCCESS.copyThis();
        r.setData(ctx);
        return r;
    }

    public static Result detail(Object obj) {
        Map<String, Object> map = new HashMap<>();
        map.put("detail", obj);
        return Result.success(map);
    }

    public static Result details(Object obj) {
        Map<String, Object> map = new HashMap<>();
        map.put("details", obj);
        return Result.success(map);
    }
}